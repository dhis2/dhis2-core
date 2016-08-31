package org.hisp.dhis.sms.listener;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;

public class ProgramStageDataEntrySMSListener 
    implements IncomingSmsListener
{    
    private static final String defaultPattern = "([a-zA-Z]+)\\s*(\\d+)";
    
    private SMSCommandService smsCommandService;

    public void setSmsCommandService( SMSCommandService smsCommandService )
    {
        this.smsCommandService = smsCommandService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }
    
    // -------------------------------------------------------------------------
    // IncomingSmsListener implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean accept( IncomingSms sms )
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

        return smsCommandService.getSMSCommand( commandString, ParserType.PROGRAM_STAGE_DATAENTRY_PARSER ) != null;
    }

    @Override
    public void receive( IncomingSms sms )
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

        SMSCommand smsCommand = smsCommandService.getSMSCommand( commandString,
            ParserType.TRACKED_ENTITY_REGISTRATION_PARSER );
        
        this.parse( message, smsCommand );

        lookForDate( message );
        
        //TODO what to do with the above?
        
        String senderPhoneNumber = StringUtils.replace( sms.getOriginator(), "+", "" );
        Collection<OrganisationUnit> orgUnits = getOrganisationUnitsByPhoneNumber( senderPhoneNumber );

        if ( orgUnits == null || orgUnits.size() == 0 )
        {
            if ( StringUtils.isEmpty( smsCommand.getNoUserMessage() ) )
            {
                throw new SMSParserException( SMSCommand.NO_USER_MESSAGE );
            }
            else
            {
                throw new SMSParserException( smsCommand.getNoUserMessage() );
            }
        }
    }
    
    private Map<String, String> parse( String message, SMSCommand smsCommand )
    {
        HashMap<String, String> output = new HashMap<>();
        Pattern pattern = Pattern.compile( defaultPattern );
        if ( !StringUtils.isBlank( smsCommand.getSeparator() ) )
        {
            String x = "(\\w+)\\s*\\" + smsCommand.getSeparator().trim() + "\\s*([\\w ]+)\\s*(\\"
                + smsCommand.getSeparator().trim() + "|$)*\\s*";
            pattern = Pattern.compile( x );
        }
        Matcher matcher = pattern.matcher( message );
        while ( matcher.find() )
        {
            String key = matcher.group( 1 );
            String value = matcher.group( 2 );

            if ( !StringUtils.isEmpty( key ) && !StringUtils.isEmpty( value ) )
            {
                output.put( key.toUpperCase(), value );
            }
        }

        return output;
    }
    
    private Date lookForDate( String message )
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
    
    private Collection<OrganisationUnit> getOrganisationUnitsByPhoneNumber( String sender )
    {
        Collection<OrganisationUnit> orgUnits = new ArrayList<>();
        Collection<User> users = userService.getUsersByPhoneNumber( sender );
        
        for ( User u : users )
        {
            if ( u.getOrganisationUnits() != null )
            {
                orgUnits.addAll( u.getOrganisationUnits() );
            }
        }

        return orgUnits;
    }
}
