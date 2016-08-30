package org.hisp.dhis.webapi.controller;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dashboard.DashboardItemType;
import org.hisp.dhis.dashboard.DashboardSearchResult;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.schema.descriptors.DashboardItemSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.DashboardSchemaDescriptor;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.dashboard.Dashboard.MAX_ITEMS;

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
    // Dashboard
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/q/{query}", method = RequestMethod.GET )
    public @ResponseBody DashboardSearchResult search( @PathVariable String query, @RequestParam( required = false ) Set<DashboardItemType> max )
        throws Exception
    {
        DashboardSearchResult dashboardSearchResult = dashboardService.search( query, max );

        return dashboardSearchResult;
    }

    @Override
    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonObject( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Dashboard dashboard = renderService.fromJson( request.getInputStream(), Dashboard.class );
        dashboard.getTranslations().clear();

        dashboardService.mergeDashboard( dashboard );
        dashboardService.saveDashboard( dashboard );

        response.addHeader( "Location", DashboardSchemaDescriptor.API_ENDPOINT + "/" + dashboard.getUid() );
        webMessageService.send( WebMessageUtils.created( "Dashboard created" ), response, request );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    public void putJsonObject( @PathVariable( "uid" ) String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Dashboard dashboard = dashboardService.getDashboard( uid );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard does not exist: " + uid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), dashboard ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this dashboard." );
        }

        Dashboard newDashboard = renderService.fromJson( request.getInputStream(), Dashboard.class );
        newDashboard.setTranslations( dashboard.getTranslations() );

        dashboard.setName( newDashboard.getName() ); // TODO Name only for now

        dashboardService.updateDashboard( dashboard );
    }

    @Override
    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteObject( @PathVariable( "uid" ) String uid, HttpServletRequest request, HttpServletResponse response )
        throws Exception
    {
        List<Dashboard> objects = getEntity( uid, NO_WEB_OPTIONS );

        if ( objects.isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard does not exist: " + uid ) );
        }

        if ( !aclService.canDelete( currentUserService.getCurrentUser(), objects.get( 0 ) ) )
        {
            throw new DeleteAccessDeniedException( "You don't have the proper permissions to delete this object." );
        }

        dashboardService.deleteDashboard( objects.get( 0 ) );
    }

    // -------------------------------------------------------------------------
    // Dashboard items
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}/items", method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonItem( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        Dashboard dashboard = dashboardService.getDashboard( uid );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard does not exist: " + uid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), dashboard ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this dashboard." );
        }

        DashboardItem item = renderService.fromJson( request.getInputStream(), DashboardItem.class );

        dashboardService.mergeDashboardItem( item );

        dashboard.getItems().add( 0, item );

        dashboardService.updateDashboard( dashboard );

        response.addHeader( "Location", DashboardItemSchemaDescriptor.API_ENDPOINT + "/" + item.getUid() );
        webMessageService.send( WebMessageUtils.created( "Dashboard item created" ), response, request );
    }

    @RequestMapping( value = "/{dashboardUid}/items/content", method = RequestMethod.POST )
    public void postJsonItemContent( HttpServletResponse response, HttpServletRequest request,
        @PathVariable String dashboardUid, @RequestParam DashboardItemType type, @RequestParam( "id" ) String contentUid ) throws Exception
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardUid );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard does not exist: " + dashboardUid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), dashboard ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this dashboard." );
        }

        DashboardItem item = dashboardService.addItemContent( dashboardUid, type, contentUid );

        if ( item == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Max number of dashboard items reached: " + MAX_ITEMS ) );
        }
        else
        {
            response.addHeader( "Location", DashboardItemSchemaDescriptor.API_ENDPOINT + "/" + item.getUid() );
            webMessageService.send( WebMessageUtils.created( "Dashboard item created" ), response, request );
        }
    }

    @RequestMapping( value = "/{dashboardUid}/items/{itemUid}/position/{position}", method = RequestMethod.POST )
    public void moveItem( HttpServletResponse response, HttpServletRequest request,
        @PathVariable String dashboardUid, @PathVariable String itemUid, @PathVariable int position ) throws Exception
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardUid );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard does not exist: " + dashboardUid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), dashboard ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this dashboard." );
        }

        if ( dashboard.moveItem( itemUid, position ) )
        {
            dashboardService.updateDashboard( dashboard );

            webMessageService.send( WebMessageUtils.ok( "Dashboard item moved" ), response, request );
        }
    }

    @RequestMapping( value = "/{dashboardUid}/items/{itemUid}", method = RequestMethod.DELETE )
    public void deleteItem( HttpServletResponse response, HttpServletRequest request,
        @PathVariable String dashboardUid, @PathVariable String itemUid ) throws WebMessageException
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardUid );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard does not exist: " + dashboardUid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), dashboard ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this dashboard." );
        }

        DashboardItem item = dashboardService.getDashboardItem( itemUid );

        if ( item == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard item does not exist: " + dashboardUid ) );
        }

        if ( dashboard.hasItems() && dashboard.getItems().remove( item ) )
        {
            dashboardService.deleteDashboardItem( item );
            dashboardService.updateDashboard( dashboard );

            webMessageService.send( WebMessageUtils.ok( "Dashboard item removed" ), response, request );
        }
    }

    @RequestMapping( value = "/{dashboardUid}/items/{itemUid}/content/{contentUid}", method = RequestMethod.DELETE )
    public void deleteItemContent( HttpServletResponse response, HttpServletRequest request,
        @PathVariable String dashboardUid, @PathVariable String itemUid, @PathVariable String contentUid ) throws WebMessageException
    {
        Dashboard dashboard = dashboardService.getDashboard( dashboardUid );

        if ( dashboard == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard does not exist: " + dashboardUid ) );
        }

        if ( !aclService.canUpdate( currentUserService.getCurrentUser(), dashboard ) )
        {
            throw new UpdateAccessDeniedException( "You don't have the proper permissions to update this dashboard." );
        }

        DashboardItem item = dashboard.getItemByUid( itemUid );

        if ( item == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Dashboard item does not exist: " + dashboardUid ) );
        }

        if ( item.removeItemContent( contentUid ) )
        {
            if ( item.getContentCount() == 0 && dashboard.getItems().remove( item ) )
            {
                dashboardService.deleteDashboardItem( item ); // Delete if empty                
            }

            dashboardService.updateDashboard( dashboard );

            webMessageService.send( WebMessageUtils.ok( "Dashboard item content removed" ), response, request );
        }
    }

    // -------------------------------------------------------------------------
    // Hooks
    // -------------------------------------------------------------------------

    @Override
    protected void postProcessEntity( Dashboard entity, WebOptions options, Map<String, String> parameters, TranslateParams translateParams ) throws Exception
    {
        for ( DashboardItem item : entity.getItems() )
        {
            if ( item != null )
            {
                item.setHref( null ); // Null item link, not relevant

                if ( item.getEmbeddedItem() != null )
                {
                    linkService.generateLinks( item.getEmbeddedItem(), true );
                }
                else if ( item.getLinkItems() != null )
                {
                    for ( IdentifiableObject link : item.getLinkItems() )
                    {
                        linkService.generateLinks( link, true );
                    }
                }
            }
        }
    }
}
