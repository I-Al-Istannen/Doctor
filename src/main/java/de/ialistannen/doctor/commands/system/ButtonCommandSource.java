package de.ialistannen.doctor.commands.system;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

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
}
