package de.ialistannen.doctor;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DocTorConfig(
    @JsonProperty("token") String token,
    @JsonProperty("author_id") String authorId,
    @JsonProperty("sources") List<SourceConfig> sources
) {

  public record SourceConfig(
      @JsonProperty("database") String database,
      @JsonProperty("external_javadoc") List<String> externalJavadoc,
      @JsonProperty("javadoc_url") String javadocUrl
  ) {

  }
}
