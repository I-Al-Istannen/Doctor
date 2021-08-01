package de.ialistannen.doctor.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class MessageDataStore {

  private final Map<String, ActiveInteractions> interactions;
  private final Map<String, BotReply> botReplies;

  public MessageDataStore() {
    this.interactions = new LinkedHashMap<>() {
      @Override
      protected boolean removeEldestEntry(Entry<String, ActiveInteractions> eldest) {
        return size() > 60;
      }
    };

    this.botReplies = new LinkedHashMap<>() {
      @Override
      protected boolean removeEldestEntry(Entry<String, BotReply> eldest) {
        return size() > 60;
      }
    };
  }

  /**
   * Adds a new active interaction to this store.
   *
   * @param id the id of the interaction. Typically this is the message or slash command
   *     interaction id we can use to retrieve it later
   * @param interaction the interaction to store
   */
  public synchronized void addActiveInteraction(String id, ActiveInteractions interaction) {
    interactions.put(id, interaction);
  }

  public synchronized Optional<ActiveInteractions> getActiveInteraction(String id) {
    return Optional.ofNullable(interactions.get(id));
  }

  public synchronized void removeActiveInteraction(String id) {
    interactions.remove(id);
  }

  public synchronized void addReply(String messageId, BotReply reply) {
    botReplies.put(messageId, reply);
  }

  public synchronized void removeReply(String messageId) {
    botReplies.remove(messageId);
  }

  public synchronized Optional<BotReply> getReply(String messageId) {
    return Optional.ofNullable(botReplies.get(messageId));
  }
}
