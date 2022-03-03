package de.ialistannen.doctor.commands.system;

import java.util.Optional;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class SlashCommandSource implements CommandSource {

  private final SlashCommandInteractionEvent event;

  public SlashCommandSource(SlashCommandInteractionEvent event) {
    this.event = event;
  }

  public SlashCommandInteractionEvent getEvent() {
    return event;
  }

  public Optional<OptionMapping> getOption(String name) {
    return Optional.ofNullable(event.getOption(name));
  }

  @Override
  public String getId() {
    return event.getId();
  }

  @Override
  public String getAuthorId() {
    return event.getUser().getId();
  }
}
