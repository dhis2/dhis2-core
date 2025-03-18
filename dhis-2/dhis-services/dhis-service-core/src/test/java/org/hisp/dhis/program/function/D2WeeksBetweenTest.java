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
class D2WeeksBetweenTest {

  @Mock private CommonExpressionVisitor visitor;

  @Nested
  class DorisTests {
    private D2WeeksBetween function;

    @BeforeEach
    void setUp() {
      function = new D2WeeksBetween();
      SqlBuilder sqlBuilder = new DorisSqlBuilder("doris", "doris");
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralDates() {
      // Arrange
      String startDate = "'2023-01-01'";
      String endDate = "'2023-01-15'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(WEEK, '2023-01-01', '2023-01-15')", result);
    }

    @Test
    void shouldGenerateSqlForColumnReferences() {
      // Arrange
      String startDate = "enrollment_date";
      String endDate = "incident_date";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(WEEK, enrollment_date, incident_date)", result);
    }

    @Test
    void shouldGenerateSqlForDifferentYears() {
      // Arrange
      String startDate = "'2022-12-25'";
      String endDate = "'2023-01-08'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(WEEK, '2022-12-25', '2023-01-08')", result);
    }
  }

  @Nested
  class PostgresTests {
    private D2WeeksBetween function;

    @BeforeEach
    void setUp() {
      function = new D2WeeksBetween();
      SqlBuilder sqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralDates() {
      // Arrange
      String startDate = "'2023-01-01'";
      String endDate = "'2023-01-15'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("((cast('2023-01-15' as date) - cast('2023-01-01' as date)) / 7)", result);
    }

    @Test
    void shouldGenerateSqlForColumnReferences() {
      // Arrange
      String startDate = "enrollment_date";
      String endDate = "incident_date";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("((cast(incident_date as date) - cast(enrollment_date as date)) / 7)", result);
    }

    @Test
    void shouldGenerateSqlForDifferentYears() {
      // Arrange
      String startDate = "'2022-12-25'";
      String endDate = "'2023-01-08'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("((cast('2023-01-08' as date) - cast('2022-12-25' as date)) / 7)", result);
    }
  }

  @Nested
  class EdgeCases {
    private D2WeeksBetween function;

    @BeforeEach
    void setUp() {
      function = new D2WeeksBetween();
    }

    @Test
    void shouldHandleWeekBoundaries() {
      // Arrange
      SqlBuilder postgresSqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(postgresSqlBuilder);

      String startDate = "'2023-01-01'"; // Sunday
      String endDate = "'2023-01-08'"; // Next Sunday

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals(
          "((cast('2023-01-08' as date) - cast('2023-01-01' as date)) / 7)",
          result,
          "Should correctly handle full week difference");
    }

    @Test
    void shouldHandleYearEndWeeks() {
      // Arrange
      SqlBuilder postgresSqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(postgresSqlBuilder);

      String startDate = "'2022-12-26'"; // Last week of 2022
      String endDate = "'2023-01-02'"; // First week of 2023

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals(
          "((cast('2023-01-02' as date) - cast('2022-12-26' as date)) / 7)",
          result,
          "Should correctly handle week difference across year boundary");
    }
  }
}
