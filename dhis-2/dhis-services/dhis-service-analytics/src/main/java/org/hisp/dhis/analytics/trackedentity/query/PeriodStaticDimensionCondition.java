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
import static org.hisp.dhis.analytics.common.ValueTypeMapping.DATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getPrefix;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.common.query.AndCondition;
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.ConstantValuesRenderer;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.period.DateField;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;

/**
 * Generates SQL conditions for period-based static dimensions. Handles relative periods, ISO
 * periods, date ranges, and operator-based filters.
 */
@RequiredArgsConstructor(staticName = "of")
public class PeriodStaticDimensionCondition extends BaseRenderable {

  private static final Pattern DATE_RANGE_PATTERN =
      Pattern.compile("\\d{4}-\\d{2}-\\d{2}_\\d{4}-\\d{2}-\\d{2}");

  private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

  private final QueryContext queryContext;

  @Override
  public String render() {
    List<Renderable> conditions = new ArrayList<>();
    String prefix = getPrefix(dimensionIdentifier);
    String columnName = dimensionIdentifier.getDimension().getStaticDimension().getColumnName();

    for (DimensionParamItem item : dimensionIdentifier.getDimension().getItems()) {
      List<Renderable> itemConditions = processItem(item, prefix, columnName);
      conditions.addAll(itemConditions);
    }

    return AndCondition.of(conditions).render();
  }

  private List<Renderable> processItem(DimensionParamItem item, String prefix, String columnName) {
    List<Renderable> conditions = new ArrayList<>();
    List<String> values = item.getValues();

    if (values.isEmpty()) {
      return conditions;
    }

    String value = values.get(0);

    if (isOperatorFormat(item)) {
      conditions.add(createOperatorCondition(item, prefix, columnName));
    } else if (isDateRange(value)) {
      conditions.addAll(createDateRangeConditions(value, prefix, columnName));
    } else if (isRelativePeriod(value)) {
      conditions.addAll(createRelativePeriodConditions(value, prefix, columnName));
    } else if (isIsoPeriod(value)) {
      conditions.addAll(createIsoPeriodConditions(value, prefix, columnName));
    } else {
      conditions.add(createOperatorCondition(item, prefix, columnName));
    }

    return conditions;
  }

  private boolean isOperatorFormat(DimensionParamItem item) {
    return item.getOperator().getQueryOperator() != QueryOperator.EQ
        || (item.getValues().size() == 1 && isNullValue(item.getValues().get(0)));
  }

  private boolean isNullValue(String value) {
    return "NV".equalsIgnoreCase(value);
  }

  private boolean isDateRange(String value) {
    return DATE_RANGE_PATTERN.matcher(value).matches();
  }

  private boolean isRelativePeriod(String value) {
    return RelativePeriodEnum.contains(value);
  }

  private boolean isIsoPeriod(String value) {
    try {
      return Period.of(value) != null;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private Renderable createOperatorCondition(
      DimensionParamItem item, String prefix, String columnName) {
    return BinaryConditionRenderer.of(
        Field.of(prefix, () -> columnName, EMPTY),
        item.getOperator(),
        item.getValues(),
        DATE,
        queryContext);
  }

  private List<Renderable> createDateRangeConditions(
      String value, String prefix, String columnName) {
    String[] dates = value.split("_");
    List<Renderable> conditions = new ArrayList<>();

    conditions.add(
        BinaryConditionRenderer.of(
            Field.of(prefix, () -> columnName, EMPTY),
            QueryOperator.GE,
            ConstantValuesRenderer.of(dates[0], DATE, queryContext)));

    conditions.add(
        BinaryConditionRenderer.of(
            Field.of(prefix, () -> columnName, EMPTY),
            QueryOperator.LT,
            ConstantValuesRenderer.of(nextDay(dates[1]), DATE, queryContext)));

    return conditions;
  }

  private List<Renderable> createRelativePeriodConditions(
      String value, String prefix, String columnName) {
    RelativePeriodEnum relativePeriodEnum = RelativePeriodEnum.valueOf(value);
    List<org.hisp.dhis.period.PeriodDimension> periods =
        RelativePeriods.getRelativePeriodsFromEnum(
            relativePeriodEnum, DateField.withDefaults(), null, false, null);

    if (periods.isEmpty()) {
      return List.of();
    }

    Date startDate = periods.get(0).getStartDate();
    Date endDate = periods.get(periods.size() - 1).getEndDate();

    return createDateConditions(startDate, endDate, prefix, columnName);
  }

  private List<Renderable> createIsoPeriodConditions(
      String value, String prefix, String columnName) {
    Period period = Period.of(value);
    return createDateConditions(period.getStartDate(), period.getEndDate(), prefix, columnName);
  }

  private List<Renderable> createDateConditions(
      Date startDate, Date endDate, String prefix, String columnName) {
    List<Renderable> conditions = new ArrayList<>();

    conditions.add(
        BinaryConditionRenderer.of(
            Field.of(prefix, () -> columnName, EMPTY),
            QueryOperator.GE,
            ConstantValuesRenderer.of(toMediumDate(startDate), DATE, queryContext)));

    conditions.add(
        BinaryConditionRenderer.of(
            Field.of(prefix, () -> columnName, EMPTY),
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
