package de.ialistannen.doctor.state;

import java.util.Map;
import java.util.Optional;

public class ActiveInteractions {

  private final Map<Integer, String> choices;
  private final boolean shortDescription;
  private final String userId;

  public ActiveInteractions(Map<Integer, String> choices, boolean shortDescription, String userId) {
    this.shortDescription = shortDescription;
    this.userId = userId;
    this.choices = choices;
  }

  public boolean isShortDescription() {
    return shortDescription;
  }

  public String getUserId() {
    return userId;
  }

  public Optional<String> getChoice(int target) {
    if (target < 0 || target >= choices.size()) {
      return Optional.empty();
    }
    return Optional.ofNullable(choices.get(target));
  }
}
