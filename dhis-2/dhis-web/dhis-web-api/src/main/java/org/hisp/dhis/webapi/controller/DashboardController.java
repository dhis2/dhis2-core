package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.dashboard.DashboardSearchResult;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.schema.descriptors.DashboardSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@Controller
@ApiVersion( { DhisApiVersion.V28, DhisApiVersion.V29, DhisApiVersion.V30, DhisApiVersion.V31, DhisApiVersion.V32, DhisApiVersion.DEFAULT } )
@RequestMapping( value = DashboardSchemaDescriptor.API_ENDPOINT )
public class DashboardController
    extends AbstractCrudController<Dashboard>
{
    @Autowired
    private DashboardService dashboardService;

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/q/{query}", method = RequestMethod.GET )
    public @ResponseBody DashboardSearchResult search( @PathVariable String query, @RequestParam( required = false ) Set<DashboardItemType> max )
    {
        return dashboardService.search( query, max );
    }

    @RequestMapping( value = "/q", method = RequestMethod.GET )
    public @ResponseBody DashboardSearchResult searchNoFilter( @RequestParam( required = false ) Set<DashboardItemType> max )
    {
        return dashboardService.search( max );
    }

    // -------------------------------------------------------------------------
    // Metadata with dependencies
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/metadata", method = RequestMethod.GET )
    public @ResponseBody RootNode getDataSetWithDependencies( @PathVariable( "uid" ) String dashboardId, HttpServletResponse response )
        throws WebMessageException, IOException
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardId );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard not found for uid: " + dashboardId ) );
        }

        return exportService.getMetadataWithDependenciesAsNode( dashboard );
    }
}
