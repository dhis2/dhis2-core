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
package org.hisp.dhis.webapi.controller.dataintegrity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.hisp.dhis.visualization.VisualizationType;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegrityCheck;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for dashboards which do not have any content {@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/analytical_objects/dashboards_empty.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDashboardsEmptyControllerTest extends AbstractDataIntegrityIntegrationTest {

  @Autowired private DashboardService dashboardService;

  @Autowired private VisualizationService visualizationService;

  private static final String check = "dashboards_no_items";

  private static final String dashboard_uid = BASE_UID + "1";

  private static final String viz_uid = BASE_UID + "2";

  private static final String detailsIdType = "dashboards";

  @Test
  void testUnusedDashboardExist() {

    Dashboard dashboardA = new Dashboard();
    dashboardA.setName("Test Dashboard");
    dashboardA.setUid(dashboard_uid);
    dashboardService.saveDashboard(dashboardA);
    dbmsManager.clearSession();

    JsonDataIntegrityCheck thisCheck =
        GET("/dataIntegrity/?checks=" + check)
            .content()
            .asList(JsonDataIntegrityCheck.class)
            .get(0);
    String detailsType = thisCheck.getIssuesIdType();
    assertEquals(detailsIdType, detailsType);

    assertNamedMetadataObjectExists(detailsIdType, "Test Dashboard");
    assertHasDataIntegrityIssues(
        detailsIdType, check, 100, dashboard_uid, "Test Dashboard", null, true);
  }

  @Test
  void testDashboardsWithItemsExist() {

    Visualization viz = new Visualization("myviz");
    viz.setUid(viz_uid);
    viz.setType(VisualizationType.SINGLE_VALUE);
    visualizationService.save(viz);

    DashboardItem diA = new DashboardItem();
    diA.setAutoFields();
    diA.setVisualization(viz);

    Dashboard dashboardA = new Dashboard();
    dashboardA.setName("Test Dashboard");
    dashboardA.setUid(dashboard_uid);
    dashboardA.setAutoFields();
    dashboardA.getItems().add(diA);
    dashboardService.saveDashboard(dashboardA);
    dbmsManager.clearSession();

    assertHasNoDataIntegrityIssues(detailsIdType, check, true);
  }

  @Test
  void testUnusedDashboardsRuns() {
    assertHasNoDataIntegrityIssues(detailsIdType, check, false);
  }
}
