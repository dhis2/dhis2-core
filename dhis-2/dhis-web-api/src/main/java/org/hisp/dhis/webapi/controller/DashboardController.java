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
package org.hisp.dhis.webapi.controller;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.beans.BeanUtils.copyProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartType;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.dashboard.DashboardSearchResult;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.schema.descriptors.DashboardSchemaDescriptor;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationType;
import org.hisp.dhis.webapi.controller.metadata.MetadataExportControllerUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = DashboardSchemaDescriptor.API_ENDPOINT )
public class DashboardController
    extends AbstractCrudController<Dashboard>
{
    @Autowired
    private DashboardService dashboardService;

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @GetMapping( "/q/{query}" )
    public @ResponseBody DashboardSearchResult search( @PathVariable String query,
        @RequestParam( required = false ) Set<DashboardItemType> max,
        @RequestParam( required = false ) Integer count,
        @RequestParam( required = false ) Integer maxCount )
    {
        return dashboardService.search( query, max, count, maxCount );
    }

    @GetMapping( "/q" )
    public @ResponseBody DashboardSearchResult searchNoFilter(
        @RequestParam( required = false ) Set<DashboardItemType> max, @RequestParam( required = false ) Integer count,
        @RequestParam( required = false ) Integer maxCount )
    {
        return dashboardService.search( max, count, maxCount );
    }

    // -------------------------------------------------------------------------
    // Metadata with dependencies
    // -------------------------------------------------------------------------

    @GetMapping( "/{uid}/metadata" )
    public ResponseEntity<RootNode> getDataSetWithDependencies( @PathVariable( "uid" ) String dashboardId,
        @RequestParam( required = false, defaultValue = "false" ) boolean download )
        throws WebMessageException
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardId );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard not found for uid: " + dashboardId ) );
        }

        return MetadataExportControllerUtils.getWithDependencies( contextService, exportService, dashboard, download );
    }

    /**
     * Logic required to keep the backward compatibility with Chart and
     * ReporTable. Otherwise it would always return VISUALIZATION type for any
     * Chart or ReportTable.
     * <p>
     * Only needed during the transition from Chart/ReportTable APIs to
     * Visualization API. Once the Visualization API is fully enabled this logic
     * should be removed.
     *
     * @param dashboards
     * @param options
     * @param parameters
     */
    @Override
    @Deprecated
    protected void postProcessResponseEntities( final List<Dashboard> dashboards, final WebOptions options,
        final java.util.Map<String, String> parameters )
    {
        if ( isNotEmpty( dashboards ) )
        {
            for ( final Dashboard dashboard : dashboards )
            {
                postProcessResponseEntity( dashboard, options, parameters );
            }
        }
    }

    /**
     * Logic required to keep the backward compatibility with Chart and
     * ReportTable. Otherwise it would always return VISUALIZATION type for any
     * Chart or ReportTable.
     * <p>
     * Only needed during the transition from Chart/ReportTable APIs to
     * Visualization API. Once the Visualization API is fully enabled this logic
     * should be removed.
     *
     * @param dashboard
     * @param options
     * @param parameters
     */
    @Override
    @Deprecated
    protected void postProcessResponseEntity( final Dashboard dashboard, final WebOptions options,
        final Map<String, String> parameters )
    {
        if ( dashboard != null && isNotEmpty( dashboard.getItems() ) )
        {
            final List<DashboardItem> dashboardItems = dashboard.getItems();

            for ( final DashboardItem dashboardItem : dashboardItems )
            {
                if ( dashboardItem == null )
                {
                    continue;
                }

                if ( dashboardItem.getVisualization() != null )
                {
                    final VisualizationType type = dashboardItem.getVisualization().getType();

                    switch ( type )
                    {
                    case PIVOT_TABLE:
                        dashboardItem.setReportTable( convertToReportTable( dashboardItem.getVisualization() ) );
                        break;
                    case AREA:
                    case BAR:
                    case COLUMN:
                    case GAUGE:
                    case LINE:
                    case PIE:
                    case RADAR:
                    case SINGLE_VALUE:
                    case STACKED_AREA:
                    case STACKED_BAR:
                    case STACKED_COLUMN:
                    case YEAR_OVER_YEAR_COLUMN:
                    case YEAR_OVER_YEAR_LINE:
                    case SCATTER:
                    case BUBBLE:
                        dashboardItem.setChart( convertToChart( dashboardItem.getVisualization() ) );
                        break;
                    }
                }
            }
        }
    }

    private Chart convertToChart( final Visualization visualization )
    {
        final Chart chart = new Chart();
        copyProperties( visualization, chart, "type" );

        // Set the correct type
        if ( visualization.getType() != null && !"PIVOT_TABLE".equalsIgnoreCase( visualization.getType().name() ) )
        {
            chart.setType( ChartType.valueOf( visualization.getType().name() ) );
        }

        chart.setCumulativeValues( visualization.isCumulativeValues() );

        return chart;
    }

    private ReportTable convertToReportTable( final Visualization visualization )
    {
        final ReportTable reportTable = new ReportTable();
        BeanUtils.copyProperties( visualization, reportTable );

        return reportTable;
    }
}
