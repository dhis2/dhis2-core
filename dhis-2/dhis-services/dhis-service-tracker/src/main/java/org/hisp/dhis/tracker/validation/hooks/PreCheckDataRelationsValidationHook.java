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
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckDataRelationsValidationHook
    extends AbstractPreCheckValidationHook
{
    @Override
    public int getOrder()
    {
        return 4;
    }

    @Override
    public void validateTrackedEntities( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        // NOTHING TO DO HERE
    }

    @Override
    public void validateEnrollments( ValidationErrorReporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );

        if ( !program.isRegistration() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1014 )
                .addArg( program ) );
        }

        TrackedEntityInstance tei = PreheatHelper.getTei( bundle, enrollment.getTrackedEntity() );

        if ( tei == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1068 )
                .addArg( enrollment.getTrackedEntity() ) );
        }

        if ( tei != null && program.getTrackedEntityType() != null
            && !program.getTrackedEntityType().equals( tei.getTrackedEntityType() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1022 )
                .addArg( tei )
                .addArg( program ) );
        }

        ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, enrollment.getEnrollment() );

        if ( !bundle.getImportStrategy().isCreate() && programInstance == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1015 )
                .addArg( enrollment )
                .addArg( enrollment.getEnrollment() ) );
        }
    }

    @Override
    public void validateEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

        if ( program.isRegistration() )
        {
            TrackedEntityInstance tei = PreheatHelper.getTei( bundle, event.getTrackedEntity() );
            if ( tei == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1036 )
                    .addArg( event ) );
            }

            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
            ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );

            if ( programStage != null && programInstance != null &&
                !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1039 ) );
            }
        }
    }
}
