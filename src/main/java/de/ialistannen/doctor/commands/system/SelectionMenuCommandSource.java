package de.ialistannen.doctor.commands.system;

import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

public class SelectionMenuCommandSource implements CommandSource {

  private final SelectMenuInteractionEvent event;

  public SelectionMenuCommandSource(SelectMenuInteractionEvent event) {
    this.event = event;
  }

  public SelectMenuInteractionEvent getEvent() {
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
}
