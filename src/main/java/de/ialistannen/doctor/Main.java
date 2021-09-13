package de.ialistannen.doctor;

import de.ialistannen.doctor.commands.DocCommand;
import de.ialistannen.doctor.commands.EditReplyCommand;
import de.ialistannen.doctor.commands.UpdateSlashesCommand;
import de.ialistannen.doctor.commands.system.Executor;
import de.ialistannen.doctor.doc.DocResultSender;
import de.ialistannen.doctor.state.MessageDataStore;
import de.ialistannen.javadocapi.indexing.OnlineJavadocIndexer;
import de.ialistannen.javadocapi.querying.FuzzyElementQuery;
import de.ialistannen.javadocapi.rendering.Java11PlusLinkResolver;
import de.ialistannen.javadocapi.storage.AggregatedElementLoader;
import de.ialistannen.javadocapi.storage.ConfiguredGson;
import de.ialistannen.javadocapi.storage.ElementLoader;
import de.ialistannen.javadocapi.storage.SqliteStorage;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import de.ialistannen.javadocapi.util.ExternalJavadocReference;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;

public class Main {

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: <config file>");
      System.exit(1);
      return;
    }

    Config config = ConfiguredGson.create()
        .fromJson(Files.readString(Path.of(args[0])), Config.class);

    List<ElementLoader> storages = new ArrayList<>();
    for (Database database : config.getDatabases()) {
      storages.add(new BaseUrlElementLoader(
          new SqliteStorage(
              ConfiguredGson.create(),
              Path.of(database.getPath())
          ),
          database.getBaseUrl(),
          indexExternalJavadoc(database.getExternalJavadocs()),
          new Java11PlusLinkResolver()
      ));
    }

    MessageDataStore messageDataStore = new MessageDataStore();
    DocResultSender resultSender = new DocResultSender(messageDataStore);
    JDA jda = JDABuilder.createDefault(config.getToken())
        .addEventListeners(new Executor(List.of(
            new DocCommand(
                new FuzzyElementQuery(),
                new AggregatedElementLoader(storages),
                resultSender,
                messageDataStore
            ),
            new UpdateSlashesCommand(),
            new EditReplyCommand(messageDataStore, resultSender)
        )))
        .build()
        .setRequiredScopes("applications.commands", "bot")
        .awaitReady();

    System.out.println(jda.getInviteUrl(Permission.MESSAGE_WRITE));
  }

  private static List<ExternalJavadocReference> indexExternalJavadoc(List<String> urls)
      throws IOException, InterruptedException {
    if (urls == null) {
      return List.of();
    }

    OnlineJavadocIndexer indexer = new OnlineJavadocIndexer(HttpClient.newHttpClient());
    List<ExternalJavadocReference> references = new ArrayList<>();

    for (String url : urls) {
      ExternalJavadocReference reference = indexer.fetchPackages(url);
      references.add(reference);
    }

    return references;
  }

  private static class Config {

    private String token;
    private List<Database> databases;

    public Config(String token, List<Database> databases) {
      this.token = token;
      this.databases = databases;
    }

    public String getToken() {
      return token;
    }

    public List<Database> getDatabases() {
      return databases;
    }
  }

  private static class Database {

    private String path;
    private String baseUrl;
    private List<String> externalJavadocs;

    public Database(String path, String baseUrl, List<String> externalJavadocs) {
      this.path = path;
      this.baseUrl = baseUrl;
      this.externalJavadocs = externalJavadocs;
    }

    public String getPath() {
      return path;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public List<String> getExternalJavadocs() {
      return externalJavadocs;
    }
  }
}
