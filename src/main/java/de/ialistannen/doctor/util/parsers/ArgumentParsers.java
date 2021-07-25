package de.ialistannen.doctor.util.parsers;

import de.ialistannen.doctor.util.Result;
import java.util.Set;
import java.util.regex.Pattern;

public class ArgumentParsers {

  private static Set<Character> QUOTE_CHARS = Set.of('"', '\'');


  public static ArgumentParser<String> literal(String text) {
    return reader -> {
      if (reader.peek(text.length()).equals(text)) {
        return Result.ok(reader.readChars(text.length()));
      }
      return Result.error(new ParseError("Expected <" + text + ">", reader));
    };
  }

  public static ArgumentParser<String> remaining(int minLength) {
    return reader -> {
      String remaining = reader.readRemaining();
      if (remaining.length() >= minLength) {
        return Result.ok(remaining);
      }
      return Result.error(new ParseError(
          "Expected at least " + minLength + " characters!", reader
      ));
    };
  }

  public static ArgumentParser<String> whitespace() {
    return reader -> {
      String spaces = reader.readWhile(Character::isWhitespace);
      if (spaces.length() > 0) {
        return Result.ok(spaces);
      }
      return Result.error(new ParseError("Expected some whitespace", reader));
    };
  }

  /**
   * A parser that reads a single word (i.e. until a space character).
   *
   * @return a parser that reads a single word
   */
  public static ArgumentParser<String> word() {
    final Pattern pattern = Pattern.compile("[\\S]*");

    return input -> {
      String readString = input.readRegex(pattern);

      if (readString.length() == 0) {
        return Result.error(new ParseError("Expected a word", input));
      }

      return Result.ok(readString);
    };
  }

  /**
   * A parser that reads a single word (i.e. until a space character).
   *
   * @return a parser that reads a single word
   */
  public static ArgumentParser<Integer> integer() {
    final Pattern pattern = Pattern.compile("\\d+");

    return input -> {
      String readString = input.readRegex(pattern);

      if (readString.length() == 0) {
        return Result.error(new ParseError("Expected an integer", input));
      }

      try {
        return Result.ok(Integer.parseInt(readString));
      } catch (NumberFormatException e) {
        return Result.error(new ParseError("Couldn't parse number: " + e.getMessage(), input));
      }
    };
  }


  /**
   * A parser that reads a single word or a quoted phrase.
   *
   * @return a parser that reads a single word or a quoted phrase
   */
  public static ArgumentParser<String> phrase() {
    return input -> {
      if (!QUOTE_CHARS.contains(input.peek())) {
        return word().parse(input);
      }

      char quoteChar = input.readChar();

      StringBuilder readString = new StringBuilder();

      boolean escaped = false;
      while (input.canRead()) {
        char read = input.readChar();

        if (escaped) {
          escaped = false;
          readString.append(read);
          continue;
        }

        if (read == '\\') {
          escaped = true;
        } else if (read == quoteChar) {
          break;
        } else {
          readString.append(read);
        }
      }

      if (readString.length() == 0) {
        return Result.error(new ParseError("Expected some (optionally quoted) phrase", input));
      }
      return Result.ok(readString.toString());
    };
  }

  /**
   * A parser that reads a quoted phrase and supports nesting. it can read until matching "{" and
   * "}", for example.
   *
   * @return a parser that reads a (nested) quoted phrase
   */
  public static ArgumentParser<String> nestedQuote(char quoteOpen, char quoteEnd) {
    return input -> {
      input.assertRead(quoteOpen);
      int nestingLevel = 1;

      StringBuilder readString = new StringBuilder();

      while (input.canRead()) {
        char read = input.readChar();

        if (read == quoteOpen) {
          nestingLevel++;
        } else if (read == quoteEnd) {
          nestingLevel--;
          if (nestingLevel == 0) {
            break;
          }
        }

        readString.append(read);
      }

      if (nestingLevel != 0) {
        return Result.error(
            new ParseError("Expected a closing " + quoteEnd, input)
        );
      }

      if (readString.length() == 0) {
        return Result.error(
            new ParseError("Expected some phrase quoted with " + quoteOpen + quoteEnd, input)
        );
      }
      return Result.ok(readString.toString());
    };
  }
}
