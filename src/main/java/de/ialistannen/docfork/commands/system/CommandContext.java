package de.ialistannen.docfork.commands.system;

import de.ialistannen.docfork.util.parsers.ArgumentParser;
import de.ialistannen.docfork.util.parsers.StringReader;
import java.util.Optional;

public class CommandContext {

  private final StringReader rawText;

  public CommandContext(StringReader rawText) {
    this.rawText = rawText;
  }

  public <T> T shift(ArgumentParser<T> parser) {
    return parser.parse(rawText).getOrThrow();
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
