package org.hisp.dhis.sms.listener;

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

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */
public class SingleEventListener
    implements IncomingSmsListener
{
    private static final Log log = LogFactory.getLog( SingleEventListener.class );

    private static final String DEFAULT_PATTERN = "(\\w+)\\s*((\\w+\\s*)=(\\s*\\w+\\s*),\\s*)*((\\w+\\s*)=(\\s*\\w+))";
    
    private static final String EVENT_REGISTERED = "Event registered successfully";

    private static final int INFO = 1;

    private static final int ERROR = 3;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityDataValueService trackedEntityDataValueService;

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private IncomingSmsService incomingSmsService;
    
    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.EVENT_REGISTRATION_PARSER ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        SMSCommand smsCommand = smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.EVENT_REGISTRATION_PARSER );

        if ( !hasCorrectFormat( sms.getText(), smsCommand ) )
        {
            sendFeedback(
                StringUtils.defaultIfEmpty( smsCommand.getWrongFormatMessage(), SMSCommand.WRONG_FORMAT_MESSAGE ),
                sms.getOriginator(), ERROR );

            return;
        }

        Map<String, String> commandValuePairs = parseMessageInput( sms, smsCommand );

        if ( !validate( commandValuePairs, smsCommand, sms ) )
        {
            return;
        }

        registerEvent( commandValuePairs, smsCommand, sms );
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    private void registerEvent( Map<String, String> commandValuePairs, SMSCommand smsCommand, IncomingSms sms )
    {
        OrganisationUnit orgUnit = getOrganisationUnits( sms ).iterator().next();

        List<ProgramInstance> programInstances = new ArrayList<>(
            programInstanceService.getProgramInstances( smsCommand.getProgram(), ProgramStatus.ACTIVE ) );

        if ( programInstances.isEmpty() )
        {
            ProgramInstance pi = new ProgramInstance();
            pi.setEnrollmentDate( new Date() );
            pi.setIncidentDate( new Date() );
            pi.setProgram( smsCommand.getProgram() );
            pi.setStatus( ProgramStatus.ACTIVE );

            programInstanceService.addProgramInstance( pi );

            programInstances.add( pi );
        }
        else if ( programInstances.size() > 1 )
        {
            update( sms, SmsMessageStatus.FAILED, false );

            sendFeedback( "Multiple active program instances exists for program: " + smsCommand.getProgram().getUid(),
                sms.getOriginator(), ERROR );

            return;
        }

        ProgramInstance programInstance = null;

        programInstance = programInstances.get( 0 );

        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setOrganisationUnit( orgUnit );
        programStageInstance.setProgramStage( smsCommand.getProgramStage() );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setExecutionDate( sms.getSentDate() );
        programStageInstance.setDueDate( sms.getSentDate() );
        programStageInstance
            .setAttributeOptionCombo( dataElementCategoryService.getDefaultDataElementCategoryOptionCombo() );
        programStageInstance.setCompletedBy( "DHIS 2" );

        programStageInstanceService.addProgramStageInstance( programStageInstance );

        for ( SMSCode smsCode : smsCommand.getCodes() )
        {
            TrackedEntityDataValue dataValue = new TrackedEntityDataValue();
            dataValue.setAutoFields();
            dataValue.setDataElement( smsCode.getDataElement() );
            dataValue.setProgramStageInstance( programStageInstance );
            dataValue.setValue( commandValuePairs.get( smsCode.getCode() ) );

            trackedEntityDataValueService.saveTrackedEntityDataValue( dataValue );
        }

        update( sms, SmsMessageStatus.PROCESSED, true );

        sendFeedback( StringUtils.defaultIfEmpty( smsCommand.getSuccessMessage(), EVENT_REGISTERED ),
            sms.getOriginator(), INFO );
    }

    private Map<String, String> parseMessageInput( IncomingSms sms, SMSCommand smsCommand )
    {
        HashMap<String, String> output = new HashMap<>();

        String message = sms.getText().substring( SmsUtils.getCommandString( sms ).length() ).trim();

        String[] messageParts = StringUtils.split( message, "," );

        for ( String string : messageParts )
        {
            String key = StringUtils.split( string,
                smsCommand.getCodeSeparator() != null ? smsCommand.getCodeSeparator() : "=" )[0].trim();
            
            String value = StringUtils.split( string,
                smsCommand.getCodeSeparator() != null ? smsCommand.getCodeSeparator() : "=" )[1].trim();
            
            output.put( key, value );
        }

        return output;
    }

    private boolean validate( Map<String, String> commandValuePairs, SMSCommand smsCommand, IncomingSms sms )
    {
        if ( !hasMandatoryParameters( commandValuePairs.keySet(), smsCommand.getCodes() ) )
        {
            sendFeedback( StringUtils.defaultIfEmpty( smsCommand.getDefaultMessage(), SMSCommand.PARAMETER_MISSING ),
                sms.getOriginator(), ERROR );

            return false;
        }

        if ( !hasOrganisationUnit( sms, smsCommand ) )
        {
            sendFeedback( StringUtils.defaultIfEmpty( smsCommand.getNoUserMessage(), SMSCommand.NO_USER_MESSAGE ),
                sms.getOriginator(), 3 );

            return false;
        }

        if ( hasMultipleOrganisationUnits( sms, smsCommand ) )
        {
            sendFeedback( StringUtils.defaultIfEmpty( smsCommand.getMoreThanOneOrgUnitMessage(),
                SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE ), sms.getOriginator(), ERROR );

            return false;
        }

        return true;
    }

    private boolean hasMandatoryParameters( Set<String> keySet, Set<SMSCode> smsCodes )
    {
        for ( SMSCode smsCode : smsCodes )
        {
            if ( smsCode.isCompulsory() && !keySet.contains( smsCode.getCode() ) )
            {
                return false;
            }
        }

        return true;
    }

    private boolean hasOrganisationUnit( IncomingSms sms, SMSCommand smsCommand )
    {
        Collection<OrganisationUnit> orgUnits = getOrganisationUnits( sms );

        if ( orgUnits == null || orgUnits.isEmpty() )
        {
            return false;
        }

        return true;
    }

    private boolean hasMultipleOrganisationUnits( IncomingSms sms, SMSCommand smsCommand )
    {
        if ( getOrganisationUnits( sms ).size() > 1 )
        {
            return true;
        }

        return false;
    }

    private boolean hasCorrectFormat( String message, SMSCommand smsCommand )
    {
        String regexp = DEFAULT_PATTERN;

        if ( smsCommand.getSeparator() != null && !smsCommand.getSeparator().trim().isEmpty() )
        {
            regexp = DEFAULT_PATTERN.replaceAll( "=", smsCommand.getSeparator() );
        }

        Pattern pattern = Pattern.compile( regexp );

        Matcher matcher = pattern.matcher( message );

        return matcher.matches();
    }

    private void update( IncomingSms sms, SmsMessageStatus status, boolean parsed )
    {
        sms.setStatus( status );
        sms.setParsed( parsed );

        incomingSmsService.update( sms );
    }

    private void sendFeedback( String message, String sender, int logType )
    {
        if ( logType == 1 )
        {
            log.info( message );
        }
        if ( logType == 2 )
        {
            log.warn( message );
        }
        if ( logType == 3 )
        {
            log.error( message );
        }

        smsSender.sendMessage( null, message, sender );
    }

    private Collection<OrganisationUnit> getOrganisationUnits( IncomingSms sms )
    {
        return SmsUtils.getOrganisationUnitsByPhoneNumber( sms.getOriginator(),
            userService.getUsersByPhoneNumber( sms.getOriginator() ) );
    }
}
