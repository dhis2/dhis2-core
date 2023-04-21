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
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1020;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1021;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1023;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1025;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.imports.validation.validator.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class DateValidator
    implements Validator<Enrollment>
{
    @Override
    public void validate( Reporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        validateMandatoryDates( reporter, enrollment );

        Program program = bundle.getPreheat().getProgram( enrollment.getProgram() );

        validateEnrollmentDatesNotInFuture( reporter, program, enrollment );

        if ( Boolean.TRUE.equals( program.getDisplayIncidentDate() ) &&
            Objects.isNull( enrollment.getOccurredAt() ) )
        {
            reporter.addError( enrollment, E1023, enrollment.getOccurredAt() );
        }
    }

    private void validateMandatoryDates( Reporter reporter, Enrollment enrollment )
    {
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );

        if ( Objects.isNull( enrollment.getEnrolledAt() ) )
        {
            reporter.addError( enrollment, E1025, enrollment.getEnrolledAt() );
        }
    }

    private void validateEnrollmentDatesNotInFuture( Reporter reporter, Program program,
        Enrollment enrollment )
    {
        checkNotNull( program, PROGRAM_CANT_BE_NULL );
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );

        final LocalDate now = LocalDate.now();
        if ( Objects.nonNull( enrollment.getEnrolledAt() )
            && Boolean.FALSE.equals( program.getSelectEnrollmentDatesInFuture() )
            && enrollment.getEnrolledAt().atOffset( ZoneOffset.UTC ).toLocalDate().isAfter( now ) )
        {
            reporter.addError( enrollment, E1020, enrollment.getEnrolledAt() );
        }

        if ( Objects.nonNull( enrollment.getOccurredAt() )
            && Boolean.FALSE.equals( program.getSelectIncidentDatesInFuture() )
            && enrollment.getOccurredAt().atOffset( ZoneOffset.UTC ).toLocalDate().isAfter( now ) )
        {
            reporter.addError( enrollment, E1021, enrollment.getOccurredAt() );
        }
    }
}
