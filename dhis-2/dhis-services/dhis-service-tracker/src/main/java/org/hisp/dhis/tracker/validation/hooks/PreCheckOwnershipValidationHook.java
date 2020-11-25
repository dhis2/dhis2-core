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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1083;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1100;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1103;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.EVENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

import java.util.Optional;

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
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.ReferenceTrackerEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckOwnershipValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private final TrackerImportAccessManager trackerImportAccessManager;

    public PreCheckOwnershipValidationHook( TrackerImportAccessManager trackerImportAccessManager )
    {
        checkNotNull( trackerImportAccessManager );

        this.trackerImportAccessManager = trackerImportAccessManager;
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( trackedEntity );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( bundle.getUser(), USER_CANT_BE_NULL );
        checkNotNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );

        if ( strategy.isDelete() )
        {
            TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
            checkNotNull( tei, TrackerImporterAssertErrors.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

            if ( tei.getProgramInstances().stream().anyMatch( pi -> !pi.isDeleted() )
                && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
            {
                addError( reporter, E1100, bundle.getUser(), tei );
            }
        }

        // Check acting user is allowed to change/write existing tei type
        if ( strategy.isUpdateOrDelete() )
        {
            TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );
            TrackedEntityType trackedEntityType = tei.getTrackedEntityType();
            trackerImportAccessManager.checkTeiTypeWriteAccess( reporter, trackedEntityType );
        }

        TrackedEntityType trackedEntityType = context
            .getTrackedEntityType( trackedEntity.getTrackedEntityType() );
        trackerImportAccessManager.checkTeiTypeWriteAccess( reporter, trackedEntityType );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( enrollment );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        checkNotNull( enrollment.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        Program program = context.getProgram( enrollment.getProgram() );
        OrganisationUnit organisationUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );
        TrackedEntityInstance tei = context.getTrackedEntityInstance( enrollment.getTrackedEntity() );

        if ( tei == null && !context.getReference( enrollment.getTrackedEntity() ).isPresent() )
        {
            throw new NullPointerException( TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );
        }

        checkNotNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( strategy.isDelete() )
        {
            ProgramInstance pi = context.getProgramInstance( enrollment.getEnrollment() );

            checkNotNull( pi, PROGRAM_INSTANCE_CANT_BE_NULL );

            boolean hasNonDeletedEvents = pi.getProgramStageInstances().stream().anyMatch( psi -> !psi.isDeleted() );
            boolean hasNotCascadeDeleteAuthority = !user
                .isAuthorized( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() );

            addErrorIf( () -> hasNonDeletedEvents && hasNotCascadeDeleteAuthority, reporter, E1103, user, pi );
        }

        // Check acting user is allowed to change/write existing pi and program
        if ( strategy.isUpdateOrDelete() )
        {
            ProgramInstance programInstance = context.getProgramInstance( enrollment.getEnrollment() );
            trackerImportAccessManager
                .checkWriteEnrollmentAccess( reporter, programInstance.getProgram(), tei.getUid(), organisationUnit );
        }

        if ( tei != null )
        {
            trackerImportAccessManager.checkWriteEnrollmentAccess( reporter, program,
                tei.getUid(), tei.getOrganisationUnit() );// This orgUnit could not be in the Preheat because is part of
                                                          // an already persisted Entity
        }
        else
        {
            final Optional<ReferenceTrackerEntity> trackedEntity = context
                .getReference( enrollment.getTrackedEntity() );

            if ( trackedEntity.isPresent() )
            {
                // We need to retrieve the orgUnit from the Preheat getting the uid from the TEI
                // in the payload
                trackerImportAccessManager.checkWriteEnrollmentAccess( reporter, program,
                    trackedEntity.get().getUid(),
                    context.getOrganisationUnit( getOrgUnitUidFromTei( context,
                        trackedEntity.get().getUid() ) ) );
            }
            else
            {
                // TODO this should be caught by earlier validator
            }
        }
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( event );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        Program program = context.getProgram( event.getProgram() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );

        String teiUid = null;

        if ( programInstance == null )
        {
            Optional<ReferenceTrackerEntity> reference = context.getReference( event.getEnrollment() );
            if ( reference.isPresent() )
            {
                teiUid = reference.get().getParentUid();
            }
        }
        else
        {
            if ( programInstance.getEntityInstance() != null ) // TODO luciano: we should add a early check where
                                                               // validation fails if a pi has no TEI and program is
                                                               // with registration
            {
                teiUid = programInstance.getEntityInstance().getUid();
            }
        }
        CategoryOptionCombo categoryOptionCombo = context.getCategoryOptionCombo( event.getAttributeOptionCombo() );

        // Check acting user is allowed to change existing/write event
        if ( strategy.isUpdateOrDelete() )
        {
            validateUpdateAndDeleteEvent( reporter, event, context.getProgramStageInstance( event.getEvent() ),
                categoryOptionCombo,
                programStage,
                teiUid,
                organisationUnit );
        }

        validateCreateEvent( reporter, user,
            categoryOptionCombo,
            programStage,
            teiUid,
            organisationUnit,
            program, event.isCreatableInSearchScope() );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        // NOTHING TO DO HERE
    }

    protected void validateCreateEvent( ValidationErrorReporter reporter, User actingUser,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage, String teiUid,
        OrganisationUnit organisationUnit, Program program, boolean isCreatableInSearchScope )
    {
        checkNotNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        checkNotNull( actingUser, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        trackerImportAccessManager.checkEventWriteAccess( reporter, programStage, organisationUnit, categoryOptionCombo,
            teiUid, isCreatableInSearchScope ); // TODO: calculate correct isCreatableInSearchScope value
    }

    protected void validateUpdateAndDeleteEvent( ValidationErrorReporter reporter, Event event,
        ProgramStageInstance programStageInstance,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage,
        String teiUid, OrganisationUnit organisationUnit )
    {
        TrackerImportStrategy strategy = reporter.getValidationContext().getStrategy( event );
        User user = reporter.getValidationContext().getBundle().getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        trackerImportAccessManager.checkEventWriteAccess( reporter, programStage, organisationUnit, categoryOptionCombo,
            teiUid, programStageInstance.isCreatableInSearchScope() );

        if ( strategy.isUpdate()
            && EventStatus.COMPLETED == programStageInstance.getStatus()
            && event.getStatus() != programStageInstance.getStatus()
            && (!user.isSuper() && !user.isAuthorized( "F_UNCOMPLETE_EVENT" )) )
        {
            addError( reporter, E1083, user );
        }
    }

    private String getOrgUnitUidFromTei( TrackerImportValidationContext context, String teiUid )
    {

        final Optional<ReferenceTrackerEntity> reference = context.getReference( teiUid );
        if ( reference.isPresent() )
        {
            final Optional<TrackedEntity> tei = context.getBundle()
                .getTrackedEntity( teiUid );
            if ( tei.isPresent() )
            {
                return tei.get().getOrgUnit();
            }
        }
        return null;
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
