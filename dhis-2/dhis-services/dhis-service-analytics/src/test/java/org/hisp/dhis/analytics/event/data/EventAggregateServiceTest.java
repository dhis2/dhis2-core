/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createLegend;
import static org.hisp.dhis.test.TestBase.createLegendSet;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.EventAnalyticsDimensionalItem;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.cache.AnalyticsCacheSettings;
import org.hisp.dhis.analytics.common.ColumnHeader;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.LocalCache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EventAggregateServiceTest {

  private final EventAggregateService service =
      new EventAggregateService(null, null, null, null, null, null, null, null, null, null, null);

  @Test
  void shouldUseEnrollmentDateHeaderForStaticPeriodDateField() throws Exception {
    Program program = new Program();
    program.setEnrollmentDateLabel("Start of treatment date");

    GridHeader header =
        invokeAddDimensionHeaders(singleDateFieldParams("ENROLLMENT_DATE", program));

    assertEquals("enrollmentdate", header.getName());
    assertEquals("Start of treatment date", header.getColumn());
  }

  @Test
  void shouldUseIncidentDateHeaderForStaticPeriodDateField() throws Exception {
    Program program = new Program();
    program.setIncidentDateLabel("Incident date custom");

    GridHeader header = invokeAddDimensionHeaders(singleDateFieldParams("INCIDENT_DATE", program));

    assertEquals("incidentdate", header.getName());
    assertEquals("Incident date custom", header.getColumn());
  }

  @Test
  void shouldUseCustomOrgUnitLabelForOuDimensionHeader() throws Exception {
    Program program = new Program();
    program.setOrgUnitLabel("Facility");

    GridHeader header = invokeAddDimensionHeaders(orgUnitDimensionParams(program));

    assertEquals("ou", header.getName());
    assertEquals("Facility", header.getColumn());
  }

  @Test
  void shouldKeepPeHeaderForDefaultPeriod() throws Exception {
    GridHeader header = invokeAddDimensionHeaders(defaultPeriodParams());

    assertEquals("pe", header.getName());
    assertEquals("Period", header.getColumn());
  }

  @Test
  void shouldExpandMixedPeriodDateFieldsIntoSeparateHeaders() throws Exception {
    Grid grid = invokeDimensionHeadersGrid(mixedPeriodDateFieldsParams());

    assertEquals(
        List.of("enrollmentdate", "pe"),
        grid.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(
        List.of("Enrollment date", "Period"),
        grid.getHeaders().stream().map(GridHeader::getColumn).toList());
  }

  @Test
  void shouldRemoveRawPeMetadataWhenNoDefaultAggregatePeriodIsVisible() throws Exception {
    Grid grid = new ListGrid();
    grid.getMetaData()
        .put(
            AnalyticsMetaDataKey.DIMENSIONS.getKey(),
            new LinkedHashMap<>(
                Map.of("scheduleddate", List.of("2021"), PERIOD_DIM_ID, List.of("2021"))));

    invokePrivate(
        "removeRawPeriodDimensionMetadata",
        Grid.class,
        EventQueryParams.class,
        grid,
        singleDateFieldParams("SCHEDULED_DATE", new Program()));

    @SuppressWarnings("unchecked")
    Map<String, List<String>> dimensions =
        (Map<String, List<String>>)
            grid.getMetaData().get(AnalyticsMetaDataKey.DIMENSIONS.getKey());

    assertEquals(List.of("scheduleddate"), dimensions.keySet().stream().toList());
  }

  @Test
  void shouldFallBackToAllLegendsWhenDimensionsMetadataOmitsLegendSetDataElement()
      throws Exception {
    Legend legendA = createLegend('A', 0.0, 2.0);
    Legend legendB = createLegend('B', 2.0, 4.0);
    LegendSet legendSet = createLegendSet('A', legendA, legendB);

    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.NUMBER);
    dataElement.setLegendSets(List.of(legendSet));

    Grid grid = new ListGrid();
    grid.getMetaData().put(AnalyticsMetaDataKey.DIMENSIONS.getKey(), new HashMap<String, Object>());

    List<EventAnalyticsDimensionalItem> dimensionalItems = new ArrayList<>();

    invokeAddEventReportDimensionalItems(dataElement, dimensionalItems, grid, dataElement.getUid());

    assertFalse(dimensionalItems.isEmpty());
  }

  @Test
  void shouldNotNpeWhenItemsMetadataOmitsRowDimension() throws Exception {
    String missingDimensionUid = "deUidAAAAA";

    Grid grid = new ListGrid();
    grid.getMetaData().put(AnalyticsMetaDataKey.ITEMS.getKey(), new HashMap<String, Object>());

    EventQueryParams params = new EventQueryParams.Builder().build();

    invokeGenerateOutputGrid(grid, params, List.of(), List.of(), List.of(missingDimensionUid));
  }

  @Test
  void shouldKeepValueAndEnrollmentOuHeadersUnchanged() throws Exception {
    EventQueryParams params =
        new EventQueryParams.Builder(defaultPeriodParams())
            .withEnrollmentOuDimension(List.<DimensionalItemObject>of(createOrganisationUnit('A')))
            .build();

    Grid grid = new ListGrid();
    invokePrivate("addHeaders", EventQueryParams.class, Grid.class, params, grid);

    List<GridHeader> headers = grid.getHeaders();
    assertEquals("pe", headers.get(0).getName());
    assertEquals(ColumnHeader.ENROLLMENT_OU.getItem(), headers.get(1).getName());
    assertEquals(VALUE_ID, headers.get(2).getName());
  }

  @Test
  void shouldAddRegistrationOuHeaderAfterPeriod() throws Exception {
    EventQueryParams params =
        new EventQueryParams.Builder(defaultPeriodParams())
            .withRegistrationOuDimension(List.of(createOrganisationUnit('A')))
            .build();

    Grid grid = new ListGrid();
    invokePrivate("addHeaders", EventQueryParams.class, Grid.class, params, grid);

    List<GridHeader> headers = grid.getHeaders();
    assertEquals("pe", headers.get(0).getName());
    assertEquals(ColumnHeader.REGISTRATION_OU.getItem(), headers.get(1).getName());
    assertEquals(VALUE_ID, headers.get(2).getName());
  }

  @Test
  void shouldCacheRegistrationOuSelectionsAndOutputShapesIndependently() {
    AnalyticsCacheSettings cacheSettings = mock(AnalyticsCacheSettings.class);
    when(cacheSettings.isCachingEnabled()).thenReturn(true);
    when(cacheSettings.fixedExpirationTimeOrDefault()).thenReturn(60L);
    CacheProvider cacheProvider = mock(CacheProvider.class);
    SimpleCacheBuilder<Grid> cacheBuilder = new SimpleCacheBuilder<>();
    cacheBuilder.expireAfterWrite(1L, TimeUnit.MINUTES);
    when(cacheProvider.<Grid>createAnalyticsCache()).thenReturn(new LocalCache<>(cacheBuilder));
    AnalyticsCache cache = new AnalyticsCache(cacheProvider, cacheSettings);

    EventAnalyticsManager manager = mock(EventAnalyticsManager.class);
    EventQueryPlanner planner = mock(EventQueryPlanner.class);
    AnalyticsSecurityManager security = mock(AnalyticsSecurityManager.class);
    MetadataItemsHandler metadata = mock(MetadataItemsHandler.class);
    when(security.withUserConstraints(any(EventQueryParams.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(planner.planAggregateQuery(any()))
        .thenAnswer(invocation -> List.of(invocation.getArgument(0, EventQueryParams.class)));

    OrganisationUnit bo = createOrganisationUnit('A');
    bo.setUid("O6uvpzGd5pu");
    bo.setName("Bo");
    OrganisationUnit bombali = createOrganisationUnit('B');
    bombali.setUid("fdc6uOvgoji");
    bombali.setName("Bombali");
    Map<String, String> counts = Map.of(bo.getUid(), "1", bombali.getUid(), "2");

    when(manager.getAggregatedEventData(any(), any(), anyInt()))
        .thenAnswer(
            invocation -> {
              EventQueryParams params = invocation.getArgument(0);
              Grid grid = invocation.getArgument(1);
              OrganisationUnit ou = params.getAllRegistrationOuItems().get(0);
              grid.addRow().addValue("2022");
              if (params.hasRegistrationOuDimension()) {
                grid.addValue(ou.getUid());
              }
              grid.addValue(counts.get(ou.getUid()));
              return grid;
            });
    doAnswer(
            invocation -> {
              Grid grid = invocation.getArgument(0);
              EventQueryParams params = invocation.getArgument(1);
              OrganisationUnit ou = params.getAllRegistrationOuItems().get(0);
              grid.getMetaData()
                  .put(AnalyticsMetaDataKey.ITEMS.getKey(), Map.of(ou.getUid(), ou.getName()));
              return null;
            })
        .when(metadata)
        .addMetadata(any(), any(), anyList());

    EventAggregateService cachedService =
        new EventAggregateService(
            null,
            null,
            manager,
            null,
            null,
            planner,
            cache,
            security,
            mock(EventQueryValidator.class),
            metadata,
            mock(SchemeIdHandler.class));

    List<EventQueryParams> requests = new ArrayList<>();
    for (boolean dimension : List.of(true, false)) {
      for (OrganisationUnit ou : List.of(bo, bombali)) {
        EventQueryParams.Builder builder =
            new EventQueryParams.Builder()
                .withPeriods(List.of(PeriodDimension.of("2022")), "yearly");
        requests.add(
            (dimension
                    ? builder.withRegistrationOuDimension(List.of(ou))
                    : builder.withRegistrationOuFilter(List.of(ou)))
                .build());
      }
    }

    // Fetch each distinct response, then repeat the requests to exercise cache hits.
    for (int pass = 0; pass < 2; pass++) {
      for (EventQueryParams request : requests) {
        Grid grid = cachedService.getAggregatedData(new EventQueryParams.Builder(request).build());
        OrganisationUnit ou = request.getAllRegistrationOuItems().get(0);
        boolean dimension = request.hasRegistrationOuDimension();
        assertEquals(
            dimension ? List.of("pe", "registrationou", "value") : List.of("pe", "value"),
            grid.getHeaders().stream().map(GridHeader::getName).toList());
        assertEquals(
            List.of(
                dimension
                    ? List.of("2022", ou.getUid(), counts.get(ou.getUid()))
                    : List.of("2022", counts.get(ou.getUid()))),
            grid.getRows());
        assertEquals(
            Map.of(ou.getUid(), ou.getName()),
            grid.getMetaData().get(AnalyticsMetaDataKey.ITEMS.getKey()));
      }
    }

    verify(manager, times(4)).getAggregatedEventData(any(), any(), anyInt());
    verify(metadata, times(4)).addMetadata(any(), any(), anyList());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldExportRegistrationOuAsRowsOrColumns(boolean registrationRows) {
    OrganisationUnit bo = createOrganisationUnit('A');
    bo.setName("Bo");
    OrganisationUnit bombali = createOrganisationUnit('B');
    bombali.setName("Bombali");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withPeriods(List.of(PeriodDimension.of("2022")), "yearly")
            .withRegistrationOuDimension(List.of(bo, bombali))
            .withDisplayProperty(DisplayProperty.NAME)
            .build();
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader("pe"))
        .addHeader(new GridHeader("registrationou"))
        .addHeader(new GridHeader("value"));
    grid.addRow().addValue("2022").addValue(bo.getUid()).addValue(1d);
    grid.addRow().addValue("2022").addValue(bombali.getUid()).addValue(2d);

    Grid result =
        exportGrid(
            params,
            grid,
            List.of(registrationRows ? "pe" : "registrationou"),
            List.of(registrationRows ? "registrationou" : "pe"));

    assertEquals(
        registrationRows
            ? List.of("registrationou", "2022")
            : List.of("pe", "registrationou Bo", "registrationou Bombali"),
        result.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(
        registrationRows
            ? List.of(List.of("Bo", 1d), List.of("Bombali", 2d))
            : List.of(List.of("2022", 1d, 2d)),
        result.getRows());
  }

  @Test
  void shouldExportRegistrationOuAlongsideEventOu() {
    OrganisationUnit registrationOu = createOrganisationUnit('A');
    registrationOu.setName("Bo");
    OrganisationUnit eventOu = createOrganisationUnit('B');
    eventOu.setName("Bombali");
    EventQueryParams params =
        new EventQueryParams.Builder()
            .withPeriods(List.of(PeriodDimension.of("2022")), "yearly")
            .withOrganisationUnits(List.of(eventOu))
            .withRegistrationOuDimension(List.of(registrationOu))
            .withDisplayProperty(DisplayProperty.NAME)
            .build();
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader("pe"))
        .addHeader(new GridHeader("ou"))
        .addHeader(new GridHeader("registrationou"))
        .addHeader(new GridHeader("value"));
    grid.addRow()
        .addValue("2022")
        .addValue(eventOu.getUid())
        .addValue(registrationOu.getUid())
        .addValue(1d);

    Grid result = exportGrid(params, grid, List.of("pe"), List.of("ou", "registrationou"));

    assertEquals(
        List.of("ou", "registrationou", "2022"),
        result.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(List.of(List.of("Bombali", "Bo", 1d)), result.getRows());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void shouldExportOrdinaryDimensionsWithOrWithoutRegistrationOuFilter(boolean registrationFilter) {
    OrganisationUnit eventOu = createOrganisationUnit('B');
    eventOu.setName("Bombali");
    EventQueryParams.Builder builder =
        new EventQueryParams.Builder()
            .withPeriods(List.of(PeriodDimension.of("2022")), "yearly")
            .withOrganisationUnits(List.of(eventOu))
            .withDisplayProperty(DisplayProperty.NAME);
    if (registrationFilter) {
      builder.withRegistrationOuFilter(List.of(createOrganisationUnit('A')));
    }
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader("pe"))
        .addHeader(new GridHeader("ou"))
        .addHeader(new GridHeader("value"));
    grid.addRow().addValue("2022").addValue(eventOu.getUid()).addValue(1d);

    Grid result = exportGrid(builder.build(), grid, List.of("pe"), List.of("ou"));

    assertEquals(
        List.of("ou", "2022"), result.getHeaders().stream().map(GridHeader::getName).toList());
    assertEquals(List.of(List.of("Bombali", 1d)), result.getRows());
  }

  private Grid exportGrid(
      EventQueryParams params, Grid grid, List<String> columns, List<String> rows) {
    EventAggregateService exportService =
        spy(
            new EventAggregateService(
                mock(DataElementService.class),
                mock(TrackedEntityAttributeService.class),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    grid.getMetaData().put(AnalyticsMetaDataKey.ITEMS.getKey(), Map.of());
    doReturn(grid).when(exportService).getAggregatedData(params);

    return exportService.getAggregatedData(params, new ArrayList<>(columns), new ArrayList<>(rows));
  }

  private GridHeader invokeAddDimensionHeaders(EventQueryParams params) throws Exception {
    return invokeDimensionHeadersGrid(params).getHeaders().get(0);
  }

  private Grid invokeDimensionHeadersGrid(EventQueryParams params) throws Exception {
    Grid grid = new ListGrid();
    invokePrivate("addDimensionHeaders", EventQueryParams.class, Grid.class, params, grid);
    return grid;
  }

  private void invokePrivate(
      String methodName, Class<?> arg0Type, Class<?> arg1Type, Object arg0, Object arg1)
      throws Exception {
    Method method = EventAggregateService.class.getDeclaredMethod(methodName, arg0Type, arg1Type);
    method.setAccessible(true);
    method.invoke(service, arg0, arg1);
  }

  private void invokeAddEventReportDimensionalItems(
      ValueTypedDimensionalItemObject item,
      List<EventAnalyticsDimensionalItem> dimensionalItems,
      Grid grid,
      String dimension)
      throws Exception {
    Method method =
        EventAggregateService.class.getDeclaredMethod(
            "addEventReportDimensionalItems",
            ValueTypedDimensionalItemObject.class,
            List.class,
            Grid.class,
            String.class);
    method.setAccessible(true);
    method.invoke(service, item, dimensionalItems, grid, dimension);
  }

  private Grid invokeGenerateOutputGrid(
      Grid grid,
      EventQueryParams params,
      List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations,
      List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations,
      List<String> rowDimensions)
      throws Exception {
    Method method =
        EventAggregateService.class.getDeclaredMethod(
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

  private EventQueryParams singleDateFieldParams(String dateField, Program program) {
    PeriodDimension period = PeriodDimension.of("2021").setDateField(dateField);
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, "Period", List.of(period));

    return new EventQueryParams.Builder()
        .addDimension(periodDimension)
        .withProgram(program)
        .build();
  }

  private EventQueryParams orgUnitDimensionParams(Program program) {
    BaseDimensionalObject orgUnitDimension =
        new BaseDimensionalObject(
            ORGUNIT_DIM_ID, ORGANISATION_UNIT, "Organisation unit", List.of());

    return new EventQueryParams.Builder()
        .addDimension(orgUnitDimension)
        .withProgram(program)
        .build();
  }

  private EventQueryParams defaultPeriodParams() {
    PeriodDimension period = PeriodDimension.of("2021");
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject(PERIOD_DIM_ID, PERIOD, "Period", List.of(period));

    return new EventQueryParams.Builder().addDimension(periodDimension).build();
  }

  private EventQueryParams mixedPeriodDateFieldsParams() {
    PeriodDimension enrollmentDatePeriod =
        PeriodDimension.of("2021").setDateField("ENROLLMENT_DATE");
    PeriodDimension defaultPeriod = PeriodDimension.of("2022");
    BaseDimensionalObject periodDimension =
        new BaseDimensionalObject(
            PERIOD_DIM_ID, PERIOD, "Period", List.of(enrollmentDatePeriod, defaultPeriod));

    return new EventQueryParams.Builder().addDimension(periodDimension).build();
  }
}
