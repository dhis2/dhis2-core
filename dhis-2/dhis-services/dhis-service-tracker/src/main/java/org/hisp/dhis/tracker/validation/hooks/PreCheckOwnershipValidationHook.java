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
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
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
    extends AbstractPreCheckValidationHook
{
    @Override
    public int getOrder()
    {
        return 4;
    }

    @Autowired
    private TrackerImportAccessManager trackerImportAccessManager;

    @Override
    public void validateTrackedEntities( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        Objects.requireNonNull( trackedEntity, TRACKED_ENTITY_CANT_BE_NULL );

        TrackedEntityInstance trackedEntityInstance = PreheatHelper.getTei( bundle, trackedEntity.getTrackedEntity() );

        if ( trackedEntityInstance != null && bundle.getImportStrategy().isDelete() )
        {
            //TODO: This need clarification and follow up. +Documentation
            Objects.requireNonNull( bundle.getUser(), Constants.USER_CANT_BE_NULL );
            Objects.requireNonNull( trackedEntityInstance, Constants.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

            Set<ProgramInstance> programInstances = trackedEntityInstance.getProgramInstances().stream()
                .filter( pi -> !pi.isDeleted() )
                .collect( Collectors.toSet() );

            if ( !programInstances.isEmpty()
                && !bundle.getUser().isAuthorized( Authorities.F_TEI_CASCADE_DELETE.getAuthority() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1100 )
                    .addArg( bundle.getUser() )
                    .addArg( trackedEntityInstance ) );
            }
        }
    }

    @Override
    public void validateEnrollments( ValidationErrorReporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        Objects.requireNonNull( enrollment, ENROLLMENT_CANT_BE_NULL );
        Objects.requireNonNull( enrollment.getOrgUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, enrollment.getOrgUnit() );
        TrackedEntityInstance tei = PreheatHelper.getTei( bundle, enrollment.getTrackedEntity() );

        Objects.requireNonNull( bundle.getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        Objects.requireNonNull( tei, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );

        ProgramInstance programInstance = new ProgramInstance( program, tei, organisationUnit );

        trackerImportAccessManager.checkWriteEnrollmentAccess( reporter, bundle.getUser(), program, programInstance );
    }

    @Override
    public void validateEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );

        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, event.getOrgUnit() );
        Program program = PreheatHelper.getProgram( bundle, event.getProgram() );
        ProgramStageInstance programStageInstance = PreheatHelper.getProgramStageInstance( bundle, event.getEvent() );
        ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );
        ProgramInstance programInstance = PreheatHelper.getProgramInstance( bundle, event.getEnrollment() );

        CategoryOptionCombo categoryOptionCombo = null;

        // TODO: how do we do this? i.e. how/where do we look up the CategoryOptionCombo, is they always in default map?
        categoryOptionCombo = (CategoryOptionCombo) bundle.getPreheat().getDefaults().get( CategoryOptionCombo.class );
        String attributeOptionCombo = event.getAttributeOptionCombo();
        // TODO: What about         String attributeCategoryOptions = event.getAttributeCategoryOptions(); ?
        // TODO: Fix this hack?

        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );
        Objects.requireNonNull( programStage, Constants.PROGRAM_STAGE_CANT_BE_NULL );

        if ( bundle.getImportStrategy().isCreate() )
        {
            validateCreateEvent( reporter, bundle.getUser(), categoryOptionCombo, programStage, programInstance,
                organisationUnit,
                program );
        }
        else
        {
            validateUpdateAndDeleteEvent( bundle, reporter, event, programStageInstance );
        }
    }

    protected void validateCreateEvent( ValidationErrorReporter reporter, User actingUser,
        CategoryOptionCombo categoryOptionCombo, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Program program )
    {
        Objects.requireNonNull( actingUser, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );

        boolean noProgramStageAndProgramIsWithoutReg = programStage == null && program.isWithoutRegistration();

        programStage = noProgramStageAndProgramIsWithoutReg ? program.getProgramStageByStage( 1 ) : programStage;

        ProgramStageInstance newProgramStageInstance = new ProgramStageInstance( programInstance, programStage )
            .setOrganisationUnit( organisationUnit );
        newProgramStageInstance.setAttributeOptionCombo( categoryOptionCombo );

        trackerImportAccessManager.checkEventWriteAccess( reporter, actingUser, newProgramStageInstance );
    }

    protected void validateUpdateAndDeleteEvent( TrackerBundle bundle, ValidationErrorReporter reporter,
        Event event, ProgramStageInstance programStageInstance )
    {
        Objects.requireNonNull( programStageInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( bundle.getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( event, EVENT_CANT_BE_NULL );

        trackerImportAccessManager.checkEventWriteAccess( reporter, bundle.getUser(), programStageInstance );

        // TODO: Should it be possible to delete a completed event, but not update? with current check above it is...
        if ( bundle.getImportStrategy().isUpdate() )
        {
            if ( event.getStatus() != programStageInstance.getStatus()
                && EventStatus.COMPLETED == programStageInstance.getStatus()
                && (!bundle.getUser().isSuper()
                // TODO: Shouldn't this be a CONSTANT somewhere? can only find it ref. 1 time in the existing code...
                && !bundle.getUser().isAuthorized( "F_UNCOMPLETE_EVENT" )) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1083 )
                    .addArg( bundle.getUser() ) );
            }
        }
    }
}
