/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.outboundmessage.*;
import org.hisp.dhis.sms.config.*;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Sets;

/**
 * @author Zubair Asghar.
 */
public class SmsMessageSenderTest
{
    private static final Integer MAX_ALLOWED_RECIPIENTS = 200;

    private static final String NO_CONFIG = "No default gateway configured";

    private SmsMessageSender smsMessageSender;

    @Mock
    private UserSettingService userSettingService;

    @Mock
    private GatewayAdministrationService gatewayAdministrationService;

    @Mock
    private OutboundSmsService outboundSmsService;

    @Mock
    private BulkSmsHttpGateway bulkSmsGateway;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ArrayList<SmsGateway> smsGateways;

    private SmsGatewayConfig smsGatewayConfig;

    private OutboundMessageResponse okStatus;

    private OutboundMessageResponse failedStatus;

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

        setUp();

        smsGateways = new ArrayList<>();

        smsGateways.add( bulkSmsGateway );

<<<<<<< HEAD
        smsMessageSender = new SmsMessageSender( gatewayAdministrationService, smsGateways, userSettingService );
=======
        smsMessageSender = new SmsMessageSender( gatewayAdministrationService, smsGateways, userSettingService,
            outboundSmsService );
    }

    private void mockGateway()
    {
        // stub for SmsGateways
        when( bulkSmsGateway.accept( any() ) ).thenReturn( true );
        Mockito.lenient()
            .when( bulkSmsGateway.send( anyString(), anyString(), anySet(), isA( BulkSmsGatewayConfig.class ) ) )
            .thenReturn( okStatus );
        Mockito.lenient().when( bulkSmsGateway.sendBatch( any(), any( BulkSmsGatewayConfig.class ) ) )
            .thenReturn( summaryResponses );

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }

    private void mockGateway()
    {
        // stub for SmsGateways
        when( bulkSmsGateway.accept( any() ) ).thenReturn( true );
        Mockito.lenient().when( bulkSmsGateway.send( anyString(), anyString(), anySet(), isA( BulkSmsGatewayConfig.class ) ) )
            .thenReturn( okStatus );
        Mockito.lenient().when( bulkSmsGateway.sendBatch( any(), any( BulkSmsGatewayConfig.class ) ) ).thenReturn( summaryResponses );

    }
    
    @Test
    public void testSendMessageWithGatewayConfig()
    {
        // stub for GateAdministrationService
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        mockGateway();
        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, recipientsNormalized );

        assertNotNull( status );
        assertTrue( status.isOk() );
        assertEquals( "success", status.getDescription() );

        verify( gatewayAdministrationService, times( 1 ) ).getDefaultGateway();
        verify( bulkSmsGateway, times( 1 ) ).accept( any() );
        verify( bulkSmsGateway, times( 1 ) ).send( anyString(), anyString(), anySet(), any() );
    }

    @Test
    public void testSendMessageWithOutGatewayConfig()
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
    public void testIsConfiguredWithOutGatewayConfig()
    {
        when( gatewayAdministrationService.getGatewayConfigurationMap() ).thenReturn( new HashMap<>() );

        boolean isConfigured = smsMessageSender.isConfigured();

        assertFalse( isConfigured );
    }

    @Test
    public void testIsConfiguredWithGatewayConfig()
    {
        when( gatewayAdministrationService.getGatewayConfigurationMap() ).thenReturn( configMap );

        boolean isConfigured = smsMessageSender.isConfigured();

        assertTrue( isConfigured );
    }

    @Test
    public void testSendMessageWithListOfUsers()
    {
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        when( userSettingService.getUserSetting( any(), any() ) ).thenReturn( Boolean.TRUE );
        when( bulkSmsGateway.send( anyString(), anyString(), anySet(), isA( BulkSmsGatewayConfig.class ) ) )
<<<<<<< HEAD
                .thenReturn( okStatus );
=======
            .thenReturn( okStatus );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        when( bulkSmsGateway.accept( any() ) ).thenReturn( true );

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, footer, sender, users, false );

        assertTrue( status.isOk() );
        assertEquals( GatewayResponse.RESULT_CODE_0, status.getResponseObject() );
        assertEquals( "success", status.getDescription() );
    }

    @Test
    public void testSendMessageWithEmptyUserList()
    {
        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, footer, sender, new HashSet<>(),
            false );

        assertFalse( status.isOk() );
        assertEquals( GatewayResponse.NO_RECIPIENT, status.getResponseObject() );
        assertEquals( "no recipient", status.getDescription() );
    }

    @Test
    public void testSendMessageWithUserSMSSettingsDisabled()
    {
        when( userSettingService.getUserSetting( any(), any() ) ).thenReturn( Boolean.FALSE );

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, footer, sender, users, false );

        assertFalse( status.isOk() );
        assertEquals( GatewayResponse.SMS_DISABLED, status.getResponseObject() );
        assertEquals( "sms notifications are disabled", status.getDescription() );
    }

    @Test
    public void testSendMessageWithSingleRecipient()
    {
        when( bulkSmsGateway.accept( any() ) ).thenReturn( true );
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        when( bulkSmsGateway.send( anyString(), anyString(), anySet(), isA( BulkSmsGatewayConfig.class ) ) )
            .thenReturn( okStatus );
        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, "47401111111" );

        assertTrue( status.isOk() );
        assertEquals( GatewayResponse.RESULT_CODE_0, status.getResponseObject() );
        assertEquals( "success", status.getDescription() );
    }

    @Test
    public void testSendMessageFailed()
    {
        // stub for GateAdministrationService
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        mockGateway();

<<<<<<< HEAD
        when( bulkSmsGateway.send( anyString(), anyString(), anySet( ), isA( BulkSmsGatewayConfig.class ) ) ).thenReturn( failedStatus );
=======
        when( bulkSmsGateway.send( anyString(), anyString(), anySet(), isA( BulkSmsGatewayConfig.class ) ) )
            .thenReturn( failedStatus );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, recipientsNormalized );

        assertNotNull( status );
        assertEquals( GatewayResponse.FAILED, status.getResponseObject() );
        assertFalse( status.isOk() );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void testNumberNormalization()
    {
        // stub for GateAdministrationService
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        mockGateway();

        Set<String> tempRecipients = Sets.newHashSet();

        when( bulkSmsGateway.send( anyString(), anyString(), anySet(), any( BulkSmsGatewayConfig.class ) ) )
            .thenAnswer( invocation -> {
                tempRecipients.addAll( (Set<String>) invocation.getArguments()[2] );
                return okStatus;

            } );

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, recipientsNonNormalized );

        assertTrue( status.isOk() );
        assertEquals( GatewayResponse.RESULT_CODE_0, status.getResponseObject() );
        assertEquals( "success", status.getDescription() );

        Sets.SetView<String> setDifference = Sets.difference( tempRecipients, recipientsNormalized );

        assertEquals( 0, setDifference.size() );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void testSendMessageWithMaxRecipients()
    {
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        mockGateway();
        List<Set<String>> recipientList = new ArrayList<>();

        generateRecipients( 500 );

        when( bulkSmsGateway.send( anyString(), anyString(), anySet(), any( BulkSmsGatewayConfig.class ) ) )
            .then( invocation -> {
                recipientList.add( (Set<String>) invocation.getArguments()[2] );

                return okStatus;
            } );

        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, generatedRecipients );

        assertNotNull( status );
        assertTrue( status.isOk() );

        recipientList.forEach( set -> assertTrue( set.size() <= MAX_ALLOWED_RECIPIENTS ) );
    }

    @Test
    public void testSendMessageBatchCompleted()
    {
        mockGateway();
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        responseForCompletedBatch();

        OutboundMessageBatch batch = new OutboundMessageBatch( outboundMessages, DeliveryChannel.SMS );

        ArgumentCaptor<OutboundMessageBatch> argumentCaptor = ArgumentCaptor.forClass( OutboundMessageBatch.class );

        OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch( batch );

        assertNotNull( summary );
        assertEquals( OutboundMessageBatchStatus.COMPLETED, summary.getBatchStatus() );

        verify( bulkSmsGateway, times( 1 ) ).sendBatch( argumentCaptor.capture(), any() );
        assertEquals( batch, argumentCaptor.getValue() );

        assertEquals( 4, argumentCaptor.getValue().size() );

        assertEquals( 4, summary.getSent() );
        assertEquals( 4, summary.getTotal() );
        assertEquals( 0, summary.getFailed() );
        assertEquals( 0, summary.getPending() );
    }

    @Test
    public void testSendMessageBatchFailed()
    {
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        mockGateway();

        responseForFailedBatch();

        OutboundMessageBatch batch = new OutboundMessageBatch( outboundMessages, DeliveryChannel.SMS );

        ArgumentCaptor<OutboundMessageBatch> argumentCaptor = ArgumentCaptor.forClass( OutboundMessageBatch.class );

        OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch( batch );

        assertNotNull( summary );
        assertEquals( OutboundMessageBatchStatus.FAILED, summary.getBatchStatus() );

        verify( bulkSmsGateway, times( 1 ) ).sendBatch( argumentCaptor.capture(), any() );
        assertEquals( batch, argumentCaptor.getValue() );
        assertEquals( 4, argumentCaptor.getValue().size() );

        assertEquals( 3, summary.getSent() );
        assertEquals( 4, summary.getTotal() );
        assertEquals( 1, summary.getFailed() );
        assertEquals( 0, summary.getPending() );
    }

    @Test
    public void testSendMessageBatchWithMaxRecipients()
    {
        // stub for GateAdministrationService
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        mockGateway();

        summaryResponses.clear();

        when( bulkSmsGateway.sendBatch( any(), isA( BulkSmsGatewayConfig.class ) ) ).then( invocation -> {
            OutboundMessageBatch batch = (OutboundMessageBatch) invocation.getArguments()[0];

            summaryResponses.addAll( batch.getMessages().stream()
                .map( message -> new OutboundMessageResponse( "success", GatewayResponse.RESULT_CODE_0, true ) )
                .collect( Collectors.toList() ) );

            return summaryResponses;
        } );

        createOutBoundMessagesWithMaxRecipients();

        ArgumentCaptor<OutboundMessageBatch> argumentCaptor = ArgumentCaptor.forClass( OutboundMessageBatch.class );

        OutboundMessageBatch batch = new OutboundMessageBatch( outboundMessages, DeliveryChannel.SMS );

        OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch( batch );

        assertNotNull( summary );
        assertEquals( OutboundMessageBatchStatus.COMPLETED, summary.getBatchStatus() );

        verify( bulkSmsGateway, times( 1 ) ).sendBatch( argumentCaptor.capture(), any() );
        assertEquals( batch, argumentCaptor.getValue() );

        assertEquals( 6, argumentCaptor.getValue().size() );
        assertEquals( 6, summary.getSent() );
        assertEquals( 6, summary.getTotal() );
        assertEquals( 0, summary.getFailed() );
        assertEquals( 0, summary.getPending() );
    }

    @Test
    public void testSendMessageBatchWithOutMaxRecipients()
    {
        // stub for GateAdministrationService
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( smsGatewayConfig );
        mockGateway();

        responseForCompletedBatch();
        createOutBoundMessagesWithOutMaxRecipients();

        ArgumentCaptor<OutboundMessageBatch> argumentCaptor = ArgumentCaptor.forClass( OutboundMessageBatch.class );

        OutboundMessageBatch batch = new OutboundMessageBatch( outboundMessages, DeliveryChannel.SMS );

        OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch( batch );

        assertNotNull( summary );
        assertEquals( OutboundMessageBatchStatus.COMPLETED, summary.getBatchStatus() );

        verify( bulkSmsGateway, times( 1 ) ).sendBatch( argumentCaptor.capture(), any() );
        assertEquals( batch, argumentCaptor.getValue() );

        assertEquals( 4, argumentCaptor.getValue().size() );

    }

    @Test
    public void testSendMessageBatchWithOutGatewayConfiguration()
    {
        when( gatewayAdministrationService.getDefaultGateway() ).thenReturn( null );

        responseForFailedBatch();

        OutboundMessageBatch batch = new OutboundMessageBatch( outboundMessages, DeliveryChannel.SMS );

        OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch( batch );

        assertNotNull( summary );
        assertEquals( OutboundMessageBatchStatus.FAILED, summary.getBatchStatus() );
        assertEquals( NO_CONFIG, summary.getErrorMessage() );
    }

    @Test
    public void testIfNoRecipient()
    {
        OutboundMessageResponse status = smsMessageSender.sendMessage( subject, text, StringUtils.EMPTY );

        assertNotNull( status );
        assertFalse( status.isOk() );
        assertEquals( GatewayResponse.NO_RECIPIENT, status.getResponseObject() );
    }

    @Test
    public void testIfBatchIsNull()
    {
        OutboundMessageResponseSummary summary = smsMessageSender.sendMessageBatch( null );

        assertNotNull( summary );
        assertEquals( OutboundMessageBatchStatus.ABORTED, summary.getBatchStatus() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setUp()
    {
        okStatus = new OutboundMessageResponse();
        okStatus.setResponseObject( GatewayResponse.RESULT_CODE_0 );

        failedStatus = new OutboundMessageResponse();
        failedStatus.setResponseObject( GatewayResponse.FAILED );

        smsGatewayConfig = new BulkSmsGatewayConfig();
        smsGatewayConfig.setUrlTemplate( "" );
        smsGatewayConfig.setName( gateway );
        smsGatewayConfig.setUsername( " " );
        smsGatewayConfig.setPassword( "" );
        smsGatewayConfig.setUrlTemplate( "" );
        smsGatewayConfig.setDefault( true );

        configMap.put( gateway, smsGatewayConfig );

        OutboundMessage outboundMessageA = new OutboundMessage( subject, text, recipientsNormalized );
        OutboundMessage outboundMessageB = new OutboundMessage( subject, text, recipientsNonNormalized );
        OutboundMessage outboundMessageC = new OutboundMessage( subject, text, recipientsNormalized );
        OutboundMessage outboundMessageD = new OutboundMessage( subject, text, recipientsNonNormalized );

        outboundMessages = Arrays.asList( outboundMessageA, outboundMessageB, outboundMessageC, outboundMessageD );

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

    private void responseForFailedBatch()
    {
        summaryResponses.clear();
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add(
            new OutboundMessageResponse( GatewayResponse.FAILED.getResponseMessage(), GatewayResponse.FAILED, false ) );
    }

    private void responseForCompletedBatch()
    {
        summaryResponses.clear();
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0, true ) );
        summaryResponses.add( new OutboundMessageResponse( GatewayResponse.RESULT_CODE_0.getResponseMessage(),
            GatewayResponse.RESULT_CODE_0, true ) );
    }

    private void createOutBoundMessagesWithMaxRecipients()
    {
        generateRecipients( 500 );

        OutboundMessage outboundMessageA = new OutboundMessage( subject, text, generatedRecipients );
        OutboundMessage outboundMessageB = new OutboundMessage( subject, text, recipientsNonNormalized );
        OutboundMessage outboundMessageC = new OutboundMessage( subject, text, recipientsNormalized );
        OutboundMessage outboundMessageD = new OutboundMessage( subject, text, recipientsNonNormalized );

        outboundMessages = Arrays.asList( outboundMessageA, outboundMessageB, outboundMessageC, outboundMessageD );
    }

    private void createOutBoundMessagesWithOutMaxRecipients()
    {
        OutboundMessage outboundMessageA = new OutboundMessage( subject, text, recipientsNormalized );
        OutboundMessage outboundMessageB = new OutboundMessage( subject, text, recipientsNonNormalized );
        OutboundMessage outboundMessageC = new OutboundMessage( subject, text, recipientsNormalized );
        OutboundMessage outboundMessageD = new OutboundMessage( subject, text, recipientsNonNormalized );

        outboundMessages = Arrays.asList( outboundMessageA, outboundMessageB, outboundMessageC, outboundMessageD );
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

            generatedRecipients.add( temp );
        }
    }
}
