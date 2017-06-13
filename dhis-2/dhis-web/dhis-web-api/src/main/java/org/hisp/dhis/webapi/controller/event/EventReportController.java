package org.hisp.dhis.webapi.controller.event;

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

import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.schema.descriptors.EventReportSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensions;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = EventReportSchemaDescriptor.API_ENDPOINT )
public class EventReportController
    extends AbstractCrudController<EventReport>
{
    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private I18nManager i18nManager;

    //--------------------------------------------------------------------------
    // CRUD
    //--------------------------------------------------------------------------

    @Override
    protected EventReport deserializeJsonEntity( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        EventReport eventReport = super.deserializeJsonEntity( request, response );
        mergeEventReport( eventReport );

        return eventReport;
    }

    //--------------------------------------------------------------------------
    // Hooks
    //--------------------------------------------------------------------------

    @Override
    protected void postProcessEntity( EventReport report )
        throws Exception
    {
        report.populateAnalyticalProperties();

        Set<OrganisationUnit> roots = currentUserService.getCurrentUser().getDataViewOrganisationUnitsWithFallback();

        for ( OrganisationUnit organisationUnit : report.getOrganisationUnits() )
        {
            report.getParentGraphMap().put( organisationUnit.getUid(), organisationUnit.getParentGraph( roots ) );
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

    //--------------------------------------------------------------------------
    // Supportive methods
    //--------------------------------------------------------------------------

    private void mergeEventReport( EventReport report )
    {
        dimensionService.mergeAnalyticalObject( report );
        dimensionService.mergeEventAnalyticalObject( report );

        report.getColumnDimensions().clear();
        report.getRowDimensions().clear();
        report.getFilterDimensions().clear();

        report.getColumnDimensions().addAll( getDimensions( report.getColumns() ) );
        report.getRowDimensions().addAll( getDimensions( report.getRows() ) );
        report.getFilterDimensions().addAll( getDimensions( report.getFilters() ) );
    }
}
