package de.ialistannen.doctor.commands;

import static de.ialistannen.doctor.util.StreamUtils.partition;
import static de.ialistannen.doctor.util.parsers.ArgumentParsers.integer;
import static de.ialistannen.doctor.util.parsers.ArgumentParsers.literal;
import static de.ialistannen.doctor.util.parsers.ArgumentParsers.remaining;
import static de.ialistannen.doctor.util.parsers.ArgumentParsers.word;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import de.ialistannen.doctor.commands.system.ButtonCommandSource;
import de.ialistannen.doctor.commands.system.Command;
import de.ialistannen.doctor.commands.system.CommandContext;
import de.ialistannen.doctor.commands.system.CommandSource;
import de.ialistannen.doctor.commands.system.SelectionMenuCommandSource;
import de.ialistannen.doctor.commands.system.SlashCommandSource;
import de.ialistannen.doctor.doc.DocEmbedBuilder;
import de.ialistannen.doctor.util.StreamUtils;
import de.ialistannen.doctor.util.parsers.ArgumentParser;
import de.ialistannen.doctor.util.parsers.StringReader;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.querying.FuzzyQueryResult;
import de.ialistannen.javadocapi.querying.QueryApi;
import de.ialistannen.javadocapi.querying.QueryResult.ElementType;
import de.ialistannen.javadocapi.rendering.Java11PlusLinkResolver;
import de.ialistannen.javadocapi.rendering.MarkdownCommentRenderer;
import de.ialistannen.javadocapi.storage.ElementLoader;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import de.ialistannen.javadocapi.util.NameShortener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.Component.Type;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import org.apache.commons.lang3.StringUtils;

public class DocCommand implements Command {

  private final QueryApi<FuzzyQueryResult> queryApi;
  private final ElementLoader loader;
  private final MarkdownCommentRenderer renderer;
  private final Java11PlusLinkResolver linkResolveStrategy;
  private final Map<String, ActiveInteractions> activeInteractions;

