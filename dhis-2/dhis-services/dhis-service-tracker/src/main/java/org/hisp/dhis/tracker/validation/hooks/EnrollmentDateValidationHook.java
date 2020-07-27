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

import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentDateValidationHook
    extends AbstractTrackerDtoValidationHook
{
    public EnrollmentDateValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( Enrollment.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        validateMandatoryDates( reporter, enrollment );

        Program program = context.getProgram( enrollment.getProgram() );

        validateEnrollmentDatesNotInFuture( reporter, program, enrollment );

        if ( Boolean.TRUE.equals( program.getDisplayIncidentDate() ) &&
            !isValidDateStringAndNotNull( enrollment.getOccurredAt() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1023 ).addArg( enrollment.getOccurredAt() ) );
        }
    }

    private void validateMandatoryDates( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );

        if ( !isValidDateStringAndNotNull( enrollment.getEnrolledAt() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1025 )
                .addArg( enrollment.getEnrolledAt() ) );
        }
    }

    private void validateEnrollmentDatesNotInFuture( ValidationErrorReporter reporter, Program program,
        Enrollment enrollment )
    {
        checkNotNull( program, PROGRAM_CANT_BE_NULL );
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );

        if ( isValidDateStringAndNotNull( enrollment.getEnrolledAt() )
            && Boolean.FALSE.equals( program.getSelectEnrollmentDatesInFuture() )
            && DateUtils.parseDate( enrollment.getEnrolledAt() ).after( new Date() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1020 )
                .addArg( enrollment.getEnrolledAt() ) );
        }

        if ( isValidDateStringAndNotNull( enrollment.getOccurredAt() )
            && Boolean.FALSE.equals( program.getSelectIncidentDatesInFuture() )
            && DateUtils.parseDate( enrollment.getOccurredAt() ).after( new Date() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1021 )
                .addArg( enrollment.getOccurredAt() ) );
        }
    }
}