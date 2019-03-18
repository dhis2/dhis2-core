package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;

import java.util.List;
import java.util.Set;

/**
 * @Author Zubair Asghar.
 */
public class SMPPGateway extends SmsGateway
{
    private static final Log log = LogFactory.getLog( SMPPGateway.class );

    private static final String SESSION_ERROR = "SMPP Session cannot be null";
    private static final String SENDING_FAILED = "SMS sending failed";

    @Override
    public List<OutboundMessageResponse> sendBatch( OutboundMessageBatch batch, SmsGatewayConfig gatewayConfig )
    {
        SMPPClient smppClient = getSMPPClient( gatewayConfig );
        return null;
    }

    @Override
    protected SmsGatewayConfig getGatewayConfigType()
    {
        return new SMPPGatewayConfig();
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig gatewayConfig )
    {
        SMPPClient smppClient = getSMPPClient( gatewayConfig );

        OutboundMessageResponse response = new OutboundMessageResponse();

        String messageId = null;

        if ( smppClient != null )
        {
            smppClient.startSMPPSession();

            messageId = smppClient.send( text, StringUtils.join( recipients, "," ) );

            if ( messageId != null )
            {
                response.setOk( true );
                response.setDescription( messageId );
            }
            else
            {
                response.setOk( false );
                response.setDescription( SENDING_FAILED );
            }

            return response;
        }

        response.setOk( false );
        response.setDescription( SESSION_ERROR );

        return response;
    }

    private SMPPClient getSMPPClient( SmsGatewayConfig gatewayConfig )
    {
        if ( gatewayConfig == null )
        {
            log.error( "Gateway cannot be null" );
            return null;
        }

        SMPPGatewayConfig smppGatewayConfig = (SMPPGatewayConfig) gatewayConfig;

        return new SMPPClient( smppGatewayConfig );
    }
}
