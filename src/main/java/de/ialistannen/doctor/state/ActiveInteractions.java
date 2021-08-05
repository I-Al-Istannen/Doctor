package de.ialistannen.doctor.state;

import de.ialistannen.javadocapi.querying.FuzzyQueryResult;
import java.util.Map;
import java.util.Optional;

public class ActiveInteractions {

  private final Map<Integer, FuzzyQueryResult> choices;
  private final boolean shortDescription;
  private final boolean omitTags;
  private final String userId;

  public ActiveInteractions(Map<Integer, FuzzyQueryResult> choices, boolean shortDescription,
      boolean omitTags, String userId) {
    this.shortDescription = shortDescription;
    this.omitTags = omitTags;
    this.userId = userId;
    this.choices = choices;
  }

  public boolean isShortDescription() {
    return shortDescription;
  }

  public boolean isOmitTags() {
    return omitTags;
  }

  public String getUserId() {
    return userId;
  }

  public Optional<FuzzyQueryResult> getChoice(int target) {
    if (target < 0 || target >= choices.size()) {
      return Optional.empty();
    }
    return Optional.ofNullable(choices.get(target));
  }
}
