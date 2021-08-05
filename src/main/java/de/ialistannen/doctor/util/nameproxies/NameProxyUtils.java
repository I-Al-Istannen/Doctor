package de.ialistannen.doctor.util.nameproxies;

import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.PossiblyGenericType;
import java.util.List;

public class NameProxyUtils {

  public static PossiblyGenericType unknownType() {
    return new PossiblyGenericType(
        new QualifiedName("Unknown"), List.of()
    );
  }

  public static JavadocElement forName(QualifiedName name) {
    if (name.isMethod()) {
      return new NameProxyMethod(name);
    }
    if (name.asString().contains("#")) {
      return new NameProxyField(name);
    }
    return new NameProxyType(name);
  }
}
