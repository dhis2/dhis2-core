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

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.EVENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.USER_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckSecurityValidationHook
    extends AbstractPreCheckValidationHook
{
    @Override
    public int getOrder()
    {
        return 2;
    }

    @Autowired
    protected AclService aclService;

    @Override
    public void validateTrackedEntities( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        TrackedEntityType entityType = getTrackedEntityType( bundle, trackedEntity );
        if ( entityType != null && !aclService.canDataWrite( bundle.getUser(), entityType ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1001 )
                .addArg( bundle.getUser() )
                .addArg( entityType ) );
        }

        OrganisationUnit orgUnit = getOrganisationUnit( bundle, trackedEntity );

        // TODO: Added comment to make sure the reason for this not so intuitive reason,
        // This should be better commented and documented somewhere
        // Ameen 10.09.2019, 12:32 fix: relax restriction on writing to tei in search scope 48a82e5f
        if ( orgUnit != null && !organisationUnitService.isInUserSearchHierarchyCached( bundle.getUser(), orgUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( bundle.getUser() )
                .addArg( orgUnit ) );
        }

        if ( bundle.getImportStrategy().isDelete() )
        {
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, trackedEntity.getTrackedEntity() );

            if ( trackedEntityInstance != null )
            {
                checkCanCascadeDeleteProgramInstances( reporter, bundle.getUser(), trackedEntityInstance );
            }
        }
    }

    @Override
    public void validateEnrollments( ValidationErrorReporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, enrollment.getOrgUnit() );

        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        // See note below on this.
        if ( !organisationUnitService.isInUserHierarchyCached( bundle.getUser(), organisationUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1028 )
                .addArg( bundle.getUser() )
                .addArg( enrollment )
                .addArg( program ) );
        }

        // TODO: This needs follow up, move to ownership validation
        // This method "trackerOwnershipManager.hasAccess()" does a lot of things,
        // is it better to use the above check directly when we are sure about the input org unit?
        // 1. Does checking hasTemporaryAccess make sense?
        // 2. Does checking isOpen make sense? we are importing not reading.
