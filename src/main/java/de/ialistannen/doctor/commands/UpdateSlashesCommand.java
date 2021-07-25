package de.ialistannen.doctor.commands;

import static de.ialistannen.doctor.util.parsers.ArgumentParsers.literal;

import de.ialistannen.doctor.util.parsers.ArgumentParser;
import de.ialistannen.doctor.commands.system.Command;
import de.ialistannen.doctor.commands.system.CommandContext;
import de.ialistannen.doctor.commands.system.Executor;
import de.ialistannen.doctor.commands.system.MessageCommandSource;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class UpdateSlashesCommand implements Command {

  private Executor executor;

  @Override
  public ArgumentParser<?> keyword() {
    return literal("update-slashes");
  }

  @Override
  public void setup(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void handle(CommandContext commandContext, MessageCommandSource source) {
    Guild guild = source.getMessage().getGuild();

    User author = source.getMessage().getAuthor();
    if (!author.getId().equals("138235433115975680")) {
      return;
    }

    guild.updateCommands()
        .addCommands(
            executor.getCommands()
                .stream()
                .flatMap(it -> it.getSlashData().stream())
                .collect(Collectors.toList())
        )
        .queue();

    source.reply("Commands in your guild re-registered").queue();
  }
}
