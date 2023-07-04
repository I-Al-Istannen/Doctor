package de.ialistannen.doctor.command;

import de.ialistannen.doctor.rendering.DocEmbedBuilder.DescriptionStyle;
import de.ialistannen.doctor.storage.ActiveMessages.ActiveMessage;
import java.util.Arrays;
import java.util.Optional;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public enum MessageCommand {
  COLLAPSE("⏫", "Collapse", "collapse", ButtonStyle.SECONDARY),
  EXPAND("⏬", "Expand", "expand", ButtonStyle.SECONDARY),
  REMOVE_TAGS("✂️", "Hide Tags", "remove_tags", ButtonStyle.SECONDARY),
  ADD_TAGS("📝", "Show Tags", "add_tags", ButtonStyle.SECONDARY),
  DELETE("🗑️", "Delete", "delete", ButtonStyle.DANGER);

  private final ButtonStyle buttonStyle;
  private final String icon;
  private final String label;
  private final String id;

  MessageCommand(String icon, String label, String id, ButtonStyle buttonStyle) {
    this.buttonStyle = buttonStyle;
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

  public ButtonStyle getButtonStyle() {
    return buttonStyle;
  }

  public Optional<ActiveMessage> update(ActiveMessage message) {
    return switch (this) {
      case COLLAPSE -> Optional.of(message.withDescriptionStyle(DescriptionStyle.SHORT));
      case EXPAND -> Optional.of(message.withDescriptionStyle(DescriptionStyle.LONG));
      case REMOVE_TAGS -> Optional.of(message.withTags(false));
      case ADD_TAGS -> Optional.of(message.withTags(true));
      case DELETE -> Optional.empty();
    };
  }

  public static Optional<MessageCommand> fromId(String id) {
    return Arrays.stream(values())
        .filter(it -> it.getId().equals(id))
        .findFirst();
  }

}

