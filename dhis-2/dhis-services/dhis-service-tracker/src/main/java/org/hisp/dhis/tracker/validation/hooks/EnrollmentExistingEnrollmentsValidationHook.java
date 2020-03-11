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
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Coordinate;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
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
public class EnrollmentExistingEnrollmentsValidationHook
    extends AbstractTrackerValidationHook
{
    @Autowired
    protected TrackerOwnershipManager trackerOwnershipManager;

    @Autowired
    private TrackerImportAccessManager trackerImportAccessManager;

    @Override
    public int getOrder()
    {
        return 104;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( bundle.getImportStrategy().isDelete() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        User actingUser = bundle.getPreheat().getUser();

        for ( Enrollment enrollment : bundle.getEnrollments() )
        {
            reporter.increment( enrollment );

            if ( EnrollmentStatus.CANCELLED == enrollment.getStatus() )
            {
                continue;
            }

            Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, enrollment.getTrackedEntity() );

            // NOTE: maybe this should qualify as a hard break, on the prev hook (required properties).
            if ( (program == null || trackedEntityInstance == null)
                || (EnrollmentStatus.COMPLETED == enrollment.getStatus()
                && Boolean.FALSE.equals( program.getOnlyEnrollOnce() )) )
            {
                continue;
            }

            validateNotEnrolledAlready( reporter, actingUser, enrollment, program, trackedEntityInstance );
        }

        return reporter.getReportList();
    }

    protected void validateNotEnrolledAlready( ValidationErrorReporter reporter, User actingUser,
        Enrollment enrollment, Program program, TrackedEntityInstance trackedEntityInstance )
    {
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntityInstance, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        // TODO: Sort out only the programs the importing user has access too...
        // Stian, Morten H.  NOTE: How will this affect validation? If there is a conflict here but importing user is not allowed to know,
        // should import still be possible?
        Set<Enrollment> activeOrCompletedEnrollments = getEnrollmentsUserHasAccessTo( reporter, actingUser,
            program, trackedEntityInstance ).stream()
            .filter( programEnrollment -> EnrollmentStatus.ACTIVE == programEnrollment.getStatus()
                || EnrollmentStatus.COMPLETED == programEnrollment.getStatus() )
            .collect( Collectors.toSet() );

        if ( EnrollmentStatus.ACTIVE == enrollment.getStatus() )
        {
            Set<Enrollment> activeEnrollments = activeOrCompletedEnrollments.stream()
                .filter( programEnrollment -> EnrollmentStatus.ACTIVE == programEnrollment.getStatus() )
                .collect( Collectors.toSet() );

            if ( !activeEnrollments.isEmpty() )
            {
                //Error: TrackedEntityInstance already has an active enrollment in another program...
                reporter.addError( newReport( TrackerErrorCode.E1015 )
                    .addArg( trackedEntityInstance )
                    .addArg( program ) );
            }
        }

        // Enrollment(¶4.b.ii) - The error of enrolling more than once is possible only if the imported enrollment
        // has a state other than CANCELLED...
        if ( Boolean.TRUE.equals( program.getOnlyEnrollOnce() )
            && !activeOrCompletedEnrollments.isEmpty() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1016 )
                .addArg( trackedEntityInstance )
                .addArg( program ) );
        }
    }

    public List<Enrollment> getEnrollmentsUserHasAccessTo( ValidationErrorReporter reporter, User actingUser,
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

        List<Enrollment> enrollments = new ArrayList<>();

        for ( ProgramInstance programInstance : programInstances )
        {
            // TODO: Move to ownership/security pre check hook if possible?
            if ( trackerOwnershipManager.hasAccess( actingUser, programInstance ) )
            {
                // Always fork the reporter when used for checking/counting errors,
                // will break hard otherwise when run in a multi threaded way
                ValidationErrorReporter fork = reporter.fork( null );

                trackerImportAccessManager.canRead( fork, actingUser, programInstance, true );

                if ( !fork.hasErrors() )
                {
                    enrollments.add( getEnrollmentFromProgramInstance( programInstance ) );
                }
                else
                {
                    reporter.merge( fork );
                }
            }
        }

        return enrollments;
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
            enrollment.setOrgUnitName( programInstance.getOrganisationUnit().getName() );
        }

        if ( programInstance.getGeometry() != null )
        {
            enrollment.setGeometry( programInstance.getGeometry() );

            if ( programInstance.getProgram().getFeatureType() == FeatureType.POINT )
            {
                com.vividsolutions.jts.geom.Coordinate co = programInstance.getGeometry().getCoordinate();
                enrollment.setCoordinate( new Coordinate( co.x, co.y ) );
            }
        }

        enrollment.setCreated( DateUtils.getIso8601NoTz( programInstance.getCreated() ) );
        enrollment.setCreatedAtClient( DateUtils.getIso8601NoTz( programInstance.getCreatedAtClient() ) );
        enrollment.setLastUpdated( DateUtils.getIso8601NoTz( programInstance.getLastUpdated() ) );
        enrollment.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( programInstance.getLastUpdatedAtClient() ) );
        enrollment.setProgram( programInstance.getProgram().getUid() );
        enrollment.setStatus( EnrollmentStatus.fromProgramStatus( programInstance.getStatus() ) );
        enrollment.setEnrollmentDate( getIso8601( programInstance.getEnrollmentDate() ) );
        enrollment.setIncidentDate( getIso8601( programInstance.getIncidentDate() ) );
        enrollment.setFollowup( programInstance.getFollowup() );
        enrollment.setCompletedDate( getIso8601( programInstance.getEndDate() ) );
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
            note.setStoredDate( DateUtils.getIso8601NoTz( comment.getCreated() ) );

            enrollment.getNotes().add( note );
        }

        return enrollment;
    }
}
