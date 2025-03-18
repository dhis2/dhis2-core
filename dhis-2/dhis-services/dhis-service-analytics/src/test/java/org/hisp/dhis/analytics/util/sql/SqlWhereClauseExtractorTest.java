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
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SqlWhereClauseExtractorTest {

  @ParameterizedTest
  @MethodSource("provideSqlAndExpectedColumns")
  @DisplayName("Extract columns from WHERE clause")
  void testExtractWhereColumns(String sql, List<String> expectedColumns) {
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);
    assertEquals(expectedColumns.size(), columns.size());
    assertTrue(columns.containsAll(expectedColumns));
  }

  private static Stream<Arguments> provideSqlAndExpectedColumns() {
    return Stream.of(
        Arguments.of("SELECT * FROM table WHERE column1 = 'value'", List.of("column1")),
        Arguments.of(
            "SELECT * FROM table WHERE column1 = 'value' AND column2 = 10",
            List.of("column1", "column2")),
        Arguments.of("SELECT * FROM table WHERE column1 IN (1, 2, 3)", List.of("column1")),
        Arguments.of(
            "SELECT * FROM table WHERE (column1 = 'value' AND (column2 = 10))",
            List.of("column1", "column2")),
        Arguments.of("SELECT * FROM table", List.of()),
        Arguments.of("SELECT * FROM table WHERE t.column1 = 'value'", List.of("column1")),
        Arguments.of(
            "SELECT * FROM table WHERE column1 = 'value' AND (column2 IN (1,2) OR column3 != 'test')",
            List.of("column1", "column2", "column3")),
        Arguments.of("SELECT * FROM table WHERE column1 LIKE '%test%'", List.of("column1")),
        Arguments.of("SELECT * FROM table WHERE column1 BETWEEN 1 AND 10", List.of("column1")),
        Arguments.of("SELECT * FROM table WHERE column1 IS NULL", List.of("column1")),
        Arguments.of(
            "SELECT * FROM table WHERE column1 IN (SELECT id FROM other_table WHERE column2 = 'value')",
            List.of("column1")),
        Arguments.of("SELECT * FROM table WHERE UPPER(column1) = 'TEST'", List.of("column1")),
        Arguments.of(
            "SELECT * FROM table WHERE \"Special Column!\" = 'value'", List.of("Special Column!")),
        Arguments.of(
            "SELECT * FROM table WHERE COLUMN1 = 'value' AND column1 = 'test'",
            List.of("COLUMN1", "column1")),
        Arguments.of("SELECT * FROM table WHERE column1 > 10 AND column1 < 20", List.of("column1")),
        Arguments.of(
            "SELECT * FROM table WHERE " + "a".repeat(128) + " = 'value'",
            List.of("a".repeat(128))),
        Arguments.of(
            "SELECT * FROM table WHERE UPPER(column1) LIKE '%TEST%' AND (column2 BETWEEN 1 AND 10) OR column3 IS NOT NULL AND LOWER(column4) IN ('a', 'b', 'c')",
            List.of("column1", "column2", "column3", "column4")),
        Arguments.of(
            "SELECT * FROM table WHERE UPPER(column1) = 'TEST' AND LOWER(column2) = 'test'",
            List.of("column1", "column2")),
        Arguments.of("SELECT * FROM table WHERE UPPER(TRIM(column1)) = 'TEST'", List.of("column1")),
        Arguments.of(
            "SELECT * FROM table WHERE CONCAT(column1, column2) = 'TEST'",
            List.of("column1", "column2")),
        Arguments.of(
            "SELECT * FROM table WHERE column1 BETWEEN column2 AND column3",
            List.of("column1", "column2", "column3")),
        Arguments.of(
            "SELECT * FROM table WHERE column1 BETWEEN 1 AND 10 AND column2 BETWEEN 'A' AND 'Z'",
            List.of("column1", "column2")),
        Arguments.of(
            "SELECT * FROM table WHERE column1 BETWEEN LOWER(column2) AND UPPER(column3)",
            List.of("column1", "column2", "column3")),
        Arguments.of(
            "SELECT * FROM table WHERE \"Special!@#$%^&*()\" = 'value1' AND \"Column With Spaces\" = 'value2' AND \"Mixed_Case-Column\" = 'value3'",
            List.of("Special!@#$%^&*()", "Column With Spaces", "Mixed_Case-Column")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "SELECT * FROM WHERE column1"})
  @DisplayName("Throw RuntimeException for invalid SQL syntax")
  void testExtractWhereColumns_invalidSql(String sql) {
    assertThrows(RuntimeException.class, () -> SqlWhereClauseExtractor.extractWhereColumns(sql));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "SELECT * FROM table WHERE column1 = 'value'",
        "SELECT * FROM table WHERE column1 IN (1, 2, 3)"
      })
  @DisplayName("Extract columns from WHERE clause with various conditions")
  void testExtractWhereColumns_variousConditions(String sql) {
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);
    assertFalse(columns.isEmpty());
  }
}
