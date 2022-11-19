package de.ialistannen.doctor.rendering;

import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedField;
import de.ialistannen.javadocbpi.model.elements.DocumentedMethod;
import de.ialistannen.javadocbpi.model.elements.DocumentedModule;
import de.ialistannen.javadocbpi.model.elements.DocumentedPackage;
import de.ialistannen.javadocbpi.model.elements.DocumentedType;
import de.ialistannen.javadocbpi.model.elements.DocumentedType.Type;
import java.awt.Color;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import spoon.reflect.declaration.ModifierKind;

public class FormatUtils {

  public static Emoji getEmoji(DocumentedElement element) {
    return switch (element) {
      case DocumentedField ignored -> Emoji.fromFormatted("<:Field:871140776346791987>");
      case DocumentedMethod ignored -> Emoji.fromFormatted("<:Method:871140776711708743>");
      case DocumentedType type -> getEmoji(type);
      case DocumentedModule ignored -> Emoji.fromUnicode("🧱");
      case DocumentedPackage ignored -> Emoji.fromUnicode("📦");
    };
  }

  private static Emoji getEmoji(DocumentedType type) {
    if (type.isException()) {
      return Emoji.fromFormatted("<:Exception:871325719127547945>");
    }

    return switch (type.type()) {
      case ANNOTATION -> Emoji.fromFormatted("<:Annotation:871325719563751444>");
      case ENUM -> Emoji.fromFormatted("<:Enum:871325719362412594>");
      case INTERFACE -> Emoji.fromFormatted("<:Interface:871325719576318002>");
      case CLASS -> Emoji.fromFormatted("<:Class:871140776900440074>");
      case RECORD -> Emoji.fromUnicode("📀");
    };
  }

  public static Optional<ElementTypeDisplayData> getCosmeticData(DocumentedElement element) {
    return typeCosmeticData().stream()
        .filter(it -> it.matches(element))
        .findFirst();
  }

  public static List<ElementTypeDisplayData> typeCosmeticData() {
    return List.of(
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/Groovy.icons.groovy.abstractClass@2x.png",
            isType(it -> it.type() == Type.CLASS && it.modifiers().contains(ModifierKind.ABSTRACT))
        ),
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.exceptionClass.svg@2x.png",
            isType(DocumentedType::isException)
        ),
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/Groovy.icons.groovy.class@2x.png",
            isType(it -> it.type() == Type.CLASS)
        ),
        new ElementTypeDisplayData(
            new Color(102, 51, 153), // rebecca purple
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.enum.svg@2x.png",
            isType(it -> it.type() == Type.ENUM)
        ),
        new ElementTypeDisplayData(
            Color.GREEN,
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.interface.svg@2x.png",
            isType(it -> it.type() == Type.INTERFACE)
        ),
        // Fallback
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/Groovy.icons.groovy.class@2x.png",
            isType(it -> true)
        ),
        new ElementTypeDisplayData(
            Color.YELLOW,
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.method.svg@2x.png",
            isMethod(it -> true)
        ),
        new ElementTypeDisplayData(
            new Color(65, 105, 225), // royal blue,
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.field.svg@2x.png",
            isField(it -> true)
        ),
        new ElementTypeDisplayData(
            new Color(128, 128, 128), // gray,
            "https://resources.jetbrains.com/help/img/idea/2019.1/icons.nodes.package.svg@2x.png",
            element -> element instanceof DocumentedPackage
        ),
        new ElementTypeDisplayData(
            new Color(128, 128, 128), // gray,
            "https://resources.jetbrains.com/help/img/idea/2019.1/icons.nodes.package.svg@2x.png",
            element -> element instanceof DocumentedModule
        )
    );
  }


  public record ElementTypeDisplayData(
      Color color,
      String iconUrl,
      Predicate<DocumentedElement> predicate
  ) {

    public boolean matches(DocumentedElement element) {
      return predicate.test(element);
    }
  }

  private static Predicate<DocumentedElement> isType(Predicate<DocumentedType> inner) {
    return element -> element instanceof DocumentedType && inner.test((DocumentedType) element);
  }

  private static Predicate<DocumentedElement> isMethod(Predicate<DocumentedMethod> inner) {
    return element -> element instanceof DocumentedMethod && inner.test((DocumentedMethod) element);
  }

  private static Predicate<DocumentedElement> isField(Predicate<DocumentedField> inner) {
    return element -> element instanceof DocumentedField && inner.test((DocumentedField) element);
  }
}
