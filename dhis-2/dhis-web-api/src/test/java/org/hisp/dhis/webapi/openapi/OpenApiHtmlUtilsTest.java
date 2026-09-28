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

import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.appendEscaped;
import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.stripHtml;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.Text;
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

  private void assertEscapedEquals(String expected, String actual) {
    StringBuilder escaped = new StringBuilder();
    appendEscaped(Text.of(actual), escaped);
    assertEquals(expected, escaped.toString());
  }
}
