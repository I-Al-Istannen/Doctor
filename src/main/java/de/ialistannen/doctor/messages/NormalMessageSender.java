package de.ialistannen.doctor.messages;

import de.ialistannen.doctor.state.SentMessageHandle;
import de.ialistannen.doctor.state.SentMessageHandle.MessageHandle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.RestAction;

public class NormalMessageSender implements MessageSender {

  private final Message message;

  public NormalMessageSender(Message message) {
    this.message = message;
  }

  @Override
  public RestAction<SentMessageHandle> reply(Message message) {
    return this.message.reply(message).mentionRepliedUser(false).map(MessageHandle::new);
  }

  @Override
  public RestAction<SentMessageHandle> editOrReply(Message newMessage) {
    if (!message.getAuthor().equals(message.getJDA().getSelfUser())) {
      return reply(newMessage);
    }

    return message.editMessage(newMessage).map(MessageHandle::new);
  }
}
