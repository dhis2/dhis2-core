/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlWhereClauseExtractorTest {

  @Test
  @DisplayName("Extract single column from simple WHERE clause")
  void testExtractWhereColumns_singleColumn() {
    String sql = "SELECT * FROM table WHERE column1 = 'value'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract multiple columns from WHERE clause with AND")
  void testExtractWhereColumns_multipleColumns() {
    String sql = "SELECT * FROM table WHERE column1 = 'value' AND column2 = 10";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(2, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
  }

  @Test
  @DisplayName("Extract column from WHERE clause with IN condition")
  void testExtractWhereColumns_inCondition() {
    String sql = "SELECT * FROM table WHERE column1 IN (1, 2, 3)";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract columns from nested parentheses in WHERE clause")
  void testExtractWhereColumns_nestedParentheses() {
    String sql = "SELECT * FROM table WHERE (column1 = 'value' AND (column2 = 10))";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(2, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
  }

  @Test
  @DisplayName("Return empty list for SQL without WHERE clause")
  void testExtractWhereColumns_noWhereClause() {
    String sql = "SELECT * FROM table";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertTrue(columns.isEmpty());
  }

  @Test
  @DisplayName("Throw RuntimeException for null SQL input")
  void testExtractWhereColumns_nullInput() {
    assertThrows(
        RuntimeException.class,
        () -> {
          SqlWhereClauseExtractor.extractWhereColumns(null);
        });
  }

  @Test
  @DisplayName("Throw RuntimeException for invalid SQL syntax")
  void testExtractWhereColumns_invalidSql() {
    String sql = "SELECT * FROM WHERE column1";
    assertThrows(
        RuntimeException.class,
        () -> {
          SqlWhereClauseExtractor.extractWhereColumns(sql);
        });
  }

  @Test
  @DisplayName("Extract column names without table aliases")
  void testExtractWhereColumns_withTableAlias() {
    String sql = "SELECT * FROM table t WHERE t.column1 = 'value'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract columns from complex WHERE clause with multiple conditions")
  void testExtractWhereColumns_complexWhere() {
    String sql =
        "SELECT * FROM table WHERE column1 = 'value' AND "
            + "(column2 IN (1,2) OR column3 != 'test')";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(3, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
    assertTrue(columns.contains("column3"));
  }

  @Test
  @DisplayName("Extract column from WHERE clause with LIKE operator")
  void testExtractWhereColumns_likeOperator() {
    String sql = "SELECT * FROM table WHERE column1 LIKE '%test%'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract column from WHERE clause with BETWEEN operator")
  void testExtractWhereColumns_betweenOperator() {
    String sql = "SELECT * FROM table WHERE column1 BETWEEN 1 AND 10";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract column from WHERE clause with IS NULL condition")
  void testExtractWhereColumns_isNullCondition() {
    String sql = "SELECT * FROM table WHERE column1 IS NULL";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Throw RuntimeException for empty string SQL")
  void testExtractWhereColumns_emptyString() {
    assertThrows(
        RuntimeException.class,
        () -> {
          SqlWhereClauseExtractor.extractWhereColumns("");
        });
  }

  @Test
  @DisplayName("Extract columns from main query WHERE clause with subquery")
  void testExtractWhereColumns_withSubquery() {
    String sql =
        "SELECT * FROM table WHERE column1 IN (SELECT id FROM other_table WHERE column2 = 'value')";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract column from WHERE clause with function calls")
  void testExtractWhereColumns_withFunctions() {
    String sql = "SELECT * FROM table WHERE UPPER(column1) = 'TEST'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract column with special characters from WHERE clause")
  void testExtractWhereColumns_specialCharacters() {
    String sql = "SELECT * FROM table WHERE \"Special Column!\" = 'value'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("Special Column!"));
  }

  @Test
  @DisplayName("Preserve case sensitivity in column names")
  void testExtractWhereColumns_caseSensitivity() {
    String sql = "SELECT * FROM table WHERE COLUMN1 = 'value' AND column1 = 'test'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(2, columns.size());
    assertTrue(columns.contains("COLUMN1"));
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Remove duplicate columns in WHERE clause")
  void testExtractWhereColumns_duplicateColumns() {
    String sql = "SELECT * FROM table WHERE column1 > 10 AND column1 < 20";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Handle extremely long column names")
  void testExtractWhereColumns_longColumnNames() {
    String longColumnName = "a".repeat(128);
    String sql = "SELECT * FROM table WHERE " + longColumnName + " = 'value'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains(longColumnName));
  }

  @Test
  @DisplayName("Handle multiple complex conditions with mixed operators")
  void testExtractWhereColumns_complexMixedOperators() {
    String sql =
        "SELECT * FROM table WHERE "
            + "UPPER(column1) LIKE '%TEST%' AND "
            + "(column2 BETWEEN 1 AND 10) OR "
            + "column3 IS NOT NULL AND "
            + "LOWER(column4) IN ('a', 'b', 'c')";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(4, columns.size());
    assertTrue(columns.containsAll(List.of("column1", "column2", "column3", "column4")));
  }

  @Test
  @DisplayName("Extract columns from multiple function calls")
  void testExtractWhereColumns_multipleFunctions() {
    String sql = "SELECT * FROM table WHERE UPPER(column1) = 'TEST' AND LOWER(column2) = 'test'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(2, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
  }

  @Test
  @DisplayName("Extract columns from nested function calls")
  void testExtractWhereColumns_nestedFunctions() {
    String sql = "SELECT * FROM table WHERE UPPER(TRIM(column1)) = 'TEST'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(1, columns.size());
    assertTrue(columns.contains("column1"));
  }

  @Test
  @DisplayName("Extract columns from function with multiple parameters")
  void testExtractWhereColumns_functionMultipleParams() {
    String sql = "SELECT * FROM table WHERE CONCAT(column1, column2) = 'TEST'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(2, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
  }

  @Test
  @DisplayName("Extract columns from BETWEEN with column references")
  void testExtractWhereColumns_betweenWithColumns() {
    String sql = "SELECT * FROM table WHERE column1 BETWEEN column2 AND column3";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(3, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
    assertTrue(columns.contains("column3"));
  }

  @Test
  @DisplayName("Extract columns from multiple BETWEEN conditions")
  void testExtractWhereColumns_multipleBetween() {
    String sql =
        "SELECT * FROM table WHERE column1 BETWEEN 1 AND 10 AND column2 BETWEEN 'A' AND 'Z'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(2, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
  }

  @Test
  @DisplayName("Extract columns from BETWEEN with functions")
  void testExtractWhereColumns_betweenWithFunctions() {
    String sql = "SELECT * FROM table WHERE column1 BETWEEN LOWER(column2) AND UPPER(column3)";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(3, columns.size());
    assertTrue(columns.contains("column1"));
    assertTrue(columns.contains("column2"));
    assertTrue(columns.contains("column3"));
  }

  @Test
  @DisplayName("Extract columns with various special characters")
  void testExtractWhereColumns_variousSpecialCharacters() {
    String sql =
        "SELECT * FROM table WHERE "
            + "\"Special!@#$%^&*()\" = 'value1' AND "
            + "\"Column With Spaces\" = 'value2' AND "
            + "\"Mixed_Case-Column\" = 'value3'";
    List<String> columns = SqlWhereClauseExtractor.extractWhereColumns(sql);

    assertEquals(3, columns.size());
    assertTrue(columns.contains("Special!@#$%^&*()"));
    assertTrue(columns.contains("Column With Spaces"));
    assertTrue(columns.contains("Mixed_Case-Column"));
  }
}
