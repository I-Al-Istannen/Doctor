package de.ialistannen.doctor.rendering;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import de.ialistannen.doctor.rendering.FormatUtils.ElementTypeDisplayData;
import de.ialistannen.doctor.util.ParseError;
import de.ialistannen.javadocbpi.model.elements.DocumentedElement;
import de.ialistannen.javadocbpi.model.elements.DocumentedElementReference;
import de.ialistannen.javadocbpi.rendering.DeclarationRenderer;
import de.ialistannen.javadocbpi.rendering.MarkdownRenderer;
import de.ialistannen.javadocbpi.rendering.links.LinkResolver;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.javadoc.api.StandardJavadocTagType;
import spoon.javadoc.api.elements.JavadocBlockTag;
import spoon.javadoc.api.elements.JavadocElement;

public class DocEmbedBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(DocEmbedBuilder.class);

  private final EmbedBuilder embedBuilder;
  private final MarkdownRenderer renderer;
  private final DocumentedElement element;
  private final DocumentedElementReference reference;
  private final String baseUrl;
  private final DeclarationFormatter declarationFormatter;
  private final DeclarationRenderer declarationRenderer;

  public DocEmbedBuilder(
      MarkdownRenderer renderer,
      DocumentedElement element,
      DocumentedElementReference reference,
      String baseUrl
  ) {
    this.renderer = renderer;
    this.element = element;
    this.reference = reference;
    this.baseUrl = baseUrl;

    this.embedBuilder = new EmbedBuilder();
    this.declarationRenderer = new DeclarationRenderer();
    this.declarationFormatter = new DeclarationFormatter(56, declarationRenderer);
  }

  public DocEmbedBuilder addDeclaration() {
    String declaration = declarationRenderer.renderDeclaration(element);

    try {
      declaration = declarationFormatter.formatDeclaration(element);
    } catch (ParseError e) {
      LOGGER.warn("Error formatting declaration", e);
    }

    embedBuilder.getDescriptionBuilder()
        .append("```java\n")
        .append(declaration)
        .append("\n```\n");
    return this;
  }

  public DocEmbedBuilder addDescription(DescriptionStyle style) {
    switch (style) {
      case SHORT -> addShortDescription();
      case LONG -> addLongDescription();
    }
    return this;
  }

  public DocEmbedBuilder addShortDescription() {
    embedBuilder.getDescriptionBuilder()
        .append(limitSize(
            renderParagraphs(element.javadoc(), 800, 8),
            MessageEmbed.DESCRIPTION_MAX_LENGTH - embedBuilder.getDescriptionBuilder().length()
        ));

    return this;
  }

  public DocEmbedBuilder addLongDescription() {
    embedBuilder.getDescriptionBuilder()
        .append(limitSize(
            renderParagraphs(element.javadoc(), Integer.MAX_VALUE, Integer.MAX_VALUE),
            MessageEmbed.DESCRIPTION_MAX_LENGTH - embedBuilder.getDescriptionBuilder().length()
        ));
    return this;
  }

  private String renderParagraphs(List<JavadocElement> elements, int maxLength, int maxNewlines) {
    int currentNewlineCount = 0;
    StringBuilder result = new StringBuilder();

    for (JavadocElement javadocElement : elements) {
      if (javadocElement instanceof JavadocBlockTag) {
        continue;
      }
      String rendered = renderer.render(javadocElement, baseUrl);
      int newNewlines = (int) rendered.chars().filter(it -> it == '\n').count();

      boolean tooManyNewlines = currentNewlineCount + newNewlines > maxNewlines;
      boolean tooLong = result.length() + rendered.length() > maxLength;

      if (tooLong || tooManyNewlines) {
        break;
      }
      result.append(rendered);
      currentNewlineCount += newNewlines;
    }

    return result.toString();
  }

  public DocEmbedBuilder addTags(boolean showTags) {
    if (!showTags) {
      return this;
    }
    Map<String, List<JavadocBlockTag>> tags = element.javadoc()
        .stream()
        .filter(it -> it instanceof JavadocBlockTag)
        .map(it -> (JavadocBlockTag) it)
        .collect(groupingBy(
            this::fieldTitle,
            toList()
        ));

    for (var entry : tags.entrySet()) {
      String title = entry.getKey();

      StringJoiner bodyJoiner = new StringJoiner(", ");
      for (int i = 0; i < entry.getValue().size(); i++) {
        JavadocBlockTag tag = entry.getValue().get(i);
        int freeSpace = MessageEmbed.VALUE_MAX_LENGTH - bodyJoiner.length();
        List<JavadocElement> elements = tag.getElements();
        if (useBlockTagArgumentInTitle(tag)) {
          elements = elements.subList(1, elements.size());
        }
        bodyJoiner.add(renderParagraphs(elements, freeSpace, Integer.MAX_VALUE));
      }

      String body = limitSize(bodyJoiner.toString(), MessageEmbed.VALUE_MAX_LENGTH);
      embedBuilder.addField(
          title,
          body,
          shouldInlineTag(entry.getValue().get(0).getTagType().getName(), body)
      );
    }

    return this;
  }

  private String fieldTitle(JavadocBlockTag tag) {
    if (!(tag.getTagType() instanceof StandardJavadocTagType)) {
      return tag.getTagType().getName();
    }
    boolean useArgument = useBlockTagArgumentInTitle(tag);
    if (!useArgument) {
      return tag.getTagType().getName();
    }
    return tag.getTagType().getName() + " " + renderer.render(tag.getElements().get(0), baseUrl);
  }

  private static boolean useBlockTagArgumentInTitle(JavadocBlockTag tag) {
    if (!(tag.getTagType() instanceof StandardJavadocTagType type)) {
      return false;
    }
    return switch (type) {
      case AUTHOR, DEPRECATED, HIDDEN, SERIAL_DATA, SINCE,
          SERIAL, SERIAL_FIELD, USES, VERSION, RETURN -> false;
      case EXCEPTION, PARAM, PROVIDES, SEE, THROWS -> true;
      default -> throw new IllegalStateException("Unexpected value: " + type);
    };
  }

  private boolean shouldInlineTag(String tagName, String rendered) {
    if (tagName.equals("implNote")) {
      return false;
    }

    return rendered.replaceAll("(\\[.+?])\\(.+?\\)", "$1").length() <= 100;
  }

  public DocEmbedBuilder addIcon(LinkResolver linkResolver) {
    String iconUrl = FormatUtils.getCosmeticData(element)
        .map(ElementTypeDisplayData::iconUrl)
        .orElse("");

    String link = linkResolver.resolve(reference, baseUrl).replace(" ", "%20");
    embedBuilder.setAuthor(
        StringUtils.abbreviateMiddle(
            reference.asQualifiedName(),
            "...",
            MessageEmbed.AUTHOR_MAX_LENGTH
        ),
        link,
        iconUrl
    );

    return this;
  }

  public DocEmbedBuilder addColor() {
    FormatUtils.getCosmeticData(element)
        .map(ElementTypeDisplayData::color)
        .ifPresent(embedBuilder::setColor);

    return this;
  }

  public DocEmbedBuilder addFooter(String source, Duration queryDuration) {
    embedBuilder.setFooter(
        "Query resolved from index '" + source + "' in " + queryDuration.toMillis() + "ms"
    );

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

  public enum DescriptionStyle {
    SHORT,
    LONG
  }

}
