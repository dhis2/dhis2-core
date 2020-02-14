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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentSecurityValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 101;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );

        User actingUser = bundle.getPreheat().getUser();

        for ( Enrollment enrollment : bundle.getEnrollments() )
        {
            reporter.increment( enrollment );

            Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
            OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, enrollment.getOrgUnit() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, enrollment.getTrackedEntityInstance() );

            // NOTE: maybe this should qualify as a hard break, on the prev hook (required properties).
            if ( program == null || organisationUnit == null || trackedEntityInstance == null )
            {
                continue;
            }

            if ( !trackerOwnershipManager.hasAccess( actingUser, trackedEntityInstance, program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1028 )
                    .addArg( trackedEntityInstance )
                    .addArg( program ) );
                continue;
            }

            if ( bundle.getImportStrategy().isUpdate() )
            {
                ProgramInstance programInstance = PreheatHelper
                    .getProgramInstance( bundle, enrollment.getEnrollment() );
                if ( programInstance == null )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1015 )
                        .addArg( enrollment )
                        .addArg( enrollment.getEnrollment() ) );
                    continue;
                }

                List<String> errors = trackerAccessManager.canUpdate( actingUser, programInstance, false );
                if ( !errors.isEmpty() )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1000 )
                        .addArg( actingUser )
                        .addArg( programInstance ) );
                    continue;
                }
            }

            if ( bundle.getImportStrategy().isCreate() )
            {
                ProgramInstance programInstance = new ProgramInstance( program, trackedEntityInstance,
                    organisationUnit );

                List<String> errors = trackerAccessManager.canCreate( actingUser, programInstance, false );
                if ( !errors.isEmpty() )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1000 )
                        .addArg( actingUser )
                        .addArg( String.join( ",", errors ) ) );
                }
            }
        }

        return reporter.getReportList();
    }

}
