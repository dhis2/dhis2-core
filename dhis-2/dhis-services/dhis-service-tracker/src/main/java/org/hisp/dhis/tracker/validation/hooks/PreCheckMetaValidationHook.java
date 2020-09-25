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
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckMetaValidationHook
    extends AbstractTrackerDtoValidationHook
{

    public PreCheckMetaValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( teAttrService );
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity tei )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        OrganisationUnit organisationUnit = context.getOrganisationUnit( tei.getOrgUnit() );
        if ( organisationUnit == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1049 )
                .addArg( reporter ) );
        }

        TrackedEntityType entityType = context.getTrackedEntityType( tei.getTrackedEntityType() );
        if ( entityType == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1005 )
                .addArg( tei.getTrackedEntityType() ) );
        }
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( enrollment );

        OrganisationUnit organisationUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );
        if ( organisationUnit == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1070 )
                .addArg( enrollment.getOrgUnit() ) );
        }

        Program program = context.getProgram( enrollment.getProgram() );
        if ( program == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1069 )
                .addArg( enrollment.getProgram() ) );
        }

        if ( (program != null && organisationUnit != null) && !program.hasOrganisationUnit( organisationUnit ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1041 )
                .addArg( organisationUnit )
                .addArg( program )
                .addArg( program.getOrganisationUnits() ) );
        }

        if ( strategy.isUpdate() )
        {
            ProgramInstance pi = context.getProgramInstance( enrollment.getEnrollment() );
            Program existingProgram = pi.getProgram();
            if ( !existingProgram.equals( program ) )
            {
                reporter.addError( newReport( TrackerErrorCode.E1094 )
                    .addArg( pi )
                    .addArg( existingProgram ) );
            }
        }
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( event );
        TrackerBundle bundle = context.getBundle();

        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        if ( organisationUnit == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1011 )
                .addArg( event.getOrgUnit() ) );
        }

        Program program = context.getProgram( event.getProgram() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );

        validateEventProgramAndProgramStage( reporter, event, context, strategy, bundle, program, programStage );
    }

    private void validateEventProgramAndProgramStage( ValidationErrorReporter reporter, Event event,
        TrackerImportValidationContext context, TrackerImportStrategy strategy, TrackerBundle bundle, Program program,
        ProgramStage programStage )
    {
        if ( program == null && programStage == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1088 )
                .addArg( event )
                .addArg( event.getProgram() )
                .addArg( event.getProgramStage() ) );
        }

        if ( program == null && programStage != null )
        {
            program = fetchProgramFromProgramStage( event, bundle, programStage );
        }

        if ( program != null && programStage == null && program.isRegistration() )
        {
            reporter.addError( newReport( TrackerErrorCode.E1086 )
                .addArg( event )
                .addArg( program ) );
        }

        if ( program != null )
        {
            programStage = (programStage == null && program.isWithoutRegistration())
                ? program.getProgramStageByStage( 1 )
                : programStage;
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

        if ( strategy.isUpdate() )
        {
            validateNotChangingProgram( reporter, event, context, program );
        }
    }

    private void validateNotChangingProgram( ValidationErrorReporter reporter, Event event,
        TrackerImportValidationContext context, Program program )
    {
        ProgramStageInstance psi = context.getProgramStageInstance( event.getEvent() );
        Program existingProgram = psi.getProgramStage().getProgram();

        if ( !existingProgram.equals( program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1110 )
                .addArg( psi )
                .addArg( existingProgram ) );
        }
    }

    private Program fetchProgramFromProgramStage( Event event, TrackerBundle bundle, ProgramStage programStage )
    {
        Program program;// We use a little trick here to put a program into the event and bundle
        // if program is missing from event but exists on the program stage.
        // TODO: This trick mutates the data, try to avoid this...
        program = programStage.getProgram();
        TrackerIdentifier identifier = bundle.getPreheat().getIdentifiers().getProgramIdScheme();
        bundle.getPreheat().put( identifier, program );
        event.setProgram( identifier.getIdentifier( program ) );
        return program;
    }
}
