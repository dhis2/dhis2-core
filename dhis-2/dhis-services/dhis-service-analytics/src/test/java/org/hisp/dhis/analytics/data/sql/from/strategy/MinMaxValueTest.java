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
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.data.sql.WhereClauseBuilder;
import org.hisp.dhis.analytics.data.sql.from.SubqueryColumnGenerator;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinMaxValueTest {

  private MinMaxValue minMaxValue;
  private DataQueryParams params;
  private PostgreSqlBuilder sqlBuilder;
  private SubqueryColumnGenerator subqueryColumnGenerator;
  private WhereClauseBuilder whereClauseBuilder;
  private Date startDate;
  private Date endDate;

  @BeforeEach
  void setUp() {
    // Initialize PostgreSqlBuilder, and start/end dates
    sqlBuilder = new PostgreSqlBuilder();
    startDate = new Date(); // Use current date as the start date
    endDate = new Date(); // End date (for testing purposes, keep it the same for simplicity)
  }

  @Test
  void testBuildSubqueryWithSinglePartition() {
    // Arrange: Using a single partition (2022)
    params =
        createParams(
            false, "analytics", createPartitions(2022), startDate, endDate, AggregationType.MAX);
    subqueryColumnGenerator =
        new SubqueryColumnGenerator(params, sqlBuilder); // SubqueryColumnGenerator
    whereClauseBuilder = new WhereClauseBuilder(params, sqlBuilder, AnalyticsTableType.DATA_VALUE);

    minMaxValue =
        new MinMaxValue(params, sqlBuilder, subqueryColumnGenerator, AnalyticsTableType.DATA_VALUE);

    String dimensionColumns = "ax.\"ou\",\"pe\""; // Group by organization unit and period
    String valueColumns =
        "max(\"daysxvalue\") as \"daysxvalue\",max(\"daysno\") as \"daysno\",max(\"value\") as \"value\",max(\"textvalue\") as \"textvalue\""; // Use max() for columns like value, daysxvalue, etc.
    String whereClause =
        "where ax.\"pe\" in ('202411') and ax.\"year\" in (2022) "; // Filtering on relevant period
    // and year

    // Expected SQL after min/max aggregation and group by
    String expectedSubquery =
        String.format(
            """
        (select %s, %s from analytics_2022 as ax %s group by ax."ou","pe")
        """
                .trim(),
            dimensionColumns,
            valueColumns,
            whereClause);

    // Act: Build the actual subquery
    String subquery = minMaxValue.buildSubquery();

    // Assert: Ensure the generated query matches the expected SQL
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithMultiplePartitions() {
    // Arrange: Using multiple partitions (2021, 2022)
    params =
        createParams(
            false,
            "analytics",
            createPartitions(2021, 2022),
            startDate,
            endDate,
            AggregationType.MAX);
    subqueryColumnGenerator =
        new SubqueryColumnGenerator(params, sqlBuilder); // SubqueryColumnGenerator
    whereClauseBuilder = new WhereClauseBuilder(params, sqlBuilder, AnalyticsTableType.DATA_VALUE);

    minMaxValue =
        new MinMaxValue(params, sqlBuilder, subqueryColumnGenerator, AnalyticsTableType.DATA_VALUE);

    String dimensionColumns = "ax.\"ou\",\"pe\""; // Group by organization unit and period
    String valueColumns =
        "max(\"daysxvalue\") as \"daysxvalue\",max(\"daysno\") as \"daysno\",max(\"value\") as \"value\",max(\"textvalue\") as \"textvalue\""; // Max aggregation
    String whereClause =
        "where ax.\"pe\" in ('202411') and ax.\"year\" in (2021, 2022) "; // Filter on period and
    // years

    // Expected SQL using UNION ALL for multiple partitions
    String expectedSubquery =
        String.format(
            """
        (select %s, %s from (select ap.* from analytics_2021 as ap union all select ap.* from analytics_2022 as ap ) as ax %s group by ax."ou","pe")
        """
                .trim(),
            dimensionColumns,
            valueColumns,
            whereClause);

    // Act: Build the actual subquery
    String subquery = minMaxValue.buildSubquery();

    // Assert: Ensure the generated query matches the expected SQL
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithSinglePartitionUsingMin() {
    // Arrange: Using a single partition (2022) with MIN aggregation
    params =
        createParams(
            false, "analytics", createPartitions(2022), startDate, endDate, AggregationType.MIN);
    subqueryColumnGenerator = new SubqueryColumnGenerator(params, sqlBuilder);
    whereClauseBuilder = new WhereClauseBuilder(params, sqlBuilder, AnalyticsTableType.DATA_VALUE);

    minMaxValue =
        new MinMaxValue(params, sqlBuilder, subqueryColumnGenerator, AnalyticsTableType.DATA_VALUE);

    String dimensionColumns = "ax.\"ou\",\"pe\"";
    String valueColumns =
        "min(\"daysxvalue\") as \"daysxvalue\",min(\"daysno\") as \"daysno\",min(\"value\") as \"value\",min(\"textvalue\") as \"textvalue\""; // MIN aggregation
    String whereClause =
        "where ax.\"pe\" in ('202411') and ax.\"year\" in (2022) "; // Filter on period and year

    // Expected SQL after min aggregation and group by
    String expectedSubquery =
        String.format(
            """
    (select %s, %s from analytics_2022 as ax %s group by ax."ou","pe")
    """
                .trim(),
            dimensionColumns,
            valueColumns,
            whereClause);

    // Act: Build the actual subquery with MIN aggregation
    String subquery = minMaxValue.buildSubquery();

    // Assert: Ensure the generated query matches the expected SQL (with MIN)
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithMultiplePartitionsUsingMin() {
    // Arrange: Using multiple partitions (2021, 2022) with MIN aggregation
    params =
        createParams(
            false,
            "analytics",
            createPartitions(2021, 2022),
            startDate,
            endDate,
            AggregationType.MIN);
    subqueryColumnGenerator = new SubqueryColumnGenerator(params, sqlBuilder);
    whereClauseBuilder = new WhereClauseBuilder(params, sqlBuilder, AnalyticsTableType.DATA_VALUE);

    minMaxValue =
        new MinMaxValue(params, sqlBuilder, subqueryColumnGenerator, AnalyticsTableType.DATA_VALUE);

    String dimensionColumns = "ax.\"ou\",\"pe\"";
    String valueColumns =
        "min(\"daysxvalue\") as \"daysxvalue\",min(\"daysno\") as \"daysno\",min(\"value\") as \"value\",min(\"textvalue\") as \"textvalue\""; // MIN aggregation
    String whereClause =
        "where ax.\"pe\" in ('202411') and ax.\"year\" in (2021, 2022) "; // Filter on periods and
    // years

    // Expected SQL using UNION ALL for multiple partitions with MIN aggregation
    String expectedSubquery =
        String.format(
            """
    (select %s, %s from (select ap.* from analytics_2021 as ap union all select ap.* from analytics_2022 as ap ) as ax %s group by ax."ou","pe")
    """
                .trim(),
            dimensionColumns,
            valueColumns,
            whereClause);

    // Act: Build the actual subquery with MIN aggregation
    String subquery = minMaxValue.buildSubquery();

    // Assert: Ensure the generated query matches the expected SQL (with MIN)
    assertEquals(expectedSubquery, subquery);
  }

  private DataQueryParams createParams(
      boolean skipPartitioning,
      String tableName,
      Partitions partitions,
      Date startDate,
      Date endDate,
      AggregationType aggregationType) {
    // Create a list of Periods with the relevant start and end date (in real life these need to be
    // correct for the query)
    Period period = new Period();
    period.setStartDate(startDate);
    period.setEndDate(endDate);
    PeriodType periodType =
        new MonthlyPeriodType(); // Use MonthlyPeriodType or other PeriodType depending on your data
    period.setPeriodType(periodType);

    return DataQueryParams.newBuilder()
        .withAggregationType(
            AnalyticsAggregationType.fromAggregationType(aggregationType)) // Use MAX or MIN
        .withSkipPartitioning(skipPartitioning)
        .withTableName(tableName)
        .withPartitions(partitions)
        .withPeriods(
            List.of(period)) // Provide a list of periods to ensure correct date-based filtering
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
}
