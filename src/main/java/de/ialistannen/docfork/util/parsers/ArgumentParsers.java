package de.ialistannen.docfork.util.parsers;

import de.ialistannen.docfork.util.Result;

public class ArgumentParsers {

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
}
