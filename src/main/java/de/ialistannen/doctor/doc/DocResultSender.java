package de.ialistannen.doctor.doc;

import de.ialistannen.doctor.commands.EditReplyCommand;
import de.ialistannen.doctor.commands.EditReplyCommand.MessageCommand;
import de.ialistannen.doctor.commands.system.CommandSource;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.state.BotReply;
import de.ialistannen.doctor.state.MessageDataStore;
import de.ialistannen.doctor.util.nameproxies.NameProxyUtils;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.rendering.LinkResolveStrategy;
import de.ialistannen.javadocapi.rendering.MarkdownCommentRenderer;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public class DocResultSender {

  private final MessageDataStore dataStore;

  public DocResultSender(MessageDataStore dataStore) {
    this.dataStore = dataStore;
  }

  public void replyForReflectiveProxy(MessageSender sender, QualifiedName name,
      LinkResolveStrategy linkResolveStrategy) {
    DocEmbedBuilder docEmbedBuilder = new DocEmbedBuilder(
        new MarkdownCommentRenderer(linkResolveStrategy),
        NameProxyUtils.forName(name),
        "https://unknown-url.example.com"
    )
        .addColor()
        .addIcon(linkResolveStrategy)
        .addDeclaration()
        .addFooter("proxy for unknown element", Duration.ZERO);

    EmbedBuilder builder = new EmbedBuilder(docEmbedBuilder.build());
    builder
        .getDescriptionBuilder()
        .append(
            "This element was not indexed but probably inherited from a dependency. I can't really "
                + "tell you more sadly."
        );

    sender.editOrReply(new MessageBuilder(builder.build()).build()).queue();
  }

  public void replyWithResult(CommandSource source, MessageSender sender,
      LoadResult<JavadocElement> loadResult, boolean shortDesc, boolean omitTags,
      Duration queryDuration, LinkResolveStrategy linkResolveStrategy) {
    DocEmbedBuilder docEmbedBuilder = new DocEmbedBuilder(
        new MarkdownCommentRenderer(linkResolveStrategy),
        loadResult.getResult(),
        ((BaseUrlElementLoader) loadResult.getLoader()).getBaseUrl()
    )
        .addColor()
        .addIcon(linkResolveStrategy)
        .addDeclaration()
        .addFooter(loadResult.getLoader().toString(), queryDuration);

    if (shortDesc) {
      docEmbedBuilder.addShortDescription();
    } else {
      docEmbedBuilder.addLongDescription();
    }
    if (!omitTags) {
      docEmbedBuilder.addTags();
    }

    List<Button> buttons = new ArrayList<>();
    if (shortDesc) {
      buttons.add(buttonFor(MessageCommand.EXPAND, ButtonStyle.SECONDARY));
    } else {
      buttons.add(buttonFor(MessageCommand.COLLAPSE, ButtonStyle.SECONDARY));
    }
    if (omitTags) {
      buttons.add(buttonFor(MessageCommand.ADD_TAGS, ButtonStyle.SECONDARY));
    } else {
      buttons.add(buttonFor(MessageCommand.REMOVE_TAGS, ButtonStyle.SECONDARY));
    }
    buttons.add(buttonFor(MessageCommand.DELETE, ButtonStyle.DANGER));

    sender
        .editOrReply(
            new MessageBuilder(docEmbedBuilder.build())
                .setActionRows(ActionRow.of(buttons))
                .build()
        )
        .queue(
            messageHandle -> dataStore.addReply(
                messageHandle.getId(),
                new BotReply(loadResult.getLoader(), loadResult, source, shortDesc, omitTags)
            )
        );
  }

  private Button buttonFor(EditReplyCommand.MessageCommand command, ButtonStyle style) {
    return Button.of(
        style,
        "!edit-reply " + command.getId(),
        command.getLabel(),
        Emoji.fromUnicode(command.getIcon())
    );
  }
}
