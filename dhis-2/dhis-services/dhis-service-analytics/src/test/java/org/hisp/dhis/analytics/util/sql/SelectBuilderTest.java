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

@DisplayName("SelectBuilder")
class SelectBuilderTest {

  @Nested
  @DisplayName("Basic SELECT queries")
  class BasicSelectQueries {
    @Test
    @DisplayName("should build simple SELECT query")
    void shouldBuildSimpleSelectQuery() {
      String sql = new SelectBuilder().addColumn("name").from("users", "u").build();

      assertEquals("select name from users as u", sql);
    }

    @Test
    @DisplayName("should build SELECT with multiple columns")
    void shouldBuildSelectWithMultipleColumns() {
      String sql =
          new SelectBuilder()
              .addColumn("id")
              .addColumn("name")
              .addColumn("email", "", "email_address")
              .addColumn("count(*)", "", "total")
              .from("users", "u")
              .build();

      assertEquals(
          "select id, name, email as email_address, count(*) as total from users as u", sql);
    }
  }

  @Nested
  @DisplayName("Column aliases")
  class ColumnAliases {
    @Test
    @DisplayName("should preserve alias for qualified column with AS keyword")
    void shouldPreserveAliasForQualifiedColumn() {
      String sql =
          new SelectBuilder()
              .addColumn("u.name as user_name")
              .addColumn("u.email as user_email")
              .from("users", "u")
              .build();

      assertEquals("select u.name as user_name, u.email as user_email from users as u", sql);
    }

    @Test
    void shouldPreserveAlias() {

      String sql =
          new SelectBuilder()
              .addColumn("enrollment", "ax")
              .from("analytics_enrollment", "ax")
              .build();
      assertEquals("select ax.enrollment from analytics_enrollment as ax", sql);
    }

    @Test
    @DisplayName("should preserve quoted alias for qualified column")
    void shouldPreserveQuotedAliasForQualifiedColumn() {
      String sql =
          new SelectBuilder().addColumn("ab.column as \"my_alias\"").from("table", "ab").build();

      assertEquals("select ab.column as \"my_alias\" from table as ab", sql);
    }

    @Test
    @DisplayName("should preserve alias for unqualified column")
    void shouldPreserveAliasForUnqualifiedColumn() {
      String sql =
          new SelectBuilder()
              .addColumn("name as display_name")
              .addColumn("email as contact_email")
              .from("users", "u")
              .build();

      assertEquals("select name as display_name, email as contact_email from users as u", sql);
    }

    @Test
    @DisplayName("should preserve quoted alias for unqualified column")
    void shouldPreserveQuotedAliasForUnqualifiedColumn() {
      String sql =
          new SelectBuilder().addColumn("column as \"my_alias\"").from("table", "t").build();

      assertEquals("select column as \"my_alias\" from table as t", sql);
    }

    @Test
    @DisplayName("should handle case-insensitive AS keyword")
    void shouldHandleCaseInsensitiveAs() {
      String sql =
          new SelectBuilder()
              .addColumn("u.name AS user_name")
              .addColumn("u.email As user_email")
              .addColumn("u.age aS user_age")
              .from("users", "u")
              .build();

      assertEquals(
          "select u.name as user_name, u.email as user_email, u.age as user_age from users as u",
          sql);
    }

    @Test
    @DisplayName("should handle alias with spaces around AS keyword")
    void shouldHandleAliasWithSpaces() {
      String sql =
          new SelectBuilder()
              .addColumn("u.name  as  user_name")
              .addColumn("u.email   AS   user_email")
              .from("users", "u")
              .build();

      assertEquals("select u.name as user_name, u.email as user_email from users as u", sql);
    }

    @Test
    @DisplayName("should handle mixed columns with and without aliases")
    void shouldHandleMixedColumnsWithAndWithoutAliases() {
      String sql =
          new SelectBuilder()
              .addColumn("u.id")
              .addColumn("u.name as user_name")
              .addColumn("u.email")
              .addColumn("u.created_at as \"registration_date\"")
              .from("users", "u")
              .build();

      assertEquals(
          "select u.id, u.name as user_name, u.email, u.created_at as \"registration_date\" from users as u",
          sql);
    }

    @Test
    @DisplayName("should handle complex expressions with aliases")
    void shouldHandleComplexExpressionsWithAliases() {
      String sql =
          new SelectBuilder()
              .addColumn("count(*) as total_count")
              .addColumn("sum(amount) as \"total_amount\"")
              .addColumn("avg(score) AS average_score")
              .from("transactions", "t")
              .build();

      assertEquals(
          "select count(*) as total_count, sum(amount) as \"total_amount\", avg(score) as average_score from transactions as t",
          sql);
    }

