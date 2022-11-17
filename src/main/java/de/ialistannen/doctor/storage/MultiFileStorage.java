package de.ialistannen.doctor.storage;

import de.ialistannen.doctor.DocTorConfig.SourceConfig;
import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.storage.SQLiteStorage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class MultiFileStorage {

  private final Map<SourceConfig, SQLiteStorage> databases;

  public MultiFileStorage(Map<SourceConfig, SQLiteStorage> databases) {
    this.databases = databases;
  }

  public Optional<FetchResult> get(String qualifiedName) throws SQLException, IOException {
    for (var database : databases.entrySet()) {
      Optional<SQLiteStorage.FetchResult> element = database.getValue().get(qualifiedName);
      if (element.isPresent()) {
        return element.map(it -> FetchResult.fromUnderlying(it, database.getKey()));
      }
    }
    return Optional.empty();
  }

  public record FetchResult(
      DocumentedElementReference reference,
      DocumentedElement element,
      SourceConfig config
  ) {

    private static FetchResult fromUnderlying(
        SQLiteStorage.FetchResult result,
        SourceConfig config
    ) {
      return new FetchResult(result.reference(), result.element(), config);
    }
  }
}
