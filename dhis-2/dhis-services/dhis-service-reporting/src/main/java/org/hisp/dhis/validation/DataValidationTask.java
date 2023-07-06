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
package org.hisp.dhis.validation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.ParseType.SIMPLE_TEST;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.hisp.dhis.system.util.MathUtils.addDoubleObjects;
import static org.hisp.dhis.system.util.MathUtils.roundSignificant;
import static org.hisp.dhis.system.util.MathUtils.zeroIfNull;
import static org.hisp.dhis.system.util.ValidationUtils.getObjectValue;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs a validation task on a thread within a multi-threaded validation run.
 *
 * <p>Each task looks for validation results in a different organisation unit.
 *
 * @author Jim Grace
 */
@Slf4j
@Component("validationTask")
@Scope("prototype")
public class DataValidationTask implements ValidationTask {
  public static final String NAME = "validationTask";

  public static final String NON_AOC = ""; // String that is not an Attribute
  // Option Combo

  private final ExpressionService expressionService;

  private final DataValueService dataValueService;

  private final CategoryService categoryService;

  private final PeriodService periodService;

  public DataValidationTask(
      ExpressionService expressionService,
      DataValueService dataValueService,
      CategoryService categoryService,
      PeriodService periodService) {
    checkNotNull(expressionService);
    checkNotNull(dataValueService);
    checkNotNull(categoryService);
    checkNotNull(periodService);

    this.expressionService = expressionService;
    this.dataValueService = dataValueService;
    this.categoryService = categoryService;
    this.periodService = periodService;
  }

  // (wired through constructor)
  private AnalyticsService analyticsService;

  private List<OrganisationUnit> orgUnits;

  private ValidationRunContext context;

  private Set<ValidationResult> validationResults;

  private PeriodTypeExtended periodTypeX; // Current period type extended.

  private Period period; // Current period.

  private OrganisationUnit orgUnit; // Current organisation unit.

  private long orgUnitId; // Current organisation unit id.

  private ValidationRuleExtended ruleX; // Current rule extended.

  // Data for current period and all rules being evaluated:
  private MapMapMap<Long, String, DimensionalItemObject, Object> dataMap;

  private MapMapMap<Long, String, DimensionalItemObject, Object> slidingWindowDataMap;

  @Override
  public void init(
      List<OrganisationUnit> orgUnits,
      ValidationRunContext context,
      AnalyticsService analyticsService) {
    this.orgUnits = orgUnits;
    this.context = context;
    this.analyticsService = analyticsService;
  }

  /**
   * Evaluates validation rules for a single organisation unit. This is the central method in
   * validation rule evaluation.
   */
  @Override
  @Transactional
  public void run() {
    try {
      runInternal();
    } catch (Exception ex) {
      log.error(DebugUtils.getStackTrace(ex));

      throw ex;
    }
  }

  /**
   * Get the data needed for this task, then evaluate each combination of organisation unit / period
   * / validation rule.
   */
  private void runInternal() {
    if (context.isAnalysisComplete()) {
      return;
    }

    loop:
    for (PeriodTypeExtended ptx : context.getPeriodTypeXs()) {
      periodTypeX = ptx;

      for (Period p : periodTypeX.getPeriods()) {
        period = p;

        getData();

        for (OrganisationUnit ou : orgUnits) {
          orgUnit = ou;
          orgUnitId = ou.getId();

          for (ValidationRuleExtended r : periodTypeX.getRuleXs()) {
            ruleX = r;

            if (context.isAnalysisComplete()) {
              break loop;
            }
            validationResults = new HashSet<>();
            validateRule();
            addValidationResultsToContext();
          }
        }
      }
    }
  }

