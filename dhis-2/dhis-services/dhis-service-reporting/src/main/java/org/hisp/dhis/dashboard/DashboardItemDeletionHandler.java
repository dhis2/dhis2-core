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

import lombok.AllArgsConstructor;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@AllArgsConstructor
public class DashboardItemDeletionHandler extends DeletionHandler {
  private final DashboardService dashboardService;

  @Override
  protected void register() {
    whenDeleting(Visualization.class, this::deleteVisualization);
    whenDeleting(EventVisualization.class, this::deleteEventVisualization);
    whenDeleting(EventChart.class, this::deleteEventChart);
    whenDeleting(Map.class, this::deleteMap);
    whenDeleting(EventReport.class, this::deleteEventReport);
    whenDeleting(User.class, this::deleteUser);
    whenDeleting(Report.class, this::deleteReport);
    whenDeleting(Document.class, this::deleteDocument);
  }

  private void deleteVisualization(Visualization visualization) {
    for (DashboardItem item : dashboardService.getVisualizationDashboardItems(visualization)) {
      dashboardService.deleteDashboardItem(item);
    }
  }

  private void deleteEventVisualization(EventVisualization eventVisualization) {
    for (DashboardItem item :
        dashboardService.getEventVisualizationDashboardItems(eventVisualization)) {
      dashboardService.deleteDashboardItem(item);
    }
  }

  private void deleteEventChart(EventChart eventChart) {
    for (DashboardItem item : dashboardService.getEventChartDashboardItems(eventChart)) {
      dashboardService.deleteDashboardItem(item);
    }
  }

  private void deleteMap(Map map) {
    for (DashboardItem item : dashboardService.getMapDashboardItems(map)) {
      dashboardService.deleteDashboardItem(item);
    }
  }

  private void deleteEventReport(EventReport eventReport) {
    for (DashboardItem item : dashboardService.getEventReportDashboardItems(eventReport)) {
      dashboardService.deleteDashboardItem(item);
    }
  }

  private void deleteUser(User user) {
    for (DashboardItem item : dashboardService.getUserDashboardItems(user)) {
      while (item.getUsers().contains(user)) // In case of duplicates
      {
        item.getUsers().remove(user);
      }

      if (item.getUsers().isEmpty()) {
        dashboardService.deleteDashboardItem(item);
      }
    }
  }

  private void deleteReport(Report report) {
    for (DashboardItem item : dashboardService.getReportDashboardItems(report)) {
      while (item.getReports().contains(report)) // In case of
      // duplicates
      {
        item.getReports().remove(report);
      }

      if (item.getReports().isEmpty()) {
        dashboardService.deleteDashboardItem(item);
      }
    }
  }

  private void deleteDocument(Document document) {
    for (DashboardItem item : dashboardService.getDocumentDashboardItems(document)) {
      while (item.getResources().contains(document)) // In case of
      // duplicates
      {
        item.getResources().remove(document);
      }

      if (item.getResources().isEmpty()) {
        dashboardService.deleteDashboardItem(item);
      }
    }
  }
}
