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
package org.hisp.dhis.webapi.controller;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.dashboard.DashboardSearchResult;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.schema.descriptors.DashboardSchemaDescriptor;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.sharing.CascadeSharingReport;
import org.hisp.dhis.sharing.CascadeSharingService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.metadata.MetadataExportControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.JsonNode;

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

    @Autowired
    private CascadeSharingService cascadeSharingService;

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @GetMapping( "/search" )
    public @ResponseBody DashboardSearchResult searchAsParam( @RequestParam String q,
        @RequestParam( required = false ) Set<DashboardItemType> max,
        @RequestParam( required = false ) Integer count,
        @RequestParam( required = false ) Integer maxCount )
    {
        return dashboardService.search( q, max, count, maxCount );
    }

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
    public ResponseEntity<JsonNode> getDataSetWithDependencies( @PathVariable( "uid" ) String dashboardId,
        @RequestParam( required = false, defaultValue = "false" ) boolean download )
        throws WebMessageException
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardId );

        if ( dashboard == null )
        {
            throw new WebMessageException( notFound( "Dashboard not found for uid: " + dashboardId ) );
        }

        return MetadataExportControllerUtils.getWithDependencies( contextService, exportService, dashboard, download );
    }

    @PostMapping( "cascadeSharing/{uid}" )
    public @ResponseBody CascadeSharingReport cascadeSharing( @PathVariable( "uid" ) String dashboardId,
        @RequestParam( required = false ) boolean dryRun, @RequestParam( required = false ) boolean atomic,
        @CurrentUser User currentUser )
        throws WebMessageException
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardId );

        if ( dashboard == null )
        {
            throw new WebMessageException( notFound( "Dashboard not found for uid: " + dashboardId ) );
        }

        return cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().user( currentUser )
                .atomic( atomic ).dryRun( dryRun ).build() );
    }

    @Override
    protected void preCreateEntity( final Dashboard dashboard )
        throws WebMessageException
    {
        checkPreConditions( dashboard );
    }

    @Override
    protected void preUpdateEntity( final Dashboard dashboard, final Dashboard newDashboard )
        throws WebMessageException
    {
        checkPreConditions( newDashboard );
    }

    private void checkPreConditions( final Dashboard dashboard )
        throws WebMessageException
    {
        if ( !hasDashboardItemsTypeSet( dashboard.getItems() ) )
        {
            throw new WebMessageException( conflict( "Dashboard item does not have any type associated." ) );
        }
    }

    private boolean hasDashboardItemsTypeSet( final List<DashboardItem> items )
    {
        if ( isNotEmpty( items ) )
        {
            for ( final DashboardItem item : items )
            {
                final boolean hasAssociationType = item != null
                    && (item.getLinkItems() != null || item.getEmbeddedItem() != null || item.getText() != null
                        || item.getMessages() != null);

                final boolean hasType = item != null && item.getType() != null;

                if ( !hasType && !hasAssociationType )
                {
                    return false;
                }
            }
        }

        return true;
    }
}
