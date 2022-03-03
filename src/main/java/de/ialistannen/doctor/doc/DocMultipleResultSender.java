package de.ialistannen.doctor.doc;

import de.ialistannen.doctor.commands.system.CommandSource;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.state.ActiveInteractions;
import de.ialistannen.doctor.state.MessageDataStore;
import de.ialistannen.doctor.util.StreamUtils;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.querying.FuzzyQueryResult;
import de.ialistannen.javadocapi.querying.QueryResult.ElementType;
import de.ialistannen.javadocapi.util.NameShortener;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component.Type;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static de.ialistannen.doctor.util.StreamUtils.partition;
import static java.util.stream.Collectors.*;

public class DocMultipleResultSender {

  private final MessageDataStore dataStore;

  public DocMultipleResultSender(MessageDataStore dataStore) {
    this.dataStore = dataStore;
  }

  public void replyMultipleResults(CommandSource source, MessageSender sender,
      boolean shortDescription, boolean omitTags, Set<FuzzyQueryResult> results) {

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

    sender.reply(message).queue();
    dataStore.addActiveInteraction(
        source.getId(),
        new ActiveInteractions(
            StreamUtils.enumerated(labelResultList.stream().map(Entry::getValue)),
            shortDescription,
            omitTags,
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
            SelectMenu.create("!javadoc " + source.getId() + " " + counter.counter++)
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
}
