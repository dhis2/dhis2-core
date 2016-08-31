package org.hisp.dhis.dashboard.impl;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Sets;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

/**
 * Note: The remove associations methods must be altered if caching is introduced.
 *
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultDashboardService
    implements DashboardService
{
    private static final int HITS_PER_OBJECT = 5;
    private static final int MAX_HITS_PER_OBJECT = 25;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private HibernateIdentifiableObjectStore<Dashboard> dashboardStore;

    public void setDashboardStore( HibernateIdentifiableObjectStore<Dashboard> dashboardStore )
    {
        this.dashboardStore = dashboardStore;
    }

    @Autowired
    private IdentifiableObjectManager objectManager;

    @Autowired
    private UserService userService;

    @Autowired
    private DashboardItemStore dashboardItemStore;

    @Autowired
    private AppManager appManager;

    // -------------------------------------------------------------------------
    // DashboardService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Dashboard
    // -------------------------------------------------------------------------

    @Override
    public DashboardSearchResult search( String query )
    {
        return search( query, new HashSet<>() );
    }

    @Override
    public DashboardSearchResult search( String query, Set<DashboardItemType> maxTypes )
    {
        Set<String> words = Sets.newHashSet( query.split( TextUtils.SPACE ) );

        List<App> dashboardApps = appManager.getAppsByType( AppType.DASHBOARD_WIDGET, new HashSet<>( appManager.getApps( null ) ) );
        
        DashboardSearchResult result = new DashboardSearchResult();

        result.setUsers( userService.getAllUsersBetweenByName( query, 0, getMax( DashboardItemType.USERS, maxTypes ) ) );
        result.setCharts( objectManager.getBetweenLikeName( Chart.class, words, 0, getMax( DashboardItemType.CHART, maxTypes ) ) );
        result.setEventCharts( objectManager.getBetweenLikeName( EventChart.class, words, 0, getMax( DashboardItemType.EVENT_CHART, maxTypes ) ) );
        result.setMaps( objectManager.getBetweenLikeName( Map.class, words, 0, getMax( DashboardItemType.MAP, maxTypes ) ) );
        result.setReportTables( objectManager.getBetweenLikeName( ReportTable.class, words, 0, getMax( DashboardItemType.REPORT_TABLE, maxTypes ) ) );
        result.setEventReports( objectManager.getBetweenLikeName( EventReport.class, words, 0, getMax( DashboardItemType.EVENT_REPORT, maxTypes ) ) );
        result.setReports( objectManager.getBetweenLikeName( Report.class, words, 0, getMax( DashboardItemType.REPORTS, maxTypes ) ) );
        result.setResources( objectManager.getBetweenLikeName( Document.class, words, 0, getMax( DashboardItemType.RESOURCES, maxTypes ) ) );
        result.setApps( appManager.getAppsByName( query, dashboardApps, "ilike" ) );

        return result;
    }

    @Override
    public DashboardItem addItemContent( String dashboardUid, DashboardItemType type, String contentUid )
    {
        Dashboard dashboard = getDashboard( dashboardUid );

        if ( dashboard == null )
        {
            return null;
        }

        DashboardItem item = new DashboardItem();

        if ( DashboardItemType.CHART.equals( type ) )
        {
            item.setChart( objectManager.get( Chart.class, contentUid ) );
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
            item.setReportTable( objectManager.get( ReportTable.class, contentUid ) );
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
    public void mergeDashboardItem( DashboardItem item )
    {
        if ( item.getChart() != null )
        {
            item.setChart( objectManager.get( Chart.class, item.getChart().getUid() ) );
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
            item.setReportTable( objectManager.get( ReportTable.class, item.getReportTable().getUid() ) );
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
    public int saveDashboard( Dashboard dashboard )
    {
        return dashboardStore.save( dashboard );
    }

    @Override
    public void updateDashboard( Dashboard dashboard )
    {
        dashboardStore.update( dashboard );
    }

    @Override
    public void deleteDashboard( Dashboard dashboard )
    {
        dashboardStore.delete( dashboard );

        for ( DashboardItem dashboardItem : dashboard.getItems() )
        {
            dashboardItemStore.delete( dashboardItem );
        }
    }

    @Override
    public Dashboard getDashboard( int id )
    {
        return dashboardStore.get( id );
    }

    @Override
    public Dashboard getDashboard( String uid )
    {
        return dashboardStore.getByUid( uid );
    }

    // -------------------------------------------------------------------------
    // DashboardItem
    // -------------------------------------------------------------------------

    @Override
    public void updateDashboardItem( DashboardItem item )
    {
        dashboardItemStore.update( item );
    }

    @Override
    public DashboardItem getDashboardItem( String uid )
    {
        return dashboardItemStore.getByUid( uid );
    }

    @Override
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
    public int countMapDashboardItems( Map map )
    {
        return dashboardItemStore.countMapDashboardItems( map );
    }

    @Override
    public int countChartDashboardItems( Chart chart )
    {
        return dashboardItemStore.countChartDashboardItems( chart );
    }

    @Override
    public int countReportTableDashboardItems( ReportTable reportTable )
    {
        return dashboardItemStore.countReportTableDashboardItems( reportTable );
    }

    @Override
    public int countReportDashboardItems( Report report )
    {
        return dashboardItemStore.countReportDashboardItems( report );
    }

    @Override
    public int countDocumentDashboardItems( Document document )
    {
        return dashboardItemStore.countDocumentDashboardItems( document );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private int getMax( DashboardItemType type, Set<DashboardItemType> maxTypes )
    {
        return maxTypes != null && maxTypes.contains( type ) ? MAX_HITS_PER_OBJECT : HITS_PER_OBJECT;
    }
}
