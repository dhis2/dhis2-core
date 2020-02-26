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
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ENROLLMENT_CAN_T_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CAN_T_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentDateValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 107;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        if ( bundle.getImportStrategy().isDelete() )
        {
            return Collections.emptyList();
        }

        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        for ( Enrollment enrollment : bundle.getEnrollments() )
        {
            reporter.increment( enrollment );

            Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
            // Hard break?
            if ( program == null )
            {
                continue;
            }

            validateEnrollmentDates( reporter, program, enrollment );
        }

        return reporter.getReportList();
    }

    private void validateEnrollmentDates( ValidationErrorReporter errorReporter, Program program,
        Enrollment enrollment )
    {
        Objects.requireNonNull( program, PROGRAM_CAN_T_BE_NULL );
        Objects.requireNonNull( enrollment, ENROLLMENT_CAN_T_BE_NULL );

        // NOTE: getCreatedAtClient is always mandatory?
        if ( !isValidDateStringAndNotNull( enrollment.getCreatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1026 )
                .addArg( enrollment.getCreatedAtClient() ) );
        }
        // NOTE: getLastUpdatedAtClient is always mandatory?
        if ( !isValidDateStringAndNotNull( enrollment.getLastUpdatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1027 )
                .addArg( enrollment.getLastUpdatedAtClient() ) );
        }
        // NOTE: getEnrollmentDate is always mandatory?
        if ( !isValidDateAndNotNull( enrollment.getEnrollmentDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1025 )
                .addArg( enrollment.getEnrollmentDate() ) );
        }

        if ( enrollment.getEnrollmentDate() != null
            && Boolean.FALSE.equals( program.getSelectEnrollmentDatesInFuture() )
            && enrollment.getEnrollmentDate().after( new Date() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1020 )
                .addArg( enrollment.getEnrollmentDate() ) );
        }

        if ( enrollment.getIncidentDate() != null
            && Boolean.FALSE.equals( program.getSelectIncidentDatesInFuture() )
            && enrollment.getIncidentDate().after( new Date() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1021 )
                .addArg( enrollment.getIncidentDate() ) );
        }

        // NOTE: getIncidentDate is only mandatory if getDisplayIncidentDate TRUE?
        if ( Boolean.TRUE.equals( program.getDisplayIncidentDate() )
            && !isValidDateAndNotNull( enrollment.getIncidentDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1023 )
                .addArg( enrollment.getIncidentDate() ) );
        }
    }
}
