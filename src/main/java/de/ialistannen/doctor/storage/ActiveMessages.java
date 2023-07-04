package de.ialistannen.doctor.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.ialistannen.doctor.rendering.DocEmbedBuilder.DescriptionStyle;
import java.util.Optional;
import java.util.UUID;

public class ActiveMessages {

  private final Cache<String, ActiveChooser> activeChoosers;
  private final Cache<String, ActiveMessage> activeMessages;

  public ActiveMessages() {
    this.activeChoosers = Caffeine.newBuilder()
        .maximumSize(1000)
        .build();
    this.activeMessages = Caffeine.newBuilder()
        .maximumSize(1000)
        .build();
  }

  public String registerChooser(ActiveChooser chooser) {
    String id = UUID.randomUUID().toString();
    activeChoosers.put(id, chooser);
    return id;
  }

  public Optional<ActiveChooser> lookupChooser(String id) {
    return Optional.ofNullable(activeChoosers.getIfPresent(id));
  }

  public ActiveMessage registerMessage(String messageId, ActiveMessage message) {
    activeMessages.put(messageId, message);
    return message;
  }

  public Optional<ActiveMessage> lookupMessage(String messageId) {
    return Optional.ofNullable(activeMessages.getIfPresent(messageId));
  }

  public void deleteMessage(String messageId) {
    activeMessages.invalidate(messageId);
  }

  public record ActiveChooser(
      String ownerId,
      String qualifiedName
  ) {

  }

  public record ActiveMessage(
      String ownerId,
      String qualifiedName,
      DescriptionStyle descriptionStyle,
      boolean tags,
      boolean expandable
  ) {

    public ActiveMessage withDescriptionStyle(DescriptionStyle style) {
      return new ActiveMessage(ownerId(), qualifiedName(), style, tags(), expandable);
    }

    public ActiveMessage withTags(boolean tags) {
      return new ActiveMessage(ownerId(), qualifiedName(), descriptionStyle(), tags, expandable);
    }

    public ActiveMessage withExpandable(boolean expandable) {
      return new ActiveMessage(ownerId(), qualifiedName(), descriptionStyle(), tags, expandable);
    }

    public static ActiveMessage of(String ownerId, String qualifiedName) {
      return new ActiveMessage(ownerId, qualifiedName, DescriptionStyle.SHORT, false, true);
    }
  }

}
