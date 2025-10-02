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
package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.user.User;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultQueryPlannerTest extends TestBase {

  @Mock private PartitionManager partitionManager;

  @InjectMocks private DefaultQueryPlanner queryPlanner;

  private DataElement dataElementA;
  private DataElement dataElementB;
  private DataElement textDataElement;
  private DataElement averageDataElement;
  private OrganisationUnit orgUnitA;
  private OrganisationUnit orgUnitB;
  private PeriodDimension periodA;
  private PeriodDimension periodB;
  private PeriodDimension quarterPeriod;
  private User currentUser;

  @BeforeEach
  void setUp() {
    // Create test data elements
    dataElementA = createDataElement('A', new CategoryCombo());
    dataElementB = createDataElement('B', new CategoryCombo());
    textDataElement = createDataElement('C', ValueType.TEXT, AggregationType.COUNT);
    averageDataElement = createDataElement('D', ValueType.INTEGER, AggregationType.AVERAGE);

    // Create test organization units at different levels
    orgUnitA = createOrganisationUnit('A', "OU_A", "UID_A", 1);
    orgUnitB = createOrganisationUnit('B', "OU_B", "UID_B", 2);

    // Create test periods
    periodA =
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2023, 1, 1, 0, 0).toDate()));
    periodB =
        PeriodDimension.of(
            new MonthlyPeriodType().createPeriod(new DateTime(2023, 2, 1, 0, 0).toDate()));
    quarterPeriod =
        PeriodDimension.of(
            new QuarterlyPeriodType().createPeriod(new DateTime(2023, 1, 1, 0, 0).toDate()));

    // Create test user
    currentUser = makeUser("U");
  }

  // -------------------------------------------------------------------------
  // Tests for planQuery method
  // -------------------------------------------------------------------------

  @Test
  void testPlanQuery_basicScenario() {
    DataQueryParams params = createBasicQueryParams();
    QueryPlannerParams plannerParams = createBasicPlannerParams();

    doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void, so we just return null
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());

    DataQueryGroups result = queryPlanner.planQuery(params, plannerParams);

    assertNotNull(result);
    assertFalse(result.getAllQueries().isEmpty());
    verify(partitionManager, atLeastOnce())
        .filterNonExistingPartitions(any(Partitions.class), anyString());
  }

  @Test
  void testPlanQuery_withOptimalQueriesReached() {
    DataQueryParams params = createBasicQueryParams();
    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder()
            .withTableType(AnalyticsTableType.DATA_VALUE)
            .withOptimalQueries(1)
            .build();

    doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void, so we just return null
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());

    DataQueryGroups result = queryPlanner.planQuery(params, plannerParams);

    assertTrue(result.isOptimal(1));
  }

  @Test
  void testPlanQuery_withSplittingByDimension() {
    // Create params with many data elements to force splitting
    List<DimensionalItemObject> manyDataElements =
        Arrays.asList(dataElementA, dataElementB, textDataElement, averageDataElement);

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(
                List.of(
                    new BaseDimensionalObject(
                        DATA_X_DIM_ID, DimensionType.DATA_X, manyDataElements),
                    new BaseDimensionalObject(
                        PERIOD_DIM_ID, DimensionType.PERIOD, List.of(periodA))))
            .build();

    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder()
            .withTableType(AnalyticsTableType.DATA_VALUE)
            .withOptimalQueries(2)
            .build();

    doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void, so we just return null
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());

    DataQueryGroups result = queryPlanner.planQuery(params, plannerParams);

    assertThat(result.getAllQueries().size(), greaterThan(1));
  }

  @Test
  void testPlanQuery_withCustomQueryGroupers() {
    Function<DataQueryParams, List<DataQueryParams>> customGrouper =
        params -> Arrays.asList(params, params);

    DataQueryParams params = createBasicQueryParams();
    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder()
            .withTableType(AnalyticsTableType.DATA_VALUE)
            .withQueryGroupers(List.of(customGrouper))
            .build();

    doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void, so we just return null
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());

    DataQueryGroups result = queryPlanner.planQuery(params, plannerParams);

    assertThat(result.getAllQueries().size(), greaterThanOrEqualTo(2));
  }

  // -------------------------------------------------------------------------
  // Tests for withTableNameAndPartitions
  // -------------------------------------------------------------------------

  @Test
  void testWithTableNameAndPartitions_withUser() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withCurrentUser(currentUser)
            .withPeriods(List.of(periodA))
            .build();

    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build();

    doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void, so we just return null
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());

    DataQueryParams result = queryPlanner.withTableNameAndPartitions(params, plannerParams);

    assertEquals("analytics", result.getTableName());
    assertNotNull(result.getPartitions());
    verify(partitionManager).filterNonExistingPartitions(any(Partitions.class), eq("analytics"));
  }

  @Test
  void testWithTableNameAndPartitions_withoutUser() {
    DataQueryParams params = DataQueryParams.newBuilder().withPeriods(List.of(periodA)).build();

    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build();

    DataQueryParams result = queryPlanner.withTableNameAndPartitions(params, plannerParams);

    assertEquals("analytics", result.getTableName());
    assertNotNull(result.getPartitions());
    verify(partitionManager, never()).filterNonExistingPartitions(any(), any());
  }

  // -------------------------------------------------------------------------
  // Tests for withPartitionsFromQueryPeriods method
  // -------------------------------------------------------------------------

  @Test
  void testWithPartitionsFromQueryPeriods_withTableName() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withTableName("existing_table")
            .withPeriods(List.of(periodA))
            .build();

    doAnswer(
            invocation -> {
              // handle PartitionManager.filterNonExistingPartitions being void
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), eq("existing_table"));

    DataQueryParams result =
        queryPlanner.withPartitionsFromQueryPeriods(params, AnalyticsTableType.DATA_VALUE);

    assertNotNull(result.getPartitions());
    verify(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), eq("existing_table"));
  }

  @Test
  void testWithPartitionsFromQueryPeriods_withoutTableName() {
    DataQueryParams params = DataQueryParams.newBuilder().withPeriods(List.of(periodA)).build();

    DataQueryParams result =
        queryPlanner.withPartitionsFromQueryPeriods(params, AnalyticsTableType.DATA_VALUE);

    assertNotNull(result.getPartitions());
    verify(partitionManager, never()).filterNonExistingPartitions(any(), any());
  }

  // -------------------------------------------------------------------------
  // Tests for groupByPeriodType
  // -------------------------------------------------------------------------

  @Test
  void testGroupByPeriodType_withPeriodDimensions() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(
                List.of(
                    new BaseDimensionalObject(
                        PERIOD_DIM_ID,
                        DimensionType.PERIOD,
                        Arrays.asList(periodA, quarterPeriod))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByPeriodType(params);

    assertThat(result.size(), greaterThan(1));
    assertTrue(result.stream().allMatch(q -> q.getPeriodType() != null));
  }

  @Test
  void testGroupByPeriodType_withPeriodFilters() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withFilters(
                List.of(
                    new BaseDimensionalObject(
                        PERIOD_DIM_ID,
                        DimensionType.PERIOD,
                        Arrays.asList(periodA, quarterPeriod))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByPeriodType(params);

    assertEquals(1, result.size());
    assertNotNull(result.get(0).getPeriodType());
  }

  @Test
  void testGroupByPeriodType_withSkipPartitioning() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withSkipPartitioning(true)
            .withPeriods(List.of(periodA))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByPeriodType(params);

    assertEquals(1, result.size());
    assertEquals(params, result.get(0));
  }

  @Test
  void testGroupByPeriodType_withoutPeriods() {
    DataQueryParams params = DataQueryParams.newBuilder().build();

    List<DataQueryParams> result = queryPlanner.groupByPeriodType(params);

    assertEquals(1, result.size());
  }

  // -------------------------------------------------------------------------
  // Tests for groupByOrgUnitLevel method
  // -------------------------------------------------------------------------

  @Test
  void testGroupByOrgUnitLevel_withOrgUnitDimensions() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(
                List.of(
                    new BaseDimensionalObject(
                        ORGUNIT_DIM_ID,
                        DimensionType.ORGANISATION_UNIT,
                        Arrays.asList(orgUnitA, orgUnitB))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByOrgUnitLevel(params);

    assertThat(result.size(), greaterThan(1));
    assertTrue(result.stream().noneMatch(q -> q.getOrganisationUnits().isEmpty()));
  }

  @Test
  void testGroupByOrgUnitLevel_withOrgUnitFilters() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withFilters(
                List.of(
                    new BaseDimensionalObject(
                        ORGUNIT_DIM_ID,
                        DimensionType.ORGANISATION_UNIT,
                        Arrays.asList(orgUnitA, orgUnitB))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByOrgUnitLevel(params);

    assertEquals(1, result.size());
    assertFalse(result.get(0).getFilterOrganisationUnits().isEmpty());
  }

  @Test
  void testGroupByOrgUnitLevel_withoutOrgUnits() {
    DataQueryParams params = DataQueryParams.newBuilder().build();

    List<DataQueryParams> result = queryPlanner.groupByOrgUnitLevel(params);

    assertEquals(1, result.size());
  }

  // -------------------------------------------------------------------------
  // Tests for groupByStartEndDateRestriction method
  // -------------------------------------------------------------------------

  @Test
  void testGroupByStartEndDateRestriction_withPeriodDimensions() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(
                List.of(
                    new BaseDimensionalObject(
                        PERIOD_DIM_ID, DimensionType.PERIOD, Arrays.asList(periodA, periodB))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByStartEndDateRestriction(params);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(q -> q.getStartDateRestriction() != null));
    assertTrue(result.stream().allMatch(q -> q.getEndDateRestriction() != null));
  }

  @Test
  void testGroupByStartEndDateRestriction_withPeriodFilters() {
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withFilters(
                List.of(
                    new BaseDimensionalObject(
                        PERIOD_DIM_ID, DimensionType.PERIOD, List.of(periodA))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByStartEndDateRestriction(params);

    assertEquals(1, result.size());
    assertNotNull(result.get(0).getStartDateRestriction());
    assertNotNull(result.get(0).getEndDateRestriction());
  }

  @Test
  void testGroupByStartEndDateRestriction_withoutPeriods() {
    DataQueryParams params = DataQueryParams.newBuilder().build();

    assertThrows(
        IllegalQueryException.class, () -> queryPlanner.groupByStartEndDateRestriction(params));
  }

  @Test
  void testPlanQuery_emptyParams() {
    DataQueryParams params = DataQueryParams.newBuilder().build();
    QueryPlannerParams plannerParams = createBasicPlannerParams();

    doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void, so we just return null
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());

    DataQueryGroups result = queryPlanner.planQuery(params, plannerParams);

    assertNotNull(result);
    assertFalse(result.getAllQueries().isEmpty());
  }

  @Test
  void testGroupByPeriodType_mixedPeriodTypes() {
    MonthlyPeriodType monthlyType = new MonthlyPeriodType();
    YearlyPeriodType yearlyType = new YearlyPeriodType();

    PeriodDimension monthPeriod =
        PeriodDimension.of(monthlyType.createPeriod(new DateTime(2023, 1, 1, 0, 0).toDate()));
    PeriodDimension yearPeriod =
        PeriodDimension.of(yearlyType.createPeriod(new DateTime(2023, 1, 1, 0, 0).toDate()));

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(
                List.of(
                    new BaseDimensionalObject(
                        PERIOD_DIM_ID,
                        DimensionType.PERIOD,
                        Arrays.asList(monthPeriod, yearPeriod))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByPeriodType(params);

    assertEquals(2, result.size());
    assertThat(
        result.stream()
            .map(DataQueryParams::getPeriodType)
            .collect(java.util.stream.Collectors.toSet()),
        hasSize(2));
  }

  @Test
  void testGroupByOrgUnitLevel_sameLevelOrgUnits() {
    OrganisationUnit orgUnit1 = createOrganisationUnit('1', "OU_1", "UID_1", 1);
    OrganisationUnit orgUnit2 = createOrganisationUnit('2', "OU_2", "UID_2", 1);

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(
                List.of(
                    new BaseDimensionalObject(
                        ORGUNIT_DIM_ID,
                        DimensionType.ORGANISATION_UNIT,
                        Arrays.asList(orgUnit1, orgUnit2))))
            .build();

    List<DataQueryParams> result = queryPlanner.groupByOrgUnitLevel(params);

    assertEquals(1, result.size());
    assertEquals(2, result.get(0).getOrganisationUnits().size());
  }

  @Test
  void testWithTableNameAndPartitions_nullTableName() {
    DataQueryParams params = DataQueryParams.newBuilder().withPeriods(List.of(periodA)).build();

    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder().withTableType(AnalyticsTableType.DATA_VALUE).build();

    DataQueryParams result = queryPlanner.withTableNameAndPartitions(params, plannerParams);

    assertNotNull(result.getPartitions());
  }

  @Test
  void testPlanQuery_withZeroOptimalQueries() {
    DataQueryParams params = createBasicQueryParams();
    QueryPlannerParams plannerParams =
        QueryPlannerParams.newBuilder()
            .withTableType(AnalyticsTableType.DATA_VALUE)
            .withOptimalQueries(0)
            .build();

    doAnswer(
            invocation -> {
              // PartitionManager.filterNonExistingPartitions returns void, so we just return null
              return null;
            })
        .when(partitionManager)
        .filterNonExistingPartitions(any(Partitions.class), anyString());

    DataQueryGroups result = queryPlanner.planQuery(params, plannerParams);

    assertNotNull(result);
    assertTrue(result.isOptimal(0));
  }

  // -------------------------------------------------------------------------
  // Helper methods
  // -------------------------------------------------------------------------

  private DataQueryParams createBasicQueryParams() {
    return DataQueryParams.newBuilder()
        .withDimensions(
            List.of(
                new BaseDimensionalObject(
                    DATA_X_DIM_ID, DimensionType.DATA_X, List.of(dataElementA)),
                new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, List.of(periodA)),
                new BaseDimensionalObject(
                    ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, List.of(orgUnitA))))
        .build();
  }

  private QueryPlannerParams createBasicPlannerParams() {
    return QueryPlannerParams.newBuilder()
        .withTableType(AnalyticsTableType.DATA_VALUE)
        .withOptimalQueries(50)
        .build();
  }

  private OrganisationUnit createOrganisationUnit(
      char uniqueChar, String name, String uid, int level) {
    OrganisationUnit orgUnit = new OrganisationUnit(name, uid, uid, null, null, "c" + uniqueChar);
    // Level is calculated dynamically from the path, so we set the path to reflect the desired
    // level
    StringBuilder pathBuilder = new StringBuilder();
    for (int i = 1; i <= level; i++) {
      pathBuilder.append("/").append("level").append(i);
    }
    if (!pathBuilder.isEmpty()) {
      // Use reflection to set the path since it's needed for level calculation
      try {
        java.lang.reflect.Field pathField = OrganisationUnit.class.getDeclaredField("path");
        pathField.setAccessible(true);
        pathField.set(orgUnit, pathBuilder.toString());
      } catch (Exception e) {
        // If reflection fails, create a hierarchy manually
        OrganisationUnit parent;
        OrganisationUnit current = orgUnit;
        for (int i = level; i > 1; i--) {
          parent =
              new OrganisationUnit("Parent" + i, "parent" + i, "parent" + i, null, null, "p" + i);
          current.setParent(parent);
          parent.getChildren().add(current);
          current = parent;
        }
        orgUnit.updatePath();
      }
    }
    return orgUnit;
  }
}
