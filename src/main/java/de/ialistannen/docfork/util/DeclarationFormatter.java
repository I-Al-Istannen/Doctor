package de.ialistannen.docfork.util;

import static de.ialistannen.docfork.util.parsers.ArgumentParsers.nestedQuote;
import static de.ialistannen.docfork.util.parsers.ArgumentParsers.phrase;

import de.ialistannen.docfork.util.parsers.StringReader;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import de.ialistannen.javadocapi.model.types.JavadocMethod;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
      if (input.remaining() + 2 > maxLength) {
        while (input.remaining() > 0) {
          String type = input.readWhile(c -> c == '.' || Character.isJavaIdentifierPart(c));
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
    }

    if (result.length() > 0) {
      result.append("\n");
    }

    return result.toString();
  }

  private String formatAnnotationParameters(int currentSize, StringReader input) {
    StringBuilder result = new StringBuilder();
    result.append(input.assertRead('('));

    List<String> parameters = new ArrayList<>();

    while (input.peek() != ')') {
      parameters.add(formatAnnotationParameter(input));
      if (input.peek() == ',') {
        input.assertRead(", ");
      }
    }

    String joinedParameters = String.join(", ", parameters);
    if (joinedParameters.length() + 2 + currentSize > maxLength) {
      joinedParameters = "\n" + String.join(",\n", parameters).indent(2);
    }

    result.append(joinedParameters);

    result.append(input.assertRead(')'));

    return result.toString();
  }

  private String formatAnnotationParameter(StringReader input) {
    StringBuilder result = new StringBuilder();
    String name = input.readWhile(Character::isJavaIdentifierPart);

    result.append(name);
    input.readRegex(Pattern.compile("\\s+=\\s+"));
    result.append(" = ");

    if (input.peek() == '"') {
      result.append('"');
      result.append(phrase().parse(input).getOrThrow());
      result.append('"');
    } else {
      result.append(phrase().parse(input).getOrThrow());
    }

    return result.toString();
  }
}
