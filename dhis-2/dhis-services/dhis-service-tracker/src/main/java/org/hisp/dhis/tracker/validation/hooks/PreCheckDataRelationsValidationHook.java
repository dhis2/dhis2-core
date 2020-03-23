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
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.EVENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.USER_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckDataRelationsValidationHook
    extends AbstractPreCheckValidationHook
{
    @Autowired
    protected TrackerOwnershipManager trackerOwnershipManager;

    @Override
    public int getOrder()
    {
        return 3;
    }

    @Override
    public void validateTrackedEntities( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        TrackedEntityInstance tei = PreheatHelper.getTei( bundle, trackedEntity.getTrackedEntity() );

        if ( tei != null && bundle.getImportStrategy().isCreate() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1002 )
                .addArg( trackedEntity.getTrackedEntity() ) );
        }
        else if ( tei == null && (bundle.getImportStrategy().isUpdate() || bundle.getImportStrategy().isDelete()) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1063 )
                .addArg( trackedEntity.getTrackedEntity() ) );
        }

        if ( tei != null && bundle.getImportStrategy().isUpdate() )
        {
            // Make sure tei has org unit set.
            if ( tei.getOrganisationUnit() == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( tei ) );
            }
        }
    }

    @Override
    public void validateEnrollments( ValidationErrorReporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        boolean exists = PreheatHelper.getProgramInstance( bundle, enrollment.getEnrollment() ) != null;

        if ( exists && bundle.getImportStrategy().isCreate() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1080 )
                .addArg( enrollment.getEnrollment() ) );
        }
        else if ( !exists && (bundle.getImportStrategy().isUpdate() || bundle.getImportStrategy().isDelete()) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1081 )
                .addArg( enrollment.getEnrollment() ) );
        }

        Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
        if ( !program.isRegistration() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1014 )
                .addArg( program ) );
        }

        TrackedEntityInstance trackedEntityInstance = PreheatHelper
            .getTei( bundle, enrollment.getTrackedEntity() );
        if ( trackedEntityInstance == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1068 )
                .addArg( enrollment.getTrackedEntity() ) );
        }

        if ( trackedEntityInstance != null )
        {
            boolean isNotSameTrackedEntityType = program.getTrackedEntityType() != null
                && !program.getTrackedEntityType().equals( trackedEntityInstance.getTrackedEntityType() );

            if ( isNotSameTrackedEntityType )
            {
                reporter.addError( newReport( TrackerErrorCode.E1022 )
                    .addArg( trackedEntityInstance )
                    .addArg( program ) );
            }
        }

        if ( !bundle.getImportStrategy().isCreate() )
        {
            ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, enrollment.getEnrollment() );

            if ( programInstance == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1015 )
                    .addArg( enrollment )
                    .addArg( enrollment.getEnrollment() ) );
            }
        }
    }

    @Override
    public void validateEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        boolean exists = PreheatHelper.getProgramStageInstance( bundle, event.getEvent() ) != null;

        if ( exists && bundle.getImportStrategy().isCreate() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1030 )
                .addArg( event.getEvent() ) );
        }
        else if ( !exists && (bundle.getImportStrategy().isUpdate() || bundle.getImportStrategy().isDelete()) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1032 )
                .addArg( event.getEvent() ) );
        }

        //TODO: Morten: I can't make this state, when I do a delete on the programStageInstance, the exist check fails before this...
        ProgramStageInstance programStageInstance = PreheatHelper.getProgramStageInstance( bundle, event.getEvent() );
        if ( bundle.getImportStrategy().isUpdate()
            && (programStageInstance != null && programStageInstance.isDeleted()) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1082 )
                .addArg( event.getEvent() ) );
        }

        ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
        TrackedEntityInstance trackedEntityInstance = PreheatHelper
            .getTei( bundle, event.getTrackedEntity() );
        Program program = PreheatHelper.getProgram( bundle, event.getProgram() );
        ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );

        validateProgramInstance( reporter, bundle.getUser(), event, programStage,
            programInstance,
            trackedEntityInstance,
            program );
    }

    protected void validateProgramInstance( ValidationErrorReporter reporter, User actingUser, Event event,
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

            ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
            params.setProgram( program );
            params.setTrackedEntityInstance( trackedEntityInstance );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
            params.setUser( actingUser );

            int count = programInstanceService.countProgramInstances( params );

            //TODO: I can't provoke this state where programInstance == NULL && program.isRegistration(),
            // the meta check will complain about that "is a registration but its program stage is not valid or missing." (E1086)
            if ( programInstance == null && trackedEntityInstance != null )
            {
                if ( count == 0 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1037 )
                        .addArg( trackedEntityInstance )
                        .addArg( program ) );
                }
                else if ( count > 1 )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1038 )
                        .addArg( trackedEntityInstance )
                        .addArg( program ) );
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
            //TODO: I don't understand the purpose of this here. This could be moved to preheater?
            ProgramInstanceQueryParams params = new ProgramInstanceQueryParams();
            params.setProgram( program );
            params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
            params.setUser( actingUser );

            int count = programInstanceService.countProgramInstances( params );

            if ( count > 1 )
            {
                reporter.addError( newReport( TrackerErrorCode.E1040 )
                    .addArg( program ) );
            }
        }
    }
}
