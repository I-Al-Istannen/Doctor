package de.ialistannen.doctor.messages;

import de.ialistannen.doctor.state.SentMessageHandle;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.RestAction;

public interface MessageSender {

  /**
   * Replies to the original message with the passed one.
   *
   * @param message the message to reply with
   * @return the action
   */
  RestAction<SentMessageHandle> reply(Message message);

  default RestAction<SentMessageHandle> reply(String message) {
    return reply(new MessageBuilder(message).build());
  }

  /**
   * Edits the original message and sets it to the passed one. If editing is not possible, this
   * method will reply instead.
   *
   * @param newMessage the enw message
   * @return the action
   */
  RestAction<SentMessageHandle> editOrReply(Message newMessage);

  default RestAction<SentMessageHandle> editOrReply(String message) {
    return editOrReply(new MessageBuilder(message).build());
  }

  /**
   * Deletes this message.
   *
   * @return the action
   */
  RestAction<Void> delete();
}