  /**
   * Validates one rule / period by seeing which attribute option combos exist for that data, and
   * then iterating through those attribute option combos.
   */
  private void validateRule() {
    // Skip validation if org unit level does not match
    if (!ruleX.getOrganisationUnitLevels().isEmpty()
        && !ruleX.getOrganisationUnitLevels().contains(orgUnit.getLevel())) {
      return;
    }

    MapMap<String, DimensionalItemObject, Object> leftValueMap =
        getValueMap(ruleX.getLeftSlidingWindow());
    MapMap<String, DimensionalItemObject, Object> rightValueMap =
        getValueMap(ruleX.getRightSlidingWindow());

    Map<String, Double> leftSideValues =
        getExpressionValueMap(ruleX.getRule().getLeftSide(), leftValueMap);
    Map<String, Double> rightSideValues =
        getExpressionValueMap(ruleX.getRule().getRightSide(), rightValueMap);

    Set<String> attributeOptionCombos =
        Sets.union(leftSideValues.keySet(), rightSideValues.keySet());

    if (context.isAnalysisComplete()) {
      return;
    }

    for (String optionCombo : attributeOptionCombos) {
      if (NON_AOC.compareTo(optionCombo) == 0) {
        continue;
      }

      if (context.processExpressionDetails()) {
        setExpressionDetails(leftValueMap.get(optionCombo), rightValueMap.get(optionCombo));
      } else {
        validateOptionCombo(
            optionCombo, leftSideValues.get(optionCombo), rightSideValues.get(optionCombo));
      }
    }
  }

  private void setExpressionDetails(
      Map<DimensionalItemObject, Object> leftSideValues,
      Map<DimensionalItemObject, Object> rightSideValues) {
    setExpressionSideDetails(
        context.getValidationRuleExpressionDetails().getLeftSide(),
        periodTypeX.getLeftSideItemIds(),
        leftSideValues);

    setExpressionSideDetails(
        context.getValidationRuleExpressionDetails().getRightSide(),
        periodTypeX.getRightSideItemIds(),
        rightSideValues);
  }

  private void setExpressionSideDetails(
      List<Map<String, String>> detailsSide,
      Set<DimensionalItemId> sideIds,
      Map<DimensionalItemObject, Object> valueMap) {
    for (DimensionalItemId itemId : sideIds) {
      DimensionalItemObject itemObject = context.getItemMap().get(itemId);

      Object itemValue = valueMap.get(itemObject);

      String itemValueString = itemValue == null ? null : itemValue.toString();

      ValidationRuleExpressionDetails.addDetailTo(
          itemObject.getName(), itemValueString, detailsSide);
    }
  }

  /**
   * Validates one rule / period / attribute option combo.
   *
   * @param optionCombo the attribute option combo.
   * @param leftSide left side value.
   * @param rightSide right side value.
   */
  private void validateOptionCombo(String optionCombo, Double leftSide, Double rightSide) {
    // Skipping any results we already know
    if (context.skipValidationOfTuple(
        orgUnit,
        ruleX.getRule(),
        period,
        optionCombo,
        periodService.getDayInPeriod(period, new Date()))) {
      return;
    }

    boolean violation = isViolation(leftSide, rightSide);

    if (violation && !context.isAnalysisComplete()) {
      validationResults.add(
          new ValidationResult(
              ruleX.getRule(),
              period,
              orgUnit,
              getAttributeOptionCombo(optionCombo),
              roundSignificant(zeroIfNull(leftSide)),
              roundSignificant(zeroIfNull(rightSide)),
              periodService.getDayInPeriod(period, new Date())));
    }
  }

  /**
   * Determines if left and right side values violate a rule.
   *
   * @param leftSide the left side value.
   * @param rightSide the right side value.
   * @return true if violation, otherwise false.
   */
  private boolean isViolation(Double leftSide, Double rightSide) {
    if (Operator.compulsory_pair.equals(ruleX.getRule().getOperator())) {
      return (leftSide == null) != (rightSide == null);
    }

    if (Operator.exclusive_pair.equals(ruleX.getRule().getOperator())) {
      return (leftSide != null) && (rightSide != null);
    }

    if (leftSide == null) {
      if (ruleX.getRule().getLeftSide().getMissingValueStrategy() == NEVER_SKIP) {
        leftSide = 0d;
      } else {
        return false;
      }
    }

    if (rightSide == null) {
      if (ruleX.getRule().getRightSide().getMissingValueStrategy() == NEVER_SKIP) {
        rightSide = 0d;
      } else {
        return false;
      }
    }

    String test = leftSide + ruleX.getRule().getOperator().getMathematicalOperator() + rightSide;
    return !(Boolean)
        expressionService.getExpressionValue(
            ExpressionParams.builder().expression(test).parseType(SIMPLE_TEST).build());
  }

