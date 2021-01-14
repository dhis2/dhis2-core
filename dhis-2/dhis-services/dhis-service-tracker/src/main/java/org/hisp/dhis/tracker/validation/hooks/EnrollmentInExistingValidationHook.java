package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1015;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1016;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentInExistingValidationHook
    extends AbstractTrackerDtoValidationHook
{
    protected final TrackerOwnershipManager trackerOwnershipManager;

    protected final ProgramInstanceService programInstanceService;

    private final TrackerImportAccessManager trackerImportAccessManager;

    public EnrollmentInExistingValidationHook( TrackerOwnershipManager trackerOwnershipManager,
        ProgramInstanceService programInstanceService,
        TrackerImportAccessManager trackerImportAccessManager )
    {
        checkNotNull( trackerOwnershipManager );
        checkNotNull( programInstanceService );
        checkNotNull( trackerImportAccessManager );

        this.trackerOwnershipManager = trackerOwnershipManager;
        this.programInstanceService = programInstanceService;
        this.trackerImportAccessManager = trackerImportAccessManager;
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext validationContext = reporter.getValidationContext();

        if ( EnrollmentStatus.CANCELLED == enrollment.getStatus() )
        {
            return;
        }

        Program program = validationContext.getProgram( enrollment.getProgram() );

        checkNotNull( program, PROGRAM_CANT_BE_NULL );

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

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );
        checkNotNull( enrollment.getTrackedEntity(), TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        TrackedEntityInstance tei = getTrackedEntityInstance( reporter, enrollment.getTrackedEntity() );

        // TODO: Create a dedicated sql query....?
        Set<Enrollment> activeAndCompleted = getAllEnrollments( reporter, program, tei.getUid() )
            .stream()
            .filter( e -> EnrollmentStatus.ACTIVE == e.getStatus() || EnrollmentStatus.COMPLETED == e.getStatus() )
            .collect( Collectors.toSet() );

        if ( EnrollmentStatus.ACTIVE == enrollment.getStatus() )
        {
            Set<Enrollment> activeOnly = activeAndCompleted.stream()
                .filter( e -> EnrollmentStatus.ACTIVE == e.getStatus() )
                .collect( Collectors.toSet() );

            if ( !activeOnly.isEmpty() && !activeOnly.contains( enrollment ) )
            {
                // TODO: How do we do this check on an import set, this only checks when the DB already contains it
                addError( reporter, E1015, tei, program );
            }
        }

        if ( Boolean.TRUE.equals( program.getOnlyEnrollOnce() ) && !activeAndCompleted.isEmpty() )
        {
            addError( reporter, E1016, tei, program );
        }
    }

    private List<Enrollment> getAllEnrollments( ValidationErrorReporter reporter, Program program,
        String trackedEntityInstanceUid )
    {
        User user = reporter.getValidationContext().getBundle().getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );
        //checkNotNull( trackedEntityInstance, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setSkipPaging( true );
        params.setProgram( program );
        params.setTrackedEntityInstanceUid( trackedEntityInstanceUid );
        List<ProgramInstance> programInstances = programInstanceService.getProgramInstances( params );

        List<Enrollment> all = new ArrayList<>();

        for ( ProgramInstance programInstance : programInstances )
        {
            if ( trackerOwnershipManager
                .hasAccess( user, programInstance.getEntityInstance(), programInstance.getProgram() ) )
            {

                ValidationErrorReporter localReporter = new ValidationErrorReporter( reporter.getValidationContext() );
                trackerImportAccessManager.checkReadEnrollmentAccess( localReporter, programInstance.getProgram(), programInstance.getOrganisationUnit(), programInstance.getEntityInstance().getUid());

                if ( localReporter.hasErrors() )
                {
                    reporter.merge( localReporter );
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
        checkNotNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( programInstance.getUid() );

        enrollment.setStatus( EnrollmentStatus.fromProgramStatus( programInstance.getStatus() ) );
        return enrollment;
    }

    /**
     * Get a {@link TrackedEntityInstance} from the pre-heat or from the reference
     * tree.
     *
     * @param reporter the {@link ValidationErrorReporter} object
     * @param uid the UID of a {@link TrackedEntityInstance} object
     * @return a TrackedEntityInstance
     */
    public TrackedEntityInstance getTrackedEntityInstance( ValidationErrorReporter reporter, String uid )
    {
        TrackedEntityInstance tei = reporter.getValidationContext().getTrackedEntityInstance( uid );

        if ( tei == null && reporter.getValidationContext().getReference( uid ).isPresent() )
        {
            tei = new TrackedEntityInstance();
            tei.setUid( uid );

        }
        return tei;
    }
}