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

import java.time.ZonedDateTime;
import java.util.Date;

import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastatistics.DataStatisticsEventStore;
import org.hisp.dhis.datastatistics.DataStatisticsEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * Tests for dashboards which have not been actively viewed in the past year.*
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/analytical_objects/dashboards_not_used_1year.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataIntegrityDashboardsNotUsedOneYearControllerTest extends AbstractDataIntegrityIntegrationTest
{

    @Autowired
    private DataStatisticsEventStore dataStatisticsEventStore;

    @Autowired
    private DashboardService dashboardService;

    private DataStatisticsEvent dse1;

    private static final String check = "dashboards_not_viewed_one_year";

    private static final String detailsIdType = "dashboards";

    @Test
    void testUnusedDashboardExist()
    {

        setUpDashboards();
        Date date = Date.from( ZonedDateTime.now().minusYears( 1 ).minusDays( 1 ).toInstant() );

        dse1 = new DataStatisticsEvent( DataStatisticsEventType.DASHBOARD_VIEW, date, "TestUser", BASE_UID );
        dataStatisticsEventStore.save( dse1 );

        dbmsManager.clearSession();

        assertNamedMetadataObjectExists( detailsIdType, "Test Dashboard" );
        assertHasDataIntegrityIssues( detailsIdType, check, 100, BASE_UID, "Test Dashboard", null, true );
    }

    @Test
    void testUsedDashboardsExist()
    {

        setUpDashboards();
        long millis = System.currentTimeMillis();
        Date date = new Date( millis );

        dse1 = new DataStatisticsEvent( DataStatisticsEventType.DASHBOARD_VIEW, date, "TestUser", BASE_UID );
        dataStatisticsEventStore.save( dse1 );

        dbmsManager.clearSession();

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );
    }

    @Test
    void testUnusedDashboardsRuns()
    {
        assertHasNoDataIntegrityIssues( detailsIdType, check, false );
    }

    void setUpDashboards()
    {
        Dashboard dashboardA = new Dashboard();
        dashboardA.setName( "Test Dashboard" );
        dashboardA.setUid( BASE_UID );
        dashboardService.saveDashboard( dashboardA );

    }
}
