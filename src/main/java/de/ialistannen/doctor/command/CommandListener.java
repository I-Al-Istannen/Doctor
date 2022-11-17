package de.ialistannen.doctor.command;

import de.ialistannen.doctor.DocTorConfig;
import de.ialistannen.doctor.storage.ActiveMessages;
import de.ialistannen.doctor.storage.ActiveMessages.ActiveChooser;
import de.ialistannen.doctor.storage.ActiveMessages.ActiveMessage;
import java.awt.Color;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandListener extends ListenerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommandListener.class);

  private final UpdateSlashesCommand updateSlashesCommand;
  private final DocCommand docCommand;
  private final ActiveMessages activeMessages;

  public CommandListener(
      DocTorConfig config,
      DocCommand docCommand,
      ActiveMessages activeMessages
  ) {
    this.updateSlashesCommand = new UpdateSlashesCommand(config.authorId());
    this.docCommand = docCommand;
    this.activeMessages = activeMessages;
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }
    if (event.getMessage().getContentRaw().startsWith("!update-slashes")) {
      updateSlashes(event);
    }
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals("doc")) {
      return;
    }

    try {
      docCommand.onCommand(event);
    } catch (Exception e) {
      LOGGER.error("Error fetching docs", e);
      event.reply(genericErrorMessage()).queue();
    }
  }

  private void updateSlashes(@NotNull MessageReceivedEvent event) {
    try {
      updateSlashesCommand.run(event);
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error("Error updating slash commands", e);
      event.getMessage()
          .reply(genericErrorMessage())
          .queue();
    }
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    Optional<ActiveChooser> reference = activeMessages.lookupChooser(event.getComponentId());
    if (reference.isPresent()) {
      if (!event.getUser().getId().equals(reference.get().ownerId())) {
        event.reply(errorMessage("🐠 No touchy the fishy 🐠")).setEphemeral(true).queue();
        return;
      }
      runChooser(event, reference.get().qualifiedName());
      return;
    }

    Optional<ActiveMessage> activeMessage = activeMessages.lookupMessage(event.getMessageId());
    Optional<MessageCommand> messageCommand = MessageCommand.fromId(event.getComponentId());
    if (messageCommand.isEmpty() || activeMessage.isEmpty()) {
      event.reply(forgotDataErrorMessage())
          .setEphemeral(true)
          .queue();
      return;
    }
    if (!activeMessage.get().ownerId().equals(event.getUser().getId())) {
      event.reply(errorMessage("🐠 No touchy the fishy 🐠")).setEphemeral(true).queue();
      return;
    }
    Optional<ActiveMessage> newMessage = messageCommand.get().update(activeMessage.get());

    if (newMessage.isEmpty()) {
      // Not sure why I need to edit first but w/e
      event.editMessage(
          new MessageEditBuilder()
              .setReplace(true)
              .setContent("Deleting...")
              .build()
      ).queue(interactionHook -> interactionHook.deleteOriginal().queue());
      activeMessages.deleteMessage(event.getMessageId());
      return;
    }

    try {
      docCommand.updateMessage(event, newMessage.get());
    } catch (Exception e) {
      LOGGER.error("Error updating message", e);
      event.reply(genericErrorMessage()).queue();
    }
  }

  private void runChooser(ButtonInteractionEvent event, String qualifiedName) {
    try {
      docCommand.updateButton(event, qualifiedName);
    } catch (Exception e) {
      LOGGER.error("Error fetching docs for button", e);
      event.reply(genericErrorMessage()).queue();
    }
  }

  @Override
  public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
    if (!event.getName().equals(DocCommand.COMMAND.getName())) {
      return;
    }
    docCommand.runAutoComplete(event);
  }

  private MessageCreateData genericErrorMessage() {
    return errorMessage("An error occurred <:feelsBadMan:1042827948211843154>");
  }

  private MessageCreateData forgotDataErrorMessage() {
    return errorMessage("I don't know what you are on about <:feelsBadMan:1042827948211843154>");
  }

  private MessageCreateData errorMessage(String error) {
    return new MessageCreateBuilder()
        .setEmbeds(
            new EmbedBuilder()
                .setTitle("Error")
                .setColor(new Color(255, 99, 71)) // tomato
                .setDescription(error)
                .setFooter("There's no sense crying over every mistake")
                .build()
        )
        .build();
  }
}