  /**
   * Gets the data for this period:
   *
   * <p>dataMap contains data for non-sliding window expressions. slidingWindowDataMap contains data
   * for sliding window expressions.
   */
  private void getData() {
    getDataValueMap();

    dataMap.putMap(getAnalyticsMap(true, periodTypeX.getIndicators()));

    slidingWindowDataMap = new MapMapMap<>();

    if (periodTypeX.areSlidingWindowsNeeded()) {
      slidingWindowDataMap.putMap(dataMap);

      slidingWindowDataMap.putMap(getEventMapForSlidingWindow(true, periodTypeX.getEventItems()));
      slidingWindowDataMap.putMap(
          getEventMapForSlidingWindow(false, periodTypeX.getEventItemsWithoutAttributeOptions()));
    }

    if (periodTypeX.areNonSlidingWindowsNeeded()) {
      dataMap.putMap(getAnalyticsMap(true, periodTypeX.getEventItems()));
      dataMap.putMap(getAnalyticsMap(false, periodTypeX.getEventItemsWithoutAttributeOptions()));
    }
  }

  private MapMap<String, DimensionalItemObject, Object> getValueMap(boolean slidingWindow) {
    return slidingWindow ? slidingWindowDataMap.get(orgUnitId) : dataMap.get(orgUnitId);
  }

  /** Adds any validation results we found to the validation context. */
  private void addValidationResultsToContext() {
    if (validationResults.size() > 0) {
      context.getValidationResults().addAll(validationResults);
    }
  }

  private Period getPeriod(long id) {
    Period p = context.getPeriodIdMap().get(id);

    if (p == null) {
      log.trace("DataValidationTask calling getPeriod( id " + id + " )");

      p = periodService.getPeriod(id);

      log.trace("DataValidationTask called getPeriod( id " + id + " )");

      context.getPeriodIdMap().put(id, p);
    }

    return p;
  }

  private CategoryOptionCombo getAttributeOptionCombo(long id) {
    CategoryOptionCombo aoc = context.getAocIdMap().get(id);

    if (aoc == null) {
      log.trace("DataValidationTask calling getCategoryOptionCombo( id " + id + " )");

      aoc = categoryService.getCategoryOptionCombo(id);

      log.trace("DataValidationTask called getCategoryOptionCombo( id " + id + ")");

      addToAocCache(aoc);
    }

    return aoc;
  }

  private CategoryOptionCombo getAttributeOptionCombo(String uid) {
    CategoryOptionCombo aoc = context.getAocUidMap().get(uid);

    if (aoc == null) {
      log.trace("DataValidationTask calling getCategoryOptionCombo( uid " + uid + " )");

      aoc = categoryService.getCategoryOptionCombo(uid);

      log.trace("DataValidationTask called getCategoryOptionCombo( uid " + uid + ")");

      addToAocCache(aoc);
    }

    return aoc;
  }

  private void addToAocCache(CategoryOptionCombo aoc) {
    context.getAocIdMap().put(aoc.getId(), aoc);
    context.getAocUidMap().put(aoc.getUid(), aoc);
  }

  /**
   * Evaluates an expression, returning a map of values by attribute option combo.
   *
   * @param expression expression to evaluate.
   * @param valueMap Map of value maps, by attribute option combo.
   * @return map of values.
   */
  private Map<String, Double> getExpressionValueMap(
      Expression expression, MapMap<String, DimensionalItemObject, Object> valueMap) {
    Map<String, Double> expressionValueMap = new HashMap<>();

    if (valueMap == null) {
      return expressionValueMap;
    }

    Map<DimensionalItemObject, Object> nonAocValues = valueMap.get(NON_AOC);

    for (Map.Entry<String, Map<DimensionalItemObject, Object>> entry : valueMap.entrySet()) {
      Map<DimensionalItemObject, Object> values = entry.getValue();

      if (nonAocValues != null) {
        values.putAll(nonAocValues);
      }

      Double value =
          castDouble(
              expressionService.getExpressionValue(
                  context.getBaseExParams().toBuilder()
                      .expression(expression.getExpression())
                      .parseType(VALIDATION_RULE_EXPRESSION)
                      .valueMap(values)
                      .days(period.getDaysInPeriod())
                      .missingValueStrategy(expression.getMissingValueStrategy())
                      .orgUnit(orgUnit)
                      .build()));

      if (MathUtils.isValidDouble(value)) {
        expressionValueMap.put(entry.getKey(), value);
      }
    }

    return expressionValueMap;
  }

