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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlConditionJoinerTest {
  @Test
  @DisplayName("Join multiple valid conditions")
  void testJoinSqlConditions_multipleValidConditions() {
    String result =
        SqlConditionJoiner.joinSqlConditions("column1 = 1", "column2 = 2", "column3 = 3");
    assertEquals("where column1 = 1 and column2 = 2 and column3 = 3", result);
  }

  @Test
  @DisplayName("Join conditions with leading 'WHERE' keyword")
  void testJoinSqlConditions_conditionsWithLeadingWhere() {
    String result = SqlConditionJoiner.joinSqlConditions("WHERE column1 = 1", "where column2 = 2");
    assertEquals("where column1 = 1 and column2 = 2", result);
  }

  @Test
  @DisplayName("Join conditions with mixed valid and invalid inputs")
  void testJoinSqlConditions_mixedValidAndInvalidConditions() {
    String result =
        SqlConditionJoiner.joinSqlConditions("", null, "column1 = 1", "   ", "column2 = 2");
    assertEquals("where column1 = 1 and column2 = 2", result);
  }

  @Test
  @DisplayName("Join single valid condition")
  void testJoinSqlConditions_singleValidCondition() {
    String result = SqlConditionJoiner.joinSqlConditions("column1 = 1");
    assertEquals("where column1 = 1", result);
  }

  @Test
  @DisplayName("Join no conditions (empty input)")
  void testJoinSqlConditions_noConditions() {
    String result = SqlConditionJoiner.joinSqlConditions();
    assertEquals("", result);
  }

  @Test
  @DisplayName("Join null conditions")
  void testJoinSqlConditions_nullConditions() {
    String result = SqlConditionJoiner.joinSqlConditions((String[]) null);
    assertEquals("", result);
  }

  @Test
  @DisplayName("Join all null or blank conditions")
  void testJoinSqlConditions_allNullOrBlankConditions() {
    String result = SqlConditionJoiner.joinSqlConditions(null, "", "   ");
    assertEquals("", result);
  }

  @Test
  @DisplayName("Join conditions with extra whitespace")
  void testJoinSqlConditions_conditionsWithExtraWhitespace() {
    String result = SqlConditionJoiner.joinSqlConditions("  column1 = 1  ", "  column2 = 2  ");
    assertEquals("where column1 = 1 and column2 = 2", result);
  }

  @Test
  @DisplayName("Join conditions with mixed case 'WHERE' keyword")
  void testJoinSqlConditions_mixedCaseWhereKeyword() {
    String result = SqlConditionJoiner.joinSqlConditions("WHERE column1 = 1", "wHeRe column2 = 2");
    assertEquals("where column1 = 1 and column2 = 2", result);
  }

  @Test
  @DisplayName("Join conditions with complex expressions")
  void testJoinSqlConditions_complexExpressions() {
    String result =
        SqlConditionJoiner.joinSqlConditions(
            "column1 = 1 AND column2 = 2", "column3 BETWEEN 1 AND 10", "column4 IS NULL");
    assertEquals(
        "where column1 = 1 AND column2 = 2 and column3 BETWEEN 1 AND 10 and column4 IS NULL",
        result);
  }

  @Test
  @DisplayName("Join conditions with special characters")
  void testJoinSqlConditions_specialCharacters() {
    String result =
        SqlConditionJoiner.joinSqlConditions("\"column!@#\" = 'value'", "`column$%^` = 123");
    assertEquals("where \"column!@#\" = 'value' and `column$%^` = 123", result);
  }

  @Test
  @DisplayName("Join conditions with function calls")
  void testJoinSqlConditions_functionCalls() {
    String result =
        SqlConditionJoiner.joinSqlConditions("UPPER(column1) = 'TEST'", "LOWER(column2) = 'test'");
    assertEquals("where UPPER(column1) = 'TEST' and LOWER(column2) = 'test'", result);
  }
}
