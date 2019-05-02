package org.hisp.dhis.datastatistics;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    @Autowired
    private StatisticsProvider statisticsProvider;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

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
    public long saveDataStatistics( DataStatistics dataStatistics )
    {
        dataStatisticsStore.save( dataStatistics );

        return dataStatistics.getId();
    }

    @Override
    public long saveDataStatisticsSnapshot()
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

    @Override
    public DataSummary getSystemStatisticsSummary()
    {
        DataSummary statistics = new DataSummary();

        /* database object counts */
        Map<String, Integer> objectCounts = new HashMap<>(  );
        statisticsProvider.getObjectCounts().forEach( (object, count) -> objectCounts.put( object.getValue(), count ));
        statistics.setObjectCounts( objectCounts );

        /* active users count */
        Date lastHour = new DateTime().minusHours( 1 ).toDate();

        Map<Integer, Integer> activeUsers = new HashMap<>(  );

        activeUsers.put( 0,  userService.getActiveUsersCount( lastHour ));
        activeUsers.put( 1,  userService.getActiveUsersCount( 0 ));
        activeUsers.put( 2,  userService.getActiveUsersCount( 1 ));
        activeUsers.put( 7,  userService.getActiveUsersCount( 7 ));
        activeUsers.put( 30,  userService.getActiveUsersCount( 30 ));

        statistics.setActiveUsers( activeUsers );

        /* user invitations count */
        Map<String, Integer> userInvitations = new HashMap<>(  );

        UserQueryParams inviteAll = new UserQueryParams();
        inviteAll.setInvitationStatus( UserInvitationStatus.ALL );
        userInvitations.put( UserInvitationStatus.ALL.getValue(),  userService.getUserCount( inviteAll ) );

        UserQueryParams inviteExpired = new UserQueryParams();
        inviteExpired.setInvitationStatus( UserInvitationStatus.EXPIRED );
        userInvitations.put( UserInvitationStatus.EXPIRED.getValue(),  userService.getUserCount( inviteExpired ) );

        statistics.setUserInvitations( userInvitations );

        /* data value count */
        Map<Integer, Integer> dataValueCount = new HashMap<>(  );

        dataValueCount.put( 0, dataValueService.getDataValueCount( 0 ));
        dataValueCount.put( 1, dataValueService.getDataValueCount( 1 ));
        dataValueCount.put( 7, dataValueService.getDataValueCount( 7 ));
        dataValueCount.put( 30, dataValueService.getDataValueCount( 30 ));

        statistics.setDataValueCount( dataValueCount );

        /* event count */
        Map<Integer, Long> eventCount = new HashMap<>(  );

        eventCount.put( 0, programStageInstanceService.getProgramStageInstanceCount( 0 ) );
        eventCount.put( 1, programStageInstanceService.getProgramStageInstanceCount( 1 ) );
        eventCount.put( 7, programStageInstanceService.getProgramStageInstanceCount( 7 ) );
        eventCount.put( 30, programStageInstanceService.getProgramStageInstanceCount( 30 ) );

        statistics.setEventCount( eventCount );

        return statistics;
    }
}
