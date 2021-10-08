package de.ialistannen.doctor.util;

import de.ialistannen.doctor.util.parsers.StringReader;
import java.util.regex.Pattern;

public class StringTrimUtils {

  /**
   * Trims the input markdown to approximately a given length. Might be longer as it tries to finish
   * links and code blocks.
   *
   * @param input the input string
   * @param maxLength the maximum input length to try and fulfill
   * @param maxNewlines the maximum amount of newlines to try and fulfill
   * @return the trimmed markdown
   */
  public static String trimMarkdown(String input, int maxLength, int maxNewlines) {
    StringBuilder result = new StringBuilder();

    StringReader inputReader = new StringReader(input);
    int encounteredNewlines = 0;
    boolean inCodeBlock = false;

    while (inputReader.canRead()) {
      if (!inCodeBlock && (encounteredNewlines >= maxNewlines || result.length() >= maxLength)) {
        break;
      }

      char next = inputReader.readChar();
      result.append(next);

      if (next == '`' && inputReader.canRead(2) && inputReader.peek(2).equals("``")) {
        inCodeBlock = !inCodeBlock;
        result.append(inputReader.readChars(2));
        continue;
      }

      if (next == '[') {
        result.append(inputReader.readRegex(Pattern.compile(".+?]\\(.+?\\)")));
      }

      if (next == '\n') {
        encounteredNewlines++;
      }
    }

    String text = result.toString();
    result = new StringBuilder(text.strip());

    if (inputReader.canRead()) {
      int skippedLines = (int) inputReader.readRemaining().chars().filter(c -> c == '\n').count();
      result.append("\n\n*Skipped ");
      if (skippedLines > 0) {
        result.append("**");
        result.append(skippedLines);
        result.append("** line");
        if (skippedLines > 1) {
          result.append("s");
        }
      } else {
        result.append("**the rest of the line**");
      }
      result.append(". Click `Expand` if you are intrigued.*");
    }

    return result.toString();
  }

}
