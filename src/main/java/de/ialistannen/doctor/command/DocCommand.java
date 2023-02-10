package de.ialistannen.doctor.command;

import de.ialistannen.doctor.DocTorConfig;
import de.ialistannen.doctor.DocTorConfig.SourceConfig;
import de.ialistannen.doctor.rendering.DocEmbedBuilder;
import de.ialistannen.doctor.rendering.DocEmbedBuilder.DescriptionStyle;
import de.ialistannen.doctor.rendering.TooManyEmbedBuilder;
import de.ialistannen.doctor.storage.ActiveMessages;
import de.ialistannen.doctor.storage.ActiveMessages.ActiveMessage;
import de.ialistannen.doctor.storage.MultiFileStorage;
import de.ialistannen.doctor.storage.MultiFileStorage.FetchResult;
import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.model.elements.DocumentedElements;
import de.ialistannen.javadocbpi.model.javadoc.ReferenceConversions;
import de.ialistannen.javadocbpi.query.CaseSensitivity;
import de.ialistannen.javadocbpi.query.MatchingStrategy;
import de.ialistannen.javadocbpi.query.PrefixTrie;
import de.ialistannen.javadocbpi.query.QueryTokenizer;
import de.ialistannen.javadocbpi.query.QueryTokenizer.Token;
import de.ialistannen.javadocbpi.rendering.links.ExternalJavadocAwareLinkResolver;
import de.ialistannen.javadocbpi.rendering.links.ExternalJavadocReference;
import de.ialistannen.javadocbpi.rendering.links.Java11PlusLinkResolver;
import de.ialistannen.javadocbpi.rendering.links.LinkResolver;
import de.ialistannen.javadocbpi.rendering.links.OnlineJavadocIndexer;
import de.ialistannen.javadocbpi.storage.JsonSerializer;
import de.ialistannen.javadocbpi.storage.SQLiteStorage;
import java.awt.Color;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class DocCommand {

  public static final CommandData COMMAND = Commands
      .slash("doc", "Fetches Javadoc for the standard library and some more.")
      .addOption(
          OptionType.STRING,
          "query",
          "The element you want to look up. E.g. 'String#contains'",
          true,
          true
      );

  private static final List<Pair<MatchingStrategy, CaseSensitivity>> MATCHING_ORDER = List.of(
      Pair.of(MatchingStrategy.EXACT, CaseSensitivity.CONSIDER_CASE),
      Pair.of(MatchingStrategy.EXACT, CaseSensitivity.IGNORE_CASE),
      Pair.of(MatchingStrategy.PREFIX, CaseSensitivity.CONSIDER_CASE),
      Pair.of(MatchingStrategy.PREFIX, CaseSensitivity.IGNORE_CASE)
  );

  private final QueryTokenizer queryTokenizer;
  private final PrefixTrie trie;
  private final MultiFileStorage storage;
  private final LinkResolver linkResolver;
  private final ActiveMessages activeMessages;
  private final TooManyEmbedBuilder tooManyEmbedBuilder;

  private DocCommand(
      QueryTokenizer queryTokenizer,
      PrefixTrie trie,
      MultiFileStorage storage,
      LinkResolver linkResolver,
      ActiveMessages activeMessages) {
    this.queryTokenizer = queryTokenizer;
    this.trie = trie;
    this.storage = storage;
    this.linkResolver = linkResolver;
    this.activeMessages = activeMessages;
    this.tooManyEmbedBuilder = new TooManyEmbedBuilder(storage, activeMessages);
  }

  public void onCommand(SlashCommandInteractionEvent event) throws SQLException, IOException {
    String query = Objects.requireNonNull(event.getOption("query")).getAsString();

    Collection<String> qualifiedNames = findQualifiedNames(queryTokenizer.tokenize(query));

    if (qualifiedNames.isEmpty()) {
      event.replyEmbeds(notFoundEmbed(query)).queue();
    } else if (qualifiedNames.size() == 1) {
      replyFound(event, qualifiedNames);
    } else {
      replyTooManyFound(event, qualifiedNames);
    }
  }

  private Collection<String> findQualifiedNames(List<Token> tokens) {
    for (var pair : MATCHING_ORDER) {
      Collection<String> names = trie.find(pair.getLeft(), pair.getRight(), tokens);
      if (!names.isEmpty()) {
        return names;
      }
    }
    return List.of();
  }

  private void replyFound(SlashCommandInteractionEvent event, Collection<String> qualifiedNames)
      throws SQLException, IOException {

    String qualifiedName = qualifiedNames.iterator().next();
    ActiveMessage message = ActiveMessage.of(event.getUser().getId(), qualifiedName);
    event.reply(
        new MessageCreateBuilder()
            .addEmbeds(foundEmbed(message))
            .setComponents(foundMessageActions(message))
            .build()
    ).queue(interactionHook -> interactionHook.retrieveOriginal()
        .queue(it -> activeMessages.registerMessage(it.getId(), message))
    );
  }

  private MessageEmbed notFoundEmbed(String query) {
    return new EmbedBuilder()
        .setTitle("Query result")
        .setDescription(
            "I could not find any result for '" + StringUtils.truncate(query, 3000) + "'"
        )
        .setColor(new Color(255, 99, 71)) // tomato
        .build();
  }

  private MessageEmbed foundEmbed(ActiveMessage message) throws SQLException, IOException {
    String qualifiedName = message.qualifiedName();

    Optional<FetchResult> documentedElement = storage.get(qualifiedName);
    if (documentedElement.isEmpty()) {
      return notFoundEmbed(qualifiedName);
    }
    Optional<DocumentedElement> parentElement = Optional.empty();
    DocumentedElementReference ref = documentedElement.get().reference();
    Optional<DocumentedElementReference> parentRef;
    if (ref.isMethod() || ref.isField()) {
      parentRef = ref.getType();
    } else {
      parentRef = ref.parent();
    }
    if (parentRef.isPresent()) {
      parentElement = storage.get(parentRef.get().asQualifiedName()).map(FetchResult::element);
    }

    FetchResult result = documentedElement.get();
    return new DocEmbedBuilder(
        linkResolver,
        result.element(),
        parentElement.orElse(null),
        result.reference(),
        documentedElement.get().config().javadocUrl()
    )
        .addColor()
        .addTitle()
        .addIcon(linkResolver)
        .addDeclaration()
        .addTags(message.tags())
        .addDescription(message.descriptionStyle())
        .addFooter()
        .build();
  }

  private void replyTooManyFound(SlashCommandInteractionEvent event, Collection<String> matches)
      throws SQLException, IOException {
    MessageCreateBuilder builder = new MessageCreateBuilder();
    tooManyEmbedBuilder.buildMessage(event.getUser().getId(), matches, builder);

    event.reply(builder.build()).queue();
  }

  private ActionRow foundMessageActions(ActiveMessage message) {
    List<MessageCommand> commands = new ArrayList<>();
    commands.add(MessageCommand.DELETE);
    commands.add(message.tags() ? MessageCommand.REMOVE_TAGS : MessageCommand.ADD_TAGS);
    commands.add(
        message.descriptionStyle() == DescriptionStyle.SHORT
            ? MessageCommand.EXPAND
            : MessageCommand.COLLAPSE
    );

    return ActionRow.of(
        commands.stream()
            .sorted()
            .map(it -> Button.of(
                it.getButtonStyle(),
                it.getId(),
                it.getLabel(),
                Emoji.fromFormatted(it.getIcon())
            ))
            .toList()
    );
  }

  public static DocCommand create(DocTorConfig config, ActiveMessages activeMessages)
      throws SQLException, IOException, InterruptedException {

    ExternalJavadocAwareLinkResolver resolver = new ExternalJavadocAwareLinkResolver(
        new Java11PlusLinkResolver(),
        indexExternalJavadoc(
            config.sources().stream()
                .flatMap(it -> it.externalJavadoc().stream())
                .toList()
        )
    );

    JsonSerializer serializer = new JsonSerializer();
    DocumentedElements elements = new DocumentedElements();
    Map<SourceConfig, SQLiteStorage> storages = new HashMap<>();

    for (SourceConfig source : config.sources()) {
      SQLiteStorage storage = new SQLiteStorage(Path.of(source.database()), serializer);
      storages.put(source, storage);
      elements.merge(storage.getAll());
    }

    return new DocCommand(
        new QueryTokenizer(),
        PrefixTrie.forElements(elements),
        new MultiFileStorage(storages),
        resolver,
        activeMessages
    );
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


  public void updateButton(ButtonInteractionEvent event, String qualifiedName)
      throws SQLException, IOException {
    updateMessage(event, ActiveMessage.of(event.getUser().getId(), qualifiedName));
  }

  public void updateMessage(ButtonInteractionEvent event, ActiveMessage message)
      throws SQLException, IOException {
    activeMessages.registerMessage(event.getMessageId(), message);

    event.getInteraction().editMessage(
        new MessageEditBuilder()
            .setEmbeds(foundEmbed(message))
            .setComponents(foundMessageActions(message))
            .setReplace(true)
            .build()
    ).queue();
  }

  public void runAutoComplete(CommandAutoCompleteInteractionEvent event) {
    String query = event.getFocusedOption().getValue().strip();
    if (query.isEmpty()) {
      event.replyChoices(List.of()).queue();
      return;
    }

    event.replyChoiceStrings(
        buildChoices(queryTokenizer.tokenize(query))
            .stream()
            .limit(OptionData.MAX_CHOICES)
            .sorted()
            .map(it -> StringUtils.truncate(it, OptionData.MAX_CHOICE_NAME_LENGTH))
            .toList()
    ).queue();
  }

  private Collection<String> buildChoices(List<Token> tokens) {
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (var pair : MATCHING_ORDER) {
      result.addAll(trie.autocomplete(
          pair.getLeft(), pair.getRight(), tokens, OptionData.MAX_CHOICES
      ));
      if (result.size() >= OptionData.MAX_CHOICES) {
        break;
      }
    }

    return result.stream().map(this::unqualifyMethodParameters).toList();
  }

  private String unqualifyMethodParameters(String fqn) {
    if (!fqn.contains("(")) {
      return fqn;
    }
    String firstPart = fqn.substring(0, fqn.indexOf('('));
    String params = fqn.substring(fqn.indexOf('(') + 1)
        // remove modules from qualifier
        .replaceAll("([^,]+?/)", "");
    return firstPart + "(" + ReferenceConversions.unqualifyReference(params) + ")";
  }
}
