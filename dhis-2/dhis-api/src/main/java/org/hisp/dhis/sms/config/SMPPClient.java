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
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.Address;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.ReplaceIfPresentFlag;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.DeliveryReceiptState;
import org.jsmpp.util.TimeFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @Author Zubair Asghar.
 */

public class SMPPClient
{
    private static final Log LOGGER = LogFactory.getLog( SMPPClient.class );
    private static final String SOURCE = "DHIS2";
    private static final TimeFormatter TIME_FORMATTER = new AbsoluteTimeFormatter();
    private static final String SENDING_FAILED = "SMS sending failed";
    private static final String SESSION_ERROR = "SMPP Session cannot be null";

    public OutboundMessageResponse send( String text, Set<String> recipients, SMPPGatewayConfig config )
    {
        SMPPSession session = start( config );

        if ( session == null )
        {
            return new OutboundMessageResponse( SESSION_ERROR, null, false );
        }

        OutboundMessageResponse response = new OutboundMessageResponse();

        SubmitMultiResult result = send( session, text, recipients );

        stop( session );

        if ( result != null )
        {
            if ( result.getUnsuccessDeliveries() == null || result.getUnsuccessDeliveries().length == 0 )
            {
                LOGGER.info( "Message pushed to broker successfully" );
                response.setOk( true );
                response.setDescription( result.getMessageId() );
                return  response;
            }
            else
            {
                LOGGER.error( DeliveryReceiptState.valueOf( result.getUnsuccessDeliveries()[0].getErrorStatusCode() ) + " - " + result.getMessageId() );
                response.setDescription( DeliveryReceiptState.valueOf( result.getUnsuccessDeliveries()[0].getErrorStatusCode() ) + " - " + result.getMessageId() );
                return response;
            }
        }

        response.setDescription( SENDING_FAILED );
        return response;
    }

    public List<OutboundMessageResponse> sendBatch( OutboundMessageBatch batch, SMPPGatewayConfig config )
    {
        SMPPSession session = start( config );

        if ( session == null )
        {
            return new ArrayList<>();
        }

        List<OutboundMessageResponse> responses = new ArrayList<>();

        SubmitMultiResult result;

        for ( OutboundMessage message : batch.getMessages() )
        {
            OutboundMessageResponse response = new OutboundMessageResponse();

            result = send( session, message.getText(), message.getRecipients() );

            if ( result != null )
            {
                if ( result.getUnsuccessDeliveries() == null || result.getUnsuccessDeliveries().length == 0 )
                {
                    LOGGER.info( "Message pushed to broker successfully" );
                    response.setOk( true );
                    response.setDescription( result.getMessageId() );
                    responses.add( response );
                    continue;
                }
                else
                {
                    LOGGER.error( DeliveryReceiptState.valueOf( result.getUnsuccessDeliveries()[0].getErrorStatusCode() ) + " - " + result.getMessageId() );
                    response.setDescription( DeliveryReceiptState.valueOf( result.getUnsuccessDeliveries()[0].getErrorStatusCode() ) + " - " + result.getMessageId() );
                }
            }
            else
            {
                response.setDescription( SENDING_FAILED );
            }

            responses.add( response );
        }

        stop( session );

        return responses;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private SubmitMultiResult send( SMPPSession session, String text, Set<String> recipients )
    {
        SubmitMultiResult result = null;
        try
        {
            result = session.submitMultiple( "cp", TypeOfNumber.NATIONAL, NumberingPlanIndicator.UNKNOWN, SOURCE, getAddresses( recipients ), new ESMClass(), (byte) 0, (byte) 1, TIME_FORMATTER.format( new Date() ), null,
                    new RegisteredDelivery( SMSCDeliveryReceipt.FAILURE ), ReplaceIfPresentFlag.REPLACE, new GeneralDataCoding( Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false ), (byte) 0,
                    text.getBytes() );

            LOGGER.info( String.format( "Messages submitted, result is %s", result.getMessageId() ) );
        }
        catch ( PDUException e )
        {
            LOGGER.error( "Invalid PDU parameter" );
            DebugUtils.getStackTrace( e );
        }
        catch ( ResponseTimeoutException e )
        {
            LOGGER.error( "Response timeout" );
            DebugUtils.getStackTrace( e );
        }
        catch ( InvalidResponseException e )
        {
            LOGGER.error("Receive invalid response" );
            DebugUtils.getStackTrace( e );
        }
        catch ( NegativeResponseException e )
        {
            LOGGER.error( "Receive negative response" );
            DebugUtils.getStackTrace( e );
        }
        catch ( IOException e )
        {
            LOGGER.error( "I/O error" );
            DebugUtils.getStackTrace( e );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Exception in submitting SMPP request" );
            DebugUtils.getStackTrace( e );
        }

        return result;
    }

    private void stop( SMPPSession session )
    {
        if( session != null )
        {
            session.unbindAndClose();
        }
    }

    private SMPPSession start( SMPPGatewayConfig config )
    {
        SMPPSession session;

        try
        {
            session = new SMPPSession( config.getHost(), config.getPort(), getBindParameters( config ) );

            LOGGER.info( "SMPP client connected SMSC" );
            return session;
        }
        catch ( IOException e )
        {
            LOGGER.error( "I/O error occured" );
            DebugUtils.getStackTrace( e );
        }

        return null;
    }

    private BindParameter getBindParameters( SMPPGatewayConfig config )
    {
       return new BindParameter(BindType.BIND_TX, config.getSystemId(), config.getPassword(), config.getSystemType(),
            TypeOfNumber.UNKNOWN, NumberingPlanIndicator.UNKNOWN, null );
    }

    private Address[] getAddresses( Set<String> recipients )
    {
        Address[] addresses = new Address[recipients.size()];
        int i = 0;

        for( String number : recipients )
        {
            addresses[i] = new Address(TypeOfNumber.NATIONAL, NumberingPlanIndicator.UNKNOWN, number );
            i++;
        }

        return addresses;
    }
}
