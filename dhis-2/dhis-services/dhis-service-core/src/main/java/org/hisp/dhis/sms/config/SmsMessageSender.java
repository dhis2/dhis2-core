package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import static org.hisp.dhis.commons.util.TextUtils.LN;

/**
 * @author Nguyen Kim Lai
 */
public class SmsMessageSender
    implements MessageSender
{
    private static final Log log = LogFactory.getLog( SmsMessageSender.class );

    private static final String NO_CONFIG = "No default gateway configured";

    private static final Pattern SUMMARY_PATTERN = Pattern.compile( "\\s*High\\s*[0-9]*\\s*,\\s*medium\\s*[0-9]*\\s*,\\s*low\\s*[0-9]*\\s*" );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private GatewayAdministrationService gatewayAdminService;

    @Autowired
    private List<SmsGateway> smsGateways;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserSettingService userSettingService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public OutboundMessageResponse sendMessage( String subject, String text, String footer, User sender, Set<User> users,
        boolean forceSend )
    {
        Set<User> toSendList = new HashSet<>();

        User currentUser = currentUserService.getCurrentUser();

        if ( !forceSend )
        {
            for ( User user : users )
            {
                if ( currentUser == null || !currentUser.equals( user ) )
                {
                    if ( isQualifiedReceiver( user ) )
                    {
                        toSendList.add( user );
                    }
                }
            }
        }
        else
        {
            toSendList.addAll( users );
        }

        Set<String> phoneNumbers = SmsUtils.getRecipientsPhoneNumber( toSendList );

        // Extract summary from text in case of COLLECTIVE_SUMMARY
        text = SUMMARY_PATTERN.matcher( text ).find() ? StringUtils.substringBefore( text, LN ) : text;

        return sendMessage( subject, text, phoneNumbers );
    }

    @Override
    public OutboundMessageResponse sendMessage( String subject, String text, String recipient )
    {
        Set<String> recipients = new HashSet<>();
        recipients.add( recipient );

        return sendMessage( subject, text, recipients );
    }

    @Override
    public OutboundMessageResponse sendMessage( String subject, String text, Set<String> recipients )
    {
        SmsGatewayConfig defaultGateway = gatewayAdminService.getDefaultGateway();

        if ( defaultGateway == null )
        {
            return new OutboundMessageResponse( NO_CONFIG, GatewayResponse.NO_GATEWAY_CONFIGURATION, false );
        }

        return sendMessage( subject, text, normalizePhoneNumbers( recipients ), defaultGateway );
    }

    @Override
    public OutboundMessageResponseSummary sendMessageBatch( OutboundMessageBatch batch )
    {
        SmsGatewayConfig defaultGateway = gatewayAdminService.getDefaultGateway();

        if ( defaultGateway == null )
        {
            return createMessageResponseSummary( NO_CONFIG, DeliveryChannel.SMS, OutboundMessageBatchStatus.FAILED, batch );
        }

        batch.getMessages().stream().forEach( item -> item.setRecipients( normalizePhoneNumbers( item.getRecipients() ) ) );

        for ( SmsGateway smsGateway : smsGateways )
        {
            if ( smsGateway.accept( defaultGateway ) )
            {
                List<OutboundMessageResponse> responses = smsGateway.sendBatch( batch, defaultGateway );

                return generateSummary( responses, batch, smsGateway );
            }
        }

        return createMessageResponseSummary( NO_CONFIG, DeliveryChannel.SMS, OutboundMessageBatchStatus.FAILED, batch );
    }

    @Override
    public boolean isConfigured()
    {
        return !gatewayAdminService.getGatewayConfigurationMap().isEmpty();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private boolean isQualifiedReceiver( User user )
    {
        if ( user.getFirstName() == null )
        {
            return true;
        }
        else
        {
            Serializable userSetting = userSettingService.getUserSetting( UserSettingKey.MESSAGE_SMS_NOTIFICATION,
                user );

            return userSetting != null ? (Boolean) userSetting : false;
        }
    }

    private OutboundMessageResponse sendMessage( String subject, String text, Set<String> recipients,
        SmsGatewayConfig gatewayConfig )
    {
        for ( SmsGateway smsGateway : smsGateways )
        {
            if ( smsGateway.accept( gatewayConfig ) )
            {
                log.info( "Sending SMS to " + recipients );

                OutboundMessageResponse status = smsGateway.send( subject, text, recipients, gatewayConfig );

                return handleResponse( status );
            }
        }

        return new OutboundMessageResponse( NO_CONFIG, GatewayResponse.NO_GATEWAY_CONFIGURATION, false );
    }

    private Set<String> normalizePhoneNumbers( Set<String> to )
    {
        return to.stream().map( SmsUtils::removePhoneNumberPrefix ).collect( Collectors.toSet() );
    }

    private OutboundMessageResponse handleResponse( OutboundMessageResponse status )
    {
        Set<GatewayResponse> okCodes = Sets.newHashSet( GatewayResponse.RESULT_CODE_0, GatewayResponse.RESULT_CODE_200,
            GatewayResponse.RESULT_CODE_202 );

        GatewayResponse gatewayResponse = (GatewayResponse) status.getResponseObject();

        if ( okCodes.contains( gatewayResponse ) )
        {
            log.info( "SMS sent" );

            return new OutboundMessageResponse( gatewayResponse.getResponseMessage(), gatewayResponse, true );
        }
        else
        {
            log.error( "SMS failed, failure cause: " + gatewayResponse.getResponseMessage() );

            return new OutboundMessageResponse( gatewayResponse.getResponseMessage(), gatewayResponse, false );
        }
    }

    private OutboundMessageResponseSummary generateSummary( List<OutboundMessageResponse> statuses, OutboundMessageBatch batch,
        SmsGateway smsGateway )
    {
        Set<GatewayResponse> okCodes = Sets.newHashSet( GatewayResponse.RESULT_CODE_0, GatewayResponse.RESULT_CODE_200,
            GatewayResponse.RESULT_CODE_202 );

        OutboundMessageResponseSummary summary = new OutboundMessageResponseSummary();

        int total, sent = 0;

        boolean ok = true;

        String errorMessage = StringUtils.EMPTY;

        total = batch.getMessages().size();

        for ( OutboundMessageResponse status : statuses )
        {
            if ( okCodes.contains( status.getResponseObject() ) )
            {
                sent = (smsGateway instanceof BulkSmsGateway) ? total : sent + 1;
            }
            else
            {
                ok = false;

                errorMessage = status.getDescription();
            }
        }

        summary.setTotal( total );
        summary.setChannel( DeliveryChannel.SMS );
        summary.setSent( sent );
        summary.setFailed( total - sent );

        if ( !ok )
        {
            summary.setBatchStatus( OutboundMessageBatchStatus.FAILED );
            summary.setErrorMessage( errorMessage );

            log.error( errorMessage );
        }
        else
        {
            summary.setBatchStatus( OutboundMessageBatchStatus.COMPLETED );
            summary.setResponseMessage( "SENT" );

            log.info( "SMS batch processed successfully" );
        }

        return summary;
    }

    private OutboundMessageResponseSummary createMessageResponseSummary( String responseMessage, DeliveryChannel channel,
        OutboundMessageBatchStatus batchStatus, OutboundMessageBatch batch )
    {
        OutboundMessageResponseSummary summary = new OutboundMessageResponseSummary( responseMessage, channel, batchStatus );
        summary.setTotal( batch.getMessages().size() );

        log.warn( responseMessage );

        return summary;
    }
}
