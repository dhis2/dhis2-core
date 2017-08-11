package org.hisp.dhis.sms.listener;

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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

public class ProgramStageDataEntrySMSListener
    implements IncomingSmsListener
{
    private static final Log log = LogFactory.getLog( ProgramStageDataEntrySMSListener.class );

    private static final String DEFAULT_PATTERN = "(\\w+)\\s*((\\w+\\s*)=(\\s*\\w+\\s*),\\s*)*((\\w+\\s*)=(\\s*\\w+))";

    private static final String PS_REGISTERED = "Program Stage registered successfully";

    private static final String MORE_THAN_ONE_TEI = "More than one tracked entity found for given phone number";

    private static final String NO_OU_FOUND = "No organisation unit found";

    private static final String NO_TEI_EXIST = "No tracked entity exists with given phone number";

    private static final String MORE_THAN_ONE_ACTIVE_PROGRAM = "Multiple active program instances exist for program: %s";

    private static final int INFO = 1;

    private static final int ERROR = 3;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

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
    // IncomingSmsListener implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        return getCommand( sms ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        SMSCommand command = getCommand( sms );

        Set<OrganisationUnit> ous = getOrganisationUnits( sms );

        List<TrackedEntityInstance> teis = getTrackedEntityInstanceByPhoneNumber( sms, command, ous );

        if ( !hasCorrectFormat( sms.getText(), command ) )
        {
            sendFeedback( StringUtils.defaultIfEmpty( command.getWrongFormatMessage(), SMSCommand.WRONG_FORMAT_MESSAGE ),
                    sms.getOriginator(), ERROR );

            return;
        }

        if ( !validate( teis, ous, sms ) )
        {
            return;
        }

        Map<String, String> commandValuePairs = parseMessageInput( sms, command );

        registerProgramStage( teis.iterator().next(), sms, command, commandValuePairs, ous );
    }

    private Map<String, String> parseMessageInput( IncomingSms sms, SMSCommand smsCommand )
    {
        HashMap<String, String> output = new HashMap<>();

        String message = sms.getText().substring( SmsUtils.getCommandString( sms ).length() ).trim();

        String[] messageParts = org.apache.commons.lang.StringUtils.split( message, "," );

        for ( String string : messageParts )
        {
            String key = org.apache.commons.lang.StringUtils.split( string,
                    smsCommand.getCodeSeparator() != null ? smsCommand.getCodeSeparator() : "=" )[0].trim();

            String value = org.apache.commons.lang.StringUtils.split( string,
                    smsCommand.getCodeSeparator() != null ? smsCommand.getCodeSeparator() : "=" )[1].trim();

            output.put( key, value );
        }

        return output;
    }

    private void registerProgramStage( TrackedEntityInstance tei, IncomingSms sms, SMSCommand smsCommand, Map<String, String> keyValue, Set<OrganisationUnit> ous )
    {
        List<ProgramInstance> programInstances = new ArrayList<>(
                programInstanceService.getProgramInstances( tei, smsCommand.getProgram(), ProgramStatus.ACTIVE ) );

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

            sendFeedback( String.format( MORE_THAN_ONE_ACTIVE_PROGRAM, smsCommand.getProgram().getName() ),
                sms.getOriginator(), ERROR );

            return;
        }

        ProgramInstance programInstance = programInstances.get( 0 );

        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setOrganisationUnit( ous.iterator().next() );
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
            dataValue.setValue( keyValue.get( smsCode.getCode() ) );

            trackedEntityDataValueService.saveTrackedEntityDataValue( dataValue );
        }

        update( sms, SmsMessageStatus.PROCESSED, true );

        sendFeedback( StringUtils.defaultIfEmpty( smsCommand.getSuccessMessage(), PS_REGISTERED ),
                sms.getOriginator(), INFO );
    }

    private List<TrackedEntityInstance> getTrackedEntityInstanceByPhoneNumber( IncomingSms sms, SMSCommand command, Set<OrganisationUnit> ous )
    {
        List<TrackedEntityAttribute> attributes = trackedEntityAttributeService.getAllTrackedEntityAttributes().stream()
            .filter( attr -> attr.getValueType().equals( ValueType.PHONE_NUMBER ) )
            .collect( Collectors.toList() );

        List<TrackedEntityInstance> teis = new ArrayList<>();

        attributes.parallelStream()
            .map( attr -> getParams( attr, sms, command.getProgram(), ous ) )
            .forEach( param -> teis.addAll( trackedEntityInstanceService.getTrackedEntityInstances( param ) ) );

        return teis;
    }

    private boolean hasMoreThanOneEntity( List<TrackedEntityInstance> trackedEntityInstances )
    {
        return  trackedEntityInstances.size() > 1 ;
    }

    private TrackedEntityInstanceQueryParams getParams( TrackedEntityAttribute attribute, IncomingSms sms, Program program, Set<OrganisationUnit> ous )
    {
        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();

        QueryFilter queryFilter = new QueryFilter();
        queryFilter.setOperator( QueryOperator.LIKE );
        queryFilter.setFilter( sms.getOriginator() );

        QueryItem item = new QueryItem( attribute );
        item.getFilters().add( queryFilter );
        item.setValueType( ValueType.PHONE_NUMBER );

        params.setProgram( program );
        params.setOrganisationUnits( ous );
        params.getFilters().add( item );

        return params;
    }

    private SMSCommand getCommand( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
                ParserType.PROGRAM_STAGE_DATAENTRY_PARSER );
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

    private Set<OrganisationUnit> getOrganisationUnits( IncomingSms sms )
    {
        return SmsUtils.getOrganisationUnitsByPhoneNumber( sms.getOriginator(),
                userService.getUsersByPhoneNumber( sms.getOriginator() ) );
    }

    private boolean validate( List<TrackedEntityInstance> teis, Set<OrganisationUnit> ous, IncomingSms sms )
    {
        if ( teis == null || teis.isEmpty() )
        {
            sendFeedback( NO_TEI_EXIST, sms.getOriginator(), ERROR );
            return false;
        }

        if ( hasMoreThanOneEntity( teis ) )
        {
            sendFeedback( MORE_THAN_ONE_TEI, sms.getOriginator(), ERROR );
            return false;
        }

        if ( validateOrganisationUnits( ous ) )
        {
            sendFeedback( NO_OU_FOUND, sms.getOriginator(), ERROR );
            return false;
        }

        return true;
    }

    private boolean validateOrganisationUnits( Set<OrganisationUnit> ous )
    {
        return ous == null || ous.isEmpty();
    }

    private void update( IncomingSms sms, SmsMessageStatus status, boolean parsed )
    {
        sms.setStatus( status );
        sms.setParsed( parsed );

        incomingSmsService.update( sms );
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
}
