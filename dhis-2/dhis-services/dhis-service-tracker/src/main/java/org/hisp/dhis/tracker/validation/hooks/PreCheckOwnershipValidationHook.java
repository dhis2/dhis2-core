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

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.EVENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.USER_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckOwnershipValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public int getOrder()
    {
        return 5;
    }

    @Autowired
    private TrackerImportAccessManager trackerImportAccessManager;

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter,
        TrackedEntity trackedEntity )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( trackedEntity );
        TrackerBundle bundle = context.getBundle();

        Objects.requireNonNull( bundle.getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );

        if ( strategy.isDelete() )
        {
            TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );

            // This is checked in existence check, but lets be very double sure this is true for now.
            Objects.requireNonNull( tei, Constants.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

            Set<ProgramInstance> programInstances = tei.getProgramInstances().stream()
                .filter( pi -> !pi.isDeleted() )
                .collect( Collectors.toSet() );

            if ( !programInstances.isEmpty()
                && !bundle.getUser().isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1100 )
                    .addArg( bundle.getUser() )
                    .addArg( tei ) );
            }
        }

        if ( strategy.isUpdateOrDelete() )
        {
            TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
            TrackedEntityType trackedEntityType = tei.getTrackedEntityType();
            trackerImportAccessManager.checkTeiTypeWriteAccess( reporter, bundle.getUser(), trackedEntityType );
        }

        TrackedEntityType trackedEntityType = context
            .getTrackedEntityType( trackedEntity.getTrackedEntityType() );
        trackerImportAccessManager.checkTeiTypeWriteAccess( reporter, bundle.getUser(), trackedEntityType );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( enrollment );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        Objects.requireNonNull( enrollment.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        Program program = context.getProgram( enrollment.getProgram() );
        OrganisationUnit organisationUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );
        TrackedEntityInstance tei = context.getTrackedEntityInstance( enrollment.getTrackedEntity() );

        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        Objects.requireNonNull( tei, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        if ( strategy.isDelete() )
        {
            ProgramInstance programInstance = context.getProgramInstance( enrollment.getEnrollment() );

            Set<ProgramStageInstance> notDeletedProgramStageInstances = programInstance.getProgramStageInstances()
                .stream()
                .filter( psi -> !psi.isDeleted() )
                .collect( Collectors.toSet() );

            if ( !notDeletedProgramStageInstances.isEmpty()
                && !user.isAuthorized( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1103 )
                    .addArg( user )
                    .addArg( programInstance ) );
            }
        }

        if ( strategy.isUpdateOrDelete() )
        {
            ProgramInstance programInstance = context.getProgramInstance( enrollment.getEnrollment() );
            trackerImportAccessManager
                .checkWriteEnrollmentAccess( reporter, user, programInstance.getProgram(),
                    programInstance );
        }

        trackerImportAccessManager.checkWriteEnrollmentAccess( reporter, user, program,
            new ProgramInstance( program, tei, organisationUnit ) );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( event );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );

        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        Program program = context.getProgram( event.getProgram() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );

        if ( strategy.isUpdateOrDelete() )
        {
            validateUpdateAndDeleteEvent( reporter, event,
                context.getProgramStageInstance( event.getEvent() ) );
        }

        CategoryOptionCombo categoryOptionCombo = context
            .getCategoryOptionCombo( event.getAttributeOptionCombo() );

        validateCreateEvent( reporter, user,
            categoryOptionCombo,
            programStage,
            programInstance,
            organisationUnit,
            program );
    }

    protected void validateCreateEvent( ValidationErrorReporter reporter, User actingUser,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Program program )
    {
        Objects.requireNonNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        ProgramStageInstance newProgramStageInstance = new ProgramStageInstance( programInstance, programStage )
            .setOrganisationUnit( organisationUnit );
        newProgramStageInstance.setAttributeOptionCombo( categoryOptionCombo );

        trackerImportAccessManager.checkEventWriteAccess( reporter, actingUser, newProgramStageInstance );
    }

    protected void validateUpdateAndDeleteEvent( ValidationErrorReporter reporter,
        Event event, ProgramStageInstance programStageInstance )
    {
        User user = reporter.getValidationContext().getBundle().getUser();

        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );

        trackerImportAccessManager.checkEventWriteAccess( reporter, user, programStageInstance );

        // TODO: Should it be possible to delete a completed event, but not update? with current check above it is...
        if ( reporter.getValidationContext().getStrategy( event ).isUpdate()
            && EventStatus.COMPLETED == programStageInstance.getStatus()
            && event.getStatus() != programStageInstance.getStatus()
            && (!user.isSuper()
            && !user.isAuthorized( "F_UNCOMPLETE_EVENT" )) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1083 )
                .addArg( user ) );
        }
    }
}
