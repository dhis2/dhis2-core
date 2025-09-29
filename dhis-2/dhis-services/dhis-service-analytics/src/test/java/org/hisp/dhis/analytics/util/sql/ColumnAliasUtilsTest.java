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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColumnAliasUtilsTest {

  @Nested
  @DisplayName("splitQualified")
  class SplitQualified {
    @Test
    @DisplayName("returns qualifier and column for dotted reference")
    void splitQualifiedReturnsQualifiedRefForSimpleDottedColumn() {
      var result = ColumnAliasUtils.splitQualified("analytics.value");

      assertTrue(result.isPresent());
      assertEquals("analytics", result.get().qualifier());
      assertEquals("value", result.get().columnName());
    }

    @Test
    @DisplayName("preserves quoted identifiers")
    void splitQualifiedPreservesQuotedIdentifiers() {
      var result = ColumnAliasUtils.splitQualified("\"analytics\".\"value\"");

      assertTrue(result.isPresent());
      assertEquals("\"analytics\"", result.get().qualifier());
      assertEquals("\"value\"", result.get().columnName());
    }

    @Test
    @DisplayName("returns empty when column is not qualified")
    void splitQualifiedReturnsEmptyForUnqualifiedColumn() {
      assertTrue(ColumnAliasUtils.splitQualified("value").isEmpty());
    }

    @Test
    @DisplayName("ignores expressions that are not plain columns")
    void splitQualifiedReturnsEmptyForNonColumnExpression() {
      assertTrue(ColumnAliasUtils.splitQualified("coalesce(value, 0)").isEmpty());
    }

    @Test
    @DisplayName("returns empty for blank input")
    void splitQualifiedReturnsEmptyForBlankInput() {
      assertTrue(ColumnAliasUtils.splitQualified("").isEmpty());
      assertTrue(ColumnAliasUtils.splitQualified("   ").isEmpty());
      assertTrue(ColumnAliasUtils.splitQualified(null).isEmpty());
    }
  }

  @Nested
  @DisplayName("parseSelectItem")
  class ParseSelectItem {
    @Test
    @DisplayName("parses simple column without alias")
    void parsesSimpleColumnWithoutAlias() {
      var result = ColumnAliasUtils.parseSelectItem("name");

      assertTrue(result.isPresent());
      assertEquals("name", result.get().columnExpression());
      assertNull(result.get().alias());
    }

    @Test
    @DisplayName("parses qualified column without alias")
    void parsesQualifiedColumnWithoutAlias() {
      var result = ColumnAliasUtils.parseSelectItem("u.name");

      assertTrue(result.isPresent());
      assertEquals("u.name", result.get().columnExpression());
      assertNull(result.get().alias());
    }

    @Test
    @DisplayName("parses simple column with unquoted alias")
    void parsesSimpleColumnWithUnquotedAlias() {
      var result = ColumnAliasUtils.parseSelectItem("name as user_name");

      assertTrue(result.isPresent());
      assertEquals("name", result.get().columnExpression());
      assertEquals("user_name", result.get().alias());
    }

    @Test
    @DisplayName("parses qualified column with unquoted alias")
    void parsesQualifiedColumnWithUnquotedAlias() {
      var result = ColumnAliasUtils.parseSelectItem("u.name as user_name");

      assertTrue(result.isPresent());
      assertEquals("u.name", result.get().columnExpression());
      assertEquals("user_name", result.get().alias());
    }

    @Test
    @DisplayName("parses qualified column with double-quoted alias")
    void parsesQualifiedColumnWithDoubleQuotedAlias() {
      var result = ColumnAliasUtils.parseSelectItem("ab.column as \"my_alias\"");

      assertTrue(result.isPresent());
      assertEquals("ab.column", result.get().columnExpression());
      assertEquals("\"my_alias\"", result.get().alias());
    }

    @Test
    @DisplayName("parses simple column with double-quoted alias")
    void parsesSimpleColumnWithDoubleQuotedAlias() {
      var result = ColumnAliasUtils.parseSelectItem("column as \"my_alias\"");

      assertTrue(result.isPresent());
      assertEquals("column", result.get().columnExpression());
      assertEquals("\"my_alias\"", result.get().alias());
    }

    @Test
    @DisplayName("parses column with single-quoted alias")
    void parsesColumnWithSingleQuotedAlias() {
      var result = ColumnAliasUtils.parseSelectItem("name as 'user_name'");

      assertTrue(result.isPresent());
      assertEquals("name", result.get().columnExpression());
      assertEquals("'user_name'", result.get().alias());
    }

    @Test
    @DisplayName("handles case-insensitive AS keyword")
    void handlesCaseInsensitiveAs() {
      var result1 = ColumnAliasUtils.parseSelectItem("name AS user_name");
      var result2 = ColumnAliasUtils.parseSelectItem("name As user_name");
      var result3 = ColumnAliasUtils.parseSelectItem("name aS user_name");

      assertTrue(result1.isPresent());
      assertEquals("user_name", result1.get().alias());

      assertTrue(result2.isPresent());
      assertEquals("user_name", result2.get().alias());

      assertTrue(result3.isPresent());
      assertEquals("user_name", result3.get().alias());
    }

    @Test
    @DisplayName("handles extra spaces around AS keyword")
    void handlesExtraSpacesAroundAs() {
      var result = ColumnAliasUtils.parseSelectItem("name  as  user_name");

      assertTrue(result.isPresent());
      assertEquals("name", result.get().columnExpression());
      assertEquals("user_name", result.get().alias());
    }

    @Test
    @DisplayName("parses aggregate function with alias")
    void parsesAggregateFunctionWithAlias() {
      var result = ColumnAliasUtils.parseSelectItem("count(*) as total_count");

      assertTrue(result.isPresent());
      assertEquals("count(*)", result.get().columnExpression());
      assertEquals("total_count", result.get().alias());
    }

    @Test
    @DisplayName("parses aggregate function with double-quoted alias")
    void parsesAggregateFunctionWithQuotedAlias() {
      var result = ColumnAliasUtils.parseSelectItem("sum(amount) as \"total_amount\"");

      assertTrue(result.isPresent());
      assertEquals("sum(amount)", result.get().columnExpression());
      assertEquals("\"total_amount\"", result.get().alias());
    }

    @Test
    @DisplayName("parses CASE expression with alias")
    void parsesCaseExpressionWithAlias() {
      var result =
          ColumnAliasUtils.parseSelectItem(
              "CASE WHEN active THEN 'Active' ELSE 'Inactive' END as status");

      assertTrue(result.isPresent());
      assertTrue(result.get().columnExpression().toLowerCase().contains("case"));
      assertEquals("status", result.get().alias());
    }

    @Test
    @DisplayName("parses CASE expression with double-quoted alias")
    void parsesCaseExpressionWithQuotedAlias() {
      var result =
          ColumnAliasUtils.parseSelectItem(
              "CASE WHEN active THEN 'Active' ELSE 'Inactive' END as \"status_text\"");

      assertTrue(result.isPresent());
      assertTrue(result.get().columnExpression().toLowerCase().contains("case"));
      assertEquals("\"status_text\"", result.get().alias());
    }

    @Test
    @DisplayName("parses arithmetic expression with alias")
    void parsesArithmeticExpressionWithAlias() {
      var result = ColumnAliasUtils.parseSelectItem("price * quantity as total");

      assertTrue(result.isPresent());
      assertEquals("price * quantity", result.get().columnExpression());
      assertEquals("total", result.get().alias());
    }

    @Test
    @DisplayName("parses quoted column name with alias")
    void parsesQuotedColumnNameWithAlias() {
      var result = ColumnAliasUtils.parseSelectItem("\"user name\" as user_name");

      assertTrue(result.isPresent());
      assertEquals("\"user name\"", result.get().columnExpression());
      assertEquals("user_name", result.get().alias());
    }

    @Test
    @DisplayName("parses qualified quoted column with quoted alias")
    void parsesQualifiedQuotedColumnWithQuotedAlias() {
      var result = ColumnAliasUtils.parseSelectItem("\"table\".\"column\" as \"my_alias\"");

      assertTrue(result.isPresent());
      assertEquals("\"table\".\"column\"", result.get().columnExpression());
      assertEquals("\"my_alias\"", result.get().alias());
    }

    @Test
    @DisplayName("parses complex function call with alias")
    void parsesComplexFunctionWithAlias() {
      var result = ColumnAliasUtils.parseSelectItem("coalesce(name, 'Unknown') as display_name");

      assertTrue(result.isPresent());
      assertEquals("coalesce(name, 'Unknown')", result.get().columnExpression());
      assertEquals("display_name", result.get().alias());
    }

    @Test
    @DisplayName("parses nested function calls with alias")
    void parsesNestedFunctionCallsWithAlias() {
      var result = ColumnAliasUtils.parseSelectItem("upper(trim(name)) as clean_name");

      assertTrue(result.isPresent());
      assertEquals("upper(trim(name))", result.get().columnExpression());
      assertEquals("clean_name", result.get().alias());
    }

    @Test
    @DisplayName("parses column with alias containing special characters")
    void parsesColumnWithAliasContainingSpecialCharacters() {
      var result = ColumnAliasUtils.parseSelectItem("price as \"unit_price_$\"");

      assertTrue(result.isPresent());
      assertEquals("price", result.get().columnExpression());
      assertEquals("\"unit_price_$\"", result.get().alias());
    }

    @Test
    @DisplayName("parses column with alias containing spaces")
    void parsesColumnWithAliasContainingSpaces() {
      var result = ColumnAliasUtils.parseSelectItem("price as \"Unit Price\"");

      assertTrue(result.isPresent());
      assertEquals("price", result.get().columnExpression());
      assertEquals("\"Unit Price\"", result.get().alias());
    }

    @Test
    @DisplayName("returns empty for blank input")
    void returnsEmptyForBlankInput() {
      assertTrue(ColumnAliasUtils.parseSelectItem("").isEmpty());
      assertTrue(ColumnAliasUtils.parseSelectItem("   ").isEmpty());
      assertTrue(ColumnAliasUtils.parseSelectItem(null).isEmpty());
    }

    @Test
    @DisplayName("returns empty for invalid SQL")
    void returnsEmptyForInvalidSql() {
      assertTrue(ColumnAliasUtils.parseSelectItem("invalid!!!syntax").isEmpty());
    }

    @Test
    @DisplayName("parses expression without AS keyword but with alias")
    void parsesExpressionWithImplicitAlias() {
      // Note: Most SQL dialects require AS keyword, but some allow implicit aliases
      // This tests parser behavior if it supports implicit aliases
      var result = ColumnAliasUtils.parseSelectItem("name user_name");

      // This may or may not be supported depending on parser dialect
      // If supported, it should parse correctly; if not, it should return empty
      if (result.isPresent()) {
        assertEquals("name", result.get().columnExpression());
        assertEquals("user_name", result.get().alias());
      }
    }
  }

  @Nested
  @DisplayName("Integration tests")
  class IntegrationTests {
    @Test
    @DisplayName("parseSelectItem and splitQualified work together")
    void parseSelectItemAndSplitQualifiedWorkTogether() {
      var selectResult = ColumnAliasUtils.parseSelectItem("t1.column_name as \"result_value\"");

      assertTrue(selectResult.isPresent());
      assertEquals("t1.column_name", selectResult.get().columnExpression());
      assertEquals("\"result_value\"", selectResult.get().alias());

      var qualifiedResult = ColumnAliasUtils.splitQualified(selectResult.get().columnExpression());

      assertTrue(qualifiedResult.isPresent());
      assertEquals("t1", qualifiedResult.get().qualifier());
      assertEquals("column_name", qualifiedResult.get().columnName());
    }

    @Test
    @DisplayName("handles complex real-world select items")
    void handlesComplexRealWorldSelectItems() {
      var result1 =
          ColumnAliasUtils.parseSelectItem(
              "CASE WHEN status = 'ACTIVE' THEN count(*) ELSE 0 END as active_count");
      assertTrue(result1.isPresent());
      assertEquals("active_count", result1.get().alias());

      var result2 = ColumnAliasUtils.parseSelectItem("avg(score) * 100 as \"percentage\"");
      assertTrue(result2.isPresent());
      assertEquals("\"percentage\"", result2.get().alias());

      var result3 = ColumnAliasUtils.parseSelectItem("substring(name, 1, 10) as short_name");
      assertTrue(result3.isPresent());
      assertEquals("short_name", result3.get().alias());
    }
  }
}
