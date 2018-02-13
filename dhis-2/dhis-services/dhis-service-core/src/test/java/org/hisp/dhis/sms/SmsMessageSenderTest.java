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

import com.google.common.collect.Sets;
import org.apache.commons.lang.RandomStringUtils;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.sms.config.*;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

/**
 * @Author Zubair Asghar.
 */
@RunWith( MockitoJUnitRunner.class )
public class SmsMessageSenderTest
{
    private static final Integer MAX_ALLOWED_RECIPIENTS = 200;

    private static final String NO_CONFIG = "No default gateway configured";

    @InjectMocks
    private SmsMessageSender smsMessageSender;

    @Mock
    private UserSettingService userSettingService;

    @Mock
    private GatewayAdministrationService gatewayAdministrationService;

    @Mock
    private BulkSmsGateway bulkSmsGateway;

    @Spy
    private ArrayList<SmsGateway> smsGateways;

    private SmsGatewayConfig smsGatewayConfig;

    private OutboundMessageResponse okStatus;

    private List<OutboundMessageResponse> summaryResponses = new ArrayList<>();

    private List<OutboundMessage> outboundMessages = new ArrayList<>();

    private Set<String> recipientsNonNormalized = Sets.newHashSet( "+4740222222", "0047407777777" );

    private Set<String> recipientsNormalized = Sets.newHashSet( "4740222222", "47407777777" );

    private Set<String> generatedRecipients = Sets.newHashSet();

    private Set<User> users = new HashSet<>();

    private User sender;

    private Map<String, SmsGatewayConfig> configMap = new HashMap<>();

    private String subject = "subject";

    private String text = "text message";

    private String gateway = "bulksms";

    private String footer = "footer";

    @Before
    public void initTest()
    {
        setup();

        smsGateways.add( bulkSmsGateway );

        // stub for GateAdministrationService
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        when( gatewayAdministrationService.getGatewayConfigurationMap() ).thenReturn( configMap );

        // stub for UserSettingService
        when( userSettingService.getUserSetting( any(), any() ) ).thenReturn( new Boolean( true ) );

        // stub for SmsGateways
        when ( bulkSmsGateway.accept( any() ) ).thenReturn( true );
        when( bulkSmsGateway.send( anyString(), anyString(), anySetOf( String.class ), Matchers.isA( BulkSmsGatewayConfig.class ) ) ).thenReturn( okStatus );
        when ( bulkSmsGateway.sendBatch( any(), Matchers.isA( BulkSmsGatewayConfig.class ) ) ).thenReturn( summaryResponses );
    }

    @Test
    public void test_sendMessageWithGatewayConfig()
    {
        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, recipientsNormalized );

        assertNotNull( status );
        assertTrue( status.isOk() );
        assertEquals( "success", status.getDescription() );

        verify( gatewayAdministrationService, times( 1 ) ).getDefaultGateway();
        verify( bulkSmsGateway, times( 1 ) ).accept( any() );
        verify( bulkSmsGateway, times( 1 ) ).send( anyString(), anyString(), anySetOf( String.class ), any() );
    }

    @Test
    public void test_sendMessageWithOutGatewayConfig()
    {
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( null );

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, recipientsNormalized );

        assertNotNull( status );
        assertFalse( status.isOk() );
        assertEquals( GatewayResponse.NO_GATEWAY_CONFIGURATION, status.getResponseObject() );
        assertEquals( NO_CONFIG, status.getDescription() );

        verify( gatewayAdministrationService, times( 1 ) ).getDefaultGateway();
        verify( bulkSmsGateway, never() ).accept( smsGatewayConfig );
    }

    @Test
    public void test_isConfiguredWithOutGatewayConfig()
    {
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( null );
        when( gatewayAdministrationService.getGatewayConfigurationMap() ).thenReturn( new HashMap<>() );

        boolean isConfigured = smsMessageSender.isConfigured();

        assertFalse( isConfigured );
    }

    @Test
    public void test_isConfiguredWithGatewayConfig()
    {
        boolean isConfigured = smsMessageSender.isConfigured();

        assertTrue( isConfigured );
    }

    @Test
    public void test_sendMessageWithListOfUsers()
    {
        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, footer, sender, users, false );

        assertTrue( status.isOk() );
        assertEquals( GatewayResponse.RESULT_CODE_0, status.getResponseObject() );
        assertEquals( "success", status.getDescription() );
    }

    @Test
    public void test_numberNormalization()
    {
        Set<String> temp_recipients = Sets.newHashSet();

        when( bulkSmsGateway.send( anyString(), anyString(), anySetOf( String.class ), Matchers.isA( BulkSmsGatewayConfig.class ) ) ).thenAnswer( invocation ->
        {
            temp_recipients.addAll( (Collection) invocation.getArguments()[2] );
            return okStatus;

        });

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, recipientsNonNormalized );

        assertTrue( status.isOk() );
        assertEquals( GatewayResponse.RESULT_CODE_0, status.getResponseObject() );
        assertEquals( "success", status.getDescription() );

        Sets.SetView<String> setDifference = Sets.difference( temp_recipients, recipientsNormalized);

        assertEquals( 0, setDifference.size() );
    }

    @Test
    public void test_sendMessageWithMaxRecipients()
    {
        List<Set<String>> recipientList = new ArrayList<>();

        generateRecipients( 500 );

        when( bulkSmsGateway.send( anyString(), anyString(), anySetOf( String.class ), Matchers.isA( BulkSmsGatewayConfig.class ) ) ).then( invocation ->
        {
            recipientList.add( (Set) invocation.getArguments()[2] );

            return okStatus;
        });

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, generatedRecipients );

        assertNotNull( status );
        assertTrue( status.isOk() );

        recipientList.stream().forEach( set -> assertTrue( set.size() <= MAX_ALLOWED_RECIPIENTS ) );
    }

    @Test
    public void test_sendMessageBatch()
    {
    }

    @Test
    public void test_sendMessageBatchWithMaxRecipients()
    {
    }

    private void setup()
    {
        okStatus = new OutboundMessageResponse();
        okStatus.setResponseObject( GatewayResponse.RESULT_CODE_0 );

        smsGatewayConfig = new BulkSmsGatewayConfig();
        smsGatewayConfig.setUrlTemplate("");
        smsGatewayConfig.setName(gateway);
        smsGatewayConfig.setUsername(" ");
        smsGatewayConfig.setPassword("");
        smsGatewayConfig.setUrlTemplate("");
        smsGatewayConfig.setDefault(true);

        configMap.put(gateway, smsGatewayConfig);

        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(), GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(), GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.FAILED.getResponseMessage(), GatewayResponse.FAILED, false ) );

        OutboundMessage outboundMessageA = new OutboundMessage( subject, text, recipientsNormalized );

        User userA = new User();
        userA.setPhoneNumber( "47401111111" );

        User userB = new User();
        userB.setPhoneNumber( "47402222222" );

        User userC = new User();
        userC.setPhoneNumber( "47403333333" );

        User userD = new User();
        userD.setPhoneNumber( "47404444444" );

        users = Sets.newHashSet( userA, userB, userC, userD );
        sender = new User();
        sender.setPhoneNumber( "4740555555" );
    }

    private void generateRecipients( int size )
    {
        generatedRecipients.clear();

        for ( int i = 0; i < size; i++ )
        {
            String temp = RandomStringUtils.random( 10, false, true );

            if ( generatedRecipients.contains( temp ) )
            {
                i--;
                continue;
            }
            else
            {
                generatedRecipients.add( temp );
            }
        }
    }
}
