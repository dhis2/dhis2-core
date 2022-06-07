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
package org.hisp.dhis.datastatistics;

import static org.hisp.dhis.setting.SettingKey.COUNT_PASSIVE_DASHBOARD_VIEWS_IN_USAGE_ANALYTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
class DataStatisticsEventStoreTest extends DhisSpringTest
{
    @Autowired
    private DataStatisticsEventStore dataStatisticsEventStore;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    private DataStatisticsEvent dse1;

    private DataStatisticsEvent dse2;

    private DataStatisticsEvent dse4;

    private DataStatisticsEvent dse5;

    private Dashboard dashboard1;

    private int dse1Id;

    private int dse2Id;

    private Date start;

    private Date end;

    private static final String DASHBOARD_UID = "dashboardid";

    @Override
    public void setUpTest()
    {
        end = getDate( 2016, 3, 21 );
        start = getDate( 2016, 3, 19 );
        Date endDate = getDate( 2016, 3, 20 );
        dse1 = new DataStatisticsEvent( DataStatisticsEventType.VISUALIZATION_VIEW, endDate, "Testuser" );
        dse2 = new DataStatisticsEvent( DataStatisticsEventType.EVENT_CHART_VIEW, endDate, "TestUser" );
        dse4 = new DataStatisticsEvent( DataStatisticsEventType.DASHBOARD_VIEW, endDate, "TestUser", DASHBOARD_UID );
        dse5 = new DataStatisticsEvent( DataStatisticsEventType.PASSIVE_DASHBOARD_VIEW, endDate, "TestUser",
            DASHBOARD_UID );
        dashboard1 = new Dashboard( "Dashboard1" );
        dashboard1.setUid( DASHBOARD_UID );
    }

    @Test
    void addDataStatisticsEventTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dse1Id = dse1.getId();
        dataStatisticsEventStore.save( dse2 );
        dse2Id = dse2.getId();
        assertTrue( dse1Id != 0 );
        assertTrue( dse2Id != 0 );
    }

    @Test
    void getDataStatisticsEventCountTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );
        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore
            .getDataStatisticsEventCount( start, end );
        // Test for 3 objects because TOTAL_VIEWS is always present
        assertEquals( 3, dsList.size() );
    }

    @Test
    void getDataStatisticsEventCountCorrectContentTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );
        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore
            .getDataStatisticsEventCount( start, end );
        double expected = 1.0;
        double firstActual = dsList.get( DataStatisticsEventType.VISUALIZATION_VIEW );
        double secondActual = dsList.get( DataStatisticsEventType.DASHBOARD_VIEW );
        assertEquals( expected, firstActual );
        assertEquals( expected, secondActual );
    }

    @Test
    void getDataStatisticsEventCountCorrectDatesTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );
        dataStatisticsEventStore.save( dse2 );
        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore
            .getDataStatisticsEventCount( start, end );
        // Test for 4 objects, because TOTAL_VIEW is always present
        assertEquals( 4, dsList.size() );
    }

    @Test
    void getDataStatisticsEventCountWrongDatesTest()
    {
        dataStatisticsEventStore.save( dse1 );
        dataStatisticsEventStore.save( dse4 );
        Map<DataStatisticsEventType, Double> dsList = dataStatisticsEventStore
            .getDataStatisticsEventCount( start, end );
        // Test for 3 objects because TOTAL_VIEW is always present
        assertEquals( 3, dsList.size() );
    }

    @Test
    void getFavoritesDataTest()
    {
        dataStatisticsEventStore.save( dse4 );
        dataStatisticsEventStore.save( dse5 );
        dashboardService.saveDashboard( dashboard1 );
        List<FavoriteStatistics> stats1 = dataStatisticsEventStore
            .getFavoritesData( DataStatisticsEventType.DASHBOARD_VIEW, 100, SortOrder.ASC, null );
        assertEquals( 1, stats1.size() );
        List<FavoriteStatistics> stats2 = dataStatisticsEventStore
            .getFavoritesData( DataStatisticsEventType.PASSIVE_DASHBOARD_VIEW, 100, SortOrder.ASC, null );
        assertEquals( 1, stats2.size() );
    }

    @Test
    void getFavoriteStatisticsTest()
    {
        dataStatisticsEventStore.save( dse4 );
        dataStatisticsEventStore.save( dse5 );
        FavoriteStatistics fs1 = dataStatisticsEventStore.getFavoriteStatistics( DASHBOARD_UID );
        assertEquals( 1, fs1.getViews() );
        systemSettingManager.saveSystemSetting( COUNT_PASSIVE_DASHBOARD_VIEWS_IN_USAGE_ANALYTICS, true );
        FavoriteStatistics fs2 = dataStatisticsEventStore.getFavoriteStatistics( DASHBOARD_UID );
        assertEquals( 2, fs2.getViews() );
    }
}
