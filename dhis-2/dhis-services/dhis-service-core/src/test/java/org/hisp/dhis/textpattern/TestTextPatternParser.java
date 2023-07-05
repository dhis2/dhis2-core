/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.textpattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TestTextPatternParser {

  private final String EXAMPLE_TEXT_SEGMENT = "\"Hello world!\"";

  private final String EXAMPLE_TEXT_SEGMENT_WITH_ESCAPED_QUOTES =
      "\"This is an \\\"escaped\\\" text\"";

  private final String EXAMPLE_SEQUENTIAL_SEGMENT = "SEQUENTIAL(#)";

  private final String EXAMPLE_RANDOM_SEGMENT = "RANDOM(#Xx*)";

  @Test
  void testParseNullExpressionThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class, () -> TextPatternParser.parse(null));
  }

  @Test
  void testParseEmptyExpressionThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class, () -> TextPatternParser.parse(""));
  }

  @Test
  void testParseWhitespaceOnlyExpressionThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class, () -> TextPatternParser.parse("   "));
  }

  @Test
  void testParseWithUnexpectedPlusThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class, () -> TextPatternParser.parse("+"));
  }

  @Test
  void testParseWithInvalidInputThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class, () -> TextPatternParser.parse("Z"));
  }

  @Test
  void testParseBadTextSegment() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> TextPatternParser.parse("\"This segment has no end"));
  }

  @Test
  void testParseTextSegment() throws TextPatternParser.TextPatternParsingException {
    testParseOK(EXAMPLE_TEXT_SEGMENT, TextPatternMethod.TEXT);
  }

  @Test
  void testParseTextWithEscapedQuotes() throws TextPatternParser.TextPatternParsingException {
    testParseOK(EXAMPLE_TEXT_SEGMENT_WITH_ESCAPED_QUOTES, TextPatternMethod.TEXT);
  }

  @Test
  void testParseSequentialSegment() throws TextPatternParser.TextPatternParsingException {
    testParseOK(EXAMPLE_SEQUENTIAL_SEGMENT, TextPatternMethod.SEQUENTIAL);
  }

  @Test
  void testParseSequentialSegmentInvalidPatternThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> testParseOK("SEQUENTIAL(X)", TextPatternMethod.SEQUENTIAL));
  }

  @Test
  void testParseSequentialSegmentWithNoEndThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> testParseOK("SEQUENTIAL(#", TextPatternMethod.SEQUENTIAL));
  }

  @Test
  void testParseSequentialSegmentWithNoPatternThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> testParseOK("SEQUENTIAL()", TextPatternMethod.SEQUENTIAL));
  }

  @Test
  void testParseRandomSegment() throws TextPatternParser.TextPatternParsingException {
    testParseOK(EXAMPLE_RANDOM_SEGMENT, TextPatternMethod.RANDOM);
  }

  @Test
  void testParseRandomSegmentInvalidPatternThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> testParseOK("RANDOM(S)", TextPatternMethod.RANDOM));
  }

  @Test
  void testParseRandomSegmentWithNoEndThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> testParseOK("RANDOM(#", TextPatternMethod.RANDOM));
  }

  @Test
  void testParseRandomSegmentWithNoPatternThrowsException() {
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> testParseOK("RANDOM()", TextPatternMethod.RANDOM));
  }

  @Test
  void testParseFullValidExpression() throws TextPatternParser.TextPatternParsingException {
    String TEXT_1 = "\"ABC\"";
    String SEPARATOR = "\"-\"";
    String SEQUENTIAL = "SEQUENTIAL(###)";
    String expression = String.format(" %s + %s + %s", TEXT_1, SEPARATOR, SEQUENTIAL);
    TextPattern textPattern = TextPatternParser.parse(expression);
    assertNotNull(textPattern);
    List<TextPatternSegment> segments = textPattern.getSegments();
    assertEquals(segments.size(), 3);
    assertEquals(segments.get(0).getMethod(), TextPatternMethod.TEXT);
    assertEquals(segments.get(1).getMethod(), TextPatternMethod.TEXT);
    assertEquals(segments.get(2).getMethod(), TextPatternMethod.SEQUENTIAL);
  }

  @Test
  void testParsePatternEndWithJoinThrowsException() {
    String pattern = "RANDOM(#) + ";
    assertThrows(
        TextPatternParser.TextPatternParsingException.class,
        () -> TextPatternParser.parse(pattern));
  }

  @Test
  void testCompletePatternOK() throws TextPatternParser.TextPatternParsingException {
    String pattern = "ORG_UNIT_CODE() + CURRENT_DATE(yyyy) + RANDOM(###) + \"-OK\"";
    TextPattern textPattern = TextPatternParser.parse(pattern);
    List<TextPatternSegment> segments = textPattern.getSegments();
    assertEquals(4, segments.size());
    assertEquals(segments.get(0).getMethod(), TextPatternMethod.ORG_UNIT_CODE);
    assertEquals(segments.get(1).getMethod(), TextPatternMethod.CURRENT_DATE);
    assertEquals(segments.get(2).getMethod(), TextPatternMethod.RANDOM);
    assertEquals(segments.get(3).getMethod(), TextPatternMethod.TEXT);
  }

  private void testParseOK(String input, TextPatternMethod method)
      throws TextPatternParser.TextPatternParsingException {
    TextPattern result = TextPatternParser.parse(input);
    assertNotNull(result);
    List<TextPatternSegment> segments = result.getSegments();
    assertEquals(segments.size(), 1);
    assertEquals(segments.get(0).getRawSegment(), input);
    assertEquals(segments.get(0).getMethod(), method);
  }
}
