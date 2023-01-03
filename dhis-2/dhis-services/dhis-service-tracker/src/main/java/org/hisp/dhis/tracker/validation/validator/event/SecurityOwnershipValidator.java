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
package org.hisp.dhis.tracker.validation.validator.event;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1083;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.EVENT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.PROGRAM_STAGE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.TRACKED_ENTITY_TYPE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

import java.util.Map;

import javax.annotation.Nonnull;

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
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerOrgUnit;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.hisp.dhis.tracker.validation.Validator;
import org.hisp.dhis.tracker.validation.validator.TrackerImporterAssertErrors;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component( "org.hisp.dhis.tracker.validation.validator.event.SecurityOwnershipValidator" )
@RequiredArgsConstructor
@Slf4j
class SecurityOwnershipValidator
    implements Validator<Event>
{
    @Nonnull
    private final AclService aclService;

    @Nonnull
    private final TrackerOwnershipManager ownershipAccessManager;

    @Nonnull
    private final OrganisationUnitService organisationUnitService;

    private static final String ORG_UNIT_NO_USER_ASSIGNED = " has no organisation unit assigned, so we skip user validation";

    @Override
    public void validate( Reporter reporter, TrackerBundle bundle, Event event )
    {
        TrackerImportStrategy strategy = bundle.getStrategy( event );
        TrackerPreheat preheat = bundle.getPreheat();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        ProgramStageInstance programStageInstance = bundle.getPreheat().getEvent( event.getEvent() );

        ProgramStage programStage = bundle.getPreheat().getProgramStage( event.getProgramStage() );
        Program program = strategy.isUpdateOrDelete() ? programStageInstance.getProgramStage()
            .getProgram() : bundle.getPreheat().getProgram( event.getProgram() );

        OrganisationUnit organisationUnit;

        if ( strategy.isUpdateOrDelete() )
        {
            organisationUnit = programStageInstance
                .getOrganisationUnit();
        }
        else
        {
            checkNotNull( event.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );
            organisationUnit = bundle.getPreheat().getOrganisationUnit( event.getOrgUnit() );
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
                checkOrgUnitInCaptureScope( reporter, bundle, event, organisationUnit );
            }
        }

        String teiUid = getTeiUidFromEvent( bundle, event, program );

        CategoryOptionCombo categoryOptionCombo = bundle.getPreheat()
            .getCategoryOptionCombo( event.getAttributeOptionCombo() );
        OrganisationUnit ownerOrgUnit = getOwnerOrganisationUnit( preheat, teiUid, program );
        // Check acting user is allowed to change existing/write event
        if ( strategy.isUpdateOrDelete() )
        {
            TrackedEntityInstance entityInstance = programStageInstance.getProgramInstance().getEntityInstance();
            validateUpdateAndDeleteEvent( reporter, bundle, event, programStageInstance,
                entityInstance == null ? null : entityInstance.getUid(), ownerOrgUnit );
        }
        else
        {
            validateCreateEvent( reporter, bundle, event, user,
                categoryOptionCombo,
                programStage,
                teiUid,
                organisationUnit,
                ownerOrgUnit,
                program, event.isCreatableInSearchScope() );
        }
    }

    private void validateCreateEvent( Reporter reporter, TrackerBundle bundle, Event event,
        User actingUser,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage, String teiUid,
        OrganisationUnit organisationUnit, OrganisationUnit ownerOrgUnit, Program program,
        boolean isCreatableInSearchScope )
    {
        checkNotNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        checkNotNull( actingUser, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        checkEventWriteAccess( reporter, bundle, event, programStage, organisationUnit, ownerOrgUnit,
            categoryOptionCombo,
            // TODO: Calculate correct `isCreateableInSearchScope` value
            teiUid, isCreatableInSearchScope );
    }

    private void validateUpdateAndDeleteEvent( Reporter reporter, TrackerBundle bundle, Event event,
        ProgramStageInstance programStageInstance,
        String teiUid, OrganisationUnit ownerOrgUnit )
    {
        TrackerImportStrategy strategy = bundle.getStrategy( event );
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        checkEventWriteAccess( reporter, bundle, event, programStageInstance.getProgramStage(),
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

        ProgramInstance programInstance = bundle.getPreheat().getEnrollment( event.getEnrollment() );

        if ( programInstance == null )
        {
            return bundle
                .findEnrollmentByUid( event.getEnrollment() )
                .map( Enrollment::getTrackedEntity )
                .orElse( null );
        }
        else
        {
            return programInstance.getEntityInstance().getUid();
        }
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

    @Override
    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return true;
    }

    private void checkOrgUnitInCaptureScope( Reporter reporter, TrackerBundle bundle, TrackerDto dto,
        OrganisationUnit orgUnit )
    {
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserHierarchyCached( user, orgUnit ) )
        {
            reporter.addError( dto, ValidationCode.E1000, user, orgUnit );
        }
    }

    private void checkTeiTypeAndTeiProgramAccess( Reporter reporter, TrackerDto dto,
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
            reporter.addError( dto, ValidationCode.E1104, user, program, program.getTrackedEntityType() );
        }

        if ( ownerOrganisationUnit != null
            && !ownershipAccessManager.hasAccess( user, trackedEntityInstance, ownerOrganisationUnit,
                program ) )
        {
            reporter.addError( dto, ValidationCode.E1102, user, trackedEntityInstance, program );
        }
    }

    private void checkEventWriteAccess( Reporter reporter, TrackerBundle bundle, Event event,
        ProgramStage programStage,
        OrganisationUnit eventOrgUnit, OrganisationUnit ownerOrgUnit,
        CategoryOptionCombo categoryOptionCombo,
        String trackedEntity, boolean isCreatableInSearchScope )
    {
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );
        checkNotNull( programStage.getProgram(), PROGRAM_CANT_BE_NULL );

        if ( bundle.getStrategy( event ) != TrackerImportStrategy.UPDATE )
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
            checkWriteCategoryOptionComboAccess( reporter, bundle.getUser(), event, categoryOptionCombo );
        }
    }

    private void checkEventOrgUnitWriteAccess( Reporter reporter, Event event,
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
            reporter.addError( event, ValidationCode.E1000, user, eventOrgUnit );
        }
    }

    private void checkProgramReadAccess( Reporter reporter, TrackerDto dto,
        User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program ) )
        {
            reporter.addError( dto, ValidationCode.E1096, user, program );
        }
    }

    private void checkProgramStageWriteAccess( Reporter reporter, TrackerDto dto,
        User user,
        ProgramStage programStage )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, programStage ) )
        {
            reporter.addError( dto, ValidationCode.E1095, user, programStage );
        }
    }

    private void checkProgramWriteAccess( Reporter reporter, TrackerDto dto,
        User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, program ) )
        {
            reporter.addError( dto, ValidationCode.E1091, user, program );
        }
    }

    public void checkWriteCategoryOptionComboAccess( Reporter reporter, User user, TrackerDto dto,
        CategoryOptionCombo categoryOptionCombo )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( categoryOptionCombo, TrackerImporterAssertErrors.CATEGORY_OPTION_COMBO_CANT_BE_NULL );

        for ( CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataWrite( user, categoryOption ) )
            {
                reporter.addError( dto, ValidationCode.E1099, user, categoryOption );
            }
        }
    }

}
