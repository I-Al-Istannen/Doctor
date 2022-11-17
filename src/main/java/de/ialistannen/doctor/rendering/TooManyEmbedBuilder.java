package de.ialistannen.doctor.rendering;

import de.ialistannen.doctor.storage.ActiveMessages;
import de.ialistannen.doctor.storage.ActiveMessages.ActiveChooser;
import de.ialistannen.doctor.storage.MultiFileStorage;
import de.ialistannen.doctor.storage.MultiFileStorage.FetchResult;
import de.ialistannen.javadocbpi.util.NameShortener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component.Type;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.AbstractMessageBuilder;

public class TooManyEmbedBuilder {

  private final static int MAX_ROWS = 5;
  private final MultiFileStorage storage;
  private final ActiveMessages activeMessages;

  public TooManyEmbedBuilder(MultiFileStorage storage, ActiveMessages activeMessages) {
    this.storage = storage;
    this.activeMessages = activeMessages;
  }

  public void buildMessage(
      String ownerId,
      Collection<String> qualifiedNames,
      AbstractMessageBuilder<?, ?> builder
  ) throws SQLException, IOException {
    Set<String> potentialMatches = qualifiedNames.stream()
        .distinct()
        .limit((long) Type.BUTTON.getMaxPerRow() * MAX_ROWS)
        .collect(Collectors.toSet());

    Map<String, String> qualifiedNameLabelMap = new NameShortener()
        .shortenMatches(potentialMatches);

    List<ActionRow> rows = new ArrayList<>();
    for (var chunk : chunk(qualifiedNameLabelMap.entrySet(), MAX_ROWS)) {
      System.out.println(chunk);
      List<ItemComponent> components = new ArrayList<>();
      for (var entry : chunk) {
        buildButton(ownerId, entry.getKey(), entry.getValue())
            .ifPresent(components::add);
      }
      if (!components.isEmpty()) {
        rows.add(ActionRow.of(components));
      }
    }
    builder.setComponents(rows);
  }

  private Optional<Button> buildButton(
      String ownerId,
      String qualifiedName,
      String label
  ) throws SQLException, IOException {
    Optional<FetchResult> result = storage.get(qualifiedName);
    if (result.isEmpty()) {
      return Optional.empty();
    }

    String id = activeMessages.registerChooser(new ActiveChooser(
        ownerId,
        result.get().reference().asQualifiedName()
    ));

    return Optional.of(
        Button.of(ButtonStyle.PRIMARY, id, label)
            .withEmoji(FormatUtils.getEmoji(result.get().element()))
    );
  }


  private static <T> Collection<List<T>> chunk(Collection<T> input, int chunkSize) {
    var holder = new Object() {
      int counter;
    };

    return input.stream()
        .sequential()
        .collect(Collectors.groupingBy(
            t -> (holder.counter++) / chunkSize
        ))
        .values();
  }
}
