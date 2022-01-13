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

import static com.google.api.client.util.Preconditions.checkNotNull;
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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

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
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.Error;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @author Ameen <ameen@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class PreCheckSecurityOwnershipValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @NonNull
    private final AclService aclService;

    @NonNull
    private final TrackerOwnershipManager ownershipAccessManager;

    @NonNull
    private final OrganisationUnitService organisationUnitService;

    @Override
    public void validateTrackedEntity( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackedEntity trackedEntity )
    {
        TrackerImportStrategy strategy = context.getStrategy( trackedEntity );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );
        checkNotNull( trackedEntity.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        // If trackedEntity is newly created, or going to be deleted, capture
        // scope has to be checked
        if ( strategy.isCreate() || strategy.isDelete() )
        {
            checkOrgUnitInCaptureScope( report, context, trackedEntity,
                context.getOrganisationUnit( trackedEntity.getOrgUnit() ) );
        }
        // if its to update trackedEntity, search scope has to be checked
        else
        {
            checkOrgUnitInSearchScope( report, context, trackedEntity,
                context.getOrganisationUnit( trackedEntity.getOrgUnit() ) );
        }

        if ( strategy.isDelete() )
        {
            TrackedEntityInstance tei = context.getTrackedEntityInstance( trackedEntity.getTrackedEntity() );

            if ( tei.getProgramInstances().stream().anyMatch( pi -> !pi.isDeleted() )
                && !user.isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
            {
                Error error = Error.builder()
                    .uid( trackedEntity.getUid() )
                    .trackerType( TrackerType.TRACKED_ENTITY )
                    .errorCode( E1100 )
                    .addArg( context.getBundle().getIdentifier(), user )
                    .addArg( context.getBundle().getIdentifier(), tei )
                    .build();
                report.addError( error );
            }
        }

        TrackedEntityType trackedEntityType = context
            .getTrackedEntityType( trackedEntity.getTrackedEntityType() );
        checkTeiTypeWriteAccess( report, context, trackedEntity.getUid(), trackedEntityType );
    }

    private void checkTeiTypeWriteAccess( TrackerValidationReport report, TrackerImportValidationContext context,
        String teUid,
        TrackedEntityType trackedEntityType )
    {
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( trackedEntityType, TRACKED_ENTITY_TYPE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, trackedEntityType ) )
        {
            Error error = Error.builder()
                .uid( teUid )
                .trackerType( TrackerType.TRACKED_ENTITY )
                .errorCode( TrackerErrorCode.E1001 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( context.getBundle().getIdentifier(), trackedEntityType )
                .build();
            report.addError( error );
        }
    }

    @Override
    public void validateEnrollment( TrackerValidationReport report, TrackerImportValidationContext context,
        Enrollment enrollment )
    {
        TrackerImportStrategy strategy = context.getStrategy( enrollment );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();
        Program program = context.getProgram( enrollment.getProgram() );
        OrganisationUnit ownerOrgUnit = context.getOwnerOrganisationUnit( enrollment.getTrackedEntity(),
            enrollment.getProgram() );

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        checkNotNull( enrollment.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        // If enrollment is newly created, or going to be deleted, capture scope
        // has to be checked
        if ( program.isWithoutRegistration() || strategy.isCreate() || strategy.isDelete() )
        {
            checkOrgUnitInCaptureScope( report, context, enrollment,
                context.getOrganisationUnit( enrollment.getOrgUnit() ) );
        }

        if ( strategy.isDelete() )
        {
            boolean hasNonDeletedEvents = context.programInstanceHasEvents( enrollment.getEnrollment() );
            boolean hasNotCascadeDeleteAuthority = !user
                .isAuthorized( Authorities.F_ENROLLMENT_CASCADE_DELETE.getAuthority() );

            if ( hasNonDeletedEvents && hasNotCascadeDeleteAuthority )
            {
                Error error = Error.builder()
                    .uid( enrollment.getUid() )
                    .trackerType( TrackerType.ENROLLMENT )
                    .errorCode( E1103 )
                    .addArg( context.getBundle().getIdentifier(), user )
                    .addArg( enrollment.getEnrollment() )
                    .build();
                report.addError( error );
            }
        }

        checkWriteEnrollmentAccess( report, context, enrollment, program, enrollment.getTrackedEntity(),
            ownerOrgUnit );
    }

    @Override
    public void validateEvent( TrackerValidationReport report, TrackerImportValidationContext context, Event event )
    {
        TrackerImportStrategy strategy = context.getStrategy( event );
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );
        checkNotNull( event.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        Program program = context.getProgram( event.getProgram() );

        // If event is newly created, or going to be deleted, capture scope
        // has to be checked
        if ( program.isWithoutRegistration() || strategy.isCreate() || strategy.isDelete() )
        {
            checkOrgUnitInCaptureScope( report, context, event, organisationUnit );
        }

        String teiUid = getTeiUidFromEvent( context, event, program );

        CategoryOptionCombo categoryOptionCombo = context.getCategoryOptionCombo( event.getAttributeOptionCombo() );
        OrganisationUnit ownerOrgUnit = context.getOwnerOrganisationUnit( teiUid, program.getUid() );
        // Check acting user is allowed to change existing/write event
        if ( strategy.isUpdateOrDelete() )
        {
            ProgramStageInstance programStageInstance = context.getProgramStageInstance( event.getEvent() );
            TrackedEntityInstance entityInstance = programStageInstance.getProgramInstance().getEntityInstance();
            validateUpdateAndDeleteEvent( report, context, event, programStageInstance,
                entityInstance == null ? null : entityInstance.getUid(), ownerOrgUnit );
        }
        else
        {

            validateCreateEvent( report, context, event, user,
                categoryOptionCombo,
                programStage,
                teiUid,
                organisationUnit,
                ownerOrgUnit,
                program, event.isCreatableInSearchScope() );
        }
    }

    @Override
    public void validateRelationship( TrackerValidationReport report, TrackerImportValidationContext context,
        Relationship relationship )
    {
        // NOTHING TO DO HERE
    }

    private void validateCreateEvent( TrackerValidationReport report, TrackerImportValidationContext context,
        Event event, User actingUser,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage, String teiUid,
        OrganisationUnit organisationUnit, OrganisationUnit ownerOrgUnit, Program program,
        boolean isCreatableInSearchScope )
    {
        checkNotNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        checkNotNull( actingUser, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        checkEventWriteAccess( report, context, event, programStage, organisationUnit, ownerOrgUnit,
            categoryOptionCombo,
            teiUid, isCreatableInSearchScope ); // TODO: calculate correct
        // isCreatableInSearchScope
        // value
    }

    private void validateUpdateAndDeleteEvent( TrackerValidationReport report, TrackerImportValidationContext context,
        Event event,
        ProgramStageInstance programStageInstance,
        String teiUid, OrganisationUnit ownerOrgUnit )
    {
        TrackerImportStrategy strategy = context.getStrategy( event );
        User user = context.getBundle().getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        checkNotNull( event, EVENT_CANT_BE_NULL );

        checkEventWriteAccess( report, context, event, programStageInstance.getProgramStage(),
            programStageInstance.getOrganisationUnit(), ownerOrgUnit,
            programStageInstance.getAttributeOptionCombo(),
            teiUid, programStageInstance.isCreatableInSearchScope() );

        if ( strategy.isUpdate()
            && EventStatus.COMPLETED == programStageInstance.getStatus()
            && event.getStatus() != programStageInstance.getStatus()
            && (!user.isSuper() && !user.isAuthorized( "F_UNCOMPLETE_EVENT" )) )
        {
            Error error = Error.builder()
                .uid( event.getUid() )
                .trackerType( TrackerType.EVENT )
                .errorCode( E1083 )
                .addArg( context.getBundle().getIdentifier(), user )
                .build();
            report.addError( error );
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

    private void checkOrgUnitInCaptureScope( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackerDto dto,
        OrganisationUnit orgUnit )
    {
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserHierarchyCached( user, orgUnit ) )
        {
            Error error = Error.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( TrackerErrorCode.E1000 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( context.getBundle().getIdentifier(), orgUnit )
                .build();
            report.addError( error );
        }
    }

    private void checkOrgUnitInSearchScope( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackerDto dto,
        OrganisationUnit orgUnit )
    {
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserSearchHierarchyCached( user, orgUnit ) )
        {
            Error error = Error.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( TrackerErrorCode.E1003 )
                .addArg( context.getBundle().getIdentifier(), orgUnit )
                .addArg( context.getBundle().getIdentifier(), user )
                .build();
            report.addError( error );
        }
    }

    private void checkTeiTypeAndTeiProgramAccess( TrackerValidationReport report,
        TrackerImportValidationContext context, TrackerDto dto,
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
            Error error = Error.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( TrackerErrorCode.E1104 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( context.getBundle().getIdentifier(), program )
                .addArg( context.getBundle().getIdentifier(), program.getTrackedEntityType() )
                .build();
            report.addError( error );
        }

        if ( ownerOrganisationUnit != null
            && !ownershipAccessManager.hasAccess( user, trackedEntityInstance, ownerOrganisationUnit,
                program ) )
        {
            Error error = Error.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( TrackerErrorCode.E1102 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( trackedEntityInstance )
                .addArg( context.getBundle().getIdentifier(), program )
                .build();
            report.addError( error );
        }
    }

    private void checkWriteEnrollmentAccess( TrackerValidationReport report, TrackerImportValidationContext context,
        Enrollment enrollment, Program program,
        String trackedEntity, OrganisationUnit ownerOrgUnit )
    {
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        checkProgramWriteAccess( report, context, enrollment, user, program );

        if ( program.isRegistration() )
        {
            checkNotNull( program.getTrackedEntityType(), TRACKED_ENTITY_TYPE_CANT_BE_NULL );
            checkTeiTypeAndTeiProgramAccess( report, context, enrollment, user, trackedEntity,
                ownerOrgUnit, program );
        }
    }

    private void checkEventWriteAccess( TrackerValidationReport report, TrackerImportValidationContext context,
        Event event, ProgramStage programStage,
        OrganisationUnit eventOrgUnit, OrganisationUnit ownerOrgUnit,
        CategoryOptionCombo categoryOptionCombo,
        String trackedEntity, boolean isCreatableInSearchScope )
    {
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );
        checkNotNull( programStage.getProgram(), PROGRAM_CANT_BE_NULL );
        checkNotNull( eventOrgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( isCreatableInSearchScope ? !organisationUnitService.isInUserSearchHierarchyCached( user, eventOrgUnit )
            : !organisationUnitService.isInUserHierarchyCached( user, eventOrgUnit ) )
        {

            Error error = Error.builder()
                .uid( event.getUid() )
                .trackerType( TrackerType.EVENT )
                .errorCode( TrackerErrorCode.E1000 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( context.getBundle().getIdentifier(), eventOrgUnit )
                .build();
            report.addError( error );
        }

        if ( programStage.getProgram().isWithoutRegistration() )
        {
            checkProgramWriteAccess( report, context, event, user, programStage.getProgram() );
        }
        else
        {
            checkProgramStageWriteAccess( report, context, event, user, programStage );
            final Program program = programStage.getProgram();

            checkProgramReadAccess( report, context, event, user, program );

            checkTeiTypeAndTeiProgramAccess( report, context, event, user,
                trackedEntity,
                ownerOrgUnit,
                programStage.getProgram() );
        }

        if ( categoryOptionCombo != null )
        {
            checkWriteCategoryOptionComboAccess( report, context, event, categoryOptionCombo );
        }
    }

    private void checkProgramReadAccess( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackerDto dto,
        User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program ) )
        {
            Error error = Error.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( TrackerErrorCode.E1096 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( context.getBundle().getIdentifier(), program )
                .build();
            report.addError( error );
        }
    }

    private void checkProgramStageWriteAccess( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackerDto dto,
        User user,
        ProgramStage programStage )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, programStage ) )
        {
            Error error = Error.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( TrackerErrorCode.E1095 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( context.getBundle().getIdentifier(), programStage )
                .build();
            report.addError( error );
        }
    }

    private void checkProgramWriteAccess( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackerDto dto,
        User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, program ) )
        {
            Error error = Error.builder()
                .uid( dto.getUid() )
                .trackerType( dto.getTrackerType() )
                .errorCode( TrackerErrorCode.E1091 )
                .addArg( context.getBundle().getIdentifier(), user )
                .addArg( context.getBundle().getIdentifier(), program )
                .build();
            report.addError( error );
        }
    }

    public void checkWriteCategoryOptionComboAccess( TrackerValidationReport report,
        TrackerImportValidationContext context, TrackerDto dto,
        CategoryOptionCombo categoryOptionCombo )
    {
        TrackerBundle bundle = context.getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( categoryOptionCombo, TrackerImporterAssertErrors.CATEGORY_OPTION_COMBO_CANT_BE_NULL );

        for ( CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataWrite( user, categoryOption ) )
            {
                Error error = Error.builder()
                    .uid( dto.getUid() )
                    .trackerType( dto.getTrackerType() )
                    .errorCode( TrackerErrorCode.E1099 )
                    .addArg( context.getBundle().getIdentifier(), user )
                    .addArg( context.getBundle().getIdentifier(), categoryOption )
                    .build();
                report.addError( error );
            }
        }
    }

}
