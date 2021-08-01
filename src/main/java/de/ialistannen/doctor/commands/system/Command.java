package de.ialistannen.doctor.commands.system;

import de.ialistannen.doctor.messages.MessageSender;
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

  default void handle(CommandContext commandContext, CommandSource source, MessageSender sender) {
    if (source instanceof MessageCommandSource) {
      handle(commandContext, (MessageCommandSource) source, sender);
    } else if (source instanceof ButtonCommandSource) {
      handle(commandContext, (ButtonCommandSource) source, sender);
    } else if (source instanceof SlashCommandSource) {
      handle(commandContext, (SlashCommandSource) source, sender);
    } else if (source instanceof SelectionMenuCommandSource) {
      handle(commandContext, (SelectionMenuCommandSource) source, sender);
    }
  }

  default void handle(CommandContext commandContext, MessageCommandSource source,
      MessageSender sender) {
    handle(commandContext, (CommandSource) source, sender);
  }

  default void handle(CommandContext commandContext, ButtonCommandSource source,
      MessageSender sender) {
    handle(commandContext, (CommandSource) source, sender);
  }

  default void handle(CommandContext commandContext, SelectionMenuCommandSource source,
      MessageSender sender) {
    handle(commandContext, (CommandSource) source, sender);
  }

  default void handle(CommandContext commandContext, SlashCommandSource source,
      MessageSender sender) {
    handle(commandContext, (CommandSource) source, sender);
  }
}
