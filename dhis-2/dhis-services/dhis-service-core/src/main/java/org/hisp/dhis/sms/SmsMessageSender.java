package org.hisp.dhis.sms;

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

import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsTransportService;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

/**
 * @author Nguyen Kim Lai
 */
public class SmsMessageSender
    implements MessageSender
{
    private static final Log log = LogFactory.getLog( SmsMessageSender.class );

    private static int MAX_CHAR = 160;

    private static final String GW_BULK = "bulk_gw";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private OutboundSmsTransportService outboundSmsTransportService;

    @Override
    public String sendMessage( String subject, String text, String footer, User sender, Set<User> users,
        boolean forceSend )
    {
        String message = null;

        if ( outboundSmsTransportService == null )
        {
            return "No gateway";
        }

        Map<String, String> gatewayMap = outboundSmsTransportService.getGatewayMap();

        String gatewayId = StringUtils.trimToNull( outboundSmsTransportService.getDefaultGateway() );

        boolean gatewayEnabled = outboundSmsTransportService.isEnabled();

        if ( gatewayMap == null || gatewayId == null || !gatewayEnabled )
        {
            return "No gateway";
        }

        Set<User> toSendList = new HashSet<>();

        User currentUser = currentUserService.getCurrentUser();

        if ( !forceSend )
        {
            for ( User user : users )
            {
                if ( currentUser == null || (currentUser != null && !currentUser.equals( user )) )
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

        Set<String> phoneNumbers = null;

        phoneNumbers = SmsUtils.getRecipientsPhoneNumber( toSendList );

        text = SmsUtils.createMessage( subject, text, sender );

        // Bulk is limited in sending long SMS, need to split in pieces

        if ( gatewayId.equals( gatewayMap.get( GW_BULK ) ) )
        {
            // Check if text contain any specific character

            for ( char each : text.toCharArray() )
            {
                if ( !Character.UnicodeBlock.of( each ).equals( UnicodeBlock.BASIC_LATIN ) )
                {
                    MAX_CHAR = 40;
                    break;
                }
            }
            if ( text.length() > MAX_CHAR )
            {
                List<String> splitTextList = new ArrayList<>();
                splitTextList = SmsUtils.splitLongUnicodeString( text, splitTextList );

                for ( String each : splitTextList )
                {
                    if ( !phoneNumbers.isEmpty() && phoneNumbers.size() > 0 )
                    {
                        message = sendMessage( each, phoneNumbers, gatewayId );
                    }
                }
            }
            else
            {
                if ( !phoneNumbers.isEmpty() && phoneNumbers.size() > 0 )
                {
                    message = sendMessage( text, phoneNumbers, gatewayId );
                }
            }
        }
        else
        {
            if ( !phoneNumbers.isEmpty() && phoneNumbers.size() > 0 )
            {
                message = sendMessage( text, phoneNumbers, gatewayId );
            }
        }

        return message;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private boolean isQualifiedReceiver( User user )
    {
        if ( user.getFirstName() == null ) // Receiver is raw number
        {
            return true;
        }
        else
        // Receiver is user
        {
            Serializable userSetting = userSettingService
                .getUserSetting( UserSettingKey.MESSAGE_SMS_NOTIFICATION, user );

            return userSetting != null ? (Boolean) userSetting : false;
        }
    }

    private String sendMessage( String text, Set<String> recipients, String gateWayId )
    {
        String message = null;

        OutboundSms sms = new OutboundSms();
        sms.setMessage( text );
        sms.setRecipients( recipients );

        try
        {
            message = outboundSmsTransportService.sendMessage( sms, gateWayId );
        }
        catch ( SmsServiceException e )
        {
            message = "Unable to send message through SMS: " + sms + e.getCause().getMessage();

            log.warn( "Unable to send message through SMS: " + sms, e );
        }

        return message;
    }
}
