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
package org.hisp.dhis.program.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.ParserExceptionWithoutContext;
import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.parser.expression.antlr.ExpressionParser;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class D2CountIfConditionTest {

  @Mock private CommonExpressionVisitor visitor;
  @Mock private ProgramIndicatorService programIndicatorService;
  @Mock private ExpressionParser.ExprContext ctx;
  @Mock private ExpressionParser.StringLiteralContext stringLiteralNode; // Correct type
  @Mock private ProgramExpressionParams programExpressionParams;
  @Mock private ProgramIndicator programIndicator;

  private final D2CountIfCondition d2CountIfCondition = new D2CountIfCondition();

  @BeforeEach
  void setUp() {
    lenient().when(visitor.getProgramIndicatorService()).thenReturn(programIndicatorService);
    lenient().when(visitor.getProgParams()).thenReturn(programExpressionParams);
    lenient().when(programExpressionParams.getReportingStartDate()).thenReturn(new Date());
    lenient().when(programExpressionParams.getReportingEndDate()).thenReturn(new Date());
  }

  static Stream<Arguments> sqlConditionProvider() {
    // Arguments: SqlBuilder, baseColumnName, conditionLiteral (unquoted),
    //            dummyExpr, expectedDummyZeroSql, expectedFullDummySql, expectedFinalSql
    return Stream.of(
        Arguments.of(
            new PostgreSqlBuilder(),
            "\"de_uid\"",
            "< 11",
            "0< 11",
            "0::numeric",
            "0::numeric < 11::numeric",
            "\"de_uid\"::numeric < 11::numeric"), // Less Than
        Arguments.of(
            new PostgreSqlBuilder(),
            "\"de_uid\"",
            "> -5",
            "0> -5",
            "0::numeric",
            "0::numeric > (-5)::numeric",
            "\"de_uid\"::numeric > (-5)::numeric"), // Greater Than Negative
        Arguments.of(
            new PostgreSqlBuilder(),
            "\"de_uid\"",
            "<= 99.9",
            "0<= 99.9",
            "0::numeric",
            "0::numeric <= 99.9::numeric",
            "\"de_uid\"::numeric <= 99.9::numeric"), // Less Equal Decimal
        Arguments.of(
            new PostgreSqlBuilder(),
            "\"other_col\"",
            "!= 100",
            "0!= 100",
            "0::numeric",
            "0::numeric != 100::numeric",
            "\"other_col\"::numeric != 100::numeric"), // Not Equal
        Arguments.of(
            new PostgreSqlBuilder(),
            "\"other_col\"",
            "= 50",
            "0= 50",
            "0::numeric",
            "0::numeric = 50::numeric",
            "\"other_col\"::numeric = 50::numeric"), // Equal
        Arguments.of(
            new DorisSqlBuilder("", ""),
            "`de_uid`",
            "< 11",
            "0< 11",
            "CAST(0 AS DECIMAL)",
            "CAST(0 AS DECIMAL) < CAST(11 AS DECIMAL)",
            "CAST(`de_uid` AS DECIMAL) < CAST(11 AS DECIMAL)"), // Less Than
        Arguments.of(
            new DorisSqlBuilder("", ""),
            "`de_uid`",
            "> -5",
            "0> -5",
            "CAST(0 AS DECIMAL)",
            "CAST(0 AS DECIMAL) > CAST(-5 AS DECIMAL)",
            "CAST(`de_uid` AS DECIMAL) > CAST(-5 AS DECIMAL)"), // Greater Than Negative
        Arguments.of(
            new DorisSqlBuilder("", ""),
            "`de_uid`",
            "<= 99.9",
            "0<= 99.9",
            "CAST(0 AS DECIMAL)",
            "CAST(0 AS DECIMAL) <= CAST(99.9 AS DECIMAL)",
            "CAST(`de_uid` AS DECIMAL) <= CAST(99.9 AS DECIMAL)"), // Less Equal Decimal
        Arguments.of(
            new DorisSqlBuilder("", ""),
            "`other_col`",
            "!= 100",
            "0!= 100",
            "CAST(0 AS DECIMAL)",
            "CAST(0 AS DECIMAL) != CAST(100 AS DECIMAL)",
            "CAST(`other_col` AS DECIMAL) != CAST(100 AS DECIMAL)"), // Not Equal
        Arguments.of(
            new DorisSqlBuilder("", ""),
            "`other_col`",
            "= 50",
            "0= 50",
            "CAST(0 AS DECIMAL)",
            "CAST(0 AS DECIMAL) = CAST(50 AS DECIMAL)",
            "CAST(`other_col` AS DECIMAL) = CAST(50 AS DECIMAL)") // Equal
        );
  }

  @ParameterizedTest(name = "[{index}] {0} - Cond: ''{2}'' -> {6}")
  @MethodSource("sqlConditionProvider")
  @DisplayName("Should generate correct SQL condition for various operators and dialects")
  void testConditionSqlParameterized(
      SqlBuilder sqlBuilder,
      String baseColumnName,
      String conditionLiteral,
      String dummyExpr,
      String expectedDummyZeroSql,
      String expectedFullDummySql,
      String expectedFinalSql) {

    when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);

    when(programExpressionParams.getProgramIndicator()).thenReturn(programIndicator);

    // Mock the context to return the condition literal via the StringLiteralContext node
    String quotedConditionLiteral = "'" + conditionLiteral + "'";
    when(ctx.stringLiteral()).thenReturn(stringLiteralNode);
    when(stringLiteralNode.getText()).thenReturn(quotedConditionLiteral);

    // Mock the ProgramIndicatorService calls for dummy expressions
    when(programIndicatorService.getAnalyticsSql(
            eq(dummyExpr),
            eq(DataType.BOOLEAN),
            any(ProgramIndicator.class),
            any(Date.class),
            any(Date.class)))
        .thenReturn(expectedFullDummySql);

    when(programIndicatorService.getAnalyticsSql(
            eq("0"),
            eq(DataType.NUMERIC),
            any(ProgramIndicator.class),
            any(Date.class),
            any(Date.class)))
        .thenReturn(expectedDummyZeroSql);

    // Method under test
    String actualSql = d2CountIfCondition.getConditionSql(ctx, visitor, baseColumnName);

    assertEquals(expectedFinalSql, actualSql);

    verify(programIndicatorService, times(1))
        .getAnalyticsSql(
            eq(dummyExpr), eq(DataType.BOOLEAN), any(), any(Date.class), any(Date.class));
    verify(programIndicatorService, times(1))
        .getAnalyticsSql(eq("0"), eq(DataType.NUMERIC), any(), any(Date.class), any(Date.class));
    verify(ctx, times(1)).stringLiteral();
    verify(stringLiteralNode, times(1)).getText();
    verify(visitor, times(1)).getSqlBuilder(); // Verify SqlBuilder was requested
  }

  @Test
  @DisplayName("Should throw ParserExceptionWithoutContext if condition literal is missing")
  void testMissingConditionLiteralThrowsException() {
    String baseColumnName = "\"some_col\"";
    when(ctx.stringLiteral()).thenReturn(null); // Simulate missing node

    ParserExceptionWithoutContext exception =
        assertThrows(
            ParserExceptionWithoutContext.class,
            () -> d2CountIfCondition.getConditionSql(ctx, visitor, baseColumnName));

    assertTrue(exception.getMessage().contains("Condition string literal is missing"));
  }
}
