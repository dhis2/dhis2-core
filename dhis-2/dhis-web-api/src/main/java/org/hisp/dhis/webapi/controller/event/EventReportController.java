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

import static org.hisp.dhis.analytics.EventDataType.AGGREGATED_VALUES;
import static org.hisp.dhis.analytics.EventDataType.EVENTS;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.LINE_LIST;
import static org.hisp.dhis.eventvisualization.EventVisualizationType.PIVOT_TABLE;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.schema.descriptors.EventReportSchemaDescriptor;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @deprecated THIS IS BEING DEPRECATED IN FAVOUR OF THE EventVisualization
 *             MODEL. WE SHOULD AVOID CHANGES ON THIS CLASS AS MUCH AS POSSIBLE.
 *             NEW FEATURES SHOULD BE ADDED ON TOP OF
 *             EventVisualizationController.
 *
 * @author Lars Helge Overland
 */
@Deprecated
@Controller
@RequestMapping( value = EventReportSchemaDescriptor.API_ENDPOINT )
public class EventReportController
    extends AbstractCrudController<EventReport>
{
    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private I18nManager i18nManager;

    // --------------------------------------------------------------------------
    // CRUD
    // --------------------------------------------------------------------------

    // TODO: ONLY allow querying LINE_LIST and PIVOT_TABLE type.

    @Override
    protected EventReport deserializeJsonEntity( HttpServletRequest request )
        throws IOException
    {
        EventReport eventReport = super.deserializeJsonEntity( request );
        mergeEventReport( eventReport );

        applyCompatibilityConversions( eventReport );

        return eventReport;
    }

    @Override
    protected EventReport deserializeXmlEntity( HttpServletRequest request )
        throws IOException
    {
        EventReport eventReport = super.deserializeXmlEntity( request );
        mergeEventReport( eventReport );

        applyCompatibilityConversions( eventReport );

        return eventReport;
    }

    // --------------------------------------------------------------------------
    // Hooks
    // --------------------------------------------------------------------------

    /**
     * @deprecated This is a temporary workaround to keep EventReport backward
     *             compatible with the new EventVisualization entity. Only
     *             legacy and report related types can be returned by this
     *             endpoint.
     *
     * @param filters
     */
    @Deprecated
    @Override
    protected void forceFiltering( final List<String> filters )
    {
        filters.add( "type:in:[PIVOT_TABLE,LINE_LIST]" );
        filters.add( "legacy:eq:true" );
    }

    @Override
    protected void postProcessResponseEntity( EventReport report, WebOptions options, Map<String, String> parameters )
        throws Exception
    {
        report.populateAnalyticalProperties();

        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser != null )
        {
            Set<OrganisationUnit> roots = currentUser.getDataViewOrganisationUnitsWithFallback();

            for ( OrganisationUnit organisationUnit : report.getOrganisationUnits() )
            {
                report.getParentGraphMap().put( organisationUnit.getUid(), organisationUnit.getParentGraph( roots ) );
            }
        }

        I18nFormat format = i18nManager.getI18nFormat();

        if ( report.getPeriods() != null && !report.getPeriods().isEmpty() )
        {
            for ( Period period : report.getPeriods() )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }
    }

    // --------------------------------------------------------------------------
    // Supportive methods
    // --------------------------------------------------------------------------

    private void mergeEventReport( EventReport report )
    {
        dimensionService.mergeAnalyticalObject( report );
        dimensionService.mergeEventAnalyticalObject( report );

        report.getColumnDimensions().clear();
        report.getRowDimensions().clear();
        report.getFilterDimensions().clear();
        report.getSimpleDimensions().clear();

        report.getColumnDimensions().addAll( getDimensions( report.getColumns() ) );
        report.getRowDimensions().addAll( getDimensions( report.getRows() ) );
        report.getFilterDimensions().addAll( getDimensions( report.getFilters() ) );
        report.associateSimpleDimensions();
    }

    /**
     * This method encapsulated the necessary conversions to keep this object
     * compatible with EventVisualization. This is need to enable backward
     * compatibility during the deprecation process of the EventReport.
     *
     * @param report
     */
    private void applyCompatibilityConversions( EventReport report )
    {
        if ( report.getDataType() == AGGREGATED_VALUES )
        {
            report.setType( PIVOT_TABLE );
        }
        else if ( report.getDataType() == EVENTS )
        {
            report.setType( LINE_LIST );
        }
        else
        {
            // This entity only accepts the types above.
            report.setType( null );
        }
    }
}
