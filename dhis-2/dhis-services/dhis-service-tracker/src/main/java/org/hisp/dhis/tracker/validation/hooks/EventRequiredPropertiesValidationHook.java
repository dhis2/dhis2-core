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

import com.google.common.base.Preconditions;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.*;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventRequiredPropertiesValidationHook
    extends AbstractTrackerValidationHook
{

    @Override
    public int getOrder()
    {
        return 300;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );
        User actingUser = bundle.getPreheat().getUser();

        for ( Event event : bundle.getEvents() )
        {
            reporter.increment( event );

            ProgramStageInstance programStageInstance = PreheatHelper
                .getProgramStageInstance( bundle, event.getEvent() );
            ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
            OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, event.getOrgUnit() );
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, event.getTrackedEntity() );
            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

            if ( bundle.getImportStrategy().isUpdate()
                && (programStageInstance != null && programStageInstance.isDeleted()) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1082 )
                    .addArg( event.getEvent() ) );
            }

            if ( bundle.getImportStrategy().isCreate() && organisationUnit == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( event.getOrgUnit() ) );
            }

            if ( program == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1034 )
                    .addArg( event ) );

                // Not anything more to check if program is null
                continue;
            }

            programStage = (programStage == null && program.isWithoutRegistration())
                ? program.getProgramStageByStage( 1 ) : programStage;

            if ( programStage == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1035 )
                    .addArg( event ) );
            }

            programInstance = validateProgramInstance( reporter, actingUser, event, programStage, programInstance,
                trackedEntityInstance, program );

            if ( !programInstance.getProgram().hasOrganisationUnit( organisationUnit ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1041 )
                    .addArg( organisationUnit ) );
            }
        }

        return reporter.getReportList();
    }

    protected ProgramInstance validateProgramInstance( ValidationErrorReporter reporter, User actingUser, Event event,
        ProgramStage programStage, ProgramInstance programInstance, TrackedEntityInstance trackedEntityInstance,
        Program program )
    {
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Preconditions.checkNotNull( actingUser, USER_CANT_BE_NULL );

        if ( program.isRegistration() )
        {
            if ( trackedEntityInstance == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1036 )
                    .addArg( event ) );
            }

            if ( programInstance == null && trackedEntityInstance != null )
            {
                List<ProgramInstance> activeProgramInstances = new ArrayList<>( programInstanceService
                    .getProgramInstances( trackedEntityInstance, program, ProgramStatus.ACTIVE ) );

                if ( activeProgramInstances.isEmpty() )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1037 )
                        .addArg( trackedEntityInstance )
                        .addArg( program ) );
                }
                else if ( activeProgramInstances.size() > 1 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1038 )
                        .addArg( trackedEntityInstance )
                        .addArg( program ) );
                }
                else
                {
                    programInstance = activeProgramInstances.get( 0 );
                }
            }

            if ( programStage != null && programInstance != null &&
                !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1039 ) );
            }
        }
        else
        {

            // NOTE: This is cached in the prev. event importer? What do we do here?
            List<ProgramInstance> activeProgramInstances = programInstanceService
                .getProgramInstances( program, ProgramStatus.ACTIVE );

            if ( activeProgramInstances.isEmpty() )
            {
                ProgramInstance pi = new ProgramInstance();
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );
                pi.setStoredBy( actingUser.getUsername() );

                programInstance = pi;
            }
            else if ( activeProgramInstances.size() > 1 )
            {
                reporter.addError( newReport( TrackerErrorCode.E1040 )
                    .addArg( program ) );
            }
            else
            {
                programInstance = activeProgramInstances.get( 0 );
            }
        }

        return programInstance;
    }

}
