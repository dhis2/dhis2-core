/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.openapi;

import static org.hisp.dhis.webapi.openapi.OpenApiMarkdown.markdownToHTML;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/**
 * Tests the parsing and rendering of markdown as used as part of rendering OpenAPI as HTML.
 *
 * @author Jan Bernitt
 */
class OpenApiMarkdownTest {

  @Test
  void testPlain() {
    assertRenders("<p>hello</p>", "hello");
    assertRenders("<p>hello world</p>", "hello world");
    assertRenders("<p>hello world!</p>", "hello world!");
  }

  @Test
  void testPlain_Escape() {
    assertRenders(
        "<p>Plain text &amp; others are HTML escaped</p>", "Plain text & others are HTML escaped");
  }

  @Test
  void testItalic() {
    assertRenders("<p><em>hello</em></p>", "_hello_");
    assertRenders("<p>hello <em>world</em></p>", "hello _world_");
    assertRenders("<p>hello <em>world</em>!</p>", "hello _world_!");
  }

  @Test
  void testItalic2() {
    assertRenders("<p><em>hello</em></p>", "*hello*");
    assertRenders("<p>hello <em>world</em></p>", "hello *world*");
    assertRenders("<p>hello <em>world</em>!</p>", "hello *world*!");
  }

  @Test
  void testItalic_Escape() {
    assertRenders(
        "<p>Text inside a <em>italic &amp; others</em> is HTML escaped</p>",
        "Text inside a _italic & others_ is HTML escaped");
    assertRenders(
        "<p>Text inside a <em>italic &amp; others</em> is HTML escaped</p>",
        "Text inside a *italic & others* is HTML escaped");
  }

  @Test
  void testBold() {
    assertRenders("<p><strong>hello</strong></p>", "__hello__");
    assertRenders("<p>hello <strong>world</strong></p>", "hello __world__");
    assertRenders("<p>hello <strong>world</strong>!</p>", "hello __world__!");
  }

  @Test
  void testBold2() {
    assertRenders("<p><strong>hello</strong></p>", "**hello**");
    assertRenders("<p>hello <strong>world</strong></p>", "hello **world**");
    assertRenders("<p>hello <strong>world</strong>!</p>", "hello **world**!");
  }

  @Test
  void testBold_Escape() {
    assertRenders(
        "<p>Text inside a <strong>bold &amp; others</strong> is HTML escaped</p>",
        "Text inside a __bold & others__ is HTML escaped");
    assertRenders(
        "<p>Text inside a <strong>bold &amp; others</strong> is HTML escaped</p>",
        "Text inside a **bold & others** is HTML escaped");
  }

  @Test
  void testItalicAndBold() {
    assertRenders("<p><em><strong>hello</strong></em></p>", "___hello___");
    assertRenders("<p>hello <em><strong>world</strong></em></p>", "hello ___world___");
    assertRenders("<p>hello <em><strong>world</strong></em>!</p>", "hello ___world___!");
  }

  @Test
  void testItalicAndBold2() {
    assertRenders("<p><em><strong>hello</strong></em></p>", "***hello***");
    assertRenders("<p>hello <em><strong>world</strong></em></p>", "hello ***world***");
    assertRenders("<p>hello <em><strong>world</strong></em>!</p>", "hello ***world***!");
  }

  @Test
  void testItalicAndBold_Escape() {
    assertRenders(
        "<p>Text inside a <em><strong>bold &amp; italic</strong></em> is HTML escaped</p>",
        "Text inside a ___bold & italic___ is HTML escaped");
    assertRenders(
        "<p>Text inside a <em><strong>bold &amp; italic</strong></em> is HTML escaped</p>",
        "Text inside a ***bold & italic*** is HTML escaped");
  }

  @Test
  void testCode() {
    assertRenders("<p><code>hello</code></p>", "`hello`");
    assertRenders("<p>hello <code>world</code></p>", "hello `world`");
    assertRenders("<p>hello <code>world</code>!</p>", "hello `world`!");
  }

  @Test
  void testCode_Escape() {
    assertRenders(
        "<p>Text inside a <code>code &amp; others</code> is HTML escaped</p>",
        "Text inside a `code & others` is HTML escaped");
  }

  @Test
  void testHeading() {
    assertRenders("<h1>hello</h1>\n", "# hello");
    assertRenders("<h2>hello <code>world</code></h2>\n", "## hello `world`");
    assertRenders("<h3>hello <code>world</code>!</h3>\n", "### hello `world`!");
  }

  @Test
  void testBlockquote() {
    assertRenders("<blockquote>hello</blockquote>\n", "> hello");
    assertRenders("<blockquote>hello\nworld</blockquote>\n", "> hello\n> world");
    assertRenders(
        "<blockquote>hello\n<strong>strong</strong>\nworld</blockquote>\n",
        """
      > hello
      > **strong**\s
      > world""");
  }

  @Test
  void testListing_Star() {
    assertRenders("<ul>\n  <li>hello</li>\n  <li>world</li>\n</ul>\n", "* hello\n* world");
    assertRenders(
        """
      <ul>
        <li>hello
      world</li>
        <li>this is
      the second
      item</li>
      </ul>
      """,
        "* hello\n  world\n* this is\n  the second\n  item");
  }

