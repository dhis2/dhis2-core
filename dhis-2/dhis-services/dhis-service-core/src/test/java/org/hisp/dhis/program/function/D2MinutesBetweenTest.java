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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class D2MinutesBetweenTest {

  @Mock private CommonExpressionVisitor visitor;

  @Nested
  class PostgresTests {
    private D2MinutesBetween function;

    @BeforeEach
    void setUp() {
      function = new D2MinutesBetween();
      SqlBuilder sqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralTimestamps() {
      // Arrange
      String startDate = "'2023-01-01 10:00:00'";
      String endDate = "'2023-01-01 10:30:00'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals(
          "(extract(epoch from (cast('2023-01-01 10:30:00' as timestamp) - "
              + "cast('2023-01-01 10:00:00' as timestamp))) / 60)",
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
          "(extract(epoch from (cast(incident_date as timestamp) - "
              + "cast(enrollment_date as timestamp))) / 60)",
          result);
    }

    @Test
    void shouldGenerateSqlForDifferentDays() {
      // Arrange
      String startDate = "'2023-01-01 23:30:00'";
      String endDate = "'2023-01-02 00:30:00'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals(
          "(extract(epoch from (cast('2023-01-02 00:30:00' as timestamp) - "
              + "cast('2023-01-01 23:30:00' as timestamp))) / 60)",
          result);
    }
  }

  @Nested
  class DorisTests {
    private D2MinutesBetween function;

    @BeforeEach
    void setUp() {
      function = new D2MinutesBetween();
      SqlBuilder sqlBuilder = new DorisSqlBuilder("doris", "doris");
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralTimestamps() {
      // Arrange
      String startDate = "'2023-01-01 10:00:00'";
      String endDate = "'2023-01-01 10:30:00'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(MINUTE, '2023-01-01 10:00:00', '2023-01-01 10:30:00')", result);
    }

    @Test
    void shouldGenerateSqlForColumnReferences() {
      // Arrange
      String startDate = "enrollment_date";
      String endDate = "incident_date";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(MINUTE, enrollment_date, incident_date)", result);
    }

    @Test
    void shouldGenerateSqlForDifferentDays() {
      // Arrange
      String startDate = "'2023-01-01 23:30:00'";
      String endDate = "'2023-01-02 00:30:00'";

      // Act
      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      // Assert
      assertEquals("TIMESTAMPDIFF(MINUTE, '2023-01-01 23:30:00', '2023-01-02 00:30:00')", result);
    }
  }
}
