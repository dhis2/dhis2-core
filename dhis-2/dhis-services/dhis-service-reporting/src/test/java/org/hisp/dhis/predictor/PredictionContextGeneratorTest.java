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
package org.hisp.dhis.predictor;

import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Collections.emptyList;
import static org.hisp.dhis.category.CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME;
import static org.hisp.dhis.category.CategoryOption.DEFAULT_NAME;
import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.hisp.dhis.predictor.PredictionContextGenerator.getContexts;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.Test;

/**
 * Tests PredictionContextGenerator.
 *
 * @author Jim Grace
 */
class PredictionContextGeneratorTest extends TestBase {
  private final OrganisationUnit ouA = createOrganisationUnit('A');

  private final Period periodA = createPeriod("202201");

  private final Period periodB = createPeriod("202202");

  private final Period periodC = createPeriod("202203");

  private final CategoryOption coDefault = createCategoryOption(DEFAULT_NAME, "coIsDefault");

  private final Category catDefault = createCategory(DEFAULT_NAME, "caIsDefault", coDefault);

  private final CategoryCombo ccDefault =
      new CategoryCombo(DEFAULT_CATEGORY_COMBO_NAME, DISAGGREGATION, List.of(catDefault));

  private final CategoryOptionCombo cocDefault =
      createCategoryOptionCombo(DEFAULT_NAME, "cocDefault", ccDefault, coDefault);

  private final CategoryCombo ccA = createCategoryCombo('D');

  private final CategoryOptionCombo cocD = createCategoryOptionCombo(ccA);

  private final DataElement deA = createDataElement('A', ccA);

  private final DataElementOperand deaA =
      new DataElementOperand(deA, createCategoryOptionCombo('C'));

  private final ProgramIndicator piA =
      createProgramIndicator('A', createProgram('A'), "Expression A", "Filter A");

  private final TrackedEntityAttribute teaA = createTrackedEntityAttribute('A');

  private final ProgramTrackedEntityAttributeDimensionItem paA =
      new ProgramTrackedEntityAttributeDimensionItem(createProgram('B'), teaA);

  private final ProgramTrackedEntityAttributeDimensionItem paB =
      new ProgramTrackedEntityAttributeDimensionItem(createProgram('C'), teaA);

  private final List<Period> outputPeriods = Lists.newArrayList(periodB, periodC);

  private final CategoryOptionCombo aocA = createCategoryOptionCombo(createCategoryCombo('A'));

  private final CategoryOptionCombo aocB = createCategoryOptionCombo(createCategoryCombo('B'));

  private final CategoryOptionCombo aocX = createCategoryOptionCombo(createCategoryCombo('X'));

  private final FoundDimensionItemValue aocVal1 =
      new FoundDimensionItemValue(ouA, periodA, null, aocA, deA, 1.0);

  private final FoundDimensionItemValue aocVal2 =
      new FoundDimensionItemValue(ouA, periodB, null, aocA, deaA, 2.0);

  private final FoundDimensionItemValue aocVal3 =
      new FoundDimensionItemValue(ouA, periodB, null, aocA, piA, 3.0);

  private final FoundDimensionItemValue aocVal4 =
      new FoundDimensionItemValue(ouA, periodC, null, aocB, teaA, 4.0);

  private final CategoryOptionCombo aocNone = null;

  private final FoundDimensionItemValue nonAocVal5 =
      new FoundDimensionItemValue(ouA, periodB, null, aocNone, paA, 5.0);

  private final FoundDimensionItemValue nonAocVal6 =
      new FoundDimensionItemValue(ouA, periodB, null, aocNone, paB, 6.0);

  private final List<FoundDimensionItemValue> aocValues =
      Lists.newArrayList(aocVal1, aocVal2, aocVal3, aocVal4);

  private final List<FoundDimensionItemValue> nonAocValues =
      Lists.newArrayList(nonAocVal5, nonAocVal6);

  private final List<FoundDimensionItemValue> allValues =
      Lists.newArrayList(aocVal1, aocVal2, aocVal3, aocVal4, nonAocVal5, nonAocVal6);

  private final List<FoundDimensionItemValue> noValues = emptyList();

  private final MapMap<Period, DimensionalItemObject, Object> emptyPeriodValueMap = new MapMap<>();

  private final OrganisationUnitLevel ouLevel1 = new OrganisationUnitLevel(1, "1");

  private final Expression expressionA = createExpression2('A', "1");

  private final Predictor predictorA =
      createPredictor(
          deA, cocD, "A", expressionA, null, periodA.getPeriodType(), Set.of(ouLevel1), 0, 0, 0);

