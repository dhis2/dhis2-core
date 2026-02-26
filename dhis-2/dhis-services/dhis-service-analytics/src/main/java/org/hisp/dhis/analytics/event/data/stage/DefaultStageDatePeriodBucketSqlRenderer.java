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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.springframework.stereotype.Component;

/**
 * Default {@link StageDatePeriodBucketSqlRenderer} implementation.
 *
 * <p>For Postgres, this renderer uses a date period structure lookup. For Doris and ClickHouse, it
 * generates direct date expressions to avoid correlated subqueries in grouped projections.
 *
 * <p>Examples:
 *
 * <p>Postgres for {@code stageUid.EVENT_DATE:2021} with yearly bucket:
 *
 * <pre>
 * (select "yearly"
 *  from analytics_rs_dateperiodstructure as dps_stage
 *  where dps_stage."dateperiod" = cast(ax."occurreddate" as date))
 * </pre>
 *
 * <p>Doris/CH for the same stage date yearly bucket:
 *
 * <pre>
 * date_format(cast(ax."occurreddate" as date), '%Y')
 * </pre>
 *
 * <p>Doris monthly example:
 *
 * <pre>
 * date_format(cast(ax."occurreddate" as date), '%Y%m')
 * </pre>
 */
@Component
public class DefaultStageDatePeriodBucketSqlRenderer implements StageDatePeriodBucketSqlRenderer {
  private final AnalyticsSqlBuilder sqlBuilder;

  /**
   * Creates a renderer using the active analytics SQL builder.
   *
   * @param sqlBuilder SQL builder used for quoting and database detection
   */
  public DefaultStageDatePeriodBucketSqlRenderer(AnalyticsSqlBuilder sqlBuilder) {
    this.sqlBuilder = sqlBuilder;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<String> resolvePeriodBucketColumn(QueryItem item) {
    if (item == null || item.getDimensionValues().isEmpty()) {
      return Optional.empty();
    }

    List<PeriodType> periodTypes = new ArrayList<>();

    for (String periodId : item.getDimensionValues()) {
      PeriodDimension periodDimension = PeriodDimension.of(periodId);
      if (periodDimension == null || periodDimension.getPeriodType() == null) {
        return Optional.empty();
      }
      periodTypes.add(periodDimension.getPeriodType());
    }

    PeriodType firstType = periodTypes.get(0);
    if (periodTypes.stream().anyMatch(type -> !type.equals(firstType))) {
      return Optional.empty();
    }

    return Optional.of(firstType.getName().toLowerCase());
  }

  /** {@inheritDoc} */
  @Override
  public String renderPeriodBucketExpression(QueryItem item, String periodBucketColumn) {
    String stageDateColumn = sqlBuilder.quoteAx(item.getItemId());

    Optional<String> dbExpression =
        sqlBuilder.renderStageDatePeriodBucket(stageDateColumn, periodBucketColumn);
    if (dbExpression.isPresent()) {
      return dbExpression.get();
    }

    String periodColumnName = sqlBuilder.quote(periodBucketColumn);
    String datePeriodColumn = sqlBuilder.quote("dateperiod");

    return "(select "
        + periodColumnName
        + " from analytics_rs_dateperiodstructure as dps_stage where dps_stage."
        + datePeriodColumn
        + " = cast("
        + stageDateColumn
        + " as date))";
  }
}
