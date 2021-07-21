package de.ialistannen.docfork.commands.system;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.requests.RestAction;

public class ButtonCommandSource implements CommandSource {

  private final ButtonClickEvent event;

  public ButtonCommandSource(ButtonClickEvent event) {
    this.event = event;
  }

  public ButtonClickEvent getEvent() {
    return event;
  }

  @Override
  public String getAuthorId() {
    return event.getInteraction().getUser().getId();
  }

  @Override
  public String getId() {
    return event.getId();
  }

  @Override
  public String rawText() {
    return event.getButton().getLabel();
  }

  @Override
  public RestAction<?> reply(Message message) {
    return event.reply(message);
  }

  @Override
  public RestAction<?> editOrReply(Message message) {
    return event.editMessage(message);
  }
}