//            if ( !trackerOwnershipManager.hasAccess( actingUser, trackedEntityInstance, program ) )
//            {
//                reporter.addError( newReport( TrackerErrorCode.E1028 )
//                    .addArg( trackedEntityInstance )
//                    .addArg( program ) );
//                continue;
//            }

        if ( bundle.getImportStrategy().isCreate() )
        {
            TrackedEntityInstance trackedEntityInstance = PreheatHelper
                .getTrackedEntityInstance( bundle, enrollment.getTrackedEntity() );

            Objects.requireNonNull( trackedEntityInstance, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

            validateCreateEnrollment( reporter, bundle.getUser(), program, organisationUnit, trackedEntityInstance );
        }
        else
        {
            validateUpdateAndDeleteEnrollment( bundle, reporter, bundle.getUser(), enrollment );
        }
    }

    @Override
    public void validateEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        ProgramStageInstance programStageInstance = PreheatHelper
            .getProgramStageInstance( bundle, event.getEvent() );
        ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
        ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );
        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, event.getOrgUnit() );
        TrackedEntityInstance trackedEntityInstance = PreheatHelper
            .getTrackedEntityInstance( bundle, event.getTrackedEntity() );
        Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

        if ( organisationUnit != null &&
            !organisationUnitService.isInUserHierarchyCached( bundle.getUser(), organisationUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( bundle.getUser() )
                .addArg( organisationUnit ) );
        }

        if ( bundle.getImportStrategy().isCreate() )
        {
            validateCreateEvent( reporter, bundle.getUser(), event, programStageInstance, programStage, programInstance,
                organisationUnit,
                trackedEntityInstance, program );
        }
        else if ( bundle.getImportStrategy().isUpdate() || bundle.getImportStrategy().isDelete() )
        {
            validateUpdateAndDeleteEvent( bundle, reporter, event, programStageInstance );
        }
    }

    private void checkCanCascadeDeleteProgramInstances( ValidationErrorReporter errorReporter, User actingUser,
        TrackedEntityInstance trackedEntityInstance )
    {
        Objects.requireNonNull( actingUser, Constants.USER_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntityInstance, Constants.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        Set<ProgramInstance> programInstances = trackedEntityInstance.getProgramInstances().stream()
            .filter( pi -> !pi.isDeleted() )
            .collect( Collectors.toSet() );

        if ( !programInstances.isEmpty()
            && !actingUser.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.NONE )
                .addArg( trackedEntityInstance )
                .addArg( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) );
        }
    }

    protected void validateUpdateAndDeleteEvent( TrackerBundle bundle, ValidationErrorReporter reporter,
        Event event, ProgramStageInstance programStageInstance )
    {
        Objects.requireNonNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( bundle.getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );

        if ( bundle.getImportStrategy().isUpdate() )
        {
            List<String> errors = trackerAccessManager.canUpdate( bundle.getUser(), programStageInstance, false );
            if ( !errors.isEmpty() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1050 )
                    .addArg( bundle.getUser() )
                    .addArg( String.join( ",", errors ) ) );
            }

            if ( event.getStatus() != programStageInstance.getStatus()
                && EventStatus.COMPLETED == programStageInstance.getStatus()
                && (!bundle.getUser().isSuper() && !bundle.getUser().isAuthorized( "F_UNCOMPLETE_EVENT" )) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1083 )
                    .addArg( bundle.getUser() ) );
            }
        }

        if ( bundle.getImportStrategy().isDelete() )
        {
            List<String> errors = trackerAccessManager.canDelete( bundle.getUser(),
                programStageInstance, false );
            if ( !errors.isEmpty() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1050 )
                    .addArg( bundle.getUser() )
                    .addArg( String.join( ",", errors ) ) );
            }
        }
    }

    protected void validateCreateEvent( ValidationErrorReporter reporter, User actingUser, Event event,
        ProgramStageInstance programStageInstance, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, TrackedEntityInstance trackedEntityInstance, Program program )
    {
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );

        programStage = (programStage == null && program.isWithoutRegistration())
            ? program.getProgramStageByStage( 1 ) : programStage;

        programInstance = getProgramInstance( actingUser, programInstance, trackedEntityInstance, program );
        if ( programStageInstance != null )
        {
            programStage = programStageInstance.getProgramStage();
        }

        ProgramStageInstance newProgramStageInstance = new ProgramStageInstance( programInstance, programStage )
            .setOrganisationUnit( organisationUnit )
            .setStatus( event.getStatus() );

        List<String> errors = trackerAccessManager.canCreate( actingUser, newProgramStageInstance, false );
        if ( !errors.isEmpty() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1050 )
                .addArg( actingUser )
                .addArg( String.join( ",", errors ) ) );
        }
    }

    protected void validateCreateEnrollment( ValidationErrorReporter reporter, User actingUser, Program program,
        OrganisationUnit organisationUnit, TrackedEntityInstance trackedEntityInstance )
    {
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntityInstance, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        ProgramInstance programInstance = new ProgramInstance( program, trackedEntityInstance, organisationUnit );

        List<String> errors = trackerAccessManager.canCreate( actingUser, programInstance, false );
        if ( !errors.isEmpty() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( actingUser )
                .addArg( String.join( ",", errors ) ) );
        }
    }

    protected void validateUpdateAndDeleteEnrollment( TrackerBundle bundle, ValidationErrorReporter reporter,
        User actingUser, Enrollment enrollment )
    {
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );
        Objects.requireNonNull( enrollment, ENROLLMENT_CANT_BE_NULL );

        ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, enrollment.getEnrollment() );

        if ( programInstance == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1015 )
                .addArg( enrollment )
                .addArg( enrollment.getEnrollment() ) );
            return;
        }

        if ( bundle.getImportStrategy().isUpdate() )
        {
            List<String> errors = trackerAccessManager.canUpdate( actingUser, programInstance, false );
            if ( !errors.isEmpty() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1000 )
                    .addArg( actingUser )
                    .addArg( programInstance ) );
            }
        }

        if ( bundle.getImportStrategy().isDelete() )
        {
            List<String> errors = trackerAccessManager.canDelete( actingUser, programInstance, false );
            if ( !errors.isEmpty() )
            {
                reporter.addError( newReport( TrackerErrorCode.E1000 )
                    .addArg( actingUser )
                    .addArg( programInstance ) );
            }
        }
    }

}