  private final PredictionDisaggregator preDisA =
      new PredictionDisaggregator(predictorA, emptyList(), cocDefault);

  // Disaggregated prediction: output data element deX has category catX with options X1 and X2

  private final CategoryOption coX1 = createCategoryOption("coX1", "coX1xxxxxxx");

  private final CategoryOption coX2 = createCategoryOption("coX2", "coX2xxxxxxx");

  private final Category catX = createCategory("catX", "catXxxxxxxx", coX1, coX2);

  private final CategoryCombo ccX = createCategoryCombo("ccX", "ccXxxxxxxxx", catX);

  private final CategoryOptionCombo cocX1 = newCoc(1L, "cocX1", "cocX1xxxxxx", ccX, coX1);

  private final CategoryOptionCombo cocX2 = newCoc(2L, "cocX2", "cocX2xxxxxx", ccX, coX2);

  private final DataElement deX = createDataElement('X', ccX);

  private final Predictor predictorX =
      createPredictor(
          deX, null, "X", expressionA, null, periodA.getPeriodType(), Set.of(ouLevel1), 0, 0, 0);

  private final PredictionDisaggregator preDisX =
      new PredictionDisaggregator(predictorX, emptyList(), cocDefault);

  private final FoundDimensionItemValue cocVal7 =
      new FoundDimensionItemValue(ouA, periodB, cocX1, aocA, piA, 7.0);

  private final FoundDimensionItemValue cocVal8 =
      new FoundDimensionItemValue(ouA, periodB, cocX2, aocA, piA, 8.0);

  // -------------------------------------------------------------------------
  // Format prediction contexts for ease of reading.
  // -------------------------------------------------------------------------

  // This may seem like a lot of work, but it makes it extremely easier to
  // read any test failures.

  private final Map<DimensionalItemObject, String> itemName =
      Map.of(
          deA, "deA",
          deaA, "deaA",
          piA, "piA",
          teaA, "teaA",
          paA, "paA",
          paB, "paB");

  private final Map<Period, String> periodName =
      Map.of(
          periodA, "periodA",
          periodB, "periodB",
          periodC, "periodC");

  private final Map<CategoryOptionCombo, String> aocName =
      Map.of(
          aocA, "aocA",
          aocB, "aocB",
          aocX, "aocX");

  private final Map<CategoryOptionCombo, String> cocName =
      Map.of(
          cocD, "cocD",
          cocX1, "cocX1",
          cocX2, "cocX2");

  private List<String> formatPredictionContextList(List<PredictionContext> contexts) {
    return contexts.stream().map(this::formatPredictionContext).collect(Collectors.toList());
  }

  private String formatPredictionContext(PredictionContext ctx) {
    return "( coc:"
        + cocName.get(ctx.getCategoryOptionCombo())
        + ", aoc:"
        + aocName.get(ctx.getAttributeOptionCombo())
        + ", period:"
        + periodName.get(ctx.getOutputPeriod())
        + ", periodValueMap:"
        + formatPeriodValueMap(ctx.getPeriodValueMap())
        + ")";
  }

  private String formatPeriodValueMap(
      MapMap<Period, DimensionalItemObject, Object> periodValueMap) {
    String format = "{";

    for (Map.Entry<Period, Map<DimensionalItemObject, Object>> e :
        sort3(periodValueMap.entrySet())) {
      format += periodName.get(e.getKey()) + ":" + formatValueMap(e.getValue()) + ",";
    }

    return stripTrailingComma(format) + "}";
  }

  private String formatValueMap(Map<DimensionalItemObject, Object> valueMap) {
    String format = "{";

    for (Map.Entry<DimensionalItemObject, Object> e : sort2(valueMap.entrySet())) {
      format += itemName.get(e.getKey()) + ":" + e.getValue() + ",";
    }

    return stripTrailingComma(format) + "}";
  }

  private String stripTrailingComma(String s) {
    return s.lastIndexOf(',') == s.length() - 1 ? s.substring(0, s.length() - 1) : s;
  }

  private List<Map.Entry<Period, Map<DimensionalItemObject, Object>>> sort3(
      Set<Map.Entry<Period, Map<DimensionalItemObject, Object>>> entrySet) {
    List<Map.Entry<Period, Map<DimensionalItemObject, Object>>> entries = new ArrayList<>(entrySet);

    entries.sort(Comparator.comparing(e -> e.getKey().getIsoDate()));

    return entries;
  }

