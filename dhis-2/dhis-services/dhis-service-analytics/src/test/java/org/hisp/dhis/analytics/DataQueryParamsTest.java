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
package org.hisp.dhis.analytics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class DataQueryParamsTest extends DhisConvenienceTest {
  private IndicatorType it;

  private Indicator inA;

  private Indicator inB;

  private CategoryOption coA;

  private CategoryOption coB;

  private Category caA;

  private CategoryCombo ccA;

  private CategoryOptionCombo cocA;

  private CategoryOptionCombo cocB;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private DataElementOperand deoA;

  private DataElementOperand deoB;

  private DataSet dsA;

  private DataSet dsB;

  private DataSet dsC;

  private DataSet dsD;

  private ReportingRate rrA;

  private ReportingRate rrB;

  private ReportingRate rrC;

  private ReportingRate rrD;

  private Program prA;

  private Program prB;

  private ProgramDataElementDimensionItem pdeA;

  private DataElementGroup degA;

  private DataElementGroup degB;

  private DataElementGroupSet degsA;

  private TrackedEntityAttribute atA;

  private Period peA;

  private Period peB;

  private Period peC;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  // pe:202305:SCHEDULED_DATE;202303:SCHEDULED_DATE;202302:SCHEDULED_DATE

  @BeforeEach
  void setUpTest() {
    it = createIndicatorType('A');
    inA = createIndicator('A', it);
    inB = createIndicator('A', it);
    coA = createCategoryOption('A');
    coB = createCategoryOption('B');
    caA = createCategory('A', coA, coB);
    ccA = createCategoryCombo('A', caA);
    cocA = createCategoryOptionCombo(ccA, coA);
    cocB = createCategoryOptionCombo(ccA, coB);
    ccA.getOptionCombos().add(cocA);
    ccA.getOptionCombos().add(cocB);
    deA = createDataElement('A', ccA);
    deB = createDataElement('B', ccA);
    deC = createDataElement('C', ccA);
    deoA = new DataElementOperand(deA, cocA);
    deoB = new DataElementOperand(deB, cocA);
    dsA = createDataSet('A');
    dsB = createDataSet('B');
    dsC = createDataSet('C');
    dsD = createDataSet('D');
    rrA = new ReportingRate(dsA, ReportingRateMetric.REPORTING_RATE);
    rrB = new ReportingRate(dsB, ReportingRateMetric.REPORTING_RATE);
    rrC = new ReportingRate(dsC, ReportingRateMetric.EXPECTED_REPORTS);
    rrD = new ReportingRate(dsD, ReportingRateMetric.ACTUAL_REPORTS);
    prA = createProgram('A');
    prB = createProgram('B');
    pdeA = new ProgramDataElementDimensionItem(prA, deA);
    degA = createDataElementGroup('A');
    degB = createDataElementGroup('B');
    degsA = createDataElementGroupSet('A');
    degsA.addDataElementGroup(degA);
    degsA.addDataElementGroup(degB);
    atA = createTrackedEntityAttribute('A');
    peA = createPeriod("201601");
    peB = createPeriod("201603");
    peC = createPeriod("2017July");
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
  }

  @Test
  void testAddDimension() {
    DimensionalObject doA =
        new BaseDimensionalObject(
            DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, List.of());
    DimensionalObject doB =
        new BaseDimensionalObject(
            DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID,
            DimensionType.CATEGORY_OPTION_COMBO,
            List.of());
    DimensionalObject doC =
        new BaseDimensionalObject(DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, List.of());
    DimensionalObject doD =
        new BaseDimensionalObject(
            DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID,
            DimensionType.ATTRIBUTE_OPTION_COMBO,
            List.of());
    DimensionalObject doE =
        new BaseDimensionalObject("WpDi1seZU0Z", DimensionType.DATA_ELEMENT_GROUP_SET, List.of());
    DimensionalObject doF =
        new BaseDimensionalObject(DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, List.of());
    DimensionalObject doG =
        new BaseDimensionalObject(
            "Cz3WQznvrCM", DimensionType.ORGANISATION_UNIT_GROUP_SET, List.of());
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(doA)
            .addDimension(doB)
            .addDimension(doC)
            .addDimension(doD)
            .addDimension(doE)
            .addDimension(doF)
            .addDimension(doG)
            .build();
    List<DimensionalObject> dimensions = params.getDimensions();
    assertEquals(7, dimensions.size());
    assertEquals(doF, dimensions.get(0));
    assertEquals(doB, dimensions.get(1));
    assertEquals(doD, dimensions.get(2));
    assertEquals(doA, dimensions.get(3));
    assertEquals(doC, dimensions.get(4));
    assertEquals(doE, dimensions.get(5));
    assertEquals(doG, dimensions.get(6));
  }

  @Test
  void testGetNonPeriodDimensions() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(List.of(deA, deB))
            .withPeriods(List.of(peA, peB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .build();

    assertEquals(3, params.getDimensions().size());
    assertEquals(2, params.getNonPeriodDimensions().size());
    assertTrue(
        params
            .getNonPeriodDimensions()
            .contains(new BaseDimensionalObject(DimensionalObject.DATA_X_DIM_ID)));
    assertTrue(
        params
            .getNonPeriodDimensions()
            .contains(new BaseDimensionalObject(DimensionalObject.ORGUNIT_DIM_ID)));
  }

  @Test
  void testIsAggregationType() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withAggregationType(
                new AnalyticsAggregationType(AggregationType.SUM, AggregationType.AVERAGE))
            .build();

    assertFalse(params.isAggregationType(AggregationType.MAX));
    assertFalse(params.isAggregationType(AggregationType.AVERAGE));
    assertFalse(params.isAggregationType(null));
    assertTrue(params.isAggregationType(AggregationType.SUM));
  }

  @Test
  void testIsAnyAggregationType() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withAggregationType(
                new AnalyticsAggregationType(AggregationType.SUM, AggregationType.AVERAGE))
            .build();

    assertFalse(params.isAnyAggregationType(AggregationType.MAX, AggregationType.MIN));
    assertFalse(params.isAnyAggregationType(AggregationType.AVERAGE, AggregationType.FIRST));
    assertFalse(params.isAnyAggregationType(null, null));
    assertTrue(params.isAnyAggregationType(AggregationType.SUM));
    assertTrue(params.isAnyAggregationType(AggregationType.SUM, AggregationType.MAX));
  }

  @Test
  void testSetGetDataElementsReportingRates() {
    List<? extends DimensionalItemObject> dataElements = List.of(deA, deB, deC);
    List<? extends DimensionalItemObject> reportingRates = List.of(rrA, rrB);
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(dataElements)
            .withReportingRates(reportingRates)
            .build();
    assertEquals(3, params.getDataElements().size());
    assertTrue(params.getDataElements().containsAll(dataElements));
    assertEquals(2, params.getReportingRates().size());
    assertTrue(params.getReportingRates().containsAll(reportingRates));
  }

  @Test
  void testGetDimensionFromParam() {
    assertEquals(
        DATA_X_DIM_ID,
        DimensionalObjectUtils.getDimensionFromParam("dx:D348asd782j;kj78HnH6hgT;9ds9dS98s2"));
  }

  @Test
  void testGetDimensionItemsFromParam() {
    List<String> expected = List.of("D348asd782j", "kj78HnH6hgT", "9ds9dS98s2");
    assertEquals(
        expected,
        DimensionalObjectUtils.getDimensionItemsFromParam("de:D348asd782j;kj78HnH6hgT;9ds9dS98s2"));
  }

  @Test
  void testGetDimensionItemsFromParamForPeriods() {
    List<String> expected = List.of("TODAY:LAST_UPDATED", "LAST_WEEK:INCIDENT_DATE", "YESTERDAY");
    assertEquals(
        expected,
        DimensionalObjectUtils.getDimensionItemsFromParam(
            "pe:TODAY:LAST_UPDATED;LAST_WEEK:INCIDENT_DATE;YESTERDAY"));
  }

  @Test
  void testGetValueFromKeywordParam() {
    assertEquals("4", DimensionalObjectUtils.getValueFromKeywordParam("LEVEL-4"));
    assertNull(DimensionalObjectUtils.getValueFromKeywordParam("LEVEL"));
  }

  @Test
  void testGetMeasureCriteriaFromParam() {
    Map<MeasureFilter, Double> expected = new HashMap<>();
    expected.put(MeasureFilter.GT, 100d);
    expected.put(MeasureFilter.LT, 200d);
    assertEquals(expected, DataQueryParams.getMeasureCriteriaFromParam("GT:100;LT:200"));
  }

  @Test
  void testHasPeriods() {
    DataQueryParams params = DataQueryParams.newBuilder().build();
    assertFalse(params.hasPeriods());
    List<DimensionalItemObject> periods = new ArrayList<>();
    params = DataQueryParams.newBuilder(params).withPeriods(periods).build();
    assertFalse(params.hasPeriods());
    params = DataQueryParams.newBuilder().removeDimension(PERIOD_DIM_ID).build();
    assertFalse(params.hasPeriods());
    periods.add(new Period());
    params = DataQueryParams.newBuilder().withPeriods(periods).build();
    assertTrue(params.hasPeriods());
  }

  @Test
  void testPruneToDimensionType() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(List.of(createIndicator('A', null), createIndicator('B', null)))
            .withOrganisationUnits(
                List.of(createOrganisationUnit('A'), createOrganisationUnit('B')))
            .withFilterPeriods(List.of(createPeriod("201201"), createPeriod("201202")))
            .build();

    assertEquals(2, params.getDimensions().size());
    assertEquals(1, params.getFilters().size());

    params =
        DataQueryParams.newBuilder(params)
            .pruneToDimensionType(DimensionType.ORGANISATION_UNIT)
            .build();

    assertEquals(1, params.getDimensions().size());
    assertEquals(DimensionType.ORGANISATION_UNIT, params.getDimensions().get(0).getDimensionType());
    assertEquals(0, params.getFilters().size());
  }

  @Test
  void testRetainDataDimension() {
    List<DimensionalItemObject> items = List.of(inA, inB, deA, deB, deC, rrA, rrB);
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addOrSetDimensionOptions(
                DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, items)
            .build();
    assertEquals(7, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().size());

    params =
        DataQueryParams.newBuilder(params)
            .retainDataDimension(DataDimensionItemType.DATA_ELEMENT)
            .build();
    assertEquals(3, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().size());
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(deA));
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(deB));
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(deC));
  }

  @Test
  void testRetainDataDimensions() {
    List<DimensionalItemObject> items = List.of(inA, inB, deA, deB, deC, rrA, rrB);
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addOrSetDimensionOptions(
                DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, items)
            .build();
    assertEquals(7, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().size());

    params =
        DataQueryParams.newBuilder(params)
            .retainDataDimensions(
                DataDimensionItemType.DATA_ELEMENT, DataDimensionItemType.REPORTING_RATE)
            .build();
    assertEquals(5, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().size());
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(deA));
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(deB));
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(deC));
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(rrA));
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(rrB));
  }

  @Test
  void testGetDimensionItemArrayExplodeCoc() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addOrSetDimensionOptions(
                DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, List.of(deA, deB, deC))
            .addOrSetDimensionOptions(
                DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, null, List.of(peA, peB))
            .build();

    List<DimensionalItemObject> items =
        params.getDimensionItemsExplodeCoc(DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID);
    assertEquals(2, items.size());
    assertTrue(items.contains(cocA));
    assertTrue(items.contains(cocB));
  }

  @Test
  void testRetainDataDimensionReportingRates() {
    List<DimensionalItemObject> items = List.of(inA, inB, deA, deB, deC, rrA, rrB, rrC, rrD);
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addOrSetDimensionOptions(
                DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, items)
            .build();
    assertEquals(9, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().size());

    params =
        DataQueryParams.newBuilder(params)
            .retainDataDimensionReportingRates(ReportingRateMetric.REPORTING_RATE)
            .build();
    assertEquals(2, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().size());
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(rrA));
    assertTrue(params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems().contains(rrB));
  }

  @Test
  void testSetDimensionOptions() {
    List<DimensionalItemObject> itemsBefore =
        List.of(
            createIndicator('A', null),
            createIndicator('B', null),
            createIndicator('C', null),
            createIndicator('D', null));
    List<DimensionalItemObject> itemsAfter =
        List.of(createIndicator('A', null), createIndicator('B', null));
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .addDimension(
                new BaseDimensionalObject(
                    DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, null, itemsBefore))
            .build();
    assertEquals(itemsBefore, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems());

    params =
        DataQueryParams.newBuilder(params)
            .withDimensionOptions(DimensionalObject.DATA_X_DIM_ID, itemsAfter)
            .build();
    assertEquals(itemsAfter, params.getDimension(DimensionalObject.DATA_X_DIM_ID).getItems());
  }

  @Test
  void testGetDaysForAvgSumIntAggregation() {
    List<DimensionalItemObject> dataElements = List.of(deA, deB, deC);
    List<DimensionalItemObject> periods = List.of(peA, peB);
    DataQueryParams params =
        DataQueryParams.newBuilder().withDataElements(dataElements).withPeriods(periods).build();
    assertEquals(peA.getDaysInPeriod(), params.getDaysForAvgSumIntAggregation());

    params =
        DataQueryParams.newBuilder()
            .withDataElements(dataElements)
            .withFilterPeriods(periods)
            .build();
    int totalDays = peA.getDaysInPeriod() + peB.getDaysInPeriod();
    assertEquals(totalDays, params.getDaysForAvgSumIntAggregation());
  }

  @Test
  void testGetDimensionsAndFiltersByDimensionTypes() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElements(List.of(deA, deB, deC))
            .withPeriods(List.of(peA, peB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .build();
    List<DimensionalObject> dimensions =
        params.getDimensionsAndFilters(
            Set.of(DimensionType.PERIOD, DimensionType.ORGANISATION_UNIT));
    assertEquals(2, dimensions.size());
    assertTrue(dimensions.contains(new BaseDimensionalObject(PERIOD_DIM_ID)));
    assertTrue(dimensions.contains(new BaseDimensionalObject(ORGUNIT_DIM_ID)));
  }

  @Test
  void testGetLatestPeriod() {
    Period jan_2016 = PeriodType.getPeriodFromIsoString("201601");
    Period feb_2016 = PeriodType.getPeriodFromIsoString("201602");
    Period mar_2016 = PeriodType.getPeriodFromIsoString("201603");
    DataQueryParams paramsA =
        DataQueryParams.newBuilder()
            .withPeriods(List.of(jan_2016))
            .withFilterPeriods(List.of(feb_2016, mar_2016))
            .build();
    DataQueryParams paramsB =
        DataQueryParams.newBuilder()
            .withPeriods(List.of(mar_2016))
            .withFilterPeriods(List.of(jan_2016, feb_2016))
            .build();
    assertEquals(mar_2016, paramsA.getLatestPeriod());
    assertEquals(mar_2016, paramsB.getLatestPeriod());
  }

  @Test
  void testGetLatestEndDate() {
    Period q1_2016 = PeriodType.getPeriodFromIsoString("2016Q1");
    Period q2_2016 = PeriodType.getPeriodFromIsoString("2016Q2");
    Calendar today = Calendar.getInstance();
    DataQueryParams paramsA =
        DataQueryParams.newBuilder()
            .withEndDate(today.getTime())
            .withPeriods(List.of(q1_2016))
            .withFilterPeriods(List.of(q2_2016))
            .build();
    DataQueryParams paramsB =
        DataQueryParams.newBuilder().withEndDate(q1_2016.getEndDate()).build();
    DataQueryParams paramsC =
        DataQueryParams.newBuilder()
            .withFilterPeriods(List.of(q2_2016))
            .withPeriods(List.of(q1_2016))
            .build();
    assertEquals(today.getTime(), paramsA.getLatestEndDate());
    assertEquals(q1_2016.getEndDate(), paramsB.getLatestEndDate());
    assertEquals(q2_2016.getEndDate(), paramsC.getLatestEndDate());
  }

  @Test
  void testGetEarliestStartDate() {
    Period jan_2016 = PeriodType.getPeriodFromIsoString("201601");
    Period feb_2016 = PeriodType.getPeriodFromIsoString("201602");
    Period mar_2016 = PeriodType.getPeriodFromIsoString("201603");
    Date dec_2015 = getDate(2015, 12, 1);
    DataQueryParams paramsA =
        DataQueryParams.newBuilder()
            .withStartDate(dec_2015)
            .withPeriods(List.of(jan_2016))
            .withFilterPeriods(List.of(feb_2016, mar_2016))
            .build();
    DataQueryParams paramsB =
        DataQueryParams.newBuilder().withStartDate(jan_2016.getStartDate()).build();
    DataQueryParams paramsC =
        DataQueryParams.newBuilder()
            .withFilterPeriods(List.of(feb_2016, mar_2016))
            .withPeriods(List.of(jan_2016))
            .build();
    assertEquals(dec_2015, paramsA.getEarliestStartDate());
    assertEquals(jan_2016.getStartDate(), paramsB.getEarliestStartDate());
    assertEquals(jan_2016.getStartDate(), paramsC.getEarliestStartDate());
  }

  @Test
  void testSetPeriodDimensionWithoutOptionsA() {
    Period mar_2016 = PeriodType.getPeriodFromIsoString("201603");
    Period apr_2016 = PeriodType.getPeriodFromIsoString("201604");
    Period may_2016 = PeriodType.getPeriodFromIsoString("201605");
    DataQueryParams params =
        DataQueryParams.newBuilder().withPeriods(List.of(mar_2016, apr_2016, may_2016)).build();
    assertEquals(3, params.getPeriods().size());

    DataQueryParams query =
        DataQueryParams.newBuilder(params)
            .withEarliestStartDateLatestEndDate()
            .withPeriodDimensionWithoutOptions()
            .build();
    assertNotNull(query.getDimension(DimensionalObject.PERIOD_DIM_ID));
    assertEquals(0, query.getPeriods().size());
    assertEquals(getDate(2016, 3, 1), query.getStartDate());
    assertEquals(getDate(2016, 5, 31), query.getEndDate());
  }

  @Test
  void testSetPeriodDimensionWithoutOptionsB() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withStartDate(getDate(2017, 3, 1))
            .withEndDate(getDate(2017, 5, 31))
            .build();
    assertEquals(0, params.getPeriods().size());

    DataQueryParams query =
        DataQueryParams.newBuilder(params)
            .withEarliestStartDateLatestEndDate()
            .withPeriodDimensionWithoutOptions()
            .build();
    assertNotNull(query.getDimension(DimensionalObject.PERIOD_DIM_ID));
    assertEquals(0, query.getPeriods().size());
    assertEquals(getDate(2017, 3, 1), query.getStartDate());
    assertEquals(getDate(2017, 5, 31), query.getEndDate());
  }

  @Test
  void testGetAllTypedOrganisationUnits() {
    DataQueryParams paramsA =
        DataQueryParams.newBuilder().withOrganisationUnits(List.of(ouA, ouB)).build();
    DataQueryParams paramsB =
        DataQueryParams.newBuilder().withFilterOrganisationUnits(List.of(ouA, ouB)).build();
    assertEquals(2, paramsA.getAllTypedOrganisationUnits().size());
    assertEquals(2, paramsB.getAllTypedOrganisationUnits().size());
  }

  @Test
  void testGetDataElements() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(List.of(deA, deB, inA, inB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .withReportingRates(List.of(rrA, rrB, rrC))
            .build();
    List<DimensionalItemObject> expected = List.of(deA, deB);
    assertEquals(expected, params.getDataElements());
  }

  @Test
  void testGetDataElementsOperandsProgramDataElements() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(List.of(inA, deoA, deoB, rrA, pdeA, deC))
            .withOrganisationUnits(List.of(ouA, ouB))
            .withReportingRates(List.of(rrA, rrB, rrC))
            .build();
    List<DimensionalItemObject> expected = List.of(deA, deB, deC);
    assertContainsOnly(expected, params.getDataElementsOperandsProgramDataElements());
  }

  @Test
  void testGetAllDataSets() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withPeriods(List.of(peA, peB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .withReportingRates(List.of(rrA, rrB, rrC))
            .build();
    Set<DimensionalItemObject> expected = Set.of(dsA, dsB, dsC);
    assertEquals(expected, params.getAllDataSets());
  }

  @Test
  void testGetCategoryOptions() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withPeriods(List.of(peA, peB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .withCategory(caA)
            .build();
    Set<DimensionalItemObject> expected = Set.of(coA, coB);
    assertEquals(expected, params.getCategoryOptions());
  }

  @Test
  void testGetDataElementGroups() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDataElementGroupSet(degsA)
            .withPeriods(List.of(peA, peB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .build();
    List<DimensionalItemObject> expected = List.of(degA, degB);
    assertEquals(expected, params.getAllDataElementGroups());
  }

  @Test
  void testGetAllProgramsInAttributesAndDataElements() {
    ProgramTrackedEntityAttributeDimensionItem ptaA =
        new ProgramTrackedEntityAttributeDimensionItem(prA, atA);
    ProgramDataElementDimensionItem pdeA = new ProgramDataElementDimensionItem(prB, deA);
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withProgramAttributes(List.of(ptaA))
            .withProgramDataElements(List.of(pdeA))
            .withPeriods(List.of(peA, peB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .build();
    Set<Program> expected = Set.of(prA, prB);
    assertEquals(expected, params.getProgramsInAttributesAndDataElements());
  }

  @Test
  void testGetAggregationType() {
    DataQueryParams paramsA =
        DataQueryParams.newBuilder()
            .withAggregationType(AnalyticsAggregationType.fromAggregationType(AggregationType.LAST))
            .build();

    DataQueryParams paramsB =
        DataQueryParams.newBuilder()
            .withAggregationType(AnalyticsAggregationType.fromAggregationType(AggregationType.SUM))
            .build();

    assertTrue(paramsA.isLastPeriodAggregationType());
    assertTrue(paramsA.isFirstOrLastPeriodAggregationType());
    assertTrue(paramsA.isFirstOrLastOrLastInPeriodAggregationType());

    assertFalse(paramsB.isLastPeriodAggregationType());
    assertFalse(paramsB.isFirstOrLastPeriodAggregationType());
    assertFalse(paramsB.isFirstOrLastOrLastInPeriodAggregationType());
  }

  @Test
  void testGetKey() {
    DataQueryParams paramsA =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(List.of(deA, deB))
            .withOrganisationUnits(List.of(ouA, ouB))
            .withPeriods(List.of(peA))
            .withLocale(Locale.FRENCH)
            .build();
    DataQueryParams paramsB =
        DataQueryParams.newBuilder()
            .withDataDimensionItems(List.of(deA))
            .withOrganisationUnits(List.of(ouA))
            .withPeriods(List.of(peB))
            .withAggregationType(AnalyticsAggregationType.AVERAGE)
            .build();
    assertNotNull(paramsA.getKey());
    assertEquals(40, paramsA.getKey().length());
    assertNotNull(paramsB.getKey());
    assertEquals(40, paramsB.getKey().length());
    // No collision
    assertNotEquals(paramsA.getKey(), paramsB.getKey());
  }

  @Test
  void testFinancialYearPeriodResultsInTwoAggregationYears() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withPeriods(List.of(peC))
            .withDataPeriodType(PeriodType.getPeriodTypeFromIsoString("2017"))
            .build();
    ListMap<DimensionalItemObject, DimensionalItemObject> periodMap =
        params.getDataPeriodAggregationPeriodMap();
    assertThat(periodMap.entrySet(), hasSize(2));
    assertThat(
        periodMap.keySet(),
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            hasProperty("isoDate", Matchers.is("2017")),
            hasProperty("isoDate", Matchers.is("2018"))));
    assertThat(periodMap.allValues(), hasSize(2));
    assertThat(
        periodMap.allValues(),
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            hasProperty("isoDate", Matchers.is(peC.getIsoDate())),
            hasProperty("isoDate", Matchers.is(peC.getIsoDate()))));
  }
}
