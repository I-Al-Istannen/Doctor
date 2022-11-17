package de.ialistannen.doctor.util;

public class ParseError extends RuntimeException {

  private final String input;
  private final int position;
  private final String message;

  public ParseError(String message, StringReader reader) {
    super(message);
    this.message = message;
    this.input = reader.getUnderlying();
    this.position = reader.getPosition();
  }

  @Override
  public String toString() {
    String lineWithError = input;
    int currentOffset = 0;
    int positionInErrorLine = 0;
    for (String line : input.lines().toList()) {
      if (position >= currentOffset && position <= line.length() + currentOffset) {
        lineWithError = line;
        positionInErrorLine = position - currentOffset;
        break;
      }
    }

    if (lineWithError.length() > 120) {
      int start = Math.max(0, positionInErrorLine - 120 / 2);
      int end = Math.min(lineWithError.length(), positionInErrorLine + 120 / 2);
      lineWithError = lineWithError.substring(start, end);
      positionInErrorLine = positionInErrorLine - start;
    }

    String pointerLine = " ".repeat(positionInErrorLine) + "^";
    String errorPadding = " ".repeat(Math.max(0, positionInErrorLine - message.length() / 2));
    String centeredError = errorPadding + message;

    return lineWithError + "\n" + pointerLine + "\n" + centeredError;
  }
}
