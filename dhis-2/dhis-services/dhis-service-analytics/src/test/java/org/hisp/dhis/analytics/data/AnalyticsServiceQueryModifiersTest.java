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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests analytics with {@see QueryModifiers}.
 *
 * @author Jim Grace
 */
class AnalyticsServiceQueryModifiersTest extends IntegrationTestBase {
  @Autowired private List<AnalyticsTableService> analyticsTableServices;

  @Autowired private DataElementService dataElementService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private PeriodService periodService;

  @Autowired private DataValueService dataValueService;

  @Autowired private AnalyticsTableGenerator analyticsTableGenerator;

  @Autowired private AnalyticsService analyticsService;

  @Autowired private IndicatorService indicatorService;

  private Period jan;

  private Period feb;

  private Period mar;

  private Period q1;

  private OrganisationUnit ouA;

  private Indicator indicatorA;

  List<String> expected;

  List<String> result;

  // -------------------------------------------------------------------------
  // Fixture
  // -------------------------------------------------------------------------

  @Override
  public void setUpTest() throws IOException, InterruptedException {
    jan = createPeriod("2022-01");
    feb = createPeriod("2022-02");
    mar = createPeriod("2022-03");
    q1 = createPeriod("2022Q1");
    periodService.addPeriod(jan);
    periodService.addPeriod(feb);
    periodService.addPeriod(mar);
    periodService.addPeriod(q1);
    jan = periodService.reloadPeriod(jan);
    feb = periodService.reloadPeriod(feb);
    mar = periodService.reloadPeriod(mar);
    q1 = periodService.reloadPeriod(q1);

    DataElement deA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM);
    DataElement deB = createDataElement('B', ValueType.TEXT, AggregationType.NONE);
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);

    ouA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(ouA);

    CategoryOption optionA = new CategoryOption("CategoryOptionA");
    CategoryOption optionB = new CategoryOption("CategoryOptionB");
    categoryService.addCategoryOption(optionA);
    categoryService.addCategoryOption(optionB);

    Category categoryA = createCategory('A', optionA, optionB);
    categoryService.addCategory(categoryA);

    CategoryCombo categoryComboA = createCategoryCombo('A', categoryA);
    categoryService.addCategoryCombo(categoryComboA);

    CategoryOptionCombo cocA = createCategoryOptionCombo(categoryComboA, optionA);
    CategoryOptionCombo cocB = createCategoryOptionCombo(categoryComboA, optionB);
    cocA.setUid("OptionCombA");
    cocB.setUid("OptionCombB");
    categoryService.addCategoryOptionCombo(cocA);
    categoryService.addCategoryOptionCombo(cocB);
    CategoryOptionCombo aocA = categoryService.getDefaultCategoryOptionCombo();

    categoryComboA.getOptionCombos().add(cocA);
    categoryComboA.getOptionCombos().add(cocB);
    categoryService.updateCategoryCombo(categoryComboA);

    IndicatorType indicatorTypeA = createIndicatorType('A');
    indicatorTypeA.setFactor(1);
    indicatorService.addIndicatorType(indicatorTypeA);

    indicatorA = createIndicator('A', indicatorTypeA);
    indicatorA.setNumerator("1"); // to be overwritten
    indicatorA.setDenominator("1");
    indicatorService.addIndicator(indicatorA);

    dataValueService.addDataValue(newDataValue(deA, jan, ouA, cocA, aocA, "1"));
    dataValueService.addDataValue(newDataValue(deA, feb, ouA, cocB, aocA, "2"));
    dataValueService.addDataValue(newDataValue(deA, mar, ouA, cocA, aocA, "3"));
    dataValueService.addDataValue(newDataValue(deB, jan, ouA, cocA, aocA, "A"));
    dataValueService.addDataValue(newDataValue(deB, feb, ouA, cocB, aocA, "B"));

    // Wait before generating analytics tables
    Thread.sleep(5000);

    // Generate analytics tables
    analyticsTableGenerator.generateTables(
        AnalyticsTableUpdateParams.newBuilder().build(), NoopJobProgress.INSTANCE);

    // Wait after generating analytics tables
    Thread.sleep(8000);
  }

  @Override
  public void tearDownTest() {
    for (AnalyticsTableService service : analyticsTableServices) {
      service.dropTables();
    }
  }

  // -------------------------------------------------------------------------
  // Test
  // -------------------------------------------------------------------------

  @Test
  void queryModifiersTest() {
    // TODO: refactor IntegrationTestBase and/or BaseSpringTest to provide
    // @BeforeAll and @AfterAll methods that can be overridden so the
    // following be individual @Tests while analytics is built only once.

    // aggregationType

    testNoAggregationType();
    testAverageAggregationType();
    testLastAggregationType();
    testWithAndWithoutAggregationType();
    testMultipleAggregationTypes();
    testGroupedAggregationType();
    testOperandAggregationType();

    // periodOffset

    testSimplePeriodOffset();
    testInsideAndOutsidePeriodOffset();
    testOperandPeriodOffset();
    testGroupedPeriodOffset();
    testAdditivePeriodOffset();

    // minDate and maxDate

    testMinDate();
    testMaxDate();
    testMinAndMaxDate();

    // subExpression

    testSimpleSubExpression();
    testMultipleReferenceSubExpression();
    testSubExpressionConversionFromTextToNumeric();
    testReferencesInsideAndOutsideOfSubExpression();
    testTwoSubExpressions();
    testOperandSubExpression();
  }

  // -------------------------------------------------------------------------
  // aggregationType
  // -------------------------------------------------------------------------

  private void testNoAggregationType() {
    expected =
        List.of(
            "inabcdefghA-202201-1.0",
            "inabcdefghA-202202-2.0",
            "inabcdefghA-202203-3.0",
            "inabcdefghA-2022Q1-6.0");

    result = query("#{deabcdefghA}", jan, feb, mar, q1);

    assertEquals(expected, result);
  }

  private void testAverageAggregationType() {
    expected = List.of("inabcdefghA-2022Q1-2.0");

    result = query("#{deabcdefghA}.aggregationType(AVERAGE)", q1);

    assertEquals(expected, result);
  }

  private void testLastAggregationType() {
    expected = List.of("inabcdefghA-2022Q1-3.0");

    result = query("#{deabcdefghA.OptionCombA}.aggregationType(LAST)", q1);

    assertEquals(expected, result);
  }

  private void testWithAndWithoutAggregationType() {
    expected = List.of("inabcdefghA-2022Q1-8.0");

    result = query("#{deabcdefghA} + #{deabcdefghA}.aggregationType(AVERAGE)", q1);

    assertEquals(expected, result);
  }

  private void testMultipleAggregationTypes() {
    expected = List.of("inabcdefghA-2022Q1-5.0");

    result =
        query("#{deabcdefghA}.aggregationType(MAX) + #{deabcdefghA}.aggregationType(AVERAGE)", q1);

    assertEquals(expected, result);
  }

  private void testGroupedAggregationType() {
    expected = List.of("inabcdefghA-2022Q1-6.0");

    result = query("(2*#{deabcdefghA} + #{deabcdefghA}).aggregationType(AVERAGE)", q1);

    assertEquals(expected, result);
  }

  private void testOperandAggregationType() {
    expected = List.of("inabcdefghA-2022Q1-4.0");

    result =
        query(
            "#{deabcdefghA.OptionCombA}.aggregationType(AVERAGE) + #{deabcdefghA.OptionCombB}", q1);

    assertEquals(expected, result);
  }

  // -------------------------------------------------------------------------
  // periodOffset
  // -------------------------------------------------------------------------

  private void testSimplePeriodOffset() {
    expected = List.of("inabcdefghA-202202-1.0", "inabcdefghA-202203-2.0");

    result = query("#{deabcdefghA}.periodOffset(-1)", jan, feb, mar);

    assertEquals(expected, result);
  }

  private void testInsideAndOutsidePeriodOffset() {
    expected =
        List.of("inabcdefghA-202201-3.0", "inabcdefghA-202202-5.0", "inabcdefghA-202203-3.0");

    result = query("#{deabcdefghA} + #{deabcdefghA}.periodOffset(1)", jan, feb, mar);

    assertEquals(expected, result);
  }

  private void testGroupedPeriodOffset() {
    expected = List.of("inabcdefghA-202202-2.0", "inabcdefghA-202203-4.0");

    result = query("(#{deabcdefghA} + #{deabcdefghA}).periodOffset(-1)", jan, feb, mar);

    assertEquals(expected, result);
  }

  private void testAdditivePeriodOffset() {
    expected = List.of("inabcdefghA-202202-1.0", "inabcdefghA-202203-3.0");

    result =
        query("(#{deabcdefghA}.periodOffset(-1) + #{deabcdefghA}).periodOffset(-1)", jan, feb, mar);

    assertEquals(expected, result);
  }

  private void testOperandPeriodOffset() {
    expected = List.of("inabcdefghA-202201-4.0", "inabcdefghA-202202-1.0");

    result =
        query(
            "#{deabcdefghA.OptionCombA}.periodOffset(-1) + 2*#{deabcdefghA.OptionCombB}.periodOffset(1)",
            jan,
            feb,
            mar);

    assertEquals(expected, result);
  }

  // -------------------------------------------------------------------------
  // minDate and maxDate
  // -------------------------------------------------------------------------

  private void testMinDate() {
    expected = List.of("inabcdefghA-202202-2.0", "inabcdefghA-202203-3.0");

    result = query("#{deabcdefghA}.minDate(2022-2-1)", jan, feb, mar);

    assertEquals(expected, result);
  }

  private void testMaxDate() {
    expected = List.of("inabcdefghA-202201-1.0", "inabcdefghA-202202-2.0");

    result = query("#{deabcdefghA}.maxDate(2022-2-28)", jan, feb, mar);

    assertEquals(expected, result);
  }

  private void testMinAndMaxDate() {
    expected = List.of("inabcdefghA-202202-2.0");

    result = query("#{deabcdefghA}.minDate(2022-2-1).maxDate(2022-2-28)", jan, feb, mar);

    assertEquals(expected, result);
  }

  // -------------------------------------------------------------------------
  // subExpression
  // -------------------------------------------------------------------------

  private void testSimpleSubExpression() {
    expected =
        List.of(
            "inabcdefghA-202201-4.0",
            "inabcdefghA-202202-5.0",
            "inabcdefghA-202203-5.0",
            "inabcdefghA-2022Q1-14.0");

    result = query("subExpression(if(#{deabcdefghA}==1,4,5))", jan, feb, mar, q1);

    assertEquals(expected, result);
  }

  private void testMultipleReferenceSubExpression() {
    expected = List.of("inabcdefghA-202201-0.0", "inabcdefghA-202202-2.0");

    result = query("subExpression(if(#{deabcdefghA}<2,0,#{deabcdefghA}))", jan, feb);

    assertEquals(expected, result);
  }

  private void testSubExpressionConversionFromTextToNumeric() {
    expected = List.of("inabcdefghA-202201-3.0", "inabcdefghA-202202-4.0");

    result = query("subExpression(if(#{deabcdefghB}=='A',3,4))", jan, feb);

    assertEquals(expected, result);
  }

  private void testReferencesInsideAndOutsideOfSubExpression() {
    expected = List.of("inabcdefghA-202201-3.0", "inabcdefghA-202202-8.0");

    result =
        query(
            "3 * #{deabcdefghA} + subExpression(if(#{deabcdefghA}<2,0,#{deabcdefghA}))", jan, feb);

    assertEquals(expected, result);
  }

  private void testTwoSubExpressions() {
    expected = List.of("inabcdefghA-202201-10.0", "inabcdefghA-202202-11.0");

    result =
        query(
            "subExpression(if(#{deabcdefghA}==1,3,5)) + subExpression(if(#{deabcdefghA}==2,6,7))",
            jan,
            feb);

    assertEquals(expected, result);
  }

  private void testOperandSubExpression() {
    expected =
        List.of("inabcdefghA-202201-3.0", "inabcdefghA-202202-2.0", "inabcdefghA-202203-9.0");

    result =
        query(
            "subExpression(if(#{deabcdefghA.OptionCombA}>0,#{deabcdefghA.OptionCombA}*3,0)) + #{deabcdefghA.OptionCombB}",
            jan,
            feb,
            mar);

    assertEquals(expected, result);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /**
   * Creates a data value. Sets the last updated time to something in the past because at the time
   * of this writing analytics won't include the value if it was last updated within the same second
   * as the analytics update.
   */
  private DataValue newDataValue(
      DataElement de,
      Period pe,
      OrganisationUnit ou,
      CategoryOptionCombo coc,
      CategoryOptionCombo aoc,
      String value) {
    return new DataValue(de, pe, ou, coc, aoc, value, null, parseDate("2022-01-01"), null);
  }

  /** Queries analytics with an indicator expression. */
  private List<String> query(String expression, Period... periods) {
    indicatorA.setNumerator(expression);
    indicatorService.updateIndicator(indicatorA);

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withIndicators(List.of(indicatorA))
            .withAggregationType(AnalyticsAggregationType.SUM)
            .withFilterOrganisationUnits(List.of(ouA))
            .withPeriods(List.of(periods))
            .withOutputFormat(OutputFormat.ANALYTICS)
            .build();

    Map<String, Object> map = analyticsService.getAggregatedDataValueMapping(params);

    return map.entrySet().stream()
        .map(e -> e.getKey() + '-' + e.getValue())
        .sorted()
        .collect(Collectors.toList());
  }
}
