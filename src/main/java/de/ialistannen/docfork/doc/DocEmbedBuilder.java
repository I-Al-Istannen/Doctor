package de.ialistannen.docfork.doc;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import de.ialistannen.docfork.util.DeclarationFormatter;
import de.ialistannen.docfork.util.parsers.ParseError;
import de.ialistannen.javadocapi.model.JavadocElement;
import de.ialistannen.javadocapi.model.JavadocElement.DeclarationStyle;
import de.ialistannen.javadocapi.model.comment.JavadocCommentTag;
import de.ialistannen.javadocapi.model.types.JavadocField;
import de.ialistannen.javadocapi.model.types.JavadocMethod;
import de.ialistannen.javadocapi.model.types.JavadocType;
import de.ialistannen.javadocapi.model.types.JavadocType.Type;
import de.ialistannen.javadocapi.rendering.LinkResolveStrategy;
import de.ialistannen.javadocapi.rendering.MarkdownCommentRenderer;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class DocEmbedBuilder {

  private final EmbedBuilder embedBuilder;
  private final MarkdownCommentRenderer renderer;
  private final JavadocElement element;
  private final String baseUrl;
  private final DeclarationFormatter declarationFormatter;

  public DocEmbedBuilder(MarkdownCommentRenderer renderer, JavadocElement element, String baseUrl) {
    this.renderer = renderer;
    this.element = element;
    this.baseUrl = baseUrl;

    this.embedBuilder = new EmbedBuilder();
    this.declarationFormatter = new DeclarationFormatter(56);
  }

  public DocEmbedBuilder addDeclaration() {
    String declaration = element.getDeclaration(DeclarationStyle.SHORT);

    try {
      declaration = declarationFormatter.formatDeclaration(element);
    } catch (ParseError e) {
      System.err.println(e.getMessage());
    }

    embedBuilder.getDescriptionBuilder()
        .append("```java\n")
        .append(declaration)
        .append("\n```\n");
    return this;
  }

  public DocEmbedBuilder addShortDescription() {
    element.getComment()
        .ifPresent(comment -> embedBuilder.getDescriptionBuilder()
            .append(limitSize(
                renderer.render(comment.getShortDescription(), baseUrl),
                MessageEmbed.DESCRIPTION_MAX_LENGTH - embedBuilder.getDescriptionBuilder().length()
            ))
        );

    return this;
  }

  public DocEmbedBuilder addLongDescription() {
    element.getComment()
        .ifPresent(comment -> embedBuilder.getDescriptionBuilder()
            .append(limitSize(
                renderer.render(comment.getLongDescription(), baseUrl),
                MessageEmbed.DESCRIPTION_MAX_LENGTH - embedBuilder.getDescriptionBuilder().length()
            ))
        );

    return this;
  }

  public DocEmbedBuilder addTags() {
    element.getComment().ifPresent(comment -> {
      Map<String, List<JavadocCommentTag>> tags = comment.getTags()
          .stream()
          .collect(groupingBy(
              tag -> tag.getTagName() + tag.getArgument().map(it -> " " + it).orElse(""),
              toList()
          ));

      for (var entry : tags.entrySet()) {
        String title = entry.getKey();

        StringJoiner bodyJoiner = new StringJoiner(", ");
        for (int i = 0; i < entry.getValue().size(); i++) {
          JavadocCommentTag tag = entry.getValue().get(i);
          String rendered = renderer.render(tag.getContent(), baseUrl);
          if (bodyJoiner.length() + rendered.length() > MessageEmbed.VALUE_MAX_LENGTH) {
            if (i < entry.getValue().size() - 1) {
              bodyJoiner.add("... and more");
            }
            break;
          }
          bodyJoiner.add(rendered);
        }

        String body = limitSize(bodyJoiner.toString(), MessageEmbed.VALUE_MAX_LENGTH);
        embedBuilder.addField(
            title,
            body,
            shouldInlineTag(entry.getValue().get(0).getTagName(), body)
        );
      }
    });

    return this;
  }

  private boolean shouldInlineTag(String tagName, String rendered) {
    if (tagName.equals("implNote")) {
      return false;
    }

    return rendered.replaceAll("(\\[.+?])\\(.+?\\)", "$1").length() <= 100;
  }

  public DocEmbedBuilder addIcon(LinkResolveStrategy linkResolveStrategy) {
    String iconUrl = displayDatas()
        .stream()
        .filter(it -> it.matches(element))
        .findFirst()
        .map(ElementTypeDisplayData::getIconUrl)
        .orElse("");

    embedBuilder.setAuthor(
        element.getQualifiedName().asStringWithModule(),
        linkResolveStrategy.resolveLink(element.getQualifiedName(), baseUrl),
        iconUrl
    );

    return this;
  }

  public DocEmbedBuilder addColor() {
    displayDatas().stream()
        .filter(it -> it.matches(element))
        .findFirst()
        .map(ElementTypeDisplayData::getColor)
        .ifPresent(embedBuilder::setColor);

    return this;
  }

  public DocEmbedBuilder addFooter(String source) {
    embedBuilder.setFooter("Query resolved from index '" + source + "'");

    return this;
  }

  public MessageEmbed build() {
    return embedBuilder.build();
  }

  private String limitSize(String input, int max) {
    if (input.length() <= max) {
      return input;
    }
    return input.substring(0, max - 3) + "...";
  }

  private List<ElementTypeDisplayData> displayDatas() {
    return List.of(
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/Groovy.icons.groovy.abstractClass@2x.png",
            isType(it -> it.getType() == Type.CLASS && it.getModifiers().contains("abstract"))
        ),
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.exceptionClass.svg@2x.png",
            isType(it -> it.getType() == Type.CLASS && it.getQualifiedName()
                .asString()
                .endsWith("Exception"))
        ),
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/Groovy.icons.groovy.class@2x.png",
            isType(it -> it.getType() == Type.CLASS)
        ),
