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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.List;
import org.hisp.dhis.datavalue.DataEntryValue;
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
            "dataElement,period,orgUnit,value\r\n"
                + "de123456789,202501,ou123456789,1\r\n"
                + "de123456789,202502,ou123456789,2\r\n"
                + "de123456789,202503,ou123456789,3\r\n");
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
            "dataElement,period,orgUnit,value\n"
                + "de123456789,202501,ou123456789,1\n"
                + "de123456789,202502,ou123456789,2");
    assertEquals(List.of("1", "2"), values.stream().map(DataEntryValue.Input::value).toList());
  }

  @Test
  void testBlankLinesSkipped() {
    List<DataEntryValue.Input> values =
        read(
            "dataElement,period,orgUnit,value\n"
                + "de123456789,202501,ou123456789,1\n"
                + "\n"
                + "de123456789,202502,ou123456789,2\n\n");
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
            "dataElement,period,orgUnit,value,comment\r\n"
                + "de123456789,202501,ou123456789,5,\"line1\r\nline2\"\r\n"
                + "de987654321,202501,ou123456789,6,x\r\n");
    assertEquals(2, values.size());
    assertEquals("line1\r\nline2", values.get(0).comment());
    assertEquals("6", values.get(1).value());
  }

  @Test
  void testUnterminatedQuote() {
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> read("dataElement,period,orgUnit,value\nde123456789,202501,ou123456789,\"5\n"));
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
            "dataElement,period,orgUnit,value\r\n"
                + "de123456789,202501,ou123456789,1\r\n"
                + ",,,\r\n"
                + "  ,\t, ,\r\n"
                + "de123456789,202502,ou123456789,2\r\n"
                + ",,,\r\n");
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
            "\r\n\"note, extra\",\"data\"\"Element\",dataElement,period,orgUnit,value\r\n"
                + "x,y,de123456789,202501,ou123456789,5\r\n");
    assertEquals(1, values.size());
    assertEquals("de123456789", values.get(0).dataElement());
    assertEquals("5", values.get(0).value());
  }

  @Test
  void testNoHeader() {
    IllegalArgumentException ex =
        assertThrowsExactly(IllegalArgumentException.class, () -> read("\n\r\n"));
    assertEquals("No header line provided.", ex.getMessage());
  }

  @Test
  void testUnterminatedQuoteReportsLine() {
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read(
                    "dataElement,period,orgUnit,value,comment\n"
                        + "\n"
                        + "de123456789,202501,ou123456789,1,\"a\nb\"\n"
                        + "de123456789,202502,ou123456789,2,\"open\n"
                        + "de123456789,202503,ou123456789,3,x\n"));
    assertEquals("Quoted value starting in line 5 is not terminated", ex.getMessage());
  }

  @Test
  void testQuotedValueTooLong() {
    String huge = "x".repeat(CSV.MAX_VALUE_LENGTH + 1);
    IllegalArgumentException ex =
        assertThrowsExactly(
            IllegalArgumentException.class,
            () ->
                read("dataElement,period,orgUnit,value\nde123456789,202501,ou123456789,\"" + huge));
    assertEquals(
        "Quoted value starting in line 2 exceeds %d characters".formatted(CSV.MAX_VALUE_LENGTH),
        ex.getMessage());
  }

  @Test
  void testQuotedValueAtMaxLength() {
    String max = "x".repeat(CSV.MAX_VALUE_LENGTH);
    List<DataEntryValue.Input> values =
        read(
            "dataElement,period,orgUnit,comment\nde123456789,202501,ou123456789,\"" + max + "\"\n");
    assertEquals(CSV.MAX_VALUE_LENGTH, values.get(0).comment().length());
  }

  private static List<DataEntryValue.Input> read(String csv) {
    return CSV.of(csv).as(DataEntryValue.Input.class).list();
  }
}