  @Test
  void testListing_Dash() {
    assertRenders("<ul>\n  <li>hello</li>\n  <li>world</li>\n</ul>\n", "- hello\n- world");
    assertRenders(
        """
      <ul>
        <li>hello
      world</li>
        <li>this is
      the second
      item</li>
      </ul>
      """,
        "- hello\n  world\n- this is\n  the second\n  item");
  }

  @Test
  void testListing_Plus() {
    assertRenders("<ul>\n  <li>hello</li>\n  <li>world</li>\n</ul>\n", "+ hello\n+ world");
    assertRenders(
        """
      <ul>
        <li>hello
      world</li>
        <li>this is
      the second
      item</li>
      </ul>
      """,
        "+ hello\n  world\n+ this is\n  the second\n  item");
  }

  @Test
  void testListing_Hash() {
    assertRenders("<ol>\n  <li>hello</li>\n  <li>world</li>\n</ol>\n", "#. hello\n#. world");
    assertRenders(
        """
      <ol>
        <li>hello
      world</li>
        <li>this is
      the second
      item</li>
      </ol>
      """,
        "#. hello\n  world\n#. this is\n  the second\n  item");
  }

  @Test
  void testListing_Numbered() {
    assertRenders("<ol>\n  <li>hello</li>\n  <li>world</li>\n</ol>\n", "1. hello\n2. world");
    assertRenders(
        """
      <ol>
        <li>hello
      world</li>
        <li>this is
      the second
      item</li>
      </ol>
      """,
        "1. hello\n  world\n2. this is\n  the second\n  item");
  }

  @Test
  void testListing_Escape() {
    assertRenders("<ol>\n  <li>A great year!</li>\n</ol>\n", "1968. A great year!");
    assertRenders("<p>1968. A great year!</p>", "1968\\. A great year!");
  }

  @Test
  void testRuler() {
    assertRenders("<hr/>\n", "***");
    assertRenders("<hr/>\n", "  ***");
    assertRenders("<hr/>\n", "  ********   ");
    assertRenders("<hr/>\n", "---  ");
    assertRenders("<hr/>\n", " --------------------------- ");
    assertRenders("<hr/>\n", "_____________");
    assertRenders("<hr/>\n", "            ___        ");
  }

  @Test
  void testCodeBlock() {
    assertRenders("<pre>\n code\n</pre>\n", "```\n code\n```");
    assertRenders(
        "<pre lang='json'>\n&quot;indeed&quot;\n</pre>\n",
        """
         ```json
         "indeed"
         ```
         """);
  }

  @Test
  void testLink() {
    assertRenders(
        "<p>I love supporting the <strong><a target=\"_blank\" href=\"https://eff.org\">EFF</a></strong>.</p>",
        "I love supporting the **[EFF](https://eff.org)**.");
    assertRenders(
        "<p>See the section on <a target=\"_blank\" href=\"#code\"><code>code</code></a>.</p>",
        "See the section on [`code`](#code).");
  }

  @Test
  void testLink_Escape() {
    assertRenders(
        "<p>The <a target=\"_blank\" href=\"#yeah&#039;\">text &amp; more</a> is HTML escaped</p>",
        "The [text & more](#yeah') is HTML escaped");
  }

  @Test
  void testLinkShorthand() {
    assertRenders(
        "<p>More info at <a target=\"_blank\" href=\"https://www.markdownguide.org\">https://www.markdownguide.org</a> ;)</p>",
        "More info at <https://www.markdownguide.org> ;)");
  }

  @Test
  void testLinkShorthand_Escape() {
    assertRenders(
        "<p>The <a target=\"_blank\" href=\"#yeah&#039;\">#yeah&#039;</a> is HTML escaped</p>",
        "The <#yeah'> is HTML escaped");
  }

  @Test
  void testImage() {
    assertRenders(
        "<p>Look! The <img src=\"/assets/images/san-juan-mountains.jpg\" alt=\"The San Juan Mountains are beautiful! /> indeed.</p>",
        "Look! The ![The San Juan Mountains are beautiful!](/assets/images/san-juan-mountains.jpg) indeed.");
  }

  @Test
  void testHtmlEntities() {
    assertRenders(
        "<p>This is a simple text that has &amp; but also an entity &amp; and &#128049; in it</p>",
        "This is a simple text that has & but also an entity &amp; and &#128049; in it");
  }

  @Test
  void testMixed() {
    assertRenders(
        """
        <h2>A section in markdown</h2>
        <p>First we start with a paragraph, <em>highlighting</em> some stuff,
        and it has <code>code</code> too.</p>

        <blockquote>not to forget we also quote something here</blockquote>


        <ul>
          <li>and make a <strong>list</strong></li>
          <li>with a couple of <code>items</code></li>
        </ul>


        <hr/>
        <h3>Subsection</h3>
        <pre>
          and some code block
          lets see how it goes
        </pre>
        """,
        """
        ## A section in markdown
        First we start with a paragraph, _highlighting_ some stuff,
        and it has `code` too.

        > not to forget we also quote something here

        * and make a **list**
        * with a couple of `items`

            ---------------
        ### Subsection
        ```
          and some code block
          lets see how it goes
        ```
        """);
  }

  private static void assertRenders(
      @Language("html") String expected, @Language("markdown") String actual) {
    assertEquals(expected, markdownToHTML(actual));
  }
}
