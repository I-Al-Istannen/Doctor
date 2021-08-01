package de.ialistannen.doctor.doc;

import de.ialistannen.doctor.commands.EditReplyCommand;
import de.ialistannen.doctor.commands.EditReplyCommand.MessageCommand;
import de.ialistannen.doctor.commands.system.CommandSource;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.state.BotReply;
import de.ialistannen.doctor.state.MessageDataStore;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.rendering.LinkResolveStrategy;
import de.ialistannen.javadocapi.rendering.MarkdownCommentRenderer;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public class DocResultSender {

  private final MarkdownCommentRenderer renderer;
  private final LinkResolveStrategy linkResolveStrategy;
  private final MessageDataStore dataStore;

  public DocResultSender(MarkdownCommentRenderer renderer, LinkResolveStrategy linkResolveStrategy,
      MessageDataStore dataStore) {
    this.renderer = renderer;
    this.linkResolveStrategy = linkResolveStrategy;
    this.dataStore = dataStore;
  }

  public void replyWithResult(CommandSource source, MessageSender sender,
      LoadResult<JavadocElement> loadResult, boolean shortDesc, boolean omitTags) {
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
                new BotReply(messageHandle, loadResult, source, shortDesc, omitTags)
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
