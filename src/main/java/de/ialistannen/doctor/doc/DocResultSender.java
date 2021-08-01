package de.ialistannen.doctor.doc;

import de.ialistannen.doctor.commands.system.CommandSource;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.reactions.AvailableReaction;
import de.ialistannen.doctor.state.BotReply;
import de.ialistannen.doctor.state.MessageDataStore;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.rendering.LinkResolveStrategy;
import de.ialistannen.javadocapi.rendering.MarkdownCommentRenderer;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import net.dv8tion.jda.api.MessageBuilder;
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

    sender
        .editOrReply(new MessageBuilder(docEmbedBuilder.build()).build())
        .queue(
            messageHandle -> {
              dataStore.addReply(
                  messageHandle.getId(),
                  new BotReply(messageHandle, loadResult, source, shortDesc, omitTags)
              );

              if (!messageHandle.hasOwnReactions()) {
                for (AvailableReaction reaction : AvailableReaction.values()) {
                  messageHandle.addReaction(reaction.getUnicode()).queue();
                }
              }
            }
        );
  }
}
