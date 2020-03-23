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
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
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
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_STAGE_INSTANCE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.TRACKED_ENTITY_CANT_BE_NULL;
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
    public void canRead( ValidationErrorReporter reporter, User user, ProgramInstance programInstance,
        boolean skipOwnershipCheck )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );

        Program program = programInstance.getProgram();
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataRead( user, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1096 )
                .addArg( user )
                .addArg( program ) );
//            errors.add( "User has no data read access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1092 )
                    .addArg( user )
                    .addArg( program ) );
//                errors.add(
//                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            if ( !skipOwnershipCheck &&
                !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1093 )
                    .addArg( user )
                    .addArg( programInstance.getEntityInstance() )
                    .addArg( program ) );
//                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }
        else
        {
            OrganisationUnit ou = programInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1101 )
                        .addArg( user )
                        .addArg( ou ) );
//                    errors.add( "User has no read access to organisation unit: " + ou.getUid() );
                }
            }
        }
    }

    @Override
    public void canCreateEnrollment( ValidationErrorReporter reporter, User user, Program program,
        ProgramInstance programInstance, boolean skipOwnershipCheck )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( program.getTrackedEntityType(), TRACKED_ENTITY_CANT_BE_NULL );

        OrganisationUnit ou = programInstance.getOrganisationUnit();
        Objects.requireNonNull( ou, ORGANISATION_UNIT_CANT_BE_NULL );
        //TODO should ou != null be allowed?
//        if ( ou != null )
//        {
        if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
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

        if ( !program.isWithoutRegistration() )
        {
            checkProgramWithoutRegistration( reporter, user, programInstance, skipOwnershipCheck, program );
        }
    }

    @Override
    public void canUpdateEnrollment( ValidationErrorReporter reporter, User user, ProgramInstance programInstance,
        boolean skipOwnershipCheck )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );

        Program program = programInstance.getProgram();
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );

        if ( !aclService.canDataWrite( user, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1091 )
                .addArg( user )
                .addArg( program ) );
//            errors.add( "User has no data write access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            checkProgramWithoutRegistration( reporter, user, programInstance, skipOwnershipCheck, program );
        }
        else
        {
            OrganisationUnit ou = programInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1098 )
                        .addArg( user )
                        .addArg( ou ) );
//                    errors.add( "User has no write access to organisation unit: " + ou.getUid() );
                }
            }
        }
    }

    @Override
    public void canDeleteEnrollment( ValidationErrorReporter reporter, User user, ProgramInstance programInstance,
        boolean skipOwnershipCheck )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programInstance, PROGRAM_INSTANCE_CANT_BE_NULL );

        Program program = programInstance.getProgram();

        if ( !aclService.canDataWrite( user, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1091 )
                .addArg( user )
                .addArg( program ) );
//            errors.add( "User has no data write access to program: " + program.getUid() );
        }

        if ( !program.isWithoutRegistration() )
        {
            checkProgramWithoutRegistration( reporter, user, programInstance, skipOwnershipCheck, program );
        }
        else
        {
            OrganisationUnit ou = programInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1094 )
                        .addArg( user )
                        .addArg( ou ) );
//                    errors.add( "User has no delete access to organisation unit: " + ou.getUid() );
                }
            }
        }

    }

    protected void checkProgramWithoutRegistration( ValidationErrorReporter reporter, User user,
        ProgramInstance programInstance, boolean skipOwnershipCheck, Program program )
    {
        if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1092 )
                .addArg( user )
                .addArg( program.getTrackedEntityType() ) );
//                errors.add(
//                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
        }
        if ( !skipOwnershipCheck &&
            !ownershipAccessManager.hasAccess( user, programInstance.getEntityInstance(), program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1093 )
                .addArg( user )
                .addArg( programInstance.getEntityInstance() )
                .addArg( program ) );
//                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
        }
    }

    @Override
    public void canCreateEnrollment( ValidationErrorReporter reporter, User user,
        ProgramStageInstance programStageInstance,
        boolean skipOwnershipCheck )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance, PROGRAM_STAGE_INSTANCE_CANT_BE_NULL );

        ProgramStage programStage = programStageInstance.getProgramStage();

        // TODO: Check this and document
        if ( isNull( programStage ) )
        {
            return;
        }

        Program program = programStage.getProgram();
        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();
        if ( ou != null )
        {
            if ( programStageInstance.isCreatableInSearchScope() ?
                !organisationUnitService.isInUserSearchHierarchyCached( user, ou )
                : !organisationUnitService.isInUserHierarchyCached( user, ou ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1090 )
                    .addArg( user )
                    .addArg( ou ) );
//                errors.add( "User has no create access to organisation unit: " + ou.getUid() );
            }
        }

        if ( program.isWithoutRegistration() )
        {
            if ( !aclService.canDataWrite( user, program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1091 )
                    .addArg( user )
                    .addArg( program ) );
//                errors.add( "User has no data write access to program: " + program.getUid() );
            }
        }
        else
        {
            checkCanWriteProgramStage( reporter, user, programStageInstance, skipOwnershipCheck );
        }

        if ( programStageInstance.getAttributeOptionCombo() != null )
        {
            canWrite( reporter, user, programStageInstance.getAttributeOptionCombo() );
        }
    }

    @Override
    public void canUpdateEnrollment( ValidationErrorReporter reporter, User user,
        ProgramStageInstance programStageInstance,
        boolean skipOwnershipCheck )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance, PROGRAM_STAGE_INSTANCE_CANT_BE_NULL );

        ProgramStage programStage = programStageInstance.getProgramStage();

        if ( isNull( programStage ) )
        {
            //TODO: Need to document this...
            return;
        }

        Program program = programStage.getProgram();

        if ( program.isWithoutRegistration() )
        {
            if ( !aclService.canDataWrite( user, program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1091 )
                    .addArg( user )
                    .addArg( program ) );
//                errors.add( "User has no data write access to program: " + program.getUid() );
            }
        }
        else
        {
            if ( !aclService.canDataWrite( user, programStage ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1095 )
                    .addArg( user )
                    .addArg( program ) );
//                errors.add( "User has no data write access to program stage: " + programStage.getUid() );
            }

            if ( !aclService.canDataRead( user, program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1096 )
                    .addArg( user )
                    .addArg( program ) );