  public DocCommand(QueryApi<FuzzyQueryResult> queryApi, ElementLoader loader) {
    this.queryApi = queryApi;
    this.loader = loader;

    this.linkResolveStrategy = new Java11PlusLinkResolver();
    this.renderer = new MarkdownCommentRenderer(linkResolveStrategy);
    this.activeInteractions = new LinkedHashMap<>() {
      @Override
      protected boolean removeEldestEntry(Entry<String, ActiveInteractions> eldest) {
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
    Integer choiceId = commandContext.shift(integer());
    String buttonId = commandContext.shift(word());

    handleStoredInteraction(
        source,
        choiceId,
        buttonId,
        () -> source.getEvent()
            .reply("\uD83D\uDE94 Are you trying to steal those buttons? \uD83D\uDE94")
            .setEphemeral(true)
            .queue()
    );
  }

  private void handleStoredInteraction(CommandSource source, Integer id, String buttonId,
      Runnable onPermissionDenied) {
    ActiveInteractions buttons = this.activeInteractions.get(buttonId);

    if (buttons == null) {
      source.editOrReply(
          "Couldn't find any stored choices for that message <:feelsBadMan:626724180284538890>"
      ).queue();
      return;
    }

    if (!Objects.equals(source.getAuthorId(), buttons.getUserId())) {
      onPermissionDenied.run();
      return;
    }

    this.activeInteractions.remove(buttonId);

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
  public void handle(CommandContext commandContext, SelectionMenuCommandSource source) {
    String buttonId = commandContext.shift(word());

    handleStoredInteraction(
        source,
        Integer.parseInt(source.getOption()),
        buttonId,
        () -> source.getEvent()
            .reply("\uD83D\uDE94 Checkbox theft is a crime! \uD83D\uDE94")
            .setEphemeral(true)
            .queue()
    );
  }

  @Override
  public void handle(CommandContext commandContext, CommandSource source) {
    Optional<String> isLong = commandContext.tryShift(literal("long"));

    handleQuery(source, commandContext.shift(remaining(2)), isLong.isEmpty(), false);
  }

  private void handleQuery(CommandSource source, String query, boolean shortDescription,
      boolean omitTags) {
    List<FuzzyQueryResult> results = queryApi.query(loader, query.strip())
        .stream()
        .distinct()
        .collect(toList());

    if (results.size() == 1) {
      replyForResult(source, results.get(0), shortDescription, omitTags);
      return;
    }

    if (results.stream().filter(FuzzyQueryResult::isExact).count() == 1) {
      FuzzyQueryResult result = results.stream()
          .filter(FuzzyQueryResult::isExact)
          .findFirst()
          .orElseThrow();

      replyForResult(source, result, shortDescription, omitTags);
      return;
    }

    if (results.size() == 0) {
      source.editOrReply(
          "I couldn't find any result for '" + query + "' <:feelsBadMan:626724180284538890>"
      ).queue();
      return;
    }
    replyMultipleResults(source, shortDescription, new HashSet<>(results));
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
      Set<FuzzyQueryResult> results) {

    Map<String, FuzzyQueryResult> nameResultMap = results.stream().collect(toMap(
        it -> it.getQualifiedName().asString(),
        it -> it,
        (a, b) -> a
    ));
    Map<String, String> shortenedNameMap = new NameShortener().shortenMatches(
        nameResultMap.keySet().stream().map(QualifiedName::new).collect(Collectors.toSet())
    );
    List<Entry<String, FuzzyQueryResult>> labelResultList = shortenedNameMap.entrySet().stream()
        .map(it -> Map.entry(it.getValue(), nameResultMap.get(it.getKey())))
        .sorted(
            Comparator.
                <Entry<String, FuzzyQueryResult>, Boolean>comparing(it -> it.getValue().isExact())
                .reversed()
                .thenComparing(Entry::getKey)
        )
        .collect(toList());

    List<ActionRow> rows;
    if (labelResultList.size() <= 5 * Type.BUTTON.getMaxPerRow()) {
      rows = buildRowsButton(labelResultList, source);
    } else {
      rows = buildRowsMenu(labelResultList, source);
    }

    Message message = new MessageBuilder("I found (at least) the following Elements:  \n")
        .setActionRows(rows)
        .build();

    source.reply(message).queue();
    activeInteractions.put(
        source.getId(),
        new ActiveInteractions(
            StreamUtils.enumerated(
                labelResultList.stream()
                    .map(it -> it.getValue().getQualifiedName().asString())
            ),
            shortDescription,
            source.getAuthorId()
        )
    );
  }

  private List<ActionRow> buildRowsButton(List<Entry<String, FuzzyQueryResult>> results,
      CommandSource source) {

    var counter = new Object() {
      int counter = 0;
    };

    return results.stream()
        .limit(Type.BUTTON.getMaxPerRow() * 5L)
        .collect(partition(Type.BUTTON.getMaxPerRow()))
        .values()
        .stream()
        .map(items -> {
          List<Button> buttons = new ArrayList<>();

          for (Entry<String, FuzzyQueryResult> entry : items) {
            boolean exact = entry.getValue().isExact();
            String label = entry.getKey();
            String command = "!javadoc " + counter.counter++ + " " + source.getId();
            buttons.add(Button.of(
                exact ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY,
                command,
                StringUtils.abbreviate(label, 80),
                getEmoji(entry.getValue())
            ));
          }

          return ActionRow.of(buttons);
        })
        .collect(toList());
  }

  private List<ActionRow> buildRowsMenu(List<Entry<String, FuzzyQueryResult>> results,
      CommandSource source) {
    var counter = new Object() {
      int counter = 0;
    };

    Map<ElementType, List<SelectOption>> grouped = results.stream()
        .collect(groupingBy(
            it -> it.getValue().getType(),
            mapping(
                it -> {
                  QualifiedName name = it.getValue().getQualifiedName();
                  String command = String.valueOf(counter.counter++);

                  String label;
                  if (name.asString().contains("#")) {
                    label = name.getLexicalParent()
                        .map(p -> p.getSimpleName() + "#")
                        .orElse("");
                    label += name.getSimpleName();
                  } else {
                    label = name.getSimpleName();
                  }

                  label = StringUtils.abbreviateMiddle(label, "...", 25);

                  return SelectOption
                      .of(
                          label,
                          command
                      )
                      .withDescription(StringUtils.abbreviate(it.getKey(), 50))
                      .withEmoji(getEmoji(it.getValue()));
                },
                toList()
            )
        ));

    return grouped.entrySet().stream()
        .limit(5)
        .map(it ->
            SelectionMenu.create("!javadoc " + source.getId())
                .addOptions(it.getValue().stream().limit(25).collect(toList()))
                .setPlaceholder(StringUtils.capitalize(it.getKey().name().toLowerCase(Locale.ROOT)))
                .build()
        )
        .map(ActionRow::of)
        .collect(toList());
  }

  private Emoji getEmoji(FuzzyQueryResult result) {
    if (result.getQualifiedName().asString().endsWith("Exception")) {
      return Emoji.fromMarkdown("<:Exception:871325719127547945>");
    }

    return switch (result.getType()) {
      case METHOD -> Emoji.fromMarkdown("<:Method:871140776711708743>");
      case FIELD -> Emoji.fromMarkdown("<:Field:871140776346791987>");
      case ANNOTATION -> Emoji.fromMarkdown("<:Annotation:871325719563751444>");
      case ENUM -> Emoji.fromMarkdown("<:Enum:871325719362412594>");
      case INTERFACE -> Emoji.fromMarkdown("<:Interface:871325719576318002>");
      case CLASS -> Emoji.fromMarkdown("<:Class:871140776900440074>");
    };
  }

  private static class ActiveInteractions {

    private final Map<Integer, String> choices;
    private final boolean shortDescription;
    private final String userId;

    private ActiveInteractions(Map<Integer, String> choices, boolean shortDescription,
        String userId) {
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
}
