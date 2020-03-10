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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.Constants.ORGANISATION_UNIT_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.Constants.PROGRAM_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckSecurityValidationHook
    extends AbstractPreCheckValidationHook
{
    @Override
    public int getOrder()
    {
        return 2;
    }

    @Autowired
    protected AclService aclService;

    @Override
    public void validateTrackedEntities( ValidationErrorReporter reporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        TrackedEntityType entityType = getTrackedEntityType( bundle, trackedEntity );
        if ( entityType != null && !aclService.canDataWrite( bundle.getUser(), entityType ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1001 )
                .addArg( bundle.getUser() )
                .addArg( entityType ) );
        }

        OrganisationUnit orgUnit = getOrganisationUnit( bundle, trackedEntity );

        // TODO: Added comment to make sure the reason for this not so intuitive reason,
        // This should be better commented and documented somewhere
        // Ameen 10.09.2019, 12:32 fix: relax restriction on writing to tei in search scope 48a82e5f
        if ( orgUnit != null && !organisationUnitService.isInUserSearchHierarchyCached( bundle.getUser(), orgUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( bundle.getUser() )
                .addArg( orgUnit ) );
        }
    }

    @Override
    public void validateEnrollments( ValidationErrorReporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, enrollment.getOrgUnit() );

        Objects.requireNonNull( program, PROGRAM_CANT_BE_NULL );
        Objects.requireNonNull( organisationUnit, ORGANISATION_UNIT_CANT_BE_NULL );

        // See note below on this.
        if ( !organisationUnitService.isInUserHierarchyCached( bundle.getUser(), organisationUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1028 )
                .addArg( bundle.getUser() )
                .addArg( enrollment )
                .addArg( program ) );
        }
    }

    @Override
    public void validateEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, event.getOrgUnit() );

        if ( organisationUnit != null &&
            !organisationUnitService.isInUserHierarchyCached( bundle.getUser(), organisationUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1000 )
                .addArg( bundle.getUser() )
                .addArg( organisationUnit ) );
        }
    }
}
