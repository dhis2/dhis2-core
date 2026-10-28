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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.appendEscaped;
import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.stripHtml;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import org.hisp.dhis.jsontree.Appender;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.TextBuilder;
import org.junit.jupiter.api.Test;

/**
 * Test the HTML strip and escape functions in {@link OpenApiHtmlUtils}.
 *
 * @author Jan Bernitt
 */
class OpenApiHtmlUtilsTest {

  @Test
  void testEscapeHtml_Null() {
    assertEscapedEquals("", "");
  }

  @Test
  void testEscapeHtml_Empty() {
    assertEscapedEquals("", "");
  }

  @Test
  void testEscapeHtml_Blank() {
    assertEscapedEquals("  ", "  ");
  }

  @Test
  void testEscapeHtml_Plain() {
    assertEscapedEquals("This is just plain text", "This is just plain text");
  }

  @Test
  void testEscapeHtml_AmpIsEscaped() {
    assertEscapedEquals("You&amp;me", "You&me");
    assertEscapedEquals("&amp;Co Internationale", "&Co Internationale");
    assertEscapedEquals("Now &amp;", "Now &");
  }

  @Test
  void testEscapeHtml_LessThanIsEscaped() {
    assertEscapedEquals("1&lt;7", "1<7");
    assertEscapedEquals("&lt; power", "< power");
    assertEscapedEquals("I &lt;3 cats", "I <3 cats");
    assertEscapedEquals("I &lt;3 cats &amp; dogs", "I <3 cats & dogs");
  }

  @Test
  void testEscapeHtml_GreaterThanIsEscaped() {
    assertEscapedEquals("1&gt;7", "1>7");
    assertEscapedEquals("&gt; power", "> power");
    assertEscapedEquals("I &gt;&gt; cats", "I >> cats");
    assertEscapedEquals("cats &gt;&gt; dogs &amp; horses", "cats >> dogs & horses");
  }

  @Test
  void testEscapeHtml_SingleQuotesAreEscaped() {
    assertEscapedEquals(
        "&#039;Hey, must be a devil between us&#039;", "'Hey, must be a devil between us'");
    assertEscapedEquals(
        "Now, &#039;Exit Music&#039; (for a film)", "Now, 'Exit Music' (for a film)");
  }

  @Test
  void testEscapeHtml_DoubleQuotesAreEscaped() {
    assertEscapedEquals(
        "&quot;Hey, must be a devil between us&quot;", "\"Hey, must be a devil between us\"");
    assertEscapedEquals(
        "Now, &quot;Exit Music&quot; (for a film)", "Now, \"Exit Music\" (for a film)");
  }

  @Test
  void testEscapeHtml_HtmlEntitiesAreNotEscaped() {
    assertEscapedEquals(
        "Remember to use &CounterClockwiseContourIntegral;",
        "Remember to use &CounterClockwiseContourIntegral;");
    assertEscapedEquals(
        "Also the &EmptyVerySmallSquare; space", "Also the &EmptyVerySmallSquare; space");
    assertEscapedEquals("Direction is &#x200E;!", "Direction is &#x200E;!");
    assertEscapedEquals("Everyone loves &#128049;", "Everyone loves &#128049;");
  }

  @Test
  void testStripHtml_Null() {
    assertEquals("", stripHtml(null));
  }

  @Test
  void testStripHtml_Empty() {
    assertEquals("", stripHtml(""));
  }

  @Test
  void testStripHtml_Blank() {
    assertEquals("  ", stripHtml("  "));
  }

  @Test
  void testStripHtml_Escapes() {
    assertEquals(
        "Any of , , , , and  will be stripped!",
        stripHtml("Any of <, >, \", ', and & will be stripped!"));
  }

  @Test
  void testEscapeHtml_SupplementaryPlane_UTF8_FastPath() {
    // Regression: appendEncoded(char) writes one char at a time, so a surrogate pair on a
    // UTF-8 sink becomes two U+FFFD. The fast path (no '&' in input) is affected because
    // the unescaped default branch calls appendEncoded(c) per char.
    assertEscapedUtf8("a \uD83D\uDE00 &lt;b&gt;", "a \uD83D\uDE00 <b>");
  }

  @Test
  void testEscapeHtml_SupplementaryPlane_UTF8_SlowPath() {
    // Same bug in the entity-preservation path — mixing '&' with a supplementary character
    // still goes through the per-char default branch for the emoji.
    assertEscapedUtf8("a \uD83D\uDE00 &amp; &lt;b&gt;", "a \uD83D\uDE00 & <b>");
  }

  @Test
  void testEscapeHtml_SupplementaryPlane_UTF8_Multiple() {
    // Multiple supplementary characters with escapes interleaved — pins down that the bug
    // is not boundary-specific
    assertEscapedUtf8("\uD83D\uDE00&lt;\uD83C\uDF89&gt;", "\uD83D\uDE00<\uD83C\uDF89>");
  }

  /**
   * Asserts the escaped output against a UTF-8 round-trip. Unlike {@link #assertEscapedEquals},
   * this exercises the same encode path as production {@code Appender.ofUTF8} sinks and is the only
   * way to observe surrogate-pair corruption — {@link TextBuilder} preserves surrogates as raw
   * {@code char} values and masks the bug.
   */
  private void assertEscapedUtf8(String expected, String actual) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (Appender out = Appender.ofUTF8(bytes)) {
      appendEscaped(Text.of(actual), out);
    }
    assertEquals(expected, bytes.toString(UTF_8));
  }

  private void assertEscapedEquals(String expected, String actual) {
    TextBuilder escaped = new TextBuilder();
    appendEscaped(Text.of(actual), escaped);
    assertEquals(expected, escaped.toString());
  }
}
