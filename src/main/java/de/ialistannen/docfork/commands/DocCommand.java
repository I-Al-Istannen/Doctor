package de.ialistannen.docfork.commands;

import static de.ialistannen.docfork.util.parsers.ArgumentParsers.integer;
import static de.ialistannen.docfork.util.parsers.ArgumentParsers.literal;
import static de.ialistannen.docfork.util.parsers.ArgumentParsers.remaining;
import static de.ialistannen.docfork.util.parsers.ArgumentParsers.word;

import de.ialistannen.docfork.commands.system.ButtonCommandSource;
import de.ialistannen.docfork.commands.system.Command;
import de.ialistannen.docfork.commands.system.CommandContext;
import de.ialistannen.docfork.commands.system.CommandSource;
import de.ialistannen.docfork.commands.system.SlashCommandSource;
import de.ialistannen.docfork.doc.DocEmbedBuilder;
import de.ialistannen.docfork.util.parsers.ArgumentParser;
import de.ialistannen.docfork.util.parsers.StringReader;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.querying.FuzzyQueryResult;
import de.ialistannen.javadocapi.querying.QueryApi;
import de.ialistannen.javadocapi.rendering.Java11PlusLinkResolver;
import de.ialistannen.javadocapi.rendering.MarkdownCommentRenderer;
import de.ialistannen.javadocapi.storage.ElementLoader;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import de.ialistannen.javadocapi.util.NameShortener;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component.Type;
import org.apache.commons.lang3.StringUtils;

public class DocCommand implements Command {

  private final QueryApi<FuzzyQueryResult> queryApi;
  private final ElementLoader loader;
  private final MarkdownCommentRenderer renderer;
  private final Java11PlusLinkResolver linkResolveStrategy;
  private final Map<String, ActiveButtons> activeButtons;

  public DocCommand(QueryApi<FuzzyQueryResult> queryApi, ElementLoader loader) {
    this.queryApi = queryApi;
    this.loader = loader;

    this.linkResolveStrategy = new Java11PlusLinkResolver();
    this.renderer = new MarkdownCommentRenderer(linkResolveStrategy);
    this.activeButtons = new LinkedHashMap<>() {
      @Override
      protected boolean removeEldestEntry(Entry<String, ActiveButtons> eldest) {
        return size() > 60;
      }
    };
  }

  @Override
  public ArgumentParser<?> keyword() {
    return literal("doc").or(literal("javadoc"));
  }

