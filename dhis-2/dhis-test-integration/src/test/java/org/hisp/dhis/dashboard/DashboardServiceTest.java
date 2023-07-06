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
package org.hisp.dhis.dashboard;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.dashboard.DashboardItemType.EVENT_REPORT;
import static org.hisp.dhis.dashboard.DashboardItemType.EVENT_VISUALIZATION;
import static org.hisp.dhis.dashboard.DashboardItemType.VISUALIZATION;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.COLUMN;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.document.DocumentService;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventchart.EventChartService;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventreport.EventReportService;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardServiceTest extends TransactionalIntegrationTest {
  @Autowired private DashboardService dashboardService;

  @Autowired private VisualizationService visualizationService;

  @Autowired private EventVisualizationService eventVisualizationService;

  @Autowired private DocumentService documentService;

  @Autowired private EventChartService eventChartService;

  @Autowired private EventReportService eventReportService;

  @Autowired private IdentifiableObjectManager objectManager;

  private Dashboard dbA;

  private Dashboard dbB;

  private DashboardItem diA;

  private DashboardItem diB;

  private DashboardItem diC;

  private DashboardItem diD;

  private DashboardItem diE;

  private Visualization vzA;

  private Visualization vzB;

  private EventVisualization evzB;

  private Document dcA;

  @Override
  public void setUpTest() {
    vzA = createVisualization("A");
    vzB = createVisualization("B");
    Program program = createProgram('Y', null, null);
    objectManager.save(program);
    evzB = createEventVisualization("A", program);
    visualizationService.save(vzA);
    visualizationService.save(vzB);
    eventVisualizationService.save(evzB);
    dcA = new Document("A", "url", false, null);
    Document dcB = new Document("B", "url", false, null);
    Document dcC = new Document("C", "url", false, null);
    Document dcD = new Document("D", "url", false, null);
    documentService.saveDocument(dcA);
    documentService.saveDocument(dcB);
    documentService.saveDocument(dcC);
    documentService.saveDocument(dcD);
    List<String> allowedFilters = new ArrayList<>(asList("kJuHtg2gkh3", "yH7Yh2jGfFs"));
    diA = new DashboardItem();
    diA.setAutoFields();
    diA.setVisualization(vzA);
    diB = new DashboardItem();
    diB.setAutoFields();
    diB.setVisualization(vzB);
    diC = new DashboardItem();
    diC.setAutoFields();
    diC.getResources().add(dcA);
    diC.getResources().add(dcB);
    diD = new DashboardItem();
    diD.setAutoFields();
    diD.getResources().add(dcC);
    diD.getResources().add(dcD);
    diE = new DashboardItem();
    diE.setAutoFields();
    diE.setEventVisualization(evzB);
    dbA = new Dashboard("A");
    dbA.setAutoFields();
    dbA.getItems().add(diA);
    dbA.getItems().add(diB);
    dbA.getItems().add(diC);
    dbB = new Dashboard("B");
    dbB.setAutoFields();
    dbB.setRestrictFilters(true);
    dbB.setAllowedFilters(allowedFilters);
    dbB.getItems().add(diD);
    dbB.getItems().add(diE);
  }

  @Test
  void testAddGet() {
    long dAId = dashboardService.saveDashboard(dbA);
    long dBId = dashboardService.saveDashboard(dbB);
    assertEquals(dbA, dashboardService.getDashboard(dAId));
    assertEquals(dbB, dashboardService.getDashboard(dBId));
    assertEquals(2, dbB.getAllowedFilters().size());
    assertEquals(3, dashboardService.getDashboard(dAId).getItems().size());
    assertEquals(2, dashboardService.getDashboard(dBId).getItems().size());
  }

  @Test
  void testUpdate() {
    long dAId = dashboardService.saveDashboard(dbA);
    assertEquals("A", dashboardService.getDashboard(dAId).getName());
    dbA.setName("B");
    dashboardService.updateDashboard(dbA);
    assertEquals("B", dashboardService.getDashboard(dAId).getName());
  }

  @Test
  void testDelete() {
    // ## Ensuring the preparation for deletion
    // When saved
    long dAId = dashboardService.saveDashboard(dbA);
    long dBId = dashboardService.saveDashboard(dbB);
    // Then confirm that they were saved
    assertThatDashboardAndItemsArePersisted(dAId);
    assertThatDashboardAndItemsArePersisted(dBId);
    // ## Testing deletion
    // Given
    List<DashboardItem> itemsOfDashA = dashboardService.getDashboard(dAId).getItems();
    List<DashboardItem> itemsOfDashB = dashboardService.getDashboard(dBId).getItems();
    // When deleted
    dashboardService.deleteDashboard(dbA);
    dashboardService.deleteDashboard(dbB);
    // Then confirm that they were deleted
    assertDashboardAndItemsAreDeleted(dAId, itemsOfDashA);
    assertDashboardAndItemsAreDeleted(dBId, itemsOfDashB);
  }

  private void assertThatDashboardAndItemsArePersisted(long dashboardId) {
    Dashboard dashboard = dashboardService.getDashboard(dashboardId);
    assertNotNull(dashboard);
    List<DashboardItem> itemsA = dashboard.getItems();
    for (final DashboardItem dAItem : itemsA) {
      assertNotNull(
          dashboardService.getDashboardItem(dAItem.getUid()), "DashboardItem should exist");
    }
  }

  private void assertDashboardAndItemsAreDeleted(
      long dashboardId, List<DashboardItem> dashboardItems) {
    assertNull(dashboardService.getDashboard(dashboardId));
    // Assert that there are not items related to the given Dashboard
    for (DashboardItem item : dashboardItems) {
      assertNull(
          dashboardService.getDashboardItem(item.getUid()), "DashboardItem should not exist");
    }
  }

  @Test
  void testAddItemContent() {
    dashboardService.saveDashboard(dbA);
    dashboardService.saveDashboard(dbB);
    DashboardItem itemA =
        dashboardService.addItemContent(dbA.getUid(), VISUALIZATION, vzA.getUid());
    assertNotNull(itemA);
    assertNotNull(itemA.getUid());
  }

  @Test
  void testSearchDashboard() {
    dashboardService.saveDashboard(dbA);
    dashboardService.saveDashboard(dbB);
    DashboardSearchResult result = dashboardService.search("A");
    assertEquals(1, result.getVisualizationCount());
    assertEquals(1, result.getResourceCount());
    result = dashboardService.search("B");
    assertEquals(1, result.getVisualizationCount());
    assertEquals(1, result.getResourceCount());
    result = dashboardService.search("Z");
    assertEquals(0, result.getVisualizationCount());
    assertEquals(0, result.getResourceCount());
  }

  @Test
  void testSearchDashboardWithMaxCount() {
    Program prA = createProgram('A', null, null);
    objectManager.save(prA);

    range(1, 30)
        .forEach(
            i -> {
              Visualization visualization = createVisualization('A');
              visualization.setName(randomAlphabetic(5));
              visualizationService.save(visualization);
            });

    range(1, 30)
        .forEach(
            i -> {
              EventVisualization eventVisualization = createEventVisualization("A", prA);
              eventVisualization.setName(randomAlphabetic(5));
              eventVisualizationService.save(eventVisualization);
            });

    // Non Line List event visualization should be ignored when we search
    // for EVENT_VISUALIZATION:
    EventVisualization eventVisualization = createEventVisualization("A", prA);
    eventVisualization.setName(randomAlphabetic(5));
    eventVisualization.setType(COLUMN);
    eventVisualizationService.save(eventVisualization);

    range(1, 30).forEach(i -> eventChartService.saveEventChart(createEventChart(prA)));
    range(1, 20).forEach(i -> eventReportService.saveEventReport(createEventReport(prA)));

    DashboardSearchResult result = dashboardService.search(Set.of(VISUALIZATION));
    assertThat(result.getVisualizationCount(), is(25));
    assertThat(result.getEventChartCount(), is(6));

    result = dashboardService.search(Set.of(VISUALIZATION), 3, null);
    assertThat(result.getVisualizationCount(), is(25));
    assertThat(result.getEventChartCount(), is(3));

    result = dashboardService.search(Set.of(VISUALIZATION), 3, 29);
    assertThat(result.getVisualizationCount(), is(29));
    assertThat(result.getEventChartCount(), is(3));

    result = dashboardService.search(Set.of(EVENT_VISUALIZATION), 3, 29);
    assertThat(result.getEventVisualizationCount(), is(29));
    assertThat(result.getEventReportCount(), is(3));

    result = dashboardService.search(Set.of(EVENT_VISUALIZATION), 3, 30);
    assertThat(result.getEventVisualizationCount(), is(30));
    assertThat(result.getEventChartCount(), is(3));

    result = dashboardService.search(Set.of(EVENT_REPORT), 3, 29);
    assertThat(result.getEventVisualizationCount(), is(3));
    assertThat(result.getEventReportCount(), is(19));
  }

  private Visualization createVisualization(String name) {
    Visualization visualization = createVisualization('X');
    visualization.setName(name);
    return visualization;
  }

  private EventVisualization createEventVisualization(String name, Program program) {
    EventVisualization eventVisualization = createEventVisualization('X', program);
    eventVisualization.setName(name);
    return eventVisualization;
  }

  private EventChart createEventChart(Program program) {
    EventChart eventChart = new EventChart(randomAlphabetic(5));
    eventChart.setProgram(program);
    eventChart.setType(COLUMN);
    return eventChart;
  }

  private EventReport createEventReport(Program program) {
    EventReport eventReport = new EventReport(randomAlphabetic(5));
    eventReport.setProgram(program);
    eventReport.setType(PIVOT_TABLE);
    return eventReport;
  }
}
