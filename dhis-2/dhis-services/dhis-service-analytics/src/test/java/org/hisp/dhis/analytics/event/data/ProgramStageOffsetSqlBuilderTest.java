/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;

/**
 * Pins the SQL fragments produced by {@link ProgramStageOffsetSqlBuilder} so the extraction from
 * {@code JdbcEnrollmentAnalyticsManager} preserves byte-identical output for the repeated-event
 * subquery offset, ordering, and execution-date filter clauses.
 */
class ProgramStageOffsetSqlBuilderTest {

  private final ProgramStageOffsetSqlBuilder builder =
      new ProgramStageOffsetSqlBuilder(new PostgreSqlAnalyticsSqlBuilder());

  @Test
  void offsetClauseZeroReturnsEmpty() {
    assertEquals("", builder.offsetClause(0));
  }

  @Test
  void offsetClauseNegativeReturnsAbsoluteValue() {
    assertEquals("offset 3", builder.offsetClause(-3));
  }

  @Test
  void offsetClausePositiveReturnsValueMinusOne() {
    assertEquals("offset 1", builder.offsetClause(2));
  }

  @Test
  void orderTypeZeroIsDescending() {
    assertEquals("order by occurreddate desc, created desc", builder.orderType(0));
  }

  @Test
  void orderTypeNegativeIsDescending() {
    assertEquals("order by occurreddate desc, created desc", builder.orderType(-1));
  }

  @Test
  void orderTypePositiveIsAscending() {
    assertEquals("order by occurreddate asc, created asc", builder.orderType(2));
  }

  @Test
  void executionDateFilterWithBothBounds() {
    Date start = DateUtils.parseDate("2024-01-15");
    Date end = DateUtils.parseDate("2024-12-31");

    assertEquals(
        " and occurreddate >= '2024-01-15'  and occurreddate <= '2024-12-31' ",
        builder.executionDateFilter(start, end));
  }

  @Test
  void executionDateFilterWithStartOnly() {
    Date start = DateUtils.parseDate("2024-01-15");

    assertEquals(" and occurreddate >= '2024-01-15' ", builder.executionDateFilter(start, null));
  }

  @Test
  void executionDateFilterWithEndOnly() {
    Date end = DateUtils.parseDate("2024-12-31");

    assertEquals(" and occurreddate <= '2024-12-31' ", builder.executionDateFilter(null, end));
  }

  @Test
  void executionDateFilterWithNoBoundsReturnsEmpty() {
    assertEquals("", builder.executionDateFilter(null, null));
  }
}
