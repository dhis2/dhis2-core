/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.TotalAggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.system.grid.ListGrid;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for DataHandler indicator handling with periodOffset, specifically for the bug fix where
 * periods in filters with periodOffset returned 0 instead of data.
 */
@ExtendWith(MockitoExtension.class)
class DataHandlerIndicatorPeriodOffsetTest {

  // Mocks for DataHandler required dependencies
  @Mock private org.hisp.dhis.analytics.event.data.EventAggregateService eventAggregatedService;

  @Mock private org.hisp.dhis.analytics.RawAnalyticsManager rawAnalyticsManager;

  @Mock private org.hisp.dhis.analytics.resolver.ExpressionResolvers resolvers;

  @Mock private ExpressionService expressionService;

  @Mock private org.hisp.dhis.analytics.QueryPlanner queryPlanner;

  @Mock private org.hisp.dhis.setting.SystemSettingsProvider settingsProvider;

  @Mock private org.hisp.dhis.analytics.AnalyticsManager analyticsManager;

  @Mock private org.hisp.dhis.organisationunit.OrganisationUnitService organisationUnitService;

  @Mock private org.hisp.dhis.analytics.analyze.ExecutionPlanStore executionPlanStore;

  @Mock private DataAggregator dataAggregator;

  @InjectMocks private DataHandler dataHandler;

  private Indicator indicator;
  private DataElement dataElement;
  private IndicatorType indicatorType;

  @BeforeEach
  void setUp() {
    // special handling for this dependency
    dataHandler.require(dataAggregator);

    // Create test data element with periodOffset
    dataElement = new DataElement("Test Data Element");
    dataElement.setUid("FQ2o8UBlcrS");
    dataElement.setAggregationType(AggregationType.SUM);
    dataElement.setQueryMods(QueryModifiers.builder().periodOffset(-1).build());

    // Create test indicator
    indicatorType = new IndicatorType("Per 1", 1, false);
    indicator = new Indicator();
    indicator.setUid("E1s0tdfYFb3");
    indicator.setName("Test Indicator");
    indicator.setNumerator("#{FQ2o8UBlcrS} - #{FQ2o8UBlcrS}.periodOffset(-1)");
    indicator.setDenominator("1");
    indicator.setIndicatorType(indicatorType);
    indicator.setAggregationType(AggregationType.SUM);
  }

  @Test
  void testAddIndicatorValuesWithPeriodOffsetInFilter() {
    // Given: Indicator with periodOffset and periods in filter
    List<Period> filterPeriods = createMonthlyPeriods(2024, 11, 3); // Nov, Dec, Jan

    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    // Mock the expression service to return indicator values per period
    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(2.0)) // Period 1: Nov 2024
        .thenReturn(createIndicatorValue(-2.0)) // Period 2: Dec 2024
        .thenReturn(createIndicatorValue(5.0)); // Period 3: Jan 2025

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should have one row with aggregated value
    assertEquals(1, grid.getRows().size(), "Should have one aggregated row");
    List<Object> row = grid.getRow(0);
    assertNotNull(row, "Row should not be null");
    assertFalse(row.isEmpty(), "Row should have values");

