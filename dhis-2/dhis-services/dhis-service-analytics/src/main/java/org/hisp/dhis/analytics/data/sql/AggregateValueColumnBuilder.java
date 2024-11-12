/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.data.sql;

import static org.hisp.dhis.analytics.AggregationType.COUNT;
import static org.hisp.dhis.analytics.AggregationType.MAX;
import static org.hisp.dhis.analytics.AggregationType.MIN;
import static org.hisp.dhis.analytics.AggregationType.STDDEV;
import static org.hisp.dhis.analytics.AggregationType.VARIANCE;
import static org.hisp.dhis.analytics.data.sql.SqlAggregationTemplate.AVERAGE_PERIOD_SUM;
import static org.hisp.dhis.analytics.data.sql.SqlAggregationTemplate.BOOLEAN_AVERAGE;
import static org.hisp.dhis.analytics.data.sql.SqlAggregationTemplate.DEFAULT_SUM;
import static org.hisp.dhis.analytics.data.sql.SqlAggregationTemplate.NUMERIC_AVERAGE;
import static org.hisp.dhis.analytics.data.sql.SqlAggregationTemplate.SIMPLE_AGGREGATION;

import java.util.EnumSet;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;

public class AggregateValueColumnBuilder {
  private final DataQueryParams params;
  private final AnalyticsAggregationType aggType;
  private final String valueColumn;

  private static final EnumSet<AggregationType> SIMPLE_AGGREGATION_TYPES =
      EnumSet.of(COUNT, STDDEV, VARIANCE, MIN, MAX);

  public AggregateValueColumnBuilder(DataQueryParams params) {
    this.params = params;
    this.aggType = params.getAggregationType();
    this.valueColumn = params.getValueColumn();
  }

  public String build() {
    if (isAveragePeriodSum()) {
      return buildAveragePeriodSum();
    }
    if (isNumericAverage()) {
      return buildNumericAverage();
    }
    if (isBooleanAverage()) {
      return buildBooleanAverage();
    }
    if (isSimpleAggregation()) {
      return buildSimpleAggregation();
    }
    return buildDefaultSum();
  }

  private boolean isAveragePeriodSum() {
    return aggType.isAggregationType(AggregationType.SUM)
        && aggType.isPeriodAggregationType(AggregationType.AVERAGE)
        && aggType.isNumericDataType();
  }

  private boolean isNumericAverage() {
    return aggType.isAggregationType(AggregationType.AVERAGE) && aggType.isNumericDataType();
  }

  private boolean isBooleanAverage() {
    return aggType.isAggregationType(AggregationType.AVERAGE) && aggType.isBooleanDataType();
  }

  private boolean isSimpleAggregation() {
    return SIMPLE_AGGREGATION_TYPES.contains(aggType.getAggregationType());
  }

  private String buildAveragePeriodSum() {
    return AVERAGE_PERIOD_SUM.format(params.getDaysForAvgSumIntAggregation());
  }

  private String buildNumericAverage() {
    return NUMERIC_AVERAGE.format(valueColumn);
  }

  private String buildBooleanAverage() {
    return BOOLEAN_AVERAGE.format();
  }

  private String buildSimpleAggregation() {
    return SIMPLE_AGGREGATION.format(aggType.getAggregationType().getValue(), valueColumn);
  }

  private String buildDefaultSum() {
    return DEFAULT_SUM.format(valueColumn);
  }
}
