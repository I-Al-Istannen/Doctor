package de.ialistannen.doctor.command;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.ExecutionException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class UpdateSlashesCommand {

  private final String authorId;

  public UpdateSlashesCommand(String authorId) {
    this.authorId = authorId;
  }

  public void run(MessageReceivedEvent event) throws ExecutionException, InterruptedException {
    if (!event.getAuthor().getId().equals(authorId)) {
      return;
    }

    event.getGuild().updateCommands()
        .addCommands(List.of(DocCommand.COMMAND))
        .submit()
        .get();

    event.getMessage().reply(
        new MessageCreateBuilder()
            .setEmbeds(
                new EmbedBuilder()
                    .setTitle("Commands reloaded")
                    .setColor(Color.GREEN)
                    .setFooter("This has been widely regarded as a bad move")
                    .build()
            )
            .build()
    ).queue();
  }
}
