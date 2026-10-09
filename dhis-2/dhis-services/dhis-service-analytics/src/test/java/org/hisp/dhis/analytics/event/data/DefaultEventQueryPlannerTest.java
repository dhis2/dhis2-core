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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.stage.DefaultStageQueryItemClassifier;
import org.hisp.dhis.analytics.event.data.stage.StageQueryItemClassifier;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RequestTypeAware.EndpointItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.user.User;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultEventQueryPlannerTest extends TestBase {

  @Mock private QueryPlanner queryPlanner;

  @Mock private PartitionManager partitionManager;

  @Spy
  private StageQueryItemClassifier stageQueryItemClassifier = new DefaultStageQueryItemClassifier();

  @InjectMocks private DefaultEventQueryPlanner eventQueryPlanner;

  private Program program;
  private DataElement dataElementA;

  private ProgramIndicator programIndicator;
  private OrganisationUnit orgUnitA;
  private PeriodDimension periodA;
  private PeriodDimension periodB;
  private User currentUser;
  private QueryItem queryItemA;
  private QueryItem queryItemB;

  @BeforeEach
  void setUp() {
    program = createProgram('A');

    dataElementA = createDataElement('A', new CategoryCombo());
    DataElement dataElementB = createDataElement('B', new CategoryCombo());

    programIndicator = createProgramIndicator('I', program, "expression", "filter");
    programIndicator.setAggregationType(AggregationType.COUNT);

    orgUnitA = createOrganisationUnit('A');

    periodA =
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2023, 1, 1, 0, 0).toDate()));
    periodB =
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2023, 2, 1, 0, 0).toDate()));

    currentUser = makeUser("U");

    queryItemA = new QueryItem(dataElementA);
    queryItemA.setProgram(program);
    queryItemA.setAggregationType(AggregationType.SUM);

    queryItemB = new QueryItem(dataElementB);
    queryItemB.setProgram(program);
    queryItemB.setAggregationType(AggregationType.SUM);
  }

  @Test
  void testPlanEventQuery_basicScenario() {
    EventQueryParams params = createBasicEventQueryParams();
    mockPartitionManager();

    EventQueryParams result = eventQueryPlanner.planEventQuery(params);

    assertNotNull(result);
    assertNotNull(result.getTableName());
    assertNotNull(result.getPartitions());
    assertFalse(result.isMultipleQueries());
  }

  @Test
  void testPlanEventQuery_withUser() {
    EventQueryParams params = createBasicEventQueryParams();

    // Manually set user via reflection
    setUser(params, "currentUser");

    mockPartitionManager();

    EventQueryParams result = eventQueryPlanner.planEventQuery(params);

    assertNotNull(result);
    verify(partitionManager).filterNonExistingPartitions(any(Partitions.class), anyString());
  }

  @Test
  void testPlanEventQuery_withoutUser() {
    EventQueryParams params = createBasicEventQueryParams();
    mockPartitionManager();

    EventQueryParams result = eventQueryPlanner.planEventQuery(params);

    assertNotNull(result);
    verify(partitionManager, never()).filterNonExistingPartitions(any(), any());
  }

  @Test
  void testPlanEventQuery_withStartEndDate() {
    Date startDate = new DateTime(2023, 1, 1, 0, 0).toDate();
    Date endDate = new DateTime(2023, 12, 31, 23, 59).toDate();

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .withStartDate(startDate)
            .withEndDate(endDate)
            .build();

    mockPartitionManager();

    EventQueryParams result = eventQueryPlanner.planEventQuery(params);

    assertNotNull(result);
    assertNotNull(result.getTableName());
    assertEquals(startDate, result.getStartDate());
    assertEquals(endDate, result.getEndDate());
  }

  // -------------------------------------------------------------------------
  // Tests for planEnrollmentQuery method
  // -------------------------------------------------------------------------

  @Test
  void testPlanEnrollmentQuery_basicScenario() {
    EventQueryParams params = createBasicEventQueryParams();

    EventQueryParams result = eventQueryPlanner.planEnrollmentQuery(params);

    assertNotNull(result);
    assertNotNull(result.getTableName());
    assertTrue(result.getTableName().contains("enrollment"));
  }

  @Test
  void testPlanEnrollmentQuery_preservesOriginalParams() {
    // Create EventQueryParams with user by copying from DataQueryParams that has user
    DataQueryParams dataParams =
        DataQueryParams.newBuilder()
            .withCurrentUser(currentUser)
            .withPeriods(List.of(periodA))
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    EventQueryParams params =
        new EventQueryParams.Builder(dataParams).withProgram(program).addItem(queryItemA).build();

    EventQueryParams result = eventQueryPlanner.planEnrollmentQuery(params);

    assertNotNull(result);
    assertEquals(params.getProgram(), result.getProgram());
    assertEquals(params.getItems(), result.getItems());
    assertEquals(params.getPeriods(), result.getPeriods());
    assertEquals(params.getOrganisationUnits(), result.getOrganisationUnits());
    assertEquals(params.getCurrentUser(), result.getCurrentUser());
    assertTrue(result.getTableName().contains("enrollment"));
  }

  // -------------------------------------------------------------------------
  // Tests for planAggregateQuery
  // -------------------------------------------------------------------------

  @Test
  void testPlanAggregateQuery_basicScenario() {
    EventQueryParams params = createBasicEventQueryParams();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    verify(queryPlanner).groupByOrgUnitLevel(any(DataQueryParams.class));
    verify(queryPlanner).groupByPeriodType(any(DataQueryParams.class));
  }

  @Test
  void testPlanAggregateQuery_withAggregateData() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .addItem(queryItemA)
            .addItem(queryItemB)
            .withAggregateData(true)
            .withPeriods(List.of(periodA), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should create separate queries for each item when aggregating
    assertThat(result.size(), greaterThanOrEqualTo(2));
    assertTrue(result.stream().allMatch(q -> q.getTableName() != null));
    assertTrue(result.stream().allMatch(q -> q.getPartitions() != null));
  }

  @Test
  void testPlanAggregateQuery_withProgramIndicators() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .addItemProgramIndicator(programIndicator)
            .withAggregateData(true)
            .withPeriods(List.of(periodA), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    assertFalse(result.isEmpty());
    assertTrue(result.stream().anyMatch(q -> q.getProgramIndicator() != null));
    assertTrue(result.stream().allMatch(q -> q.getTableName() != null));
  }

  @Test
  void testPlanAggregateQuery_withCollapseDataDimensions() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .addItem(queryItemA)
            .addItem(queryItemB)
            .withCollapseDataDimensions(true)
            .withPeriods(List.of(periodA), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should create separate queries for each item when collapsing
    assertThat(result.size(), greaterThanOrEqualTo(2));
    assertTrue(result.stream().noneMatch(q -> q.getItems().isEmpty()));
  }

  @Test
  void testPlanAggregateQuery_withFirstLastAggregationType() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .addItem(queryItemA)
            .withAggregationType(AnalyticsAggregationType.LAST)
            .withPeriods(List.of(periodA, periodB), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should create separate queries for each period
    assertThat(result.size(), greaterThanOrEqualTo(2));
    assertTrue(result.stream().allMatch(q -> q.getPeriods().size() == 1));
  }

  @Test
  @Disabled("Disabled until bug is fixed: NPE when item aggregation type is null")
  void testPlanAggregateQuery_bugNullAggregationType() {
    QueryItem nullAggTypeItem = new QueryItem(dataElementA);
    nullAggTypeItem.setProgram(program);

    EventQueryParams params =
        new EventQueryParams.Builder()
            .addItem(nullAggTypeItem)
            .withAggregateData(true)
            .withPeriods(List.of(periodA), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();
    eventQueryPlanner.planAggregateQuery(params);
  }

  @Test
  @Disabled("Disabled until bug is fixed: NPE when program is null")
  void testPlanEventQuery_bugNullProgram() {
    // Test the real bug: NPE when program is null
    EventQueryParams params =
        new EventQueryParams.Builder().withPeriods(List.of(periodA), "monthly").build();

    mockPartitionManager();
    eventQueryPlanner.planEventQuery(params);
  }

  @Test
  @Disabled("Disabled until bug is fixed: NPE when program is null in enrollment query")
  void testPlanEnrollmentQuery_bugNullProgram() {
    // Test the real bug: NPE when program is null in enrollment query
    EventQueryParams params =
        new EventQueryParams.Builder().withPeriods(List.of(periodA), "monthly").build();
    eventQueryPlanner.planEnrollmentQuery(params);
  }

  @Test
  void testPlanAggregateQuery_emptyItemsAggregateData() {
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .withAggregateData(true)
            .withPeriods(List.of(periodA), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should handle empty items when aggregate data is true
    assertNotNull(result);
    // Empty items with aggregate data should return empty list (nothing to aggregate)
    assertTrue(result.isEmpty());
  }

  @Test
  void testPlanAggregateQuery_itemsWithoutProgram() {
    QueryItem itemWithoutProgram = new QueryItem(dataElementA);
    itemWithoutProgram.setAggregationType(AggregationType.SUM); // Avoid NPE
    // -> not setting program on item

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .addItem(itemWithoutProgram)
            .withAggregateData(true)
            .withPeriods(List.of(periodA), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should handle items without program set
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void testPlanAggregateQuery_periodsWithDifferentTypesLastAggregation() {
    // Mix different period types with last aggregation
    PeriodDimension dailyPeriod =
        PeriodDimension.of(
            new DailyPeriodType().createPeriod(new DateTime(2023, 1, 1, 0, 0).toDate()));
    PeriodDimension yearlyPeriod =
        PeriodDimension.of(
            new YearlyPeriodType().createPeriod(new DateTime(2023, 1, 1, 0, 0).toDate()));

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .addItem(queryItemA)
            .withAggregationType(AnalyticsAggregationType.LAST)
            .withPeriods(List.of(dailyPeriod, yearlyPeriod), "mixed")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should handle mixed period types and create separate queries
    assertNotNull(result);
    assertThat(result.size(), greaterThanOrEqualTo(2));
  }

  @ParameterizedTest
  @EnumSource(
      value = AggregationType.class,
      names = {
        "FIRST", "FIRST_AVERAGE_ORG_UNIT", "FIRST_FIRST_ORG_UNIT",
        "LAST", "LAST_AVERAGE_ORG_UNIT", "LAST_LAST_ORG_UNIT"
      })
  void shouldPlanEachStageDatePeriodWithoutChangingTheRequest(AggregationType aggregationType) {
    ProgramStage stage = createProgramStage('A', program);
    QueryItem dateItem = createStageEventDateItem(stage, "202601", "202602");
    dateItem.addFilter(new QueryFilter(QueryOperator.GE, "2026-01-01"));
    dateItem.addFilter(new QueryFilter(QueryOperator.LE, "2026-02-28"));
    EventQueryParams params = createStageDateAggregateParams(stage, dateItem, aggregationType);
    mockQueryPlannerToReturnSameEventParams();

    List<EventQueryParams> queries = eventQueryPlanner.planAggregateQuery(params);

    assertEquals(2, queries.size());
    assertEquals(
        List.of("202601", "202602"),
        queries.stream().map(q -> q.getFirstOrLastStagePeriod().period().getIsoDate()).toList());
    assertTrue(queries.stream().allMatch(EventQueryParams::isMultipleQueries));
    assertTrue(queries.stream().allMatch(EventQueryParams::isSkipPartitioning));
    assertNull(params.getFirstOrLastStagePeriod());
    assertNull(params.getStartDate());
    assertNull(params.getEndDate());
    assertEquals(List.of("202601", "202602"), dateItem.getDimensionValues());
    assertEquals(2, dateItem.getFilters().size());
  }

  @ParameterizedTest
  @EnumSource(
      value = AggregationType.class,
      names = {"SUM", "AVERAGE", "COUNT", "LAST_IN_PERIOD", "LAST_IN_PERIOD_AVERAGE_ORG_UNIT"})
  void shouldKeepStagePeriodFilteringForOtherAggregations(AggregationType aggregationType) {
    ProgramStage stage = createProgramStage('A', program);
    QueryItem item = createStageEventDateItem(stage, "202601", "202602");
    EventQueryParams params = createStageDateAggregateParams(stage, item, aggregationType);
    mockQueryPlannerToReturnSameEventParams();

    List<EventQueryParams> queries = eventQueryPlanner.planAggregateQuery(params);

    assertEquals(1, queries.size());
    assertNull(queries.get(0).getFirstOrLastStagePeriod());
    assertEquals(
        List.of("202601", "202602"), queries.get(0).getItems().get(0).getDimensionValues());
  }

  // -------------------------------------------------------------------------
  // Helper methods
  // -------------------------------------------------------------------------

  private QueryItem createStageEventDateItem(ProgramStage stage, String... periods) {
    QueryItem item =
        new QueryItem(
            new BaseDimensionalItemObject("occurreddate"),
            program,
            null,
            ValueType.DATE,
            AggregationType.NONE,
            null);
    item.setProgramStage(stage);
    for (String period : periods) {
      item.addDimensionValue(period);
    }
    return item;
  }

  private EventQueryParams createStageDateAggregateParams(
      ProgramStage stage, QueryItem stageDateItem, AggregationType aggregationType) {
    return new EventQueryParams.Builder()
        .withProgram(program)
        .withProgramStage(stage)
        .withValue(dataElementA)
        .withEndpointItem(EndpointItem.EVENT)
        .addItem(stageDateItem)
        .withAggregationType(AnalyticsAggregationType.fromAggregationType(aggregationType))
        .withOrganisationUnits(List.of(orgUnitA))
        .build();
  }

  private EventQueryParams createBasicEventQueryParams() {
    return new EventQueryParams.Builder()
        .withProgram(program)
        .addItem(queryItemA)
        .withPeriods(List.of(periodA), "monthly")
        .withOrganisationUnits(List.of(orgUnitA))
        .build();
  }

  private void mockQueryPlannerToReturnSameEventParams() {
    // Mock to return EventQueryParams that extend DataQueryParams
    // This avoids the ClassCastException in QueryPlannerUtils.convert()
    lenient()
        .when(queryPlanner.groupByOrgUnitLevel(any(DataQueryParams.class)))
        .thenAnswer(
            invocation -> {
              DataQueryParams input = invocation.getArgument(0);
              // Return the same input as it's already an EventQueryParams
              return List.of(input);
            });

    lenient()
        .when(queryPlanner.groupByPeriodType(any(DataQueryParams.class)))
        .thenAnswer(
            invocation -> {
              DataQueryParams input = invocation.getArgument(0);
              // Return the same input as it's already an EventQueryParams
              return List.of(input);
            });
  }

  @Test
  void testPlanAggregateQuery_withAggregateData_debug() {

    var periodA = PeriodDimension.of(new MonthlyPeriodType().createPeriod("202501"));
    var periodB = PeriodDimension.of(new MonthlyPeriodType().createPeriod("202502"));
    var periodC = PeriodDimension.of(new MonthlyPeriodType().createPeriod("202503"));
    var periodD = PeriodDimension.of(new MonthlyPeriodType().createPeriod("202504"));

    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            // .addItem(queryItemA)
            .addItem(queryItemB)
            .withAggregateData(true)
            .withStartDate(periodA.getStartDate())
            .withEndDate(periodD.getEndDate())
            .withPeriods(List.of(periodA, periodB, periodC, periodD), "monthly")
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should create separate queries for each item when aggregating
    assertThat(result.size(), is(1));
    assertTrue(result.stream().allMatch(q -> q.getTableName() != null));
    assertTrue(result.stream().allMatch(q -> q.getPartitions() != null));
  }

  @Test
  void testPlanAggregateQuery_withSingleTimeDimension() {
    // Create params with single time dimension
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withProgram(program)
            .addItem(queryItemA)
            .withAggregateData(true)
            .withOrganisationUnits(List.of(orgUnitA))
            .build();

    // Add single time dimension
    params
        .getTimeDateRanges()
        .put(
            TimeField.EVENT_DATE,
            List.of(
                new org.hisp.dhis.common.DateRange(periodA.getStartDate(), periodA.getEndDate())));

    mockQueryPlannerToReturnSameEventParams();
    mockPartitionManager();

    List<EventQueryParams> result = eventQueryPlanner.planAggregateQuery(params);

    // Should not split - single time dimension
    assertThat(result.size(), is(1));
    assertThat(result.get(0).getActiveTimeDimensionCount(), is(1));
    assertTrue(result.get(0).hasActiveTimeDimension(TimeField.EVENT_DATE));
  }

  private void mockPartitionManager() {
    lenient()
        .doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());
  }

  private void setUser(EventQueryParams params, String username) {
    try {
      java.lang.reflect.Field currentUserField = DataQueryParams.class.getDeclaredField(username);
      currentUserField.setAccessible(true);
      currentUserField.set(params, currentUser);
    } catch (Exception e) {
      // Skip this test if reflection fails
      fail("Failed to set user via reflection: " + e.getMessage());
    }
  }
}
