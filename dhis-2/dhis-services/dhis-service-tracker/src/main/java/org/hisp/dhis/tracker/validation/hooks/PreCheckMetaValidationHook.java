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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckMetaValidationHook
    extends AbstractPreCheckValidationHook
{
    @Override
    public int getOrder()
    {
        return 1;
    }

    public void validateTrackedEntities( ValidationErrorReporter reporter, TrackerBundle bundle, TrackedEntity tei )
    {
        validateOrganisationUnit( reporter, bundle, tei.getOrgUnit() );
        validateTrackedEntityType( reporter, bundle, tei );
    }

    public void validateEnrollments( ValidationErrorReporter reporter, TrackerBundle bundle, Enrollment enrollment )
    {
        Program program = PreheatHelper.getProgram( bundle, enrollment.getProgram() );
        if ( program == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1069 )
                .addArg( enrollment.getTrackedEntity() ) );
        }

        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, enrollment.getOrgUnit() );
        if ( organisationUnit == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1070 )
                .addArg( enrollment.getTrackedEntity() ) );
        }

        if ( (program != null && organisationUnit != null) && !program.hasOrganisationUnit( organisationUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1041 )
                .addArg( organisationUnit )
                .addArg( program )
                .addArg( program.getOrganisationUnits() ) );
        }
    }

    public void validateEvents( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
    {
        Program program = PreheatHelper.getProgram( bundle, event.getProgram() );
        OrganisationUnit organisationUnit = PreheatHelper.getOrganisationUnit( bundle, event.getOrgUnit() );
        ProgramStage programStage = PreheatHelper.getProgramStage( bundle, event.getProgramStage() );

        if ( organisationUnit == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1011 )
                .addArg( event.getOrgUnit() ) );
        }

        if ( program == null && programStage == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1088 )
                .addArg( event ) );
        }

        if ( program == null && programStage != null )
        {
            // We use a little trick here to put a program into the event and bundle
            // if program is missing from event but exists on the program stage.
            program = programStage.getProgram();
            TrackerIdentifier identifier = bundle.getPreheat().getIdentifiers().getProgramIdScheme();
            bundle.getPreheat().put( identifier, program );
            event.setProgram( identifier.getIdentifier( program ) );
        }

        if ( program != null && programStage == null && program.isRegistration() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1086 )
                .addArg( event )
                .addArg( program ) );
        }

        // TODO: Should this be placed before the check above?
        if ( program != null )
        {
            programStage = (programStage == null && program.isWithoutRegistration()) ?
                program.getProgramStageByStage( 1 ) :
                programStage;
        }

        if ( programStage == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1035 )
                .addArg( event ) );
        }

        if ( program != null && programStage != null && !program.equals( programStage.getProgram() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1089 )
                .addArg( program )
                .addArg( programStage ) );
        }
    }

    private void validateOrganisationUnit( ValidationErrorReporter errorReporter,
        TrackerBundle bundle, String organisationUnitID )
    {
        if ( bundle.getImportStrategy().isCreate() )
        {
            if ( StringUtils.isEmpty( organisationUnitID ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1010 ) );
                return;
            }

            OrganisationUnit organisationUnit = PreheatHelper
                .getOrganisationUnit( bundle, organisationUnitID );

            if ( organisationUnit == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1011 )
                    .addArg( organisationUnitID ) );
            }
        }
    }

    private void validateTrackedEntityType( ValidationErrorReporter errorReporter, TrackerBundle bundle,
        TrackedEntity trackedEntity )
    {
        Objects.requireNonNull( trackedEntity, Constants.TRACKED_ENTITY_CANT_BE_NULL );

        if ( bundle.getImportStrategy().isCreate() )
        {
            if ( trackedEntity.getTrackedEntityType() == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1004 ) );
                return;
            }

            TrackedEntityType entityType = PreheatHelper
                .getTrackedEntityType( bundle, trackedEntity.getTrackedEntityType() );
            if ( entityType == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1005 )
                    .addArg( trackedEntity.getTrackedEntityType() ) );
            }
        }
    }
}
