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

@ExtendWith({MockitoExtension.class})
class D2DaysBetweenTest {

  @Mock private CommonExpressionVisitor visitor;

  @Nested
  class PostgresTests {
    private D2DaysBetween function;
    private SqlBuilder sqlBuilder;

    @BeforeEach
    void setUp() {
      function = new D2DaysBetween();
      sqlBuilder = new PostgreSqlBuilder();
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralDates() {
      String startDate = "'2023-01-01'";
      String endDate = "'2023-01-02'";

      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      assertEquals("(cast('2023-01-02' as date) - cast('2023-01-01' as date))", result);
    }

    @Test
    void shouldGenerateSqlForColumnReferences() {
      String startDate = "enrollment_date";
      String endDate = "incident_date";

      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      assertEquals("(cast(incident_date as date) - cast(enrollment_date as date))", result);
    }
  }

  @Nested
  class DorisTests {
    private D2DaysBetween function;
    private SqlBuilder sqlBuilder;

    @BeforeEach
    void setUp() {
      function = new D2DaysBetween();
      sqlBuilder = new DorisSqlBuilder("doris", "doris");
      when(visitor.getSqlBuilder()).thenReturn(sqlBuilder);
    }

    @Test
    void shouldGenerateSqlForLiteralDates() {
      String startDate = "'2023-01-01'";
      String endDate = "'2023-01-02'";

      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      assertEquals("DATEDIFF('2023-01-02', '2023-01-01')", result);
    }

    @Test
    void shouldGenerateSqlForColumnReferences() {
      String startDate = "enrollment_date";
      String endDate = "incident_date";

      String result = (String) function.getSqlBetweenDates(startDate, endDate, visitor);

      assertEquals("DATEDIFF(incident_date, enrollment_date)", result);
    }
  }
}
