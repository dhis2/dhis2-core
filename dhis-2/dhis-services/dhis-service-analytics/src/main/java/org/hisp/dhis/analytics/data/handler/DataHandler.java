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
package org.hisp.dhis.analytics.data.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.min;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.remove;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.COUNT;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.SUM;
import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS;
import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS_TARGET;
import static org.hisp.dhis.analytics.AnalyticsTableType.DATA_VALUE;
import static org.hisp.dhis.analytics.AnalyticsTableType.ORG_UNIT_TARGET;
import static org.hisp.dhis.analytics.AnalyticsTableType.VALIDATION_RESULT;
import static org.hisp.dhis.analytics.DataQueryParams.COMPLETENESS_DIMENSION_TYPES;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DX_INDEX;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_DENOMINATOR_PROPERTIES_COUNT;
import static org.hisp.dhis.analytics.DataQueryParams.getPermutationDimensionalItemValueMap;
import static org.hisp.dhis.analytics.DataQueryParams.getPermutationOrgUnitGroupCountMap;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.DimensionItem.asItemKey;
import static org.hisp.dhis.analytics.DimensionItem.getItemIdentifiers;
import static org.hisp.dhis.analytics.DimensionItem.getOrganisationUnitItem;
import static org.hisp.dhis.analytics.DimensionItem.getPeriodItem;
import static org.hisp.dhis.analytics.OutputFormat.ANALYTICS;
import static org.hisp.dhis.analytics.event.EventQueryParams.fromDataQueryParams;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.convertDxToOperand;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.findDimensionalItems;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getDoubleMap;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getRoundedValue;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getRoundedValueObject;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.hasPeriod;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.isPeriodInPeriods;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.withExceptionHandling;
import static org.hisp.dhis.analytics.util.PeriodOffsetUtils.buildYearToDateRows;
import static org.hisp.dhis.analytics.util.PeriodOffsetUtils.getPeriodOffsetRow;
import static org.hisp.dhis.analytics.util.PeriodOffsetUtils.isYearToDate;
import static org.hisp.dhis.analytics.util.ReportRatesHelper.getCalculatedTarget;
import static org.hisp.dhis.common.DataDimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DataDimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.common.DataDimensionItemType.EXPRESSION_DIMENSION_ITEM;
import static org.hisp.dhis.common.DataDimensionItemType.INDICATOR;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_ATTRIBUTE_OPTION;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_DATA_ELEMENT_OPTION;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DataDimensionItemType.VALIDATION_RULE;
import static org.hisp.dhis.common.DimensionConstants.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_GROUP_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionType.ATTRIBUTE_OPTION_COMBO;
import static org.hisp.dhis.common.DimensionType.CATEGORY_OPTION_COMBO;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT_GROUP;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.convertToDimItemValueMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.getAttributeOptionCombos;
import static org.hisp.dhis.common.DimensionalObjectUtils.getCategoryOptionCombos;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDataElements;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItem;
import static org.hisp.dhis.common.ReportingRateMetric.ACTUAL_REPORTS;
import static org.hisp.dhis.common.ReportingRateMetric.ACTUAL_REPORTS_ON_TIME;
import static org.hisp.dhis.common.ReportingRateMetric.EXPECTED_REPORTS;
import static org.hisp.dhis.common.ReportingRateMetric.REPORTING_RATE_ON_TIME;
import static org.hisp.dhis.commons.util.DebugUtils.getStackTrace;
import static org.hisp.dhis.commons.util.SystemUtils.getCpuCores;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType.values;
import static org.hisp.dhis.period.PeriodType.getPeriodTypeFromIsoString;
import static org.hisp.dhis.system.grid.GridUtils.getGridIndexByDimensionItem;
import static org.hisp.dhis.system.util.MathUtils.getWithin;
import static org.hisp.dhis.system.util.MathUtils.isZero;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryParams.Builder;
import org.hisp.dhis.analytics.DimensionItem;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.EventAggregateService;
import org.hisp.dhis.analytics.resolver.ExpressionResolver;
import org.hisp.dhis.analytics.resolver.ExpressionResolvers;
import org.hisp.dhis.analytics.util.PeriodOffsetUtils;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemObjectValue;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperand.TotalType;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This component is responsible for handling and retrieving data based on the input provided to the
 * public methods. The main goal is to correctly populate the data into the Grid object.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataHandler {
  private static final int MAX_QUERIES = 8;

  private static final int PERCENT = 100;

  private final EventAggregateService eventAggregatedService;

  private final RawAnalyticsManager rawAnalyticsManager;

  private final ExpressionResolvers resolvers;

  private final ExpressionService expressionService;

  private final QueryPlanner queryPlanner;

  private final SystemSettingsProvider settingsProvider;

  private final AnalyticsManager analyticsManager;

  private final OrganisationUnitService organisationUnitService;

  private DataAggregator dataAggregator;

  private final ExecutionPlanStore executionPlanStore;

  /**
   * Adds performance metrics.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid} to add performance metrics to.
   */
  void addPerformanceMetrics(DataQueryParams params, Grid grid) {
    if (params.analyzeOnly()) {
      String key = params.getExplainOrderId();

      List<ExecutionPlan> plans = executionPlanStore.getExecutionPlans(key);

      grid.addPerformanceMetrics(plans);

      executionPlanStore.removeExecutionPlans(key);
    }
  }

  /**
   * Adds indicator values to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   */
  @Transactional(readOnly = true)
  public void addIndicatorValues(DataQueryParams params, Grid grid) {
    if (!params.getIndicators().isEmpty() && !params.isSkipData()) {
      DataQueryParams dataSourceParams =
          newBuilder(params).retainDataDimension(INDICATOR).withIncludeNumDen(false).build();

      List<Indicator> indicators = resolveIndicatorExpressions(dataSourceParams);

      addIndicatorValues(params, dataSourceParams, indicators, grid);
    }
  }

  /**
   * Adds expressions values to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   */
  @Transactional(readOnly = true)
  public void addExpressionDimensionItemValues(DataQueryParams params, Grid grid) {
    if (!params.getExpressionDimensionItems().isEmpty() && !params.isSkipData()) {
      DataQueryParams dataSourceParams =
          newBuilder(params)
              .retainDataDimension(EXPRESSION_DIMENSION_ITEM)
              .withIncludeNumDen(false)
              .build();

      List<ExpressionDimensionItem> expressionDimensionItems =
          resolveExpressionDimensionItemExpressions(dataSourceParams);

      List<Indicator> indicators = expressionDimensionItemsToIndicators(expressionDimensionItems);

      addIndicatorValues(params, dataSourceParams, indicators, grid);
    }
  }

  /**
   * Adds subexpressions values to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   */
  @Transactional(readOnly = true)
  public void addSubexpressionDimensionItemValues(DataQueryParams params, Grid grid) {
    if (params.hasSubexpressions() && !params.isSkipData()) {
      // Generate one query per Subexpression
      for (DimensionalItemObject subex : params.getSubexpressions()) {
        DataQueryParams dataSourceParams =
            newBuilder(params)
                .withDataDimensionItems(List.of(subex))
                .withIncludeNumDen(false)
                .build();

        Map<String, Object> aggregatedDataMap =
            getAggregatedDataValueMapObjectTyped(dataSourceParams);

        for (Map.Entry<String, Object> entry : aggregatedDataMap.entrySet()) {
          Object value = getRoundedValueObject(params, entry.getValue());

          grid.addRow().addValues(entry.getKey().split(DIMENSION_SEP)).addValue(value);
        }
      }
    }
  }

  /**
   * Transform expression dimension item object in the indicator object with some default values
   * missing in expression dimension item
   */
  private List<Indicator> expressionDimensionItemsToIndicators(
      List<ExpressionDimensionItem> expressionDimensionItems) {
    return expressionDimensionItems.stream().map(edi -> edi.toIndicator()).collect(toList());
  }

  private void addIndicatorValues(
      DataQueryParams dataQueryParams,
      DataQueryParams dataSourceParams,
      List<Indicator> indicators,
      Grid grid) {
    List<Period> filterPeriods = getFilterPeriods(dataQueryParams, dataSourceParams);
    boolean periodsInFilter = dataQueryParams.getPeriods().isEmpty() && !filterPeriods.isEmpty();

    Map<String, Map<String, Integer>> permutationOrgUnitTargetMap =
        getOrgUnitTargetMap(dataSourceParams, indicators);

    List<List<DimensionItem>> dimensionItemPermutations =
        dataSourceParams.getDimensionItemPermutations();

    Map<DimensionalItemId, DimensionalItemObject> itemMap =
        expressionService.getIndicatorDimensionalItemMap(indicators);

    DataQueryParams paramsForValueMap =
        getParamsForValueMap(dataQueryParams, itemMap, periodsInFilter, indicators);

    Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap =
        getPermutationDimensionItemValueMap(paramsForValueMap, new ArrayList<>(itemMap.values()));

    handleEmptyDimensionItemPermutations(dimensionItemPermutations);

    for (Indicator indicator : indicators) {
      addIndicatorValueToGrid(
          indicator,
          dataQueryParams,
          dataSourceParams,
          grid,
          filterPeriods,
          periodsInFilter,
          itemMap,
          permutationOrgUnitTargetMap,
          permutationDimensionItemValueMap,
          dimensionItemPermutations,
          paramsForValueMap);
    }
  }

  /** Adds indicator values to the grid for a single indicator. */
  private void addIndicatorValueToGrid(
      Indicator indicator,
      DataQueryParams dataQueryParams,
      DataQueryParams dataSourceParams,
      Grid grid,
      List<Period> filterPeriods,
      boolean periodsInFilter,
      Map<DimensionalItemId, DimensionalItemObject> itemMap,
      Map<String, Map<String, Integer>> permutationOrgUnitTargetMap,
      Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap,
      List<List<DimensionItem>> dimensionItemPermutations,
      DataQueryParams paramsForValueMap) {

    // Get dimensional items specific to this indicator only
    Map<DimensionalItemId, DimensionalItemObject> indicatorItemMap =
        expressionService.getIndicatorDimensionalItemMap(List.of(indicator));

    // Check periodOffset only for this specific indicator's items
    boolean indicatorHasPeriodOffset = hasPeriodOffsetInItems(indicatorItemMap.values());

    if (periodsInFilter && indicatorHasPeriodOffset) {
      addAggregatedIndicatorValueWithPeriodOffset(
          indicator,
          dataQueryParams,
          dataSourceParams,
          grid,
          filterPeriods,
          indicatorItemMap,
          permutationOrgUnitTargetMap,
          permutationDimensionItemValueMap,
          paramsForValueMap);
    } else {
      addStandardIndicatorValues(
          indicator,
          dataQueryParams,
          dataSourceParams,
          grid,
          filterPeriods,
          itemMap,
          permutationOrgUnitTargetMap,
          permutationDimensionItemValueMap,
          dimensionItemPermutations);
    }
  }

  /**
   * Handles the special case where periods are in filters and the indicator has periodOffset.
   * Calculates indicator for each period separately, then aggregates the results. Preserves
   * dimension grouping by aggregating only across filter periods for each distinct combination of
   * other dimensions (e.g., org units, category combos).
   */
  private void addAggregatedIndicatorValueWithPeriodOffset(
      Indicator indicator,
      DataQueryParams dataQueryParams,
      DataQueryParams dataSourceParams,
      Grid grid,
      List<Period> filterPeriods,
      Map<DimensionalItemId, DimensionalItemObject> itemMap,
      Map<String, Map<String, Integer>> permutationOrgUnitTargetMap,
      Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap,
      DataQueryParams paramsForValueMap) {

    List<List<DimensionItem>> periodDimensionPermutations =
        paramsForValueMap.getDimensionItemPermutations();

    // Group dimension permutations by non-period dimensions (e.g., org units, categories)
    Map<String, List<List<DimensionItem>>> groupedByNonPeriodDimensions =
        groupDimensionPermutationsByNonPeriodDimensions(periodDimensionPermutations);

    // For each group (e.g., one org unit), aggregate indicator values across all periods
    for (Map.Entry<String, List<List<DimensionItem>>> entry :
        groupedByNonPeriodDimensions.entrySet()) {

      AggregationResult result =
          aggregateIndicatorAcrossPeriods(
              entry.getValue(),
              filterPeriods,
              itemMap,
              permutationOrgUnitTargetMap,
              permutationDimensionItemValueMap,
              indicator);

      if (result.hasData()) {
        addAggregatedResultToGrid(result, indicator, dataQueryParams, dataSourceParams, grid);
      }
    }
  }

  /**
   * Groups dimension permutations by their non-period dimensions. This allows us to aggregate
   * across periods while preserving the structure of other dimensions (org units, categories,
   * etc.).
   *
   * <p>Example: Input permutations: [(OrgUnit1, Period1), (OrgUnit1, Period2), (OrgUnit2, Period1)]
   * Output groups: {"OrgUnit1": [(OrgUnit1, Period1), (OrgUnit1, Period2)], "OrgUnit2": [(OrgUnit2,
   * Period1)]}
   *
   * @param periodDimensionPermutations all dimension permutations including periods
   * @return map where keys are non-period dimension identifiers and values are lists of
   *     permutations for that group
   */
  private Map<String, List<List<DimensionItem>>> groupDimensionPermutationsByNonPeriodDimensions(
      List<List<DimensionItem>> periodDimensionPermutations) {
    return periodDimensionPermutations.stream()
        .collect(
            Collectors.groupingBy(
                dimensionItems -> {
                  // Create a unique key from all non-period dimension items
                  // Example: "OrgUnit1-CategoryA" or "OrgUnit2-CategoryB"
                  return dimensionItems.stream()
                      .filter(item -> !PERIOD_DIM_ID.equals(item.getDimension()))
                      .map(item -> item.getItem().getDimensionItem())
                      .collect(Collectors.joining("-"));
                }));
  }

  /**
   * Aggregates indicator values across multiple periods for a single dimension group (e.g., one org
   * unit). Calculates the indicator separately for each period, then combines the results based on
   * the indicator's aggregation type.
   *
   * @param dimensionPermutations list of dimension permutations for this group (same org unit,
   *     different periods)
   * @param filterPeriods the periods to include in aggregation
   * @param itemMap dimensional items map for indicator expression evaluation
   * @param permutationOrgUnitTargetMap org unit target counts
   * @param permutationDimensionItemValueMap dimension item values
   * @param indicator the indicator to calculate
   * @return aggregation result containing the summed value, count, and dimension items
   */
  private AggregationResult aggregateIndicatorAcrossPeriods(
      List<List<DimensionItem>> dimensionPermutations,
      List<Period> filterPeriods,
      Map<DimensionalItemId, DimensionalItemObject> itemMap,
      Map<String, Map<String, Integer>> permutationOrgUnitTargetMap,
      Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap,
      Indicator indicator) {

    double sumValue = 0.0;
    int count = 0;
    List<DimensionItem> nonPeriodDimensionItems = null;

    // Calculate indicator for each period and accumulate results
    for (List<DimensionItem> dimensionItemsWithPeriod : dimensionPermutations) {

      // Extract and validate the period from this permutation
      PeriodDimension periodDim = extractValidPeriod(dimensionItemsWithPeriod, filterPeriods);
      if (periodDim == null) {
        continue; // Skip invalid or filtered-out periods
      }

      // Capture non-period dimensions from first valid permutation
      if (nonPeriodDimensionItems == null) {
        nonPeriodDimensionItems = extractNonPeriodDimensions(dimensionItemsWithPeriod);
      }

      // Calculate indicator value for this specific period
      IndicatorValue value =
          getIndicatorValue(
              List.of(periodDim.getPeriod()),
              itemMap,
              permutationOrgUnitTargetMap,
              permutationDimensionItemValueMap,
              indicator,
              dimensionItemsWithPeriod);

      // Accumulate non-null values
      if (value != null) {
        double valueDouble = value.getValue();
        sumValue += valueDouble;
        count++;
      }
    }

    return new AggregationResult(sumValue, count, nonPeriodDimensionItems);
  }

  /**
   * Extracts and validates a period from a list of dimension items. Returns null if the period is
   * invalid or not in the filter list.
   *
   * @param dimensionItems the dimension items that may contain a period
   * @param filterPeriods the list of valid filter periods
   * @return the validated PeriodDimension, or null if invalid
   */
  private PeriodDimension extractValidPeriod(
      List<DimensionItem> dimensionItems, List<Period> filterPeriods) {
    DimensionalItemObject periodItem = getPeriodItem(dimensionItems);

    // Type safety check to prevent ClassCastException
    if (!(periodItem instanceof PeriodDimension periodDim)) {
      return null;
    }

    // Check if this period is in our filter list
    if (!filterPeriods.contains(periodDim.getPeriod())) {
      return null;
    }

    return periodDim;
  }

  /**
   * Extracts all non-period dimension items from a list of dimension items. This includes org
   * units, categories, and any other dimensions except the period.
   *
   * @param dimensionItems the full list of dimension items
   * @return list containing only non-period dimension items
   */
  private List<DimensionItem> extractNonPeriodDimensions(List<DimensionItem> dimensionItems) {
    return dimensionItems.stream()
        .filter(item -> !PERIOD_DIM_ID.equals(item.getDimension()))
        .collect(Collectors.toList());
  }

  /**
   * Adds the aggregated indicator result to the grid. Calculates the final value based on the
   * indicator's aggregation type and creates a grid row.
   *
   * @param result the aggregation result containing sum, count, and dimensions
   * @param indicator the indicator being calculated
   * @param dataQueryParams the original query parameters
   * @param dataSourceParams the data source query parameters
   * @param grid the grid to add the result to
   */
  private void addAggregatedResultToGrid(
      AggregationResult result,
      Indicator indicator,
      DataQueryParams dataQueryParams,
      DataQueryParams dataSourceParams,
      Grid grid) {

    // Calculate final value based on aggregation type (SUM or AVERAGE)
    double finalValue = calculateAggregatedValue(result.sum, result.count, indicator);

    // Create indicator value object with the final aggregated value
    IndicatorValue aggregatedValue =
        new IndicatorValue()
            .setNumeratorValue(finalValue)
            .setDenominatorValue(1.0)
            .setMultiplier(1)
            .setDivisor(1);

    // Add row to grid with non-period dimensions and aggregated value
    addIndicatorValuesToGrid(
        dataQueryParams,
        grid,
        dataSourceParams,
        indicator,
        result.nonPeriodDimensions,
        aggregatedValue);
  }

  /**
   * Data holder for aggregation results across periods. Contains the accumulated sum, count of
   * non-null values, and the non-period dimension items for this aggregation group.
   *
   * @param sum the accumulated sum of indicator values across periods
   * @param count the number of non-null values included in the sum
   * @param nonPeriodDimensions the dimension items excluding period (e.g., org unit, categories)
   */
  private record AggregationResult(double sum, int count, List<DimensionItem> nonPeriodDimensions) {

    /** Returns true if this result contains valid data (at least one non-null value). */
    boolean hasData() {
      return count > 0;
    }
  }

  /**
   * Calculates the aggregated value based on the indicator's total aggregation type.
   *
   * @param sumValue the sum of all values
   * @param count the count of values
   * @param indicator the indicator with aggregation type
   * @return the aggregated value
   */
  private double calculateAggregatedValue(double sumValue, int count, Indicator indicator) {
    TotalAggregationType aggregationType = indicator.getTotalAggregationType();

    // Default to SUM when aggregation type is not specified
    if (aggregationType == null) {
      return sumValue;
    }

    return switch (aggregationType) {
      case SUM, NONE -> sumValue;
      case AVERAGE -> sumValue / count;
    };
  }

  /**
   * Handles standard indicator value calculation (periods in dimensions or in filters without
   * periodOffset).
   */
  private void addStandardIndicatorValues(
      Indicator indicator,
      DataQueryParams dataQueryParams,
      DataQueryParams dataSourceParams,
      Grid grid,
      List<Period> filterPeriods,
      Map<DimensionalItemId, DimensionalItemObject> itemMap,
      Map<String, Map<String, Integer>> permutationOrgUnitTargetMap,
      Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap,
      List<List<DimensionItem>> dimensionItemPermutations) {

    for (List<DimensionItem> dimensionItems : dimensionItemPermutations) {
      IndicatorValue value =
          getIndicatorValue(
              filterPeriods,
              itemMap,
              permutationOrgUnitTargetMap,
              permutationDimensionItemValueMap,
              indicator,
              dimensionItems);

      addIndicatorValuesToGrid(
          dataQueryParams, grid, dataSourceParams, indicator, dimensionItems, value);
    }
  }

  /** Gets filter periods from query parameters. */
  private List<Period> getFilterPeriods(
      DataQueryParams dataQueryParams, DataQueryParams dataSourceParams) {
    if (isNotEmpty(dataQueryParams.getTypedFilterPeriods())) {
      return dataQueryParams.getTypedFilterPeriods();
    }
    if (isNotEmpty(dataSourceParams.getTypedFilterPeriods())) {
      return dataSourceParams.getTypedFilterPeriods();
    }
    return dataSourceParams.getStartEndDatesToSingleList();
  }

  /** Creates params with periods moved to dimensions if needed for periodOffset handling. */
  private DataQueryParams getParamsForValueMap(
      DataQueryParams dataQueryParams,
      Map<DimensionalItemId, DimensionalItemObject> itemMap,
      boolean periodsInFilter,
      List<Indicator> indicators) {

    if (!periodsInFilter) {
      return dataQueryParams;
    }

    // Use the reusable hasPeriodOffsetInItems method
    if (!hasPeriodOffsetInItems(itemMap.values())) {
      return dataQueryParams;
    }

    return newBuilder(dataQueryParams)
        .withPeriods(dataQueryParams.getFilterPeriods())
        .removeFilter(PERIOD_DIM_ID)
        .build();
  }

  /** Checks if any items have periodOffset modifier. */
  private boolean hasPeriodOffsetInItems(Collection<DimensionalItemObject> items) {
    return items.stream()
        .anyMatch(
            item -> item.getQueryMods() != null && item.getQueryMods().getPeriodOffset() != 0);
  }

  /**
   * Based on the given indicator plus additional parameters, this method will find the respective
   * IndicatorValue.
   *
   * @param filterPeriods the filter periods. See {@link ConstantService#getConstantMap()}.
   * @param permutationOrgUnitTargetMap the org unit permutation map. See {@link
   *     #getOrgUnitTargetMap(DataQueryParams, Collection)}.
   * @param itemMap Every dimensional item to process.
   * @param permutationDimensionItemValueMap the dimension item permutation map. See {@link
   *     #getPermutationDimensionItemValueMap(DataQueryParams, List<DimensionalItemObject>)}.
   * @param indicator the input Indicator where the IndicatorValue will be based.
   * @param dimensionItems the dimensional items permutation map. See {@link
   *     DataQueryParams#getDimensionItemPermutations()}.
   * @return the IndicatorValue
   */
  private IndicatorValue getIndicatorValue(
      List<Period> filterPeriods,
      Map<DimensionalItemId, DimensionalItemObject> itemMap,
      Map<String, Map<String, Integer>> permutationOrgUnitTargetMap,
      Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap,
      Indicator indicator,
      List<DimensionItem> dimensionItems) {
    String permKey = asItemKey(dimensionItems);

    List<DimensionItemObjectValue> values =
        permutationDimensionItemValueMap.getOrDefault(permKey, new ArrayList<>());

    List<Period> periods =
        !filterPeriods.isEmpty()
            ? filterPeriods
            : List.of(((PeriodDimension) getPeriodItem(dimensionItems)).getPeriod());

    OrganisationUnit unit = (OrganisationUnit) getOrganisationUnitItem(dimensionItems);

    String ou = unit != null ? unit.getUid() : null;

    Map<String, Integer> orgUnitCountMap =
        permutationOrgUnitTargetMap != null ? permutationOrgUnitTargetMap.get(ou) : null;

    return expressionService.getIndicatorValueObject(
        indicator, periods, itemMap, convertToDimItemValueMap(values), orgUnitCountMap);
  }

  /**
   * Adds data element values to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid.
   */
  @Transactional(readOnly = true)
  public void addDataElementValues(DataQueryParams params, Grid grid) {
    if (!params.getAllDataElements().isEmpty() && (!params.isSkipData() || params.analyzeOnly())) {
      DataQueryParams dataSourceParams =
          newBuilder(params).retainDataDimension(DATA_ELEMENT).withIncludeNumDen(false).build();

      Map<String, Object> aggregatedDataMap =
          getAggregatedDataValueMapObjectTyped(dataSourceParams);

      for (Map.Entry<String, Object> entry : aggregatedDataMap.entrySet()) {
        Object value = getRoundedValueObject(params, entry.getValue());

        grid.addRow().addValues(entry.getKey().split(DIMENSION_SEP)).addValue(value);

        if (params.isIncludeNumDen()) {
          grid.addNullValues(NUMERATOR_DENOMINATOR_PROPERTIES_COUNT);
        }
      }
    }
  }

  /**
   * Adds program data element values to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid.
   */
  @Transactional(readOnly = true)
  public void addProgramDataElementAttributeIndicatorValues(DataQueryParams params, Grid grid) {
    if ((isNotEmpty(params.getAllProgramDataElementsAndAttributes())
            || isNotEmpty(params.getAllProgramDataElementsAndAttributesOptions())
            || isNotEmpty(params.getProgramIndicators()))
        && !params.isSkipData()) {
      DataQueryParams dataSourceParams =
          newBuilder(params)
              .retainDataDimensions(
                  PROGRAM_DATA_ELEMENT,
                  PROGRAM_DATA_ELEMENT_OPTION,
                  PROGRAM_ATTRIBUTE,
                  PROGRAM_ATTRIBUTE_OPTION,
                  PROGRAM_INDICATOR)
              .build();

      EventQueryParams eventQueryParams =
          new EventQueryParams.Builder(fromDataQueryParams(dataSourceParams))
              .withSkipMeta(true)
              .build();

      Grid eventGrid = eventAggregatedService.getAggregatedData(eventQueryParams);

      grid.addRows(eventGrid);

      replaceGridIfNeeded(grid, eventGrid);
    }
  }

  /**
   * This method will replace the headers in the current grid by the event grid IF, and only IF,
   * there is a mismatch between the current grid and the event grid headers.
   *
   * @param grid the current/actual grid
   * @param eventGrid the event grid
   */
  private void replaceGridIfNeeded(Grid grid, Grid eventGrid) {
    boolean eventGridHasAdditionalHeaders = grid.getHeaderWidth() < eventGrid.getHeaderWidth();
    boolean eventHeaderSizeIsSameAsGridColumns = eventGrid.getHeaderWidth() == eventGrid.getWidth();

    // Replacing the current grid headers by the actual event grid headers.
    if (eventGridHasAdditionalHeaders && eventHeaderSizeIsSameAsGridColumns) {
      grid.replaceHeaders(eventGrid.getHeaders());
    }
  }

  /**
   * Adds reporting rates to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid.
   */
  @Transactional(readOnly = true)
  public void addReportingRates(DataQueryParams params, Grid grid) {
    if (!params.getReportingRates().isEmpty() && !params.isSkipData()) {
      for (ReportingRateMetric metric : ReportingRateMetric.values()) {
        DataQueryParams dataSourceParams =
            newBuilder(params)
                .retainDataDimensionReportingRates(metric)
                .ignoreDataApproval() // No approval for reporting rates
                .withAggregationType(COUNT)
                .withTimely((REPORTING_RATE_ON_TIME == metric || ACTUAL_REPORTS_ON_TIME == metric))
                .build();

        addReportingRates(dataSourceParams, grid, metric);
      }
    }
  }

  /**
   * Adds data element operand values to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid.
   */
  @Transactional(readOnly = true)
  public void addDataElementOperandValues(DataQueryParams params, Grid grid) {
    if (!params.getAllDataElementOperands().isEmpty() && !params.isSkipData()) {
      DataQueryParams dataSourceParams =
          newBuilder(params).retainDataDimension(DATA_ELEMENT_OPERAND).build();

      for (TotalType type : values()) {
        addDataElementOperandValues(dataSourceParams, grid, type);
      }
    }
  }

  /**
   * Adds values to the given grid based on dynamic dimensions from the given data query parameters.
   * This assumes that no fixed dimensions are part of the query.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid.
   */
  @Transactional(readOnly = true)
  public void addDynamicDimensionValues(DataQueryParams params, Grid grid) {
    if (params.getDataDimensionAndFilterOptions().isEmpty() && !params.isSkipData()) {
      Map<String, Double> aggregatedDataMap =
          getAggregatedDataValueMap(newBuilder(params).withIncludeNumDen(false).build());

      fillGridWithAggregatedDataMap(params, grid, aggregatedDataMap);
    }
  }

  /**
   * Adds validation results to the given grid based on the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid.
   */
  @Transactional(readOnly = true)
  public void addValidationResultValues(DataQueryParams params, Grid grid) {
    if (!params.getAllValidationResults().isEmpty() && !params.isSkipData()) {
      DataQueryParams dataSourceParams =
          newBuilder(params)
              .retainDataDimension(VALIDATION_RULE)
              .withAggregationType(COUNT)
              .withIncludeNumDen(false)
              .build();

      Map<String, Double> aggregatedDataMap =
          getAggregatedValidationResultMapObjectTyped(dataSourceParams);

      fillGridWithAggregatedDataMap(params, grid, aggregatedDataMap);
    }
  }

  /**
   * Adds raw data to the grid for the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid.
   */
  @Transactional(readOnly = true)
  public void addRawData(DataQueryParams params, Grid grid) {
    if (!params.isSkipData()) {
      QueryPlannerParams plannerParams =
          QueryPlannerParams.newBuilder().withTableType(DATA_VALUE).build();

      params = queryPlanner.withTableNameAndPartitions(params, plannerParams);

      final DataQueryParams immutableParams = DataQueryParams.newBuilder(params).build();
      withExceptionHandling(() -> rawAnalyticsManager.getRawDataValues(immutableParams, grid));
    }
  }

  /**
   * Prepares the given data query parameters.
   *
   * @param params the {@link DataQueryParams}.
   */
  @Transactional(readOnly = true)
  public DataQueryParams prepareForRawDataQuery(DataQueryParams params) {
    DataQueryParams.Builder builder =
        newBuilder(params)
            .withEarliestStartDateLatestEndDate()
            .withPeriodDimensionWithoutOptions()
            .withIncludePeriodStartEndDates(true);

    if (params.isShowHierarchy()) {
      builder.withOrgUnitLevels(organisationUnitService.getFilledOrganisationUnitLevels());
    }

    return builder.build();
  }

  /**
   * Generates a mapping between the count of a validation result.
   *
   * @param params the {@link DataQueryParams}.
   * @return a mapping between validation results and counts of them
   */
  private Map<String, Double> getAggregatedValidationResultMapObjectTyped(DataQueryParams params) {
    return getDoubleMap(getAggregatedValueMap(params, VALIDATION_RESULT, newArrayList()));
  }

  /**
   * Fill grid with aggregated data map with key and value
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the grid
   * @param aggregatedDataMap the aggregated data map
   */
  private void fillGridWithAggregatedDataMap(
      DataQueryParams params, Grid grid, Map<String, Double> aggregatedDataMap) {
    for (Map.Entry<String, Double> entry : aggregatedDataMap.entrySet()) {
      Number value =
          params.isSkipRounding()
              ? entry.getValue()
              : (Number) getRoundedValueObject(params, entry.getValue());

      grid.addRow().addValues(entry.getKey().split(DIMENSION_SEP)).addValue(value);

      if (params.isIncludeNumDen()) {
        grid.addNullValues(NUMERATOR_DENOMINATOR_PROPERTIES_COUNT);
      }
    }
  }

  /**
   * Adds reporting rates to the given grid based on the given data query parameters and reporting
   * rate metric.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   * @param metric the reporting rate metric.
   */
  private void addReportingRates(DataQueryParams params, Grid grid, ReportingRateMetric metric) {
    if (!params.getReportingRates().isEmpty() && !params.isSkipData()) {
      if (!COMPLETENESS_DIMENSION_TYPES.containsAll(params.getDimensionTypes())) {
        return;
      }

      DataQueryParams targetParams =
          newBuilder(params)
              .withSkipPartitioning(true)
              .withTimely(false)
              .withRestrictByOrgUnitOpeningClosedDate(true)
              .withRestrictByCategoryOptionStartEndDate(true)
              .withAggregationType(SUM)
              .build();

      Map<String, Double> targetMap = getAggregatedCompletenessTargetMap(targetParams);

      Map<String, Double> dataMap =
          metric != EXPECTED_REPORTS ? getAggregatedCompletenessValueMap(params) : new HashMap<>();

      Integer periodIndex = params.getPeriodDimensionIndex();
      Integer dataSetIndex = DX_INDEX;
      Map<String, PeriodType> dsPtMap = params.getDataSetPeriodTypeMap();
      PeriodType filterPeriodType = params.getFilterPeriodType();

      int timeUnits = getTimeUnits(params);

      for (Map.Entry<String, Double> entry : targetMap.entrySet()) {
        List<String> dataRow = newArrayList(entry.getKey().split(DIMENSION_SEP));

        Double target = entry.getValue();
        Double actual = firstNonNull(dataMap.get(entry.getKey()), 0d);

        if (target != null) {
          // ---------------------------------------------------------
          // Multiply target value by number of periods in time span
          // ---------------------------------------------------------

          PeriodType queryPt =
              filterPeriodType != null
                  ? filterPeriodType
                  : getPeriodTypeFromIsoString(dataRow.get(periodIndex));
          PeriodType dataSetPt = dsPtMap.get(dataRow.get(dataSetIndex));

          target =
              getCalculatedTarget(
                  periodIndex,
                  timeUnits,
                  dataRow,
                  target,
                  queryPt,
                  dataSetPt,
                  params.getFilterPeriods());

          addReportRateToGrid(params, grid, metric, dataRow, target, actual);
        }
      }
    }
  }

  /**
   * Returns the number of filter periods, or 1 if no filter periods exist.
   *
   * @param params the {@link DataQueryParams}.
   * @return the number of filter periods, or 1 if no filter periods exist.
   */
  private int getTimeUnits(DataQueryParams params) {
    return params.hasFilter(PERIOD_DIM_ID) ? params.getFilterPeriods().size() : 1;
  }

  /**
   * Calculates reporting rate and replace data set with rate and add the rate
   * to the Grid.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid} to be manipulated.
   * @param metric the object to assist with the report rate calculation.
   * @param dataRow the current dataRow, based on the key map built by
   *        {@link #getAggregatedCompletenessTargetMap(DataQueryParams)).
   * @param target the current value of the respective key ("dataRow"). See
   * @param actual the current actual value from
   *        {@link #getAggregatedCompletenessValueMap(DataQueryParams)} or
   *        zero (default).
   */
  private void addReportRateToGrid(
      DataQueryParams params,
      Grid grid,
      ReportingRateMetric metric,
      List<String> dataRow,
      Double target,
      Double actual) {
    Double value = getReportingRate(metric, target, actual);

    String reportingRate = getDimensionItem(dataRow.get(DX_INDEX), metric);
    dataRow.set(DX_INDEX, reportingRate);

    if (satisfiesMeasureCriteria(params, value)) {
      grid.addRow()
          .addValues(dataRow.toArray())
          .addValue(params.isSkipRounding() ? value : getRoundedValueObject(params, value));

      if (params.isIncludeNumDen()) {
        grid.addValue(actual).addValue(target).addValue(PERCENT).addNullValues(2);
      }
    }
  }

  private boolean satisfiesMeasureCriteria(DataQueryParams params, Double value) {
    if (params.hasMeasureCriteria() && value != null) {
      Number finalValue =
          params.isSkipRounding() ? value : (Number) getRoundedValueObject(params, value);

      return params.getMeasureCriteria().entrySet().stream()
          .anyMatch(
              measureValue ->
                  measureValue
                      .getKey()
                      .measureIsValid(finalValue.doubleValue(), measureValue.getValue()));
    }

    return true;
  }

  /**
   * Calculates the reporting rate based on the given parameters.
   *
   * @param metric the {@link ReportingRateMetric}.
   * @param target the target value.
   * @param actual the actual value.
   * @return the reporting rate.
   */
  private Double getReportingRate(ReportingRateMetric metric, Double target, Double actual) {
    Double value = 0d;

    if (EXPECTED_REPORTS == metric) {
      value = target;
    } else if (ACTUAL_REPORTS == metric || ACTUAL_REPORTS_ON_TIME == metric) {
      value = actual;
    } else if (!isZero(target)) {
      // REPORTING_RATE or REPORTING_RATE_ON_TIME
      value = min(((actual * PERCENT) / target), 100d);
    }

    return value;
  }

  /**
   * Generates aggregated values for the given query. Creates a mapping between a dimension key and
   * the aggregated value. The dimension key is a concatenation of the identifiers of the dimension
   * items separated by "-".
   *
   * @param params the {@link DataQueryParams}.
   * @return a mapping between a dimension key and the aggregated value.
   */
  private Map<String, Double> getAggregatedCompletenessValueMap(DataQueryParams params) {
    return getDoubleMap(getAggregatedValueMap(params, COMPLETENESS, newArrayList()));
  }

  /**
   * Generates a mapping between the data set dimension key and the count of expected data sets to
   * report.
   *
   * @param params the {@link DataQueryParams}.
   * @return a mapping between the data set dimension key and the count of expected data sets to
   *     report.
   */
  private Map<String, Double> getAggregatedCompletenessTargetMap(DataQueryParams params) {
    List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers = newArrayList();
    queryGroupers.add(queryPlanner::groupByStartEndDateRestriction);

    return getDoubleMap(getAggregatedValueMap(params, COMPLETENESS_TARGET, queryGroupers));
  }

  /**
   * Adds data element operand values to the given grid.
   *
   * @param params the {@link DataQueryParams}.
   * @param grid the {@link Grid}.
   * @param totalType the operand {@link TotalType}.
   */
  private void addDataElementOperandValues(DataQueryParams params, Grid grid, TotalType totalType) {
    List<DataElementOperand> operands = asTypedList(params.getAllDataElementOperands());
    operands =
        operands.stream()
            .filter(o -> totalType.equals(o.getTotalType()))
            .collect(Collectors.toList());

    if (operands.isEmpty()) {
      return;
    }

    DataQueryParams operandParams = getOperandDataQueryParams(params, operands, totalType);

    Map<String, Object> aggregatedDataMap = getAggregatedDataValueMapObjectTyped(operandParams);

    aggregatedDataMap = convertDxToOperand(aggregatedDataMap, totalType);

    for (Map.Entry<String, Object> entry : aggregatedDataMap.entrySet()) {
      Object value = getRoundedValueObject(operandParams, entry.getValue());

      grid.addRow().addValues(entry.getKey().split(DIMENSION_SEP)).addValue(value);

      if (params.isIncludeNumDen()) {
        grid.addNullValues(NUMERATOR_DENOMINATOR_PROPERTIES_COUNT);
      }
    }
  }

  /**
   * Based on the list of operands, it adds dimensions and filters into the given {@link
   * DataQueryParams}.
   *
   * @param params the {@link DataQueryParams}.
   * @param operands the collection of {@link DataElementOperand}.
   * @param totalType the {@link TotalType}.
   * @return mapped DataQueryParams
   */
  DataQueryParams getOperandDataQueryParams(
      DataQueryParams params, List<DataElementOperand> operands, TotalType totalType) {

    List<DimensionalItemObject> dataElements = newArrayList(getDataElements(operands));
    List<DimensionalItemObject> categoryOptionCombos =
        newArrayList(getCategoryOptionCombos(operands));
    List<DimensionalItemObject> attributeOptionCombos =
        newArrayList(getAttributeOptionCombos(operands));
    List<DimensionalItemObject> dataElementOperands = params.getDataElementOperands();
    List<DimensionalItemObject> filterDataElementOperands = params.getFilterDataElementOperands();

    DataQueryParams.Builder builder = newBuilder(params).removeDimension(DATA_X_DIM_ID);

    // Data elements.
    handleDataElementOperands(
        getDataElementInDataElementOperands(dataElementOperands, dataElements),
        getDataElementInDataElementOperands(filterDataElementOperands, dataElements),
        builder,
        DATA_X_DIM_ID,
        DATA_X);

    // Category option combos.
    if (totalType.isCategoryOptionCombo()) {
      handleDataElementOperands(
          getCategoryOptionCombosInDataElementOperands(dataElementOperands, categoryOptionCombos),
          getCategoryOptionCombosInDataElementOperands(
              filterDataElementOperands, categoryOptionCombos),
          builder,
          CATEGORYOPTIONCOMBO_DIM_ID,
          CATEGORY_OPTION_COMBO);
    }

    // Attribute option combos.
    if (totalType.isAttributeOptionCombo()) {
      handleDataElementOperands(
          getAttributeOptionComboDimensionInDataElementOperands(
              dataElementOperands, attributeOptionCombos),
          getAttributeOptionComboDimensionInDataElementOperands(
              filterDataElementOperands, attributeOptionCombos),
          builder,
          ATTRIBUTEOPTIONCOMBO_DIM_ID,
          ATTRIBUTE_OPTION_COMBO);
    }

    return builder.build();
  }

  /**
   * Decides if data element operands should or not be added to the "builder" reference, based on
   * the given arguments. Note that the "builder" object might have his state changed.
   *
   * @param dataElementOperands the list of data element operands ({@link DimensionalItemObject}).
   * @param filterDataElementOperands the list of filter data element operands ({@link
   *     DimensionalItemObject}).
   * @param builder the current {@link Builder}.
   * @param dimensionUid the dimension uid.
   * @param dimensionType the {@link DimensionType}.
   */
  private void handleDataElementOperands(
      List<DimensionalItemObject> dataElementOperands,
      List<DimensionalItemObject> filterDataElementOperands,
      Builder builder,
      String dimensionUid,
      DimensionType dimensionType) {

    addDimensionToBuilder(dataElementOperands, builder, dimensionUid, dimensionType);
    addFilterToBuilder(filterDataElementOperands, builder, dimensionUid, dimensionType);
  }

  /**
   * Adds the given list "dimensionalItemObjects" to the filter of the "builder", if the list is not
   * empty. Note that the "builder" object might have his state changed.
   *
   * @param dimensionalItemObjects the list of data element operands ({@link
   *     DimensionalItemObject}).
   * @param builder the current {@link Builder}.
   * @param dimensionUid the dimension uid.
   * @param dimensionType the {@link DimensionType}.
   */
  private static void addFilterToBuilder(
      List<DimensionalItemObject> dimensionalItemObjects,
      Builder builder,
      String dimensionUid,
      DimensionType dimensionType) {
    if (!dimensionalItemObjects.isEmpty()) {
      builder.addFilter(
          new BaseDimensionalObject(dimensionUid, dimensionType, dimensionalItemObjects));
    }
  }

  /**
   * Adds the given list "dimensionalItemObjects" to the dimension of the "builder", if the list is
   * not empty. Note that the "builder" object might have his state changed.
   *
   * @param dimensionalItemObjects the list of data element operands ({@link
   *     DimensionalItemObject}).
   * @param builder the current {@link Builder}.
   * @param dimensionUid the dimension uid.
   * @param dimensionType the {@link DimensionType}.
   */
  private static void addDimensionToBuilder(
      List<DimensionalItemObject> dimensionalItemObjects,
      Builder builder,
      String dimensionUid,
      DimensionType dimensionType) {
    if (!dimensionalItemObjects.isEmpty()) {
      builder.addDimension(
          new BaseDimensionalObject(dimensionUid, dimensionType, dimensionalItemObjects));
    }
  }

  /**
   * Returns a list of category option combos found in the given data element operands.
   *
   * @param dataElementOperands the list of {@link DimensionalItemObject}.
   * @param categoryOptionCombos the list of {@link DimensionalItemObject}.
   * @return the list of category option combos as {@link DimensionalItemObject}.
   */
  private List<DimensionalItemObject> getCategoryOptionCombosInDataElementOperands(
      List<DimensionalItemObject> dataElementOperands,
      List<DimensionalItemObject> categoryOptionCombos) {
    List<DimensionalItemObject> dimensionCategoryOptionCombos = new ArrayList<>();

    for (DimensionalItemObject coc : categoryOptionCombos) {
      if (matchCategoryOptionCombo(dataElementOperands, coc)) {
        dimensionCategoryOptionCombos.add(coc);
      }
    }

    return dimensionCategoryOptionCombos;
  }

  /**
   * Evaluates the given list of data element operands and returns true if there is match for the
   * "coc" provided.
   *
   * @param dataElementOperands the list of {@link DimensionalItemObject}.
   * @param coc the {@DimensionalItemObject} representing a category option combo.
   * @return true if there is a match, false otherwise.
   */
  private boolean matchCategoryOptionCombo(
      List<DimensionalItemObject> dataElementOperands, DimensionalItemObject coc) {
    for (DimensionalItemObject deo : dataElementOperands) {
      if (((DataElementOperand) deo).getCategoryOptionCombo() != null
          && (((DataElementOperand) deo).getCategoryOptionCombo().getUid().equals(coc.getUid()))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns a list of attribute option combos found in the given data element operands.
   *
   * @param dataElementOperands the list of {@link DimensionalItemObject}.
   * @param attributeOptionCombos the list of {@link DimensionalItemObject}.
   * @return the list of attribute option combos as {@link DimensionalItemObject}.
   */
  private List<DimensionalItemObject> getAttributeOptionComboDimensionInDataElementOperands(
      List<DimensionalItemObject> dataElementOperands,
      List<DimensionalItemObject> attributeOptionCombos) {
    List<DimensionalItemObject> dimensionAttributeOptionCombos = new ArrayList<>();
    for (DimensionalItemObject aoc : attributeOptionCombos) {
      if (matchAttributeOptionCombo(dataElementOperands, aoc)) {
        dimensionAttributeOptionCombos.add(aoc);
      }
    }

    return dimensionAttributeOptionCombos;
  }

  /**
   * Evaluates the given list of data element operands and returns true if there is match for the
   * "aoc" provided.
   *
   * @param dataElementOperands the list of {@link DimensionalItemObject}.
   * @param aoc the {@DimensionalItemObject} representing an attribute option combo.
   * @return true if there is a match, false otherwise.
   */
  private boolean matchAttributeOptionCombo(
      List<DimensionalItemObject> dataElementOperands, DimensionalItemObject aoc) {
    for (DimensionalItemObject deo : dataElementOperands) {
      if (((DataElementOperand) deo).getAttributeOptionCombo() != null
          && (((DataElementOperand) deo).getAttributeOptionCombo().getUid().equals(aoc.getUid()))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns a list of data elements found in the given data element operands.
   *
   * @param dataElementOperands the list of {@link DimensionalItemObject}.
   * @param dataElements the list of {@link DimensionalItemObject}.
   * @return the list of data elements as {@link DimensionalItemObject}.
   */
  private List<DimensionalItemObject> getDataElementInDataElementOperands(
      List<DimensionalItemObject> dataElementOperands, List<DimensionalItemObject> dataElements) {
    List<DimensionalItemObject> dimensionDataElements = new ArrayList<>();

    for (DimensionalItemObject de : dataElements) {
      if (matchDataElement(dataElementOperands, de)) {
        dimensionDataElements.add(de);
      }
    }

    return dimensionDataElements;
  }

  /**
   * Evaluates the given list of data element operands and returns true if there is match for the
   * "de" provided.
   *
   * @param dataElementOperands the list of {@link DimensionalItemObject}.
   * @param de the {@DimensionalItemObject} representing a data element.
   * @return true if there is a match, false otherwise.
   */
  private boolean matchDataElement(
      List<DimensionalItemObject> dataElementOperands, DimensionalItemObject de) {
    for (DimensionalItemObject deo : dataElementOperands) {
      if (((DataElementOperand) deo).getDataElement() != null
          && (((DataElementOperand) deo).getDataElement().getUid().equals(de.getUid()))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Generates aggregated values for the given query. Creates a mapping between a dimension key and
   * the aggregated value. The dimension key is a concatenation of the identifiers of the dimension
   * items separated by "-".
   *
   * @param params the {@link DataQueryParams}.
   * @return a mapping between a dimension key and the aggregated value.
   */
  private Map<String, Object> getAggregatedDataValueMapObjectTyped(DataQueryParams params) {
    return getAggregatedValueMap(params, DATA_VALUE, newArrayList());
  }

  /**
   * Returns a mapping of permutation keys and mappings of data element operands and values based on
   * the given query.
   *
   * @param params the {@link DataQueryParams}.
   */
  private Map<String, List<DimensionItemObjectValue>> getPermutationDimensionItemValueMap(
      DataQueryParams params, List<DimensionalItemObject> items) {
    MultiValuedMap<String, DimensionItemObjectValue> aggregatedDataMap =
        getAggregatedDataValueMap(params, items);

    return getPermutationDimensionalItemValueMap(aggregatedDataMap);
  }

  /**
   * Checks whether the measure criteria in query parameters is satisfied for the given indicator
   * value.
   *
   * @param params the {@link DataQueryParams}.
   * @param value the {@link IndicatorValue}.
   * @param indicator the {@link Indicator}.
   * @return true if all the measure criteria are satisfied for this indicator value, false
   *     otherwise.
   */
  private boolean satisfiesMeasureCriteria(
      DataQueryParams params, IndicatorValue value, Indicator indicator) {
    if (!params.hasMeasureCriteria() || value == null) {
      return true;
    }

    Double indicatorRoundedValue =
        getRoundedValue(params, indicator.getDecimals(), value.getValue()).doubleValue();

    return params.getMeasureCriteria().entrySet().stream()
        .allMatch(
            measureValue ->
                measureValue
                    .getKey()
                    .measureIsValid(indicatorRoundedValue, measureValue.getValue()));
  }

  /**
   * Handles the case where there are no dimension item permutations by adding an empty dimension
   * item list to the permutations list. This state occurs where there are only data or category
   * option combo dimensions specified.
   *
   * @param dimensionItemPermutations list of {@link DimensionItem} permutations.
   */
  private void handleEmptyDimensionItemPermutations(
      List<List<DimensionItem>> dimensionItemPermutations) {
    if (dimensionItemPermutations.isEmpty()) {
      dimensionItemPermutations.add(new ArrayList<>());
    }
  }

  /**
   * Generates a mapping of permutations keys and mappings of organisation unit group and counts.
   *
   * @param params the {@link DataQueryParams}.
   * @param indicators the indicators for which formulas to scan for organisation unit groups.
   * @return a map of maps.
   */
  private Map<String, Map<String, Integer>> getOrgUnitTargetMap(
      DataQueryParams params, Collection<Indicator> indicators) {
    List<OrganisationUnitGroup> orgUnitGroups =
        expressionService.getOrgUnitGroupCountGroups(indicators);

    if (orgUnitGroups.isEmpty()) {
      return null;
    }

    DataQueryParams orgUnitTargetParams =
        newBuilder(params)
            .pruneToDimensionType(ORGANISATION_UNIT)
            .addDimension(
                new BaseDimensionalObject(
                    ORGUNIT_GROUP_DIM_ID, ORGANISATION_UNIT_GROUP, orgUnitGroups))
            .withOutputFormat(ANALYTICS)
            .withSkipPartitioning(true)
            .withSkipDataDimensionValidation(true)
            .build();

    Map<String, Double> orgUnitCountMap =
        getAggregatedOrganisationUnitTargetMap(orgUnitTargetParams);

    return getPermutationOrgUnitGroupCountMap(orgUnitCountMap);
  }

  /**
   * Generates a mapping between the organisation unit dimension key and the count of organisation
   * units inside the subtree of the given organisation units and members of the given organisation
   * unit groups.
   *
   * @param params the {@link DataQueryParams}.
   * @return a mapping between the the data set dimension key and the count of expected data sets to
   *     report.
   */
  private Map<String, Double> getAggregatedOrganisationUnitTargetMap(DataQueryParams params) {
    return getDoubleMap(getAggregatedValueMap(params, ORG_UNIT_TARGET, newArrayList()));
  }

  /**
   * Resolves the numerator and denominator expressions of indicators in the data query.
   *
   * @param params the {@link DataQueryParams}.
   * @return the resolved list of indicators.
   */
  private List<Indicator> resolveIndicatorExpressions(DataQueryParams params) {
    List<Indicator> indicators = asTypedList(params.getIndicators());

    for (Indicator indicator : indicators) {
      for (ExpressionResolver resolver : resolvers.getExpressionResolvers()) {
        indicator.setNumerator(resolver.resolve(indicator.getNumerator()));

        indicator.setDenominator(resolver.resolve(indicator.getDenominator()));
      }
    }

    return indicators;
  }

  /**
   * Resolves expressions of expression dimension items in the data query.
   *
   * @param params the {@link DataQueryParams}.
   * @return the resolved list of expression dimension items.
   */
  private List<ExpressionDimensionItem> resolveExpressionDimensionItemExpressions(
      DataQueryParams params) {
    List<ExpressionDimensionItem> expressionDimensionItems =
        asTypedList(params.getExpressionDimensionItems());

    for (ExpressionDimensionItem item : expressionDimensionItems) {
      for (ExpressionResolver resolver : resolvers.getExpressionResolvers()) {
        item.setExpression(resolver.resolve(item.getExpression()));
      }
    }

    return expressionDimensionItems;
  }

  /**
   * Returns a mapping between dimension items and values for the given data query and list of
   * indicators. The dimensional items part of the indicator numerators and denominators are used as
   * dimensional item for the aggregated values being retrieved. In case of circular references
   * between Indicators, an exception is thrown.
   *
   * @param params the {@link DataQueryParams}.
   * @param items the list of {@link DimensionalItemObject}.
   * @return a dimensional items to aggregate values map.
   */
  private MultiValuedMap<String, DimensionItemObjectValue> getAggregatedDataValueMap(
      DataQueryParams params, List<DimensionalItemObject> items) {
    if (items.isEmpty()) {
      return new ArrayListValuedHashMap<>();
    }

    DimensionalObject dimension =
        new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, null, DISPLAY_NAME_DATA_X, items);

    // If any items have periodOffset and periods are in filters, we need to
    // temporarily move periods to dimensions to get period-specific values
    boolean hasPeriodOffset = hasPeriodOffsetInItems(items);
    boolean periodsInFilter = params.getPeriods().isEmpty() && !params.getFilterPeriods().isEmpty();

    DataQueryParams.Builder builder =
        newBuilder(params)
            .replaceDimension(dimension)
            .withMeasureCriteria(new HashMap<>())
            .withIncludeNumDen(false)
            .withSkipHeaders(true)
            .withOutputFormat(ANALYTICS)
            .withSkipMeta(true);

    if (hasPeriodOffset && periodsInFilter) {
      // Move filter periods to dimensions to get period-specific data
      builder.withPeriods(params.getFilterPeriods()).removeFilter(PERIOD_DIM_ID);
    }

    DataQueryParams dataSourceParams = builder.build();

    Grid grid = dataAggregator.getAggregatedDataValueGrid(dataSourceParams);

    if (isEmpty(grid.getRows())) {
      return new ArrayListValuedHashMap<>();
    }

    return getAggregatedValueMapFromGrid(dataSourceParams, items, grid);
  }

  /**
   * Gets a mapping between dimension items and values from a grid.
   *
   * @param params the {@link DataQueryParams}.
   * @param items the list of {@link DimensionalItemObject}.
   * @param grid the {@link Grid}.
   * @return a dimensional items to aggregate values map.
   */
  private MultiValuedMap<String, DimensionItemObjectValue> getAggregatedValueMapFromGrid(
      DataQueryParams params, List<DimensionalItemObject> items, Grid grid) {
    // Derive the Grid indexes for data, value and period based on the first
    // row of the Grid

    int dataIndex = getGridIndexByDimensionItem(grid.getRow(0), items, 0);
    int periodIndex = getGridIndexByDimensionItem(grid.getRow(0), params.getPeriods(), 1);
    int valueIndex = grid.getWidth() - 1;

    List<DimensionalItemObject> basePeriods = params.getPeriods();

    MultiValuedMap<String, DimensionItemObjectValue> valueMap = new ArrayListValuedHashMap<>();

    Map<String, List<Object>> yearToDateRows = new HashMap<>();

    // Process the grid rows. If yearToDate, build any yearToDate rows for
    // adding later. Otherwise, add the row to the result.

    List<DimensionalItemObject> nonYearToDateItems =
        items.stream().filter(i -> !isYearToDate(i)).collect(toList());

    Map<String, List<DimensionalItemObject>> yearToDateItemsById =
        items.stream()
            .filter(PeriodOffsetUtils::isYearToDate)
            .collect(groupingBy(DimensionalItemObject::getDimensionItemWithQueryModsId));

    for (List<Object> row : grid.getRows()) {
      for (DimensionalItemObject dimensionalItem :
          findDimensionalItems((String) row.get(dataIndex), nonYearToDateItems)) {
        addRowToValueMap(periodIndex, valueIndex, row, dimensionalItem, basePeriods, valueMap);
      }

      List<DimensionalItemObject> yearToDateItems = yearToDateItemsById.get(row.get(dataIndex));

      if (yearToDateItems != null) {
        buildYearToDateRows(
            periodIndex, valueIndex, row, yearToDateItems, basePeriods, yearToDateRows);
      }
    }

    if (!yearToDateRows.isEmpty()) {
      addYearToDateRowsToValueMap(
          dataIndex, periodIndex, valueIndex, items, basePeriods, yearToDateRows, valueMap);
    }

    return valueMap;
  }

  private void addYearToDateRowsToValueMap(
      int dataIndex,
      int periodIndex,
      int valueIndex,
      List<DimensionalItemObject> items,
      List<DimensionalItemObject> basePeriods,
      Map<String, List<Object>> yearToDateRows,
      MultiValuedMap<String, DimensionItemObjectValue> valueMap) {
    List<DimensionalItemObject> yearToDateItems =
        items.stream().filter(PeriodOffsetUtils::isYearToDate).collect(toList());

    for (List<Object> row : yearToDateRows.values()) {
      for (DimensionalItemObject dimensionalItem :
          findDimensionalItems((String) row.get(dataIndex), yearToDateItems)) {
        addRowToValueMap(periodIndex, valueIndex, row, dimensionalItem, basePeriods, valueMap);
      }
    }
  }

  private void addRowToValueMap(
      int periodIndex,
      int valueIndex,
      List<Object> row,
      DimensionalItemObject dimensionalItem,
      List<DimensionalItemObject> basePeriods,
      MultiValuedMap<String, DimensionItemObjectValue> valueMap) {
    Optional<List<Object>> adjustedRow =
        getAdjustedRow(periodIndex, valueIndex, row, dimensionalItem, basePeriods);

    if (adjustedRow.isPresent()) {
      List<Object> aRow = adjustedRow.get();
      String key = join(remove(aRow.toArray(new Object[0]), valueIndex), DIMENSION_SEP);
      Double value = ((Number) aRow.get(valueIndex)).doubleValue();

      valueMap.put(key, new DimensionItemObjectValue(dimensionalItem, value));
    }
  }

  /**
   * Add the given Indicator values to the given grid.
   *
   * @param params the current DataQueryParams.
   * @param grid the current Grid.
   * @param dataSourceParams the DataQueryParams built for Indicators.
   * @param indicator the Indicator which the values will be extracted from, and added to be added
   *     to the Grid.
   * @param dimensionItems the dimensional items permutation. See {@link
   *     DataQueryParams#getDimensionItemPermutations()}.
   * @param value the IndicatorValue which the values will be extracted from.
   */
  private void addIndicatorValuesToGrid(
      DataQueryParams params,
      Grid grid,
      DataQueryParams dataSourceParams,
      Indicator indicator,
      List<DimensionItem> dimensionItems,
      IndicatorValue value) {
    if (value != null && satisfiesMeasureCriteria(params, value, indicator)) {
      List<DimensionItem> row = new ArrayList<>(dimensionItems);

      row.add(DX_INDEX, new DimensionItem(DATA_X_DIM_ID, indicator));

      grid.addRow()
          .addValues(getItemIdentifiers(row))
          .addValue(getRoundedValue(dataSourceParams, indicator.getDecimals(), value.getValue()));

      if (params.isIncludeNumDen()) {
        grid.addValue(
                getRoundedValue(
                    dataSourceParams, indicator.getDecimals(), value.getNumeratorValue()))
            .addValue(
                getRoundedValue(
                    dataSourceParams, indicator.getDecimals(), value.getDenominatorValue()))
            .addValue(getRoundedValue(dataSourceParams, indicator.getDecimals(), value.getFactor()))
            .addValue(value.getMultiplier())
            .addValue(value.getDivisor());
      }
    }
  }

  private Optional<List<Object>> getAdjustedRow(
      int periodIndex,
      int valueIndex,
      List<Object> row,
      DimensionalItemObject dimensionalItemObject,
      List<DimensionalItemObject> basePeriods) {
    if (!hasPeriod(row, periodIndex)) {
      return Optional.of(row);
    }

    if (row.get(valueIndex) == null) {
      return Optional.empty();
    }

    int periodOffset =
        (dimensionalItemObject.getQueryMods() == null)
            ? 0
            : dimensionalItemObject.getQueryMods().getPeriodOffset();

    List<Object> adjustedRow =
        (periodOffset != 0) ? getPeriodOffsetRow(row, periodIndex, periodOffset) : row;

    // Check if the current row's Period belongs to the list of
    // periods from the original Analytics request. The row may
    // not have a Period if Period is used as filter.

    if (!isPeriodInPeriods((String) adjustedRow.get(periodIndex), basePeriods)) {
      return Optional.empty();
    }

    return Optional.of(adjustedRow);
  }

  /**
   * Generates a mapping between a dimension key and the aggregated value. The dimension key is a
   * concatenation of the identifiers of the dimension items separated by "-".
   *
   * @param params the {@link DataQueryParams}.
   * @param tableType the {@link AnalyticsTableType}.
   * @param queryGroupers the list of additional query groupers to use for query planning, use empty
   *     list for none.
   * @return a mapping between a dimension key and aggregated values.
   */
  private Map<String, Object> getAggregatedValueMap(
      DataQueryParams params,
      AnalyticsTableType tableType,
      List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers) {
    int optimalQueries = getWithin(getProcessNo(), 1, MAX_QUERIES);

    int maxLimit =
        params.isIgnoreLimit() ? 0 : settingsProvider.getCurrentSettings().getAnalyticsMaxLimit();

    Timer timer = new Timer().start().disablePrint();

    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder()
            .withOptimalQueries(optimalQueries)
            .withTableType(tableType)
            .withQueryGroupers(queryGroupers)
            .build();

    DataQueryGroups queryGroups = queryPlanner.planQuery(params, plannerParams);

    timer.getSplitTime(
        "Planned analytics query, got: {} for optimal: {}",
        queryGroups.getLargestGroupSize(),
        optimalQueries);

    Map<String, Object> map = new HashMap<>();

    for (List<DataQueryParams> queries : queryGroups.getSequentialQueries()) {
      executeQueries(tableType, maxLimit, map, queries);
    }

    timer.getTime("Got analytics values");

    return map;
  }

  /**
   * Executes the given list of queries in parallel.
   *
   * @param tableType the {@link AnalyticsTableType}.
   * @param maxLimit the max limit of records to retrieve.
   * @param map the map of metadata identifiers to data values.
   * @param queries the list of {@link DataQueryParams} to execute.
   */
  private void executeQueries(
      AnalyticsTableType tableType,
      int maxLimit,
      Map<String, Object> map,
      List<DataQueryParams> queries) {
    List<Future<Map<String, Object>>> futures = new ArrayList<>();

    for (DataQueryParams query : queries) {
      futures.add(analyticsManager.getAggregatedDataValues(query, tableType, maxLimit));
    }

    for (Future<Map<String, Object>> future : futures) {
      try {
        Map<String, Object> taskValues = future.get();

        if (taskValues != null) {
          map.putAll(taskValues);
        }
      } catch (Exception ex) {
        log.error(getStackTrace(ex));
        log.error(getStackTrace(ex.getCause()));

        if (ex.getCause() instanceof RuntimeException) {
          // Throw the real exception
          throw (RuntimeException) ex.getCause();
        } else {
          throw new RuntimeException("Error during execution of aggregation query task", ex);
        }
      }
    }
  }

  /**
   * Gets the number of available cores. Uses explicit number from system setting if available.
   * Detects number of cores from current server runtime if not.
   *
   * @return the number of available cores.
   */
  private int getProcessNo() {
    int cores = settingsProvider.getCurrentSettings().getDatabaseServerCpus();
    return cores == 0 ? getCpuCores() : cores;
  }

  /**
   * Generates aggregated values for the given query. Creates a mapping between a dimension key and
   * the aggregated value. The dimension key is a concatenation of the identifiers of the dimension
   * items separated by "-".
   *
   * @param params the {@link DataQueryParams}.
   * @return a mapping between a dimension key and the aggregated value.
   */
  private Map<String, Double> getAggregatedDataValueMap(DataQueryParams params) {
    return getDoubleMap(getAggregatedValueMap(params, DATA_VALUE, newArrayList()));
  }

  void require(DataAggregator dataAggregator) {
    this.dataAggregator = dataAggregator;
  }
}
