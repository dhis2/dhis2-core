/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.config.BulkSmsGatewayConfig;
import org.hisp.dhis.sms.config.BulkSmsHttpGateway;
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
import org.hisp.dhis.sms.config.SmsGateway;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Zubair Asghar.
 */
@ExtendWith( MockitoExtension.class )
class BulkSmsGatewayTest extends DhisConvenienceTest
{

    private static final String MESSAGE = "text-MESSAGE";

    private static final String SUBJECT = "subject";

    private static final String PHONE_NUMBER = "4X000000";

    private static final String SUCCESS_RESPONSE_STRING = "0|abc|5656";

    private static final String ERROR_RESPONSE_STRING = "24|abc|5656";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PBEStringEncryptor pbeStringEncryptor;

    @InjectMocks
    private BulkSmsHttpGateway bulkSmsGateway;

    private SmsGatewayConfig smsGatewayConfig;

    private final Set<String> recipients = Set.of( PHONE_NUMBER );

    private final List<OutboundMessage> outboundMessageList = List.of(
        new OutboundMessage( SUBJECT, MESSAGE, recipients ),
        new OutboundMessage( SUBJECT, MESSAGE, recipients ),
        new OutboundMessage( SUBJECT, MESSAGE, recipients ) );

    private final OutboundMessageBatch batch = new OutboundMessageBatch( outboundMessageList, DeliveryChannel.SMS );

    @BeforeEach
    public void initTest()
    {
        smsGatewayConfig = new BulkSmsGatewayConfig();
        smsGatewayConfig.setDefault( true );
        smsGatewayConfig.setUsername( "username" );
        smsGatewayConfig.setPassword( "password" );

        lenient().when( pbeStringEncryptor.decrypt( anyString() ) ).thenReturn( smsGatewayConfig.getPassword() );
    }

    @Test
    void testAccept()
    {
        boolean result = bulkSmsGateway.accept( smsGatewayConfig );

        assertTrue( result );

        smsGatewayConfig = new GenericHttpGatewayConfig();
        result = bulkSmsGateway.accept( smsGatewayConfig );

        assertFalse( result );
    }

    @Test
    void testSuccessful()
    {
        ResponseEntity<String> successResponse = new ResponseEntity<>( SUCCESS_RESPONSE_STRING, HttpStatus.OK );

        when( restTemplate.exchange( any( String.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) )
            .thenReturn( successResponse );

        OutboundMessageResponse status = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status );
        assertTrue( SmsGateway.OK_CODES.contains( successResponse.getStatusCode() ) );
    }

    @Test
    void testBulkSend()
    {
        ResponseEntity<String> successResponse = new ResponseEntity<>( SUCCESS_RESPONSE_STRING, HttpStatus.OK );

        when( restTemplate.exchange( any( String.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) )
            .thenReturn( successResponse );

        List<OutboundMessageResponse> responses = bulkSmsGateway.sendBatch( batch, smsGatewayConfig );

        assertNotNull( responses );
        assertEquals( 3, responses.size() );
    }

    @Test
    void testFailureCode()
    {
        ResponseEntity<String> errorResponse = new ResponseEntity<>( ERROR_RESPONSE_STRING, HttpStatus.CONFLICT );

        when( restTemplate.exchange( any( String.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) )
            .thenReturn( errorResponse );

        OutboundMessageResponse status = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status );
        assertFalse( status.isOk() );
        assertEquals( GatewayResponse.FAILED, status.getResponseObject() );
    }

    @Test
    void testWhenServerResponseIsNull()
    {
        when( restTemplate.exchange( any( String.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) )
                .thenReturn( null );

        OutboundMessageResponse status2 = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status2 );
        assertFalse( status2.isOk() );
        assertEquals( GatewayResponse.RESULT_CODE_504, status2.getResponseObject() );
    }

    @Test
    void testException()
    {
        when( restTemplate.exchange( any( String.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) )
                .thenThrow( HttpClientErrorException.class );

        OutboundMessageResponse status2 = bulkSmsGateway.send( SUBJECT, MESSAGE, recipients, smsGatewayConfig );

        assertNotNull( status2 );
        assertFalse( status2.isOk() );
        assertEquals( GatewayResponse.FAILED, status2.getResponseObject() );
    }
}
