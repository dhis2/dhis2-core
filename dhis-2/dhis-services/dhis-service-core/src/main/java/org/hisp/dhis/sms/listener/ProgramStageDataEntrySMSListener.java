/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by zubair@dhis2.org on 11.08.17.
 */
@Component( "org.hisp.dhis.sms.listener.ProgramStageDataEntrySMSListener" )
@Transactional
public class ProgramStageDataEntrySMSListener extends CommandSMSListener
{
    private static final String MORE_THAN_ONE_TEI = "More than one tracked entity found for given phone number";

    private static final String NO_OU_FOUND = "No organisation unit found";

    private static final String NO_TEI_EXIST = "No tracked entity exists with given phone number";

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final SMSCommandService smsCommandService;

    private final ProgramInstanceService programInstanceService;

    public ProgramStageDataEntrySMSListener( ProgramInstanceService programInstanceService,
        CategoryService dataElementCategoryService, EventService eventService,
        UserService userService, CurrentUserService currentUserService, IncomingSmsService incomingSmsService,
        @Qualifier( "smsMessageSender" ) MessageSender smsSender,
        TrackedEntityInstanceService trackedEntityInstanceService,
        TrackedEntityAttributeService trackedEntityAttributeService, SMSCommandService smsCommandService,
        ProgramInstanceService programInstanceService1 )
    {
        super( programInstanceService, dataElementCategoryService, eventService, userService,
            currentUserService, incomingSmsService, smsSender );

        checkNotNull( trackedEntityAttributeService );
        checkNotNull( trackedEntityInstanceService );
        checkNotNull( smsCommandService );
        checkNotNull( programInstanceService );

        this.trackedEntityInstanceService = trackedEntityInstanceService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.smsCommandService = smsCommandService;
        this.programInstanceService = programInstanceService1;
    }

    // -------------------------------------------------------------------------
    // IncomingSmsListener implementation
    // -------------------------------------------------------------------------

    @Override
    public void postProcess( IncomingSms sms, SMSCommand smsCommand, Map<String, String> parsedMessage )
    {
        Set<OrganisationUnit> ous = getOrganisationUnits( sms );

        List<TrackedEntityInstance> teis = getTrackedEntityInstanceByPhoneNumber( sms, smsCommand, ous );

        if ( !validate( teis, ous, sms ) )
        {
            return;
        }

        registerProgramStage( teis.iterator().next(), sms, smsCommand, parsedMessage, ous );
    }

    @Override
    protected SMSCommand getSMSCommand( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.PROGRAM_STAGE_DATAENTRY_PARSER );
    }

    private void registerProgramStage( TrackedEntityInstance tei, IncomingSms sms, SMSCommand smsCommand,
        Map<String, String> keyValue, Set<OrganisationUnit> ous )
    {
        List<Enrollment> enrollments = new ArrayList<>(
            programInstanceService.getProgramInstances( tei, smsCommand.getProgram(), ProgramStatus.ACTIVE ) );

        register( enrollments, keyValue, smsCommand, sms, ous );
    }

    private List<TrackedEntityInstance> getTrackedEntityInstanceByPhoneNumber( IncomingSms sms, SMSCommand command,
        Set<OrganisationUnit> ous )
    {
        List<TrackedEntityAttribute> attributes = trackedEntityAttributeService.getAllTrackedEntityAttributes().stream()
            .filter( attr -> attr.getValueType().equals( ValueType.PHONE_NUMBER ) ).collect( Collectors.toList() );

        List<TrackedEntityInstance> teis = new ArrayList<>();

        attributes.parallelStream().map( attr -> getParams( attr, sms, command.getProgram(), ous ) )
            .forEach(
                param -> teis.addAll( trackedEntityInstanceService.getTrackedEntityInstances( param, false, true ) ) );

        return teis;
    }

    private boolean hasMoreThanOneEntity( List<TrackedEntityInstance> trackedEntityInstances )
    {
        return trackedEntityInstances.size() > 1;
    }

    private TrackedEntityInstanceQueryParams getParams( TrackedEntityAttribute attribute, IncomingSms sms,
        Program program, Set<OrganisationUnit> ous )
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
}
