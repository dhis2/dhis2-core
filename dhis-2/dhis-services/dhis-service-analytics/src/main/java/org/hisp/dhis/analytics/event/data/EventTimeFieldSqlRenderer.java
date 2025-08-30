/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.AnalyticsConstants.DATE_PERIOD_STRUCT_ALIAS;
import static org.hisp.dhis.analytics.EventOutputType.ENROLLMENT;
import static org.hisp.dhis.analytics.TimeField.ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.TimeField.EVENT_DATE;
import static org.hisp.dhis.analytics.TimeField.LAST_UPDATED;
import static org.hisp.dhis.analytics.TimeField.SCHEDULED_DATE;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.util.DateUtils.plusOneDay;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsType;
import org.springframework.stereotype.Component;

@Component
class EventTimeFieldSqlRenderer extends TimeFieldSqlRenderer {

  public EventTimeFieldSqlRenderer(SqlBuilder sqlBuilder) {
    super(sqlBuilder);
  }

  @Getter private final Set<TimeField> allowedTimeFields = Set.of(LAST_UPDATED, SCHEDULED_DATE);

  @Override
  protected String getAggregatedConditionForPeriods(EventQueryParams params) {
    List<DimensionalItemObject> periods = params.getAllDimensionOrFilterItems(PERIOD_DIM_ID);

    Optional<TimeField> timeField = getTimeField(params);
    StringBuilder sql = new StringBuilder();

    if (timeField.isPresent() && !timeField.get().supportsRawPeriod()) {
      return sql.append(
              periods.stream()
                  .filter(this::isPeriod)
                  .map(dimensionalItemObject -> (Period) dimensionalItemObject)
                  .map(period -> toSqlCondition(period, timeField.get()))
                  .collect(Collectors.joining(" or ", "(", ")")))
          .toString();
    }
    return sql.append(getSqlForAllPeriods(getPeriodAlias(params), periods)).toString();
  }

  @Override
  protected String getColumnName(Optional<TimeField> timeField, EventOutputType outputType) {
    return getTimeCol(timeField, outputType);
  }

  @Override
  protected String getConditionForNonDefaultBoundaries(EventQueryParams params) {
    return params.getProgramIndicator().getAnalyticsPeriodBoundaries().stream()
        .map(
            analyticsPeriodBoundary ->
                statementBuilder.getBoundaryCondition(
                    analyticsPeriodBoundary,
                    params.getProgramIndicator(),
                    params.getTimeFieldAsField(AnalyticsType.EVENT),
                    params.getEarliestStartDate(),
                    params.getLatestEndDate()))
        .collect(Collectors.joining(" and "));
  }

  private String getTimeCol(Optional<TimeField> timeField, EventOutputType outputType) {
    if (timeField.isPresent()) {
      return sqlBuilder.quoteAx(timeField.get().getEventColumnName());
    } else if (ENROLLMENT == outputType) {
      return sqlBuilder.quoteAx(ENROLLMENT_DATE.getEnrollmentColumnName());
    } else {
      // EVENTS
      return sqlBuilder.quoteAx(EVENT_DATE.getEventColumnName());
    }
  }

  private String toSqlCondition(Period period, TimeField timeField) {
    String timeCol = sqlBuilder.quoteAx(timeField.getEventColumnName());
    return "( "
        + timeCol
        + " >= '"
        + toMediumDate(period.getStartDate())
        + "' and "
        + timeCol
        + " < '"
        + toMediumDate(plusOneDay(period.getEndDate()))
        + "') ";
  }

  private String getPeriodAlias(EventQueryParams params) {
    return params.hasTimeField() ? DATE_PERIOD_STRUCT_ALIAS : ANALYTICS_TBL_ALIAS;
  }
}
