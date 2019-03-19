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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.util.DeliveryReceiptState;

import java.util.ArrayList;
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
        SMPPClient smppClient = createSMPPClient( gatewayConfig );

        List<OutboundMessageResponse> messageResponses = new ArrayList<>();

        if ( smppClient != null )
        {
            smppClient.start();
            OutboundMessageResponse response = null;

            for ( OutboundMessage message : batch.getMessages() )
            {
                response = send( smppClient, message.getText(), message.getRecipients() );
                messageResponses.add( response );
            }

            smppClient.stop();
            return messageResponses;
        }

        log.error( SESSION_ERROR );

        return new ArrayList<>();
    }

    @Override
    protected SmsGatewayConfig getGatewayConfigType()
    {
        return new SMPPGatewayConfig();
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig gatewayConfig )
    {
        SMPPClient smppClient = createSMPPClient( gatewayConfig );

        OutboundMessageResponse response = new OutboundMessageResponse();
        response.setOk( false );

        if ( smppClient != null )
        {
            smppClient.stop();

            response = send( smppClient, text, recipients );

            smppClient.stop();

            return response;
        }

        log.error( SESSION_ERROR );
        response.setDescription( SESSION_ERROR );
        return response;
    }

    private OutboundMessageResponse send( SMPPClient smppClient, String text, Set<String> recipients )
    {
        SubmitMultiResult result = null;
        OutboundMessageResponse response = new OutboundMessageResponse();
        response.setOk( false );

        result = smppClient.send( text, recipients );

        if ( result != null )
        {
            if ( result.getUnsuccessDeliveries() == null || result.getUnsuccessDeliveries().length == 0 )
            {
                log.info( "Message pushed to broker successfully" );
                response.setOk( true );
                response.setDescription( result.getMessageId() );
                return  response;
            }
            else
            {
                log.error( DeliveryReceiptState.valueOf( result.getUnsuccessDeliveries()[0].getErrorStatusCode() ) + " - " +result.getMessageId() );
            }
        }

        response.setDescription( SENDING_FAILED );
        return response;
    }

    private SMPPClient createSMPPClient( SmsGatewayConfig gatewayConfig )
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
