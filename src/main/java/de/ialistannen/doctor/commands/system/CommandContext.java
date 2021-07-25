package de.ialistannen.doctor.commands.system;

import de.ialistannen.doctor.util.parsers.ArgumentParser;
import de.ialistannen.doctor.util.parsers.StringReader;
import java.util.Optional;

public class CommandContext {

  private final StringReader rawText;

  public CommandContext(StringReader rawText) {
    this.rawText = rawText;
  }

  public <T> T shift(ArgumentParser<T> parser) {
    final T result = parser.parse(rawText).getOrThrow();
    rawText.readWhile(Character::isWhitespace);
    return result;
  }

  public <T> Optional<T> tryShift(ArgumentParser<T> parser) {
    int start = rawText.getPosition();

    Optional<T> result = parser.parse(rawText).getValue();
    if (result.isPresent()) {
      return result;
    }
    rawText.reset(start);

    return result;
  }
}
