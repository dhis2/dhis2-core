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
package org.hisp.dhis.tracker.imports.validation.validator.enrollment;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1015;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1016;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class ExistingEnrollmentValidator
    implements Validator<org.hisp.dhis.tracker.imports.domain.Enrollment>
{
    @Override
    public void validate( Reporter reporter, TrackerBundle bundle,
        org.hisp.dhis.tracker.imports.domain.Enrollment enrollment )
    {
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );

        if ( EnrollmentStatus.CANCELLED == enrollment.getStatus() )
        {
            return;
        }

        Program program = bundle.getPreheat().getProgram( enrollment.getProgram() );

        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( (EnrollmentStatus.COMPLETED == enrollment.getStatus()
            && Boolean.FALSE.equals( program.getOnlyEnrollOnce() )) )
        {
            return;
        }

        validateTeiNotEnrolledAlready( reporter, bundle, enrollment, program );
    }

    private void validateTeiNotEnrolledAlready( Reporter reporter, TrackerBundle bundle,
        org.hisp.dhis.tracker.imports.domain.Enrollment enrollment, Program program )
    {
        checkNotNull( enrollment.getTrackedEntity(), TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        TrackedEntity tei = getTrackedEntityInstance( bundle, enrollment.getTrackedEntity() );

        Set<org.hisp.dhis.tracker.imports.domain.Enrollment> payloadEnrollment = bundle.getEnrollments()
            .stream().filter( Objects::nonNull )
            .filter( e -> e.getProgram().isEqualTo( program ) )
            .filter( e -> e.getTrackedEntity().equals( tei.getUid() )
                && !e.getEnrollment().equals( enrollment.getEnrollment() ) )
            .filter( e -> EnrollmentStatus.ACTIVE == e.getStatus() || EnrollmentStatus.COMPLETED == e.getStatus() )
            .collect( Collectors.toSet() );

        Set<org.hisp.dhis.tracker.imports.domain.Enrollment> dbEnrollment = bundle.getPreheat()
            .getTrackedEntityToEnrollmentMap().getOrDefault( enrollment.getTrackedEntity(), new ArrayList<>() )
            .stream()
            .filter( Objects::nonNull )
            .filter( e -> e.getProgram().getUid().equals( program.getUid() )
                && !e.getUid().equals( enrollment.getEnrollment() ) )
            .filter( e -> ProgramStatus.ACTIVE == e.getStatus() || ProgramStatus.COMPLETED == e.getStatus() )
            .distinct().map( this::getEnrollmentFromDbEnrollment )
            .collect( Collectors.toSet() );

        // Priority to payload
        Collection<org.hisp.dhis.tracker.imports.domain.Enrollment> mergedEnrollments = Stream
            .of( payloadEnrollment, dbEnrollment )
            .flatMap( Set::stream )
            .filter( e -> !Objects.equals( e.getEnrollment(), enrollment.getEnrollment() ) )
            .collect( Collectors.toMap( org.hisp.dhis.tracker.imports.domain.Enrollment::getEnrollment,
                p -> p,
                ( org.hisp.dhis.tracker.imports.domain.Enrollment x,
                    org.hisp.dhis.tracker.imports.domain.Enrollment y ) -> x ) )
            .values();

        if ( EnrollmentStatus.ACTIVE == enrollment.getStatus() )
        {
            Set<org.hisp.dhis.tracker.imports.domain.Enrollment> activeOnly = mergedEnrollments.stream()
                .filter( e -> EnrollmentStatus.ACTIVE == e.getStatus() )
                .collect( Collectors.toSet() );

            if ( !activeOnly.isEmpty() )
            {
                reporter.addError( enrollment, E1015, tei, program );
            }
        }

        if ( Boolean.TRUE.equals( program.getOnlyEnrollOnce() ) && !mergedEnrollments.isEmpty() )
        {
            reporter.addError( enrollment, E1016, tei, program );
        }
    }

    public org.hisp.dhis.tracker.imports.domain.Enrollment getEnrollmentFromDbEnrollment(
        Enrollment dbEnrollment )
    {
        org.hisp.dhis.tracker.imports.domain.Enrollment enrollment = new org.hisp.dhis.tracker.imports.domain.Enrollment();
        enrollment.setEnrollment( dbEnrollment.getUid() );
        enrollment.setStatus( EnrollmentStatus.fromProgramStatus( dbEnrollment.getStatus() ) );

        return enrollment;
    }

    private TrackedEntity getTrackedEntityInstance( TrackerBundle bundle, String uid )
    {
        TrackedEntity tei = bundle.getPreheat().getTrackedEntity( uid );

        if ( tei == null && bundle.findTrackedEntityByUid( uid ).isPresent() )
        {
            tei = new TrackedEntity();
            tei.setUid( uid );

        }
        return tei;
    }
}