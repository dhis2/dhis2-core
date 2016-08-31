package org.hisp.dhis.sms.outbound;

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

import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.SmsGatewayConfig;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */
public class DefaultOutboundSmsTransportService
    implements OutboundSmsTransportService
{
    private static final Log log = LogFactory.getLog( DefaultOutboundSmsTransportService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private OutboundSmsService outboundSmsService;

    public void setOutboundSmsService( OutboundSmsService outboundSmsService )
    {
        this.outboundSmsService = outboundSmsService;
    }

    @Autowired
    private GatewayAdministrationService gatewayAdminService;

    @Autowired
    private List<SmsGateway> smsGateways;

    // -------------------------------------------------------------------------
    // OutboundSmsTransportService implementation
    // -------------------------------------------------------------------------

    @Override
    public GatewayResponse sendMessage( OutboundSms sms, String gatewayName )
    {
        SmsGatewayConfig gatewayConfiguration = gatewayAdminService.getGatewayConfigurationByName( gatewayName );

        if ( gatewayConfiguration == null )
        {
            return GatewayResponse.NO_GATWAY_CONFIGURATION;
        }

        return sendMessage( sms, gatewayConfiguration );
    }

    @Override
    public GatewayResponse sendMessage( OutboundSms sms )
    {
        SmsGatewayConfig gatewayConfiguration = gatewayAdminService.getDefaultGateway();

        if ( gatewayConfiguration == null )
        {
            return GatewayResponse.NO_GATWAY_CONFIGURATION;
        }

        return sendMessage( sms, gatewayConfiguration );
    }

    @Override
    public GatewayResponse sendMessage( String message, String recipient )
    {
        OutboundSms sms = new OutboundSms( message, recipient );

        return sendMessage( sms );
    }

    @Override
    public GatewayResponse sendMessage( List<OutboundSms> smsBatch )
    {
        SmsGatewayConfig gatewayConfiguration = gatewayAdminService.getDefaultGateway();

        if ( gatewayConfiguration == null )
        {
            return GatewayResponse.NO_GATWAY_CONFIGURATION;
        }

        return sendMessage( smsBatch, gatewayConfiguration );
    }

    @Override
    public GatewayResponse sendMessage( List<OutboundSms> smsBatch, String gatewayName )
    {
        SmsGatewayConfig gatewayConfiguration = gatewayAdminService.getGatewayConfigurationByName( gatewayName );

        if ( gatewayConfiguration == null )
        {
            return GatewayResponse.NO_GATWAY_CONFIGURATION;
        }

        return sendMessage( smsBatch, gatewayConfiguration );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private GatewayResponse sendMessage( OutboundSms sms, SmsGatewayConfig gatewayConfig )
    {
        for ( SmsGateway smsGateway : smsGateways )
        {
            if ( smsGateway.accept( gatewayConfig ) )
            {
                GatewayResponse gatewayResponse = smsGateway.send( sms, gatewayConfig );

                return responseHandler( Arrays.asList( sms ), gatewayResponse );
            }
        }

        return GatewayResponse.NO_GATWAY_CONFIGURATION;
    }

    private GatewayResponse sendMessage( List<OutboundSms> smsBatch, SmsGatewayConfig gatewayConfig )
    {
        for ( SmsGateway smsGateway : smsGateways )
        {
            if ( smsGateway.accept( gatewayConfig ) )
            {
                GatewayResponse gatewayResponse = smsGateway.send( smsBatch, gatewayConfig );

                return responseHandler( smsBatch, gatewayResponse );
            }
        }

        return GatewayResponse.NO_GATWAY_CONFIGURATION;
    }

    private GatewayResponse responseHandler( Collection<OutboundSms> smsBatch, GatewayResponse gatewayResponse )
    {
        Set<GatewayResponse> okCodes = Sets.newHashSet( GatewayResponse.RESULT_CODE_0, 
            GatewayResponse.RESULT_CODE_200, GatewayResponse.RESULT_CODE_202 );
        
        if ( okCodes.contains( gatewayResponse ) )
        {
            for ( OutboundSms sms : smsBatch )
            {
                sms.setStatus( OutboundSmsStatus.SENT );
                saveMessage( sms );

                log.info( "SMS sent" );
            }

            return GatewayResponse.SENT;
        }
        else
        {
            for ( OutboundSms sms : smsBatch )
            {
                sms.setStatus( OutboundSmsStatus.ERROR );
                saveMessage( sms );

                log.info( "Message failed" );
                log.info( "Failure cause: " + gatewayResponse );
            }

            return gatewayResponse;
        }
    }

    private void saveMessage( OutboundSms sms )
    {
        if ( sms.getId() == 0 )
        {
            outboundSmsService.saveOutboundSms( sms );
        }
        else
        {
            outboundSmsService.updateOutboundSms( sms );
        }
    }
}
