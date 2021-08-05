package de.ialistannen.doctor.util.nameproxies;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocField;
import java.util.List;

public class NameProxyField extends JavadocField {

  public NameProxyField(QualifiedName qualifiedName) {
    super(qualifiedName, List.of(), NameProxyUtils.unknownType(), null);
  }
}
