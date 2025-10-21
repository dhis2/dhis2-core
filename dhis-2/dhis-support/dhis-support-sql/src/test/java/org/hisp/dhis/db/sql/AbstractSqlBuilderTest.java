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
package org.hisp.dhis.db.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link AbstractSqlBuilder} using {@link PostgreSqlBuilder} as concrete implementation.
 *
 * @author Lars Helge Overland
 */
@DisplayName("AbstractSqlBuilder.aggrDecimal() Tests")
class AbstractSqlBuilderTest {

  private PostgreSqlBuilder sqlBuilder;

  @BeforeEach
  void setUp() {
    sqlBuilder = new PostgreSqlBuilder();
  }

  @ParameterizedTest
  @DisplayName("Should decimalize numeric aggregate functions")
  @CsvSource({
    "'AVG(value)', 10, 2, 'AVG((value)::numeric(10,2))'",
    "'SUM(amount)', 15, 4, 'SUM((amount)::numeric(15,4))'",
    "'MIN(price)', 12, 3, 'MIN((price)::numeric(12,3))'",
    "'MAX(salary)', 18, 2, 'MAX((salary)::numeric(18,2))'",
    "'STDDEV(measurement)', 10, 5, 'STDDEV((measurement)::numeric(10,5))'",
    "'VARIANCE(score)', 12, 6, 'VARIANCE((score)::numeric(12,6))'"
  })
  void testAggrDecimalWithNumericAggregates(
      String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @DisplayName("Should NOT decimalize COUNT variants (returns unchanged)")
  @CsvSource({
    "'COUNT(id)', 10, 2, 'COUNT(id)'",
    "'COUNT(*)', 10, 2, 'COUNT(*)'",
    "'count(records)', 10, 2, 'count(records)'",
    "'COUNT(DISTINCT user_id)', 15, 3, 'COUNT(DISTINCT user_id)'",
    "'count(distinct email)', 12, 4, 'count(distinct email)'"
  })
  void testAggrDecimalWithCountVariants(String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result, "COUNT variants should be returned unchanged");
  }

