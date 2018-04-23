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

import org.hisp.dhis.common.*;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by zubair@dhis2.org on 11.08.17.
 */

@Transactional
public class ProgramStageDataEntrySMSListener
    extends BaseSMSListener
{
    private static final String DEFAULT_PATTERN = "([^\\s|=]+)\\s*\\=\\s*([-\\w\\s ]+)\\s*(\\=|$)*\\s*";

    private static final String SUCCESS = "Program Stage registered successfully";

    private static final String UID = "uid";

    private static final String UID_UPPER_CASE = "UID";

    private static final String NO_OU_FOUND = "No organisation unit found";

    private static final String NO_TEI_EXIST = "No tracked entity exists with given phone number";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private ProgramInstanceService programInstanceService;

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
        SMSCommand smsCommand = getCommand( sms );

        Set<OrganisationUnit> ous = getOrganisationUnits( sms );

        Map<String, String> commandValuePairs = parseMessageInput( sms, smsCommand );

        TrackedEntityInstance tei = getTrackedEntityInstance( commandValuePairs );

        if ( !hasCorrectFormat( sms, smsCommand ) || !validateInputValues( commandValuePairs, smsCommand, sms ) )
        {
            return;
        }

        if ( !validate( tei, ous, sms ) )
        {
            return;
        }

        registerProgramStage( tei, sms, smsCommand, commandValuePairs, ous );
    }

    @Override
    protected String getDefaultPattern()
    {
        return DEFAULT_PATTERN;
    }

    @Override
    protected String getSuccessMessage()
    {
        return SUCCESS;
    }

    private void registerProgramStage( TrackedEntityInstance tei, IncomingSms sms, SMSCommand smsCommand, Map<String, String> keyValue, Set<OrganisationUnit> ous )
    {
        List<ProgramInstance> programInstances = new ArrayList<>(
            programInstanceService.getProgramInstances( tei, smsCommand.getProgram(), ProgramStatus.ACTIVE ) );

        register( programInstances, keyValue, smsCommand, sms, ous );
    }

    private TrackedEntityInstance getTrackedEntityInstance( Map<String,String> commandValuePair )
    {
        String tempUid = "";

        if ( commandValuePair.containsKey( UID ) )
        {
            tempUid = commandValuePair.get( UID );
        }
        else if ( commandValuePair.containsKey( UID_UPPER_CASE ) )
        {
            tempUid = commandValuePair.get( UID_UPPER_CASE );
        }

        return trackedEntityInstanceService.getTrackedEntityInstance( tempUid );
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

    private boolean validate( TrackedEntityInstance tei, Set<OrganisationUnit> ous, IncomingSms sms )
    {
        if ( tei == null )
        {
            sendFeedback( NO_TEI_EXIST, sms.getOriginator(), ERROR );
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
}
