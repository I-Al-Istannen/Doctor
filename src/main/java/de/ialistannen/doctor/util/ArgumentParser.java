package de.ialistannen.doctor.util;

import de.ialistannen.doctor.util.Result;

public interface ArgumentParser<T> {

  /**
   * Tries to parse an Argument.
   *
   * @param reader the reader to parse from
   * @return the parsed value or an error
   */
  Result<T, ParseError> parse(StringReader reader);

  default ArgumentParser<T> or(ArgumentParser<T> other) {
    return reader -> {
      int start = reader.getPosition();
      Result<T, ParseError> myResult = parse(reader);
      if (myResult.getValue().isPresent()) {
        return myResult;
      }
      reader.reset(start);

      return other.parse(reader);
    };
  }

  default <R> ArgumentParser<R> andThen(ArgumentParser<R> next) {
    return reader -> {
      Result<T, ParseError> myResult = parse(reader);
      if (myResult.getValue().isEmpty()) {
        @SuppressWarnings("unchecked")
        Result<R, ParseError> r = (Result<R, ParseError>) myResult;
        return r;
      }
      return next.parse(reader);
    };
  }

  default boolean canParse(StringReader reader) {
    int start = reader.getPosition();

    if (parse(reader).getValue().isPresent()) {
      return true;
    }

    reader.reset(start);
    return false;
  }
}
