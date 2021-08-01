package de.ialistannen.doctor.commands.system;

import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;

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
}