//                errors.add( "User has no data read access to program: " + program.getUid() );
            }

            if ( !aclService.canDataRead( user, program.getTrackedEntityType() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1092 )
                    .addArg( user )
                    .addArg( program.getTrackedEntityType() ) );
//                errors.add(
//                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
            }

            OrganisationUnit ou = programStageInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserSearchHierarchyCached( user, ou ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1097 )
                        .addArg( user )
                        .addArg( ou ) );
//                    errors.add( "User has no update access to organisation unit: " + ou.getUid() );
                }
            }

            if ( !skipOwnershipCheck && !ownershipAccessManager
                .hasAccess( user, programStageInstance.getProgramInstance().getEntityInstance(), program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1093 )
                    .addArg( user )
                    .addArg( programStageInstance.getProgramInstance().getEntityInstance() )
                    .addArg( program ) );
//                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
            }
        }

        if ( programStageInstance.getAttributeOptionCombo() != null )
        {
            canWrite( reporter, user, programStageInstance.getAttributeOptionCombo() );
        }
//        errors.addAll( canWrite( user, programStageInstance.getAttributeOptionCombo() ) );

//        return errors;
    }

    @Override
    public void canDeleteEnrollment( ValidationErrorReporter reporter, User user,
        ProgramStageInstance programStageInstance,
        boolean skipOwnershipCheck )
    {
        Objects.requireNonNull( user, USER_CANT_BE_NULL );
        Objects.requireNonNull( programStageInstance, PROGRAM_STAGE_INSTANCE_CANT_BE_NULL );

        if ( programStageInstance.getProgramStage().getProgram().isWithoutRegistration() )
        {
            OrganisationUnit ou = programStageInstance.getOrganisationUnit();
            if ( ou != null )
            {
                if ( !organisationUnitService.isInUserHierarchyCached( user, ou ) )
                {
                    reporter.addError( newReport( TrackerErrorCode.E1094 )
                        .addArg( user )
                        .addArg( ou ) );
//                    errors.add( "User has no delete access to organisation unit: " + ou.getUid() );
                }
            }

            if ( !aclService.canDataWrite( user, programStageInstance.getProgramStage().getProgram() ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1091 )
                    .addArg( user )
                    .addArg( programStageInstance.getProgramStage().getProgram() ) );
//                errors.add( "User has no data write access to program: " + program.getUid() );
            }
        }
        else
        {
            checkCanWriteProgramStage( reporter, user, programStageInstance, skipOwnershipCheck );
        }
        if ( programStageInstance.getAttributeOptionCombo() != null )
        {
            canWrite( reporter, user, programStageInstance.getAttributeOptionCombo() );
        }
    }

    protected void checkCanWriteProgramStage( ValidationErrorReporter reporter, User user,
        ProgramStageInstance programStageInstance, boolean skipOwnershipCheck )
    {
        if ( !aclService.canDataWrite( user, programStageInstance.getProgramStage() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1095 )
                .addArg( user )
                .addArg( programStageInstance.getProgramStage() ) );
//                errors.add( "User has no data write access to program stage: " + programStage.getUid() );
        }

        if ( !aclService.canDataRead( user, programStageInstance.getProgramStage().getProgram() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1096 )
                .addArg( user )
                .addArg( programStageInstance.getProgramStage() ) );
//                errors.add( "User has no data read access to program: " + program.getUid() );
        }

        if ( !aclService
            .canDataRead( user, programStageInstance.getProgramStage().getProgram().getTrackedEntityType() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1092 )
                .addArg( user )
                .addArg( programStageInstance.getProgramStage() ) );
//                errors.add(
//                    "User has no data read access to tracked entity type: " + program.getTrackedEntityType().getUid() );
        }

        if ( !skipOwnershipCheck && !ownershipAccessManager
            .hasAccess( user, programStageInstance.getProgramInstance().getEntityInstance(),
                programStageInstance.getProgramStage().getProgram() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1093 )
                .addArg( user )
                .addArg( programStageInstance.getProgramInstance().getEntityInstance() )
                .addArg( programStageInstance.getProgramStage().getProgram() ) );
//                errors.add( TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED );
        }
    }

    @Override
    public void canWrite( ValidationErrorReporter reporter, User user, CategoryOptionCombo categoryOptionCombo )
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
//                errors.add( "User has no write access to category option: " + categoryOption.getUid() );
            }
        }

//        return errors;
    }

    private boolean isNull( ProgramStage programStage )
    {
        return programStage == null || programStage.getProgram() == null;
    }

}
