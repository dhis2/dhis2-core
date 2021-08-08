/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dashboard.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppType;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemStore;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.dashboard.DashboardSearchResult;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.report.Report;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.visualization.SimpleVisualizationView;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationStore;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * Note: The remove associations methods must be altered if caching is
 * introduced.
 *
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.dashboard.DashboardService" )
public class DefaultDashboardService
    implements DashboardService
{
    private static final int HITS_PER_OBJECT = 6;

    private static final int MAX_HITS_PER_OBJECT = 25;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final HibernateIdentifiableObjectStore<Dashboard> dashboardStore;

    private final IdentifiableObjectManager objectManager;

    private final UserService userService;

    private final DashboardItemStore dashboardItemStore;

    private final VisualizationStore visualizationStore;

    private final AppManager appManager;

    public DefaultDashboardService(
        @Qualifier( "org.hisp.dhis.dashboard.DashboardStore" ) HibernateIdentifiableObjectStore<Dashboard> dashboardStore,
        IdentifiableObjectManager objectManager, UserService userService, DashboardItemStore dashboardItemStore,
        VisualizationStore visualizationStore, AppManager appManager )
    {
        checkNotNull( dashboardStore );
        checkNotNull( objectManager );
        checkNotNull( userService );
        checkNotNull( dashboardItemStore );
        checkNotNull( visualizationStore );
        checkNotNull( appManager );

        this.dashboardStore = dashboardStore;
        this.objectManager = objectManager;
        this.userService = userService;
        this.dashboardItemStore = dashboardItemStore;
        this.visualizationStore = visualizationStore;
        this.appManager = appManager;
    }

    // -------------------------------------------------------------------------
    // DashboardService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public DashboardSearchResult search( String query )
    {
        return search( query, new HashSet<>(), null, null );
    }

    @Override
    @Transactional( readOnly = true )
    public DashboardSearchResult search( String query, Set<DashboardItemType> maxTypes, Integer count,
        Integer maxCount )
    {
        Set<String> words = Sets.newHashSet( query.split( TextUtils.SPACE ) );

        List<App> dashboardApps = appManager.getAppsByType( AppType.DASHBOARD_WIDGET,
            new HashSet<>( appManager.getApps( null ) ) );

        DashboardSearchResult result = new DashboardSearchResult();

        result.setUsers( userService.getAllUsersBetweenByName( query, 0,
            getMax( DashboardItemType.USERS, maxTypes, count, maxCount ) ) );
        result.setVisualizations( convertFrom( visualizationStore.getAllLikeName( words, 0,
            getMax( DashboardItemType.VISUALIZATION, maxTypes, count, maxCount ) ) ) );
        result.setCharts( visualizationStore.getChartsLikeName( words, 0,
            getMax( DashboardItemType.VISUALIZATION, maxTypes, count, maxCount ) ) );
        result.setEventCharts( objectManager.getBetweenLikeName( EventChart.class, words, 0,
            getMax( DashboardItemType.EVENT_CHART, maxTypes, count, maxCount ) ) );
        result.setMaps( objectManager.getBetweenLikeName( Map.class, words, 0,
            getMax( DashboardItemType.MAP, maxTypes, count, maxCount ) ) );
        result.setReportTables( visualizationStore.getPivotTablesLikeName( words, 0,
            getMax( DashboardItemType.VISUALIZATION, maxTypes, count, maxCount ) ) );
        result.setEventReports( objectManager.getBetweenLikeName( EventReport.class, words, 0,
            getMax( DashboardItemType.EVENT_REPORT, maxTypes, count, maxCount ) ) );
        result.setReports( objectManager.getBetweenLikeName( Report.class, words, 0,
            getMax( DashboardItemType.REPORTS, maxTypes, count, maxCount ) ) );
        result.setResources( objectManager.getBetweenLikeName( Document.class, words, 0,
            getMax( DashboardItemType.RESOURCES, maxTypes, count, maxCount ) ) );
        result.setApps( appManager.getAppsByName( query, dashboardApps, "ilike" ) );

        return result;
    }

    @Override
    @Transactional( readOnly = true )
    public DashboardSearchResult search( Set<DashboardItemType> maxTypes, Integer count, Integer maxCount )
    {
        DashboardSearchResult result = new DashboardSearchResult();

        result.setVisualizations( convertFrom( objectManager.getBetweenSorted( Visualization.class, 0,
            getMax( DashboardItemType.VISUALIZATION, maxTypes, count, maxCount ) ) ) );
        result.setCharts(
            visualizationStore.getCharts( 0, getMax( DashboardItemType.VISUALIZATION, maxTypes, count, maxCount ) ) );
        result.setEventCharts( objectManager.getBetweenSorted( EventChart.class, 0,
            getMax( DashboardItemType.EVENT_CHART, maxTypes, count, maxCount ) ) );
        result.setMaps( objectManager.getBetweenSorted( Map.class, 0,
            getMax( DashboardItemType.MAP, maxTypes, count, maxCount ) ) );
        result.setReportTables( visualizationStore.getPivotTables( 0,
            getMax( DashboardItemType.VISUALIZATION, maxTypes, count, maxCount ) ) );
        result.setEventReports( objectManager.getBetweenSorted( EventReport.class, 0,
            getMax( DashboardItemType.EVENT_REPORT, maxTypes, count, maxCount ) ) );
        result.setReports( objectManager.getBetweenSorted( Report.class, 0,
            getMax( DashboardItemType.REPORTS, maxTypes, count, maxCount ) ) );
        result.setResources( objectManager.getBetweenSorted( Document.class, 0,
            getMax( DashboardItemType.RESOURCES, maxTypes, count, maxCount ) ) );
        result.setApps( appManager.getApps( AppType.DASHBOARD_WIDGET,
            getMax( DashboardItemType.APP, maxTypes, count, maxCount ) ) );

        return result;
    }

    @Override
    @Transactional( readOnly = true )
    public DashboardSearchResult search( String query, Set<DashboardItemType> maxTypes )
    {
        return search( query, maxTypes, null, null );
    }

    @Override
    @Transactional( readOnly = true )
    public DashboardSearchResult search( Set<DashboardItemType> maxTypes )
    {
        return search( maxTypes, null, null );
    }

    @Override
    @Transactional( readOnly = true )
    public DashboardItem addItemContent( String dashboardUid, DashboardItemType type, String contentUid )
    {
        Dashboard dashboard = getDashboard( dashboardUid );

        if ( dashboard == null )
        {
            return null;
        }

        DashboardItem item = new DashboardItem();

        if ( DashboardItemType.VISUALIZATION.equals( type ) )
        {
            item.setVisualization( objectManager.get( Visualization.class, contentUid ) );
            dashboard.getItems().add( 0, item );
        }
        else if ( DashboardItemType.CHART.equals( type ) )
        {
            item.setVisualization( objectManager.get( Visualization.class, contentUid ) );
            dashboard.getItems().add( 0, item );
        }
        else if ( DashboardItemType.EVENT_CHART.equals( type ) )
        {
            item.setEventChart( objectManager.get( EventChart.class, contentUid ) );
            dashboard.getItems().add( 0, item );
        }
        else if ( DashboardItemType.MAP.equals( type ) )
        {
            item.setMap( objectManager.get( Map.class, contentUid ) );
            dashboard.getItems().add( 0, item );
        }
        else if ( DashboardItemType.REPORT_TABLE.equals( type ) )
        {
            item.setVisualization( objectManager.get( Visualization.class, contentUid ) );
            dashboard.getItems().add( 0, item );
        }
        else if ( DashboardItemType.EVENT_REPORT.equals( type ) )
        {
            item.setEventReport( objectManager.get( EventReport.class, contentUid ) );
            dashboard.getItems().add( 0, item );
        }
        else if ( DashboardItemType.MESSAGES.equals( type ) )
        {
            item.setMessages( true );
            dashboard.getItems().add( 0, item );
        }
        else if ( DashboardItemType.APP.equals( type ) )
        {
            item.setAppKey( contentUid );
            dashboard.getItems().add( 0, item );
        }
        else // Link item
        {
            DashboardItem availableItem = dashboard.getAvailableItemByType( type );

            item = availableItem == null ? new DashboardItem() : availableItem;

            if ( DashboardItemType.USERS.equals( type ) )
            {
                item.getUsers().add( objectManager.get( User.class, contentUid ) );
            }
            else if ( DashboardItemType.REPORTS.equals( type ) )
            {
                item.getReports().add( objectManager.get( Report.class, contentUid ) );
            }
            else if ( DashboardItemType.RESOURCES.equals( type ) )
            {
                item.getResources().add( objectManager.get( Document.class, contentUid ) );
            }

            if ( availableItem == null )
            {
                dashboard.getItems().add( 0, item );
            }
        }

        if ( dashboard.getItemCount() > Dashboard.MAX_ITEMS )
        {
            return null;
        }

        updateDashboard( dashboard );

        return item;
    }

    @Override
    @Transactional( readOnly = true )
    public void mergeDashboard( Dashboard dashboard )
    {
        if ( dashboard.getItems() != null )
        {
            for ( DashboardItem item : dashboard.getItems() )
            {
                mergeDashboardItem( item );
            }
        }
    }

    @Override
    @Transactional( readOnly = true )
    public void mergeDashboardItem( DashboardItem item )
    {
        if ( item.getVisualization() != null )
        {
            item.setVisualization( objectManager.get( Visualization.class, item.getVisualization().getUid() ) );
        }

        if ( item.getChart() != null )
        {
            item.setVisualization( objectManager.get( Visualization.class, item.getChart().getUid() ) );
        }

        if ( item.getEventChart() != null )
        {
            item.setEventChart( objectManager.get( EventChart.class, item.getEventChart().getUid() ) );
        }

        if ( item.getMap() != null )
        {
            item.setMap( objectManager.get( Map.class, item.getMap().getUid() ) );
        }

        if ( item.getReportTable() != null )
        {
            item.setVisualization( objectManager.get( Visualization.class, item.getReportTable().getUid() ) );
        }

        if ( item.getEventReport() != null )
        {
            item.setEventReport( objectManager.get( EventReport.class, item.getEventReport().getUid() ) );
        }

        if ( item.getUsers() != null )
        {
            item.setUsers( objectManager.getByUid( User.class, getUids( item.getUsers() ) ) );
        }

        if ( item.getReports() != null )
        {
            item.setReports( objectManager.getByUid( Report.class, getUids( item.getReports() ) ) );
        }

        if ( item.getResources() != null )
        {
            item.setResources( objectManager.getByUid( Document.class, getUids( item.getResources() ) ) );
        }

        if ( item.getAppKey() != null )
        {
            item.setAppKey( item.getAppKey() );
        }
    }

    @Override
    @Transactional
    public long saveDashboard( Dashboard dashboard )
    {
        dashboardStore.save( dashboard );

        return dashboard.getId();
    }

    @Override
    @Transactional
    public void updateDashboard( Dashboard dashboard )
    {
        dashboardStore.update( dashboard );
    }

    @Override
    @Transactional
    public void deleteDashboard( Dashboard dashboard )
    {
        dashboardStore.delete( dashboard );
    }

    @Override
    @Transactional( readOnly = true )
    public Dashboard getDashboard( long id )
    {
        return dashboardStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public Dashboard getDashboard( String uid )
    {
        return dashboardStore.getByUid( uid );
    }

    // -------------------------------------------------------------------------
    // DashboardItem
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void updateDashboardItem( DashboardItem item )
    {
        dashboardItemStore.update( item );
    }

    @Override
    @Transactional( readOnly = true )
    public DashboardItem getDashboardItem( String uid )
    {
        return dashboardItemStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public Dashboard getDashboardFromDashboardItem( DashboardItem dashboardItem )
    {
        return dashboardItemStore.getDashboardFromDashboardItem( dashboardItem );
    }

    @Override
    public void deleteDashboardItem( DashboardItem item )
    {
        dashboardItemStore.delete( item );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getVisualizationDashboardItems( Visualization visualization )
    {
        return dashboardItemStore.getVisualizationDashboardItems( visualization );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getReportTableDashboardItems( ReportTable reportTable )
    {
        return dashboardItemStore.getReportTableDashboardItems( reportTable );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getChartDashboardItems( Chart chart )
    {
        return dashboardItemStore.getChartDashboardItems( chart );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getEventChartDashboardItems( EventChart eventChart )
    {
        return dashboardItemStore.getEventChartDashboardItems( eventChart );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getMapDashboardItems( Map map )
    {
        return dashboardItemStore.getMapDashboardItems( map );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getEventReportDashboardItems( EventReport eventReport )
    {
        return dashboardItemStore.getEventReportDashboardItems( eventReport );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getUserDashboardItems( User user )
    {
        return dashboardItemStore.getUserDashboardItems( user );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getReportDashboardItems( Report report )
    {
        return dashboardItemStore.getReportDashboardItems( report );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DashboardItem> getDocumentDashboardItems( Document document )
    {
        return dashboardItemStore.getDocumentDashboardItems( document );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private int getMax( DashboardItemType type, Set<DashboardItemType> maxTypes, Integer count, Integer maxCount )
    {
        int dashboardsMax = ObjectUtils.firstNonNull( maxCount, MAX_HITS_PER_OBJECT );
        int dashboardsCount = ObjectUtils.firstNonNull( count, HITS_PER_OBJECT );

        return maxTypes != null && maxTypes.contains( type ) ? dashboardsMax : dashboardsCount;
    }

    private List<SimpleVisualizationView> convertFrom( final List<Visualization> visualizations )
    {
        final List<SimpleVisualizationView> views = new ArrayList<>( 1 );

        if ( isNotEmpty( visualizations ) )
        {
            for ( final Visualization visualization : visualizations )
            {
                views.add( convertFrom( visualization ) );
            }
        }
        return views;
    }

    private SimpleVisualizationView convertFrom( final Visualization visualization )
    {
        final SimpleVisualizationView view = new SimpleVisualizationView();
        BeanUtils.copyProperties( visualization, view );
        return view;
    }
}
