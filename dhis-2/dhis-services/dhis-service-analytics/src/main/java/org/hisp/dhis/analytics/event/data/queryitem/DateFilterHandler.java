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
package org.hisp.dhis.analytics.event.data.queryitem;

import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.period.DateField;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.util.DateUtils;

/**
 * Handles filter application for date-type query items, specifically EVENT_DATE (occurreddate) and
 * SCHEDULED_DATE (scheduleddate).
 *
 * <p>Supports multiple filter formats:
 *
 * <ul>
 *   <li>Relative periods: THIS_MONTH, LAST_YEAR, etc.
 *   <li>Date ranges: 2025-01-01_2025-01-31
 *   <li>ISO periods: 2025Q1, 202501, 2025W01, etc.
 *   <li>Explicit operators: GT:2025-01-01, LE:2025-12-31
 *   <li>Multiple operator:value pairs: EVENT_DATE:GT:2025-01-01:LT:2025-12-31
 * </ul>
 */
public class DateFilterHandler implements QueryItemFilterHandler {

  /** Pattern for date range format: YYYY-MM-DD_YYYY-MM-DD (e.g., 2025-01-01_2025-01-31) */
  private static final Pattern DATE_RANGE_PATTERN =
      Pattern.compile("\\d{4}-\\d{2}-\\d{2}_\\d{4}-\\d{2}-\\d{2}");

  @Override
  public boolean supports(QueryItem queryItem) {
    String itemId = queryItem.getItemId();
    return (EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME.equals(itemId)
            || EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME.equals(itemId))
        && queryItem.getValueType() == ValueType.DATE;
  }

  @Override
  public void applyFilters(
      QueryItem queryItem, String[] filterParts, String dimensionString, Date relativePeriodDate) {
    if (filterParts.length == 2) {
      // Single filter value: EVENT_DATE:THIS_MONTH or EVENT_DATE:2025Q1
      parseAndAddDateFilters(queryItem, filterParts[1], relativePeriodDate);
    } else if (filterParts.length > 2) {
      // Multiple operator:value filters: EVENT_DATE:GT:2025-01-01:LT:2025-12-31
      addGenericFilters(queryItem, filterParts);
    } else {
      throwIllegalQueryEx(ErrorCode.E7222, dimensionString);
    }
  }

  private void parseAndAddDateFilters(
      QueryItem queryItem, String filterString, Date relativePeriodDate) {
    // Use relativePeriodDate if provided, otherwise fall back to current date
    Date referenceDate = relativePeriodDate != null ? relativePeriodDate : new Date();

    // Handle relative periods (e.g., THIS_MONTH, LAST_YEAR)
    if (RelativePeriodEnum.contains(filterString)) {
      RelativePeriodEnum relativePeriodEnum = RelativePeriodEnum.valueOf(filterString);
      List<Period> periods =
          RelativePeriods.getRelativePeriodsFromEnum(
                  relativePeriodEnum,
                  DateField.withDefaults().withDate(referenceDate),
                  null,
                  false,
                  null)
              .stream()
              .map(org.hisp.dhis.period.PeriodDimension::getPeriod)
              .toList();

      if (!periods.isEmpty()) {
        Date startDate = periods.get(0).getStartDate();
        Date endDate = periods.get(periods.size() - 1).getEndDate();
        queryItem.addFilter(new QueryFilter(QueryOperator.GE, DateUtils.toMediumDate(startDate)));
        queryItem.addFilter(new QueryFilter(QueryOperator.LE, DateUtils.toMediumDate(endDate)));
      }
      return;
    }

    // Handle date ranges (e.g., 2025-01-01_2025-01-31)
    if (DATE_RANGE_PATTERN.matcher(filterString).matches()) {
      String[] dates = filterString.split("_");
      if (dates.length == 2) {
        queryItem.addFilter(new QueryFilter(QueryOperator.GE, dates[0]));
        queryItem.addFilter(new QueryFilter(QueryOperator.LE, dates[1]));
      }
      return;
    }

    // Handle ISO periods using Period.of() - supports ALL period types
    // (yearly, quarterly, monthly, weekly, daily, six-monthly, financial, etc.)
    try {
      Period period = Period.of(filterString);
      if (period != null) {
        queryItem.addFilter(
            new QueryFilter(QueryOperator.GE, DateUtils.toMediumDate(period.getStartDate())));
        queryItem.addFilter(
            new QueryFilter(QueryOperator.LE, DateUtils.toMediumDate(period.getEndDate())));
        return;
      }
    } catch (IllegalArgumentException e) {
      // Not a valid ISO period, continue to next check
    }

    // Fallback to explicit operator:value parsing (e.g., GT:2025-01-01)
    String[] parts = filterString.split(":");
    if (parts.length == 2) {
      QueryOperator operator = QueryOperator.fromString(parts[0]);
      QueryFilter filter = new QueryFilter(operator, parts[1]);
      queryItem.addFilter(filter);
      return;
    }

    throwIllegalQueryEx(ErrorCode.E7222, filterString);
  }

  private void addGenericFilters(QueryItem queryItem, String[] filterParts) {
    for (int i = 1; i < filterParts.length; i += 2) {
      QueryOperator operator = QueryOperator.fromString(filterParts[i]);
      QueryFilter filter = new QueryFilter(operator, filterParts[i + 1]);
      queryItem.addFilter(filter);
    }
  }
}
