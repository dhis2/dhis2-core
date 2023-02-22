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

import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.common.Dhis2Info;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.eventvisualization.EventVisualizationStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserInvitationStatus;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.visualization.Visualization;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.datastatistics.DataStatisticsService" )
@Transactional
public class DefaultDataStatisticsService
    implements DataStatisticsService
{
    private final DataStatisticsStore dataStatisticsStore;

    private final DataStatisticsEventStore dataStatisticsEventStore;

    private final UserService userService;

    private final IdentifiableObjectManager idObjectManager;

    private final DataValueService dataValueService;

    private final StatisticsProvider statisticsProvider;

    private final ProgramStageInstanceService programStageInstanceService;

    private final EventVisualizationStore eventVisualizationStore;

    private final SystemService systemService;

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

    private DataStatistics getDataStatisticsSnapshot( Date day, JobProgress progress )
    {
        Date startDate = getStartDate( day );
        int days = getDays( startDate );

        // when counting fails we use null so the count does not appear in the
        // stats
        Integer errorValue = null;
        progress.startingStage( "Counting maps", SKIP_STAGE );
        Integer savedMaps = progress.runStage( errorValue,
            () -> idObjectManager.getCountByCreated( org.hisp.dhis.mapping.Map.class, startDate ) );
        progress.startingStage( "Counting visualisations", SKIP_STAGE );
        Integer savedVisualizations = progress.runStage( errorValue,
            () -> idObjectManager.getCountByCreated( Visualization.class, startDate ) );
        progress.startingStage( "Counting event reports", SKIP_STAGE );
        Integer savedEventReports = progress.runStage( errorValue,
            () -> eventVisualizationStore.countReportsCreated( startDate ) );
        progress.startingStage( "Counting event charts", SKIP_STAGE );
        Integer savedEventCharts = progress.runStage( errorValue,
            () -> eventVisualizationStore.countChartsCreated( startDate ) );
        progress.startingStage( "Counting event visualisations", SKIP_STAGE );
        Integer savedEventVisualizations = progress.runStage( errorValue,
            () -> eventVisualizationStore.countEventVisualizationsCreated( startDate ) );
        progress.startingStage( "Counting dashboards", SKIP_STAGE );
        Integer savedDashboards = progress.runStage( errorValue,
            () -> idObjectManager.getCountByCreated( Dashboard.class, startDate ) );
        progress.startingStage( "Counting indicators", SKIP_STAGE );
        Integer savedIndicators = progress.runStage( errorValue,
            () -> idObjectManager.getCountByCreated( Indicator.class, startDate ) );
        progress.startingStage( "Counting data values", SKIP_STAGE );
        Integer savedDataValues = progress.runStage( errorValue,
            () -> dataValueService.getDataValueCount( days ) );
        progress.startingStage( "Counting active users", SKIP_STAGE );
        Integer activeUsers = progress.runStage( errorValue,
            () -> userService.getActiveUsersCount( 1 ) );
        progress.startingStage( "Counting users", SKIP_STAGE );
        Integer users = progress.runStage( errorValue,
            () -> idObjectManager.getCount( User.class ) );
        progress.startingStage( "Counting views", SKIP_STAGE );
        Map<DataStatisticsEventType, Double> eventCountMap = progress.runStage( Map.of(),
            () -> dataStatisticsEventStore.getDataStatisticsEventCount( startDate, day ) );

        return new DataStatistics(
            eventCountMap.get( DataStatisticsEventType.MAP_VIEW ),
            eventCountMap.get( DataStatisticsEventType.VISUALIZATION_VIEW ),
            eventCountMap.get( DataStatisticsEventType.EVENT_REPORT_VIEW ),
            eventCountMap.get( DataStatisticsEventType.EVENT_CHART_VIEW ),
            eventCountMap.get( DataStatisticsEventType.EVENT_VISUALIZATION_VIEW ),
            eventCountMap.get( DataStatisticsEventType.DASHBOARD_VIEW ),
            eventCountMap.get( DataStatisticsEventType.PASSIVE_DASHBOARD_VIEW ),
            eventCountMap.get( DataStatisticsEventType.DATA_SET_REPORT_VIEW ),
            eventCountMap.get( DataStatisticsEventType.TOTAL_VIEW ),
            asDouble( savedMaps ), asDouble( savedVisualizations ), asDouble( savedEventReports ),
            asDouble( savedEventCharts ), asDouble( savedEventVisualizations ), asDouble( savedDashboards ),
            asDouble( savedIndicators ), asDouble( savedDataValues ), activeUsers, users );
    }

    @Override
    public long saveDataStatistics( DataStatistics dataStatistics )
    {
        dataStatisticsStore.save( dataStatistics );

        return dataStatistics.getId();
    }

    @Override
    public long saveDataStatisticsSnapshot( JobProgress progress )
    {
        return saveDataStatistics( getDataStatisticsSnapshot( new Date(), progress ) );
    }

    @Override
    public List<FavoriteStatistics> getTopFavorites( DataStatisticsEventType eventType, int pageSize,
        SortOrder sortOrder, String username )
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

        // Database objects
        Map<String, Long> objectCounts = new HashMap<>();
        statisticsProvider.getObjectCounts()
            .forEach( ( object, count ) -> objectCounts.put( object.getValue(), count ) );

        statistics.setObjectCounts( objectCounts );

        // Active users
        Date lastHour = new DateTime().minusHours( 1 ).toDate();

        Map<Integer, Integer> activeUsers = new HashMap<>();

        activeUsers.put( 0, userService.getActiveUsersCount( lastHour ) );
        activeUsers.put( 1, userService.getActiveUsersCount( 0 ) );
        activeUsers.put( 2, userService.getActiveUsersCount( 1 ) );
        activeUsers.put( 7, userService.getActiveUsersCount( 7 ) );
        activeUsers.put( 30, userService.getActiveUsersCount( 30 ) );

        statistics.setActiveUsers( activeUsers );

        // User invitations
        Map<String, Integer> userInvitations = new HashMap<>();

        UserQueryParams inviteAll = new UserQueryParams();
        inviteAll.setInvitationStatus( UserInvitationStatus.ALL );
        userInvitations.put( UserInvitationStatus.ALL.getValue(), userService.getUserCount( inviteAll ) );

        UserQueryParams inviteExpired = new UserQueryParams();
        inviteExpired.setInvitationStatus( UserInvitationStatus.EXPIRED );
        userInvitations.put( UserInvitationStatus.EXPIRED.getValue(), userService.getUserCount( inviteExpired ) );

        statistics.setUserInvitations( userInvitations );

        // Data values
        Map<Integer, Integer> dataValueCount = new HashMap<>();

        dataValueCount.put( 0, dataValueService.getDataValueCount( 0 ) );
        dataValueCount.put( 1, dataValueService.getDataValueCount( 1 ) );
        dataValueCount.put( 7, dataValueService.getDataValueCount( 7 ) );
        dataValueCount.put( 30, dataValueService.getDataValueCount( 30 ) );

        statistics.setDataValueCount( dataValueCount );

        // Events
        Map<Integer, Long> eventCount = new HashMap<>();

        eventCount.put( 0, programStageInstanceService.getProgramStageInstanceCount( 0 ) );
        eventCount.put( 1, programStageInstanceService.getProgramStageInstanceCount( 1 ) );
        eventCount.put( 7, programStageInstanceService.getProgramStageInstanceCount( 7 ) );
        eventCount.put( 30, programStageInstanceService.getProgramStageInstanceCount( 30 ) );

        statistics.setEventCount( eventCount );

        statistics.setSystem( getDhis2Info() );

        return statistics;
    }

    private Date getStartDate( Date day )
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime( day );
        cal.add( Calendar.DATE, -1 );
        return cal.getTime();
    }

    private int getDays( Date startDate )
    {
        Date now = new Date();
        long diff = now.getTime() - startDate.getTime();
        return (int) TimeUnit.DAYS.convert( diff, TimeUnit.MILLISECONDS );
    }

    private Double asDouble( Integer count )
    {
        return count == null ? null : count.doubleValue();
    }

    private Dhis2Info getDhis2Info()
    {
        SystemInfo system = systemService.getSystemInfo();

        return new Dhis2Info()
            .setVersion( system.getVersion() )
            .setRevision( system.getRevision() )
            .setBuildTime( system.getBuildTime() )
            .setSystemId( system.getSystemId() )
            .setServerDate( system.getServerDate() );
    }
}
