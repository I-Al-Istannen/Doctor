package de.ialistannen.doctor.commands;

import de.ialistannen.doctor.commands.system.*;
import de.ialistannen.doctor.doc.DocMultipleResultSender;
import de.ialistannen.doctor.doc.DocResultSender;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.state.ActiveInteractions;
import de.ialistannen.doctor.state.MessageDataStore;
import de.ialistannen.doctor.util.parsers.ArgumentParser;
import de.ialistannen.doctor.util.parsers.StringReader;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.querying.FuzzyQueryResult;
import de.ialistannen.javadocapi.querying.QueryApi;
import de.ialistannen.javadocapi.storage.ElementLoader;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import de.ialistannen.javadocapi.util.NameShortener;
import net.dv8tion.jda.api.interactions.commands.Command.Choice;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static de.ialistannen.doctor.util.parsers.ArgumentParsers.*;
import static java.util.stream.Collectors.toList;

public class DocCommand implements Command {

  private final QueryApi<FuzzyQueryResult> queryApi;
  private final ElementLoader loader;
  private final DocResultSender docResultSender;
  private final DocMultipleResultSender docMultipleResultSender;
  private final MessageDataStore dataStore;

  public DocCommand(QueryApi<FuzzyQueryResult> queryApi, ElementLoader loader,
      DocResultSender docResultSender, MessageDataStore dataStore) {
    this.queryApi = queryApi;
    this.loader = loader;
    this.docResultSender = docResultSender;
    this.dataStore = dataStore;

    this.docMultipleResultSender = new DocMultipleResultSender(dataStore);
  }

  @Override
  public ArgumentParser<?> keyword() {
    return literal("doc").or(literal("javadoc"));
  }

  @Override
  public Optional<CommandData> getSlashData() {
    return Optional.of(
        Commands.slash(
                "doc",
                "Fetches Javadoc for methods, classes and fields."
            )
            .addOption(
                OptionType.STRING,
                "query",
                "The query. Example: 'String#contains('",
                true,
                true
            )
            .addOption(
                OptionType.BOOLEAN,
                "long",
                "Display a long version of the javadoc",
                false
            )
            .addOption(
                OptionType.BOOLEAN,
                "omit-tags",
                "If true the Javadoc tags will be omitted",
                false
            )
    );
  }

  @Override
  public void handle(final CommandContext commandContext, final AutoCompleteCommandSource source, final MessageSender sender) {

    String query = source.getOptionValue().strip();

    if (query.length() <= 2) {
      source.getEvent().replyChoices(Collections.emptyList()).queue();
      return;
    }

    handleAutoCompleteQuery(
        query,
        source);
  }

  @Override
  public void handle(CommandContext commandContext, SlashCommandSource source,
      MessageSender sender) {
    String query = remaining(2)
        .parse(new StringReader(source.getOption("query").orElseThrow().getAsString()))
        .getOrThrow();

    handleQuery(
        source,
        query,
        !source.getOption("long").map(OptionMapping::getAsBoolean).orElse(false),
        source.getOption("omit-tags").map(OptionMapping::getAsBoolean).orElse(true),
        sender
    );
  }

  @Override
  public void handle(CommandContext commandContext, ButtonCommandSource source,
      MessageSender sender) {
    Integer choiceId = commandContext.shift(integer());
    String buttonId = commandContext.shift(word());

    handleStoredInteraction(
        source,
        choiceId,
        buttonId,
        () -> source.getEvent()
            .reply("\uD83D\uDE94 Are you trying to steal those buttons? \uD83D\uDE94")
            .setEphemeral(true)
            .queue(),
        sender
    );
  }

  private void handleStoredInteraction(CommandSource source, Integer id, String buttonId,
      Runnable onPermissionDenied, MessageSender sender) {
    Optional<ActiveInteractions> buttons = this.dataStore.getActiveInteraction(buttonId);

    if (buttons.isEmpty()) {
      sender
          .editOrReply(
              "Couldn't find any stored choices for that message <:feelsBadMan:626724180284538890>"
          )
          .queue();
      return;
    }

    if (!Objects.equals(source.getAuthorId(), buttons.get().getUserId())) {
      onPermissionDenied.run();
      return;
    }

    this.dataStore.removeActiveInteraction(buttonId);

    Optional<FuzzyQueryResult> choice = buttons.get().getChoice(id);

    if (choice.isEmpty()) {
      sender
          .editOrReply("Somehow you provided an invalid choice <:feelsBadMan:626724180284538890>")
          .queue();
      return;
    }

    handleQuery(
        source,
        choice.get().getQualifiedName().asString(),
        buttons.get().isShortDescription(),
        buttons.get().isOmitTags(),
        sender,
        () -> docResultSender.replyForReflectiveProxy(
            sender,
            new QualifiedName(choice.get().getQualifiedName().asString()),
            ((BaseUrlElementLoader) choice.get().getSourceLoader()).getLinkResolveStrategy()
        )
    );
  }