  /** Gets data elements and data element operands from the datavalue table. */
  private void getDataValueMap() {
    DataExportParams params = new DataExportParams();
    params.setDataElements(periodTypeX.getDataElements());
    params.setDataElementOperands(periodTypeX.getDataElementOperands());
    params.setIncludedDate(period.getStartDate());
    params.setOrganisationUnits(new HashSet<>(orgUnits));
    params.setPeriodTypes(periodTypeX.getAllowedPeriodTypes());
    params.setCoDimensionConstraints(context.getCoDimensionConstraints());
    params.setCogDimensionConstraints(context.getCogDimensionConstraints());

    if (context.getAttributeCombo() != null) {
      params.setAttributeOptionCombos(Sets.newHashSet(context.getAttributeCombo()));
    }

    List<DeflatedDataValue> dataValues = dataValueService.getDeflatedDataValues(params);

    dataMap = new MapMapMap<>();

    MapMapMap<Long, String, DimensionalItemObject, Long> duplicateCheck = new MapMapMap<>();

    for (DeflatedDataValue dv : dataValues) {
      DataElement dataElement = periodTypeX.getDataElementIdMap().get(dv.getDataElementId());
      String deoIdKey = periodTypeX.getDeoIds(dv.getDataElementId(), dv.getCategoryOptionComboId());
      DataElementOperand dataElementOperand =
          periodTypeX.getDataElementOperandIdMap().get(deoIdKey);
      Period p = getPeriod(dv.getPeriodId());
      long orgUnitId = dv.getSourceId();
      String attributeOptionComboUid =
          getAttributeOptionCombo(dv.getAttributeOptionComboId()).getUid();

      if (dataElement != null) {
        Object value = getObjectValue(dv.getValue(), dataElement.getValueType());

        addValueToDataMap(
            orgUnitId, attributeOptionComboUid, dataElement, value, p, duplicateCheck);
      }

      if (dataElementOperand != null) {
        Object value =
            getObjectValue(dv.getValue(), dataElementOperand.getDataElement().getValueType());

        addValueToDataMap(
            orgUnitId, attributeOptionComboUid, dataElementOperand, value, p, duplicateCheck);
      }
    }
  }

  private void addValueToDataMap(
      long orgUnitId,
      String aocUid,
      DimensionalItemObject dimItemObject,
      Object value,
      Period p,
      MapMapMap<Long, String, DimensionalItemObject, Long> duplicateCheck) {
    Object existingValue = dataMap.getValue(orgUnitId, aocUid, dimItemObject);

    long periodInterval = p.getEndDate().getTime() - p.getStartDate().getTime();

    Long existingPeriodInterval = duplicateCheck.getValue(orgUnitId, aocUid, dimItemObject);

    if (existingPeriodInterval != null) {
      if (existingPeriodInterval < periodInterval) {
        return; // Don't overwrite previous value if a shorter interval
      } else if (existingPeriodInterval > periodInterval) {
        existingValue = null; // Overwrite if for a longer interval
      }
    }

    if (existingValue != null) {
      value = addDoubleObjects(value, existingValue);
    }

    dataMap.putEntry(orgUnitId, aocUid, dimItemObject, value);

    duplicateCheck.putEntry(orgUnitId, aocUid, dimItemObject, periodInterval);
  }

  /**
   * Gets analytics data for the given parameters.
   *
   * @param hasAttributeOptions whether the event data has attribute options.
   */
  private MapMapMap<Long, String, DimensionalItemObject, Object> getAnalyticsMap(
      boolean hasAttributeOptions, Set<DimensionalItemObject> analyticsItems) {
    if (analyticsItems.isEmpty()) {
      return new MapMapMap<>();
    }

    DataQueryParams.Builder paramsBuilder =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(Lists.newArrayList(analyticsItems))
            .withAttributeOptionCombos(Lists.newArrayList())
            .withFilterPeriods(Lists.newArrayList(period))
            .withOrganisationUnits(orgUnits);

    if (hasAttributeOptions) {
      paramsBuilder.withAttributeOptionCombos(Lists.newArrayList());
    }

    return getAnalyticsData(paramsBuilder.build(), hasAttributeOptions);
  }

