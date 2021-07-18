package de.ialistannen.docfork.commands.system;

import static de.ialistannen.docfork.util.parsers.ArgumentParsers.whitespace;

import de.ialistannen.docfork.util.parsers.ArgumentParser;
import de.ialistannen.docfork.util.parsers.StringReader;
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
