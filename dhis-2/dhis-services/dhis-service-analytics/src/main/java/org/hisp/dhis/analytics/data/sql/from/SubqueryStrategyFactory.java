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
package org.hisp.dhis.analytics.data.sql.from;

import static org.hisp.dhis.analytics.data.sql.AnalyticsColumns.LAST_VALUE_YEARS_OFFSET;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.data.sql.from.strategy.DefaultStrategy;
import org.hisp.dhis.analytics.data.sql.from.strategy.FirstLastValueStrategy;
import org.hisp.dhis.analytics.data.sql.from.strategy.MinMaxValueStrategy;
import org.hisp.dhis.analytics.data.sql.from.strategy.PreMeasureCriteriaStrategy;
import org.hisp.dhis.analytics.data.sql.from.strategy.SubqueryStrategy;
import org.hisp.dhis.db.sql.SqlBuilder;

@RequiredArgsConstructor
public class SubqueryStrategyFactory {
  private final DataQueryParams params;
  private final SqlBuilder sqlBuilder;
  private final ColumnBuilder columnBuilder;
  private final AnalyticsTableType tableType;

  public SubqueryStrategy createStrategy() {
    AnalyticsAggregationType aggregationType = params.getAggregationType();

    if (isFirstOrLastPeriodAggregationType(aggregationType)) {
      return createFirstLastValueStrategyForFirstOrLastPeriod();
    }

    if (isLastInPeriodAggregationType(aggregationType)) {
      return createFirstLastValueStrategyForLastInPeriod();
    }

    if (isMinOrMaxInPeriodAggregationType(aggregationType)) {
      return createMinMaxValueStrategy();
    }

    if (hasPreAggregateMeasureCriteriaAndIsNumeric()) {
      return createPreMeasureCriteriaStrategy();
    }

    return createDefaultStrategy();
  }

  private boolean isFirstOrLastPeriodAggregationType(AnalyticsAggregationType aggregationType) {
    return aggregationType.isFirstOrLastPeriodAggregationType();
  }

  private boolean isLastInPeriodAggregationType(AnalyticsAggregationType aggregationType) {
    return aggregationType.isLastInPeriodAggregationType();
  }

  private boolean isMinOrMaxInPeriodAggregationType(AnalyticsAggregationType aggregationType) {
    return aggregationType.isMinOrMaxInPeriodAggregationType();
  }

  private boolean hasPreAggregateMeasureCriteriaAndIsNumeric() {
    return params.hasPreAggregateMeasureCriteria() && params.isDataType(DataType.NUMERIC);
  }

  private SubqueryStrategy createFirstLastValueStrategyForFirstOrLastPeriod() {
    Date earliest = DateUtils.addYears(params.getLatestEndDate(), LAST_VALUE_YEARS_OFFSET);
    return new FirstLastValueStrategy(params, sqlBuilder, columnBuilder, earliest);
  }

  private SubqueryStrategy createFirstLastValueStrategyForLastInPeriod() {
    return new FirstLastValueStrategy(
        params, sqlBuilder, columnBuilder, params.getEarliestStartDate());
  }

  private SubqueryStrategy createMinMaxValueStrategy() {
    return new MinMaxValueStrategy(params, sqlBuilder, columnBuilder, tableType);
  }

  private SubqueryStrategy createPreMeasureCriteriaStrategy() {
    return new PreMeasureCriteriaStrategy(params, sqlBuilder);
  }

  private SubqueryStrategy createDefaultStrategy() {
    return new DefaultStrategy(params, sqlBuilder);
  }
}
