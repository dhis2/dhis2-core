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

import java.util.*;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TrackedEntityRegistrationSMSListener
    extends BaseSMSListener
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;
    
    @Autowired
    private IncomingSmsService incomingSmsService;
    
    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    // -------------------------------------------------------------------------
    // IncomingSmsListener implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.TRACKED_ENTITY_REGISTRATION_PARSER ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        String message = sms.getText();
        SMSCommand smsCommand = smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.TRACKED_ENTITY_REGISTRATION_PARSER );

        Map<String, String> parsedMessage = parseMessageInput( sms, smsCommand );

        if ( !hasCorrectFormat( sms, smsCommand ) || !validateInputValues( parsedMessage, smsCommand, sms ) )
        {
            return;
        }

        Date date = SmsUtils.lookForDate( message );
        String senderPhoneNumber = StringUtils.replace( sms.getOriginator(), "+", "" );
        Collection<OrganisationUnit> orgUnits = getOrganisationUnits( sms );

        Program program = smsCommand.getProgram();

        OrganisationUnit orgUnit = SmsUtils.selectOrganisationUnit( orgUnits, parsedMessage, smsCommand );

        if ( !program.hasOrganisationUnit( orgUnit ) )
        {
            sendFeedback( SMSCommand.NO_OU_FOR_PROGRAM, senderPhoneNumber, WARNING );

            throw new SMSParserException( SMSCommand.NO_OU_FOR_PROGRAM );
        }

        TrackedEntityInstance trackedEntityInstance = new TrackedEntityInstance();
        trackedEntityInstance.setOrganisationUnit( orgUnit );
        trackedEntityInstance.setTrackedEntityType( trackedEntityTypeService.getTrackedEntityByName( smsCommand.getProgram().getTrackedEntityType().getName() ) );
        Set<TrackedEntityAttributeValue> patientAttributeValues = new HashSet<>();

        smsCommand.getCodes().stream()
            .filter( code -> parsedMessage.containsKey( code.getCode() ) )
            .forEach( code ->
            {
                TrackedEntityAttributeValue trackedEntityAttributeValue = this.createTrackedEntityAttributeValue( parsedMessage, code, trackedEntityInstance) ;
                patientAttributeValues.add( trackedEntityAttributeValue );
            });

        int trackedEntityInstanceId = 0;
        if ( patientAttributeValues.size() > 0 )
        {
            trackedEntityInstanceId = trackedEntityInstanceService.createTrackedEntityInstance( trackedEntityInstance,
                null, null, patientAttributeValues );
        }
        else
        {
            sendFeedback( "No TrackedEntityAttribute found", senderPhoneNumber, WARNING );
        }

        programInstanceService.enrollTrackedEntityInstance(
            trackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstanceId ), smsCommand.getProgram(),
            new Date(), date, orgUnit );

        sendFeedback( "Tracked Entity Registered Successfully", senderPhoneNumber, INFO );
        
        sms.setStatus( SmsMessageStatus.PROCESSED );
        sms.setParsed( true );
        incomingSmsService.update( sms );
    }

    private TrackedEntityAttributeValue createTrackedEntityAttributeValue( Map<String, String> parsedMessage,
        SMSCode code, TrackedEntityInstance trackedEntityInstance )
    {
        String value = parsedMessage.get( code.getCode() );
        TrackedEntityAttribute trackedEntityAttribute = code.getTrackedEntityAttribute();

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( trackedEntityAttribute );
        trackedEntityAttributeValue.setEntityInstance( trackedEntityInstance );
        trackedEntityAttributeValue.setValue( value );
        return trackedEntityAttributeValue;
    }
}
