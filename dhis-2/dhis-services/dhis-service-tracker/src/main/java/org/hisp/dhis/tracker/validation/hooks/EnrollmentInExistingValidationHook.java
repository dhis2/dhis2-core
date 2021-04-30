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
package org.hisp.dhis.tracker.validation.hooks;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1015;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1016;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.program.*;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentInExistingValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );

        if ( EnrollmentStatus.CANCELLED == enrollment.getStatus() )
        {
            return;
        }

        TrackerImportValidationContext validationContext = reporter.getValidationContext();

        Program program = validationContext.getProgram( enrollment.getProgram() );

        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( (EnrollmentStatus.COMPLETED == enrollment.getStatus()
            && Boolean.FALSE.equals( program.getOnlyEnrollOnce() )) )
        {
            return;
        }

        validateTeiNotEnrolledAlready( reporter, enrollment, program );
    }

    private void validateTeiNotEnrolledAlready( ValidationErrorReporter reporter,
        Enrollment enrollment, Program program )
    {
        checkNotNull( enrollment.getTrackedEntity(), TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        TrackedEntityInstance tei = getTrackedEntityInstance( reporter, enrollment.getTrackedEntity() );

        Set<Enrollment> payloadEnrollment = reporter.getValidationContext().getBundle().getEnrollments()
            .stream().filter( Objects::nonNull )
            .filter( pi -> pi.getProgram().equals( program.getUid() ) )
            .filter( pi -> pi.getTrackedEntity().equals( tei.getUid() )
                && !pi.getUid().equals( enrollment.getEnrollment() ) )
            .filter( pi -> EnrollmentStatus.ACTIVE == pi.getStatus() || EnrollmentStatus.COMPLETED == pi.getStatus() )
            .collect( Collectors.toSet() );

        Set<Enrollment> dbEnrollment = reporter.getValidationContext().getBundle().getPreheat()
            .getTrackedEntityToProgramInstanceMap().getOrDefault( enrollment.getTrackedEntity(), new ArrayList<>() )
            .stream()
            .filter( Objects::nonNull )
            .filter( pi -> pi.getProgram().getUid().equals( program.getUid() )
                && !pi.getUid().equals( enrollment.getEnrollment() ) )
            .filter( pi -> ProgramStatus.ACTIVE == pi.getStatus() || ProgramStatus.COMPLETED == pi.getStatus() )
            .collect( Collectors.toSet() ).stream().map( this::getEnrollmentFromProgramInstance )
            .collect( Collectors.toSet() );

        // Priority to payload
        Collection<Enrollment> mergedEnrollments = Stream.of( payloadEnrollment, dbEnrollment )
            .flatMap( Set::stream )
            .collect( Collectors.toMap( Enrollment::getEnrollment,
                p -> p,
                ( Enrollment x, Enrollment y ) -> x ) )
            .values();

        if ( EnrollmentStatus.ACTIVE == enrollment.getStatus() )
        {
            Set<Enrollment> activeOnly = mergedEnrollments.stream()
                .filter( e -> EnrollmentStatus.ACTIVE == e.getStatus() )
                .collect( Collectors.toSet() );

            if ( !activeOnly.isEmpty() && !activeOnly.contains( enrollment ) )
            {
                addError( reporter, E1015, tei, program );
            }
        }

        if ( !mergedEnrollments.isEmpty() )
        {
            addError( reporter, E1016, tei, program );
        }
    }

    public Enrollment getEnrollmentFromProgramInstance( ProgramInstance programInstance )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( programInstance.getUid() );
        enrollment.setStatus( EnrollmentStatus.fromProgramStatus( programInstance.getStatus() ) );

        return enrollment;
    }

    private TrackedEntityInstance getTrackedEntityInstance( ValidationErrorReporter reporter, String uid )
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