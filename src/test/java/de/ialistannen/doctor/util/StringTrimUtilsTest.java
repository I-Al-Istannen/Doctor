package de.ialistannen.doctor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringTrimUtilsTest {

  @Test
  void trimsNormalSentenceMaxLength() {
    String input = "Hello world, this is a nice little sentence!".strip();

    assertEquals(
        "Hello world, this is\n\n" +
            "*Skipped **the rest of the line**. Click `Expand` if you are intrigued.*",
        StringTrimUtils.trimMarkdown(input, 20, 1)
    );
  }

  @Test
  void trimsNormalSentenceMaxLines() {
    String input = "Hello\n world,\n this is a nice little sentence!".strip();

    assertEquals(
        "Hello\n\n"
            + "*Skipped **1** line. Click `Expand` if you are intrigued.*",
        StringTrimUtils.trimMarkdown(input, 2000, 1)
    );
  }

  @Test
  void keepCodeBlockIntact() {
    String input = """
        ```java
        class Foo {
        }
        ```""".strip();

    assertEquals(
        """
            ```java
            class Foo {
            }
            ```""",
        StringTrimUtils.trimMarkdown(input, 10, 1)
    );
  }

  @Test
  void cutBeforeCodeBlock() {
    String input = """
        Hallo Welt, this is cool!
        ```java
        class Foo {
        }
        ```""".strip();

    assertEquals(
        """
            Hallo Welt, this is cool!
            
            *Skipped **3** lines. Click `Expand` if you are intrigued.*""",
        StringTrimUtils.trimMarkdown(input, 100, 1)
    );
  }

  @Test
  void keepLinkIntact() {
    String input = """
        [Hello world](https://example.com)""".strip();

    assertEquals(
        """
            [Hello world](https://example.com)""",
        StringTrimUtils.trimMarkdown(input, 10, 1)
    );
  }

  @Test
  void trimsLong() {
    String input = """
        The `String` class represents character strings. All string literals in Java programs,
        such as `"abc"`, are implemented as instances of this class.
                
        Strings are constant; their values cannot be changed after they are created. String buffers
        support mutable strings. Because String objects are immutable they can be shared.
        For example:
                
        ```java
        String str = "abc";
        ```
                
        is equivalent to:
                
        ```java
        char data[] = {'a', 'b', 'c'};
        String str = new String(data);
        ```
                
        Here are some more examples of how strings can be used:
                
        ```java
        System.out.println("abc");
        String cde = "cde";
        System.out.println("abc" + cde);
        String c = "abc".substring(2, 3);
        String d = cde.substring(1, 2);
        ```
                
        The class `String` includes methods for examining individual characters of the sequence,
        for comparing strings, for searching strings, for extracting substrings, and for creating
        a copy of a string with all characters translated to uppercase or to lowercase.
        Case mapping is based on the Unicode Standard version specified by
        the [`Character`](/java.base/java/lang/Character.html) class.
                
        The Java language provides special support for the string concatenation operator ( + ),
        and for conversion of other objects to strings. For additional information on string
        concatenation and conversion, see _The Java Language Specification_.
                
        Unless otherwise noted, passing a `null` argument to a constructor or method in this class
        will cause a [`NullPointerException`](/java.base/java/lang/NullPointerException.html)
        to be thrown.
                
        A `String` represents a string in the UTF-16 format in which _supplementary characters_
        are represented by _surrogate pairs_ (see the section
        [Unicode Character Representations](Character.html#unicode) in the `Character` class for
        more information). Index values refer to `char` code units, so a supplementary character
        uses two positions in a `String`.
                
        The `String` class provides methods for dealing with Unicode code points (i.e., characters),
        in addition to those for dealing with Unicode code units (i.e., `char` values).
                
        Unless otherwise noted, methods for comparing Strings do not take locale into account.
        the [`Collator`](/java.base/java/text/Collator.html) class provides methods for finer-grain,
        locale-sensitive String comparison.""".strip();

    assertEquals(
        """
            The `String` class represents character strings. All string literals in Java programs,
            such as `"abc"`, are implemented as instances of this class.
            
            Strings are constant; their values cannot be changed after they are created. String buffers
            support mutable strings. Because String objects are immutable they can be shared.
            For example:
            
            ```java
            String str = "abc";
            ```
            
            is equivalent to:
            
            ```java
            char data[] = {'a', 'b', 'c'};
            String str = new String(data);
            ```
            
            *Skipped **37** lines. Click `Expand` if you are intrigued.*""",
        StringTrimUtils.trimMarkdown(input, 1000, 14)
    );
  }
}
