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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.analytics.event.data.JdbcEventAnalyticsManager.OPEN_IN;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.util.DateUtils.toMediumDate;
import static org.hisp.dhis.util.TextUtils.getQuotedCommaDelimitedString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.parser.expression.statement.DefaultStatementBuilder;
import org.hisp.dhis.parser.expression.statement.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/** Provides methods targeting the generation of SQL statements for periods and time fields. */
public abstract class TimeFieldSqlRenderer {
  protected final SqlBuilder sqlBuilder;
  protected final StatementBuilder statementBuilder;

  protected TimeFieldSqlRenderer(SqlBuilder sqlBuilder) {
    this.sqlBuilder = sqlBuilder;
    this.statementBuilder = new DefaultStatementBuilder(sqlBuilder);
  }

  /**
   * Generates a SQL statement for periods or time field based on the given params.
   *
   * @param params the {@link EventQueryParams}
   * @return the SQL statement
   */
  public String renderPeriodTimeFieldSql(EventQueryParams params) {
    StringBuilder sql = new StringBuilder();

    if (params.hasNonDefaultBoundaries()) {
      sql.append(getConditionForNonDefaultBoundaries(params));
    } else if (params.useStartEndDates() || params.hasTimeDateRanges()) {
      sql.append(getDateRangeCondition(params));
    } else // Periods condition only for pivot table (aggregated).
    {
      sql.append(getAggregatedConditionForPeriods(params));
    }

    if (isEmpty(sql)) {
      return sql.toString();
    }

    return "(" + sql + ") ";
  }

  /**
   * Checks if the given time field is allowed in time/date queries.
   *
   * @param timeField the {@link TimeField}
   * @return true if the time field is allowed, false otherwise
   */
  private boolean isAllowed(TimeField timeField) {
    return getAllowedTimeFields().contains(timeField);
  }

  /**
   * Returns a SQL statement based on start/end dates and all time/date ranges defined, if any.
   *
   * @param params the {@link EventQueryParams}
   * @return the SQL statement
   */
  private String getDateRangeCondition(EventQueryParams params) {
    Map<String, List<String>> conditions = new HashMap<>();

    if (params.hasStartEndDate()) {
      addStartEndDateToCondition(params, conditions);
    }

    params
        .getTimeDateRanges()
        .forEach(
            (timeField, dateRanges) -> {
              if (params.hasContinuousRange(dateRanges)) {
                // Picks the start date of the first range and end date of the last range.
                DateRange dateRange =
                    new DateRange(
                        dateRanges.get(0).getStartDate(),
                        dateRanges.get(dateRanges.size() - 1).getEndDate());
                collectDateRangeSqlConditions(params, timeField, dateRange, conditions);
              } else {
                dateRanges.forEach(
                    dateRange ->
                        collectDateRangeSqlConditions(params, timeField, dateRange, conditions));
              }
            });

    // the same columns are in the "OR" relation
    // different columns are in the "AND" relation
    String dateRangeCondition =
        String.join(
            " and ",
            conditions.values().stream()
                .map(s -> "(" + String.join(" or ", s) + ")")
                .collect(Collectors.toSet()));

    return isEmpty(dateRangeCondition) ? EMPTY : dateRangeCondition;
  }

  /**
   * The method collects all conditions grouped by the date range column name (enrollmentdate,
   * incidentdate, etc..).
   *
   * @param eventQueryParams the {@link EventQueryParams}
   * @param timeField the {@link TimeField}
   * @param dateRange the {@link DateRange}
   * @param conditions the Map for conditions grouped by the date range column name
   */
  private void collectDateRangeSqlConditions(
      EventQueryParams eventQueryParams,
      TimeField timeField,
      DateRange dateRange,
      Map<String, List<String>> conditions) {
    ColumnWithDateRange columnWithDateRange =
        ColumnWithDateRange.of(
            getColumnName(Optional.of(timeField), eventQueryParams.getOutputType()), dateRange);
    List<String> sqlConditions = conditions.get(columnWithDateRange.column);
    if (sqlConditions == null) {
      sqlConditions = new ArrayList<>();
      sqlConditions.add(getDateRangeCondition(columnWithDateRange));
      conditions.put(columnWithDateRange.column, sqlConditions);
    } else {
      sqlConditions.add(getDateRangeCondition(columnWithDateRange));
    }
  }

  /**
   * Returns a string representing the SQL condition for the given {@link ColumnWithDateRange}.
   *
   * @param dateRangeColumn the {@link ColumnWithDateRange}
   * @return the SQL statement
   */
  private String getDateRangeCondition(ColumnWithDateRange dateRangeColumn) {
    return "("
        + dateRangeColumn.getColumn()
        + " >= '"
        + toMediumDate(dateRangeColumn.getDateRange().getStartDate())
        + "' and "
        + dateRangeColumn.getColumn()
        + " < '"
        + toMediumDate(dateRangeColumn.getDateRange().getEndDatePlusOneDay())
        + "')";
  }

