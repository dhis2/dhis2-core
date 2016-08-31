package org.hisp.dhis.webapi.controller.sms;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.sms.outbound.OutboundSmsTransportService;
import org.hisp.dhis.sms.outbound.SMSServiceStatus;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@RestController
@RequestMapping( value = "/sms/service" )
public class SmsServiceController
{
    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private OutboundSmsTransportService outboundSmsTransportService;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole(' F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.GET )
    public void getSmsServiceStatus( HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        if ( outboundSmsTransportService == null )
        {
            throw new WebMessageException( WebMessageUtils.error( "Transport service is not available" ) );
        }

        SMSServiceStatus status = outboundSmsTransportService.getServiceStatusEnum();

        webMessageService.send( WebMessageUtils.ok( status.toString() ), response, request );
    }

    // -------------------------------------------------------------------------
    // POST, PUT
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole(' F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.POST )
    public void startSmsService( HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        if ( outboundSmsTransportService == null )
        {
            throw new WebMessageException( WebMessageUtils.error( "Transport service is not available" ) );
        }

        SMSServiceStatus status = outboundSmsTransportService.getServiceStatusEnum();
        
        if ( status == SMSServiceStatus.STARTED )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Service already started" ) );
        }

        outboundSmsTransportService.startService();

        webMessageService.send( WebMessageUtils.ok( "Service started" ), response, request );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole(' F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.PUT )
    public void reloadSmsService( HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        if ( outboundSmsTransportService == null )
        {
            throw new WebMessageException( WebMessageUtils.error( "Transport service is not available" ) );
        }

        outboundSmsTransportService.reloadConfig();

        webMessageService.send( WebMessageUtils.ok( "Sms configuration reloaded" ), response, request );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole(' F_MOBILE_SENDSMS')" )
    @RequestMapping( method = RequestMethod.DELETE )
    public void stopSmsService( HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException
    {
        if ( outboundSmsTransportService == null )
        {
            throw new WebMessageException( WebMessageUtils.error( "Transport service is not available" ) );
        }

        SMSServiceStatus status = outboundSmsTransportService.getServiceStatusEnum();
        
        if ( status == SMSServiceStatus.STOPPED )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Service already stopped" ) );
        }

        outboundSmsTransportService.stopService();

        webMessageService.send( WebMessageUtils.ok( "Service stopped" ), response, request );
    }
}
