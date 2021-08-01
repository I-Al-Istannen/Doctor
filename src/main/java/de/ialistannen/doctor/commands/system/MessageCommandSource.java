package de.ialistannen.doctor.commands.system;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;

public class MessageCommandSource implements CommandSource {

  private final Message message;

  public MessageCommandSource(Message message) {
    this.message = message;
  }

  public Message getMessage() {
    return message;
  }

  public MessageChannel getChannel() {
    return message.getChannel();
  }

  @Override
  public String getId() {
    return message.getId();
  }

  @Override
  public String getAuthorId() {
    return message.getAuthor().getId();
  }
}