    @Test
    @DisplayName("should handle CASE expressions with aliases")
    void shouldHandleCaseExpressionsWithAliases() {
      String sql =
          new SelectBuilder()
              .addColumn("CASE WHEN active THEN 'Active' ELSE 'Inactive' END as status")
              .addColumn("u.name as \"user_name\"")
              .from("users", "u")
              .build();

      assertEquals(
          "select case when active then 'Active' else 'Inactive' end as status, u.name as \"user_name\" from users as u",
          sql);
    }

    @Test
    @DisplayName("should handle qualified column with dots in table alias")
    void shouldHandleQualifiedColumnWithComplexAlias() {
      String sql =
          new SelectBuilder()
              .addColumn("t1.column_name as \"result_value\"")
              .addColumn("t2.other_column AS result_other")
              .from("table1", "t1")
              .leftJoin("table2", "t2", alias -> alias + ".id = t1.id")
              .build();

      assertEquals(
          "select t1.column_name as \"result_value\", t2.other_column as result_other from table1 as t1 left join table2 t2 on t2.id = t1.id",
          sql);
    }

    @Test
    @DisplayName("should preserve single-quoted aliases")
    void shouldPreserveSingleQuotedAliases() {
      String sql =
          new SelectBuilder()
              .addColumn("u.name as 'user_name'")
              .addColumn("u.email as \"user_email\"")
              .from("users", "u")
              .build();

      assertEquals("select u.name as 'user_name', u.email as \"user_email\" from users as u", sql);
    }

    @Test
    @DisplayName("should handle mixed quoted and unquoted aliases")
    void shouldHandleMixedQuotedAndUnquotedAliases() {
      String sql =
          new SelectBuilder()
              .addColumn("u.id as user_id")
              .addColumn("u.name as \"user name\"")
              .addColumn("u.email as 'user-email'")
              .addColumn("u.age as user_age")
              .from("users", "u")
              .build();

      assertEquals(
          "select u.id as user_id, u.name as \"user name\", u.email as 'user-email', u.age as user_age from users as u",
          sql);
    }
  }

  @Nested
  @DisplayName("CTEs")
  class CommonTableExpressions {
    @Test
    @DisplayName("should build query with single CTE")
    void shouldBuildQueryWithSingleCTE() {
      String sql =
          new SelectBuilder()
              .withCTE("user_counts", "select user_id, count(*) from events group by user_id")
              .addColumn("u.name")
              .addColumn("uc.count")
              .from("users", "u")
              .leftJoin("user_counts", "uc", alias -> alias + ".user_id = u.id")
              .build();

      assertEquals(
          "with user_counts as ("
              + " select user_id, count(*) from events group by user_id"
              + " ) select u.name, uc.count from users as u left join user_counts uc on uc.user_id = u.id",
          sql);
    }

    @Test
    @DisplayName("should build query with multiple CTEs")
    void shouldBuildQueryWithMultipleCTEs() {
      String sql =
          new SelectBuilder()
              .withCTE("cte1", "select 1")
              .withCTE("cte2", "select 2")
              .addColumn("*")
              .from("table", "t")
              .build();

      assertEquals("with cte1 as ( select 1 ), cte2 as ( select 2 ) select * from table as t", sql);
    }
  }

  @Nested
  @DisplayName("JOINs")
  class Joins {
    @Test
    @DisplayName("should build query with single JOIN")
    void shouldBuildQueryWithSingleJoin() {
      String sql =
          new SelectBuilder()
              .addColumn("u.name")
              .addColumn("o.total")
              .from("users", "u")
              .leftJoin("orders", "o", alias -> alias + ".user_id = u.id")
              .build();

      assertEquals(
          "select u.name, o.total from users as u left join orders o on o.user_id = u.id", sql);
    }

    @Test
    @DisplayName("should build query with multiple JOINs")
    void shouldBuildQueryWithMultipleJoins() {
      String sql =
          new SelectBuilder()
              .addColumn("u.name")
              .addColumn("o.total")
              .addColumn("a.address")
              .from("users", "u")
              .leftJoin("orders", "o", alias -> alias + ".user_id = u.id")
              .leftJoin("addresses", "a", alias -> alias + ".user_id = u.id")
              .build();

      assertEquals(
          "select u.name, o.total, a.address from users as u "
              + "left join orders o on o.user_id = u.id "
              + "left join addresses a on a.user_id = u.id",
          sql);
    }
  }

