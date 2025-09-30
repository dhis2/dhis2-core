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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ColumnUtils Tests")
class ColumnUtilsTest {

  @Test
  @DisplayName("Should split simple columns separated by newlines")
  void shouldSplitSimpleColumns() {
    String input = "column_a\ncolumn_b\ncolumn_c";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
    assertEquals("column_c", result.get(2));
  }

  @Test
  @DisplayName("Should split columns with trailing commas")
  void shouldSplitColumnsWithTrailingCommas() {
    String input = "column_a,\ncolumn_b,\ncolumn_c";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
    assertEquals("column_c", result.get(2));
  }

  @Test
  @DisplayName("Should handle complex SQL column definitions")
  void shouldHandleComplexSqlColumns() {
    String input =
        """
           djmkd_1.value as "EPEcjy3FWmI[-1].lJTx9EZ1dk1",
           coalesce(djmkd_1.rn = 2, false) as "EPEcjy3FWmI[-1].lJTx9EZ1dk1.exists",
           djmkd_1.eventstatus as "EPEcjy3FWmI[-1].lJTx9EZ1dk1.status"
           """;

    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("djmkd_1.value as \"EPEcjy3FWmI[-1].lJTx9EZ1dk1\"", result.get(0));
    assertEquals(
        "coalesce(djmkd_1.rn = 2, false) as \"EPEcjy3FWmI[-1].lJTx9EZ1dk1.exists\"", result.get(1));
    assertEquals("djmkd_1.eventstatus as \"EPEcjy3FWmI[-1].lJTx9EZ1dk1.status\"", result.get(2));
  }

  @Test
  @DisplayName("Should trim whitespace from columns")
  void shouldTrimWhitespace() {
    String input = "  column_a  \n  column_b  \n  column_c  ";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
    assertEquals("column_c", result.get(2));
  }

  @Test
  @DisplayName("Should trim whitespace before comma")
  void shouldTrimWhitespaceBeforeComma() {
    String input = "column_a  ,\ncolumn_b\t,\ncolumn_c   ,";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
    assertEquals("column_c", result.get(2));
  }

  @Test
  @DisplayName("Should filter out empty lines")
  void shouldFilterOutEmptyLines() {
    String input = "column_a\n\ncolumn_b\n\n\ncolumn_c";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
    assertEquals("column_c", result.get(2));
  }

  @Test
  @DisplayName("Should filter out whitespace-only lines")
  void shouldFilterOutWhitespaceOnlyLines() {
    String input = "column_a\n   \ncolumn_b\n\t\t\ncolumn_c";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
    assertEquals("column_c", result.get(2));
  }

  @Test
  @DisplayName("Should return empty list for null input")
  void shouldReturnEmptyListForNullInput() {
    List<String> result = ColumnUtils.splitColumns(null);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should return empty list for empty string")
  void shouldReturnEmptyListForEmptyString() {
    List<String> result = ColumnUtils.splitColumns("");

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle single column without newline")
  void shouldHandleSingleColumn() {
    String input = "column_a";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(1, result.size());
    assertEquals("column_a", result.get(0));
  }

  @Test
  @DisplayName("Should handle single column with trailing comma")
  void shouldHandleSingleColumnWithComma() {
    String input = "column_a,";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(1, result.size());
    assertEquals("column_a", result.get(0));
  }

  @Test
  @DisplayName("Should handle mixed columns with and without commas")
  void shouldHandleMixedColumnsWithAndWithoutCommas() {
    String input = "column_a,\ncolumn_b\ncolumn_c,";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
    assertEquals("column_c", result.get(2));
  }

  @Test
  @DisplayName("Should handle columns with commas in the middle (not trailing)")
  void shouldPreserveCommasInMiddle() {
    String input = "func(a, b) as result,\nother_column";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(2, result.size());
    assertEquals("func(a, b) as result", result.get(0));
    assertEquals("other_column", result.get(1));
  }

  @Test
  @DisplayName("Should handle string with only newlines")
  void shouldHandleOnlyNewlines() {
    String input = "\n\n\n";
    List<String> result = ColumnUtils.splitColumns(input);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("Should handle string with only whitespace and newlines")
  void shouldHandleOnlyWhitespaceAndNewlines() {
    String input = "  \n\t\n   \n";
    List<String> result = ColumnUtils.splitColumns(input);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {",", " ,", ", ", "  ,  "})
  @DisplayName("Should handle comma-only lines")
  void shouldHandleCommaOnlyLines(String commaLine) {
    String input = "column_a\n" + commaLine + "\ncolumn_b";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(2, result.size());
    assertEquals("column_a", result.get(0));
    assertEquals("column_b", result.get(1));
  }

  @Test
  @DisplayName("Should handle very long column definitions")
  void shouldHandleLongColumns() {
    String longColumn = "a".repeat(1000) + " as \"very_long_alias\"";
    String input = longColumn + ",\ncolumn_b";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(2, result.size());
    assertEquals(longColumn.substring(0, longColumn.length()), result.get(0));
    assertEquals("column_b", result.get(1));
  }

  @Test
  @DisplayName("Should preserve special characters in column definitions")
  void shouldPreserveSpecialCharacters() {
    String input = "column@#$%,\ncolumn[123],\ncolumn.field->value";
    List<String> result = ColumnUtils.splitColumns(input);

    assertEquals(3, result.size());
    assertEquals("column@#$%", result.get(0));
    assertEquals("column[123]", result.get(1));
    assertEquals("column.field->value", result.get(2));
  }
}
