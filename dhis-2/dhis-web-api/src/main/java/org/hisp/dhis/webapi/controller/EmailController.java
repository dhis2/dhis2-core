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
package org.hisp.dhis.webapi.controller;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.email.Email;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@Controller
@RequestMapping( value = EmailController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class EmailController
{
    public static final String RESOURCE_PATH = "/email";

    private static final String SMTP_ERROR = "SMTP server not configured";

    // --------------------------------------------------------------------------
    // Dependencies
    // --------------------------------------------------------------------------

    @Autowired
    private EmailService emailService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private WebMessageService webMessageService;

    @PostMapping( "/test" )
    public void sendTestEmail( HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        checkEmailSettings();

        if ( !currentUserService.getCurrentUser().hasEmail() )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Could not send test email, no email configured for current user" ) );
        }

        OutboundMessageResponse emailResponse = emailService.sendTestEmail();

        emailResponseHandler( emailResponse, request, response );
    }

    @PostMapping( "/notification" )
    public void sendSystemNotificationEmail( @RequestBody Email email,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        checkEmailSettings();

        boolean systemNotificationEmailValid = systemSettingManager.systemNotificationEmailValid();

        if ( !systemNotificationEmailValid )
        {
            throw new WebMessageException( WebMessageUtils
                .conflict( "Could not send email, system notification email address not set or not valid" ) );
        }

        OutboundMessageResponse emailResponse = emailService.sendSystemEmail( email );

        emailResponseHandler( emailResponse, request, response );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_SEND_EMAIL')" )
    @PostMapping( value = "/notification", produces = "application/json" )
    public void sendEmailNotification( @RequestParam Set<String> recipients, @RequestParam String message,
        @RequestParam( defaultValue = "DHIS 2" ) String subject,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        checkEmailSettings();

        OutboundMessageResponse emailResponse = emailService.sendEmail( subject, message, recipients );

        emailResponseHandler( emailResponse, request, response );
    }

    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private void emailResponseHandler( OutboundMessageResponse emailResponse, HttpServletRequest request,
        HttpServletResponse response )
    {
        if ( emailResponse.isOk() )
        {
            String msg = !StringUtils.isEmpty( emailResponse.getDescription() ) ? emailResponse.getDescription()
                : EmailResponse.SENT.getResponseMessage();
            webMessageService.send( WebMessageUtils.ok( msg ), response, request );
        }
        else
        {
            String msg = !StringUtils.isEmpty( emailResponse.getDescription() ) ? emailResponse.getDescription()
                : EmailResponse.FAILED.getResponseMessage();
            webMessageService.send( WebMessageUtils.error( msg ), response, request );
        }
    }

    private void checkEmailSettings()
        throws WebMessageException
    {
        if ( !emailService.emailConfigured() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( SMTP_ERROR ) );
        }
    }
}
