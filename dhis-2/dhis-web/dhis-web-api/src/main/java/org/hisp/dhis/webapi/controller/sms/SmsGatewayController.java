/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.webapi.controller.sms;

<<<<<<< HEAD
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
=======
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
<<<<<<< HEAD
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.sms.config.SmsConfigurationManager;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.sms.config.views.SmsConfigurationViews;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */

@RestController
@RequestMapping( value = "/gateways" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class SmsGatewayController
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private GatewayAdministrationService gatewayAdminService;

    @Autowired
    private SmsConfigurationManager smsConfigurationManager;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.GET, produces = { "application/json" } )
<<<<<<< HEAD
    public void getGateways( HttpServletResponse response ) throws IOException
=======
    public void getGateways( HttpServletResponse response )
        throws IOException
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        generateOutput( response, smsConfigurationManager.getSmsConfiguration() );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( value = "/{uid}", method = RequestMethod.GET, produces = "application/json" )
    public void getGatewayConfiguration( @PathVariable String uid, HttpServletResponse response )
<<<<<<< HEAD
        throws WebMessageException, IOException
=======
        throws WebMessageException,
        IOException
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        SmsGatewayConfig gateway = gatewayAdminService.getByUid( uid );

        if ( gateway == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "No gateway found" ) );
        }

        generateOutput( response, gateway );
    }

    // -------------------------------------------------------------------------
    // PUT,POST
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
<<<<<<< HEAD
    @RequestMapping( value = "/clickatell", method = { RequestMethod.POST, RequestMethod.PUT }, produces = "application/json" )
    public void addOrUpdateClickatellConfiguration( HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        SmsGatewayConfig payLoad = renderService.fromJson( request.getInputStream(), ClickatellGatewayConfig.class );

        if ( gatewayAdminService.addOrUpdateGateway( payLoad, ClickatellGatewayConfig.class ) )
        {
            webMessageService.send( WebMessageUtils.ok( "SAVED" ), response, request );
        }
        else
        {
            webMessageService.send( WebMessageUtils.error( "FAILED" ), response, request );
        }
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( value = "/bulksms", method = { RequestMethod.POST, RequestMethod.PUT }, produces = "application/json" )
    public void addOrUpdatebulksmsConfiguration( HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        BulkSmsGatewayConfig payLoad = renderService.fromJson( request.getInputStream(), BulkSmsGatewayConfig.class );

        if ( gatewayAdminService.addOrUpdateGateway( payLoad, BulkSmsGatewayConfig.class ) )
        {
            webMessageService.send( WebMessageUtils.ok( "SAVED" ), response, request );
        }
        else
        {
            webMessageService.send( WebMessageUtils.error( "FAILED" ), response, request );
        }
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( value = "/generichttp", method = { RequestMethod.POST, RequestMethod.PUT }, produces = "application/json" )
    public void addOrUpdateGenericConfiguration( HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        SmsGatewayConfig payLoad = renderService.fromJson( request.getInputStream(),
                GenericHttpGatewayConfig.class );

        if ( gatewayAdminService.addGateway( payLoad ) )
        {
            webMessageService.send( WebMessageUtils.ok( "SAVED" ), response, request );
        }
        else
        {
            webMessageService.send( WebMessageUtils.error( "FAILED" ), response, request );
        }
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    @RequestMapping( value = "/default/{uid}", method = RequestMethod.PUT )
    public void setDefault( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        SmsGatewayConfig gateway = gatewayAdminService.getByUid( uid );

        if ( gateway == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "No gateway found" ) );
        }

        gatewayAdminService.setDefaultGateway( gateway );

        webMessageService.send( WebMessageUtils.ok( gateway.getName() + " is set to default" ), response, request );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT )
    public void updateGateway( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException,
        IOException
    {
        SmsGatewayConfig config = gatewayAdminService.getByUid( uid );

        if ( config == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "No gateway found" ) );
        }

        SmsGatewayConfig updatedConfig = renderService.fromJson( request.getInputStream(), SmsGatewayConfig.class );

        if ( gatewayAdminService.hasDefaultGateway() && updatedConfig.isDefault() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Default gateway already exists" ) );
        }

        gatewayAdminService.updateGateway( config, updatedConfig );

        webMessageService.send( WebMessageUtils.ok( String.format( "Gateway with uid: %s has been updated", uid ) ),
            response, request );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.POST )
    public void addGateway( HttpServletRequest request, HttpServletResponse response )
<<<<<<< HEAD
            throws IOException, WebMessageException
=======
        throws IOException,
        WebMessageException
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
<<<<<<< HEAD
        SmsGatewayConfig config = renderService.fromJson( request.getInputStream(),  SmsGatewayConfig.class );
=======
        SmsGatewayConfig config = renderService.fromJson( request.getInputStream(), SmsGatewayConfig.class );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        if ( config == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Cannot de-serialize SMS configurations" ) );
        }

        gatewayAdminService.addGateway( config );
        webMessageService.send( WebMessageUtils.ok( "Gateway configuration added" ), response, request );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    public void removeGateway( @PathVariable String uid, HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        SmsGatewayConfig gateway = gatewayAdminService.getByUid( uid );

        if ( gateway == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "No gateway found with id: " + uid ) );
        }

        gatewayAdminService.removeGatewayByUid( uid );

        webMessageService.send( WebMessageUtils.ok( "Gateway removed successfully" ), response, request );
    }

<<<<<<< HEAD
    private void generateOutput( HttpServletResponse response, Object value ) throws IOException
    {
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.disable( MapperFeature.DEFAULT_VIEW_INCLUSION );
        jsonMapper.writerWithView( SmsConfigurationViews.Public.class )
                .writeValue( response.getOutputStream(), value );
=======
    private void generateOutput( HttpServletResponse response, Object value )
        throws IOException
    {
        response.setContentType( "application/json" );

        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.disable( MapperFeature.DEFAULT_VIEW_INCLUSION );
        jsonMapper.writerWithView( SmsConfigurationViews.Public.class )
            .writeValue( response.getOutputStream(), value );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }
}
