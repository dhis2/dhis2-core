package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.SMSResponse;
import org.hisp.dhis.smscompression.models.EnrollmentSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSAttributeValue;
import org.hisp.dhis.smscompression.models.SMSEvent;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.UID;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component( "org.hisp.dhis.sms.listener.EnrollmentSMSListener" )
@Transactional
public class EnrollmentSMSListener
    extends
    CompressionSMSListener
{
    private final TrackedEntityInstanceService teiService;

    private final ProgramInstanceService programInstanceService;

    private final ProgramStageService programStageService;

    private final UserService userService;

    public EnrollmentSMSListener( IncomingSmsService incomingSmsService,
        @Qualifier( "smsMessageSender" ) MessageSender smsSender, UserService userService,
        TrackedEntityTypeService trackedEntityTypeService, TrackedEntityAttributeService trackedEntityAttributeService,
        ProgramService programService, OrganisationUnitService organisationUnitService, CategoryService categoryService,
        DataElementService dataElementService, ProgramStageService programStageService,
        ProgramStageInstanceService programStageInstanceService, TrackedEntityInstanceService teiService,
        ProgramInstanceService programInstanceService, IdentifiableObjectManager identifiableObjectManager )
    {
        super( incomingSmsService, smsSender, userService, trackedEntityTypeService, trackedEntityAttributeService,
            programService, organisationUnitService, categoryService, dataElementService, programStageInstanceService,
            identifiableObjectManager );

        this.teiService = teiService;
        this.programStageService = programStageService;
        this.programInstanceService = programInstanceService;
        this.userService = userService;
    }

    @Override
    protected SMSResponse postProcess( IncomingSms sms, SMSSubmission submission )
        throws SMSProcessingException
    {
        EnrollmentSMSSubmission subm = (EnrollmentSMSSubmission) submission;

        Date enrollmentDate = subm.getEnrollmentDate();
        Date incidentDate = subm.getIncidentDate();
        UID teiUID = subm.getTrackedEntityInstance();
        UID progid = subm.getTrackerProgram();
        UID tetid = subm.getTrackedEntityType();
        UID ouid = subm.getOrgUnit();
        UID enrollmentid = subm.getEnrollment();
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( ouid.uid );

        Program program = programService.getProgram( progid.uid );
        if ( program == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_PROGRAM.set( progid ) );
        }

        TrackedEntityType entityType = trackedEntityTypeService.getTrackedEntityType( tetid.uid );
        if ( entityType == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_TETYPE.set( tetid ) );
        }

        if ( !program.hasOrganisationUnit( orgUnit ) )
        {
            throw new SMSProcessingException( SMSResponse.OU_NOTIN_PROGRAM.set( ouid, progid ) );
        }

        TrackedEntityInstance entityInstance;
        boolean teiExists = teiService.trackedEntityInstanceExists( teiUID.uid );

        if ( teiExists )
        {
            log.info( String.format( "Given TEI [%s] exists. Updating...", teiUID ) );
            entityInstance = teiService.getTrackedEntityInstance( teiUID.uid );
        }
        else
        {
            log.info( String.format( "Given TEI [%s] does not exist. Creating...", teiUID ) );
            entityInstance = new TrackedEntityInstance();
            entityInstance.setUid( teiUID.uid );
            entityInstance.setOrganisationUnit( orgUnit );
            entityInstance.setTrackedEntityType( entityType );
        }

        Set<TrackedEntityAttributeValue> attributeValues = getSMSAttributeValues( subm, entityInstance );

        if ( teiExists )
        {
            entityInstance.setTrackedEntityAttributeValues( attributeValues );
            teiService.updateTrackedEntityInstance( entityInstance );
        }
        else
        {
            teiService.createTrackedEntityInstance( entityInstance, attributeValues );
        }

        TrackedEntityInstance tei = teiService.getTrackedEntityInstance( teiUID.uid );

        // TODO: Unsure about this handling for enrollments, this needs to be
        // checked closely
        ProgramInstance enrollment;
        boolean enrollmentExists = programInstanceService.programInstanceExists( enrollmentid.uid );
        if ( enrollmentExists )
        {
            enrollment = programInstanceService.getProgramInstance( enrollmentid.uid );
            // Update these dates in case they've changed
            enrollment.setEnrollmentDate( enrollmentDate );
            enrollment.setIncidentDate( incidentDate );
        }
        else
        {
            enrollment = programInstanceService.enrollTrackedEntityInstance( tei, program, enrollmentDate, incidentDate,
                orgUnit );
        }
        if ( enrollment == null )
        {
            throw new SMSProcessingException( SMSResponse.ENROLL_FAILED.set( teiUID, progid ) );
        }
        enrollment.setStatus( getCoreProgramStatus( subm.getEnrollmentStatus() ) );
        enrollment.setGeometry( convertGeoPointToGeometry( subm.getCoordinates() ) );
        programInstanceService.updateProgramInstance( enrollment );

        // We now check if the enrollment has events to process
        User user = userService.getUser( subm.getUserID().uid );
        List<Object> errorUIDs = new ArrayList<>();
        if ( subm.getEvents() != null )
        {
            for ( SMSEvent event : subm.getEvents() )
            {
                errorUIDs.addAll( processEvent( event, user, enrollment, sms ) );
            }
        }

        if ( !errorUIDs.isEmpty() )
        {
            return SMSResponse.WARN_DVERR.setList( errorUIDs );
        }

        if ( attributeValues == null || attributeValues.isEmpty() )
        {
            // TODO: Is this correct handling?
            return SMSResponse.WARN_AVEMPTY;
        }

        return SMSResponse.SUCCESS;
    }

    @Override
    protected boolean handlesType( SubmissionType type )
    {
        return (type == SubmissionType.ENROLLMENT);
    }

    private Set<TrackedEntityAttributeValue> getSMSAttributeValues( EnrollmentSMSSubmission submission,
        TrackedEntityInstance entityInstance )
    {
        if ( submission.getValues() == null )
        {
            return null;
        }
        return submission.getValues().stream().map( v -> createTrackedEntityValue( v, entityInstance ) )
            .collect( Collectors.toSet() );
    }

    protected TrackedEntityAttributeValue createTrackedEntityValue( SMSAttributeValue SMSAttributeValue,
        TrackedEntityInstance tei )
    {
        UID attribUID = SMSAttributeValue.getAttribute();
        String val = SMSAttributeValue.getValue();

        TrackedEntityAttribute attribute = trackedEntityAttributeService.getTrackedEntityAttribute( attribUID.uid );
        if ( attribute == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_ATTRIB.set( attribUID ) );
        }
        else if ( val == null )
        {
            // TODO: Is this an error we can't recover from?
            throw new SMSProcessingException( SMSResponse.NULL_ATTRIBVAL.set( attribUID ) );
        }
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute( attribute );
        trackedEntityAttributeValue.setEntityInstance( tei );
        trackedEntityAttributeValue.setValue( val );
        return trackedEntityAttributeValue;
    }

    protected List<Object> processEvent( SMSEvent event, User user, ProgramInstance programInstance, IncomingSms sms )
    {
        UID stageid = event.getProgramStage();
        UID aocid = event.getAttributeOptionCombo();
        UID orgunitid = event.getOrgUnit();

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( orgunitid.uid );
        if ( orgUnit == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_ORGUNIT.set( orgunitid ) );
        }

        ProgramStage programStage = programStageService.getProgramStage( stageid.uid );
        if ( programStage == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_STAGE.set( stageid ) );
        }

        CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo( aocid.uid );
        if ( aoc == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_AOC.set( aocid ) );
        }

        List<Object> errorUIDs = saveNewEvent( event.getEvent().uid, orgUnit, programStage, programInstance, sms, aoc,
            user, event.getValues(), event.getEventStatus(), event.getEventDate(), event.getDueDate(),
            event.getCoordinates() );

        return errorUIDs;
    }
}