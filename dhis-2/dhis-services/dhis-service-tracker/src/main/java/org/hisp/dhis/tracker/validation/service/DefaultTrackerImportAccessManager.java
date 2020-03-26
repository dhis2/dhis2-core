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
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.Constants;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_STAGE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_STAGE_INSTANCE_CANT_BE_NULL;
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

    @Override
    public void canRead( ValidationErrorReporter reporter, User user, ProgramInstance programInstance )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( programInstance.getProgram(), PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, programInstance.getProgram() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1096 )
                .addArg( user )
                .addArg( programInstance.getProgram() ) );
        }

        if ( !programInstance.getProgram().isWithoutRegistration() )
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
        if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1092 )
                .addArg( user )
                .addArg( program ) );
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
    public void canWriteEnrollment( ValidationErrorReporter reporter, User user, Program program,
        ProgramInstance programInstance )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        //TODO: Investigate, must all program have a tei type?
        // Objects.requireNonNull( program.getTrackedEntityType(), TRACKED_ENTITY_CANT_BE_NULL );

        OrganisationUnit ou = programInstance.getOrganisationUnit();
        // TODO: Investigate programInstance without ou possible or bug?
        if ( ou != null && !organisationUnitService.isInUserHierarchyCached( user, ou ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1090 )
                .addArg( user )
                .addArg( ou ) );
        }

        if ( !aclService.canDataWrite( user, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1091 )
                .addArg( user )
                .addArg( program ) );
        }

        // TODO: IS without reg. always same as isReg? or is NULL also a state?
        if ( !program.isWithoutRegistration() )
        {
            checkTeiTypeAndTeiProgramAccess( reporter, user, programInstance.getEntityInstance(), program );
        }
    }

    @Override
    public void canWriteEvent( ValidationErrorReporter reporter, User user, ProgramStageInstance programStageInstance )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance, PROGRAM_STAGE_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance.getProgramStage(), PROGRAM_STAGE_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance.getProgramStage().getProgram(), PROGRAM_CANT_BE_NULL );

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();
        // TODO: ou possible?
        if ( ou != null && (programStageInstance.isCreatableInSearchScope() ?
            !organisationUnitService.isInUserSearchHierarchyCached( user, ou )
            : !organisationUnitService.isInUserHierarchyCached( user, ou )) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1090 )
                .addArg( user )
                .addArg( ou ) );
        }

        if ( programStageInstance.getProgramStage().getProgram().isWithoutRegistration() )
        {
            if ( !aclService.canDataWrite( user, programStageInstance.getProgramStage().getProgram() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1091 )
                    .addArg( user )
                    .addArg( programStageInstance.getProgramStage().getProgram() ) );
            }
        }
        else
        {
            if ( !aclService.canDataWrite( user, programStageInstance.getProgramStage() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1095 )
                    .addArg( user )
                    .addArg( programStageInstance.getProgramStage() ) );
            }

            if ( !aclService.canDataRead( user, programStageInstance.getProgramStage().getProgram() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1096 )
                    .addArg( user )
                    .addArg( programStageInstance.getProgramStage() ) );
            }

            checkTeiTypeAndTeiProgramAccess( reporter, user,
                programStageInstance.getProgramInstance().getEntityInstance(),
                programStageInstance.getProgramStage().getProgram() );
        }

        if ( programStageInstance.getAttributeOptionCombo() != null )
        {
            canWriteCategoryOptionCombo( reporter, user, programStageInstance.getAttributeOptionCombo() );
        }
    }

    @Override
    public void canWriteCategoryOptionCombo( ValidationErrorReporter reporter, User user,
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
