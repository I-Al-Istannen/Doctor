package de.ialistannen.doctor.util;

import static de.ialistannen.doctor.util.parsers.ArgumentParsers.nestedQuote;
import static de.ialistannen.doctor.util.parsers.ArgumentParsers.phrase;

import de.ialistannen.doctor.util.parsers.StringReader;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import de.ialistannen.javadocapi.model.types.JavadocMethod;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class DeclarationFormatter {

  private final int maxLength;

  public DeclarationFormatter(int maxLength) {
    this.maxLength = maxLength;
  }

  public String formatDeclaration(JavadocElement element) {
    String declaration = element.getDeclaration(DeclarationStyle.SHORT).strip();
    StringReader reader = new StringReader(declaration);

    StringBuilder result = new StringBuilder();
    result.append(formatAnnotations(reader));

    if (element instanceof JavadocMethod) {
      result.append(formatMethod(reader));
    }

    if (element instanceof JavadocType) {
      result.append(formatType(reader, (JavadocType) element));
    }

    return result.toString().strip();
  }

  private String formatMethod(StringReader input) {
    StringBuilder result = new StringBuilder();

    result.append(input.readWhile(c -> c != '('));

    result.append(input.assertRead('('));
    result.append(formatMethodParameters(result.length(), input));
    result.append(input.assertRead(')'));

    return result.toString();
  }

  private String formatMethodParameters(int currentSize, StringReader input) {
    StringBuilder result = new StringBuilder();
    boolean chopDown = input.peekWhile(c -> c != ')').length() + currentSize > maxLength;

    while (input.peek() != ')') {
      String untilNext = input.readWhile(c -> c != ')' && c != ',' && c != '<').strip();
      if (input.peek() == '<') {
        untilNext += "<" + nestedQuote('<', '>').parse(input).getOrThrow() + ">";
        untilNext += input.readWhile(c -> c != ')' && c != ',');
        untilNext = untilNext.strip();
      }

      if (chopDown) {
        result.append("\n  ");
      }

      result.append(untilNext);
      if (input.peek() == ',') {
        result.append(input.readChar());
        if (!chopDown) {
          result.append(" ");
        }
      }
    }

    if (chopDown) {
      result.append("\n");
    }

    return result.toString();
  }

  private String formatType(StringReader input, JavadocType element) {
    StringBuilder result = new StringBuilder();

    boolean choppedDown = input.remaining() > maxLength;

    if (element.getSuperClass() != null) {
      String rest = input.peekWhile(c -> true);
      int index = rest.indexOf(" extends ");
      result.append(input.readChars(index));

      String extendsKeyword = input.assertRead(" extends ");
      String superclass = input.readWhile(Character::isJavaIdentifierPart);

      if (choppedDown) {
        result.append("\n ");
      }
      result.append(extendsKeyword);
      result.append(superclass);
    }

    if (element.getSuperInterfaces() != null && !element.getSuperInterfaces().isEmpty()) {
      if (!input.peek(" implements ".length()).equals(" implements ")) {
        String rest = input.peekWhile(c -> true);
        int index = rest.indexOf(" implements ");
        result.append(input.readChars(index));
      }

      if (choppedDown) {
        result.append("\n ");
      }

      result.append(input.assertRead(" implements "));
      // Chop down interfaces!
      if (input.remaining() + " implements ".length() > maxLength) {
        while (input.remaining() > 0) {
          String type = input.readWhile(c -> c == '.' || Character.isJavaIdentifierPart(c));
          if (input.peek() == '<') {
            type += "<" + nestedQuote('<', '>').parse(input).getOrThrow() + ">";
          }
          result
              .append(type)
              .append(input.remaining() > 0 ? "," : "")
              .append("\n  ")
              .append(" ".repeat("implements ".length()));

          if (input.remaining() > 0) {
            input.assertRead(", ");
          }
        }
      }
      result.append(input.readRemaining());
    }

    if (result.isEmpty()) {
      result.append(input.readRemaining());
    }

    return result.toString().strip();
  }

  private String formatAnnotations(StringReader input) {
    StringBuilder result = new StringBuilder();

    while (input.peek() == '@') {
      String name = input.readWhile(c -> c != '(' && c != '\n');
      result.append(name);
      if (input.peek() == '(') {
        result.append(formatAnnotationParameters(name.length(), input));
      }

      if (input.peek() == '\n') {
        input.readChar();
      }
      if (input.peek() == '@') {
        result.append("\n");
      }
    }

    if (result.length() > 0) {
      result.append("\n");
    }

    return result.toString();
  }

  private String formatAnnotationParameters(int currentSize, StringReader input) {
    StringBuilder result = new StringBuilder();
    result.append(input.assertRead('('));

    List<Pair<String, String>> parameters = new ArrayList<>();

    while (input.peek() != ')') {
      parameters.add(parseAnnotationParameter(input));
      if (input.peek() == ',') {
        input.assertRead(", ");
      }
    }

    if (parameters.size() == 1 && parameters.get(0).getKey().equals("value")) {
      String value = parameters.get(0).getValue();
      if (value.length() + currentSize + result.length() <= maxLength) {
        result.append(value);
      } else {
        result.append("\n");
        result.append(formatAnnotationParameterValue(parameters.get(0).getValue()).indent(2));
      }
      parameters.clear();
    }

    String joinedParameters = parameters.stream()
        .map(it -> it.getKey() + " = " + it.getValue())
        .collect(Collectors.joining(", "));

    if (joinedParameters.length() + 2 + currentSize <= maxLength) {
      result.append(joinedParameters);
    } else {
      result.append("\n");

      StringBuilder chopBuilder = new StringBuilder();
      // Choppy choppy
      for (int i = 0; i < parameters.size(); i++) {
        Pair<String, String> parameter = parameters.get(i);
        chopBuilder
            .append(parameter.getKey())
            .append(" = ");
        chopBuilder.append(formatAnnotationParameterValue(parameter.getValue()));

        if (i != parameters.size() - 1) {
          chopBuilder.append(",\n");
        }
      }

      result.append(chopBuilder.toString().indent(2));
    }

    result.append(input.assertRead(')'));

    return result.toString();
  }

  private String formatAnnotationParameterValue(String parameterValue) {
    String result = "";
    if (parameterValue.contains("{")) {
      List<String> elements = getAnnotationArrayElements(
          new StringReader(parameterValue)
      );
      result += "{\n";
      result += String.join(",\n", elements).indent(2);
      result += "}";
    } else {
      result += parameterValue;
    }

    return result;
  }

  private Pair<String, String> parseAnnotationParameter(StringReader input) {
    String value = "";

    String name = input.readWhile(Character::isJavaIdentifierPart);
    input.readRegex(Pattern.compile("\\s+=\\s+"));

    if (input.peek() == '"') {
      value += '"';
      value += phrase().parse(input).getOrThrow();
      value += '"';
    } else if (input.peek() == '{') {
      value += input.readWhile(c -> c != '}');
      value += input.assertRead("}");
    } else {
      value += input.readRegex(Pattern.compile("[^,)} ]+"));
    }

    return Pair.of(name, value);
  }

  private List<String> getAnnotationArrayElements(StringReader input) {
    List<String> elements = new ArrayList<>();

    input.assertRead("{");
    while (input.peek() != '}') {
      if (input.peek() == '"') {
        elements.add('"' + phrase().parse(input).getOrThrow() + '"');
      } else {
        elements.add(input.readRegex(Pattern.compile("[^,)} ]+")));
      }

      if (input.peek() != '}') {
        input.assertRead(", ");
      }
    }
    input.assertRead("}");

    return elements;
  }
}
