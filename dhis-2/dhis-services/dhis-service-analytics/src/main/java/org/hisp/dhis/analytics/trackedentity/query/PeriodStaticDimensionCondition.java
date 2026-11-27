/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.DATE;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.common.query.AndCondition;
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.ConstantValuesRenderer;
import org.hisp.dhis.analytics.common.query.OrCondition;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.period.DateField;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;

/**
 * Generates SQL conditions for period-based static dimensions. Handles relative periods, ISO
 * periods, date ranges, and operator-based filters.
 */
@RequiredArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class PeriodStaticDimensionCondition extends BaseRenderable {

  private static final Pattern DATE_RANGE_PATTERN =
      Pattern.compile("\\d{4}-\\d{2}-\\d{2}_\\d{4}-\\d{2}-\\d{2}");

  private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

  private final QueryContext queryContext;

  private final ScopedColumnResolver columnResolver;

  public static PeriodStaticDimensionCondition of(
      DimensionIdentifier<DimensionParam> dimensionIdentifier, QueryContext queryContext) {
    return of(dimensionIdentifier, queryContext, ScopedColumnResolver.period(dimensionIdentifier));
  }

  public static PeriodStaticDimensionCondition of(
      DimensionIdentifier<DimensionParam> dimensionIdentifier,
      QueryContext queryContext,
      ScopedColumnResolver columnResolver) {
    return new PeriodStaticDimensionCondition(dimensionIdentifier, queryContext, columnResolver);
  }

  @Override
  public String render() {
    List<Renderable> conditions = new ArrayList<>();
    String columnName = dimensionIdentifier.getDimension().getStaticDimension().getColumnName();

    for (DimensionParamItem item : dimensionIdentifier.getDimension().getItems()) {
      List<Renderable> itemConditions = processItem(item, columnName);
      conditions.addAll(itemConditions);
    }

    return AndCondition.of(conditions).render();
  }

  /**
   * Returns the conditions of the given item. An item compared with an operator is a single
   * condition over all its values. Otherwise each value selects a period, a date range or a date of
   * its own, and a row matches when it falls in any of them.
   */
  private List<Renderable> processItem(DimensionParamItem item, String columnName) {
    List<String> values = item.getValues();

    if (values.isEmpty()) {
      return List.of();
    }

    if (isOperatorFormat(item)) {
      return List.of(createOperatorCondition(item, values, columnName));
    }

    List<AndCondition> valueConditions =
        values.stream()
            .map(value -> AndCondition.of(processValue(item, value, columnName)))
            .toList();

    return List.of(OrCondition.ofList(valueConditions));
  }

  private List<Renderable> processValue(DimensionParamItem item, String value, String columnName) {
    if (isDateRange(value)) {
      return createDateRangeConditions(value, columnName);
    }

    List<PeriodDimension> periods = periodsOf(value);

    if (periods.isEmpty() && !RelativePeriodEnum.contains(value)) {
      return List.of(createOperatorCondition(item, List.of(value), columnName));
    }

    return createPeriodConditions(periods, columnName);
  }

  private boolean isOperatorFormat(DimensionParamItem item) {
    return item.getOperator().getQueryOperator() != QueryOperator.EQ
        || (item.getValues().size() == 1 && isNullValue(item.getValues().get(0)));
  }

  private boolean isNullValue(String value) {
    return NV.equalsIgnoreCase(value);
  }

  private boolean isDateRange(String value) {
    return DATE_RANGE_PATTERN.matcher(value).matches();
  }

  private Renderable createOperatorCondition(
      DimensionParamItem item, List<String> values, String columnName) {
    return BinaryConditionRenderer.of(
        columnResolver.resolve(columnName), item.getOperator(), values, DATE, queryContext);
  }

  private List<Renderable> createDateRangeConditions(String value, String columnName) {
    String[] dates = value.split("_");
    List<Renderable> conditions = new ArrayList<>();

    conditions.add(
        BinaryConditionRenderer.of(
            columnResolver.resolve(columnName),
            QueryOperator.GE,
            ConstantValuesRenderer.of(dates[0], DATE, queryContext)));

    conditions.add(
        BinaryConditionRenderer.of(
            columnResolver.resolve(columnName),
            QueryOperator.LT,
            ConstantValuesRenderer.of(nextDay(dates[1]), DATE, queryContext)));

    return conditions;
  }

  /**
   * Returns the periods the dimension's items select. A relative period contributes every period it
   * spans and an ISO period contributes itself. The list is empty when an item is a comparison, a
   * date range or any other value that is not a period.
   *
   * @param dimension the date dimension identifier.
   * @return the distinct periods, in request order.
   */
  public static List<PeriodDimension> periods(DimensionIdentifier<DimensionParam> dimension) {
    Map<String, PeriodDimension> periods = new LinkedHashMap<>();

    for (DimensionParamItem item : dimension.getDimension().getItems()) {
      if (item.getOperator().getQueryOperator() != QueryOperator.EQ) {
        return List.of();
      }
      for (String value : item.getValues()) {
        List<PeriodDimension> valuePeriods = periodsOf(value);
        if (valuePeriods.isEmpty()) {
          return List.of();
        }
        valuePeriods.forEach(period -> periods.putIfAbsent(period.getIsoDate(), period));
      }
    }

    return List.copyOf(periods.values());
  }

  /**
   * Returns the periods the given value stands for: the periods a relative period name spans, the
   * ISO period itself, or none for any other value.
   */
  private static List<PeriodDimension> periodsOf(String value) {
    if (RelativePeriodEnum.contains(value)) {
      return relativePeriods(value);
    }

    PeriodDimension period = PeriodDimension.of(value);
    return period == null ? List.of() : List.of(period);
  }

  /**
   * Returns the periods a relative period name spans, relative to the current date.
   *
   * @param value a {@link RelativePeriodEnum} name.
   * @return the periods, in chronological order.
   */
  private static List<PeriodDimension> relativePeriods(String value) {
    return RelativePeriods.getRelativePeriodsFromEnum(
        RelativePeriodEnum.valueOf(value), DateField.withDefaults(), null, false, null, null);
  }

  /**
   * Returns the conditions matching a date from the start of the first given period to the end of
   * the last one, or none when there is no period.
   */
  private List<Renderable> createPeriodConditions(
      List<PeriodDimension> periods, String columnName) {
    if (periods.isEmpty()) {
      return List.of();
    }

    Date startDate = periods.get(0).getStartDate();
    Date endDate = periods.get(periods.size() - 1).getEndDate();

    return createDateConditions(startDate, endDate, columnName);
  }

  private List<Renderable> createDateConditions(Date startDate, Date endDate, String columnName) {
    List<Renderable> conditions = new ArrayList<>();

    conditions.add(
        BinaryConditionRenderer.of(
            columnResolver.resolve(columnName),
            QueryOperator.GE,
            ConstantValuesRenderer.of(toMediumDate(startDate), DATE, queryContext)));

    conditions.add(
        BinaryConditionRenderer.of(
            columnResolver.resolve(columnName),
            QueryOperator.LT,
            ConstantValuesRenderer.of(toMediumDate(nextDay(endDate)), DATE, queryContext)));

    return conditions;
  }

  private String nextDay(String date) {
    return toMediumDate(nextDay(org.hisp.dhis.util.DateUtils.parseDate(date)));
  }

  private Date nextDay(Date date) {
    return Date.from(date.toInstant().plus(1, DAYS));
  }
}
