package de.ialistannen.doctor.commands.system;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.requests.RestAction;

public class SelectionMenuCommandSource implements CommandSource {

  private final SelectionMenuEvent event;

  public SelectionMenuCommandSource(SelectionMenuEvent event) {
    this.event = event;
  }

  public SelectionMenuEvent getEvent() {
    return event;
  }

  public String getOption() {
    return event.getValues().get(0);
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
    return String.join("\n", event.getValues());
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
