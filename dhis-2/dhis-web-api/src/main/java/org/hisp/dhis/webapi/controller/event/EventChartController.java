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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE_LIST;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventchart.EventChartService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.schema.descriptors.EventChartSchemaDescriptor;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.ChartService;
import org.hisp.dhis.visualization.PlotData;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @deprecated THIS IS BEING DEPRECATED IN FAVOUR OF THE EventVisualization
 *             MODEL. WE SHOULD AVOID CHANGES ON THIS CLASS AS MUCH AS POSSIBLE.
 *             NEW FEATURES SHOULD BE ADDED ON TOP OF
 *             EventVisualizationController.
 *
 * @author Jan Henrik Overland
 */
@Deprecated
@Controller
@RequestMapping( value = EventChartSchemaDescriptor.API_ENDPOINT )
public class EventChartController
    extends AbstractCrudController<EventChart>
{
    @Autowired
    private EventChartService eventChartService;

    @Autowired
    private ChartService chartService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private I18nManager i18nManager;

    @Autowired
    private ContextUtils contextUtils;

    // --------------------------------------------------------------------------
    // CRUD
    // --------------------------------------------------------------------------

    // TODO: Block querying LINE_LIST and PIVOT_TABLE type.

    @Override
    protected EventChart deserializeJsonEntity( HttpServletRequest request )
        throws IOException
    {
        EventChart eventChart = super.deserializeJsonEntity( request );
        mergeEventChart( eventChart );

        applyCompatibilityConversions( eventChart );

        return eventChart;
    }

    @Override
    protected EventChart deserializeXmlEntity( HttpServletRequest request )
        throws IOException
    {
        EventChart eventChart = super.deserializeXmlEntity( request );
        mergeEventChart( eventChart );

        applyCompatibilityConversions( eventChart );

        return eventChart;
    }

    // --------------------------------------------------------------------------
    // Get data
    // --------------------------------------------------------------------------

    @GetMapping( value = { "/{uid}/data", "/{uid}/data.png" } )
    public void getChart(
        @PathVariable( "uid" ) String uid,
        @RequestParam( value = "date", required = false ) Date date,
        @RequestParam( value = "ou", required = false ) String ou,
        @RequestParam( value = "width", defaultValue = "800", required = false ) int width,
        @RequestParam( value = "height", defaultValue = "500", required = false ) int height,
        @RequestParam( value = "attachment", required = false ) boolean attachment,
        HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        EventChart chart = eventChartService.getEventChart( uid ); // TODO no
                                                                   // acl?

        if ( chart == null )
        {
            throw new WebMessageException( notFound( "Event chart does not exist: " + uid ) );
        }

        OrganisationUnit unit = ou != null ? organisationUnitService.getOrganisationUnit( ou ) : null;

        JFreeChart jFreeChart = chartService.getJFreeChart( new PlotData( chart ), date, unit,
            i18nManager.getI18nFormat() );

        String filename = CodecUtils.filenameEncode( chart.getName() ) + ".png";

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_PNG, CacheStrategy.RESPECT_SYSTEM_SETTING,
            filename, attachment );

        ChartUtils.writeChartAsPNG( response.getOutputStream(), jFreeChart, width, height );
    }

    // --------------------------------------------------------------------------
    // Hooks
    // --------------------------------------------------------------------------

    /**
     * @deprecated This is a temporary workaround to keep EventChart backward
     *             compatible with the new EventVisualization entity. Only
     *             legacy and chart related types can be returned by this
     *             endpoint.
     *
     * @param filters
     */
    @Deprecated
    @Override
    protected void forceFiltering( final List<String> filters )
    {
        filters.add( "type:!eq:PIVOT_TABLE" );
        filters.add( "type:!eq:LINE_LIST" );
        filters.add( "legacy:eq:true" );
    }

    @Override
    protected void postProcessResponseEntity( EventChart eventChart, WebOptions options,
        Map<String, String> parameters )
    {
        eventChart.populateAnalyticalProperties();

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            Set<OrganisationUnit> roots = currentUser.getDataViewOrganisationUnitsWithFallback();

            for ( OrganisationUnit organisationUnit : eventChart.getOrganisationUnits() )
            {
                eventChart.getParentGraphMap().put( organisationUnit.getUid(),
                    organisationUnit.getParentGraph( roots ) );
            }
        }

        I18nFormat format = i18nManager.getI18nFormat();

        if ( eventChart.getPeriods() != null && !eventChart.getPeriods().isEmpty() )
        {
            for ( Period period : eventChart.getPeriods() )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }
    }

    // --------------------------------------------------------------------------
    // Supportive methods
    // --------------------------------------------------------------------------

    private void mergeEventChart( EventChart chart )
    {
        dimensionService.mergeAnalyticalObject( chart );
        dimensionService.mergeEventAnalyticalObject( chart );

        chart.getColumnDimensions().clear();
        chart.getRowDimensions().clear();
        chart.getFilterDimensions().clear();

        chart.getColumnDimensions().addAll( getDimensions( chart.getColumns() ) );
        chart.getRowDimensions().addAll( getDimensions( chart.getRows() ) );
        chart.getFilterDimensions().addAll( getDimensions( chart.getFilters() ) );
    }

    /**
     * This method encapsulated the necessary conversions to keep this object
     * compatible with EventVisualization. This is need to enable backward
     * compatibility during the deprecation process of the EventChart.
     *
     * @param eventChart
     */
    private void applyCompatibilityConversions( EventChart eventChart )
        throws IOException
    {
        // Block persisting of LINE_LIST and PIVOT_TABLE types.
        if ( eventChart.getType() == LINE_LIST || eventChart.getType() == PIVOT_TABLE )
        {
            throw new IOException( "Cannot convert type: " + eventChart.getType() );
        }
    }
}
