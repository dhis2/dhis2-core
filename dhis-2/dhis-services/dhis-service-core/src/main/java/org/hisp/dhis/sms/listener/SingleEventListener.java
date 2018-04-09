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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zubair <rajazubair.asghar@gmail.com>
 */
@Transactional
public class SingleEventListener
    extends BaseSMSListener
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private ProgramInstanceService programInstanceService;

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

        Set<OrganisationUnit> ous = getOrganisationUnits( sms );

        Map<String, String> commandValuePairs = parseMessageInput( sms, smsCommand );

        if ( !hasCorrectFormat( sms, smsCommand ) || !validateInputValues( commandValuePairs, smsCommand, sms ) )
        {
            return;
        }

        registerEvent( commandValuePairs, smsCommand, sms, ous );
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    private void registerEvent( Map<String, String> commandValuePairs, SMSCommand smsCommand, IncomingSms sms, Set<OrganisationUnit> ous )
    {
        List<ProgramInstance> programInstances = new ArrayList<>(
            programInstanceService.getProgramInstances( smsCommand.getProgram(), ProgramStatus.ACTIVE ) );

        register( programInstances, commandValuePairs, smsCommand, sms, ous );
    }
}
