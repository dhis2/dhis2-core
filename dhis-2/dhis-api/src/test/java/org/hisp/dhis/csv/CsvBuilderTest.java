/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.csv;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hisp.dhis.jsontree.Appender;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CsvBuilder}.
 *
 * <p>Focused on the writer's output, not on {@link CSV}'s reader — the two are covered separately
 * because they only need to agree on the common cases.
 */
class CsvBuilderTest {

  // ====== Basic output ======

  @Test
  void testEmptyStream_HeadersOnly() {
    assertEquals("a,b\n", csv(b -> b.addRows(List.of("a", "b"), Stream.empty())));
  }

  @Test
  void testSingleRow() {
    assertEquals(
        "a,b\n1,2\n",
        csv(b -> b.addRows(List.of("a", "b"), Stream.<Object[]>of(new Object[] {1, 2}))));
  }

  @Test
  void testMultipleRows() {
    assertEquals(
        "a,b\n1,2\n3,4\n",
        csv(
            b ->
                b.addRows(
                    List.of("a", "b"),
                    Stream.<Object[]>of(new Object[] {1, 2}, new Object[] {3, 4}))));
  }

  @Test
  void testSkipHeaders() {
    assertEquals(
        "1,2\n",
        csv(
            b ->
                b.skipHeaders(true)
                    .addRows(List.of("a", "b"), Stream.<Object[]>of(new Object[] {1, 2}))));
  }

  @Test
  void testNonArrayRow() {
    // A non-array value in the stream is treated as a single-field row
    assertEquals("a\nhello\n", csv(b -> b.addRows(List.of("a"), Stream.of("hello"))));
  }

  // ====== Quoting — triggers ======

