package de.ialistannen.docfork.commands;

import static de.ialistannen.docfork.util.parsers.ArgumentParsers.literal;
import static de.ialistannen.docfork.util.parsers.ArgumentParsers.remaining;

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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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

public class DocCommand implements Command {

  private final QueryApi<FuzzyQueryResult> queryApi;
  private final ElementLoader loader;
  private final MarkdownCommentRenderer renderer;

  public DocCommand(QueryApi<FuzzyQueryResult> queryApi, ElementLoader loader) {
    this.queryApi = queryApi;
    this.loader = loader;

    this.renderer = new MarkdownCommentRenderer(new Java11PlusLinkResolver());
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
                "short",
                "Only display a short summary of the javadoc",
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
        source.getOption("long").map(OptionMapping::getAsBoolean).orElse(true),
        source.getOption("omit-tags").map(OptionMapping::getAsBoolean).orElse(false)
    );
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
      DocEmbedBuilder docEmbedBuilder = new DocEmbedBuilder(
          renderer,
          elements.iterator().next().getResult(),
          "https://docs.oracle.com/en/java/javase/16/docs/api/"
      )
          .addColor()
          .addIcon()
          .addShortDescription()
          .addFooter(elements.iterator().next().getLoader().toString());

      if (!shortDesc) {
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

  private void replyMultipleResults(CommandSource source, boolean shortDescritpion,
      List<FuzzyQueryResult> results) {
    if (results.size() < Type.BUTTON.getMaxPerRow() * 5) {
      AtomicInteger counter = new AtomicInteger();
      List<ActionRow> rows = results.stream()
          .map(FuzzyQueryResult::getQualifiedName)
          .map(QualifiedName::asString)
          .distinct()
          .sorted(Comparator.naturalOrder())
          .map(it -> Button.of(
              ButtonStyle.SECONDARY,
              "!javadoc " + (shortDescritpion ? "short " : "") + it,
              it
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
}
