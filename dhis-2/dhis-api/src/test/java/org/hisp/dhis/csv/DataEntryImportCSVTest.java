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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/**
 * Tests reading data value import CSV into {@link DataEntryValue.Input} with {@link CSV}: line
 * endings, quoting and header handling.
 *
 * <p>Up to 2.42 data value CSV was read with javacsv which handled all of these (DHIS2-22466).
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class DataEntryImportCSVTest {

  @Test
  void testWindowsLineEndings() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value\r
                de123456789,202501,ou123456789,1\r
                de123456789,202502,ou123456789,2\r
                de123456789,202503,ou123456789,3\r
                """);
    assertEquals(List.of("1", "2", "3"), values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testOldMacLineEndings() {
    List<DataEntryValue.Input> values =
        read(
            "dataElement,period,orgUnit,value\r"
                + "de123456789,202501,ou123456789,1\r"
                + "de123456789,202502,ou123456789,2\r");
    assertEquals(List.of("1", "2"), values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testNoTrailingLineBreak() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value
                de123456789,202501,ou123456789,1
                de123456789,202502,ou123456789,2""");
    assertEquals(List.of("1", "2"), values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testBlankLinesSkipped() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value
                de123456789,202501,ou123456789,1

                de123456789,202502,ou123456789,2

                """);
    assertEquals(List.of("1", "2"), values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testHeaderOnly() {
    assertEquals(List.of(), read("dataElement,period,orgUnit,value\n"));
  }

  @Test
  void testEscapedQuotes() {
    List<DataEntryValue.Input> values =
        read(
            """
        dataElement,period,orgUnit,value,comment
        de123456789,202501,ou123456789,5,"say ""hi"", ok"
        de987654321,202501,ou123456789,6,""
        """);
    assertEquals(2, values.size());
    assertEquals("say \"hi\", ok", values.get(0).comment());
    assertEquals("5", values.get(0).value());
    assertEquals("", values.get(1).comment());
    assertEquals("6", values.get(1).value());
  }

  @Test
  void testQuotedLineBreak() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value,comment\r
                de123456789,202501,ou123456789,5,"line1\r
                line2"\r
                de987654321,202501,ou123456789,6,x\r
                """);
    assertEquals(2, values.size());
    assertEquals("line1\r\nline2", values.get(0).comment());
    assertEquals("6", values.get(1).value());
  }

  @Test
  void testUnterminatedQuote() {
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                dataElement,period,orgUnit,value
                de123456789,202501,ou123456789,"5
                """));
    assertEquals(
        "CSV input error at line 2 column 4: Quoted value is not terminated", ex.getMessage());
  }

  @Test
  void testEmptyValuesAreNull() {
    List<DataEntryValue.Input> values =
        read(
            """
        dataElement,period,orgUnit,categoryOptionCombo,value,comment
        de123456789,202501,ou123456789,  ,7,
        """);
    assertNull(values.get(0).categoryOptionCombo());
    assertNull(values.get(0).categoryOptions());
    assertNull(values.get(0).comment());
    assertEquals("7", values.get(0).value());
  }

  @Test
  void testValuesKeptVerbatim() {
    List<DataEntryValue.Input> values =
        read(
            """
        dataElement,period,orgUnit,value
        de123456789,202501,ou123456789,3.14
        de123456789,202501,ou123456789,-1e3
        de123456789,202501,ou123456789,"007"
        de123456789,202501,ou123456789," padded "
        """);
    assertEquals(
        List.of("3.14", "-1e3", "007", " padded "),
        values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testBooleansCaseInsensitive() {
    List<DataEntryValue.Input> values =
        read(
            """
        dataElement,period,orgUnit,value,followUp,deleted
        de123456789,202501,ou123456789,1,TRUE,True
        de123456789,202501,ou123456789,1,false,FALSE
        """);
    assertEquals(true, values.get(0).followUp());
    assertEquals(true, values.get(0).deleted());
    assertEquals(false, values.get(1).followUp());
    assertEquals(false, values.get(1).deleted());
  }

  /** Excel "Save as CSV" pads empty rows with commas. */
  @Test
  void testEmptyRecordsSkipped() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value\r
                de123456789,202501,ou123456789,1\r
                ,,,\r
                  ,\t, ,\r
                de123456789,202502,ou123456789,2\r
                ,,,\r
                """);
    assertEquals(List.of("1", "2"), values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testRecordWithQuotedEmptyValueNotSkipped() {
    List<DataEntryValue.Input> values = read("dataElement,period,orgUnit,value\n,,,\"\"\n");
    assertEquals(1, values.size());
    assertEquals("", values.get(0).value());
  }

  @Test
  void testHeaderParsedLikeRecords() {
    List<DataEntryValue.Input> values =
        read(
            """
                \r
                 note_extra ,"comment" ,dataElement,period,orgUnit,value\r
                x,y,de123456789,202501,ou123456789,5\r
                """);
    assertEquals(1, values.size());
    assertEquals("de123456789", values.get(0).dataElement());
    assertEquals("5", values.get(0).value());
  }

  @Test
  void testNoHeader() {
    IllegalArgumentException ex =
        assertThrowsExactly(IllegalArgumentException.class, () -> read("\n\r\n"));
    assertEquals("CSV input error at line 3 column 1: No header row found", ex.getMessage());
  }

  @Test
  void testUnterminatedQuoteReportsLine() {
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                        dataElement,period,orgUnit,value,comment

                        de123456789,202501,ou123456789,1,"a
                        b"
                        de123456789,202502,ou123456789,2,"open
                        de123456789,202503,ou123456789,3,x
                        """));
    assertEquals(
        "CSV input error at line 5 column 5: Quoted value is not terminated", ex.getMessage());
  }

  @Test
  void testQuotedValueTooLong() {
    String huge = "x".repeat(CSV.DEFAULT_MAX_VALUE_LENGTH + 1);
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read("dataElement,period,orgUnit,value\nde123456789,202501,ou123456789,\"" + huge));
    assertEquals(
        "CSV input error at line 2 column 4: Value longer than %d"
            .formatted(CSV.DEFAULT_MAX_VALUE_LENGTH),
        ex.getMessage());
  }

  @Test
  void testQuotedValueAtMaxLength() {
    int maxLenInQuotes = CSV.DEFAULT_MAX_VALUE_LENGTH - 3;
    String max = "x".repeat(maxLenInQuotes);
    List<DataEntryValue.Input> values =
        read(
            "dataElement,period,orgUnit,comment\nde123456789,202501,ou123456789,\"" + max + "\"\n");
    assertEquals(maxLenInQuotes, values.get(0).comment().length());
  }

  @Test
  void testUnquotedValueAtMaxLength() {
    // we need to be able to find the comma or end of line
    // so 1 character is lost
    int maxLen = CSV.DEFAULT_MAX_VALUE_LENGTH - 1;
    String max = "x".repeat(maxLen);
    List<DataEntryValue.Input> values =
        read("dataElement,period,orgUnit,comment\nde123456789,202501,ou123456789," + max + "\n");
    assertEquals(max, values.get(0).comment());
  }

  @Test
  void testUnquotedValuesAreTrimmed() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement , period ,orgUnit,value
                  de123456789  ,\t202501 \t, ou123456789,  5  \s
                """);
    DataEntryValue.Input v0 = values.get(0);
    assertEquals("de123456789", v0.dataElement());
    assertEquals("202501", v0.period());
    assertEquals("ou123456789", v0.orgUnit());
    assertEquals("5", v0.value());
  }

  @Test
  void testAdditionalColumnsThrow() {
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                        dataElement,period,orgUnit,value
                        de123456789,202501,ou123456789,1,extra
                        """));
    assertEquals("CSV input error at line 2 column 5: Additional columns found", ex.getMessage());
  }

  @Test
  void testMultipleMaxLengthValuesInSameRow() {
    int maxLen = CSV.DEFAULT_MAX_VALUE_LENGTH - 1;
    String max = "x".repeat(maxLen);
    List<DataEntryValue.Input> values =
        read(
            "dataElement,period,orgUnit,value,comment\n"
                + "de123456789,202501,ou123456789,"
                + max
                + ","
                + max
                + "\n");
    DataEntryValue.Input v0 = values.get(0);
    assertEquals(max, v0.value());
    assertEquals(max, v0.comment());
  }

  @Test
  void testMaxLengthHeaderNameAndValue() {
    int maxLen = CSV.DEFAULT_MAX_VALUE_LENGTH - 1;
    String max = "x".repeat(maxLen);
    List<DataEntryValue.Input> values =
        read(
            "dataElement,period,orgUnit,value,"
                + max
                + "\n"
                + "de123456789,202501,ou123456789,"
                + max
                + ",ignored\n");
    assertEquals(max, values.get(0).value());
    assertEquals("de123456789", values.get(0).dataElement());
  }

  @Test
  void testHeaderOnlyNoTrailingLineBreak() {
    assertEquals(List.of(), read("dataElement,period,orgUnit,value"));
  }

  @Test
  void testShortRowDoesNotReusePreviousRowValues() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value
                de123456789,202501,ou123456789,1
                de987654321,202502
                """);
    assertEquals(2, values.size());
    // previous row had non-null values for these...
    DataEntryValue.Input v0 = values.get(0);
    assertEquals("ou123456789", v0.orgUnit());
    assertEquals("1", v0.value());
    // ...but the short row must not inherit them
    DataEntryValue.Input v1 = values.get(1);
    assertEquals("de987654321", v1.dataElement());
    assertEquals("202502", v1.period());
    assertNull(v1.orgUnit());
    assertNull(v1.value());
  }

  @Test
  void testShortRowAtEndOfFileMissingColumnsAreNull() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value
                de123456789,202501,ou123456789,1
                de987654321,202502"""); // no trailing newline
    assertEquals(2, values.size());
    DataEntryValue.Input v1 = values.get(1);
    assertNull(v1.orgUnit());
    assertNull(v1.value());
  }

  @Test
  void testBackslashEscapesForOtherCharsPreservedVerbatim() {
    // No unescaping at all: \\ stays as \\, \n stays as the two chars \ and n
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,comment
                de123456789,202501,ou123456789,"a\\\\b\\nc"
                """);
    assertEquals("a\\\\b\\nc", values.get(0).comment());
  }

  // ----------------------------------------------------------------
  // A '"' that appears mid-value (after non-whitespace content) is a
  // malformed field. It is not a valid opening quote because the field
  // was already started unquoted.

  @Test
  void testQuoteInsideUnquotedValueRejected() {
    // currently: throws "Quoted value is not terminated" because skipQuotes
    // scans the rest of the file looking for a closing quote that never
    // comes. After fix: quote is caught at the point it is seen.
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                    dataElement,period,orgUnit,value
                    de123456789,202501,ou123456789,ab"cd
                    """));
    assertEquals(
        "CSV input error at line 2 column 4: Quote inside unquoted value", ex.getMessage());
  }

  @Test
  void testQuoteInsideUnquotedValueWithMatchingQuoteRejected() {
    // currently: silently parses value as the literal string  ab"cd"
    // (6 chars including both quotes) with no error. After fix: throws
    // before the mid-field quote is consumed.
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                    dataElement,period,orgUnit,value
                    de123456789,202501,ou123456789,ab"cd"
                    """));
    assertEquals(
        "CSV input error at line 2 column 4: Quote inside unquoted value", ex.getMessage());
  }

  @Test
  void testQuoteInsideUnquotedHeaderRejected() {
    // currently: throws "Quoted value is not terminated" while scanning
    // forward from the stray quote. After fix: errors at the header cell.
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                    dataElement,per"iod,orgUnit,value
                    de123456789,202501,ou123456789,1
                    """));
    assertEquals(
        "CSV input error at line 1 column 2: Quote inside unquoted value", ex.getMessage());
  }

  // ----------------------------------------------------------------
  // After a quoted field's closing quote, only whitespace may precede the
  // delimiter (comma or line break). Any other character is a malformed
  // field: the writer either forgot a delimiter or emitted stray bytes.

  @Test
  void testContentAfterClosingQuoteRejected() {
    // currently: silently splits into two fields ["abc", "def"] and
    // shifts the remaining columns. After fix: throws at the offending
    // character.
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                    dataElement,period,orgUnit,value
                    de123456789,202501,ou123456789,"abc"def
                    """));
    assertEquals(
        "CSV input error at line 2 column 4: Unexpected character after closing quote",
        ex.getMessage());
  }

  @Test
  void testContentAfterClosingQuoteInHeaderRejected() {
    // currently: header parses as ["data", "Element", "period", ...],
    // mangling the column names. After fix: throws during header read,
    // before any data rows are examined.
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                    "data"Element,period,orgUnit,value
                    de123456789,202501,ou123456789,1
                    """));
    assertEquals(
        "CSV input error at line 1 column 1: Unexpected character after closing quote",
        ex.getMessage());
  }

  @Test
  void testNonWhitespaceAfterClosingQuoteAndWhitespaceRejected() {
    // Leading whitespace does not save it — the first non-whitespace char
    // after the closing quote must still be a delimiter or line break.
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    """
                    dataElement,period,orgUnit,value,comment
                    de123456789,202501,ou123456789,"abc"   def,x
                    """));
    assertEquals(
        "CSV input error at line 2 column 4: Unexpected character after closing quote",
        ex.getMessage());
  }

  // ------------------------------------------------- boundary validations

  @Test
  void testWhitespaceAfterClosingQuoteStillAllowed() {
    // whitespace between closing quote and comma is tolerated (Javadoc:
    // "Space and tab between the closing quote and the delimiter or line
    // end are tolerated").
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value,comment
                de123456789,202501,ou123456789,"abc"  ,x
                """);
    assertEquals("abc", values.get(0).value());
    assertEquals("x", values.get(0).comment());
  }

  @Test
  void testQuotedValueFollowedByLineBreakStillAllowed() {
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value
                de123456789,202501,ou123456789,"abc"
                de123456789,202502,ou123456789,"def"
                """);
    assertEquals(List.of("abc", "def"), values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testQuotedValueAtEndOfFileStillAllowed() {
    // closing quote is the last char in the file — no trailing newline
    List<DataEntryValue.Input> values =
        read("dataElement,period,orgUnit,value\n" + "de123456789,202501,ou123456789,\"abc\"");
    assertEquals("abc", values.get(0).value());
  }

  @Test
  void testCompleteRowAtExactBufferBoundary() {
    // Buffer in CSV.Reader is 2048 chars. A 2048-char CSV with no trailing
    // newline forces a second read() after the initial fill; StringReader
    // returns -1, which the current fillBufferTail mishandles (len ends up
    // keepLen - 1, dropping the last char).
    record Row(String v) {}

    StringBuilder csv = new StringBuilder("v\n");
    while (csv.length() < 2046) csv.append("X\n"); // 2 chars per row
    csv.append("XY"); // last row, no trailing \n
    assertEquals(2048, csv.length());

    List<Row> rows = CSV.of(csv.toString()).as(Row.class).list();
    assertEquals(1023, rows.size());
    assertEquals("XY", rows.get(1022).v());
  }

  @Test
  void testShortReadsFromChunkedReader() {
    record Row(String v) {}

    StringBuilder csv = new StringBuilder("v\n");
    for (int i = 0; i < 1000; i++) csv.append("X\n");
    String text = csv.toString();

    java.io.Reader chunked =
        new java.io.Reader() {
          private final StringReader delegate = new StringReader(text);

          @Override
          public int read(char[] cbuf, int off, int len) throws IOException {
            return delegate.read(cbuf, off, Math.min(100, len));
          }

          @Override
          public void close() throws IOException {
            delegate.close();
          }
        };

    List<Row> rows = CSV.of(chunked).as(Row.class).list();
    assertEquals(1000, rows.size());
  }

  @Test
  void testBoundaryQuoteWithTrailingWhitespaceAtBufferEnd() {
    record Row(String a, String b) {}

    // Close quote at index 2047, whitespace fills to the end of the buffer,
    // then the separator + y arrive in the refill. Exercises the
    // skipIndent(p) == len && !eoi path in skipValue.
    // Prefix "a,b\n" = 4 chars, close quote at 2047 → 2042 x's.
    // Whitespace "     " (5) brings us to 2053... no, that's past the buffer.
    // Adjust: close quote earlier, so whitespace ends exactly at 2047.
    // "a,b\n" + `"` + 2041 x's + `"` + 1 space = 2048.
    String csv = "a,b\n\"" + "x".repeat(2041) + "\" ,y\r\n";

    List<Row> rows = CSV.of(csv).as(Row.class).list();
    assertEquals(1, rows.size());
    assertEquals(2041, rows.get(0).a().length());
    assertEquals("y", rows.get(0).b());
  }

  /**
   * RFC 4180 has no backslash escaping: a backslash inside a quoted value has no special meaning
   * and a value may end in one. Before the backslash-escape feature was removed, the closing quote
   * following the trailing backslash was consumed as escaped, and the whole file failed with
   * "Quoted value is not terminated" (S2).
   */
  @Test
  void testQuotedValueEndingInBackslash() {
    // Value:  C:\data, dir\
    // CSV:    "C:\data, dir\"
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value,comment
                de123456789,202501,ou123456789,5,"C:\\data, dir\\"
                """);
    assertEquals(1, values.size());
    assertEquals("C:\\data, dir\\", values.get(0).comment());
    assertEquals("5", values.get(0).value());
  }

  @Test
  void testQuotedValueIsSingleTrailingBackslash() {
    // Value: C:\   CSV: "C:\"
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value,comment
                de123456789,202501,ou123456789,5,"C:\\"
                """);
    assertEquals(1, values.size());
    assertEquals("C:\\", values.get(0).comment());
  }

  @Test
  void testBackslashDoesNotInterfereWithDoubledQuotes() {
    // Value:  say \"hi\"
    // CSV:    "say \""hi\"""   (the "" pairs are RFC 4180 doubled quotes)
    List<DataEntryValue.Input> values =
        read(
            """
                dataElement,period,orgUnit,value,comment
                de123456789,202501,ou123456789,5,"say \\""hi\\""\"
                """);
    assertEquals(1, values.size());
    assertEquals("say \\\"hi\\\"", values.get(0).comment());
  }

  // ---------------------------------------------------------------- S1
  // Header validation: every header cell must be non-null, non-empty,
  // unique (case-insensitively, matching columnToComponentIndexMapping),
  // and a syntactically valid Java identifier.

  @Test
  void testEmptyHeaderNameAtStartRejected() {
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of(",b,c\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 1: Empty header name", ex.getMessage());
  }

  @Test
  void testEmptyHeaderNameInMiddleRejected() {
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,,c\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 2: Empty header name", ex.getMessage());
  }

  @Test
  void testEmptyHeaderNameAtEndRejected() {
    // Two trailing commas: the parser treats the single trailing comma as
    // end-of-row but a *double* comma yields one empty middle cell.
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,b,,\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 3: Empty header name", ex.getMessage());
  }

  @Test
  void testWhitespaceOnlyHeaderNameRejected() {
    // Unquoted whitespace-only cell is trimmed to null by the CSV reader,
    // so it hits the same "empty" check as an unquoted empty cell.
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,  ,c\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 2: Empty header name", ex.getMessage());
  }

  @Test
  void testQuotedEmptyHeaderNameRejected() {
    // Quoted "" produces the empty string, not null — different code path
    // in value(), but still rejected.
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,\"\",c\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 2: Empty header name", ex.getMessage());
  }

  @Test
  void testDuplicateHeaderNameRejected() {
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,b,b\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 3: Duplicate header name: b", ex.getMessage());
  }

  @Test
  void testDuplicateHeaderNameCaseInsensitiveRejected() {
    // b and B both resolve to component "b" via the lowercase lookup in
    // columnToComponentIndexMapping, so they must be rejected as duplicates.
    // Message reports the second occurrence as written ("B"); switch to "b"
    // if the implementation normalises to lowercase before reporting.
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,b,B\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 3: Duplicate header name: B", ex.getMessage());
  }

  @Test
  void testHeaderNameStartingWithDigitRejected() {
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,1b,c\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 2: Invalid header name: 1b", ex.getMessage());
  }

  @Test
  void testHeaderNameWithHyphenRejected() {
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,b-c,d\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 2: Invalid header name: b-c", ex.getMessage());
  }

  @Test
  void testHeaderNameWithInternalSpaceRejected() {
    // Internal space is not trimmed (only leading/trailing are) and is not
    // a valid Java identifier character.
    record Row(String a, String b, String c) {}
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class, () -> CSV.of("a,b c,d\n1,2,3\n").as(Row.class).list());
    assertEquals("CSV input error at line 1 column 2: Invalid header name: b c", ex.getMessage());
  }

  @Test
  void testValidHeaderNamesAccepted() {
    record Row(String a, String b, String c) {}
    List<Row> rows = CSV.of("a,b,c\n1,2,3\n").as(Row.class).list();
    assertEquals(1, rows.size());
    Row r0 = rows.get(0);
    assertEquals("1", r0.a());
    assertEquals("2", r0.b());
    assertEquals("3", r0.c());
  }

  @Test
  void testHeaderNamesMatchedCaseInsensitively() {
    // Sanity: validation must not break the existing case-insensitive
    // lookup (all-uppercase / mixed-case headers still map correctly).
    record Row(String dataElement, String period) {}
    List<Row> rows = CSV.of("DATAELEMENT,Period\nde,2025\n").as(Row.class).list();
    assertEquals(1, rows.size());
    Row r0 = rows.get(0);
    assertEquals("de", r0.dataElement());
    assertEquals("2025", r0.period());
  }

  // ----------------------------------------------------------------
  // Iterator contract: next() must work without a preceding hasNext(),
  // hasNext() must be idempotent (no row consumption), and next() past
  // the last row must throw NoSuchElementException (not replay the last).

  @Test
  void testNextWithoutHasNextReturnsFirstRow() {
    record Row(String v) {}
    Iterator<Row> it = CSV.of("v\n1\n2\n").as(Row.class).iterator();
    // Contract: next() returns the next element without requiring hasNext()
    // first. Currently throws NoSuchElementException ("next == null").
    assertEquals("1", it.next().v());
    assertEquals("2", it.next().v());
    assertThrowsExactly(NoSuchElementException.class, it::next);
  }

  @Test
  void testHasNextIsIdempotent() {
    record Row(String v) {}
    Iterator<Row> it = CSV.of("v\n1\n2\n").as(Row.class).iterator();
    assertTrue(it.hasNext());
    assertTrue(it.hasNext());
    assertTrue(it.hasNext());
    // Multiple hasNext() calls must not consume a row; next() returns "1".
    // Currently the third call hits EOI and returns false, and next() would
    // return "2" because the pending slot was overwritten.
    assertEquals("1", it.next().v());
    assertEquals("2", it.next().v());
    assertFalse(it.hasNext());
  }

  @Test
  void testNextPastEndDoesNotReplayLastRow() {
    record Row(String v) {}
    Iterator<Row> it = CSV.of("v\n1\n").as(Row.class).iterator();
    assertTrue(it.hasNext());
    assertEquals("1", it.next().v());
    // Second next() without an intervening hasNext() must throw, not
    // return "1" again. Currently `next` is still `true`, so the second
    // call returns "1" a second time and the whole file is duplicated.
    assertThrowsExactly(NoSuchElementException.class, it::next);
    assertFalse(it.hasNext());
    assertThrowsExactly(NoSuchElementException.class, it::next);
  }

  @Test
  void testHasNextFalseOnEmptyData() {
    // Regression guard: with a header but no data rows, hasNext() returns
    // false repeatedly and next() throws. Currently passes; keep it so the
    // fix does not change this behaviour.
    record Row(String v) {}
    Iterator<Row> it = CSV.of("v\n").as(Row.class).iterator();
    assertFalse(it.hasNext());
    assertFalse(it.hasNext());
    assertThrowsExactly(NoSuchElementException.class, it::next);
  }

  private static List<DataEntryValue.Input> read(@Language("csv") String csv) {
    return CSV.of(csv).as(DataEntryValue.Input.class).list();
  }
}