  @Test
  void testPlainField_NotQuoted() {
    assertEquals(
        "a\nhello\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"hello"}))));
  }

  @Test
  void testFieldWithInteriorSpace_NotQuoted() {
    // Interior whitespace does not require quoting — the reader preserves it
    assertEquals(
        "a\nhello world\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"hello world"}))));
  }

  @Test
  void testFieldWithDelimiter_Quoted() {
    assertEquals(
        "a\n\"x,y\"\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"x,y"}))));
  }

  @Test
  void testFieldWithQuote_QuotedAndDoubled() {
    assertEquals(
        "a\n\"say \"\"hi\"\"\"\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"say \"hi\""}))));
  }

  @Test
  void testFieldWithLF_Quoted() {
    assertEquals(
        "a\n\"line1\nline2\"\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"line1\nline2"}))));
  }

  @Test
  void testFieldWithCR_Quoted() {
    assertEquals(
        "a\n\"line1\rline2\"\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"line1\rline2"}))));
  }

  @Test
  void testEmptyField_IsQuoted() {
    assertEquals(
        "a,b\n\"\",2\n",
        csv(b -> b.addRows(List.of("a", "b"), Stream.<Object[]>of(new Object[] {"", 2}))));
  }

  // ====== Custom delimiter ======

  @Test
  void testCustomDelimiter() {
    assertEquals(
        "a;b\n1;2\n",
        csv(
            b ->
                b.withDelimiter(';')
                    .addRows(List.of("a", "b"), Stream.<Object[]>of(new Object[] {1, 2}))));
  }

  @Test
  void testCustomDelimiter_CommaNotQuoted() {
    // With ';' delimiter, a comma in the value is no longer special
    assertEquals(
        "a\nx,y\n",
        csv(
            b ->
                b.withDelimiter(';')
                    .addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"x,y"}))));
  }

  @Test
  void testCustomDelimiter_SemicolonQuoted() {
    // With ';' delimiter, a semicolon in the value must be quoted
    assertEquals(
        "a\n\"x;y\"\n",
        csv(
            b ->
                b.withDelimiter(';')
                    .addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"x;y"}))));
  }

  // ====== Null handling ======

  @Test
  void testNull_DefaultWritesEmptyQuotes() {
    assertEquals(
        "a,b\n\"\",2\n",
        csv(b -> b.addRows(List.of("a", "b"), Stream.<Object[]>of(new Object[] {null, 2}))));
  }

  @Test
  void testNull_ExplicitNullsWritesNullLiteral() {
    assertEquals(
        "a,b\nnull,2\n",
        csv(
            b ->
                b.nullAsNull()
                    .addRows(List.of("a", "b"), Stream.<Object[]>of(new Object[] {null, 2}))));
  }

  @Test
  void testIsNullEmpty_Default() {
    CsvBuilder b = new CsvBuilder(Appender.ofUTF8(new ByteArrayOutputStream()));
    assertTrue(b.isNullEmpty());
  }

  @Test
  void testIsNullEmpty_ExplicitNulls() {
    CsvBuilder b = new CsvBuilder(Appender.ofUTF8(new ByteArrayOutputStream()));
    assertFalse(b.nullAsNull().isNullEmpty());
  }

  @Test
  void testNullAsEmpty_TogglesBackToDefault() {
    CsvBuilder b = new CsvBuilder(Appender.ofUTF8(new ByteArrayOutputStream()));
    assertTrue(b.nullAsNull().nullAsEmpty().isNullEmpty());
  }

  // ====== Value conversion ======

  @Test
  void testDate_Formatted() {
    Date date = Date.from(Instant.parse("2024-01-15T10:30:00Z"));
    String out = csv(b -> b.addRows(List.of("when"), Stream.<Object[]>of(new Object[] {date})));
    assertTrue(out.startsWith("when\n"), out);
    String dataRow = out.substring("when\n".length());
    assertTrue(dataRow.endsWith("\n"), out);
    // DateUtils.toIso8601 output varies but the year should be present
    assertTrue(dataRow.contains("2024"), "date row was: " + dataRow);
  }

  @Test
  void testCollection_SingleElementIsUnwrapped() {
    assertEquals(
        "a\nhello\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {List.of("hello")}))));
  }

  @Test
  void testCollection_EmptyBecomesEmptyQuotes() {
    assertEquals(
        "a\n\"\"\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {List.of()}))));
  }

  @Test
  void testCollection_FewNumbers_JoinedWithSpace() {
    assertEquals(
        "a\n1 2 3\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {List.of(1, 2, 3)}))));
  }

  @Test
  void testCollection_ManyNumbers_SummaryFormat() {
    assertEquals(
        "a\n(12 elements)\n",
        csv(
            b ->
                b.addRows(
                    List.of("a"),
                    Stream.<Object[]>of(
                        new Object[] {List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)}))));
  }

  @Test
  void testCollection_FewStrings_SummaryFormat() {
    // Few non-numbers are not joined — they fall through to summary
    assertEquals(
        "a\n(3 elements)\n",
        csv(
            b ->
                b.addRows(
                    List.of("a"), Stream.<Object[]>of(new Object[] {List.of("x", "y", "z")}))));
  }

  @Test
  void testFieldValueIsObjectArray() {
    // A column value that is itself an Object[] uses the same conversion as a Collection
    assertEquals(
        "a\n1 2 3\n",
        csv(
            b ->
                b.addRows(
                    List.of("a"), Stream.<Object[]>of(new Object[] {new Object[] {1, 2, 3}}))));
  }

  @Test
  void testFieldValueIsEmptyObjectArray() {
    assertEquals(
        "a\n\"\"\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {new Object[0]}))));
  }

  // ====== UTF-8 ======

  @Test
  void testUnicode_PlainField() {
    assertEquals(
        "a\nhéllo\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"héllo"}))));
  }

  @Test
  void testUnicode_QuotedField() {
    assertEquals(
        "a\n\"héllo, wörld\"\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"héllo, wörld"}))));
  }

  @Test
  void testUnicode_SupplementaryPlane() {
    // 😀 = U+1F600, 4 UTF-8 bytes
    assertEquals(
        "a\n😀\n", csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"😀"}))));
  }

  // ====== Chaining / preferences ======

  @Test
  void testChainedPreferences() {
    assertEquals(
        "null,2\n",
        csv(
            b ->
                b.skipHeaders(true)
                    .nullAsNull()
                    .addRows(List.of("a", "b"), Stream.<Object[]>of(new Object[] {null, 2}))));
  }

  @Test
  void testWithPreference_ExplicitNulls() {
    assertEquals(
        "a\nnull\n",
        csv(
            b ->
                b.with(CsvBuilder.Preference.EXPLICIT_NULLS)
                    .addRows(List.of("a"), Stream.<Object[]>of(new Object[] {null}))));
  }

  @Test
  void testWithoutPreference_SkipHeaders() {
    // no-op default state — explicitly removing a preference that wasn't set shouldn't break
    assertEquals(
        "a\n1\n",
        csv(
            b ->
                b.without(CsvBuilder.Preference.SKIP_HEADERS)
                    .addRows(List.of("a"), Stream.<Object[]>of(new Object[] {1}))));
  }

  // ====== Trimming ======

  @Test
  void testFieldStartingWithSpace_Trimmed() {
    assertEquals(
        "a\nhello\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {" hello"}))));
  }

  @Test
  void testFieldStartingWithTab_Trimmed() {
    assertEquals(
        "a\nhello\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"\thello"}))));
  }

  @Test
  void testTrailingSpace_Trimmed() {
    assertEquals(
        "a\nhello\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"hello "}))));
  }

  @Test
  void testTrailingTab_Trimmed() {
    assertEquals(
        "a\nhello\n",
        csv(b -> b.addRows(List.of("a"), Stream.<Object[]>of(new Object[] {"hello\t"}))));
  }

  // ====== Helpers ======

  private static String csv(Consumer<CsvBuilder> body) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Appender appender = Appender.ofUTF8(out)) {
      CsvBuilder builder = new CsvBuilder(appender);
      body.accept(builder);
    }
    return out.toString(UTF_8);
  }
}
