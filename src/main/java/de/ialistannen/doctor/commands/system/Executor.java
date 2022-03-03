package de.ialistannen.doctor.commands.system;

import de.ialistannen.doctor.messages.InitialInteractionMessageSender;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.messages.NormalMessageSender;
import de.ialistannen.doctor.util.parsers.ParseError;
import de.ialistannen.doctor.util.parsers.StringReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    MessageSender sender = new NormalMessageSender(event.getMessage());
    MessageCommandSource source = new MessageCommandSource(event.getMessage());
    StringReader reader = new StringReader(event.getMessage().getContentRaw());
    findCommand(reader).ifPresent(
        command -> handleCommandError(
            sender,
            () -> command.handle(new CommandContext(reader), source, sender)
        )
    );
  }

  @Override
  public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
    SelectMenuInteraction interaction = event.getInteraction();
    if (interaction.getValues().size() != 1) {
      event.reply("Sorry, I can't really handle multiple/none options right now.")
          .setEphemeral(true)
          .queue();
      return;
    }
    StringReader reader = new StringReader(interaction.getComponentId());
    Optional<Command> command = findCommand(reader);

    if (command.isEmpty()) {
      event.reply("I couldn't find a command to handle this :/").setEphemeral(true).queue();
      return;
    }

    MessageSender sender = new InitialInteractionMessageSender(interaction);
    SelectionMenuCommandSource source = new SelectionMenuCommandSource(event);
    handleCommandError(
        sender,
        () -> command.get().handle(new CommandContext(reader), source, sender)
    );
  }

  @Override
  public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
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

    MessageSender sender = new InitialInteractionMessageSender(event);
    ButtonCommandSource source = new ButtonCommandSource(event);
    handleCommandError(
        sender,
        () -> command.get().handle(new CommandContext(reader), source, sender)
    );
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    StringReader reader = new StringReader("!" + event.getName() + " ");

    Optional<Command> command = findCommand(reader);

    if (command.isEmpty()) {
      event.reply("Command not found :/").queue();
      return;
    }

    MessageSender sender = new InitialInteractionMessageSender(event);
    SlashCommandSource source = new SlashCommandSource(event);
    handleCommandError(
        sender,
        () -> command.get().handle(new CommandContext(reader), source, sender)
    );
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

  private void handleCommandError(MessageSender sender, Runnable runnable) {
    try {
      runnable.run();
    } catch (ParseError e) {
      sender.editOrReply(
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
