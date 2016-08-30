package org.hisp.dhis.system.util;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;

import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.user.User;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.incoming.IncomingSms;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public class SmsUtils
{
    private static int MAX_CHAR = 160;

    public static String getCommandString( IncomingSms sms )
    {
        String message = sms.getText();
        String commandString = null;

        for ( int i = 0; i < message.length(); i++ )
        {
            String c = String.valueOf( message.charAt( i ) );

            if ( c.matches( "\\W" ) )
            {
                commandString = message.substring( 0, i );
                message = message.substring( commandString.length() + 1 );
                break;
            }
        }

        return commandString.trim();
    }

    public static Collection<OrganisationUnit> getOrganisationUnitsByPhoneNumber( String sender,
        Collection<User> users )
    {
        Collection<OrganisationUnit> orgUnits = new ArrayList<>();
        for ( User u : users )
        {
            if ( u.getOrganisationUnits() != null )
            {
                orgUnits.addAll( u.getOrganisationUnits() );
            }
        }

        return orgUnits;
    }

    public static ProgramMessage createProgramMessage( String subject, String text, String footer, User sender,
        Set<User> users, boolean forceSend, Set<DeliveryChannel> channels )
    {
        ProgramMessageRecipients recipients = new ProgramMessageRecipients();
        recipients.setPhoneNumbers( SmsUtils.getRecipientsPhoneNumber( users ) );

        ProgramMessage message = new ProgramMessage();

        message.setText( text );
        message.setSubject( subject );
        message.setDeliveryChannels( channels );
        message.setRecipients( recipients );

        return message;
    }

    public static Date lookForDate( String message )
    {
        if ( !message.contains( " " ) )
        {
            return null;
        }

        Date date = null;
        String dateString = message.trim().split( " " )[0];
        SimpleDateFormat format = new SimpleDateFormat( "ddMM" );

        try
        {
            Calendar cal = Calendar.getInstance();
            date = format.parse( dateString );
            cal.setTime( date );
            int year = Calendar.getInstance().get( Calendar.YEAR );
            int month = Calendar.getInstance().get( Calendar.MONTH );

            if ( cal.get( Calendar.MONTH ) < month )
            {
                cal.set( Calendar.YEAR, year );
            }
            else
            {
                cal.set( Calendar.YEAR, year - 1 );
            }

            date = cal.getTime();
        }
        catch ( Exception e )
        {
            // no date found
        }

        return date;
    }

    public static User getUser( String sender, SMSCommand smsCommand, List<User> userList )
    {
        OrganisationUnit orgunit = null;
        User user = null;

        for ( User u : userList )
        {
            OrganisationUnit ou = u.getOrganisationUnit();

            if ( ou != null )
            {
                if ( orgunit == null )
                {
                    orgunit = ou;
                }
                else if ( orgunit.getId() == ou.getId() )
                {
                }
                else
                {
                    if ( StringUtils.isEmpty( smsCommand.getMoreThanOneOrgUnitMessage() ) )
                    {
                        throw new SMSParserException( SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE );
                    }
                    else
                    {
                        throw new SMSParserException( smsCommand.getMoreThanOneOrgUnitMessage() );
                    }
                }
            }

            user = u;
        }

        if ( user == null )
        {
            throw new SMSParserException( "User is not associated with any orgunit. Please contact your supervisor." );
        }

        return user;
    }

    public static List<String> splitLongUnicodeString( String message, List<String> result )
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

    public static ProgramMessage createProgramMessage( String text, String recipient, String subject,
        Set<DeliveryChannel> channels, boolean storeCopy )
    {
        ProgramMessage programMessage = new ProgramMessage();
        programMessage.setText( text );
        programMessage.setSubject( subject );
        programMessage.setDeliveryChannels( channels );
        programMessage.setRecipients( getRecipients( recipient, channels ) );
        programMessage.setStoreCopy( storeCopy );

        return programMessage;
    }

    public static ProgramMessageRecipients getRecipients( Set<User> users, Set<DeliveryChannel> channels )
    {
        ProgramMessageRecipients to = new ProgramMessageRecipients();

        if ( channels.contains( DeliveryChannel.SMS ) )
        {
            to.setPhoneNumbers( getRecipientsPhoneNumber( users ) );
        }

        if ( channels.contains( DeliveryChannel.EMAIL ) )
        {
            to.setEmailAddresses( getRecipientsEmail( users ) );
        }

        return to;
    }

    public static String createMessage( String subject, String text, User sender )
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

    public static Set<String> getRecipientsPhoneNumber( Collection<User> users )
    {
        Set<String> recipients = new HashSet<>();

        for ( User user : users )
        {
            String phoneNumber = user.getPhoneNumber();

            if ( StringUtils.trimToNull( phoneNumber ) != null )
            {
                recipients.add( phoneNumber );
            }
        }
        return recipients;
    }

    public static Set<String> getRecipientsEmail( Collection<User> users )
    {
        Set<String> recipients = new HashSet<>();

        for ( User user : users )
        {
            String email = user.getEmail();

            if ( StringUtils.trimToNull( email ) != null )
            {
                recipients.add( email );
            }
        }
        return recipients;
    }

    public static OrganisationUnit selectOrganisationUnit( Collection<OrganisationUnit> orgUnits,
        Map<String, String> parsedMessage, SMSCommand smsCommand )
    {
        OrganisationUnit orgUnit = null;

        for ( OrganisationUnit o : orgUnits )
        {
            if ( orgUnits.size() == 1 )
            {
                orgUnit = o;
            }
            if ( parsedMessage.containsKey( "ORG" ) && o.getCode().equals( parsedMessage.get( "ORG" ) ) )
            {
                orgUnit = o;
                break;
            }
        }

        if ( orgUnit == null && orgUnits.size() > 1 )
        {
            String messageListingOrgUnits = smsCommand.getMoreThanOneOrgUnitMessage();

            for ( Iterator<OrganisationUnit> i = orgUnits.iterator(); i.hasNext(); )
            {
                OrganisationUnit o = i.next();
                messageListingOrgUnits += TextUtils.SPACE + o.getName() + ":" + o.getCode();

                if ( i.hasNext() )
                {
                    messageListingOrgUnits += ",";
                }
            }

            throw new SMSParserException( messageListingOrgUnits );
        }

        return orgUnit;
    }

    public static ProgramMessageRecipients getRecipients( String recipient, Set<DeliveryChannel> channels )
    {
        ProgramMessageRecipients to = new ProgramMessageRecipients();

        Set<String> recipientsList = new HashSet<>();
        recipientsList.add( recipient );

        if ( channels.contains( DeliveryChannel.SMS ) )
        {
            to.setPhoneNumbers( recipientsList );
        }

        if ( channels.contains( DeliveryChannel.EMAIL ) )
        {
            to.setEmailAddresses( recipientsList );
        }

        return to;
    }
}
