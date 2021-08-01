package de.ialistannen.doctor.reactions;

import java.util.Arrays;
import java.util.Optional;

public enum AvailableReaction {
  COLLAPSE("\u23EB"),
  EXPAND("\u23EC"),
  REMOVE_TAGS("\u2702\uFE0F"),
  DELETE("\uD83D\uDDD1️");

  private final String unicode;

  AvailableReaction(String unicode) {
    this.unicode = unicode;
  }

  public String getUnicode() {
    return unicode;
  }

  public static Optional<AvailableReaction> fromUnicode(String unicode) {
    return Arrays.stream(values())
        .filter(it -> it.getUnicode().equals(unicode))
        .findFirst();
  }
}
