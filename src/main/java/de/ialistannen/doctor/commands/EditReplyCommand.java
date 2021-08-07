package de.ialistannen.doctor.commands;

import static de.ialistannen.doctor.util.parsers.ArgumentParsers.literal;
import static de.ialistannen.doctor.util.parsers.ArgumentParsers.word;

import de.ialistannen.doctor.commands.system.ButtonCommandSource;
import de.ialistannen.doctor.commands.system.Command;
import de.ialistannen.doctor.commands.system.CommandContext;
import de.ialistannen.doctor.commands.system.CommandSource;
import de.ialistannen.doctor.doc.DocResultSender;
import de.ialistannen.doctor.messages.MessageSender;
import de.ialistannen.doctor.state.BotReply;
import de.ialistannen.doctor.state.MessageDataStore;
import de.ialistannen.doctor.util.parsers.ArgumentParser;
import de.ialistannen.javadocapi.util.BaseUrlElementLoader;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public class EditReplyCommand implements Command {

  private final MessageDataStore dataStore;
  private final DocResultSender resultSender;

  public EditReplyCommand(MessageDataStore dataStore, DocResultSender resultSender) {
    this.dataStore = dataStore;
    this.resultSender = resultSender;
  }

  @Override
  public ArgumentParser<?> keyword() {
    return literal("edit-reply");
  }

  @Override
  public void handle(CommandContext commandContext, CommandSource source, MessageSender sender) {
    sender.editOrReply("Don't snoop, this isn't for you!").queue();
  }

  @Override
  public void handle(CommandContext context, ButtonCommandSource source, MessageSender sender) {
    Optional<BotReply> reply = dataStore.getReply(source.getEvent().getMessageId());

    if (reply.isEmpty()) {
      source.getEvent().reply("I think I forgot what this does :(").setEphemeral(true).queue();
      return;
    }
    BotReply botReply = reply.get();

    if (!botReply.getSource().getAuthorId().equals(source.getAuthorId())) {
      source.getEvent().reply("This is not for you to decide...").setEphemeral(true).queue();
      return;
    }

    Optional<MessageCommand> reaction = MessageCommand.fromId(context.shift(word()));

    if (reaction.isEmpty()) {
      source.getEvent().reply("I have no idea what you want :/").setEphemeral(true).queue();
      return;
    }

    boolean shortDescription = botReply.isShortDescription();
    boolean omitTags = botReply.isOmitTags();

    switch (reaction.get()) {
      case DELETE -> {
        sender.delete().queue();
        dataStore.removeReply(source.getEvent().getMessageId());
        return;
      }
      case COLLAPSE -> shortDescription = true;
      case EXPAND -> shortDescription = false;
      case REMOVE_TAGS -> omitTags = true;
      case ADD_TAGS -> omitTags = false;
    }

    resultSender.replyWithResult(
        botReply.getSource(),
        sender,
        botReply.getElement(),
        shortDescription,
        omitTags,
        Duration.ofMillis(0),
        ((BaseUrlElementLoader) botReply.getLoader()).getLinkResolveStrategy()
    );
  }

  public enum MessageCommand {
    COLLAPSE("\u23EB", "Collapse", "collapse"),
    EXPAND("\u23EC", "Expand", "expand"),
    REMOVE_TAGS("\u2702\uFE0F", "Remove Tags", "remove_tags"),
    ADD_TAGS("\uD83D\uDCDD", "Add Tags", "add_tags"),
    DELETE("\uD83D\uDDD1️", "Delete", "delete");

    private final String icon;
    private final String label;
    private final String id;

    MessageCommand(String icon, String label, String id) {
      this.icon = icon;
      this.label = label;
      this.id = id;
    }

    public String getIcon() {
      return icon;
    }

    public String getLabel() {
      return label;
    }

    public String getId() {
      return id;
    }

    public static Optional<MessageCommand> fromId(String id) {
      return Arrays.stream(values())
          .filter(it -> it.getId().equals(id))
          .findFirst();
    }
  }

}
