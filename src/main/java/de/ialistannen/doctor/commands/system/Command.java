package de.ialistannen.doctor.commands.system;

import de.ialistannen.doctor.util.parsers.ArgumentParser;
import java.util.Optional;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface Command {

  ArgumentParser<?> keyword();

  default void setup(Executor executor) {
  }

  default Optional<CommandData> getSlashData() {
    return Optional.empty();
  }

  default void handle(CommandContext commandContext, CommandSource source) {
    if (source instanceof MessageCommandSource) {
      handle(commandContext, (MessageCommandSource) source);
    } else if (source instanceof ButtonCommandSource) {
      handle(commandContext, (ButtonCommandSource) source);
    } else if (source instanceof SlashCommandSource) {
      handle(commandContext, (SlashCommandSource) source);
    }
  }

  default void handle(CommandContext commandContext, MessageCommandSource source) {
    handle(commandContext, (CommandSource) source);
  }

  default void handle(CommandContext commandContext, ButtonCommandSource source) {
    handle(commandContext, (CommandSource) source);
  }

  default void handle(CommandContext commandContext, SlashCommandSource source) {
    handle(commandContext, (CommandSource) source);
  }
}
