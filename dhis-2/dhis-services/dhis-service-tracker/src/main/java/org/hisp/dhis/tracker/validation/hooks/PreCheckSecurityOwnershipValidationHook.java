/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1083;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1100;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1103;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ENROLLMENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.EVENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_STAGE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_TYPE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PreCheckSecurityOwnershipValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @NonNull
    private final AclService aclService;

    @NonNull
    private final TrackerOwnershipManager ownershipAccessManager;

    @NonNull
    private final OrganisationUnitService organisationUnitService;

    private static final String ORG_UNIT_NO_USER_ASSIGNED = " has no organisation unit assigned, so we skip user validation";

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        TrackerImportStrategy strategy = reporter.getBundle().getStrategy( trackedEntity );
        TrackerBundle bundle = reporter.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );

        TrackedEntityType trackedEntityType = strategy.isUpdateOrDelete()
            ? reporter.getBundle().getTrackedEntityInstance( trackedEntity.getTrackedEntity() ).getTrackedEntityType()
            : reporter.getBundle().getPreheat()
                .getTrackedEntityType( trackedEntity.getTrackedEntityType() );

        OrganisationUnit organisationUnit = strategy.isUpdateOrDelete()
            ? reporter.getBundle().getTrackedEntityInstance( trackedEntity.getTrackedEntity() ).getOrganisationUnit()
            : reporter.getBundle().getPreheat().getOrganisationUnit( trackedEntity.getOrgUnit() );

        // If trackedEntity is newly created, or going to be deleted, capture
        // scope has to be checked
        if ( strategy.isCreate() || strategy.isDelete() )
        {
            checkOrgUnitInCaptureScope( reporter, trackedEntity,
                organisationUnit );
        }
        // if its to update trackedEntity, search scope has to be checked
        else
        {
            checkOrgUnitInSearchScope( reporter, trackedEntity,
                organisationUnit );
        }

        if ( strategy.isDelete() )
        {
            TrackedEntityInstance tei = reporter.getBundle()
                .getTrackedEntityInstance( trackedEntity.getTrackedEntity() );

            if ( tei.getProgramInstances().stream().anyMatch( pi -> !pi.isDeleted() )
                && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
            {
                reporter.addError( trackedEntity, E1100, user, tei );
            }
        }

        checkTeiTypeWriteAccess( reporter, trackedEntity, trackedEntityType );
    }

    private void checkTeiTypeWriteAccess( ValidationErrorReporter reporter, TrackedEntity trackedEntity,
        TrackedEntityType trackedEntityType )
    {
        TrackerBundle bundle = reporter.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( trackedEntityType, TRACKED_ENTITY_TYPE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, trackedEntityType ) )
        {
            reporter.addError( trackedEntity, TrackerErrorCode.E1001, user, trackedEntityType );
        }
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportStrategy strategy = reporter.getBundle().getStrategy( enrollment );
        TrackerBundle bundle = reporter.getBundle();
        TrackerPreheat preheat = bundle.getPreheat();
        User user = bundle.getUser();
        Program program = strategy.isUpdateOrDelete()
            ? reporter.getBundle().getProgramInstance( enrollment.getEnrollment() )
                .getProgram()
            : reporter.getBundle().getPreheat().getProgram( enrollment.getProgram() );
        OrganisationUnit ownerOrgUnit = getOwnerOrganisationUnit( preheat, enrollment.getTrackedEntity(),
            preheat.getProgram( enrollment.getProgram() ) );

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        checkEnrollmentOrgUnit( reporter, strategy, enrollment, program );

        if ( strategy.isDelete() )
        {
            boolean hasNonDeletedEvents = programInstanceHasEvents( preheat, enrollment.getEnrollment() );
            boolean hasNotCascadeDeleteAuthority = !user
                .isAuthorized( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() );

            if ( hasNonDeletedEvents && hasNotCascadeDeleteAuthority )
            {
                reporter.addError( enrollment, E1103, user, enrollment.getEnrollment() );
            }
        }

        checkWriteEnrollmentAccess( reporter, enrollment, program, ownerOrgUnit );
    }

    private OrganisationUnit getOwnerOrganisationUnit( TrackerPreheat preheat, String teiUid, Program program )
    {
        Map<String, TrackedEntityProgramOwnerOrgUnit> programOwner = preheat.getProgramOwner()
            .get( teiUid );
        if ( programOwner == null || programOwner.get( program.getUid() ) == null )
        {
            return null;
        }
        else
        {
            return programOwner.get( program.getUid() ).getOrganisationUnit();
        }
    }

    private boolean programInstanceHasEvents( TrackerPreheat preheat, String programInstanceUid )
    {
        return preheat.getProgramInstanceWithOneOrMoreNonDeletedEvent().contains( programInstanceUid );
    }

    private void checkEnrollmentOrgUnit( ValidationErrorReporter reporter,
        TrackerImportStrategy strategy, Enrollment enrollment, Program program )
    {
        OrganisationUnit enrollmentOrgUnit;

        if ( strategy.isUpdateOrDelete() )
        {
            enrollmentOrgUnit = reporter.getBundle().getProgramInstance( enrollment.getEnrollment() )
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
            checkNotNull( enrollment.getOrgUnit().getIdentifierOrAttributeValue(), ORGANISATION_UNIT_CANT_BE_NULL );
            enrollmentOrgUnit = reporter.getBundle().getPreheat().getOrganisationUnit( enrollment.getOrgUnit() );
        }

        // If enrollment is newly created, or going to be deleted, capture scope
        // has to be checked
        if ( program.isWithoutRegistration() || strategy.isCreate()
            || strategy.isDelete() )
        {
            checkOrgUnitInCaptureScope( reporter, enrollment, enrollmentOrgUnit );
        }
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        TrackerImportStrategy strategy = reporter.getBundle().getStrategy( event );
        TrackerPreheat preheat = bundle.getPreheat();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        ProgramStageInstance programStageInstance = reporter.getBundle().getProgramStageInstance( event.getEvent() );

        ProgramStage programStage = reporter.getBundle().getPreheat().getProgramStage( event.getProgramStage() );
        Program program = strategy.isUpdateOrDelete() ? programStageInstance.getProgramStage()
            .getProgram() : reporter.getBundle().getPreheat().getProgram( event.getProgram() );

        OrganisationUnit organisationUnit;

        if ( strategy.isUpdateOrDelete() )
        {
            organisationUnit = programStageInstance
                .getOrganisationUnit();
        }
        else
        {
            checkNotNull( event.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );
            organisationUnit = reporter.getBundle().getPreheat().getOrganisationUnit( event.getOrgUnit() );
        }

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
                checkOrgUnitInCaptureScope( reporter, event, organisationUnit );
            }
        }

        String teiUid = getTeiUidFromEvent( reporter.getBundle(), event, program );

        CategoryOptionCombo categoryOptionCombo = bundle.getPreheat()
            .getCategoryOptionCombo( event.getAttributeOptionCombo() );
        OrganisationUnit ownerOrgUnit = getOwnerOrganisationUnit( preheat, teiUid, program );
        // Check acting user is allowed to change existing/write event
        if ( strategy.isUpdateOrDelete() )
        {
            TrackedEntityInstance entityInstance = programStageInstance.getProgramInstance().getEntityInstance();
            validateUpdateAndDeleteEvent( reporter, event, programStageInstance,
                entityInstance == null ? null : entityInstance.getUid(), ownerOrgUnit );
        }
        else
        {
            validateCreateEvent( reporter, event, user,
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

    private void validateCreateEvent( ValidationErrorReporter reporter, Event event, User actingUser,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage, String teiUid,
        OrganisationUnit organisationUnit, OrganisationUnit ownerOrgUnit, Program program,
        boolean isCreatableInSearchScope )
    {
        checkNotNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        checkNotNull( actingUser, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        checkEventWriteAccess( reporter, event, programStage, organisationUnit, ownerOrgUnit,
            categoryOptionCombo,
            teiUid, isCreatableInSearchScope ); // TODO:
                                                // calculate
                                                // correct
        // isCreatableInSearchScope
        // value
    }

    private void validateUpdateAndDeleteEvent( ValidationErrorReporter reporter, Event event,
        ProgramStageInstance programStageInstance,
        String teiUid, OrganisationUnit ownerOrgUnit )
    {
        TrackerImportStrategy strategy = reporter.getBundle().getStrategy( event );
        User user = reporter.getBundle().getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        checkEventWriteAccess( reporter, event, programStageInstance.getProgramStage(),
            programStageInstance.getOrganisationUnit(), ownerOrgUnit,
            programStageInstance.getAttributeOptionCombo(),
            teiUid, programStageInstance.isCreatableInSearchScope() );

        if ( strategy.isUpdate()
            && EventStatus.COMPLETED == programStageInstance.getStatus()
            && event.getStatus() != programStageInstance.getStatus()
            && (!user.isSuper() && !user.isAuthorized( "F_UNCOMPLETE_EVENT" )) )
        {
            reporter.addError( event, E1083, user );
        }
    }

    private String getTeiUidFromEvent( TrackerBundle bundle, Event event, Program program )
    {
        if ( program.isWithoutRegistration() )
        {
            return null;
        }

        ProgramInstance programInstance = bundle.getProgramInstance( event.getEnrollment() );

        if ( programInstance == null )
        {
            return bundle
                .getEnrollment( event.getEnrollment() )
                .map( Enrollment::getTrackedEntity )
                .orElse( null );
        }
        else
        {
            return programInstance.getEntityInstance().getUid();
        }
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

    private void checkOrgUnitInCaptureScope( ValidationErrorReporter reporter, TrackerDto dto,
        OrganisationUnit orgUnit )
    {
        TrackerBundle bundle = reporter.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserHierarchyCached( user, orgUnit ) )
        {
            reporter.addError( dto, TrackerErrorCode.E1000, user, orgUnit );
        }
    }

    private void checkOrgUnitInSearchScope( ValidationErrorReporter reporter, TrackerDto dto,
        OrganisationUnit orgUnit )
    {
        TrackerBundle bundle = reporter.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserSearchHierarchyCached( user, orgUnit ) )
        {
            reporter.addError( dto, TrackerErrorCode.E1003, orgUnit, user );
        }
    }

    private void checkTeiTypeAndTeiProgramAccess( ValidationErrorReporter reporter, TrackerDto dto,
        User user,
        String trackedEntityInstance,
        OrganisationUnit ownerOrganisationUnit,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );
        checkNotNull( program.getTrackedEntityType(), TRACKED_ENTITY_TYPE_CANT_BE_NULL );
        checkNotNull( trackedEntityInstance, TRACKED_ENTITY_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
        {
            reporter.addError( dto, TrackerErrorCode.E1104, user, program, program.getTrackedEntityType() );
        }

        if ( ownerOrganisationUnit != null
            && !ownershipAccessManager.hasAccess( user, trackedEntityInstance, ownerOrganisationUnit,
                program ) )
        {
            reporter.addError( dto, TrackerErrorCode.E1102, user, trackedEntityInstance, program );
        }
    }

    private void checkWriteEnrollmentAccess( ValidationErrorReporter reporter, Enrollment enrollment, Program program,
        OrganisationUnit ownerOrgUnit )
    {
        TrackerBundle bundle = reporter.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        checkProgramWriteAccess( reporter, enrollment, user, program );

        if ( program.isRegistration() )
        {
            String trackedEntity = reporter.getBundle().getStrategy( enrollment ).isDelete()
                ? reporter.getBundle().getProgramInstance( enrollment.getEnrollment() ).getEntityInstance().getUid()
                : enrollment.getTrackedEntity();

            checkNotNull( program.getTrackedEntityType(), TRACKED_ENTITY_TYPE_CANT_BE_NULL );
            checkTeiTypeAndTeiProgramAccess( reporter, enrollment, user, trackedEntity,
                ownerOrgUnit, program );
        }
    }

    private void checkEventWriteAccess( ValidationErrorReporter reporter, Event event, ProgramStage programStage,
        OrganisationUnit eventOrgUnit, OrganisationUnit ownerOrgUnit,
        CategoryOptionCombo categoryOptionCombo,
        String trackedEntity, boolean isCreatableInSearchScope )
    {
        TrackerBundle bundle = reporter.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );
        checkNotNull( programStage.getProgram(), PROGRAM_CANT_BE_NULL );

        if ( reporter.getBundle().getStrategy( event ) != TrackerImportStrategy.UPDATE )
        {
            checkEventOrgUnitWriteAccess( reporter, event, eventOrgUnit, isCreatableInSearchScope, user );
        }

        if ( programStage.getProgram().isWithoutRegistration() )
        {
            checkProgramWriteAccess( reporter, event, user, programStage.getProgram() );
        }
        else
        {
            checkProgramStageWriteAccess( reporter, event, user, programStage );
            final Program program = programStage.getProgram();

            checkProgramReadAccess( reporter, event, user, program );

            checkTeiTypeAndTeiProgramAccess( reporter, event, user,
                trackedEntity,
                ownerOrgUnit,
                programStage.getProgram() );
        }

        if ( categoryOptionCombo != null )
        {
            checkWriteCategoryOptionComboAccess( reporter, event, categoryOptionCombo );
        }
    }

    private void checkEventOrgUnitWriteAccess( ValidationErrorReporter reporter, Event event,
        OrganisationUnit eventOrgUnit,
        boolean isCreatableInSearchScope, User user )
    {
        if ( eventOrgUnit == null )
        {
            log.warn( "ProgramStageInstance " + event.getUid()
                + ORG_UNIT_NO_USER_ASSIGNED );
        }
        else if ( isCreatableInSearchScope
            ? !organisationUnitService.isInUserSearchHierarchyCached( user, eventOrgUnit )
            : !organisationUnitService.isInUserHierarchyCached( user, eventOrgUnit ) )
        {
            reporter.addError( event, TrackerErrorCode.E1000, user, eventOrgUnit );
        }
    }

    private void checkProgramReadAccess( ValidationErrorReporter reporter, TrackerDto dto,
        User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program ) )
        {
            reporter.addError( dto, TrackerErrorCode.E1096, user, program );
        }
    }

    private void checkProgramStageWriteAccess( ValidationErrorReporter reporter, TrackerDto dto,
        User user,
        ProgramStage programStage )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, programStage ) )
        {
            reporter.addError( dto, TrackerErrorCode.E1095, user, programStage );
        }
    }

    private void checkProgramWriteAccess( ValidationErrorReporter reporter, TrackerDto dto,
        User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, program ) )
        {
            reporter.addError( dto, TrackerErrorCode.E1091, user, program );
        }
    }

    public void checkWriteCategoryOptionComboAccess( ValidationErrorReporter reporter, TrackerDto dto,
        CategoryOptionCombo categoryOptionCombo )
    {
        TrackerBundle bundle = reporter.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( categoryOptionCombo, TrackerImporterAssertErrors.CATEGORY_OPTION_COMBO_CANT_BE_NULL );

        for ( CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataWrite( user, categoryOption ) )
            {
                reporter.addError( dto, TrackerErrorCode.E1099, user, categoryOption );
            }
        }
    }

}
