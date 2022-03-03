package de.ialistannen.doctor.commands.system;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public class ButtonCommandSource implements CommandSource {

  private final ButtonInteractionEvent event;

  public ButtonCommandSource(ButtonInteractionEvent event) {
    this.event = event;
  }

  public ButtonInteractionEvent getEvent() {
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
