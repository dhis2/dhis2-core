/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.hisp.dhis.db.sql.DorisSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class D2YearsBetweenTest {

  @Mock private CommonExpressionVisitor visitor;

  @Nested
  class DorisTests {
    private D2YearsBetween function;

    @BeforeEach
    void setUp() {
      function = new D2YearsBetween();
      SqlBuilder sqlBuilder = new DorisSqlBuilder("doris", "doris");
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralDates() {
      // Arrange
      String startDate = "'2020-01-01'";
      String endDate = "'2023-01-01'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(YEAR, '2020-01-01', '2023-01-01')", result);
    }

    @Test
    void shouldGenerateSqlForColumnReferences() {
      // Arrange
      String startDate = "enrollment_date";
      String endDate = "incident_date";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(YEAR, enrollment_date, incident_date)", result);
    }

    @Test
    void shouldGenerateSqlForPartialYears() {
      // Arrange
      String startDate = "'2020-06-15'";
      String endDate = "'2023-03-01'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(YEAR, '2020-06-15', '2023-03-01')", result);
    }
  }

  @Nested
  class PostgresTests {
    private D2YearsBetween function;

    @BeforeEach
    void setUp() {
      function = new D2YearsBetween();
      SqlBuilder sqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralDates() {
      // Arrange
      String startDate = "'2020-01-01'";
      String endDate = "'2023-01-01'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals(
          "(date_part('year',age(cast('2023-01-01' as date), cast('2020-01-01' as date))))",
          result);
    }

    @Test
    void shouldGenerateSqlForColumnReferences() {
      // Arrange
      String startDate = "enrollment_date";
      String endDate = "incident_date";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals(
          "(date_part('year',age(cast(incident_date as date), cast(enrollment_date as date))))",
          result);
    }

    @Test
    void shouldGenerateSqlForPartialYears() {
      // Arrange
      String startDate = "'2020-06-15'";
      String endDate = "'2023-03-01'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals(
          "(date_part('year',age(cast('2023-03-01' as date), cast('2020-06-15' as date))))",
          result);
    }
  }

  @Nested
  class EdgeCases {
    private D2YearsBetween function;

    @BeforeEach
    void setUp() {
      function = new D2YearsBetween();
    }

    @Test
    void shouldHandleLeapYears() {
      SqlBuilder sqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);

      String startDate = "'2020-02-29'"; // Leap year
      String endDate = "'2023-02-28'"; // Non-leap year

      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      assertEquals(
          "(date_part('year',age(cast('2023-02-28' as date), cast('2020-02-29' as date))))",
          result,
          "Should correctly handle leap year dates");
    }

    @Test
    void shouldHandleCenturyBoundary() {
      SqlBuilder sqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);

      String startDate = "'1999-12-31'";
      String endDate = "'2000-01-01'";

      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      assertEquals(
          "(date_part('year',age(cast('2000-01-01' as date), cast('1999-12-31' as date))))",
          result,
          "Should correctly handle century boundary");
    }
  }
}
