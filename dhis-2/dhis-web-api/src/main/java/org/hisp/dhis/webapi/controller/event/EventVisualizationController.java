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

import static org.hisp.dhis.common.DhisApiVersion.ALL;
import static org.hisp.dhis.common.DhisApiVersion.DEFAULT;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE_LIST;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;
import static org.hisp.dhis.schema.descriptors.EventVisualizationSchemaDescriptor.API_ENDPOINT;
import static org.hisp.dhis.system.util.CodecUtils.filenameEncode;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_PNG;
import static org.jfree.chart.ChartUtils.writeChartAsPNG;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.User;
import org.hisp.dhis.visualization.ChartService;
import org.hisp.dhis.visualization.PlotData;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.jfree.chart.JFreeChart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller responsible for providing the basic CRUD endpoints for the model
 * EventVisualization.
 *
 * @author maikel arabori
 */
@Controller
@RequestMapping( value = API_ENDPOINT )
@ApiVersion( { DEFAULT, ALL } )
@AllArgsConstructor
public class EventVisualizationController
    extends
    AbstractCrudController<EventVisualization>
{
    private final DimensionService dimensionService;

    private final OrganisationUnitService organisationUnitService;

    private final EventVisualizationService eventVisualizationService;

    private final ChartService chartService;

    private final I18nManager i18nManager;

    private final ContextUtils contextUtils;

    @GetMapping( value = { "/{uid}/data", "/{uid}/data.png" } )
    void generateChart(
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
        // TODO no acl?
        final EventVisualization eventVisualization = eventVisualizationService.getEventVisualization( uid );

        if ( eventVisualization == null )
        {
            throw new WebMessageException( notFound( "Event visualization does not exist: " + uid ) );
        }

        doesNotAllowPivotAndReportChart( eventVisualization );

        final OrganisationUnit unit = ou != null ? organisationUnitService.getOrganisationUnit( ou ) : null;

        final JFreeChart jFreeChart = chartService.getJFreeChart( new PlotData( eventVisualization ), date, unit,
            i18nManager.getI18nFormat() );

        final String filename = filenameEncode( eventVisualization.getName() ) + ".png";

        contextUtils.configureResponse( response, CONTENT_TYPE_PNG, RESPECT_SYSTEM_SETTING, filename, attachment );

        writeChartAsPNG( response.getOutputStream(), jFreeChart, width, height );
    }

    @Override
    protected EventVisualization deserializeJsonEntity( final HttpServletRequest request )
        throws IOException
    {
        final EventVisualization eventVisualization = super.deserializeJsonEntity( request );

        prepare( eventVisualization );

        return eventVisualization;
    }

    @Override
    protected EventVisualization deserializeXmlEntity( final HttpServletRequest request )
        throws IOException
    {
        final EventVisualization eventVisualization = super.deserializeXmlEntity( request );

        prepare( eventVisualization );

        return eventVisualization;
    }

    @Override
    protected void postProcessResponseEntity( final EventVisualization eventVisualization, final WebOptions options,
        final Map<String, String> parameters )
    {
        eventVisualization.populateAnalyticalProperties();

        final User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            final Set<OrganisationUnit> roots = currentUser.getDataViewOrganisationUnitsWithFallback();

            for ( OrganisationUnit organisationUnit : eventVisualization.getOrganisationUnits() )
            {
                eventVisualization.getParentGraphMap().put( organisationUnit.getUid(),
                    organisationUnit.getParentGraph( roots ) );
            }
        }

        final I18nFormat format = i18nManager.getI18nFormat();

        if ( eventVisualization.getPeriods() != null && !eventVisualization.getPeriods().isEmpty() )
        {
            for ( final Period period : eventVisualization.getPeriods() )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }
    }

    private void prepare( final EventVisualization eventVisualization )
    {
        dimensionService.mergeAnalyticalObject( eventVisualization );
        dimensionService.mergeEventAnalyticalObject( eventVisualization );

        eventVisualization.getColumnDimensions().clear();
        eventVisualization.getRowDimensions().clear();
        eventVisualization.getFilterDimensions().clear();
        eventVisualization.getSimpleDimensions().clear();

        eventVisualization.getColumnDimensions().addAll( getDimensions( eventVisualization.getColumns() ) );
        eventVisualization.getRowDimensions().addAll( getDimensions( eventVisualization.getRows() ) );
        eventVisualization.getFilterDimensions().addAll( getDimensions( eventVisualization.getFilters() ) );
        eventVisualization.associateSimpleDimensions();
    }

    private void doesNotAllowPivotAndReportChart( final EventVisualization eventVisualization )
        throws WebMessageException
    {
        if ( eventVisualization.getType() == PIVOT_TABLE || eventVisualization.getType() == LINE_LIST )
        {
            throw new WebMessageException( notFound( "Cannot generate chart for " + eventVisualization.getType() ) );
        }
    }
}
