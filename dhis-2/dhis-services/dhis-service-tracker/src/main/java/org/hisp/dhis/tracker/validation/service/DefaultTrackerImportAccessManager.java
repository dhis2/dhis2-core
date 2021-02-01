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
package org.hisp.dhis.tracker.validation.service;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.PROGRAM_STAGE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_TYPE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.USER_CANT_BE_NULL;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class DefaultTrackerImportAccessManager
    implements TrackerImportAccessManager
{
    @NonNull
    private final AclService aclService;

    @NonNull
    private final TrackerOwnershipManager ownershipAccessManager;

    @NonNull
    private final OrganisationUnitService organisationUnitService;

    public void checkOrgUnitInSearchScope( ValidationErrorReporter reporter, OrganisationUnit orgUnit )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserSearchHierarchyCached( user, orgUnit ) )
        {
            // TODO: This state I can't reach, can't enroll in programs without
            // registration...
            // maybe remove in the new importer?
            reporter.addError( newReport( TrackerErrorCode.E1093 )
                .addArg( user )
                .addArg( orgUnit ) );
        }
    }

    public void checkOrgUnitInCaptureScope( ValidationErrorReporter reporter, OrganisationUnit orgUnit )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserHierarchyCached( user, orgUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( user )
                .addArg( orgUnit ) );
        }
    }

    public void checkTeiTypeWriteAccess( ValidationErrorReporter reporter, TrackedEntityType trackedEntityType )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( trackedEntityType, TRACKED_ENTITY_TYPE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, trackedEntityType ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1001 )
                .addArg( user )
                .addArg( trackedEntityType ) );
        }
    }

    @Override
    public void checkReadEnrollmentAccess( ValidationErrorReporter reporter, Program program,
        OrganisationUnit organisationUnit, String trackedEntity )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        checkProgramReadAccess( reporter, user, program );

        if ( program.isRegistration() )
        {
            checkTeiTypeAndTeiProgramAccess( reporter, user, trackedEntity, organisationUnit, program );
        }
        else
        {
            // TODO: This state I can't reach, can't enroll in programs without
            // registration...
            // maybe remove in the new importer?
            checkOrgUnitInSearchScope( reporter, organisationUnit );
        }
    }

    protected void checkTeiTypeAndTeiProgramAccess( ValidationErrorReporter reporter, User user,
        String trackedEntityInstance,
        OrganisationUnit organisationUnit,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );
        checkNotNull( program.getTrackedEntityType(), TRACKED_ENTITY_TYPE_CANT_BE_NULL );
        checkNotNull( trackedEntityInstance, TRACKED_ENTITY_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1104 )
                .addArg( user )
                .addArg( program )
                .addArg( program.getTrackedEntityType() ) );
        }

        if ( !ownershipAccessManager.hasAccess( user, trackedEntityInstance, organisationUnit,
            program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1102 )
                .addArg( user )
                .addArg( trackedEntityInstance )
                .addArg( program ) );
        }
    }

    @Override
    public void checkWriteEnrollmentAccess( ValidationErrorReporter reporter, Program program,
        String trackedEntity, OrganisationUnit organisationUnit )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        checkProgramWriteAccess( reporter, user, program );

        if ( program.isRegistration() )
        {
            checkNotNull( program.getTrackedEntityType(), TRACKED_ENTITY_TYPE_CANT_BE_NULL );
            checkTeiTypeAndTeiProgramAccess( reporter, user, trackedEntity, organisationUnit, program );
        }
    }

    @Override
    public void checkEventWriteAccess( ValidationErrorReporter reporter, ProgramStage programStage,
        OrganisationUnit orgUnit,
        CategoryOptionCombo categoryOptionCombo,
        String trackedEntity, boolean isCreatableInSearchScope )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );
        checkNotNull( programStage.getProgram(), PROGRAM_CANT_BE_NULL );
        checkNotNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( isCreatableInSearchScope ? !organisationUnitService.isInUserSearchHierarchyCached( user, orgUnit )
            : !organisationUnitService.isInUserHierarchyCached( user, orgUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( user )
                .addArg( orgUnit ) );
        }

        if ( programStage.getProgram().isWithoutRegistration() )
        {
            checkProgramWriteAccess( reporter, user, programStage.getProgram() );
        }
        else
        {
            checkProgramStageWriteAccess( reporter, user, programStage );
            // at this point the link between program and program stage should
            // be validated
            // so it is safe to fetch the Program from the program stage
            final String programUid = programStage.getProgram().getUid();
            final Program program = reporter.getPreheat().getAll( Program.class )
                .stream().filter( p -> p.getUid().equals( programUid ) ).findAny()
                .orElseThrow( () -> new NullPointerException( PROGRAM_CANT_BE_NULL ) );

            checkProgramReadAccess( reporter, user, program );

            checkTeiTypeAndTeiProgramAccess( reporter, user,
                trackedEntity,
                orgUnit,
                programStage.getProgram() );
        }

        if ( categoryOptionCombo != null )
        {
            checkWriteCategoryOptionComboAccess( reporter, categoryOptionCombo );
        }
    }

    protected void checkProgramReadAccess( ValidationErrorReporter reporter, User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1096 )
                .addArg( user )
                .addArg( program ) );
        }
    }

    protected void checkProgramStageWriteAccess( ValidationErrorReporter reporter, User user,
        ProgramStage programStage )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( programStage, PROGRAM_STAGE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, programStage ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1095 )
                .addArg( user )
                .addArg( programStage ) );
        }
    }

    protected void checkProgramWriteAccess( ValidationErrorReporter reporter, User user,
        Program program )
    {
        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1091 )
                .addArg( user )
                .addArg( program ) );
        }
    }

    @Override
    public void checkWriteCategoryOptionComboAccess( ValidationErrorReporter reporter,
        CategoryOptionCombo categoryOptionCombo )
    {
        TrackerBundle bundle = reporter.getValidationContext().getBundle();
        User user = bundle.getUser();

        checkNotNull( user, USER_CANT_BE_NULL );
        checkNotNull( categoryOptionCombo, TrackerImporterAssertErrors.CATEGORY_OPTION_COMBO_CANT_BE_NULL );

        for ( CategoryOption categoryOption : categoryOptionCombo.getCategoryOptions() )
        {
            if ( !aclService.canDataWrite( user, categoryOption ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1099 )
                    .addArg( user )
                    .addArg( categoryOption ) );
            }
        }
    }
}
