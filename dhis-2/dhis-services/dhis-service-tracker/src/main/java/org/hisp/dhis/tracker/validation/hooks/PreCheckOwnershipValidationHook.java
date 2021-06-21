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
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.*;

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
        Program program = context.getProgram( enrollment.getProgram() );

        checkNotNull( user, USER_CANT_BE_NULL );

        if ( strategy.isDelete() )
        {
            ProgramInstance pi = context.getProgramInstance( enrollment.getEnrollment() );

            checkNotNull( pi, PROGRAM_INSTANCE_CANT_BE_NULL );

            boolean hasNonDeletedEvents = context.programInstanceHasEvents( pi.getUid() );
            boolean hasNotCascadeDeleteAuthority = !user
                .isAuthorized( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() );

            addErrorIf( () -> hasNonDeletedEvents && hasNotCascadeDeleteAuthority, reporter, E1103, user, pi );
        }

        trackerImportAccessManager.checkWriteEnrollmentAccess( reporter, program,
            enrollment.getTrackedEntity(), getOrgUnitFromTei( context, enrollment.getTrackedEntity() ) );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( event );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );

        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        Program program = context.getProgram( event.getProgram() );

        String teiUid = getTeiUidFromEvent( context, event, program );

        CategoryOptionCombo categoryOptionCombo = context.getCategoryOptionCombo( event.getAttributeOptionCombo() );

        // Check acting user is allowed to change existing/write event
        if ( strategy.isUpdateOrDelete() )
        {
            ProgramStageInstance programStageInstance = context.getProgramStageInstance( event.getEvent() );
            TrackedEntityInstance entityInstance = programStageInstance.getProgramInstance().getEntityInstance();
            validateUpdateAndDeleteEvent( reporter, event, programStageInstance,
                entityInstance == null ? null : entityInstance.getUid() );
        }
        else
        {
            validateCreateEvent( reporter, user,
                categoryOptionCombo,
                programStage,
                teiUid,
                organisationUnit,
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
        OrganisationUnit organisationUnit, Program program, boolean isCreatableInSearchScope )
    {
        checkNotNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        checkNotNull( actingUser, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        trackerImportAccessManager.checkEventWriteAccess( reporter, programStage, organisationUnit, categoryOptionCombo,
            teiUid, isCreatableInSearchScope ); // TODO: calculate correct
                                                // isCreatableInSearchScope
                                                // value
    }

    protected void validateUpdateAndDeleteEvent( ValidationErrorReporter reporter, Event event,
        ProgramStageInstance programStageInstance,
        String teiUid )
    {
        TrackerImportStrategy strategy = reporter.getValidationContext().getStrategy( event );
        User user = reporter.getValidationContext().getBundle().getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        trackerImportAccessManager.checkEventWriteAccess( reporter, programStageInstance.getProgramStage(),
            programStageInstance.getOrganisationUnit(), programStageInstance.getAttributeOptionCombo(), teiUid,
            programStageInstance.isCreatableInSearchScope() );

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
        ProgramInstance programInstance = context.getProgramInstance( event.getEnrollment() );

        if ( program.isWithoutRegistration() )
        {
            return null;
        }

        if ( programInstance == null )
        {
            Enrollment enrollment = context.getBundle().getEnrollment( event.getEnrollment() ).get();
            return enrollment.getTrackedEntity();
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
