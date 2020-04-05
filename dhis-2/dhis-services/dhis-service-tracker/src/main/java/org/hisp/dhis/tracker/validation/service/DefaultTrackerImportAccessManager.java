package org.hisp.dhis.tracker.validation.service;

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

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.Constants;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_STAGE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_STAGE_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_TYPE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.USER_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class DefaultTrackerImportAccessManager
    implements TrackerImportAccessManager
{
    private final AclService aclService;

    private final TrackerOwnershipManager ownershipAccessManager;

    private final OrganisationUnitService organisationUnitService;

    public DefaultTrackerImportAccessManager( AclService aclService, TrackerOwnershipManager ownershipAccessManager,
        OrganisationUnitService organisationUnitService )
    {
        checkNotNull( aclService );
        checkNotNull( ownershipAccessManager );
        checkNotNull( organisationUnitService );

        this.aclService = aclService;
        this.ownershipAccessManager = ownershipAccessManager;
        this.organisationUnitService = organisationUnitService;
    }


    public void checkOrgUnitInSearchScope( ValidationErrorReporter reporter, TrackerBundle bundle,
        OrganisationUnit orgUnit )
    {
        Objects.requireNonNull( bundle.getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserSearchHierarchyCached( bundle.getUser(), orgUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1093 )
                .addArg( bundle.getUser() )
                .addArg( orgUnit ) );
        }
    }

    public void checkOrgUnitInCaptureScope( ValidationErrorReporter reporter, TrackerBundle bundle,
        OrganisationUnit orgUnit )
    {
        Objects.requireNonNull( bundle.getUser(), USER_CANT_BE_NULL );
        Objects.requireNonNull( orgUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        if ( !organisationUnitService.isInUserHierarchyCached( bundle.getUser(), orgUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( bundle.getUser() )
                .addArg( orgUnit ) );
        }
    }

    public void checkTeiTypeWriteAccess( ValidationErrorReporter reporter, User user,
        TrackedEntityType trackedEntityType )
    {
        Objects.requireNonNull( trackedEntityType, TRACKED_ENTITY_TYPE_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, trackedEntityType ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1001 )
                .addArg( user )
                .addArg( trackedEntityType ) );
        }
    }

    @Override
    public void checkReadEnrollmentAccess( ValidationErrorReporter reporter, User user,
        ProgramInstance programInstance )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( programInstance.getProgram(), PROGRAM_CANT_BE_NULL );

        checkProgramReadAccess( reporter, user, programInstance.getProgram() );

        if ( programInstance.getProgram().isRegistration() )
        {
            checkTeiTypeAndTeiProgramAccess( reporter, user, programInstance.getEntityInstance(),
                programInstance.getProgram() );
        }
        else
        {
            OrganisationUnit ou = programInstance.getOrganisationUnit();
            if ( ou != null && !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1101 )
                    .addArg( user )
                    .addArg( ou ) );
            }
        }
    }

    protected void checkTeiTypeAndTeiProgramAccess( ValidationErrorReporter reporter, User user,
        TrackedEntityInstance trackedEntityInstance,
        Program program )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( program.getTrackedEntityType(), TRACKED_ENTITY_TYPE_CANT_BE_NULL );
        Objects.requireNonNull( trackedEntityInstance, TRACKED_ENTITY_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1104 )
                .addArg( user )
                .addArg( program )
                .addArg( program.getTrackedEntityType() ) );
        }

        if ( !ownershipAccessManager.hasAccess( user, trackedEntityInstance, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1102 )
                .addArg( user )
                .addArg( trackedEntityInstance )
                .addArg( program ) );
        }
    }

    @Override
    public void checkWriteEnrollmentAccess( ValidationErrorReporter reporter, User user, Program program,
        ProgramInstance programInstance )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );

        checkProgramWriteAccess( reporter, user, program );

        if ( program.isRegistration() )
        {
            Objects.requireNonNull( program.getTrackedEntityType(), TRACKED_ENTITY_TYPE_CANT_BE_NULL );
            checkTeiTypeAndTeiProgramAccess( reporter, user, programInstance.getEntityInstance(), program );
        }
    }

    @Override
    public void checkEventWriteAccess( ValidationErrorReporter reporter, User user,
        ProgramStageInstance programStageInstance )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance, PROGRAM_STAGE_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance.getProgramStage(), PROGRAM_STAGE_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance.getProgramStage().getProgram(), PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance.getOrganisationUnit(), ORGANISATION_UNIT_CANT_BE_NULL );

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();

        // TODO: Get better explanation for isCreatableInSearchScope() what is this
        if ( programStageInstance.isCreatableInSearchScope() ?
            !organisationUnitService.isInUserSearchHierarchyCached( user, ou )
            : !organisationUnitService.isInUserHierarchyCached( user, ou ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( user )
                .addArg( ou ) );
        }

        if ( programStageInstance.getProgramStage().getProgram().isWithoutRegistration() )
        {
            checkProgramWriteAccess( reporter, user, programStageInstance.getProgramStage().getProgram() );
        }
        else
        {
            Objects.requireNonNull( programStageInstance.getProgramInstance(), PROGRAM_INSTANCE_CANT_BE_NULL );

            checkProgramStageWriteAccess( reporter, user, programStageInstance.getProgramStage() );
            checkProgramReadAccess( reporter, user, programStageInstance.getProgramStage().getProgram() );

            checkTeiTypeAndTeiProgramAccess( reporter, user,
                programStageInstance.getProgramInstance().getEntityInstance(),
                programStageInstance.getProgramStage().getProgram() );
        }

        if ( programStageInstance.getAttributeOptionCombo() != null )
        {
            checkWriteCategoryOptionComboAccess( reporter, user, programStageInstance.getAttributeOptionCombo() );
        }
    }

    protected void checkProgramReadAccess( ValidationErrorReporter reporter, User user,
        Program program )
    {
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
        if ( !aclService.canDataWrite( user, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1091 )
                .addArg( user )
                .addArg( program ) );
        }
    }

    @Override
    public void checkWriteCategoryOptionComboAccess( ValidationErrorReporter reporter, User user,
        CategoryOptionCombo categoryOptionCombo )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( categoryOptionCombo, Constants.CATEGORY_OPTION_COMBO_CANT_BE_NULL );

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
