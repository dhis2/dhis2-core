package org.hisp.dhis.sms.listener;

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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zubair@dhis2.org on 11.08.17.
 */
@Transactional
public abstract class BaseSMSListener implements IncomingSmsListener
{
    private static final Log log = LogFactory.getLog( BaseSMSListener.class );

    private static final String DEFAULT_PATTERN = "([^\\s|=]+)\\s*\\=\\s*([-\\w\\s ]+)\\s*(\\=|$)*\\s*";
    private static final String NO_SMS_CONFIG = "No sms configuration found";

    protected static final int INFO = 1;

    protected static final int WARNING = 2;

    protected static final int ERROR = 3;

    private static final ImmutableMap<Integer, Consumer<String>> LOGGER = new ImmutableMap.Builder<Integer, Consumer<String>>()
        .put( 1, log::info )
        .put( 2, log::warn )
        .put( 3, log::error )
        .build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    @Autowired
    private TrackedEntityDataValueService trackedEntityDataValueService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private UserService userService;

    @Autowired
    private IncomingSmsService incomingSmsService;

    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    protected void sendFeedback( String message, String sender, int logType )
    {
        LOGGER.getOrDefault( logType, log::info ).accept( message );

        if( smsSender.isConfigured() )
        {
            smsSender.sendMessage( null, message, sender );
            return;
        }

        LOGGER.getOrDefault( WARNING, log::info ).accept(  NO_SMS_CONFIG );
    }

    protected boolean hasCorrectFormat( IncomingSms sms, SMSCommand smsCommand )
    {
        String regexp = "";

        if ( smsCommand.getSeparator() != null && !smsCommand.getSeparator().trim().isEmpty() )
        {
            regexp = DEFAULT_PATTERN.replaceAll( "=", smsCommand.getSeparator() );
        }

        Pattern pattern = Pattern.compile( regexp );

        Matcher matcher = pattern.matcher( sms.getText() );

        if ( !matcher.find() )
        {
            sendFeedback(
                StringUtils.defaultIfEmpty( smsCommand.getWrongFormatMessage(), SMSCommand.WRONG_FORMAT_MESSAGE ),
                sms.getOriginator(), ERROR );
            return false;
        }

        return true;
    }

    protected Set<OrganisationUnit> getOrganisationUnits( IncomingSms sms )
    {
        return SmsUtils.getOrganisationUnitsByPhoneNumber( sms.getOriginator(),
            Collections.singleton( getUser( sms ) ) );
    }

    protected User getUser( IncomingSms sms )
    {
        return userService.getUser( sms.getUser().getUid() );
    }

    protected void update( IncomingSms sms, SmsMessageStatus status, boolean parsed )
    {
        sms.setStatus( status );
        sms.setParsed( parsed );

        incomingSmsService.update( sms );
    }

    protected boolean validateInputValues( Map<String, String> commandValuePairs, SMSCommand smsCommand, IncomingSms sms )
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

    protected void register( List<ProgramInstance> programInstances , Map<String, String> commandValuePairs, SMSCommand smsCommand, IncomingSms sms, Set<OrganisationUnit> ous )
    {
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
            dataValue.setValue( commandValuePairs.get( smsCode.getCode() ) );

            trackedEntityDataValueService.saveTrackedEntityDataValue( dataValue );
        }

        update( sms, SmsMessageStatus.PROCESSED, true );

        sendFeedback( StringUtils.defaultIfEmpty( smsCommand.getSuccessMessage(), SMSCommand.SUCCESS_MESSAGE ),
                sms.getOriginator(), INFO );
    }

    protected  Map<String, String> parseMessageInput( IncomingSms sms, SMSCommand smsCommand )
    {
        HashMap<String, String> output = new HashMap<>();

        Pattern pattern = Pattern.compile( DEFAULT_PATTERN );

        if ( !StringUtils.isBlank( smsCommand.getSeparator() ) )
        {
            String regex = DEFAULT_PATTERN.replaceAll( "=", smsCommand.getSeparator() );

            pattern = Pattern.compile( regex );
        }

        Matcher matcher = pattern.matcher( sms.getText() );
        while ( matcher.find() )
        {
            String key = matcher.group( 1 ).trim();
            String value = matcher.group( 2 ).trim();

            if ( !StringUtils.isEmpty( key ) && !StringUtils.isEmpty( value ) )
            {
                output.put( key, value );
            }
        }

        return output;
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

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
}
