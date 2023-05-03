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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.SimpleEventSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component( "org.hisp.dhis.sms.listener.SimpleEventSMSListener" )
@Transactional
public class SimpleEventSMSListener extends CompressionSMSListener
{
    private final ProgramInstanceService programInstanceService;

    public SimpleEventSMSListener( IncomingSmsService incomingSmsService,
        @Qualifier( "smsMessageSender" ) MessageSender smsSender, UserService userService,
        TrackedEntityTypeService trackedEntityTypeService, TrackedEntityAttributeService trackedEntityAttributeService,
        ProgramService programService, OrganisationUnitService organisationUnitService, CategoryService categoryService,
        DataElementService dataElementService, EventService eventService,
        ProgramInstanceService programInstanceService, IdentifiableObjectManager identifiableObjectManager )
    {
        super( incomingSmsService, smsSender, userService, trackedEntityTypeService, trackedEntityAttributeService,
            programService, organisationUnitService, categoryService, dataElementService, eventService,
            identifiableObjectManager );

        this.programInstanceService = programInstanceService;
    }

    @Override
    protected SmsResponse postProcess( IncomingSms sms, SmsSubmission submission )
        throws SMSProcessingException
    {
        SimpleEventSmsSubmission subm = (SimpleEventSmsSubmission) submission;

        Uid ouid = subm.getOrgUnit();
        Uid aocid = subm.getAttributeOptionCombo();
        Uid progid = subm.getEventProgram();

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( ouid.getUid() );
        User user = userService.getUser( subm.getUserId().getUid() );

        Program program = programService.getProgram( subm.getEventProgram().getUid() );

        if ( program == null )
        {
            throw new SMSProcessingException( SmsResponse.INVALID_PROGRAM.set( progid ) );
        }

        CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo( aocid.getUid() );

        if ( aoc == null )
        {
            throw new SMSProcessingException( SmsResponse.INVALID_AOC.set( aocid ) );
        }

        if ( !programService.hasOrgUnit( program, orgUnit ) )
        {
            throw new SMSProcessingException( SmsResponse.OU_NOTIN_PROGRAM.set( ouid, progid ) );
        }

        List<Enrollment> enrollments = new ArrayList<>(
            programInstanceService.getProgramInstances( program, ProgramStatus.ACTIVE ) );

        // For Simple Events, the Program should have one Program Instance
        // If it doesn't exist, this is the first event, we can create it here
        if ( enrollments.isEmpty() )
        {
            Enrollment pi = new Enrollment();
            pi.setEnrollmentDate( new Date() );
            pi.setIncidentDate( new Date() );
            pi.setProgram( program );
            pi.setStatus( ProgramStatus.ACTIVE );

            programInstanceService.addProgramInstance( pi );

            enrollments.add( pi );
        }
        else if ( enrollments.size() > 1 )
        {
            // TODO: Are we sure this is a problem we can't recover from?
            throw new SMSProcessingException( SmsResponse.MULTI_PROGRAMS.set( progid ) );
        }

        Enrollment enrollment = enrollments.get( 0 );
        Set<ProgramStage> programStages = enrollment.getProgram().getProgramStages();
        if ( programStages.size() > 1 )
        {
            throw new SMSProcessingException( SmsResponse.MULTI_STAGES.set( progid ) );
        }
        ProgramStage programStage = programStages.iterator().next();

        List<Object> errorUIDs = saveNewEvent( subm.getEvent().getUid(), orgUnit, programStage, enrollment, sms,
            aoc, user, subm.getValues(), subm.getEventStatus(), subm.getEventDate(), subm.getDueDate(),
            subm.getCoordinates() );
        if ( !errorUIDs.isEmpty() )
        {
            return SmsResponse.WARN_DVERR.setList( errorUIDs );
        }
        else if ( subm.getValues() == null || subm.getValues().isEmpty() )
        {
            // TODO: Should we save the event if there are no data values?
            return SmsResponse.WARN_DVEMPTY;
        }

        return SmsResponse.SUCCESS;
    }

    @Override
    protected boolean handlesType( SubmissionType type )
    {
        return (type == SubmissionType.SIMPLE_EVENT);
    }

}