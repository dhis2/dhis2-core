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
package org.hisp.dhis.webapi.controller.sms;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */
@RestController
@RequestMapping( value = "/sms/inbound" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class SmsInboundController extends AbstractCrudController<IncomingSms>
{
    private final WebMessageService webMessageService;

    private final IncomingSmsService incomingSMSService;

    private final RenderService renderService;

    private final SMSCommandService smsCommandService;

    private final UserService userService;

    private final CurrentUserService currentUserService;

    public SmsInboundController(
        WebMessageService webMessageService,
        IncomingSmsService incomingSMSService,
        RenderService renderService,
        SMSCommandService smsCommandService,
        UserService userService,
        CurrentUserService currentUserService )
    {
        this.webMessageService = webMessageService;
        this.incomingSMSService = incomingSMSService;
        this.renderService = renderService;
        this.smsCommandService = smsCommandService;
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, produces = { "application/json" } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SETTINGS')" )
    public void receiveSMSMessage( @RequestParam String originator, @RequestParam( required = false ) Date receivedTime,
        @RequestParam String message, @RequestParam( defaultValue = "Unknown", required = false ) String gateway,
        HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        if ( originator == null || originator.length() <= 0 )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Originator must be specified" ) );
        }

        if ( message == null || message.length() <= 0 )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Message must be specified" ) );
        }

        long smsId = incomingSMSService.save( message, originator, gateway, receivedTime,
            getUserByPhoneNumber( originator, message ) );

        webMessageService.send( WebMessageUtils.ok( "Received SMS: " + smsId ), response, request );
    }

    @RequestMapping( method = RequestMethod.POST, consumes = { "application/json" }, produces = { "application/json" } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SETTINGS')" )
    public void receiveSMSMessage( HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException,
        IOException
    {
        IncomingSms sms = renderService.fromJson( request.getInputStream(), IncomingSms.class );
        sms.setCreatedBy( getUserByPhoneNumber( sms.getOriginator(), sms.getText() ) );

        long smsId = incomingSMSService.save( sms );

        webMessageService.send( WebMessageUtils.ok( "Received SMS: " + smsId ), response, request );
    }

    @RequestMapping( value = "/import", method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SETTINGS')" )
    public void importUnparsedSMSMessages( HttpServletRequest request, HttpServletResponse response )
    {
        List<IncomingSms> importMessageList = incomingSMSService.getAllUnparsedMessages();

        for ( IncomingSms sms : importMessageList )
        {
            incomingSMSService.update( sms );
        }

        webMessageService.send( WebMessageUtils.ok( "Import successful" ), response, request );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE, produces = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SETTINGS')" )
    public void deleteInboundMessage( @PathVariable String uid, HttpServletRequest request,
        HttpServletResponse response )
        throws WebMessageException
    {
        IncomingSms sms = incomingSMSService.get( uid );

        if ( sms == null )
        {
            throw new WebMessageException( notFound( "No IncomingSms with id '" + uid + "' was found." ) );
        }

        incomingSMSService.delete( uid );

        webMessageService.send( WebMessageUtils.ok( "IncomingSms with " + uid + " deleted" ), response, request );
    }

    @RequestMapping( method = RequestMethod.DELETE, produces = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_MOBILE_SETTINGS')" )
    public void deleteInboundMessages( @RequestParam List<String> ids, HttpServletRequest request,
        HttpServletResponse response )
    {
        ids.forEach( incomingSMSService::delete );

        webMessageService.send( WebMessageUtils.ok( "Objects deleted" ), response, request );
    }

    // -------------------------------------------------------------------------
    // SUPPORTIVE METHOD
    // -------------------------------------------------------------------------

    private User getUserByPhoneNumber( String phoneNumber, String text )
        throws WebMessageException
    {
        SMSCommand unregisteredParser = smsCommandService.getSMSCommand( SmsUtils.getCommandString( text ),
            ParserType.UNREGISTERED_PARSER );

        List<User> users = userService.getUsersByPhoneNumber( phoneNumber );

        User currentUser = currentUserService.getCurrentUser();

        if ( SmsUtils.isBase64( text ) )
        {
            return handleCompressedCommands( currentUser, phoneNumber );
        }

        if ( users == null || users.isEmpty() )
        {
            if ( unregisteredParser != null )
            {
                return null;
            }

            // No user belong to this phone number
            throw new WebMessageException(
                WebMessageUtils.conflict( "User's phone number is not registered in the system" ) );
        }

        return users.iterator().next();
    }

    private User handleCompressedCommands( User currentUser, String phoneNumber )
        throws WebMessageException
    {
        if ( currentUser != null && !phoneNumber.equals( currentUser.getPhoneNumber() ) )
        {
            // current user does not belong to this number
            throw new WebMessageException(
                WebMessageUtils.conflict( "Originator's number does not match user's Phone number" ) );
        }

        return currentUser;
    }
}
