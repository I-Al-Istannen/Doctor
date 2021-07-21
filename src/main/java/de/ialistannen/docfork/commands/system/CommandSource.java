package de.ialistannen.docfork.commands.system;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.RestAction;

public interface CommandSource {

  String rawText();

  String getId();

  String getAuthorId();

  RestAction<?> reply(Message message);

  default RestAction<?> reply(String message) {
    return this.reply(new MessageBuilder(message).build());
  }

  RestAction<?> editOrReply(Message message);

  default RestAction<?> editOrReply(String message) {
    return editOrReply(new MessageBuilder(message).build());
  }
}
