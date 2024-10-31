/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.data.sql.from.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.data.sql.from.SubqueryColumnGenerator;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FirstLastValueTest {
  private FirstLastValue firstLastValue;
  private DataQueryParams params;
  private PostgreSqlBuilder sqlBuilder;
  private SubqueryColumnGenerator subqueryColumnGenerator;
  private Date startDate;
  private Date endDate;

  private static final String expectedColumns =
      "\"year\",\"pestartdate\",\"peenddate\",\"oulevel\",\"daysxvalue\",\"daysno\",\"value\",\"textvalue\",cast('202411' as text) as \"pe\"";
  private static final String expectedPartitionColumns = "ax.\"dx\",ax.\"ou\",ax.\"co\",ax.\"ao\"";

  @BeforeEach
  void setUp() {
    sqlBuilder = new PostgreSqlBuilder();
    startDate = new Date(); // Use current date as the start date
    endDate = new Date(); // End date (for testing purposes, keep it the same for simplicity)
  }

  @Test
  void testBuildSubqueryWithFirstPeriodAggregation() {
    // Create DataQueryParams with first period aggregation and adding a valid period with
    // PeriodType
    params =
        createParams(
            AnalyticsAggregationType.FIRST,
            false,
            "analytics",
            createPartitions(2022),
            startDate,
            endDate);
    subqueryColumnGenerator =
        new SubqueryColumnGenerator(params, sqlBuilder); // Correct constructor

    firstLastValue = new FirstLastValue(params, sqlBuilder, subqueryColumnGenerator, startDate);

    // Expected SQL query for 'FIRST' aggregation
    String expectedSubquery =
        String.format(
            """
        (select %s, row_number() over (partition by %s order by peenddate asc, pestartdate asc) as pe_rank
         from analytics_2022 as ax
         where ax."pestartdate" >= '%s' and
            ax."peenddate" <= '%s' and
            (ax."value" is not null or ax."textvalue" is not null))
        """,
            expectedColumns,
            expectedPartitionColumns,
            toMediumDate(startDate),
            toMediumDate(endDate));

    // Act: Build the actual subquery
    String subquery = firstLastValue.buildSubquery();

    // Assert: Ensure the generated query matches the expected SQL
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithLastPeriodAggregation() {
    // Arrange: LAST aggregation, single partition
    params =
        createParams(
            AnalyticsAggregationType.LAST,
            false,
            "analytics",
            createPartitions(2022),
            startDate,
            endDate);
    subqueryColumnGenerator =
        new SubqueryColumnGenerator(params, sqlBuilder); // Correct constructor with params

    firstLastValue = new FirstLastValue(params, sqlBuilder, subqueryColumnGenerator, startDate);

    // Adjusted expected SQL query for 'LAST' aggregation (using DESC order)
    String expectedSubquery =
        String.format(
            """
        (select %s, row_number() over (partition by %s order by peenddate desc, pestartdate desc) as pe_rank
         from analytics_2022 as ax
         where ax."pestartdate" >= '%s' and
            ax."peenddate" <= '%s' and
            (ax."value" is not null or ax."textvalue" is not null))
        """,
            expectedColumns,
            expectedPartitionColumns,
            toMediumDate(startDate),
            toMediumDate(endDate));

    // Act: Build the actual subquery
    String subquery = firstLastValue.buildSubquery();

    // Assert: Ensure the generated query matches the expected SQL
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithMultiplePartitions() {
    params =
        createParams(
            AnalyticsAggregationType.FIRST,
            false,
            "analytics",
            createPartitions(2021, 2022),
            startDate,
            endDate);
    subqueryColumnGenerator =
        new SubqueryColumnGenerator(
            params, sqlBuilder); // Proper constructor with params and PostgreSqlBuilder

    firstLastValue = new FirstLastValue(params, sqlBuilder, subqueryColumnGenerator, startDate);

    // Updated expected SQL query with UNION ALL for multiple partitions (2021 and 2022)
    String expectedSubquery =
        String.format(
            """
        (select %s, row_number() over (partition by %s order by peenddate asc, pestartdate asc) as pe_rank
         from (select ap.* from analytics_2021 as ap union all select ap.* from analytics_2022 as ap ) as ax
         where ax."pestartdate" >= '%s' and
            ax."peenddate" <= '%s' and
            (ax."value" is not null or ax."textvalue" is not null))
        """,
            expectedColumns,
            expectedPartitionColumns,
            toMediumDate(startDate),
            toMediumDate(endDate));

    // Act: Build the actual subquery
    String subquery = firstLastValue.buildSubquery();

    // Assert: Ensure the generated query matches the expected SQL
    assertEquals(expectedSubquery, subquery);
  }

  private DataQueryParams createParams(
      AnalyticsAggregationType aggregationType,
      boolean skipPartitioning,
      String tableName,
      Partitions partitions,
      Date startDate,
      Date endDate) {
    // Create a Period object with startDate, endDate, and an appropriate PeriodType (e.g.,
    // MonthlyPeriodType)
    Period period = new Period();
    period.setStartDate(startDate);
    period.setEndDate(endDate);
    PeriodType periodType = new MonthlyPeriodType(); // Assign a valid period type (e.g. monthly)
    period.setPeriodType(periodType);

    // Create the builder for DataQueryParams, include the period in settings
    return DataQueryParams.newBuilder()
        .withAggregationType(aggregationType)
        .withSkipPartitioning(skipPartitioning)
        .withTableName(tableName)
        .withPartitions(partitions)
        .withPeriods(List.of(period))
        .build();
  }

  private Partitions createPartitions(Integer... years) {
    Partitions partitions = new Partitions();
    if (years != null) {
      for (Integer year : years) {
        partitions.add(year);
      }
    }
    return partitions;
  }

  private String toMediumDate(Date date) {
    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(date);
  }
}
