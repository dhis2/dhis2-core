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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.sms.SmsSender;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;

public class TrackedEntityRegistrationSMSListener
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

    private TrackedEntityService trackedEntityService;

    public void setTrackedEntityService( TrackedEntityService trackedEntityService )
    {
        this.trackedEntityService = trackedEntityService;
    }

    private TrackedEntityInstanceService trackedEntityInstanceService;

    public void setTrackedEntityInstanceService( TrackedEntityInstanceService trackedEntityInstanceService )
    {
        this.trackedEntityInstanceService = trackedEntityInstanceService;
    }

    private ProgramInstanceService programInstanceService;

    public void setProgramInstanceService( ProgramInstanceService programInstanceService )
    {
        this.programInstanceService = programInstanceService;
    }

    private SmsSender smsSender;
    
    public void setSmsSender( SmsSender smsSender )
    {
        this.smsSender = smsSender;
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

        return smsCommandService.getSMSCommand( commandString, ParserType.TRACKED_ENTITY_REGISTRATION_PARSER ) != null;
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
        
        Map<String, String> parsedMessage = this.parse( message, smsCommand );

        Date date = lookForDate( message );
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

        OrganisationUnit orgUnit = this.selectOrganisationUnit( orgUnits, parsedMessage, smsCommand );

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setOrganisationUnit( orgUnit );
        trackedEntityInstance.setTrackedEntity( trackedEntityService.getTrackedEntityByName( "Person" ) );
        Set<TrackedEntityAttributeValue> patientAttributeValues = new HashSet<>();

        for ( SMSCode code : smsCommand.getCodes() )
        {
            if ( parsedMessage.containsKey( code.getCode().toUpperCase() ) )
            {
                TrackedEntityAttributeValue trackedEntityAttributeValue = this.createTrackedEntityAttributeValue(
                    parsedMessage, code, smsCommand, trackedEntityInstance );
                patientAttributeValues.add( trackedEntityAttributeValue );
            }
        }
        

        int trackedEntityInstanceId = 0;
        if ( patientAttributeValues.size() > 0 )
        {
            trackedEntityInstanceId = trackedEntityInstanceService.createTrackedEntityInstance( trackedEntityInstance,
                null, null, patientAttributeValues );
        }

        programInstanceService.enrollTrackedEntityInstance( trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstanceId ), smsCommand.getProgram(), new Date(), date, orgUnit );
        smsSender.sendMessage( "Register new User successfully", senderPhoneNumber );
        
    }

    private TrackedEntityAttributeValue createTrackedEntityAttributeValue( Map<String, String> parsedMessage,
        SMSCode code, SMSCommand smsCommand, TrackedEntityInstance trackedEntityInstance )
    {
        String value = parsedMessage.get( code.getCode().toUpperCase() );
        TrackedEntityAttribute trackedEntityAttribute = code.getTrackedEntityAttribute();

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setEntityInstance( trackedEntityInstance );
        trackedEntityAttributeValue.setValue( value );
        return trackedEntityAttributeValue;
    }

    private OrganisationUnit selectOrganisationUnit( Collection<OrganisationUnit> orgUnits,
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
                messageListingOrgUnits += " " + o.getName() + ":" + o.getCode();
                if ( i.hasNext() )
                {
                    messageListingOrgUnits += ",";
                }
            }
            
            throw new SMSParserException( messageListingOrgUnits );
        }

        return orgUnit;
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
}