  private List<Map.Entry<DimensionalItemObject, Object>> sort2(
      Set<Map.Entry<DimensionalItemObject, Object>> entrySet) {
    List<Map.Entry<DimensionalItemObject, Object>> entries = new ArrayList<>(entrySet);

    entries.sort(Comparator.comparing(e -> (Double) e.getValue()));

    return entries;
  }

  private CategoryOptionCombo newCoc(
      Long id, String name, String uid, CategoryCombo cc, CategoryOption... options) {
    CategoryOptionCombo coc = createCategoryOptionCombo(name, uid, cc, options);
    coc.setId(id);
    cc.getOptionCombos().add(coc);

    return coc;
  }

  // -------------------------------------------------------------------------
  // Check order
  // -------------------------------------------------------------------------

  /**
   * For each attribute option combination, earlier periods must be evaluated before later periods.
   * This is because an earlier predicted value may be used as input to a later prediction.
   *
   * <p>However, the ordering between contexts of attribute option combinations does not matter.
   * That order depends on the order they are pulled out of a Map within the context generator. This
   * only checks the order of periods within contexts of the same attribute option combination.
   */
  private void checkOrder(List<PredictionContext> contexts) {
    List<CategoryOptionCombo> aocs =
        contexts.stream()
            .map(PredictionContext::getAttributeOptionCombo)
            .collect(Collectors.toList());

    for (CategoryOptionCombo aoc : aocs) {
      List<Period> outputPeriods =
          contexts.stream()
              .filter(c -> c.getAttributeOptionCombo().equals(aoc))
              .map(PredictionContext::getOutputPeriod)
              .collect(Collectors.toList());

      List<Period> sortedPeriods = new ArrayList<>(outputPeriods);

      sortedPeriods.sort(Comparator.comparing(Period::getStartDate));

      assertEquals(sortedPeriods, outputPeriods);
    }
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  @Test
  void testGetContextsWithAocData() {
    // valueMap for attributeOptionCombo A, period B:
    Map<DimensionalItemObject, Object> aocAPerBValueMap =
        Map.of(
            deaA, 2.0,
            piA, 3.0);

    // valueMap for attributeOptionCombo B, period C:
    Map<DimensionalItemObject, Object> aocBPerCValueMap = Map.of(teaA, 4.0);

    // periodValueMap for attributeOptionCombo A:
    MapMap<Period, DimensionalItemObject, Object> aocAPeriodValueMap =
        MapMap.ofEntries(
            immutableEntry(periodA, Map.of(deA, 1.0)), immutableEntry(periodB, aocAPerBValueMap));

    // periodValueMap for attributeOptionCombo B:
    MapMap<Period, DimensionalItemObject, Object> aocBPeriodValueMap =
        MapMap.ofEntries(immutableEntry(periodC, aocBPerCValueMap));

    PredictionContext expected1 = new PredictionContext(cocD, aocA, periodB, aocAPeriodValueMap);
    PredictionContext expected2 = new PredictionContext(cocD, aocA, periodC, aocAPeriodValueMap);
    PredictionContext expected3 = new PredictionContext(cocD, aocB, periodB, aocBPeriodValueMap);
    PredictionContext expected4 = new PredictionContext(cocD, aocB, periodC, aocBPeriodValueMap);

    String formatted1 = formatPredictionContext(expected1);
    String formatted2 = formatPredictionContext(expected2);
    String formatted3 = formatPredictionContext(expected3);
    String formatted4 = formatPredictionContext(expected4);

    List<PredictionContext> actual = getContexts(outputPeriods, aocValues, aocX, preDisA);

    List<String> actualFormatted = formatPredictionContextList(actual);

    assertContainsOnly(List.of(formatted1, formatted2, formatted3, formatted4), actualFormatted);

    checkOrder(actual);
  }

  @Test
  void testGetContextsWithNonAocData() {
    // valueMap for attributeOptionCombo X, period B:
    Map<DimensionalItemObject, Object> aocXPerBValueMap =
        Map.of(
            paA, 5.0,
            paB, 6.0);

    // periodValueMap for attributeOptionCombo X (default AOC):
    MapMap<Period, DimensionalItemObject, Object> aocXPeriodValueMap =
        MapMap.ofEntries(immutableEntry(periodB, aocXPerBValueMap));

    PredictionContext expected1 = new PredictionContext(cocD, aocX, periodB, aocXPeriodValueMap);
    PredictionContext expected2 = new PredictionContext(cocD, aocX, periodC, aocXPeriodValueMap);

    String formatted1 = formatPredictionContext(expected1);
    String formatted2 = formatPredictionContext(expected2);

    List<PredictionContext> actual = getContexts(outputPeriods, nonAocValues, aocX, preDisA);

    List<String> actualFormatted = formatPredictionContextList(actual);

    assertContainsOnly(List.of(formatted1, formatted2), actualFormatted);

    checkOrder(actual);
  }

  @Test
  void testGetContextsWithAocAndNonAocData() {
    // valueMap for attributeOptionCombo A, period B:
    Map<DimensionalItemObject, Object> aocAPerBValueMap =
        Map.of(
            deaA, 2.0,
            piA, 3.0,
            paA, 5.0,
            paB, 6.0);

    // valueMap for attributeOptionCombo A, period B:
    Map<DimensionalItemObject, Object> aocBPerBValueMap =
        Map.of(
            paA, 5.0,
            paB, 6.0);

    // valueMap for attributeOptionCombo B, period C:
    Map<DimensionalItemObject, Object> aocBPerCValueMap = Map.of(teaA, 4.0);

    // periodValueMap for attributeOptionCombo A:
    MapMap<Period, DimensionalItemObject, Object> aocAPeriodValueMap =
        MapMap.ofEntries(
            immutableEntry(periodA, Map.of(deA, 1.0)), immutableEntry(periodB, aocAPerBValueMap));

    // periodValueMap for attributeOptionCombo B:
    MapMap<Period, DimensionalItemObject, Object> aocBPeriodValueMap =
        MapMap.ofEntries(
            immutableEntry(periodB, aocBPerBValueMap), immutableEntry(periodC, aocBPerCValueMap));

    PredictionContext expected1 = new PredictionContext(cocD, aocA, periodB, aocAPeriodValueMap);
    PredictionContext expected2 = new PredictionContext(cocD, aocA, periodC, aocAPeriodValueMap);
    PredictionContext expected3 = new PredictionContext(cocD, aocB, periodB, aocBPeriodValueMap);
    PredictionContext expected4 = new PredictionContext(cocD, aocB, periodC, aocBPeriodValueMap);

    String formatted1 = formatPredictionContext(expected1);
    String formatted2 = formatPredictionContext(expected2);
    String formatted3 = formatPredictionContext(expected3);
    String formatted4 = formatPredictionContext(expected4);

    List<PredictionContext> actual = getContexts(outputPeriods, allValues, aocX, preDisA);

    List<String> actualFormatted = formatPredictionContextList(actual);

    assertContainsOnly(List.of(formatted1, formatted2, formatted3, formatted4), actualFormatted);

    checkOrder(actual);
  }

  @Test
  void testGetContextsWithNoData() {
    PredictionContext expected1 = new PredictionContext(cocD, aocX, periodB, emptyPeriodValueMap);
    PredictionContext expected2 = new PredictionContext(cocD, aocX, periodC, emptyPeriodValueMap);

    String formatted1 = formatPredictionContext(expected1);
    String formatted2 = formatPredictionContext(expected2);

    List<PredictionContext> actual = getContexts(outputPeriods, noValues, aocX, preDisA);

    List<String> actualFormatted = formatPredictionContextList(actual);

    assertContainsOnly(List.of(formatted1, formatted2), actualFormatted);

    checkOrder(actual);
  }

  @Test
  void testGetContextsWithDisaggregation() {
    // periodValueMaps for attributeOptionCombo A, for each output COC:
    MapMap<Period, DimensionalItemObject, Object> cocX1PeriodValueMap =
        MapMap.ofEntries(immutableEntry(periodB, Map.of(piA, 7.0)));
    MapMap<Period, DimensionalItemObject, Object> cocX2PeriodValueMap =
        MapMap.ofEntries(immutableEntry(periodB, Map.of(piA, 8.0)));

    PredictionContext expected1 = new PredictionContext(cocX1, aocA, periodB, cocX1PeriodValueMap);
    PredictionContext expected2 = new PredictionContext(cocX2, aocA, periodB, cocX2PeriodValueMap);
    PredictionContext expected3 = new PredictionContext(cocX1, aocA, periodC, cocX1PeriodValueMap);
    PredictionContext expected4 = new PredictionContext(cocX2, aocA, periodC, cocX2PeriodValueMap);

    List<PredictionContext> actual =
        getContexts(outputPeriods, List.of(cocVal7, cocVal8), aocX, preDisX);

    assertContainsOnly(
        List.of(
            formatPredictionContext(expected1),
            formatPredictionContext(expected2),
            formatPredictionContext(expected3),
            formatPredictionContext(expected4)),
        formatPredictionContextList(actual));

    checkOrder(actual);
  }
}