  @Nested
  @DisplayName("WHERE conditions")
  class WhereConditions {
    @Test
    @DisplayName("should build query with simple WHERE")
    void shouldBuildQueryWithSimpleWhere() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.raw("active = true"))
              .build();

      assertEquals("select name from users as u where active = true", sql);
    }

    @Test
    @DisplayName("should build query with AND conditions")
    void shouldBuildQueryWithAndConditions() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.and(Condition.raw("active = true"), Condition.raw("age >= 18")))
              .build();

      assertEquals("select name from users as u where active = true and age >= 18", sql);
    }
  }

  @Nested
  @DisplayName("GROUP BY and HAVING")
  class GroupByAndHaving {
    @Test
    @DisplayName("should build query with GROUP BY")
    void shouldBuildQueryWithGroupBy() {
      String sql =
          new SelectBuilder()
              .addColumn("department")
              .addColumn("count(*)", "", "total")
              .from("employees", "e")
              .groupBy("department")
              .build();

      assertEquals(
          "select department, count(*) as total from employees as e group by department", sql);
    }

    @Test
    @DisplayName("should build query with GROUP BY and HAVING")
    void shouldBuildQueryWithGroupByAndHaving() {
      String sql =
          new SelectBuilder()
              .addColumn("department")
              .addColumn("count(*)", "", "total")
              .from("employees", "e")
              .groupBy("department")
              .having(Condition.raw("count(*) > 10"))
              .build();

      assertEquals(
          "select department, count(*) as total from employees as e "
              + "group by department having count(*) > 10",
          sql);
    }
  }

  @Nested
  @DisplayName("Pagination")
  class Pagination {
    @Test
    @DisplayName("should build query with LIMIT")
    void shouldBuildQueryWithLimit() {
      String sql = new SelectBuilder().addColumn("name").from("users", "u").limit(10).build();

      assertEquals("select name from users as u limit 10", sql);
    }

    @Test
    @DisplayName("should build query with LIMIT and OFFSET")
    void shouldBuildQueryWithLimitAndOffset() {
      String sql =
          new SelectBuilder().addColumn("name").from("users", "u").limit(10).offset(20).build();

      assertEquals("select name from users as u limit 10 offset 20", sql);
    }

    @Test
    @DisplayName("should build query with LIMIT plus one")
    void shouldBuildQueryWithLimitPlusOne() {
      String sql =
          new SelectBuilder().addColumn("name").from("users", "u").limitPlusOne(10).build();

      assertEquals("select name from users as u limit 11", sql);
    }

    @Test
    @DisplayName("should build query with max LIMIT")
    void shouldBuildQueryWithMaxLimit() {
      String sql =
          new SelectBuilder().addColumn("name").from("users", "u").limitWithMax(100, 50).build();

      assertEquals("select name from users as u limit 50", sql);
    }
  }

  @Nested
  @DisplayName("SQL keyword case handling")
  class SqlKeywordCaseHandling {
    @Test
    @DisplayName("should lowercase CASE statement keywords")
    void shouldLowerCaseCaseStatementKeywords() {
      String sql =
          new SelectBuilder()
              .addColumn("CASE WHEN active THEN 'Active' ELSE 'Inactive' END", "", "status")
              .from("users", "u")
              .build();

      assertEquals(
          "select case when active then 'Active' else 'Inactive' end as status from users as u",
          sql);
    }

    @Test
    @DisplayName("should handle CASE statement in ORDER BY")
    void shouldHandleCaseStatementInOrderBy() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("CASE WHEN active THEN 1 ELSE 2 END ASC")
              .build();

      assertEquals(
          "select name from users as u order by case when active then 1 else 2 end asc", sql);
    }

    @Test
    @DisplayName("should handle multiple CASE statements")
    void shouldHandleMultipleCaseStatements() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy(
                  "CASE WHEN active THEN 1 ELSE 2 END ASC, "
                      + "CASE WHEN status = 'VIP' THEN 1 ELSE 2 END DESC")
              .build();

      assertEquals(
          "select name from users as u order by "
              + "case when active then 1 else 2 end asc, "
              + "case when status = 'VIP' then 1 else 2 end desc",
          sql);
    }

    @Test
    @DisplayName("should handle CASE statements with multiple WHEN clauses")
    void shouldHandleCaseStatementsWithMultipleWhenClauses() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy(
                  "CASE "
                      + "WHEN status = 'ACTIVE' THEN 1 "
                      + "WHEN status = 'PENDING' THEN 2 "
                      + "ELSE 3 END DESC")
              .build();

      assertEquals(
          "select name from users as u order by case "
              + "when status = 'ACTIVE' then 1 "
              + "when status = 'PENDING' then 2 "
              + "else 3 end desc",
          sql);
    }
  }

  @Nested
  @DisplayName("ORDER BY")
  class OrderBy {
    @Test
    @DisplayName("should build query with simple ORDER BY")
    void shouldBuildQueryWithSimpleOrderBy() {
      String sql =
          new SelectBuilder().addColumn("name").from("users", "u").orderBy("name", "asc").build();

      assertEquals("select name from users as u order by name asc", sql);
    }

    @Test
    @DisplayName("should build query with ORDER BY and NULL handling")
    void shouldBuildQueryWithOrderByAndNullHandling() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("name", "desc", "nulls last")
              .build();

      assertEquals("select name from users as u order by name desc nulls last", sql);
    }

    @Test
    @DisplayName("should parse ORDER BY clause from string")
    void shouldParseOrderByClauseFromString() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("order by name desc nulls last, created_at asc")
              .build();

      assertEquals(
          "select name from users as u order by name desc nulls last, created_at asc", sql);
    }

    @Test
    @DisplayName("should correctly handle ASC keyword")
    void shouldCorrectlyHandleAscKeyword() {
      String sql = "SELECT * FROM users ORDER BY name ASC, description DESC";
      String formatted = SqlFormatter.lowercase(sql);
      assertEquals("select * from users order by name asc, description desc", formatted);
    }

    @Test
    @DisplayName("should not affect words containing SQL keywords")
    void shouldNotAffectWordsContainingKeywords() {
      String sql = "SELECT description, ASCII(name) FROM users";
      String formatted = SqlFormatter.lowercase(sql);
      assertEquals("select description, ASCII(name) from users", formatted);
    }

    @Test
    @DisplayName("should handle keywords at start and end of string")
    void shouldHandleKeywordsAtBoundaries() {
      String sql = "ASC name DESC";
      String formatted = SqlFormatter.lowercase(sql);
      assertEquals("asc name desc", formatted);
    }
  }

  @Nested
  @DisplayName("ORDER BY parsing")
  class OrderByParsing {
    @Test
    @DisplayName("should handle simple direction")
    void shouldHandleSimpleDirection() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated DESC")
              .build();

      assertEquals("select name from users as u order by last_updated desc", sql);
    }

    @Test
    @DisplayName("should handle NULLS LAST without direction")
    void shouldHandleNullsLastWithoutDirection() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated NULLS LAST")
              .build();

      assertEquals("select name from users as u order by last_updated nulls last", sql);
    }

    @Test
    @DisplayName("should handle NULLS FIRST with direction")
    void shouldHandleNullsFirstWithDirection() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated DESC NULLS FIRST")
              .build();

      assertEquals("select name from users as u order by last_updated desc nulls first", sql);
    }

    @Test
    @DisplayName("should handle multiple columns with different combinations")
    void shouldHandleMultipleColumnsWithDifferentCombinations() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated DESC NULLS LAST, created_at ASC, name DESC NULLS FIRST")
              .build();

      assertEquals(
          "select name from users as u order by last_updated desc nulls last, "
              + "created_at asc, name desc nulls first",
          sql);
    }

    @Test
    @DisplayName("should handle column name containing direction words")
    void shouldHandleColumnNameContainingDirectionWords() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("description_asc DESC")
              .build();

      assertEquals("select name from users as u order by description_asc desc", sql);
    }
  }

  @Nested
  @DisplayName("ORDER BY clause combinations")
  class OrderByCombinations {
    @Test
    @DisplayName("should handle column only")
    void shouldHandleColumnOnly() {
      String sql =
          new SelectBuilder().addColumn("name").from("users", "u").orderBy("last_updated").build();

      assertEquals("select name from users as u order by last_updated", sql);
    }

    @Test
    @DisplayName("should handle explicit ASC")
    void shouldHandleExplicitAsc() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated ASC")
              .build();

      assertEquals("select name from users as u order by last_updated asc", sql);
    }

    @Test
    @DisplayName("should handle NULLS LAST without direction")
    void shouldHandleNullsLastWithoutDirection() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated NULLS LAST")
              .build();

      assertEquals("select name from users as u order by last_updated nulls last", sql);
    }

    @Test
    @DisplayName("should handle NULLS FIRST without direction")
    void shouldHandleNullsFirstWithoutDirection() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated NULLS FIRST")
              .build();

      assertEquals("select name from users as u order by last_updated nulls first", sql);
    }

    @Test
    @DisplayName("should handle multiple columns with different specifications")
    void shouldHandleMultipleColumnsWithDifferentSpecifications() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("status NULLS FIRST, created_at, updated_at DESC NULLS LAST")
              .build();

      assertEquals(
          "select name from users as u order by status nulls first, "
              + "created_at, updated_at desc nulls last",
          sql);
    }
  }

  @Nested
  @DisplayName("ORDER BY raw strings")
  class OrderByRawStrings {
    @Test
    @DisplayName("should handle ORDER BY with single column")
    void shouldHandleOrderByWithSingleColumn() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("created_at DESC")
              .build();

      assertEquals("select name from users as u order by created_at desc", sql);
    }

    @Test
    @DisplayName("should handle ORDER BY with multiple columns")
    void shouldHandleOrderByWithMultipleColumns() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_name ASC, first_name DESC")
              .build();

      assertEquals("select name from users as u order by last_name asc, first_name desc", sql);
    }

    @Test
    @DisplayName("should handle ORDER BY with NULLS handling")
    void shouldHandleOrderByWithNullsHandling() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_updated DESC NULLS LAST")
              .build();

      assertEquals("select name from users as u order by last_updated desc nulls last", sql);
    }

    @Test
    @DisplayName("should handle ORDER BY with 'order by' prefix")
    void shouldHandleOrderByWithPrefix() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("ORDER BY created_at DESC")
              .build();

      assertEquals("select name from users as u order by created_at desc", sql);
    }

    @Test
    @DisplayName("should handle empty ORDER BY")
    void shouldHandleEmptyOrderBy() {
      String sql = new SelectBuilder().addColumn("name").from("users", "u").orderBy("").build();

      assertEquals("select name from users as u", sql);
    }

    @Test
    @DisplayName("should handle null ORDER BY")
    void shouldHandleNullOrderBy() {
      String sql =
          new SelectBuilder().addColumn("name").from("users", "u").orderBy((String) null).build();

      assertEquals("select name from users as u", sql);
    }

    @Test
    @DisplayName("should handle multiple ORDER BY calls")
    void shouldHandleMultipleOrderByCalls() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_name ASC")
              .orderBy("first_name DESC")
              .build();

      assertEquals("select name from users as u order by last_name asc, first_name desc", sql);
    }

    @Test
    @DisplayName("should handle complex ORDER BY expression")
    void shouldHandleComplexOrderBy() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("CASE WHEN active THEN 1 ELSE 2 END ASC, created_at DESC NULLS LAST")
              .build();

      assertEquals(
          "select name from users as u order by case when active then 1 else 2 end asc, "
              + "created_at desc nulls last",
          sql);
    }
  }

  @Nested
  @DisplayName("WHERE raw conditions")
  class WhereRawConditions {
    @Test
    @DisplayName("should handle raw WHERE condition")
    void shouldHandleRawWhereCondition() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.raw("WHERE active = true"))
              .build();

      assertEquals("select name from users as u where active = true", sql);
    }

    @Test
    @DisplayName("should handle raw WHERE with AND")
    void shouldHandleRawWhereWithAnd() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.raw("WHERE active = true AND age >= 18"))
              .build();

      assertEquals("select name from users as u where active = true and age >= 18", sql);
    }

    @Test
    @DisplayName("should clean WHERE prefix from raw condition")
    void shouldCleanWherePrefixFromRawCondition() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.raw("WHERE status = 'ACTIVE'"))
              .build();

      assertEquals("select name from users as u where status = 'ACTIVE'", sql);
    }

    @Test
    @DisplayName("should clean WHERE prefix from raw condition")
    void shouldHandleRawWhereConditionWithNestedOr() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(
                  Condition.raw(
                      "WHERE ps = '12345' AND (status = 'ACTIVE' OR status = 'INACTIVE')"))
              .build();

      assertEquals(
          "select name from users as u where ps = '12345' and (status = 'ACTIVE' or status = 'INACTIVE')",
          sql);
    }

    @Test
    @DisplayName("should handle multiple nested conditions with mixed operators")
    void shouldHandleMultipleNestedConditions() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(
                  Condition.raw(
                      "WHERE (ps = '12345' OR ps = '67890') AND (status = 'ACTIVE' OR (status = 'INACTIVE' AND role = 'ADMIN'))"))
              .build();

      assertEquals(
          "select name from users as u where (ps = '12345' or ps = '67890') and (status = 'ACTIVE' or (status = 'INACTIVE' and role = 'ADMIN'))",
          sql);
    }

    @Test
    @DisplayName("should handle complex conditions with NOT operator")
    void shouldHandleComplexConditionsWithNot() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(
                  Condition.raw(
                      "WHERE NOT (ps = '12345' AND status = 'ACTIVE') OR (role = 'ADMIN' AND NOT status = 'INACTIVE')"))
              .build();

      assertEquals(
          "select name from users as u where not (ps = '12345' and status = 'ACTIVE') or (role = 'ADMIN' and not status = 'INACTIVE')",
          sql);
    }

    @Test
    @DisplayName("should handle conditions with IN and BETWEEN operators")
    void shouldHandleInAndBetweenOperators() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(
                  Condition.raw(
                      "WHERE ps IN ('12345', '67890') AND created_at BETWEEN '2023-01-01' AND '2023-12-31'"))
              .build();

      assertEquals(
          "select name from users as u where ps IN ('12345', '67890') and created_at BETWEEN '2023-01-01' and '2023-12-31'",
          sql);
    }

    @Test
    @DisplayName("should handle conditions with LIKE and IS NULL operators")
    void shouldHandleLikeAndIsNullOperators() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.raw("WHERE name LIKE '%John%' AND email IS NULL"))
              .build();

      assertEquals("select name from users as u where name LIKE '%John%' and email IS NULL", sql);
    }

    @Test
    @DisplayName("should handle conditions with subqueries")
    void shouldHandleConditionsWithSubqueries() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(
                  Condition.raw(
                      "WHERE ps = (SELECT ps FROM profiles WHERE user_id = u.id) AND status = 'ACTIVE'"))
              .build();

      assertEquals(
          "select name from users as u where ps = (select ps from profiles where user_id = u.id) and status = 'ACTIVE'",
          sql);
    }

    @Test
    @DisplayName("should clean AND prefix from raw condition")
    void shouldCleanAndPrefixFromRawCondition() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.raw("AND status = 'ACTIVE'"))
              .build();

      assertEquals("select name from users as u where status = 'ACTIVE'", sql);
    }
  }

  @Nested
  @DisplayName("Mixed raw and structured conditions")
  class MixedConditions {
    @Test
    @DisplayName("should handle mix of raw and structured ORDER BY")
    void shouldHandleMixedOrderBy() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .orderBy("last_name ASC")
              .orderBy("created_at", "DESC", "NULLS LAST")
              .build();

      assertEquals(
          "select name from users as u order by last_name asc, created_at desc nulls last", sql);
    }

    @Test
    @DisplayName("should handle mix of raw and structured WHERE conditions")
    void shouldHandleMixedWhereConditions() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.and(Condition.raw("status = 'ACTIVE'"), Condition.raw("age >= 18")))
              .build();

      assertEquals("select name from users as u where status = 'ACTIVE' and age >= 18", sql);
    }

    @Test
    @DisplayName("should handle conditions with CASE statements")
    void shouldHandleConditionsWithCaseStatements() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(
                  Condition.raw(
                      "WHERE CASE WHEN status = 'ACTIVE' THEN ps = '12345' ELSE ps = '67890' END"))
              .build();

      assertEquals(
          "select name from users as u where case when status = 'ACTIVE' then ps = '12345' else ps = '67890' end",
          sql);
    }

    @Test
    @DisplayName("should handle conditions with EXISTS operator")
    void shouldHandleConditionsWithExists() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(Condition.raw("WHERE EXISTS (SELECT 1 FROM profiles WHERE user_id = u.id)"))
              .build();

      assertEquals(
          "select name from users as u where EXISTS (select 1 from profiles where user_id = u.id)",
          sql);
    }

    @Test
    @DisplayName("should handle complex parentheses grouping")
    void shouldHandleComplexParenthesesGrouping() {
      String sql =
          new SelectBuilder()
              .addColumn("name")
              .from("users", "u")
              .where(
                  Condition.raw(
                      "WHERE (ps = '12345' OR (status = 'ACTIVE' AND role = 'ADMIN')) AND (created_at > '2023-01-01' OR updated_at < '2023-12-31')"))
              .build();

      assertEquals(
          "select name from users as u where (ps = '12345' or (status = 'ACTIVE' and role = 'ADMIN')) and (created_at > '2023-01-01' or updated_at < '2023-12-31')",
          sql);
    }
  }
}
