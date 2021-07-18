package de.ialistannen.docfork.util.parsers;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility reader for a string.
 */
public class StringReader {

  private final String underlying;
  private int position;

  /**
   * Creates a new string reader.
   *
   * @param underlying the underlying string
   */
  public StringReader(String underlying) {
    this(underlying, 0);
  }

  /**
   * Creates a new string reader.
   *
   * @param underlying the underlying string
   * @param position the initial position
   */
  public StringReader(String underlying, int position) {
    this.underlying = underlying;
    this.position = position;
  }

  /**
   * Returns true if there is more to read.
   *
   * @return true if there is more to read
   */
  public boolean canRead() {
    return position < underlying.length();
  }

  /**
   * Returns true if there is enough input to read {@code amount} chars.
   *
   * @param amount the amount of chars to read
   * @return true if there is more to read
   */
  public boolean canRead(int amount) {
    return position + amount <= underlying.length();
  }

  /**
   * Peeks at a single char.
   *
   * @return the char or 0 if EOF is reached
   */
  public char peek() {
    if (position >= underlying.length()) {
      return 0;
    }
    return underlying.charAt(position);
  }

  public String peekWhile(Predicate<Character> predicate) {
    int start = getPosition();
    String result = readWhile(predicate);
    reset(start);

    return result;
  }

  public String assertRead(String string) {
    if (!readChars(string.length()).equals(string)) {
      throw new ParseError("Expected '" + string + "'", this);
    }
    return string;
  }

  public char assertRead(char character) {
    if (readChar() != character) {
      throw new ParseError("Expected '" + character + "'", this);
    }
    return character;
  }

  /**
   * Returns the next {@code amount} chars or less, if the input ends before it
   *
   * @param amount the amount of chars to peek at
   * @return the read text
   */
  public String peek(int amount) {
    return underlying.substring(position, Math.min(underlying.length(), position + amount));
  }

  /**
   * Reads a single char.
   *
   * @return the read char
   */
  public char readChar() {
    return underlying.charAt(position++);
  }

  /**
   * Reads the given amount of characters.
   *
   * @param count the amount of characters to read
   * @return the read string
   */
  public String readChars(int count) {
    int oldPos = this.position;
    position = this.position + count;

    return underlying.substring(oldPos, position);
  }

  /**
   * Reads for as long as {@link #canRead()} is true and the predicate matches.
   *
   * Will place the cursor at the first char that did not match.
   *
   * @param predicate the predicate
   * @return the read string
   */
  public String readWhile(Predicate<Character> predicate) {
    int start = position;
    while (canRead() && predicate.test(peek())) {
      readChar();
    }

    return underlying.substring(start, position);
  }

  /**
   * Reads the whole string matching the regex.
   *
   * @param pattern the pattern to use
   * @return the read string or an empty String, if the regex didn't match
   */
  public String readRegex(Pattern pattern) {
    Matcher matcher = pattern.matcher(getUnderlying());
    boolean resultFound = matcher.find(position);

    if (!resultFound) {
      return "";
    }

    if (matcher.start() != position) {
      // The match must start at the current position or it does not count
      return "";
    }

    int start = position;
    position = matcher.end();

    return underlying.substring(start, position);
  }

  /**
   * Reads the remaining string.
   *
   * @return the reamining string
   */
  public String readRemaining() {
    return readWhile(it -> true);
  }

  /**
   * Returns the underlying string.
   *
   * @return the underlying string
   */
  public String getUnderlying() {
    return underlying;
  }

  /**
   * Returns the current position of this reader.
   *
   * @return the current position of this reader
   */
  public int getPosition() {
    return position;
  }

  /**
   * Sets the position of the reader.
   *
   * @param position the new position
   */
  public void reset(int position) {
    this.position = position;
  }

  /**
   * Returns a copy of this reader which is at the same position.
   *
   * @return a copy of this reader   *
   */
  public StringReader copy() {
    return new StringReader(underlying, position);
  }

  public int remaining() {
    return underlying.length() - position - 1;
  }
}
