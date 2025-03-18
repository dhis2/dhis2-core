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

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SqlAliasReplacerTest {

  @Test
  @DisplayName("Should handle columns without aliases")
  void testColumnsWithoutAliases() {
    List<String> columns = Arrays.asList("employee", "country");
    String input = "employee = 10 and country = 'IT'";
    String expected = "%s.employee = 10 AND %s.country = 'IT'";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle mix of aliased and non-aliased columns")
  void testMixedAliasedAndNonAliasedColumns() {
    List<String> columns = Arrays.asList("employee", "country", "status");
    String input = "employee = 10 and ax.country = 'IT' and status = 'ACTIVE'";
    String expected = "%s.employee = 10 AND %s.country = 'IT' AND %s.status = 'ACTIVE'";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle case insensitive column names")
  void testCaseInsensitiveColumns() {
    List<String> columns = Arrays.asList("Employee", "COUNTRY");
    String input = "employee = 10 and country = 'IT'";
    String expected = "%s.employee = 10 AND %s.country = 'IT'";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should not modify non-column words")
  void testNonColumnWords() {
    List<String> columns = Arrays.asList("employee", "country");
    String input = "employee = 10 and country = 'IT' and status = 'ACTIVE'";
    String expected = "%s.employee = 10 AND %s.country = 'IT' AND status = 'ACTIVE'";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle complex conditions with functions")
  void testComplexConditionsWithFunctions() {
    List<String> columns = Arrays.asList("date", "amount");
    String input = "EXTRACT(YEAR FROM date) = 2023 and COALESCE(amount, 0) > 100";
    String expected = "EXTRACT(YEAR FROM %s.date) = 2023 AND COALESCE(%s.amount, 0) > 100";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle quoted column names")
  void testQuotedColumnNames() {
    List<String> columns = Arrays.asList("employee", "country");
    String input = "\"employee\" = 10 and `country` = 'IT'";
    String expected = "%s.\"employee\" = 10 AND %s.`country` = 'IT'";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle OR conditions")
  void testOrConditions() {
    List<String> columns = Arrays.asList("employee", "country", "age");
    String input = "employee = 10 OR ax.country = 'IT' OR age > 25";
    String expected = "%s.employee = 10 OR %s.country = 'IT' OR %s.age > 25";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle mixed AND/OR conditions with parentheses")
  void testMixedConditionsWithParentheses() {
    List<String> columns = Arrays.asList("employee", "country", "age", "salary");
    String input = "(employee = 10 OR ax.country = 'IT') AND (age > 25 OR by.salary >= 50000)";
    String expected =
        "(%s.employee = 10 OR %s.country = 'IT') AND (%s.age > 25 OR %s.salary >= 50000)";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle various SQL functions")
  void testVariousSqlFunctions() {
    List<String> columns = Arrays.asList("date", "name", "salary");
    String input =
        "UPPER(name) = 'JOHN' AND DATE_TRUNC('month', ax.date) = '2023-01-01' AND ABS(by.salary) > 1000";
    String expected =
        "UPPER(%s.name) = 'JOHN' AND DATE_TRUNC('month', %s.date) = '2023-01-01' AND ABS(%s.salary) > 1000";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle nested functions")
  void testNestedFunctions() {
    List<String> columns = Arrays.asList("date", "amount");
    String input =
        "ROUND(COALESCE(ax.amount, 0) * 100, 2) > 1000 AND EXTRACT(YEAR FROM DATE_TRUNC('month', date)) = 2023";
    String expected =
        "ROUND(COALESCE(%s.amount, 0) * 100, 2) > 1000 AND EXTRACT(YEAR FROM DATE_TRUNC('month', %s.date)) = 2023";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle IN conditions")
  void testInConditions() {
    List<String> columns = Arrays.asList("country", "status");
    String input = "ax.country IN ('US', 'UK', 'IT') AND status IN ('ACTIVE', 'PENDING')";
    String expected = "%s.country IN ('US', 'UK', 'IT') AND %s.status IN ('ACTIVE', 'PENDING')";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle BETWEEN conditions")
  void testBetweenConditions() {
    List<String> columns = Arrays.asList("date", "amount");
    String input = "ax.date BETWEEN '2023-01-01' AND '2023-12-31' AND amount BETWEEN 100 AND 1000";
    String expected =
        "%s.date BETWEEN '2023-01-01' AND '2023-12-31' AND %s.amount BETWEEN 100 AND 1000";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle NULL comparisons")
  void testNullComparisons() {
    List<String> columns = Arrays.asList("name", "date");
    String input = "ax.name IS NULL AND date IS NOT NULL";
    String expected = "%s.name IS NULL AND %s.date IS NOT NULL";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle LIKE conditions")
  void testLikeConditions() {
    List<String> columns = Arrays.asList("name", "email");
    String input = "ax.name LIKE 'John%' AND email NOT LIKE '%test%'";
    String expected = "%s.name LIKE 'John%' AND %s.email NOT LIKE '%test%'";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle mathematical expressions")
  void testMathematicalExpressions() {
    List<String> columns = Arrays.asList("price", "quantity", "discount");
    String input = "ax.price * by.quantity * (1 - discount/100) > 1000";
    String expected = "%s.price * %s.quantity * (1 - %s.discount / 100) > 1000";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle subqueries in conditions")
  void testSubqueries() {
    List<String> columns = Arrays.asList("department_id", "salary");
    String input =
        "ax.department_id IN (SELECT id FROM departments) AND salary > (SELECT AVG(by.salary) FROM employees)";
    String expected =
        "%s.department_id IN (SELECT id FROM departments) AND %s.salary > (SELECT AVG(salary) FROM employees)";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle CASE expressions")
  void testCaseExpressions() {
    List<String> columns = Arrays.asList("status", "amount");
    String input = "CASE WHEN ax.status = 'ACTIVE' THEN by.amount * 1.1 ELSE amount END > 1000";
    String expected =
        "CASE WHEN %s.status = 'ACTIVE' THEN %s.amount * 1.1 ELSE %s.amount END > 1000";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle special characters in column names")
  void testSpecialCharactersInColumnNames() {
    List<String> columns = Arrays.asList("first_name", "last_name", "email_address");
    String input =
        "ax.\"first_name\" = 'John' AND by.`last_name` = 'Doe' AND \"email_address\" LIKE '%@%'";
    String expected =
        "%s.\"first_name\" = 'John' AND %s.`last_name` = 'Doe' AND %s.\"email_address\" LIKE '%@%'";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle nested functions in subqueries")
  void testNestedFunctionsInSubqueries() {
    List<String> columns = Arrays.asList("salary", "bonus");
    String input = "salary > (SELECT MAX(COALESCE(ax.salary + by.bonus, 0)) FROM employees)";
    String expected = "%s.salary > (SELECT MAX(COALESCE(salary + bonus, 0)) FROM employees)";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle multiple subqueries with functions")
  void testMultipleSubqueriesWithFunctions() {
    List<String> columns = Arrays.asList("salary", "age");
    String input =
        "ax.salary > (SELECT AVG(by.salary) FROM emp1) AND age < (SELECT MAX(cx.age) FROM emp2)";
    String expected =
        "%s.salary > (SELECT AVG(salary) FROM emp1) AND %s.age < (SELECT MAX(age) FROM emp2)";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle subqueries with multiple function arguments")
  void testSubqueriesWithMultipleFunctionArguments() {
    List<String> columns = Arrays.asList("salary", "bonus", "tax");
    String input = "ax.salary > (SELECT SUM(by.salary + cx.bonus - dx.tax) FROM employees)";
    String expected = "%s.salary > (SELECT SUM(salary + bonus - tax) FROM employees)";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }

  @Test
  @DisplayName("Should handle subqueries with conditional functions")
  void testSubqueriesWithConditionalFunctions() {
    List<String> columns = Arrays.asList("salary", "status");
    String input =
        "ax.salary > (SELECT AVG(CASE WHEN by.status = 'ACTIVE' THEN cx.salary ELSE 0 END) FROM employees)";
    String expected =
        "%s.salary > (SELECT AVG(CASE WHEN status = 'ACTIVE' THEN salary ELSE 0 END) FROM employees)";
    assertEquals(expected, SqlAliasReplacer.replaceTableAliases(input, columns));
  }
}
