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

import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.escapeHtml;
import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.stripHtml;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Test the HTML strip and escape functions in {@link OpenApiHtmlUtils}.
 *
 * @author Jan Bernitt
 */
class OpenApiHtmlUtilsTest {

  @Test
  void testEscapeHtml_Null() {
    assertEquals("", escapeHtml(null));
  }

  @Test
  void testEscapeHtml_Empty() {
    assertEquals("", escapeHtml(""));
  }

  @Test
  void testEscapeHtml_Blank() {
    assertEquals("  ", escapeHtml("  "));
  }

  @Test
  void testEscapeHtml_Plain() {
    assertEquals("This is just plain text", escapeHtml("This is just plain text"));
  }

  @Test
  void testEscapeHtml_AmpIsEscaped() {
    assertEquals("You&amp;me", escapeHtml("You&me"));
    assertEquals("&amp;Co Internationale", escapeHtml("&Co Internationale"));
    assertEquals("Now &amp;", escapeHtml("Now &"));
  }

  @Test
  void testEscapeHtml_LessThanIsEscaped() {
    assertEquals("1&lt;7", escapeHtml("1<7"));
    assertEquals("&lt; power", escapeHtml("< power"));
    assertEquals("I &lt;3 cats", escapeHtml("I <3 cats"));
    assertEquals("I &lt;3 cats &amp; dogs", escapeHtml("I <3 cats & dogs"));
  }

  @Test
  void testEscapeHtml_GreaterThanIsEscaped() {
    assertEquals("1&gt;7", escapeHtml("1>7"));
    assertEquals("&gt; power", escapeHtml("> power"));
    assertEquals("I &gt;&gt; cats", escapeHtml("I >> cats"));
    assertEquals("cats &gt;&gt; dogs &amp; horses", escapeHtml("cats >> dogs & horses"));
  }

  @Test
  void testEscapeHtml_SingleQuotesAreEscaped() {
    assertEquals(
        "&#039;Hey, must be a devil between us&#039;",
        escapeHtml("'Hey, must be a devil between us'"));
    assertEquals(
        "Now, &#039;Exit Music&#039; (for a film)", escapeHtml("Now, 'Exit Music' (for a film)"));
  }

  @Test
  void testEscapeHtml_DoubleQuotesAreEscaped() {
    assertEquals(
        "&quot;Hey, must be a devil between us&quot;",
        escapeHtml("\"Hey, must be a devil between us\""));
    assertEquals(
        "Now, &quot;Exit Music&quot; (for a film)", escapeHtml("Now, \"Exit Music\" (for a film)"));
  }

  @Test
  void testEscapeHtml_HtmlEntitiesAreNotEscaped() {
    assertEquals(
        "Remember to use &CounterClockwiseContourIntegral;",
        escapeHtml("Remember to use &CounterClockwiseContourIntegral;"));
    assertEquals(
        "Also the &EmptyVerySmallSquare; space",
        escapeHtml("Also the &EmptyVerySmallSquare; space"));
    assertEquals("Direction is &#x200E;!", escapeHtml("Direction is &#x200E;!"));
    assertEquals("Everyone loves &#128049;", escapeHtml("Everyone loves &#128049;"));
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
}
