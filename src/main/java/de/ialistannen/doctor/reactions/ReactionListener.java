package de.ialistannen.doctor.reactions;

import de.ialistannen.doctor.doc.DocResultSender;
import de.ialistannen.doctor.state.BotReply;
import de.ialistannen.doctor.state.MessageDataStore;
import java.util.Optional;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ReactionListener extends ListenerAdapter {

  private final MessageDataStore dataStore;
  private final DocResultSender resultSender;

  public ReactionListener(MessageDataStore dataStore, DocResultSender resultSender) {
    this.dataStore = dataStore;
    this.resultSender = resultSender;
  }

  @Override
  public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
    Optional<BotReply> reply = dataStore.getReply(event.getMessageId());

    if (reply.isEmpty() || event.getUser() == null || event.getUser().isBot()) {
      return;
    }
    BotReply botReply = reply.get();

    if (!botReply.getSource().getAuthorId().equals(event.getUserId())) {
      event.retrieveUser().queue(user -> event.getReaction().removeReaction(user).queue());
      return;
    }

    Optional<AvailableReaction> reaction = AvailableReaction
        .fromUnicode(event.getReactionEmote().getAsReactionCode());

    if (reaction.isEmpty()) {
      return;
    }

    boolean shortDescription = botReply.isShortDescription();
    boolean omitTags = botReply.isOmitTags();

    switch (reaction.get()) {
      case DELETE -> {
        botReply.getMessageHandle().delete().queue();
        dataStore.removeReply(event.getMessageId());
        return;
      }
      case COLLAPSE -> shortDescription = true;
      case EXPAND -> {
        shortDescription = false;
        omitTags = false;
      }
      case REMOVE_TAGS -> omitTags = !omitTags;
    }

    resultSender.replyWithResult(
        botReply.getSource(),
        botReply.getMessageHandle().asSender(),
        botReply.getElement(),
        shortDescription,
        omitTags
    );

    event.retrieveUser().queue(user -> event.getReaction().removeReaction(user).queue());
  }
}
