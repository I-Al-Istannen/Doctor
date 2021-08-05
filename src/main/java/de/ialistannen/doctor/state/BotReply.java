package de.ialistannen.doctor.state;

import de.ialistannen.doctor.commands.system.CommandSource;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.storage.ElementLoader;
import de.ialistannen.javadocapi.storage.ElementLoader.LoadResult;

public class BotReply {

  private final ElementLoader loader;
  private final LoadResult<JavadocElement> element;
  private final CommandSource source;
  private final boolean shortDescription;
  private final boolean omitTags;

  public BotReply(ElementLoader loader, LoadResult<JavadocElement> element, CommandSource source,
      boolean shortDescription,
      boolean omitTags) {
    this.loader = loader;
    this.element = element;
    this.source = source;
    this.shortDescription = shortDescription;
    this.omitTags = omitTags;
  }

  public LoadResult<JavadocElement> getElement() {
    return element;
  }

  public CommandSource getSource() {
    return source;
  }

  public ElementLoader getLoader() {
    return loader;
  }

  public boolean isShortDescription() {
    return shortDescription;
  }

  public boolean isOmitTags() {
    return omitTags;
  }
}
