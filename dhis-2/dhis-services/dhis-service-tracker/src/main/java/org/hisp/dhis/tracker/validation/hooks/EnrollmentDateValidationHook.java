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
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

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
        return 106;
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

            // NOTE: maybe this should qualify as a hard break, on the prev hook (required properties).
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

        if ( program.getDisplayIncidentDate() )
        {
            if ( enrollment.getIncidentDate() == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1023 ) );
            }

            boolean validEnrollmentIncidentDate = DateUtils
                .dateIsValid( DateUtils.getMediumDateString( enrollment.getIncidentDate() ) );

            if ( !validEnrollmentIncidentDate )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1024 )
                    .addArg( enrollment.getIncidentDate() ) );
            }
        }

        // NOTE: Need clarification regarding if this should be checked or not
//        boolean validEnrollmentIncidentDate =
//            enrollment.getIncidentDate() != null
//                && DateUtils.dateIsValid( DateUtils.getMediumDateString( enrollment.getIncidentDate() ) );
//
//        if ( !validEnrollmentIncidentDate )
//        {
//            errorReporter.addError( newReport( TrackerErrorCode.E1024 )
//                .addArg( enrollment.getIncidentDate() ) );
//        }
//
//        boolean validEnrollmentDate =
//            enrollment.getEnrollmentDate() != null
//                && DateUtils.dateIsValid( DateUtils.getMediumDateString( enrollment.getEnrollmentDate() ) );
//
//        if ( !validEnrollmentDate )
//        {
//            errorReporter.addError( newReport( TrackerErrorCode.E1025 )
//                .addArg( enrollment.getEnrollmentDate() ) );
//        }

        if ( Boolean.FALSE.equals( program.getSelectEnrollmentDatesInFuture() ) )
        {
            boolean enrollmentIsInFuture = Objects.nonNull( enrollment.getEnrollmentDate() ) &&
                enrollment.getEnrollmentDate().after( new Date() );
            if ( enrollmentIsInFuture )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1020 )
                    .addArg( enrollment.getEnrollmentDate() ) );
            }
        }

        if ( Boolean.FALSE.equals( program.getSelectIncidentDatesInFuture() ) )
        {
            boolean incidentIsInFuture = Objects.nonNull( enrollment.getIncidentDate() )
                && enrollment.getIncidentDate().after( new Date() );
            if ( incidentIsInFuture )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1021 )
                    .addArg( enrollment.getIncidentDate() ) );
            }
        }

        boolean validEnrollmentCreatedAtClientDate =
            enrollment.getCreatedAtClient() != null && DateUtils.dateIsValid( enrollment.getCreatedAtClient() );

        if ( !validEnrollmentCreatedAtClientDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1026 )
                .addArg( enrollment.getCreatedAtClient() ) );
        }

        boolean validLastUpdatedAtClientDate =
            enrollment.getLastUpdatedAtClient() != null && DateUtils.dateIsValid( enrollment.getLastUpdatedAtClient() );

        if ( !validLastUpdatedAtClientDate )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1027 )
                .addArg( enrollment.getLastUpdatedAtClient() ) );
        }
    }
}