  /**
   * Gets sliding window analytics event data for the given parameters.
   *
   * @param hasAttributeOptions whether the event data has attribute options.
   */
  private MapMapMap<Long, String, DimensionalItemObject, Object> getEventMapForSlidingWindow(
      boolean hasAttributeOptions, Set<DimensionalItemObject> eventItems) {
    if (eventItems.isEmpty()) {
      return new MapMapMap<>();
    }

    // We want to position the sliding window over the most recent data.
    // To achieve this, we need to satisfy the following criteria:
    //
    // 1. Window end should not be later than the current date
    // 2. Window end should not be later than the period.endDate

    // Criteria 1
    Calendar endDate = Calendar.getInstance();
    Calendar startDate = Calendar.getInstance();

    // Criteria 2
    if (endDate.getTime().after(period.getEndDate())) {
      endDate.setTime(period.getEndDate());
    }

    // The window size is based on the frequencyOrder of the period's
    // periodType:
    startDate.setTime(endDate.getTime());
    startDate.add(Calendar.DATE, (-1 * period.frequencyOrder()));

    DataQueryParams.Builder paramsBuilder =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(Lists.newArrayList(eventItems))
            .withAttributeOptionCombos(Lists.newArrayList())
            .withStartDate(startDate.getTime())
            .withEndDate(endDate.getTime())
            .withOrganisationUnits(orgUnits);

    if (hasAttributeOptions) {
      paramsBuilder.withAttributeOptionCombos(Lists.newArrayList());
    }

    return getAnalyticsData(paramsBuilder.build(), hasAttributeOptions);
  }

  /**
   * Gets analytics data.
   *
   * @param params event data query parameters.
   * @param hasAttributeOptions whether the event data has attribute options.
   * @return event data.
   */
  private MapMapMap<Long, String, DimensionalItemObject, Object> getAnalyticsData(
      DataQueryParams params, boolean hasAttributeOptions) {
    MapMapMap<Long, String, DimensionalItemObject, Object> map = new MapMapMap<>();

    Grid grid;

    try {
      grid = analyticsService.getAggregatedDataValues(params);
    } catch (PersistenceException ex) // No data
    {
      return map;
    } catch (RuntimeException ex) // Other error
    {
      log.error(DebugUtils.getStackTrace(ex));

      return map;
    }

    int dxInx = grid.getIndexOfHeader(DimensionalObject.DATA_X_DIM_ID);
    int ouInx = grid.getIndexOfHeader(DimensionalObject.ORGUNIT_DIM_ID);
    int aoInx =
        hasAttributeOptions
            ? grid.getIndexOfHeader(DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID)
            : 0;
    int vlInx = grid.getWidth() - 1;

    Map<String, OrganisationUnit> ouLookup =
        orgUnits.stream().collect(Collectors.toMap(BaseIdentifiableObject::getUid, o -> o));
    Map<String, DimensionalItemObject> dxLookup =
        periodTypeX.getEventItems().stream()
            .collect(Collectors.toMap(DimensionalItemObject::getDimensionItem, d -> d));
    dxLookup.putAll(
        periodTypeX.getIndicators().stream()
            .collect(Collectors.toMap(DimensionalItemObject::getDimensionItem, d -> d)));

    for (List<Object> row : grid.getRows()) {
      String dx = (String) row.get(dxInx);
      String ao = hasAttributeOptions ? (String) row.get(aoInx) : NON_AOC;
      String ou = (String) row.get(ouInx);
      Object vl = ((Number) row.get(vlInx)).doubleValue();

      OrganisationUnit orgUnit = ouLookup.get(ou);
      DimensionalItemObject analyticsItem = dxLookup.get(dx);

      map.putEntry(orgUnit.getId(), ao, analyticsItem, vl);
    }

    return map;
  }
}
