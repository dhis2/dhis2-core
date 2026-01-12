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
package org.hisp.dhis.analytics.event;

import static org.hisp.dhis.analytics.TimeField.SCHEDULED_DATE;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.period.PeriodTypeEnum.MONTHLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.table.EventAnalyticsColumnName;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Lars Helge Overland
 */
class EventQueryParamsTest extends TestBase {
  private Option opA;

  private Option opB;

  private Option opC;

  private Option opD;

  private OptionSet osA;

  private OptionSet osB;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private DataElement deD;

  private DataElement deE;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private Program prA;

  private Program prB;

  private Program prC;

  private ProgramStage psA;

  private ProgramStage psB;

  private ProgramStage psC;

  private PeriodDimension peA;

  private PeriodDimension peB;

  private PeriodDimension peC;

  @BeforeEach
  void before() {
    opA = createOption('A');
    opB = createOption('B');
    opC = createOption('C');
    opD = createOption('D');
    osA = createOptionSet('A', opA, opB);
    osB = createOptionSet('B', opC, opD);
    deA = createDataElement('A');
    deB = createDataElement('B');
    deC = createDataElement('C');
    deC.setValueType(ValueType.DATE);
    deD = createDataElement('D');
    deD.setValueType(ValueType.ORGANISATION_UNIT);
    deE = createDataElement('E');
    deE.setValueType(ValueType.TEXT);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    psA = createProgramStage('A', prA);
    psB = createProgramStage('B', prB);
    psC = createProgramStage('B', prC);
    // Program Stage A
    psA.addDataElement(deA, 0);
    psA.addDataElement(deB, 1);
    psA.addDataElement(deC, 2);
    psA.addDataElement(deD, 3);
    // Program Stage B
    psB.addDataElement(deA, 0);
    psB.addDataElement(deB, 1);
    // Program Stage C
    psC.addDataElement(deA, 0);
    prA = createProgram('A', Set.of(psA), ouA);
    prB = createProgram('B', Set.of(psB), ouA);
    prC = createProgram('C', Set.of(psC), ouA);
    TrackedEntityAttribute teA = createTrackedEntityAttribute('A', ValueType.ORGANISATION_UNIT);
    teA.setUid(deD.getUid());
    ProgramTrackedEntityAttribute pteA = createProgramTrackedEntityAttribute(prC, teA);
    prC.setProgramAttributes(List.of(pteA));
    peA =
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2014, 4, 1, 0, 0).toDate()));
    peB =
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate()));
    peC =
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2014, 6, 1, 0, 0).toDate()));
  }

  @Test
  void testHasDimensionValue() {
    EventQueryParams paramsA =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA, ouB))
            .withValue(deA)
            .build();

    assertTrue(paramsA.hasValueDimension());
  }

  @Test
  void testHasNumericDimensionValue() {
    EventQueryParams paramsA =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA, ouB))
            .withValue(deA)
            .build();

    EventQueryParams paramsB =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA, ouB))
            .withValue(deC)
            .build();

    assertTrue(paramsA.hasNumericValueDimension());
    assertFalse(paramsB.hasNumericValueDimension());
  }

  @Test
  void testHasBooleanDimensionValue() {
    DataElement boolElement = createDataElement('A');
    boolElement.setValueType(BOOLEAN);

    DataElement notBoolElement = createDataElement('B');

    EventQueryParams paramsA =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA, ouB))
            .withValue(boolElement)
            .build();

    EventQueryParams paramsB =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA, ouB))
            .withValue(notBoolElement)
            .build();

    assertTrue(paramsA.hasBooleanValueDimension());
    assertFalse(paramsB.hasBooleanValueDimension());
  }

  @Test
  void testHasTextDimensionValue() {
    EventQueryParams paramsA =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA, ouB))
            .withValue(deE)
            .build();

    EventQueryParams paramsB =
        new EventQueryParams.Builder()
            .withOrganisationUnits(List.of(ouA, ouB))
            .withValue(deA)
            .build();

    assertTrue(paramsA.hasTextValueDimension());
    assertFalse(paramsB.hasTextValueDimension());
  }

  @Test
  void testGetKey() {
    QueryItem qiA = new QueryItem(deA, null, deA.getValueType(), deA.getAggregationType(), osA);
    QueryItem qiB = new QueryItem(deB, null, deB.getValueType(), deB.getAggregationType(), osB);
    EventQueryParams paramsA =
        new EventQueryParams.Builder()
            .withPeriods(List.of(peA, peB, peC), MONTHLY.getName())
            .withOrganisationUnits(List.of(ouA, ouB))
            .addItem(qiA)
            .addItem(qiB)
            .withLocale(Locale.FRENCH)
            .build();
    EventQueryParams paramsB =
        new EventQueryParams.Builder()
            .withPeriods(List.of(peA, peB), MONTHLY.getName())
            .withOrganisationUnits(List.of(ouA))
            .addItem(qiA)
            .addItem(qiB)
            .withGeometryOnly(true)
            .build();
    assertNotNull(paramsA.getKey());
    assertEquals(40, paramsA.getKey().length());
    assertNotNull(paramsB.getKey());
    assertEquals(40, paramsB.getKey().length());
    assertNotEquals(paramsA.getKey(), paramsB.getKey());
  }

  @Test
  void testWithStartEndDatesForPeriodsForScheduledMonthlyWithDateField() {
    // Given
    PeriodDimension periodMay = PeriodDimension.of("202305");
    periodMay.setDateField(SCHEDULED_DATE.name());

    PeriodDimension periodMarch = PeriodDimension.of("202303");
    periodMarch.setDateField(SCHEDULED_DATE.name());

    PeriodDimension periodFebruary = PeriodDimension.of("202302");
    periodFebruary.setDateField(SCHEDULED_DATE.name());

    // When
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withPeriods(List.of(periodMay, periodMarch, periodFebruary), MONTHLY.getName())
            .withStartEndDatesForPeriods()
            .build();

    // Then
    assertNull(params.getStartDate());
    assertNull(params.getEndDate());
    assertEquals(1, params.getTimeDateRanges().size());
    assertTrue(params.getTimeDateRanges().containsKey(SCHEDULED_DATE));

    List<DateRange> ranges = params.getTimeDateRanges().get(SCHEDULED_DATE);
    assertEquals(3, ranges.size());

    LocalDate febDate = toLocalDate(ranges.get(0).getStartDate());
    assertEquals(2023, febDate.getYear());
    assertEquals(2, febDate.getMonthValue());
    assertEquals(1, febDate.getDayOfMonth());

    LocalDate marchDate = toLocalDate(ranges.get(1).getStartDate());
    assertEquals(2023, marchDate.getYear());
    assertEquals(3, marchDate.getMonthValue());
    assertEquals(1, marchDate.getDayOfMonth());

    LocalDate mayDate = toLocalDate(ranges.get(2).getStartDate());
    assertEquals(2023, mayDate.getYear());
    assertEquals(5, mayDate.getMonthValue());
    assertEquals(1, mayDate.getDayOfMonth());
  }

  @Test
  void testReplacePeriodsWithDatesWithDifferentPeriodTypesWithDateField() {
    // Given
    PeriodDimension weeklyPeriod = PeriodDimension.of(Period.of("2023W5"));
    weeklyPeriod.setDateField(SCHEDULED_DATE.name());

    PeriodDimension monthlyPeriod = PeriodDimension.of("202303");
    monthlyPeriod.setDateField(SCHEDULED_DATE.name());

    PeriodDimension dailyPeriod = PeriodDimension.of(Period.of("20230105"));
    dailyPeriod.setDateField(SCHEDULED_DATE.name());

    List<PeriodDimension> periods = List.of(weeklyPeriod, monthlyPeriod, dailyPeriod);

    EventQueryParams params = new EventQueryParams.Builder().withStartEndDatesForPeriods().build();
    params.getDimensions().add(new BaseDimensionalObject("pe", PERIOD, periods));

    // When
    params.replacePeriodsWithDates();

    // Then
    assertNull(params.getStartDate());
    assertNull(params.getEndDate());
    assertEquals(1, params.getTimeDateRanges().size());
    assertTrue(params.getTimeDateRanges().containsKey(SCHEDULED_DATE));
    assertEquals(3, params.getTimeDateRanges().get(SCHEDULED_DATE).size());
  }

  @Test
  void testReplacePeriodsWithDatesWithDifferentPeriodTypesWithoutDateField() {
    // Given
    PeriodDimension weeklyPeriod = PeriodDimension.of(Period.of("2023W5"));
    PeriodDimension monthlyPeriod = PeriodDimension.of("202303");
    PeriodDimension dailyPeriod = PeriodDimension.of(Period.of("20230105"));

    List<PeriodDimension> periods = List.of(weeklyPeriod, monthlyPeriod, dailyPeriod);

    EventQueryParams params = new EventQueryParams.Builder().withStartEndDatesForPeriods().build();
    params.getDimensions().add(new BaseDimensionalObject("pe", PERIOD, periods));

    // When
    params.replacePeriodsWithDates();

    // Then
    assertNotNull(params.getStartDate());
    assertNotNull(params.getEndDate());
    assertEquals(0, params.getTimeDateRanges().size());
    assertFalse(params.getTimeDateRanges().containsKey(SCHEDULED_DATE));
    assertNull(params.getTimeDateRanges().get(SCHEDULED_DATE));
  }

  @Test
  void testHasContinuousRangeDateRangeIsFalseWithDateField() {
    // Given
    Period weeklyPeriod = Period.of("2023W5");
    DateRange weeklyRange = new DateRange(weeklyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    Period monthlyPeriod = Period.of("202303");
    DateRange monthlyRange = new DateRange(monthlyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    Period dailyPeriod = Period.of("20230105");
    DateRange dailyRange = new DateRange(dailyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    EventQueryParams params = new EventQueryParams.Builder().build();

    // When
    boolean hasContinuousRange =
        params.hasContinuousRange(List.of(weeklyRange, monthlyRange, dailyRange));

    // Then
    assertFalse(hasContinuousRange);
  }

  @Test
  void testHasContinuousRangeDateRangeIsFalse() {
    // Given
    Period weeklyPeriod = Period.of("2023W5");
    DateRange weeklyRange = new DateRange(weeklyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    Period monthlyPeriod = Period.of("202303");
    DateRange monthlyRange = new DateRange(monthlyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    Period dailyPeriod = Period.of("20230105");
    DateRange dailyRange = new DateRange(dailyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    EventQueryParams params = new EventQueryParams.Builder().build();

    // When
    boolean hasContinuousRange =
        params.hasContinuousRange(List.of(weeklyRange, monthlyRange, dailyRange));

    // Then
    assertFalse(hasContinuousRange);
  }

  @Test
  void testHasContinuousRangeDateRangeIsTrue() {
    // Given
    Period jan = Period.of("202301");
    DateRange janRange = new DateRange(jan.getStartDate(), jan.getEndDate());

    Period feb = Period.of("202302");
    DateRange febRange = new DateRange(feb.getStartDate(), feb.getEndDate());

    Period mar = Period.of("20230103");
    DateRange marRange = new DateRange(mar.getStartDate(), mar.getEndDate());

    EventQueryParams params = new EventQueryParams.Builder().build();

    // When
    boolean hasContinuousRange = params.hasContinuousRange(List.of(janRange, febRange, marRange));

    // Then
    assertTrue(hasContinuousRange);
  }

  @Test
  void testHasContinuousRangeDateRangeForThisWeeklyAndDailyPeriods() {
    // Given
    Period weeklyPeriod =
        new WeeklyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate());
    DateRange weeklyRange = new DateRange(weeklyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    Period todayPeriod =
        new DailyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate());
    DateRange todayRange = new DateRange(todayPeriod.getStartDate(), todayPeriod.getEndDate());

    EventQueryParams params = new EventQueryParams.Builder().build();

    // When
    boolean hasContinuousRange = params.hasContinuousRange(List.of(weeklyRange, todayRange));

    // Then
    assertTrue(hasContinuousRange);
  }

  @Test
  void testHasContinuousRangeDateRangeForThisWeeklyDailyAndMonthlyPeriods() {
    // Given
    Period weeklyPeriod =
        new WeeklyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate());
    DateRange weeklyRange = new DateRange(weeklyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    Period todayPeriod =
        new DailyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate());
    DateRange todayRange = new DateRange(todayPeriod.getStartDate(), todayPeriod.getEndDate());

    Period monthlyPeriod =
        new MonthlyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate());
    DateRange monthlyRange =
        new DateRange(monthlyPeriod.getStartDate(), monthlyPeriod.getEndDate());

    EventQueryParams params = new EventQueryParams.Builder().build();

    // When
    boolean hasContinuousRange =
        params.hasContinuousRange(List.of(weeklyRange, todayRange, monthlyRange));

    // Then
    assertTrue(hasContinuousRange);
  }

  @Test
  void testHasContinuousRangeDateRangeForWeeklyDailyAndMonthlyIsFalse() {
    // Given
    Period monthlyPeriod =
        new MonthlyPeriodType().createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate());
    DateRange monthlyRange =
        new DateRange(monthlyPeriod.getStartDate(), monthlyPeriod.getEndDate());

    Period weeklyPeriod =
        new WeeklyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate());
    DateRange weeklyRange = new DateRange(weeklyPeriod.getStartDate(), weeklyPeriod.getEndDate());

    Period todayPeriod =
        new DailyPeriodType().createPeriod(new DateTime(2014, 5, 1, 0, 0).toDate());
    DateRange todayRange = new DateRange(todayPeriod.getStartDate(), todayPeriod.getEndDate());

    EventQueryParams params = new EventQueryParams.Builder().build();

    // When
    boolean hasContinuousRange =
        params.hasContinuousRange(List.of(monthlyRange, weeklyRange, todayRange));

    // Then
    assertFalse(hasContinuousRange);
  }

  @Test
  void testGetItemLegends() {
    Legend leA = createLegend('A', 0d, 1d);
    Legend leB = createLegend('B', 1d, 2d);
    LegendSet lsA = createLegendSet('A', leA, leB);
    QueryItem qiA = new QueryItem(deA, lsA, deA.getValueType(), deA.getAggregationType(), null);
    EventQueryParams params = new EventQueryParams.Builder().addItem(qiA).build();
    Set<Legend> expected = Set.of(leA, leB);
    assertEquals(expected, params.getItemLegends());
  }

  @Test
  void testGetItemOptions() {
    QueryItem qiA = new QueryItem(deA, null, deA.getValueType(), deA.getAggregationType(), osA);
    QueryItem qiB = new QueryItem(deB, null, deB.getValueType(), deB.getAggregationType(), osB);
    EventQueryParams params = new EventQueryParams.Builder().addItem(qiA).addItem(qiB).build();
    Set<Option> expected = Set.of(opA, opB, opC, opD);
    assertEquals(expected, params.getItemOptions());
  }

  @Test
  void testGetDuplicateQueryItems() {
    QueryItem iA = new QueryItem(createDataElement('A', new CategoryCombo()));
    QueryItem iB = new QueryItem(createDataElement('B', new CategoryCombo()));
    QueryItem iC = new QueryItem(createDataElement('B', new CategoryCombo()));
    QueryItem iD = new QueryItem(createDataElement('D', new CategoryCombo()));
    EventQueryParams params =
        new EventQueryParams.Builder().addItem(iA).addItem(iB).addItem(iC).addItem(iD).build();
    List<QueryItem> duplicates = params.getDuplicateQueryItems();
    assertEquals(1, duplicates.size());
    assertTrue(duplicates.contains(iC));
  }

  @Test
  void testIsTimeFieldValid() {
    QueryItem iA = new QueryItem(createDataElement('A', new CategoryCombo()));
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withTimeField(deC.getUid())
            .addItem(iA)
            .build();
    assertTrue(params.timeFieldIsValid());
    params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withTimeField(SCHEDULED_DATE.name())
            .addItem(iA)
            .build();
    assertTrue(params.timeFieldIsValid());
    params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withTimeField("someInvalidTimeField")
            .addItem(iA)
            .build();
    assertFalse(params.timeFieldIsValid());
  }

  @Test
  void testIsOrgUnitFieldValid() {
    QueryItem iA = new QueryItem(createDataElement('A', new CategoryCombo()));
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withOrgUnitField(new OrgUnitField(deD.getUid()))
            .addItem(iA)
            .build();
    assertTrue(params.orgUnitFieldIsValid());
    params =
        new EventQueryParams.Builder()
            .withProgram(prA)
            .withOrgUnitField(new OrgUnitField("someInvalidOrgUnitField"))
            .addItem(iA)
            .build();
    assertFalse(params.orgUnitFieldIsValid());
  }

  @Test
  void testIsOrgUnitFieldValidWithOneProgramIndicator() {
    QueryItem iA = new QueryItem(createDataElement('A', new CategoryCombo()));
    ProgramIndicator programIndicatorA = createProgramIndicator('A', prA, "", "");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(null)
            .withOrgUnitField(new OrgUnitField(deD.getUid()))
            .addItem(iA)
            .addItemProgramIndicator(programIndicatorA)
            .build();
    assertTrue(params.orgUnitFieldIsValid());
  }

  @Test
  void testIsOrgUnitFieldValidWithMultipleProgramIndicator() {
    QueryItem iA = new QueryItem(createDataElement('A', new CategoryCombo()));
    ProgramIndicator programIndicatorA = createProgramIndicator('A', prA, "", "");
    // This PI has 0 Data Element of type OrgUnit -> test should fail.
    ProgramIndicator programIndicatorB = createProgramIndicator('B', prB, "", "");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(null)
            .withOrgUnitField(new OrgUnitField(deD.getUid()))
            .addItem(iA)
            .addItemProgramIndicator(programIndicatorA)
            .addItemProgramIndicator(programIndicatorB)
            .build();
    assertFalse(params.orgUnitFieldIsValid());
  }

  @Test
  void testIsOrgUnitFieldValidWithMultipleProgramIndicator2() {
    QueryItem iA = new QueryItem(createDataElement('A', new CategoryCombo()));
    // This PI has a Program that has a Tracked Entity Attribute of type Org Unit.
    ProgramIndicator programIndicatorA = createProgramIndicator('A', prC, "", "");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(null)
            .withOrgUnitField(new OrgUnitField(deD.getUid()))
            .addItem(iA)
            .addItemProgramIndicator(programIndicatorA)
            .build();
    assertTrue(params.orgUnitFieldIsValid());
  }

  @ParameterizedTest
  @CsvSource({"AGGREGATE, ENROLLMENT, true", "QUERY, ENROLLMENT, false", "AGGREGATE, EVENT, false"})
  void testIsAggregatedEnrollmentsShouldBeTrue(
      RequestTypeAware.EndpointAction endpointAction,
      RequestTypeAware.EndpointItem endpointItem,
      boolean expected) {
    // Given
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withEndpointAction(endpointAction)
            .withEndpointItem(endpointItem)
            .build();
    // When
    // Then
    assertEquals(expected, params.isAggregatedEnrollments());
  }

  @Test
  void testStageDateItemQueryItem_OccurredDate() {
    // Test that a QueryItem for occurreddate is created correctly
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);
    qi.addFilter(new QueryFilter(QueryOperator.GE, "2019-01-01"));
    qi.addFilter(new QueryFilter(QueryOperator.LE, "2019-12-31"));

    assertEquals(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME, qi.getItemId());
    assertEquals(ValueType.DATE, qi.getValueType());
    assertEquals(psA, qi.getProgramStage());
    assertEquals(2, qi.getFilters().size());
  }

  @Test
  void testStageDateItemQueryItem_ScheduledDate() {
    // Test that a QueryItem for scheduleddate is created correctly
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);
    qi.addFilter(new QueryFilter(QueryOperator.GE, "2020-01-01"));
    qi.addFilter(new QueryFilter(QueryOperator.LE, "2020-06-30"));

    assertEquals(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME, qi.getItemId());
    assertEquals(ValueType.DATE, qi.getValueType());
    assertEquals(psA, qi.getProgramStage());
    assertEquals(2, qi.getFilters().size());
  }

  @Test
  void testStageDateItemIdentification() {
    // Test that stage date items are identifiable by their itemId
    BaseDimensionalItemObject occurredDateItem =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    BaseDimensionalItemObject scheduledDateItem =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    BaseDimensionalItemObject otherItem = new BaseDimensionalItemObject("someOtherItem");

    QueryItem qiOccurred = new QueryItem(occurredDateItem);
    QueryItem qiScheduled = new QueryItem(scheduledDateItem);
    QueryItem qiOther = new QueryItem(otherItem);

    // Test the logic by checking getItemId matches expected column names
    assertEquals(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME, qiOccurred.getItemId());
    assertEquals(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME, qiScheduled.getItemId());
    assertNotEquals(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME, qiOther.getItemId());
  }

  @Test
  void testHasOrganisationUnitsWithStandardOrgUnit() {
    // Test that hasOrganisationUnits returns true when standard org unit dimension is present
    EventQueryParams params =
        new EventQueryParams.Builder().withOrganisationUnits(List.of(ouA, ouB)).build();

    assertTrue(params.hasOrganisationUnits());
  }

  @Test
  void testHasOrganisationUnitsWithStageSpecificOrgUnit() {
    // Test that hasOrganisationUnits returns true when stage-specific org unit QueryItem is present
    BaseDimensionalItemObject ouItem =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME);
    QueryItem stageOuItem =
        new QueryItem(ouItem, prA, null, ValueType.ORGANISATION_UNIT, AggregationType.NONE, null);
    stageOuItem.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(stageOuItem).build();

    assertTrue(params.hasOrganisationUnits());
  }

  @Test
  void testHasOrganisationUnitsWithStageSpecificOrgUnitAsFilter() {
    // Test that hasOrganisationUnits returns true when stage-specific org unit is in itemFilters
    BaseDimensionalItemObject ouItem =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME);
    QueryItem stageOuItem =
        new QueryItem(ouItem, prA, null, ValueType.ORGANISATION_UNIT, AggregationType.NONE, null);
    stageOuItem.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItemFilter(stageOuItem).build();

    assertTrue(params.hasOrganisationUnits());
  }

  @Test
  void testHasOrganisationUnitsWithNoOrgUnit() {
    // Test that hasOrganisationUnits returns false when no org unit is present
    EventQueryParams params = new EventQueryParams.Builder().build();

    assertFalse(params.hasOrganisationUnits());
  }

  @Test
  void testHasStageSpecificItemWithEventDateItem() {
    // Given: a QueryItem for event date with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return true for stage-specific item
    assertTrue(params.hasStageSpecificItem());
  }

  @Test
  void testHasStageSpecificItemWithScheduledDateItem() {
    // Given: a QueryItem for scheduled date with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return true for stage-specific item
    assertTrue(params.hasStageSpecificItem());
  }

  @Test
  void testHasStageSpecificItemWithOuItem() {
    // Given: a QueryItem for organisation unit with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OU_COLUMN_NAME);
    QueryItem qi =
        new QueryItem(item, prA, null, ValueType.ORGANISATION_UNIT, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return true for stage-specific item
    assertTrue(params.hasStageSpecificItem());
  }

  @Test
  void testHasStageSpecificItemWithEventStatusItem() {
    // Given: a QueryItem for event status with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.TEXT, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return true for stage-specific item
    assertTrue(params.hasStageSpecificItem());
  }

  @Test
  void testHasStageSpecificItemReturnsFalseWhenNoStageSpecificItems() {
    // Given: a QueryItem without a program stage
    QueryItem qi = new QueryItem(deA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return false
    assertFalse(params.hasStageSpecificItem());
  }

  @Test
  void testHasStageSpecificItemWithItemFilter() {
    // Given: a QueryItem filter with program stage and stage-specific dimension
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItemFilter(qi).build();

    // Then: should return true as itemFilters are also checked
    assertTrue(params.hasStageSpecificItem());
  }

  @Test
  void testHasStageSpecificItemWithRegularDataElementWithStage() {
    // Given: a regular data element with program stage (not a stage-specific dimension)
    QueryItem qi = new QueryItem(deA);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return false as data element is not a stage-specific dimension
    assertFalse(params.hasStageSpecificItem());
  }

  @Test
  void testHasStageDateItemWithOccurredDateItem() {
    // Given: a QueryItem for occurred date with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return true
    assertTrue(params.hasStageDateItem());
  }

  @Test
  void testHasStageDateItemWithScheduledDateItem() {
    // Given: a QueryItem for scheduled date with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return true
    assertTrue(params.hasStageDateItem());
  }

  @Test
  void testHasStageDateItemReturnsFalseWhenNoStageDateItems() {
    // Given: a QueryItem for event status (not a date item) with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.EVENT_STATUS_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.TEXT, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return false as event status is not a date item
    assertFalse(params.hasStageDateItem());
  }

  @Test
  void testHasStageDateItemWithDateItemWithoutProgramStage() {
    // Given: a QueryItem for occurred date without program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi).build();

    // Then: should return false as no program stage is set
    assertFalse(params.hasStageDateItem());
  }

  @Test
  void testHasStageDateItemWithItemFilter() {
    // Given: a QueryItem filter for occurred date with program stage
    BaseDimensionalItemObject item =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi = new QueryItem(item, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItemFilter(qi).build();

    // Then: should return true as itemFilters are also checked
    assertTrue(params.hasStageDateItem());
  }

  @Test
  void testHasStageDateItemWithBothDateTypes() {
    // Given: QueryItems for both occurred date and scheduled date
    BaseDimensionalItemObject occurredItem =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qiOccurred =
        new QueryItem(occurredItem, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qiOccurred.setProgramStage(psA);

    BaseDimensionalItemObject scheduledItem =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    QueryItem qiScheduled =
        new QueryItem(scheduledItem, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qiScheduled.setProgramStage(psB);

    EventQueryParams params =
        new EventQueryParams.Builder().addItem(qiOccurred).addItem(qiScheduled).build();

    // Then: should return true
    assertTrue(params.hasStageDateItem());
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithDuplicates() {
    // Given: two QueryItems with the same stage and item identifier
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi1).addItem(qi2).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return the duplicate identifier
    assertEquals(1, duplicates.size());
    assertTrue(
        duplicates.contains(
            psA.getUid() + "." + EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithNoDuplicates() {
    // Given: QueryItems with different stage/item combinations
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi1).addItem(qi2).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return empty set
    assertTrue(duplicates.isEmpty());
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithItemsAndFilters() {
    // Given: items and filters with same stage/item combination
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);

    EventQueryParams params =
        new EventQueryParams.Builder().addItem(qi1).addItemFilter(qi2).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return duplicate as both items and filters are checked
    assertEquals(1, duplicates.size());
    assertTrue(
        duplicates.contains(
            psA.getUid() + "." + EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersIgnoresItemsWithoutProgramStage() {
    // Given: QueryItems without program stage
    QueryItem qi1 = new QueryItem(deA);
    QueryItem qi2 = new QueryItem(deA);

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi1).addItem(qi2).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return empty set as items without program stage are ignored
    assertTrue(duplicates.isEmpty());
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithMultipleDuplicates() {
    // Given: multiple duplicate combinations
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);

    BaseDimensionalItemObject item3 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    QueryItem qi3 = new QueryItem(item3, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi3.setProgramStage(psA);

    BaseDimensionalItemObject item4 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME);
    QueryItem qi4 = new QueryItem(item4, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi4.setProgramStage(psA);

    EventQueryParams params =
        new EventQueryParams.Builder().addItem(qi1).addItem(qi2).addItem(qi3).addItem(qi4).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return both duplicate identifiers
    assertEquals(2, duplicates.size());
    assertTrue(
        duplicates.contains(
            psA.getUid() + "." + EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME));
    assertTrue(
        duplicates.contains(
            psA.getUid() + "." + EventAnalyticsColumnName.SCHEDULED_DATE_COLUMN_NAME));
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithDifferentOffsets() {
    // Given: two QueryItems with same stage/item but different offsets (0 and -1)
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);
    qi1.setRepeatableStageParams(RepeatableStageParams.of(0)); // offset [0]

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);
    qi2.setRepeatableStageParams(RepeatableStageParams.of(-1)); // offset [-1]

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi1).addItem(qi2).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return empty set as offsets are different
    assertTrue(duplicates.isEmpty());
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithSameOffsets() {
    // Given: two QueryItems with same stage/item and same offset
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);
    qi1.setRepeatableStageParams(RepeatableStageParams.of(0)); // offset [0]

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);
    qi2.setRepeatableStageParams(RepeatableStageParams.of(0)); // offset [0]

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi1).addItem(qi2).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return duplicate as stage/item/offset combination is the same
    assertEquals(1, duplicates.size());
    assertTrue(
        duplicates.contains(
            psA.getUid() + "." + EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME + ".0"));
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithOffsetAndNoOffset() {
    // Given: one QueryItem with offset [0] and one without offset (default)
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);
    qi1.setRepeatableStageParams(RepeatableStageParams.of(0)); // offset [0]

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);
    // No repeatableStageParams set - default

    EventQueryParams params = new EventQueryParams.Builder().addItem(qi1).addItem(qi2).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return empty set as one has explicit offset [0] and one is default
    assertTrue(duplicates.isEmpty());
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithMultipleDifferentOffsets() {
    // Given: multiple QueryItems with different offsets
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);
    qi1.setRepeatableStageParams(RepeatableStageParams.of(0));

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);
    qi2.setRepeatableStageParams(RepeatableStageParams.of(-1));

    BaseDimensionalItemObject item3 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi3 = new QueryItem(item3, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi3.setProgramStage(psA);
    qi3.setRepeatableStageParams(RepeatableStageParams.of(1));

    EventQueryParams params =
        new EventQueryParams.Builder().addItem(qi1).addItem(qi2).addItem(qi3).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return empty set as all offsets are different
    assertTrue(duplicates.isEmpty());
  }

  @Test
  void testGetDuplicateStageDimensionIdentifiersWithMixedOffsets() {
    // Given: multiple QueryItems with mixed offsets
    BaseDimensionalItemObject item1 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi1 = new QueryItem(item1, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi1.setProgramStage(psA);
    qi1.setRepeatableStageParams(RepeatableStageParams.of(0));

    BaseDimensionalItemObject item2 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi2 = new QueryItem(item2, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi2.setProgramStage(psA);
    qi2.setRepeatableStageParams(RepeatableStageParams.of(-1));

    BaseDimensionalItemObject item3 =
        new BaseDimensionalItemObject(EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME);
    QueryItem qi3 = new QueryItem(item3, prA, null, ValueType.DATE, AggregationType.NONE, null);
    qi3.setProgramStage(psA);
    qi3.setRepeatableStageParams(RepeatableStageParams.of(0)); // same offset as qi1

    EventQueryParams params =
        new EventQueryParams.Builder().addItem(qi1).addItem(qi2).addItem(qi3).build();

    // When: getting duplicate stage dimension identifiers
    Set<String> duplicates = params.getDuplicateStageDimensionIdentifiers();

    // Then: should return duplicate for offset [0] only
    assertEquals(1, duplicates.size());
    assertTrue(
        duplicates.contains(
            psA.getUid() + "." + EventAnalyticsColumnName.OCCURRED_DATE_COLUMN_NAME + ".0"));
  }
}
