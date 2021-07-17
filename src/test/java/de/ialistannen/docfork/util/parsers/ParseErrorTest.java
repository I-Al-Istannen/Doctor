package de.ialistannen.docfork.util.parsers;

import static de.ialistannen.docfork.util.parsers.ArgumentParsers.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ParseErrorTest {

  @Test
  void testErrorStartsAtLine() {
    StringReader reader = new StringReader("Hello world, this is quite nice");
    literal("Hello world").parse(reader);

    ParseError error = assertThrows(ParseError.class,
        () -> literal("this is quite nice").parse(reader).getOrThrow()
    );

    assertEquals(
        """
            Hello world, this is quite nice
                       ^
            Expected <this is quite nice>""",
        error.toString()
    );
  }

  @Test
  void testErrorCentered() {
    StringReader reader = new StringReader("Hello              world, this is quite nice");
    literal("Hello              world").parse(reader);

    ParseError error = assertThrows(ParseError.class,
        () -> literal("this is quite nice").parse(reader).getOrThrow()
    );

    assertEquals(
        """
            Hello              world, this is quite nice
                                    ^
                      Expected <this is quite nice>""",
        error.toString()
    );
  }
}
