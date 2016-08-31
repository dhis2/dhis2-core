package org.hisp.dhis.sms;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.sms.outbound.DefaultOutboundSmsTransportService;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsTransportService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class DefaultSmsSender
    implements SmsSender
{
    private static final Log log = LogFactory.getLog( SmsSender.class );

    private static int MAX_CHAR = 160;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private OutboundSmsService outboundSmsService;

    public void setOutboundSmsService( OutboundSmsService outboundSmsService )
    {
        this.outboundSmsService = outboundSmsService;
    }

    @Autowired
    private OutboundSmsTransportService transportService;

    @Transactional
    @Override
    public String sendMessage( OutboundSms sms, String gatewayId )
        throws SmsServiceException
    {
        if ( transportService == null || !transportService.isEnabled() )
        {
            throw new SmsServiceNotEnabledException();
        }

        return transportService.sendMessage( sms, gatewayId );
    }

    @Transactional
    @Override
    public String sendMessage( OutboundSms sms )
        throws SmsServiceException
    {
        if ( transportService == null || !transportService.isEnabled() )
        {
            throw new SmsServiceNotEnabledException();
        }

        return transportService.sendMessage( sms, transportService.getDefaultGateway() );
    }

    @Transactional
    @Override
    public String sendMessage( String message, String phoneNumber )
        throws SmsServiceException
    {
        if ( transportService == null || !transportService.isEnabled() )
        {
            throw new SmsServiceNotEnabledException();
        }

        message = createMessage( null, message, currentUserService.getCurrentUser() );
        OutboundSms sms = new OutboundSms( message, phoneNumber );
        return sendMessage( sms );
    }

    @Transactional
    @Override
    public String sendMessage( String subject, String text, User sender, List<User> users, boolean forceSend )
    {
        String message = null;

        if ( transportService == null || DefaultOutboundSmsTransportService.GATEWAY_MAP == null )
        {
            message = "No gateway";
            return message;
        }

        List<User> toSendList = new ArrayList<>();

        String gatewayId = transportService.getDefaultGateway();

        if ( gatewayId != null && !gatewayId.trim().isEmpty() )
        {
            if ( !forceSend )
            {
                for ( User user : users )
                {
                    if ( currentUserService.getCurrentUser() != null )
                    {
                        if ( !currentUserService.getCurrentUser().equals( user ) )
                        {
                            toSendList.add( user );
                        }
                    }
                    else if ( currentUserService.getCurrentUser() == null )
                    {
                        toSendList.add( user );
                    }
                }
            }
            else
            {
                toSendList.addAll( users );
            }

            int maxChar = MAX_CHAR;
            
            Set<String> phoneNumbers = null;

            if ( transportService != null && transportService.isEnabled() )
            {
                phoneNumbers = getRecipientsPhoneNumber( toSendList );

                text = createMessage( subject, text, sender );

                // Bulk is limited in sending long SMS, need to cut into small pieces
                if ( DefaultOutboundSmsTransportService.GATEWAY_MAP.get( "bulk_gw" ) != null
                    && DefaultOutboundSmsTransportService.GATEWAY_MAP.get( "bulk_gw" ).equals( gatewayId ) )
                {
                    // Check if text contain any specific unicode character
                    for ( char each : text.toCharArray() )
                    {
                        if ( !Character.UnicodeBlock.of( each ).equals( UnicodeBlock.BASIC_LATIN ) )
                        {
                            maxChar = 40;
                            break;
                        }
                    }
                    if ( text.length() > maxChar )
                    {
                        List<String> splitTextList = new ArrayList<>();
                        splitTextList = splitLongUnicodeString( text, splitTextList );
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
            }
        }

        return message;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String createMessage( String subject, String text, User sender )
    {
        String name = "DHIS";

        if ( sender != null )
        {
            name = sender.getUsername();
        }

        if ( subject == null || subject.isEmpty() )
        {
            subject = "";
        }
        else
        {
            subject = " - " + subject;
        }

        text = name + subject + ": " + text;

        int length = text.length(); // Simplistic cut off 160 characters

        return (length > 160) ? text.substring( 0, 157 ) + "..." : text;
    }

    private Set<String> getRecipientsPhoneNumber( List<User> users )
    {
        Set<String> recipients = new HashSet<>();

        for ( User user : users )
        {
            String phoneNumber = user.getPhoneNumber();

            if ( phoneNumber != null && !phoneNumber.trim().isEmpty() )
            {
                recipients.add( phoneNumber );
            }
        }
        return recipients;
    }

    private String sendMessage( String text, Set<String> recipients, String gateWayId )
    {
        String message = null;
        OutboundSms sms = new OutboundSms();
        sms.setMessage( text );
        sms.setRecipients( recipients );

        try
        {
            message = transportService.sendMessage( sms, gateWayId );
        }
        catch ( SmsServiceException e )
        {
            message = "Unable to send message through sms: " + sms + e.getCause().getMessage();
            log.warn( "Unable to send message through sms: " + sms, e );
        }

        return message;
    }

    public List<String> splitLongUnicodeString( String message, List<String> result )
    {
        String firstTempString = null;
        String secondTempString = null;
        int indexToCut = 0;

        firstTempString = message.substring( 0, MAX_CHAR );

        indexToCut = firstTempString.lastIndexOf( " " );

        firstTempString = firstTempString.substring( 0, indexToCut );

        result.add( firstTempString );

        secondTempString = message.substring( indexToCut + 1, message.length() );

        if ( secondTempString.length() <= MAX_CHAR )
        {
            result.add( secondTempString );
            return result;
        }
        else
        {
            return splitLongUnicodeString( secondTempString, result );
        }
    }

    public String isWastedSMS( OutboundSms sms )
    {
        List<OutboundSms> listOfRecentOutboundSms = outboundSmsService.getAllOutboundSms( 0, 10 );
        
        for ( OutboundSms each : listOfRecentOutboundSms )
        {
            if ( each.getRecipients().equals( sms.getRecipients() )
                && each.getMessage().equalsIgnoreCase( sms.getMessage() ) )
            {
                return "system is trying to send out wasted SMS";
            }
        }
        
        return null;
    }

    public CurrentUserService getCurrentUserService()
    {
        return currentUserService;
    }

    public UserService getUserService()
    {
        return userService;
    }
}
