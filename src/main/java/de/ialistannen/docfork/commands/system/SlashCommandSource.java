package de.ialistannen.docfork.commands.system;

import java.util.Optional;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.RestAction;

public class SlashCommandSource implements CommandSource {

  private final SlashCommandEvent event;

  public SlashCommandSource(SlashCommandEvent event) {
    this.event = event;
  }

  public SlashCommandEvent getEvent() {
    return event;
  }

  @Override
  public String rawText() {
    return event.getOptions().stream()
        .filter(it -> it.getType() == OptionType.STRING)
        .map(OptionMapping::getAsString)
        .findFirst()
        .orElse("");
  }

  public Optional<OptionMapping> getOption(String name) {
    return Optional.ofNullable(event.getOption(name));
  }

  @Override
  public RestAction<?> reply(Message message) {
    return event.reply(message);
  }

  @Override
  public RestAction<?> editOrReply(Message message) {
    return reply(message);
  }
}
