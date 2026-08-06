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
package org.hisp.dhis.analytics.event.data;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.EventAnalyticsDimensionalItem;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.data.handler.SchemeIdResponseMapper;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the table-layout path of {@link DefaultEventAnalyticsService} — an event aggregate request
 * with both {@code rows=} and {@code columns=} set, which pivots the normalized analytics grid into
 * one output row per row-dimension permutation.
 *
 * <p>The value map used to fill those cells is a pure function of the input grid, so it is built
 * once per request rather than once per output row. These tests pin the output that hoisting must
 * not change: every permutation lands in the right cell, missing combinations come out {@code
 * null}, and a permutation with no values at all is dropped from the output entirely.
 *
 * <p>{@code generateOutputGrid} is reached by reflection, as {@link
 * DefaultEventAnalyticsServiceTest} already does, so that the pivot is exercised on its own rather
 * than through the whole query pipeline.
 */
@ExtendWith(MockitoExtension.class)
class DefaultEventAnalyticsServiceTableLayoutTest {
  private static final String ROW_DIMENSION = "rowDimUidA";

  private static final String COLUMN_DIMENSION = "colDimUidB";

  private DefaultEventAnalyticsService service;

  @Mock private AnalyticsSecurityManager securityManager;

  @Mock private EventQueryValidator eventQueryValidator;

  @Mock private DataElementService dataElementService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private EventAnalyticsManager eventAnalyticsManager;

  @Mock private EnrollmentAnalyticsManager enrollmentAnalyticsManager;

  @Mock private EventDataQueryService eventDataQueryService;

  @Mock private EventQueryPlanner queryPlanner;

  @Mock private DatabaseInfoProvider databaseInfoProvider;

  @Mock private AnalyticsCache analyticsCache;

  @Mock private SchemeIdResponseMapper schemeIdResponseMapper;

  @Mock private UserService userService;

  @Mock private OrganisationUnitResolver organisationUnitResolver;

  @BeforeEach
  void setUp() {
    when(databaseInfoProvider.getDatabaseInfo()).thenReturn(DatabaseInfo.builder().build());
    service =
        new DefaultEventAnalyticsService(
            dataElementService,
            trackedEntityAttributeService,
            eventAnalyticsManager,
            eventDataQueryService,
            securityManager,
            queryPlanner,
            eventQueryValidator,
            databaseInfoProvider,
            analyticsCache,
            enrollmentAnalyticsManager,
            schemeIdResponseMapper,
            userService,
            organisationUnitResolver);
  }

  /**
   * Two row options against two column options give four cells. Three of them have a value in the
   * input grid; the fourth (R2 / C2) does not and must come out as {@code null}.
   */
  @Test
  void testTableLayoutFillsEveryPermutationFromTheInputGrid() throws Exception {
    Grid grid = inputGrid(row("R1", "C1", 1d), row("R1", "C2", 2d), row("R2", "C1", 3d));

    Grid outputGrid = pivot(grid);

    // One header for the row dimension, then one per column permutation.
    assertEquals(
        List.of(ROW_DIMENSION, COLUMN_DIMENSION + " Column one", COLUMN_DIMENSION + " Column two"),
        outputGrid.getHeaders().stream().map(GridHeader::getColumn).toList());

    assertEquals(
        List.of(asList("Row one", 1d, 2d), asList("Row two", 3d, null)), outputGrid.getRows());
  }

  /**
   * A row permutation with no value in any column is removed from the output. R2 has no values at
   * all here, so only R1 survives.
   */
  @Test
  void testTableLayoutDropsRowPermutationsWithNoValues() throws Exception {
    Grid grid = inputGrid(row("R1", "C2", 2d));

    Grid outputGrid = pivot(grid);

    assertEquals(List.of(asList("Row one", null, 2d)), outputGrid.getRows());
  }

  /**
   * When the input grid holds no values every row permutation is dropped, and the pivot falls back
   * to returning the input grid rather than an output grid with headers and no rows.
   */
  @Test
  void testTableLayoutFallsBackToTheInputGridWhenThereAreNoValues() throws Exception {
    Grid grid = inputGrid();

    Grid outputGrid = pivot(grid);

    assertEquals(List.of(), outputGrid.getRows());
    assertEquals(
        List.of(ROW_DIMENSION, COLUMN_DIMENSION, "value"),
        outputGrid.getHeaders().stream().map(GridHeader::getName).toList());
  }

  /**
   * The normalized grid the analytics query produces: one meta column per dimension, then the
   * value.
   */
  @SafeVarargs
  private Grid inputGrid(List<Object>... rows) {
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(ROW_DIMENSION, "Row dimension", ValueType.TEXT, false, true));
    grid.addHeader(
        new GridHeader(COLUMN_DIMENSION, "Column dimension", ValueType.TEXT, false, true));
    grid.addHeader(new GridHeader("value", "Value", ValueType.NUMBER, false, false));

    for (List<Object> row : rows) {
      grid.addRow().addValuesAsList(row);
    }

    Map<String, Object> items = new HashMap<>();
    items.put(ROW_DIMENSION, new MetadataItem("Row dimension"));
    items.put(COLUMN_DIMENSION, new MetadataItem("Column dimension"));
    grid.getMetaData().put(AnalyticsMetaDataKey.ITEMS.getKey(), items);

    return grid;
  }

  private static List<Object> row(String rowOptionCode, String columnOptionCode, Double value) {
    return asList(rowOptionCode, columnOptionCode, value);
  }

  private Grid pivot(Grid grid) throws Exception {
    List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations =
        List.of(
            Map.of(ROW_DIMENSION, item("Row one", "R1", ROW_DIMENSION)),
            Map.of(ROW_DIMENSION, item("Row two", "R2", ROW_DIMENSION)));

    List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations =
        List.of(
            Map.of(COLUMN_DIMENSION, item("Column one", "C1", COLUMN_DIMENSION)),
            Map.of(COLUMN_DIMENSION, item("Column two", "C2", COLUMN_DIMENSION)));

    return invokeGenerateOutputGrid(
        grid,
        new EventQueryParams.Builder().build(),
        rowPermutations,
        columnPermutations,
        new ArrayList<>(List.of(ROW_DIMENSION)));
  }

  /**
   * An option-backed dimensional item: its {@code toString} is the option code, which is what the
   * pivot joins into a value-map key, and its display property is the option name, which is what
   * ends up in the row header.
   */
  private static EventAnalyticsDimensionalItem item(String name, String code, String parentUid) {
    return new EventAnalyticsDimensionalItem(new Option(name, code), parentUid);
  }

  private Grid invokeGenerateOutputGrid(
      Grid grid,
      EventQueryParams params,
      List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations,
      List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations,
      List<String> rowDimensions)
      throws Exception {
    Method method =
        DefaultEventAnalyticsService.class.getDeclaredMethod(
            "generateOutputGrid",
            Grid.class,
            EventQueryParams.class,
            List.class,
            List.class,
            List.class);
    method.setAccessible(true);
    return (Grid)
        method.invoke(service, grid, params, rowPermutations, columnPermutations, rowDimensions);
  }
}
