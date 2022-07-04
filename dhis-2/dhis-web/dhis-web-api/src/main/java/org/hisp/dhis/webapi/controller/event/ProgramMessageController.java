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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageBatch;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */
@RestController
@RequestMapping( value = "/messages" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ProgramMessageController
    extends AbstractCrudController<ProgramMessage>
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramMessageService programMessageService;

    @Autowired
    private RenderService renderService;

    @Autowired
    @Qualifier( "org.hisp.dhis.program.notification.ProgramNotificationInstanceStore" )
    private IdentifiableObjectStore<ProgramNotificationInstance> programNotificationInstanceStore;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.GET, produces = { "application/json" } )
    public void getProgramMessages( @RequestParam( required = false ) Set<String> ou,
        @RequestParam( required = false ) String programInstance,
        @RequestParam( required = false ) String programStageInstance,
        @RequestParam( required = false ) ProgramMessageStatus messageStatus,
        @RequestParam( required = false ) Date afterDate, @RequestParam( required = false ) Date beforeDate,
        @RequestParam( required = false ) Integer page, @RequestParam( required = false ) Integer pageSize,
        HttpServletRequest request, HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        ProgramMessageQueryParams params = programMessageService.getFromUrl( ou, programInstance, programStageInstance,
            messageStatus, page, pageSize, afterDate, beforeDate );

        if ( programInstance == null && programStageInstance == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "ProgramInstance or ProgramStageInstance must be specified." ) );
        }

        List<ProgramMessage> programMessages = programMessageService.getProgramMessages( params );

        renderService.toJson( response.getOutputStream(), programMessages );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( value = "/scheduled", method = RequestMethod.GET )
    public void getScheduledMessage( @RequestParam( required = false ) Date scheduledAt, HttpServletResponse response )
        throws IOException
    {
        List<ProgramNotificationInstance> instances = programNotificationInstanceStore.getAll();

        if ( scheduledAt != null )
        {
            instances = instances.parallelStream().filter( Objects::nonNull )
                .filter( i -> scheduledAt.equals( i.getScheduledAt() ) )
                .collect( Collectors.toList() );
        }

        renderService.toJson( response.getOutputStream(), instances );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS')" )
    @RequestMapping( value = "/scheduled/sent", method = RequestMethod.GET )
    public void getScheduledSentMessage(
        @RequestParam( required = false ) String programInstance,
        @RequestParam( required = false ) String programStageInstance,
        @RequestParam( required = false ) Date afterDate, @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        HttpServletResponse response )
        throws IOException
    {
        ProgramMessageQueryParams params = programMessageService.getFromUrl( null, programInstance,
            programStageInstance,
            null, page, pageSize, afterDate, null );

        List<ProgramMessage> programMessages = programMessageService.getProgramMessages( params );

        renderService.toJson( response.getOutputStream(), programMessages );
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SENDSMS') or hasRole('F_SEND_EMAIL')" )
    @RequestMapping( method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" } )
    public void saveMessages( HttpServletRequest request, HttpServletResponse response )
        throws IOException,
        WebMessageException
    {
        ProgramMessageBatch batch = renderService.fromJson( request.getInputStream(), ProgramMessageBatch.class );

        for ( ProgramMessage programMessage : batch.getProgramMessages() )
        {
            programMessageService.validatePayload( programMessage );
        }

        BatchResponseStatus status = programMessageService.sendMessages( batch.getProgramMessages() );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        renderService.toJson( response.getOutputStream(), status );
    }
}
