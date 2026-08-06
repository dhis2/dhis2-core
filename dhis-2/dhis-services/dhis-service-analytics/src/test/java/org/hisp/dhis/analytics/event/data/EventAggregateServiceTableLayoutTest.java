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

import static java.util.Arrays.asList;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createPeriod;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.table.model.Partitions;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the table-layout path of {@link EventAggregateService} — an event aggregate request with
 * both {@code rows=} and {@code columns=}, which is the path that pivots the normalized analytics
 * grid into one output row per row-dimension permutation.
 *
 * <p>The value map used to fill those cells is a pure function of the input grid, so it is built
 * once per request rather than once per output row. These tests pin the output that hoisting must
 * not change: every permutation lands in the right cell, missing combinations come out {@code
 * null}, and a permutation with no values at all is dropped from the output entirely.
 */
@ExtendWith(MockitoExtension.class)
class EventAggregateServiceTableLayoutTest {
  @Mock private DataElementService dataElementService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private EventAnalyticsManager eventAnalyticsManager;

  @Mock private EnrollmentAnalyticsManager enrollmentAnalyticsManager;

  @Mock private EventDataQueryService eventDataQueryService;

  @Mock private EventQueryPlanner queryPlanner;

  @Mock private AnalyticsCache analyticsCache;

  @Mock private AnalyticsSecurityManager securityManager;

  @Mock private EventQueryValidator queryValidator;

  @Mock private MetadataItemsHandler metadataHandler;

  @Mock private SchemeIdHandler schemeIdHandler;

  @InjectMocks private EventAggregateService service;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private Period peA;

  private Period peB;

  @BeforeAll
  static void setUpSecurityContext() {
    injectSecurityContextNoSettings(new SystemUser());
  }

  /**
   * Three org units as rows and two periods as columns give six cells. Five of them have a value in
   * the input grid; the sixth (ouB / peB) does not and must come out as {@code null}.
   */
  @Test
  void testTableLayoutFillsEveryPermutationFromTheInputGrid() {
    EventQueryParams params = params();

    stubAggregatedData(
        params,
        row(ouA, peA, 1d),
        row(ouA, peB, 2d),
        row(ouB, peA, 3d),
        row(ouC, peA, 4d),
        row(ouC, peB, 5d));

    Grid outputGrid =
        service.getAggregatedData(
            params,
            new ArrayList<>(List.of(PERIOD_DIM_ID)),
            new ArrayList<>(List.of(ORGUNIT_DIM_ID)));

    // One header for the row dimension, then one per column permutation.
    assertEquals(
        List.of("organisationunit", peA.getShortName(), peB.getShortName()),
        outputGrid.getHeaders().stream().map(GridHeader::getColumn).toList());

    assertEquals(
        List.of(
            asList(ouA.getShortName(), 1d, 2d),
            asList(ouB.getShortName(), 3d, null),
            asList(ouC.getShortName(), 4d, 5d)),
        outputGrid.getRows());
  }

  /**
   * A row permutation with no value in any column is removed from the output. ouB has no values at
   * all here, so only ouA and ouC survive.
   */
  @Test
  void testTableLayoutDropsRowPermutationsWithNoValues() {
    EventQueryParams params = params();

    stubAggregatedData(params, row(ouA, peA, 1d), row(ouC, peB, 5d));

    Grid outputGrid =
        service.getAggregatedData(
            params,
            new ArrayList<>(List.of(PERIOD_DIM_ID)),
            new ArrayList<>(List.of(ORGUNIT_DIM_ID)));

    assertEquals(
        List.of(asList(ouA.getShortName(), 1d, null), asList(ouC.getShortName(), null, 5d)),
        outputGrid.getRows());
  }

  /**
   * When the input grid is empty every row permutation is dropped, and the service falls back to
   * returning the input grid rather than an output grid with headers and no rows.
   */
  @Test
  void testTableLayoutFallsBackToTheInputGridWhenThereAreNoValues() {
    EventQueryParams params = params();

    stubAggregatedData(params);

    Grid outputGrid =
        service.getAggregatedData(
            params,
            new ArrayList<>(List.of(PERIOD_DIM_ID)),
            new ArrayList<>(List.of(ORGUNIT_DIM_ID)));

    assertEquals(List.of(), outputGrid.getRows());
    assertEquals(
        List.of(PERIOD_DIM_ID, ORGUNIT_DIM_ID, "value"),
        outputGrid.getHeaders().stream().map(GridHeader::getName).toList());
  }

  private EventQueryParams params() {
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    peA = createPeriod("202401");
    peB = createPeriod("202402");

    Program program = createProgram('A', null, null, Sets.newHashSet(ouA, ouB, ouC), null);

    return new EventQueryParams.Builder()
        .withPeriods(getList(peA, peB), "monthly")
        .withOrganisationUnits(getList(ouA, ouB, ouC))
        .withPartitions(new Partitions())
        .withProgram(program)
        .build();
  }

  private List<Object> row(OrganisationUnit orgUnit, Period period, Double value) {
    return asList(orgUnit.getDimensionItem(), period.getDimensionItem(), value);
  }

  /**
   * Stubs the collaborators of {@link EventAggregateService#getAggregatedData(EventQueryParams)} so
   * that it produces a normalized grid with the given rows, plus the metadata items the table
   * layout reads to name its row headers.
   */
  @SafeVarargs
  private void stubAggregatedData(EventQueryParams params, List<Object>... rows) {
    when(securityManager.withUserConstraints(any(EventQueryParams.class))).thenReturn(params);
    when(analyticsCache.isEnabled()).thenReturn(false);
    when(queryValidator.getMaxLimit()).thenReturn(0);
    when(queryPlanner.planAggregateQuery(any(EventQueryParams.class))).thenReturn(List.of(params));

    doAnswer(
            invocation -> {
              Grid grid = invocation.getArgument(1);
              for (List<Object> row : rows) {
                grid.addRow().addValuesAsList(row);
              }
              return null;
            })
        .when(eventAnalyticsManager)
        .getAggregatedEventData(any(EventQueryParams.class), any(Grid.class), anyInt());

    doAnswer(
            invocation -> {
              Grid grid = invocation.getArgument(0);
              Map<String, Object> items = new HashMap<>();
              items.put(ORGUNIT_DIM_ID, new MetadataItem("Organisation unit"));
              items.put(PERIOD_DIM_ID, new MetadataItem("Period"));
              grid.addMetaData(ITEMS.getKey(), items);
              return null;
            })
        .when(metadataHandler)
        .addMetadata(any(Grid.class), any(EventQueryParams.class), any());
  }
}