  /**
   * Adds the default data range condition into the given list of conditions.
   *
   * @param params the {@link EventQueryParams}
   * @param conditions a Map of SQL conditions grouped by the date range column name
   */
  private void addStartEndDateToCondition(
      EventQueryParams params, Map<String, List<String>> conditions) {
    ColumnWithDateRange defaultDateRangeColumn =
        ColumnWithDateRange.builder()
            .column(getColumnName(getTimeField(params), params.getOutputType()))
            .dateRange(new DateRange(params.getStartDate(), params.getEndDate()))
            .build();
    List<String> dateRangeConditions = new ArrayList<>();
    dateRangeConditions.add(getDateRangeCondition(defaultDateRangeColumn));
    conditions.put(defaultDateRangeColumn.column, dateRangeConditions);
  }

  /**
   * Returns the time field {@link TimeField} set in the given params {@link EventQueryParams}. It
   * also checks if the time field set is allowed. Case negative, it returns an empty optional.
   *
   * @param params the {@link EventQueryParams}
   * @return and optional {@link TimeField}
   */
  protected Optional<TimeField> getTimeField(EventQueryParams params) {
    return TimeField.of(params.getTimeField()).filter(this::isAllowed);
  }

  protected boolean isPeriod(DimensionalItemObject dimensionalItemObject) {
    return dimensionalItemObject instanceof Period;
  }

  @Data
  @Builder
  private static class ColumnWithDateRange {
    private final String column;

    private final DateRange dateRange;

    static ColumnWithDateRange of(String column, DateRange dateRange) {
      return ColumnWithDateRange.builder().column(column).dateRange(dateRange).build();
    }
  }

  /**
   * It generates a period SQL statement for aggregated queries based on the given params.
   *
   * @param params {@link EventQueryParams}
   * @return the SQL statement
   */
  protected abstract String getAggregatedConditionForPeriods(EventQueryParams params);

  /**
   * Returns the field/column associated with the given {@link TimeField}. If the time field is
   * empty, it will return a default one. The {@link EventOutputType} might be used to decided which
   * field/column will be the default.
   *
   * @param timeField the optional {@link TimeField}
   * @param outputType the {@link EventOutputType}
   * @return the field/column of the given time field
   */
  protected abstract String getColumnName(
      Optional<TimeField> timeField, EventOutputType outputType);

  /**
   * Generates a SQL statement based on program indicators boundaries.
   *
   * @param params {@link EventQueryParams}
   * @return the SQL statement
   */
  protected abstract String getConditionForNonDefaultBoundaries(EventQueryParams params);

  /**
   * Simply returns a collection of {@link TimeField} allowed for the respective implementation.
   *
   * @return the collection of {@link TimeField}
   */
  protected abstract Set<TimeField> getAllowedTimeFields();

  /**
   * Returns a SQL statement for the given {@link DimensionalItemObject} list. {@link
   * DimensionalItemObject} are of type {@link Period}. This method renders SQL condition to match
   * the given periods, considering their different {@link PeriodType}.<br>
   * <br>
   * Example (different period types)<br>
   * <br>
   * {@code ('daily' in ('20200111', '20210211') or 'monthly' in ('202001', '202002'))}<br>
   * <br>
   * Example (same period type)<br>
   * <br>
   * {@code 'monthly' in ('202001', '202002')}
   *
   * @param alias
   * @param periods
   * @return
   */
  protected String getSqlForAllPeriods(String alias, List<DimensionalItemObject> periods) {
    StringBuilder sql = new StringBuilder();

    Map<PeriodType, List<Period>> periodsByType =
        periods.stream()
            .map(Period.class::cast)
            .collect(Collectors.groupingBy(Period::getPeriodType));

    List<String> periodSingleConditions =
        periodsByType.entrySet().stream().map(entry -> toSqlCondition(alias, entry)).toList();

    String periodsCondition = wrapAndJoinWithOrIfNecessary(periodSingleConditions);

    if (periodsByType.size() > 1) {
      sql.append(" (").append(periodsCondition).append(")");
    } else {
      sql.append(periodsCondition);
    }
    return sql.toString();
  }

  private String wrapAndJoinWithOrIfNecessary(Collection<String> periodSingleConditions) {
    if (periodSingleConditions.size() > 1) {
      return periodSingleConditions.stream().collect(Collectors.joining(" or ", "(", ")"));
    }
    return periodSingleConditions.stream().findFirst().orElse(EMPTY);
  }

  private String toSqlCondition(String alias, Entry<PeriodType, List<Period>> entry) {
    String columnName = entry.getKey().getName().toLowerCase();
    return sqlBuilder.quote(alias, columnName)
        + OPEN_IN
        + getQuotedCommaDelimitedString(getUids(entry.getValue()))
        + ") ";
  }
}