    // The aggregated value should be sum of all periods: 2.0 + (-2.0) + 5.0 = 5.0
    Object value = row.get(row.size() - 1); // Last column is the value
    assertNotNull(value, "Value should not be null");
    assertEquals(5.0, ((Number) value).doubleValue(), 0.001, "Aggregated value should be 5.0");
  }

  @Test
  void testAddIndicatorValuesWithoutPeriodOffsetInFilter() {
    // Given: Indicator without periodOffset and periods in filter
    DataElement normalDataElement = new DataElement("Normal Data Element");
    normalDataElement.setUid("NormalDE123");
    normalDataElement.setAggregationType(AggregationType.SUM);
    // No periodOffset

    Indicator normalIndicator = new Indicator();
    normalIndicator.setUid("NormalInd123");
    normalIndicator.setName("Normal Indicator");
    normalIndicator.setNumerator("#{NormalDE123}");
    normalIndicator.setDenominator("1");
    normalIndicator.setIndicatorType(indicatorType);

    List<Period> filterPeriods = createMonthlyPeriods(2024, 11, 2);

    DataQueryParams params =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(normalIndicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    // Mock the expression service
    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, normalDataElement.getUid()),
        normalDataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(10.0));

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should have one row (standard behavior)
    assertFalse(grid.getRows().isEmpty(), "Should have at least one row");
  }

  @Test
  void testAddIndicatorValuesWithPeriodOffsetInDimensions() {
    // Given: Indicator with periodOffset and periods in dimensions
    List<Period> dimensionPeriods = createMonthlyPeriods(2024, 11, 2);

    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)))
            .addDimension(
                new BaseDimensionalObject(
                    PERIOD_DIM_ID,
                    PERIOD,
                    dimensionPeriods.stream().map(PeriodDimension::of).toList()))
            .build();

    Grid grid = new ListGrid();

    // Mock the expression service
    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(3.0))
        .thenReturn(createIndicatorValue(4.0));

    Grid dataGrid = createMockDataGrid(dimensionPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should have rows for each period (one row per period)
    assertFalse(grid.getRows().isEmpty(), "Should have rows for periods in dimensions");
  }

  @Test
  void testAddIndicatorValuesWithSinglePeriodNoPeriodOffset() {
    // Given: Indicator without periodOffset and single period in filter
    DataElement normalDataElement = new DataElement("Normal Data Element");
    normalDataElement.setUid("NormalDE999");
    normalDataElement.setAggregationType(AggregationType.SUM);

    Indicator normalIndicator = new Indicator();
    normalIndicator.setUid("NormalInd999");
    normalIndicator.setName("Normal Indicator");
    normalIndicator.setNumerator("#{NormalDE999}");
    normalIndicator.setDenominator("1");
    normalIndicator.setIndicatorType(indicatorType);
    normalIndicator.setAggregationType(AggregationType.SUM);

    List<Period> singlePeriod = createMonthlyPeriods(2024, 6, 1); // Just June 2024

    DataQueryParams params =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(normalIndicator)))
            .withFilterPeriods(singlePeriod.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    // Mock the expression service
    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, normalDataElement.getUid()),
        normalDataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(15.0));

    Grid dataGrid = createMockDataGrid(singlePeriod);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should have one row
    assertEquals(1, grid.getRows().size(), "Should have one row for single period");
  }

  @Test
  void testAddIndicatorValuesWithEmptyIndicatorList() {
    // Given: Empty indicator list
    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of()))
            .build();

    Grid grid = new ListGrid();

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should remain empty
    assertEquals(0, grid.getRows().size(), "Grid should have no rows");
  }

  @Test
  void testAddIndicatorValuesWithMultiplePeriodsAndPeriodOffset() {
    // Given: Indicator with periodOffset and multiple periods in filter (12 months)
    List<Period> filterPeriods = createMonthlyPeriods(2024, 1, 12); // Full year

    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    // Mock the expression service
    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    // Mock values for each month (alternating positive and negative)
    IndicatorValue[] values = new IndicatorValue[12];
    for (int i = 0; i < 12; i++) {
      values[i] = createIndicatorValue(i % 2 == 0 ? 2.0 : -1.0);
    }
    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(
            values[0],
            values[1],
            values[2],
            values[3],
            values[4],
            values[5],
            values[6],
            values[7],
            values[8],
            values[9],
            values[10],
            values[11]);

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should have one aggregated row
    assertEquals(1, grid.getRows().size(), "Should have one aggregated row for 12 periods");
  }

  @Test
  void testAddIndicatorValuesWithPeriodOffsetAndAverageAggregation() {
    // Given: Indicator with periodOffset and denominator != "1" (triggers AVERAGE aggregation)
    Indicator averageIndicator = new Indicator();
    averageIndicator.setUid("AvgInd123");
    averageIndicator.setName("Average Indicator");
    averageIndicator.setNumerator("#{FQ2o8UBlcrS} - #{FQ2o8UBlcrS}.periodOffset(-1)");
    averageIndicator.setDenominator("100"); // Non-"1" denominator triggers AVERAGE
    averageIndicator.setIndicatorType(indicatorType);
    averageIndicator.setAggregationType(AggregationType.AVERAGE);

    List<Period> filterPeriods = createMonthlyPeriods(2024, 1, 3); // Jan, Feb, Mar

    DataQueryParams params =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(averageIndicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    // Mock the expression service
    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    // Mock indicator values: 6.0, 9.0, 12.0
    // Expected: AVERAGE = (6.0 + 9.0 + 12.0) / 3 = 9.0
    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(6.0)) // Period 1: Jan 2024
        .thenReturn(createIndicatorValue(9.0)) // Period 2: Feb 2024
        .thenReturn(createIndicatorValue(12.0)); // Period 3: Mar 2024

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should have one row with averaged value
    assertEquals(1, grid.getRows().size(), "Should have one aggregated row");
    List<Object> row = grid.getRow(0);
    assertNotNull(row, "Row should not be null");
    assertFalse(row.isEmpty(), "Row should have values");

    // The aggregated value should be average: (6.0 + 9.0 + 12.0) / 3 = 9.0
    Object value = row.get(row.size() - 1);
    assertNotNull(value, "Value should not be null");
    assertEquals(9.0, ((Number) value).doubleValue(), 0.001, "Averaged value should be 9.0");
  }

  @Test
  void testAddIndicatorValuesWithNoneAggregationType() {
    // Given: Indicator with periodOffset and NONE aggregation type (should behave like SUM)
    Indicator noneIndicator = spy(new Indicator());
    noneIndicator.setUid("NoneInd123");
    noneIndicator.setName("None Aggregation Indicator");
    noneIndicator.setNumerator("#{FQ2o8UBlcrS} - #{FQ2o8UBlcrS}.periodOffset(-1)");
    noneIndicator.setDenominator("1");
    noneIndicator.setIndicatorType(indicatorType);

    // Mock getTotalAggregationType to return NONE
    when(noneIndicator.getTotalAggregationType()).thenReturn(TotalAggregationType.NONE);

    List<Period> filterPeriods = createMonthlyPeriods(2024, 1, 3); // Jan, Feb, Mar

    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(noneIndicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    // Mock the expression service
    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    // Mock indicator values: 3.0, 7.0, 5.0
    // Expected: NONE behaves like SUM = 3.0 + 7.0 + 5.0 = 15.0
    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(3.0)) // Period 1: Jan 2024
        .thenReturn(createIndicatorValue(7.0)) // Period 2: Feb 2024
        .thenReturn(createIndicatorValue(5.0)); // Period 3: Mar 2024

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Grid should have one row with summed value (NONE behaves like SUM)
    assertEquals(1, grid.getRows().size(), "Should have one aggregated row");
    List<Object> row = grid.getRow(0);
    assertNotNull(row, "Row should not be null");
    assertFalse(row.isEmpty(), "Row should have values");

    // The aggregated value should be sum: 3.0 + 7.0 + 5.0 = 15.0 (NONE behaves like SUM)
    Object value = row.get(row.size() - 1);
    assertNotNull(value, "Value should not be null");
    assertEquals(
        15.0,
        ((Number) value).doubleValue(),
        0.001,
        "NONE aggregation should behave like SUM (15.0)");
  }

  @Test
  void testMixedIndicatorsWithAndWithoutPeriodOffset() {
    // Given: Two indicators - one with periodOffset, one without
    DataElement normalDataElement = new DataElement("Normal Data Element");
    normalDataElement.setUid("NormalDE456");
    normalDataElement.setAggregationType(AggregationType.SUM);
    // No periodOffset

    Indicator normalIndicator = new Indicator();
    normalIndicator.setUid("NormalInd456");
    normalIndicator.setName("Normal Indicator");
    normalIndicator.setNumerator("#{NormalDE456}");
    normalIndicator.setDenominator("1");
    normalIndicator.setIndicatorType(indicatorType);
    normalIndicator.setAggregationType(AggregationType.SUM);

    List<Period> filterPeriods = createMonthlyPeriods(2024, 1, 2);

    // Two separate queries - one for each indicator
    DataQueryParams paramsWithOffset =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    DataQueryParams paramsWithoutOffset =
        newBuilder()
            .addDimension(
                new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(normalIndicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid1 = new ListGrid();
    Grid grid2 = new ListGrid();

    // Mock for indicator with periodOffset
    Map<DimensionalItemId, DimensionalItemObject> itemMapOffset = new HashMap<>();
    itemMapOffset.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);

    // Mock for indicator without periodOffset
    Map<DimensionalItemId, DimensionalItemObject> itemMapNormal = new HashMap<>();
    itemMapNormal.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, normalDataElement.getUid()),
        normalDataElement);

    when(expressionService.getIndicatorDimensionalItemMap(List.of(indicator)))
        .thenReturn(itemMapOffset);
    when(expressionService.getIndicatorDimensionalItemMap(List.of(normalIndicator)))
        .thenReturn(itemMapNormal);

    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(3.0))
        .thenReturn(createIndicatorValue(4.0))
        .thenReturn(createIndicatorValue(10.0));

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values for both
    dataHandler.addIndicatorValues(paramsWithOffset, grid1);
    dataHandler.addIndicatorValues(paramsWithoutOffset, grid2);

    // Then: Both should produce results independently
    assertEquals(
        1, grid1.getRows().size(), "Indicator with periodOffset should have one aggregated row");
    assertEquals(1, grid2.getRows().size(), "Indicator without periodOffset should have one row");
  }

  @Test
  void testIndicatorWithNullValues() {
    // Given: Indicator with periodOffset where some periods return null
    List<Period> filterPeriods = createMonthlyPeriods(2024, 1, 3);

    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    // Mock: Period 1 = 5.0, Period 2 = null, Period 3 = 3.0
    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(createIndicatorValue(5.0))
        .thenReturn(null) // Null value for second period
        .thenReturn(createIndicatorValue(3.0));

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: Should aggregate only non-null values: 5.0 + 3.0 = 8.0
    assertEquals(1, grid.getRows().size(), "Should have one aggregated row");
    List<Object> row = grid.getRow(0);
    Object value = row.get(row.size() - 1);
    assertNotNull(value, "Value should not be null");
    assertEquals(8.0, ((Number) value).doubleValue(), 0.001, "Should sum only non-null values");
  }

  @Test
  void testIndicatorWithAllNullValues() {
    // Given: Indicator where all periods return null
    List<Period> filterPeriods = createMonthlyPeriods(2024, 1, 3);

    DataQueryParams params =
        newBuilder()
            .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DATA_X, List.of(indicator)))
            .withFilterPeriods(filterPeriods.stream().map(PeriodDimension::of).toList())
            .build();

    Grid grid = new ListGrid();

    Map<DimensionalItemId, DimensionalItemObject> itemMap = new HashMap<>();
    itemMap.put(
        new DimensionalItemId(DimensionItemType.DATA_ELEMENT, dataElement.getUid()), dataElement);
    when(expressionService.getIndicatorDimensionalItemMap(anyList())).thenReturn(itemMap);

    // All periods return null
    when(expressionService.getIndicatorValueObject(
            any(Indicator.class), anyList(), any(), any(), any()))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);

    Grid dataGrid = createMockDataGrid(filterPeriods);
    when(dataAggregator.getAggregatedDataValueGrid(any())).thenReturn(dataGrid);

    // When: Adding indicator values
    dataHandler.addIndicatorValues(params, grid);

    // Then: No row should be added when all values are null
    assertEquals(0, grid.getRows().size(), "Should have no rows when all values are null");
  }

  private List<Period> createMonthlyPeriods(int startYear, int startMonth, int count) {
    MonthlyPeriodType periodType = new MonthlyPeriodType();
    List<Period> periods = new java.util.ArrayList<>();

    for (int i = 0; i < count; i++) {
      DateTime date = new DateTime(startYear, startMonth, 1, 0, 0);
      date = date.plusMonths(i);
      Period period = periodType.createPeriod(date.toDate());
      periods.add(period);
    }

    return periods;
  }

  private IndicatorValue createIndicatorValue(double value) {
    return new IndicatorValue()
        .setNumeratorValue(value)
        .setDenominatorValue(1.0)
        .setMultiplier(1)
        .setDivisor(1);
  }

  private Grid createMockDataGrid(List<Period> periods) {
    Grid grid = new ListGrid();
    // Add minimal grid structure to avoid null pointer exceptions
    for (Period period : periods) {
      grid.addRow();
      grid.addValue(dataElement.getUid());
      grid.addValue(PeriodDimension.of(period).getIsoDate());
      grid.addValue(10.0); // Mock data value
    }
    return grid;
  }
}