  @Override
  public void handle(CommandContext commandContext, SelectionMenuCommandSource source,
      MessageSender sender) {
    String buttonId = commandContext.shift(word());

    handleStoredInteraction(
        source,
        Integer.parseInt(source.getOption()),
        buttonId,
        () -> source.getEvent()
            .reply("\uD83D\uDE94 Checkbox theft is a crime! \uD83D\uDE94")
            .setEphemeral(true)
            .queue(),
        sender
    );
  }

  @Override
  public void handle(CommandContext commandContext, CommandSource source, MessageSender sender) {
    Optional<String> isLong = commandContext.tryShift(literal("long"));

    handleQuery(source, commandContext.shift(remaining(2)), isLong.isEmpty(), true, sender);
  }

  private void handleQuery(CommandSource source, String query, boolean shortDescription,
      boolean omitTags, MessageSender sender) {
    handleQuery(
        source,
        query,
        shortDescription,
        omitTags,
        sender,
        () -> sender
            .editOrReply(
                "I couldn't find any result for '" + query + "' <:feelsBadMan:626724180284538890>"
            )
            .queue()
    );
  }

  private void handleQuery(CommandSource source, String query, boolean shortDescription,
      boolean omitTags, MessageSender sender, Runnable onNoResult) {
    Instant start = Instant.now();

    List<FuzzyQueryResult> results = queryApi.query(loader, query.strip())
        .stream()
        .distinct()
        .collect(toList());

    Instant end = Instant.now();

    if (results.size() == 1) {
      replyForResult(
          source,
          results.get(0),
          shortDescription,
          omitTags,
          sender,
          Duration.between(start, end)
      );
      return;
    }

    if (results.stream().filter(FuzzyQueryResult::isExact).count() == 1) {
      FuzzyQueryResult result = results.stream()
          .filter(FuzzyQueryResult::isExact)
          .findFirst()
          .orElseThrow();

      replyForResult(
          source,
          result,
          shortDescription,
          omitTags,
          sender,
          Duration.between(start, end)
      );
      return;
    }
    if (results.stream().filter(FuzzyQueryResult::isCaseSensitiveExact).count() == 1) {
      FuzzyQueryResult result = results.stream()
          .filter(FuzzyQueryResult::isCaseSensitiveExact)
          .findFirst()
          .orElseThrow();

      replyForResult(
          source,
          result,
          shortDescription,
          omitTags,
          sender,
          Duration.between(start, end)
      );
      return;
    }

    if (results.isEmpty()) {
      onNoResult.run();
      return;
    }
    docMultipleResultSender
        .replyMultipleResults(source, sender, shortDescription, omitTags, new HashSet<>(results));
  }

  private void replyForResult(CommandSource source, FuzzyQueryResult result, boolean shortDesc,
      boolean omitTags, MessageSender sender, Duration queryDuration) {
    Collection<LoadResult<JavadocElement>> elements = loader.findByQualifiedName(
        result.getQualifiedName()
    );

    if (elements.size() == 1) {
      LoadResult<JavadocElement> loadResult = elements.iterator().next();
      docResultSender.replyWithResult(
          source,
          sender,
          loadResult,
          shortDesc,
          omitTags,
          queryDuration,
          ((BaseUrlElementLoader) loadResult.getLoader()).getLinkResolveStrategy()
      );
    } else {
      sender
          .reply(
              "I found multiple elements for this qualified name <:feelsBadMan:626724180284538890>"
          )
          .queue();
    }
  }

  private void handleAutoCompleteQuery(final String query, final AutoCompleteCommandSource source) {
    List<QualifiedName> results = queryApi.autocomplete(loader, query)
        .stream()
        .distinct()
        .limit(25)
        .map(DocCommand::toQualifiedName)
        .collect(toList());

    Map<String, String> shortened = new NameShortener().shortenMatches(Set.copyOf(results));

    System.out.println(shortened);

    source.getEvent().replyChoices(
        results.stream()
            .map(it -> new Choice(
                shortenIfNeeded(shortened.get(it.asString())),
                shortenIfNeeded(it.asString())
            ))
            .collect(toList())).queue();
  }

  private static String shortenIfNeeded(String input) {
    return input.length() > 100 ? input.substring(0, 100) : input;
  }

  private static QualifiedName toQualifiedName(String asString) {
    if (asString.contains("/")) {
      String[] parts = asString.split("/");
      return new QualifiedName(parts[1], parts[0]);
    }
    return new QualifiedName(asString);
  }
}
