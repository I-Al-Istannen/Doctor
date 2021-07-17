package de.ialistannen.docfork.commands.system;

import de.ialistannen.docfork.util.parsers.ParseError;
import de.ialistannen.docfork.util.parsers.StringReader;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class Executor extends ListenerAdapter {

  private final List<Command> commands;

  public Executor(List<Command> commands) {
    this.commands = commands;

    for (Command command : commands) {
      command.setup(this);
    }
  }

  public List<Command> getCommands() {
    return Collections.unmodifiableList(commands);
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    MessageCommandSource source = new MessageCommandSource(event.getMessage());
    StringReader reader = new StringReader(event.getMessage().getContentRaw());
    findCommand(reader).ifPresent(
        command -> handleCommandError(
            source,
            () -> command.handle(new CommandContext(reader), source)
        )
    );
  }

  @Override
  public void onButtonClick(@NotNull ButtonClickEvent event) {
    if (event.getButton() == null) {
      event.reply("Unknown button :/").queue();
      return;
    }
    if (event.getButton().getId() == null) {
      event.reply("Button had no id :/").queue();
      return;
    }

    StringReader reader = new StringReader(event.getButton().getId());
    Optional<Command> command = findCommand(reader);

    if (command.isEmpty()) {
      event.reply("I couldn't find a command to handle this :/").queue();
      return;
    }

    ButtonCommandSource source = new ButtonCommandSource(event);
    handleCommandError(source, () -> command.get().handle(new CommandContext(reader), source));
  }

  @Override
  public void onSlashCommand(@NotNull SlashCommandEvent event) {
    StringReader reader = new StringReader("!" + event.getName() + " ");

    Optional<Command> command = findCommand(reader);

    if (command.isEmpty()) {
      event.reply("Command not found :/").queue();
      return;
    }

    SlashCommandSource source = new SlashCommandSource(event);
    handleCommandError(source, () -> command.get().handle(new CommandContext(reader), source));
  }

  private Optional<Command> findCommand(StringReader reader) {
    if (!reader.canRead() || reader.readChar() != '!') {
      return Optional.empty();
    }
    for (Command command : commands) {
      if (!command.keyword().canParse(reader)) {
        continue;
      }

      reader.readWhile(Character::isWhitespace);

      return Optional.of(command);
    }

    return Optional.empty();
  }

  private void handleCommandError(CommandSource source, Runnable runnable) {
    try {
      runnable.run();
    } catch (ParseError e) {
      source.editOrReply(
          new MessageBuilder()
              .setEmbeds(
                  new EmbedBuilder()
                      .setTitle("Error executing command")
                      .setDescription("```\n" + e + "\n```")
                      .setColor(new Color(255, 99, 71))
                      .build()
              )
              .build()
      ).queue();
    }
  }
}
