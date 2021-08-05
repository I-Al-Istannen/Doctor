package de.ialistannen.doctor.util.nameproxies;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocType;
import java.util.List;

public class NameProxyType extends JavadocType {

  public NameProxyType(QualifiedName name) {
    super(
        name,
        List.of(),
        List.of(),
        null,
        List.of(),
        List.of(),
        Type.CLASS,
        List.of(),
        null
    );
  }
}
