package org.hisp.dhis.sms;

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

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.config.*;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Zubair Asghar.
 */
public class SmsGatewayTest extends DhisConvenienceTest
{
    private static final String MESSAGE = "text-MESSAGE";
    private static final String SUBJECT = "subject";
    private static final String PHONE_NUMBER = "4X000000";
    private static final String SUCCESS_RESPONSE_STRING = "0|abc|5656";
    private static final String ERROR_RESPONSE_STRING = "24|abc|5656";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BulkSmsGateway bulkSmsGateway;

    private SmsGatewayConfig smsGatewayConfig;

    private Set<String> recipients = new HashSet<>();

    private OutboundMessageBatch batch;

    private List<OutboundMessage> outboundMessageList = new ArrayList<>();

    @Before
    public void initTest()
    {
        smsGatewayConfig = new BulkSmsGatewayConfig();
        smsGatewayConfig.setDefault( true );

        recipients.add( PHONE_NUMBER );

        outboundMessageList.add( new OutboundMessage( SUBJECT,MESSAGE, recipients ) );
        outboundMessageList.add( new OutboundMessage( SUBJECT,MESSAGE, recipients ) );
        outboundMessageList.add( new OutboundMessage( SUBJECT,MESSAGE, recipients ) );

        batch = new OutboundMessageBatch( outboundMessageList, DeliveryChannel.SMS );
    }

    @Test
    public void testAccept()
    {
        boolean result = bulkSmsGateway.accept( smsGatewayConfig );

        assertTrue( result );

        smsGatewayConfig = new GenericHttpGatewayConfig();
        result = bulkSmsGateway.accept( smsGatewayConfig );

        assertFalse( result );
    }

    @Test
    public void testSuccessful()
    {
        ResponseEntity<String> successResponse = new ResponseEntity<>( SUCCESS_RESPONSE_STRING, HttpStatus.OK );

        when( restTemplate.exchange( any(), any() , any(), eq( String.class ) ) )
            .thenReturn( successResponse );

        OutboundMessageResponse status = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status );
        assertEquals( GatewayResponse.RESULT_CODE_0, status.getResponseObject() );
    }

    @Test
    public void testBulkSend()
    {
        ResponseEntity<String> successResponse = new ResponseEntity<>( SUCCESS_RESPONSE_STRING, HttpStatus.OK );

        when( restTemplate.exchange( any(), any() , any(), eq( String.class ) ) )
            .thenReturn( successResponse );

        List<OutboundMessageResponse> responses = bulkSmsGateway.sendBatch( batch, smsGatewayConfig );

        assertNotNull( responses );
        assertEquals( 3, responses.size() );
    }

    @Test
    public void testFailureCode()
    {
        ResponseEntity<String> errorResponse = new ResponseEntity<>( ERROR_RESPONSE_STRING, HttpStatus.CONFLICT );

        when( restTemplate.exchange( any(), any() , any(), eq( String.class ) ) )
            .thenReturn( errorResponse );

        OutboundMessageResponse status2 = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status2 );
        assertFalse( status2.isOk() );
        assertEquals( GatewayResponse.RESULT_CODE_24, status2.getResponseObject() );
    }

    @Test
    public void testFailure()
    {
        when( restTemplate.exchange( any(), any() , any(), eq( String.class ) ) )
            .thenReturn( null );

        OutboundMessageResponse status2 = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status2 );
        assertFalse( status2.isOk() );
        assertEquals( GatewayResponse.FAILED, status2.getResponseObject() );
    }

    @Test
    public void testException()
    {
        when( restTemplate.exchange( any(), any() , any(), eq( String.class ) ) )
            .thenThrow( HttpClientErrorException.class );

        OutboundMessageResponse status2 = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status2 );
        assertFalse( status2.isOk() );
        assertEquals( GatewayResponse.FAILED, status2.getResponseObject() );
    }
}
