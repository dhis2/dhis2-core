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
package org.hisp.dhis.analytics.event.data.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.ClickHouseAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.DorisAnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.junit.jupiter.api.Test;

class StageDatePeriodBucketSqlRendererTest {
  @Test
  void shouldReturnEmptyForNullItem() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder());

    assertFalse(subject.resolvePeriodBucketColumn(null).isPresent());
  }

  @Test
  void shouldReturnEmptyForNoDimensionValues() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder());
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);

    assertFalse(subject.resolvePeriodBucketColumn(item).isPresent());
  }

  @Test
  void shouldReturnEmptyForInvalidPeriodId() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder());
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    item.setDimensionValues(List.of("NOT_A_PERIOD"));

    assertFalse(subject.resolvePeriodBucketColumn(item).isPresent());
  }

  @Test
  void shouldResolveMonthlyPeriodBucket() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder());
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    item.setDimensionValues(List.of("202401", "202402"));

    assertEquals("monthly", subject.resolvePeriodBucketColumn(item).orElseThrow());
  }

  @Test
  void shouldNotResolveMixedPeriodBuckets() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder());
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    item.setDimensionValues(List.of("202401", "2024Q1"));

    assertFalse(subject.resolvePeriodBucketColumn(item).isPresent());
  }

  @Test
  void shouldRenderPostgresCorrelatedSubquery() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(new PostgreSqlAnalyticsSqlBuilder());
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);

    String expression = subject.renderPeriodBucketExpression(item, "monthly");

    assertTrue(expression.contains("analytics_rs_dateperiodstructure"));
    assertTrue(expression.contains("cast(ax.\"occurreddate\" as date)"));
    assertTrue(expression.contains("\"monthly\""));
  }

  @Test
  void shouldRenderDorisExpressionFromStageDateColumn() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(
            new DorisAnalyticsSqlBuilder("internal", "doris-jdbc.jar"));
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);

    String expression = subject.renderPeriodBucketExpression(item, "yearly");

    assertEquals("date_format(cast(ax.`occurreddate` as date), '%Y')", expression);
  }

  @Test
  void shouldRenderClickHouseExpressionFromScheduledDateColumn() {
    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(new ClickHouseAnalyticsSqlBuilder("default"));
    QueryItem item = createItem(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);

    String expression = subject.renderPeriodBucketExpression(item, "monthly");

    assertEquals("formatDateTime(toDate(ax.\"scheduleddate\"), '%Y%m')", expression);
  }

  @Test
  void shouldUseBuilderSpecificExpressionWhenProvided() {
    AnalyticsSqlBuilder builder =
        new PostgreSqlAnalyticsSqlBuilder() {
          @Override
          public Optional<String> renderStageDatePeriodBucket(
              String stageDateColumn, String periodBucketColumn) {
            return Optional.of("custom_bucket_expr");
          }
        };

    DefaultStageDatePeriodBucketSqlRenderer subject =
        new DefaultStageDatePeriodBucketSqlRenderer(builder);
    QueryItem item = createItem(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);

    String expression = subject.renderPeriodBucketExpression(item, "monthly");

    assertEquals("custom_bucket_expr", expression);
  }

  private QueryItem createItem(String itemId) {
    DataElement dataElement = new DataElement();
    dataElement.setUid(itemId);
    return new QueryItem(dataElement);
  }
}
