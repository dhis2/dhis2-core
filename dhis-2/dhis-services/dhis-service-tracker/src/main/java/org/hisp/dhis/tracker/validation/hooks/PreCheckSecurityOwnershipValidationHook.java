/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.validation.hooks;

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
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

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
 * @author Ameen <ameen@dhis2.org>
 */
@Component
@Slf4j
public class PreCheckSecurityOwnershipValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private final TrackerImportAccessManager trackerImportAccessManager;

    private static final String ORG_UNIT_NO_USER_ASSIGNED = " has no organisation unit assigned, so we skip user validation";

    public PreCheckSecurityOwnershipValidationHook( TrackerImportAccessManager trackerImportAccessManager )
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
        checkNotNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );

        TrackedEntityType trackedEntityType = strategy.isUpdateOrDelete()
            ? context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() ).getTrackedEntityType()
            : context
                .getTrackedEntityType( trackedEntity.getTrackedEntityType() );

        OrganisationUnit organisationUnit = strategy.isUpdateOrDelete()
            ? context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() ).getOrganisationUnit()
            : context.getOrganisationUnit( trackedEntity.getOrgUnit() );

        // If trackedEntity is newly created, or going to be deleted, capture
        // scope has to be checked
        if ( strategy.isCreate() || strategy.isDelete() )
        {
            trackerImportAccessManager.checkOrgUnitInCaptureScope( reporter,
                organisationUnit );
        }
        // if its to update trackedEntity, search scope has to be checked
        else
        {
            trackerImportAccessManager.checkOrgUnitInSearchScope( reporter,
                organisationUnit );
        }

        if ( strategy.isDelete() )
        {
            TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );

            if ( tei.getProgramInstances().stream().anyMatch( pi -> !pi.isDeleted() )
                && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
            {
                addError( reporter, E1100, user, tei );
            }
        }

        trackerImportAccessManager.checkTeiTypeWriteAccess( reporter, trackedEntityType );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( enrollment );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();
        Program program = strategy.isUpdateOrDelete() ? context.getProgramInstance( enrollment.getEnrollment() )
            .getProgram() : context.getProgram( enrollment.getProgram() );
        OrganisationUnit ownerOrgUnit = context.getOwnerOrganisationUnit( enrollment.getTrackedEntity(),
            enrollment.getProgram() );

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        checkEnrollmentOrgUnit( reporter, context, strategy, enrollment, program );

        if ( strategy.isDelete() )
        {
            boolean hasNonDeletedEvents = context.programInstanceHasEvents( enrollment.getEnrollment() );
            boolean hasNotCascadeDeleteAuthority = !user
                .isAuthorized( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() );

            addErrorIf( () -> hasNonDeletedEvents && hasNotCascadeDeleteAuthority, reporter, E1103, user,
                enrollment.getEnrollment() );
        }

        String trackedEntity = context.getStrategy( enrollment ).isDelete()
            ? context.getProgramInstance( enrollment.getEnrollment() ).getEntityInstance().getUid()
            : enrollment.getTrackedEntity();

        trackerImportAccessManager.checkWriteEnrollmentAccess( reporter, program,
            trackedEntity, getOrgUnitFromTei( context, trackedEntity ), ownerOrgUnit );
    }

    private void checkEnrollmentOrgUnit( ValidationErrorReporter reporter, TrackerImportValidationContext context,
        TrackerImportStrategy strategy, Enrollment enrollment,
        Program program )
    {
        OrganisationUnit enrollmentOrgUnit;

        if ( strategy.isUpdateOrDelete() )
        {
            enrollmentOrgUnit = context.getProgramInstance( enrollment.getEnrollment() )
                .getOrganisationUnit();

            if ( enrollmentOrgUnit == null )
            {
                log.warn( "ProgramInstance " + enrollment.getEnrollment()
                    + ORG_UNIT_NO_USER_ASSIGNED );
                return;
            }
        }
        else
        {
            checkNotNull( enrollment.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );
            enrollmentOrgUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );
        }

        // If enrollment is newly created, or going to be deleted, capture scope
        // has to be checked
        if ( program.isWithoutRegistration() || strategy.isCreate() || strategy.isDelete() )
        {
            trackerImportAccessManager
                .checkOrgUnitInCaptureScope( reporter, enrollmentOrgUnit );
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
        checkNotNull( event.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        ProgramStageInstance programStageInstance = context.getProgramStageInstance( event.getEvent() );

        OrganisationUnit organisationUnit;

        if ( strategy.isUpdateOrDelete() )
        {
            organisationUnit = programStageInstance
                .getOrganisationUnit();
        }
        else
        {
            checkNotNull( event.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );
            organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        }

        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        Program program = strategy.isUpdateOrDelete() ? programStage
            .getProgram() : context.getProgram( event.getProgram() );

        // If event is newly created, or going to be deleted, capture scope
        // has to be checked
        if ( program.isWithoutRegistration() || strategy.isCreate() || strategy.isDelete() )
        {
            if ( organisationUnit == null )
            {
                log.warn( "ProgramStageInstance " + event.getEvent()
                    + ORG_UNIT_NO_USER_ASSIGNED );
            }
            else
            {
                trackerImportAccessManager
                    .checkOrgUnitInCaptureScope( reporter, organisationUnit );
            }
        }

        String teiUid = getTeiUidFromEvent( context, event, program );

        CategoryOptionCombo categoryOptionCombo = bundle.getPreheat()
            .getCategoryOptionCombo( event.getAttributeOptionCombo() );
        OrganisationUnit ownerOrgUnit = context.getOwnerOrganisationUnit( teiUid, program.getUid() );
        // Check acting user is allowed to change existing/write event
        if ( strategy.isUpdateOrDelete() )
        {
            TrackedEntityInstance entityInstance = programStageInstance.getProgramInstance().getEntityInstance();
            validateUpdateAndDeleteEvent( reporter, event, programStageInstance,
                entityInstance == null ? null : entityInstance.getUid(), ownerOrgUnit );
        }
        else
        {
            validateCreateEvent( reporter, user,
                categoryOptionCombo,
                programStage,
                teiUid,
                organisationUnit,
                ownerOrgUnit,
                program, event.isCreatableInSearchScope() );
        }
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        // NOTHING TO DO HERE
    }

    protected void validateCreateEvent( ValidationErrorReporter reporter, User actingUser,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage, String teiUid,
        OrganisationUnit organisationUnit, OrganisationUnit ownerOrgUnit, Program program,
        boolean isCreatableInSearchScope )
    {
        checkNotNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        checkNotNull( actingUser, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        trackerImportAccessManager.checkEventWriteAccess( reporter, programStage, organisationUnit, ownerOrgUnit,
            categoryOptionCombo,
            teiUid, isCreatableInSearchScope ); // TODO: calculate correct
                                                // isCreatableInSearchScope
                                                // value
    }

    protected void validateUpdateAndDeleteEvent( ValidationErrorReporter reporter, Event event,
        ProgramStageInstance programStageInstance,
        String teiUid, OrganisationUnit ownerOrgUnit )
    {
        TrackerImportStrategy strategy = reporter.getValidationContext().getStrategy( event );
        User user = reporter.getValidationContext().getBundle().getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        trackerImportAccessManager.checkEventWriteAccess( reporter, programStageInstance.getProgramStage(),
            programStageInstance.getOrganisationUnit(), ownerOrgUnit,
            programStageInstance.getAttributeOptionCombo(),
            teiUid, programStageInstance.isCreatableInSearchScope() );

        if ( strategy.isUpdate()
            && EventStatus.COMPLETED == programStageInstance.getStatus()
            && event.getStatus() != programStageInstance.getStatus()
            && (!user.isSuper() && !user.isAuthorized( "F_UNCOMPLETE_EVENT" )) )
        {
            addError( reporter, E1083, user );
        }
    }

    private String getTeiUidFromEvent( TrackerImportValidationContext context, Event event, Program program )
    {
        if ( program.isWithoutRegistration() )
        {
            return null;
        }

        ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );

        if ( programInstance == null )
        {
            return context.getBundle()
                .getEnrollment( event.getEnrollment() )
                .map( Enrollment::getTrackedEntity )
                .orElse( null );
        }
        else
        {
            return programInstance.getEntityInstance().getUid();
        }
    }

    private OrganisationUnit getOrgUnitFromTei( TrackerImportValidationContext context, String teiUid )
    {
        TrackedEntityInstance trackedEntityInstance = context.getTrackedEntityInstance( teiUid );
        if ( trackedEntityInstance != null )
        {
            return trackedEntityInstance.getOrganisationUnit();
        }

        final Optional<ReferenceTrackerEntity> reference = context.getReference( teiUid );
        if ( reference.isPresent() )
        {
            final Optional<TrackedEntity> tei = context.getBundle()
                .getTrackedEntity( teiUid );
            if ( tei.isPresent() )
            {
                return context.getOrganisationUnit( tei.get().getOrgUnit() );
            }
        }
        return null;
    }

    @Override
    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return true;
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