//        new ElementTypeDisplayData(
//            new Color(255, 99, 71), // tomato
//            "https://intellij-icons.jetbrains.design/icons/AllIcons/nodes/annotationtype.svg",
//            isType(it -> it.getType() == Type.ANNOTATION)
//        ),
        new ElementTypeDisplayData(
            new Color(102, 51, 153), // rebecca purple
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.enum.svg@2x.png",
            isType(it -> it.getType() == Type.ENUM)
        ),
        new ElementTypeDisplayData(
            Color.GREEN,
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.interface.svg@2x.png",
            isType(it -> it.getType() == Type.INTERFACE)
        ),
        // Fallback
        new ElementTypeDisplayData(
            new Color(255, 99, 71), // tomato
            "https://www.jetbrains.com/help/img/idea/2019.1/Groovy.icons.groovy.class@2x.png",
            isType(it -> true)
        ),
//        new ElementTypeDisplayData(
//            Color.YELLOW,
//            "https://intellij-icons.jetbrains.design/icons/AllIcons/nodes/abstractMethod.svg",
//            isMethod(it -> it.getModifiers().contains("abstract"))
//        ),
        new ElementTypeDisplayData(
            Color.YELLOW,
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.method.svg@2x.png",
            isMethod(it -> true)
        ),
        new ElementTypeDisplayData(
            new Color(65, 105, 225), // royal blue,
            "https://www.jetbrains.com/help/img/idea/2019.1/icons.nodes.field.svg@2x.png",
            isField(it -> true)
        )
    );
  }

  private static class ElementTypeDisplayData {

    private final Color color;
    private final String iconUrl;
    private final Predicate<JavadocElement> predicate;

    private ElementTypeDisplayData(Color color, String iconUrl,
        Predicate<JavadocElement> predicate) {
      this.color = color;
      this.iconUrl = iconUrl;
      this.predicate = predicate;
    }

    public Color getColor() {
      return color;
    }

    public String getIconUrl() {
      return iconUrl;
    }

    public boolean matches(JavadocElement element) {
      return predicate.test(element);
    }
  }

  private static Predicate<JavadocElement> isType(Predicate<JavadocType> inner) {
    return element -> element instanceof JavadocType && inner.test((JavadocType) element);
  }

  private static Predicate<JavadocElement> isMethod(Predicate<JavadocMethod> inner) {
    return element -> element instanceof JavadocMethod && inner.test((JavadocMethod) element);
  }

  private static Predicate<JavadocElement> isField(Predicate<JavadocField> inner) {
    return element -> element instanceof JavadocField && inner.test((JavadocField) element);
  }
}