  @Override
  public Optional<CommandData> getSlashData() {
    return Optional.of(
        new CommandData(
            "doc",
            "Fetches Javadoc for methods, classes and fields."
        )
            .addOption(
                OptionType.STRING,
                "query",
                "The query. Example: 'String#contains('",
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
  public void handle(CommandContext commandContext, SlashCommandSource source) {
    String query = remaining(2)
        .parse(new StringReader(source.getOption("query").orElseThrow().getAsString()))
        .getOrThrow();

    handleQuery(
        source,
        query,
        !source.getOption("long").map(OptionMapping::getAsBoolean).orElse(false),
        source.getOption("omit-tags").map(OptionMapping::getAsBoolean).orElse(false)
    );
  }

  @Override
  public void handle(CommandContext commandContext, ButtonCommandSource source) {
    Integer id = commandContext.shift(integer());
    String buttonId = commandContext.shift(word());
    ActiveButtons buttons = this.activeButtons.get(buttonId);

    if (buttons == null) {
      source.editOrReply(
          "Couldn't find any stored choices for that message <:feelsBadMan:626724180284538890>"
      ).queue();
      return;
    }

    if (!Objects.equals(source.getAuthorId(), buttons.getUserId())) {
      source.getEvent()
          .reply("\uD83D\uDE94 Are you trying to steal those buttons? \uD83D\uDE94")
          .setEphemeral(true)
          .queue();
      return;
    }

    this.activeButtons.remove(buttonId);

    Optional<String> choice = buttons.getChoice(id);

    if (choice.isEmpty()) {
      source.editOrReply(
          "Somehow you provided an invalid choice <:feelsBadMan:626724180284538890>"
      ).queue();
      return;
    }

    handleQuery(source, choice.get(), buttons.isShortDescription(), false);
  }

  @Override
  public void handle(CommandContext commandContext, CommandSource source) {
    Optional<String> isLong = commandContext.tryShift(literal("long"));

    handleQuery(source, commandContext.shift(remaining(2)), isLong.isEmpty(), false);
  }

  private void handleQuery(CommandSource source, String query, boolean shortDescription,
      boolean omitTags) {
    List<FuzzyQueryResult> results = queryApi.query(loader, query.strip());

    if (results.size() == 1) {
      replyForResult(source, results.get(0), shortDescription, omitTags);
      return;
    }

    if (results.stream().filter(FuzzyQueryResult::isExact).count() == 1) {
      FuzzyQueryResult result = results.stream()
          .filter(FuzzyQueryResult::isExact)
          .findFirst()
          .get();

      replyForResult(source, result, shortDescription, omitTags);
      return;
    }

    if (results.size() == 0) {
      source.editOrReply(
          "I couldn't find any result for '" + query + "' <:feelsBadMan:626724180284538890>"
      ).queue();
      return;
    }
    replyMultipleResults(source, shortDescription, results);
  }

  private void replyForResult(CommandSource source, FuzzyQueryResult result, boolean shortDesc,
      boolean omitTags) {
    Collection<LoadResult<JavadocElement>> elements = loader.findByQualifiedName(
        result.getQualifiedName()
    );

    if (elements.size() == 1) {
      LoadResult<JavadocElement> loadResult = elements.iterator().next();
      DocEmbedBuilder docEmbedBuilder = new DocEmbedBuilder(
          renderer,
          loadResult.getResult(),
          ((BaseUrlElementLoader) loadResult.getLoader()).getBaseUrl()
      )
          .addColor()
          .addIcon(linkResolveStrategy)
          .addDeclaration()
          .addFooter(loadResult.getLoader().toString());

      if (shortDesc) {
        docEmbedBuilder.addShortDescription();
      } else {
        docEmbedBuilder.addLongDescription();
      }
      if (!omitTags) {
        docEmbedBuilder.addTags();
      }

      source.editOrReply(new MessageBuilder(docEmbedBuilder.build()).build()).queue();
    } else {
      source.reply(
          "I found multiple elements for this qualified name <:feelsBadMan:626724180284538890>"
      ).queue();
    }
  }

  private void replyMultipleResults(CommandSource source, boolean shortDescription,
      List<FuzzyQueryResult> results) {
    if (results.size() < Type.BUTTON.getMaxPerRow() * 5) {
      AtomicInteger counter = new AtomicInteger();
      Map<String, Integer> resultIdMapping = results.stream()
          .map(FuzzyQueryResult::getQualifiedName)
          .map(QualifiedName::asString)
          .collect(Collectors.toMap(it -> it, it -> counter.getAndIncrement(), (a, b) -> a));

      counter.set(0);

      List<ActionRow> rows = new NameShortener().shortenMatches(
          results.stream()
              .map(FuzzyQueryResult::getQualifiedName)
              .collect(Collectors.toSet())
      )
          .entrySet()
          .stream()
          .sorted(Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
          .map(it -> Button.of(
              ButtonStyle.SECONDARY,
              "!javadoc " + resultIdMapping.get(it.getKey()) + " " + source.getId(),
              StringUtils.abbreviate(it.getValue(), 80)
          ))
          .collect(Collectors.groupingBy(
              it -> counter.getAndIncrement() / Type.BUTTON.getMaxPerRow(),
              Collectors.toList()
          ))
          .values()
          .stream()
          .map(ActionRow::of)
          .collect(Collectors.toList());

      source.reply(
          new MessageBuilder("I found multiple types:  \n")
              .setActionRows(rows)
              .build()
      )
          .queue();
      activeButtons.put(
          source.getId(),
          new ActiveButtons(resultIdMapping, shortDescription, source.getAuthorId())
      );
      return;
    }

    String possibleOptions = results.stream()
        .map(FuzzyQueryResult::getQualifiedName)
        .map(QualifiedName::asString)
        .map(it -> "* `" + it + "`")
        .limit(10)
        .collect(Collectors.joining("\n"));

    source.reply("I found at least the following types:  \n\n" + possibleOptions)
        .queue();
  }

  private static class ActiveButtons {

    private final Map<Integer, String> choices;
    private final boolean shortDescription;
    private final String userId;

    private ActiveButtons(Map<String, Integer> choices, boolean shortDescription, String userId) {
      this.shortDescription = shortDescription;
      this.userId = userId;
      this.choices = new HashMap<>();

      for (Entry<String, Integer> entry : choices.entrySet()) {
        this.choices.put(entry.getValue(), entry.getKey());
      }
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
}
