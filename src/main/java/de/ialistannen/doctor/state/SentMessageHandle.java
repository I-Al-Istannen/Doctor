package de.ialistannen.doctor.state;

import de.ialistannen.doctor.messages.InitialInteractionMessageSender;
import de.ialistannen.doctor.messages.InteractionHookMessageSender;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.messages.NormalMessageSender;
import de.ialistannen.doctor.reactions.AvailableReaction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;

public interface SentMessageHandle {

  /**
   * @return the snowflake id for this message
   */
  String getId();

  /**
   * @return a {@link MessageSender} initialized from this message
   */
  MessageSender asSender();

  /**
   * Delees this message.
   *
   * @return the action
   */
  RestAction<?> delete();

  /**
   * Reacts on the message this handle refers to, if there is any.
   *
   * @param emote the emote to react with
   * @return the action
   */
  RestAction<Void> addReaction(String emote);

  /**
   * @return true if the message has all of our custom reactions
   */
  boolean hasOwnReactions();

  class MessageHandle implements SentMessageHandle {

    private final Message underlying;

    public MessageHandle(Message underlying) {
      this.underlying = underlying;
    }

    @Override
    public MessageSender asSender() {
      return new NormalMessageSender(underlying);
    }

    @Override
    public String getId() {
      return underlying.getId();
    }

    @Override
    public RestAction<?> delete() {
      return underlying.delete();
    }

    @Override
    public RestAction<Void> addReaction(String emote) {
      return underlying.addReaction(emote);
    }

    @Override
    public boolean hasOwnReactions() {
      for (AvailableReaction reaction : AvailableReaction.values()) {
        boolean hasReaction = underlying.getReactions()
            .stream()
            .anyMatch(
                it -> it.getReactionEmote().getAsReactionCode().equals(reaction.getUnicode())
            );
        if (!hasReaction) {
          return false;
        }
      }
      return true;
    }
  }

  class InteractionHookHandle implements SentMessageHandle {

    private final InteractionHook hook;
    private final Message message;

    public InteractionHookHandle(InteractionHook hook, Message message) {
      this.hook = hook;
      this.message = message;
    }

    public Message getMessage() {
      return message;
    }

    @Override
    public MessageSender asSender() {
      if (hook.getInteraction().isAcknowledged()) {
        return new InteractionHookMessageSender(hook);
      }
      return new InitialInteractionMessageSender(hook.getInteraction());
    }

    @Override
    public String getId() {
      return message.getId();
    }

    @Override
    public RestAction<?> delete() {
      return message.delete();
    }

    @Override
    public RestAction<Void> addReaction(String emote) {
      return message.addReaction(emote);
    }

    @Override
    public boolean hasOwnReactions() {
      for (AvailableReaction reaction : AvailableReaction.values()) {
        boolean hasReaction = message.getReactions()
            .stream()
            .anyMatch(
                it -> it.getReactionEmote().getAsReactionCode().equals(reaction.getUnicode())
            );
        if (!hasReaction) {
          return false;
        }
      }
      return true;
    }

  }

}