  @ParameterizedTest
  @DisplayName("Should decimalize aggregates with DISTINCT keyword")
  @CsvSource({
    "'AVG(DISTINCT value)', 10, 2, 'AVG(DISTINCT (value)::numeric(10,2))'",
    "'SUM(DISTINCT amount)', 15, 4, 'SUM(DISTINCT (amount)::numeric(15,4))'",
    "'sum(distinct price)', 12, 3, 'SUM(DISTINCT (price)::numeric(12,3))'",
    "'MIN(DISTINCT salary)', 18, 2, 'MIN(DISTINCT (salary)::numeric(18,2))'",
    "'MAX(  DISTINCT   total  )', 20, 5, 'MAX(DISTINCT (total)::numeric(20,5))'"
  })
  void testAggrDecimalWithDistinctKeyword(String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @DisplayName("Should handle various precision and scale combinations")
  @CsvSource({
    "'AVG(value)', 5, 2, 'AVG((value)::numeric(5,2))'",
    "'SUM(amount)', 10, 0, 'SUM((amount)::numeric(10,0))'",
    "'MIN(price)', 38, 10, 'MIN((price)::numeric(38,10))'",
    "'MAX(total)', 18, 18, 'MAX((total)::numeric(18,18))'",
    "'AVG(data)', 1, 0, 'AVG((data)::numeric(1,0))'",
    "'SUM(val)', 30, 15, 'SUM((val)::numeric(30,15))'"
  })
  void testAggrDecimalWithVariousPrecisionScaleCombinations(
      String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @DisplayName("Should handle complex inner expressions")
  @CsvSource(
      delimiter = '|',
      value = {
        "SUM(price * quantity) | 20 | 4 | SUM((price * quantity)::numeric(20,4))",
        "SUM((price + tax) * quantity) | 20 | 2 | SUM(((price + tax) * quantity)::numeric(20,2))",
        "AVG(table.column) | 15 | 3 | AVG((table.column)::numeric(15,3))",
        "MAX(schema.table.column) | 12 | 4 | MAX((schema.table.column)::numeric(12,4))"
      })
  void testAggrDecimalWithComplexExpressions(
      String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @DisplayName("Should handle aggregates with whitespace variations")
  @CsvSource({
    "'AVG(  value  )', 10, 2, 'AVG((value)::numeric(10,2))'",
    "'SUM( amount )', 15, 4, 'SUM((amount)::numeric(15,4))'",
    "'MIN(price)', 12, 3, 'MIN((price)::numeric(12,3))'",
    "'  AVG(value)  ', 10, 2, 'AVG((value)::numeric(10,2))'"
  })
  void testAggrDecimalWithWhitespaceVariations(
      String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @DisplayName("Should handle case variations in function names")
  @CsvSource({
    "'avg(value)', 10, 2, 'AVG((value)::numeric(10,2))'",
    "'sum(amount)', 15, 4, 'SUM((amount)::numeric(15,4))'",
    "'Min(price)', 12, 3, 'MIN((price)::numeric(12,3))'",
    "'mAx(salary)', 18, 2, 'MAX((salary)::numeric(18,2))'",
    "'StdDev(data)', 10, 5, 'STDDEV((data)::numeric(10,5))'"
  })
  void testAggrDecimalWithCaseVariations(String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Should handle expression with no parentheses")
  void testAggrDecimalWithNoParentheses() {
    String result = sqlBuilder.aggrDecimal("simple_value", 8, 3);
    assertEquals("avg((simple_value)::numeric(8,3))", result);
  }

  @ParameterizedTest
  @DisplayName("Should handle nested function calls in aggregate")
  @CsvSource(
      delimiter = '|',
      value = {
        "AVG(COALESCE(price, 0)) | 10 | 2 | AVG((COALESCE(price, 0))::numeric(10,2))",
        "SUM(NULLIF(amount, 0)) | 15 | 4 | SUM((NULLIF(amount, 0))::numeric(15,4))",
        "MAX(GREATEST(a, b, c)) | 12 | 3 | MAX((GREATEST(a, b, c))::numeric(12,3))",
        "MIN(ABS(value)) | 10 | 2 | MIN((ABS(value))::numeric(10,2))"
      })
  void testAggrDecimalWithNestedFunctions(String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @DisplayName("Should preserve DISTINCT with COUNT and return unchanged")
  @CsvSource({
    "'COUNT(DISTINCT id)', 10, 2",
    "'count(DISTINCT user_id)', 15, 4",
    "'COUNT(  DISTINCT  email  )', 12, 3"
  })
  void testAggrDecimalCountDistinctUnchanged(String input, int precision, int scale) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    // For COUNT, the original expression should be returned unchanged
    // We need to normalize whitespace for comparison
    String normalizedInput = input.replaceAll("\\s+", " ").trim();
    String normalizedResult = result.replaceAll("\\s+", " ").trim();
    assertEquals(normalizedInput, normalizedResult);
  }

  @Test
  @DisplayName("Should handle aggregate with arithmetic expression")
  void testAggrDecimalWithArithmeticExpression() {
    String result = sqlBuilder.aggrDecimal("AVG(price * 1.1 + discount)", 20, 4);
    assertEquals("AVG((price * 1.1 + discount)::numeric(20,4))", result);
  }

  @Test
  @DisplayName("Should handle aggregate with column reference containing underscores")
  void testAggrDecimalWithUnderscoreColumns() {
    String result = sqlBuilder.aggrDecimal("SUM(user_total_amount)", 15, 2);
    assertEquals("SUM((user_total_amount)::numeric(15,2))", result);
  }

  @Test
  @DisplayName("Should handle aggregate with quoted identifiers in expression")
  void testAggrDecimalWithQuotedIdentifiers() {
    String result = sqlBuilder.aggrDecimal("AVG(\"quoted_column\")", 10, 2);
    assertEquals("AVG((\"quoted_column\")::numeric(10,2))", result);
  }

  @ParameterizedTest
  @DisplayName("Should handle edge case precision and scale values")
  @CsvSource({
    "'AVG(value)', 1, 0, 'AVG((value)::numeric(1,0))'",
    "'SUM(amount)', 38, 0, 'SUM((amount)::numeric(38,0))'",
    "'MAX(price)', 38, 38, 'MAX((price)::numeric(38,38))'"
  })
  void testAggrDecimalWithEdgeCasePrecisionScale(
      String input, int precision, int scale, String expected) {
    String result = sqlBuilder.aggrDecimal(input, precision, scale);
    assertEquals(expected, result);
  }
}
