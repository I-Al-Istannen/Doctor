package de.ialistannen.doctor.commands.system;

public interface CommandSource {

  /**
   * Returns the id of the source. This can be an interaction id, button click id, message id or
   * something similar.
   *
   * @return the id of this source
   */
  String getId();

  /**
   * @return the id of the author
   */
  String getAuthorId();
}
