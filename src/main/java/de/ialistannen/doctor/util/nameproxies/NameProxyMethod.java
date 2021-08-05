package de.ialistannen.doctor.util.nameproxies;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.types.JavadocMethod;
import de.ialistannen.javadocapi.model.types.PossiblyGenericType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NameProxyMethod extends JavadocMethod {

  public NameProxyMethod(QualifiedName name) {
    super(
        name,
        NameProxyUtils.unknownType(),
        List.of(),
        extractParameters(name),
        List.of(),
        List.of(),
        List.of(),
        null
    );
  }

  private static List<Parameter> extractParameters(QualifiedName name) {
    String asString = name.asString();
    String parameterPart = asString.substring(asString.indexOf('(') + 1, asString.length() - 1);

    return Arrays.stream(parameterPart.split(","))
        .map(String::strip)
        .map(type -> new Parameter(
            new PossiblyGenericType(new QualifiedName(type), List.of()),
            "unknown"
        ))
        .collect(Collectors.toList());
  }
}
