package org.hisp.dhis.datastatistics;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.analytics.SortOrder;

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@Transactional
public class DefaultDataStatisticsService
    implements DataStatisticsService
{
    @Autowired
    private DataStatisticsStore dataStatisticsStore;

    @Autowired
    private DataStatisticsEventStore dataStatisticsEventStore;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataValueService dataValueService;

    // -------------------------------------------------------------------------
    // DataStatisticsService implementation
    // -------------------------------------------------------------------------

    @Override
    public int addEvent( DataStatisticsEvent event )
    {
        dataStatisticsEventStore.save( event );

        return event.getId();
    }

    @Override
    public List<AggregatedStatistics> getReports( Date startDate, Date endDate, EventInterval eventInterval )
    {
        return dataStatisticsStore.getSnapshotsInInterval( eventInterval, startDate, endDate );
    }

    @Override
    public DataStatistics getDataStatisticsSnapshot( Date day )
    {        
        Calendar cal = Calendar.getInstance();
        cal.setTime( day );
        cal.add( Calendar.DATE, -1 );
        Date startDate = cal.getTime();
        Date now = new Date();
        long diff = now.getTime() - startDate.getTime();
        int days = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

        double savedMaps = idObjectManager.getCountByCreated( org.hisp.dhis.mapping.Map.class, startDate );
        double savedCharts = idObjectManager.getCountByCreated( Chart.class, startDate );
        double savedReportTables = idObjectManager.getCountByCreated( ReportTable.class, startDate );
        double savedEventReports = idObjectManager.getCountByCreated( EventReport.class, startDate );
        double savedEventCharts = idObjectManager.getCountByCreated( EventChart.class, startDate );
        double savedDashboards = idObjectManager.getCountByCreated( Dashboard.class, startDate );
        double savedIndicators = idObjectManager.getCountByCreated( Indicator.class, startDate );
        double savedDataValues = dataValueService.getDataValueCount( days );
        int activeUsers = userService.getActiveUsersCount( 1 );
        int users = idObjectManager.getCount( User.class );

        Map<DataStatisticsEventType, Double> eventCountMap = dataStatisticsEventStore.getDataStatisticsEventCount( startDate, day );

        DataStatistics dataStatistics = new DataStatistics( 
            eventCountMap.get( DataStatisticsEventType.MAP_VIEW ),
            eventCountMap.get( DataStatisticsEventType.CHART_VIEW ),
            eventCountMap.get( DataStatisticsEventType.REPORT_TABLE_VIEW ),
            eventCountMap.get( DataStatisticsEventType.EVENT_REPORT_VIEW ),
            eventCountMap.get( DataStatisticsEventType.EVENT_CHART_VIEW ),
            eventCountMap.get( DataStatisticsEventType.DASHBOARD_VIEW ),
            eventCountMap.get( DataStatisticsEventType.DATA_SET_REPORT_VIEW ),
            eventCountMap.get( DataStatisticsEventType.TOTAL_VIEW ),
            savedMaps, savedCharts, savedReportTables, savedEventReports,
            savedEventCharts, savedDashboards, savedIndicators, savedDataValues, activeUsers, users );
        
        return dataStatistics;
    }

    @Override
    public int saveDataStatistics( DataStatistics dataStatistics )
    {
        dataStatisticsStore.save( dataStatistics );

        return dataStatistics.getId();
    }

    @Override
    public int saveDataStatisticsSnapshot()
    {
        return saveDataStatistics( getDataStatisticsSnapshot( new Date() ) );
    }

    @Override
    public List<FavoriteStatistics> getTopFavorites( DataStatisticsEventType eventType, int pageSize, SortOrder sortOrder, String username )
    {
        return dataStatisticsEventStore.getFavoritesData( eventType, pageSize, sortOrder, username );
    }

    @Override
    public FavoriteStatistics getFavoriteStatistics( String uid )
    {
        return dataStatisticsEventStore.getFavoriteStatistics( uid );
    }
}
