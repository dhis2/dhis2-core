/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuoteUtilsTest {

  @Test
  void unquote_NullInput_ReturnsEmptyString() {
    assertEquals("", QuoteUtils.unquote(null));
  }

  @Test
  void unquote_EmptyString_ReturnsEmptyString() {
    assertEquals("", QuoteUtils.unquote(""));
  }

  @Test
  void unquote_BlankString_ReturnsEmptyString() {
    assertEquals("", QuoteUtils.unquote("   "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "1"})
  void unquote_SingleCharacter_ReturnsSameString(String input) {
    assertEquals(input, QuoteUtils.unquote(input));
  }

  @Test
  void unquote_DoubleQuotes_ReturnsUnquotedString() {
    assertEquals("hello", QuoteUtils.unquote("\"hello\""));
  }

  @Test
  void unquote_Backticks_ReturnsUnquotedString() {
    assertEquals("hello", QuoteUtils.unquote("`hello`"));
  }

  @Test
  void unquote_MismatchedQuotes_ReturnsSameString() {
    assertEquals("\"hello`", QuoteUtils.unquote("\"hello`"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"hello world", "hello\"world", "hello`world", "`hello\"", "\"hello`"})
  void unquote_NoMatchingQuotes_ReturnsSameString(String input) {
    assertEquals(input, QuoteUtils.unquote(input));
  }

  @Test
  void unquote_EmptyQuotes_ReturnsEmptyString() {
    assertEquals("", QuoteUtils.unquote("\"\""));
    assertEquals("", QuoteUtils.unquote("``"));
  }

  @Test
  void unquote_QuotesWithSpaces_ReturnsUnquotedString() {
    assertEquals("hello world", QuoteUtils.unquote("\"hello world\""));
    assertEquals("hello world", QuoteUtils.unquote("`hello world`"));
  }
}
