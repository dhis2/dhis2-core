package org.hisp.dhis.tracker.validation.hooks;

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

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.USER_CANT_BE_NULL;
import static org.hisp.dhis.util.DateUtils.getIso8601;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentInExistingValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Autowired
    protected TrackerOwnershipManager trackerOwnershipManager;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    private TrackerImportAccessManager trackerImportAccessManager;

    @Override
    public int getOrder()
    {
        return 104;
    }

    public EnrollmentInExistingValidationHook()
    {
        super( Enrollment.class, TrackerImportStrategy.CREATE_AND_UPDATE );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();
        TrackerImportStrategy strategy = validationContext.getStrategy( enrollment );
        TrackerBundle bundle = validationContext.getBundle();

        // TODO: check existing on update...
        if ( EnrollmentStatus.CANCELLED == enrollment.getStatus() )
        {
            return;
        }

        Program program = validationContext.getProgram( enrollment.getProgram() );

        if ( (EnrollmentStatus.COMPLETED == enrollment.getStatus()
            && Boolean.FALSE.equals( program.getOnlyEnrollOnce() )) )
        {
            return;
        }

        validateTeiNotEnrolledAlready( reporter, enrollment, program );
    }

    protected void validateTeiNotEnrolledAlready( ValidationErrorReporter reporter,
        Enrollment enrollment, Program program )
    {
        User user = reporter.getValidationContext().getBundle().getUser();
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( enrollment.getTrackedEntity(), TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        TrackedEntityInstance tei = reporter.getValidationContext()
            .getTrackedEntityInstance( enrollment.getTrackedEntity() );

        // TODO: SQLOPT Sort out only the programs the importing user has access too...
        //   create a dedicated sql query....?
        // Stian, Morten H.  NOTE: How will this affect validation? If there is a conflict here but importing user is not allowed to know,
        // should import still be possible?
        Set<Enrollment> activeAndCompleted = getAllEnrollments( reporter, user, program, tei )
            .stream()
            .filter( programEnrollment -> EnrollmentStatus.ACTIVE == programEnrollment.getStatus()
                || EnrollmentStatus.COMPLETED == programEnrollment.getStatus() )
            .collect( Collectors.toSet() );

        if ( EnrollmentStatus.ACTIVE == enrollment.getStatus() )
        {
            Set<Enrollment> activeEnrollments = activeAndCompleted.stream()
                .filter( programEnrollment -> EnrollmentStatus.ACTIVE == programEnrollment.getStatus() )
                .collect( Collectors.toSet() );

            if ( !activeEnrollments.isEmpty() )
            {
                //Error: TrackedEntityInstance already has an active enrollment in another program...
                reporter.addError( newReport( TrackerErrorCode.E1015 )
                    .addArg( tei )
                    .addArg( program ) );
            }
        }

        // Enrollment(¶4.b.ii) - The error of enrolling more than once is possible only if the imported enrollment
        // has a state other than CANCELLED...
        if ( Boolean.TRUE.equals( program.getOnlyEnrollOnce() ) && !activeAndCompleted.isEmpty() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1016 )
                .addArg( tei )
                .addArg( program ) );
        }
    }

    public List<Enrollment> getAllEnrollments( ValidationErrorReporter reporter, User actingUser,
        Program program, TrackedEntityInstance trackedEntityInstance )
    {
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntityInstance, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setSkipPaging( true );
        params.setProgram( program );
        params.setTrackedEntityInstance( trackedEntityInstance );
        List<ProgramInstance> programInstances = programInstanceService.getProgramInstances( params );

        List<Enrollment> all = new ArrayList<>();

        for ( ProgramInstance programInstance : programInstances )
        {
            // TODO: Move to ownership/security pre check hook if possible?
            if ( trackerOwnershipManager
                .hasAccess( actingUser, programInstance.getEntityInstance(), programInstance.getProgram() ) )
            {
                // Always create a fork of the reporter when used for checking/counting errors,
                // this is needed for thread safety in parallel mode.
                ValidationErrorReporter reporterFork = reporter.fork();

                trackerImportAccessManager.checkReadEnrollmentAccess( reporterFork, actingUser, programInstance );

                if ( reporterFork.hasErrors() )
                {
                    reporter.merge( reporterFork );
                }
                else
                {
                    all.add( getEnrollmentFromProgramInstance( programInstance ) );
                }
            }
        }

        return all;
    }

    public Enrollment getEnrollmentFromProgramInstance( ProgramInstance programInstance )
    {
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( programInstance.getUid() );

        if ( programInstance.getEntityInstance() != null )
        {
            enrollment.setTrackedEntityType( programInstance.getEntityInstance().getTrackedEntityType().getUid() );
            enrollment.setTrackedEntity( programInstance.getEntityInstance().getUid() );
        }

        if ( programInstance.getOrganisationUnit() != null )
        {
            enrollment.setOrgUnit( programInstance.getOrganisationUnit().getUid() );
//            enrollment.setOrgUnitName( programInstance.getOrganisationUnit().getName() );
        }

        if ( programInstance.getGeometry() != null )
        {
            enrollment.setGeometry( programInstance.getGeometry() );

//            if ( programInstance.getProgram().getFeatureType() == FeatureType.POINT )
//            {
//                com.vividsolutions.jts.geom.Coordinate co = programInstance.getGeometry().getCoordinate();
//                enrollment.setCoordinate( new Coordinate( co.x, co.y ) );
//            }
        }

        enrollment.setCreatedAt( DateUtils.getIso8601NoTz( programInstance.getCreated() ) );
//        enrollment.setCreatedAtClient( DateUtils.getIso8601NoTz( programInstance.getCreatedAtClient() ) );
        enrollment.setUpdatedAt( DateUtils.getIso8601NoTz( programInstance.getLastUpdated() ) );
//        enrollment.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( programInstance.getLastUpdatedAtClient() ) );
        enrollment.setProgram( programInstance.getProgram().getUid() );
        enrollment.setStatus( EnrollmentStatus.fromProgramStatus( programInstance.getStatus() ) );
        enrollment.setEnrolledAt( getIso8601( programInstance.getEnrollmentDate() ) );
        enrollment.setOccurredAt( getIso8601( programInstance.getIncidentDate() ) );
        enrollment.setFollowUp( programInstance.getFollowup() );
        enrollment.setCreatedAt( getIso8601( programInstance.getEndDate() ) );
        enrollment.setCompletedBy( programInstance.getCompletedBy() );
        enrollment.setStoredBy( programInstance.getStoredBy() );
        enrollment.setDeleted( programInstance.isDeleted() );

        List<TrackedEntityComment> comments = programInstance.getComments();

        for ( TrackedEntityComment comment : comments )
        {
            Note note = new Note();

            note.setNote( comment.getUid() );
            note.setValue( comment.getCommentText() );
            note.setStoredBy( comment.getCreator() );
            note.setStoredAt( DateUtils.getIso8601NoTz( comment.getCreated() ) );

            enrollment.getNotes().add( note );
        }

        return enrollment;
    }
}