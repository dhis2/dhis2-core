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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1005;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1011;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1029;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1033;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1035;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1041;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1069;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1070;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1086;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1087;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1088;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1089;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1094;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4006;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckMetaValidationHook
        extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity tei )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        OrganisationUnit organisationUnit = context.getOrganisationUnit( tei.getOrgUnit() );
        if ( organisationUnit == null )
        {
            addError( reporter, TrackerErrorCode.E1049, tei.getOrgUnit() );
        }

        TrackedEntityType entityType = context.getTrackedEntityType( tei.getTrackedEntityType() );
        if ( entityType == null )
        {
            addError( reporter, E1005, tei.getTrackedEntityType() );
        }
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        TrackerImportStrategy strategy = context.getStrategy( enrollment );

        OrganisationUnit organisationUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );
        addErrorIfNull( organisationUnit, reporter, E1070, enrollment.getOrgUnit() );

        Program program = context.getProgram( enrollment.getProgram() );
        addErrorIfNull( program,  reporter, E1069, enrollment.getProgram() );

        if ( (program != null && organisationUnit != null)
            && !programHasOrgUnit( program, organisationUnit, context.getProgramWithOrgUnitsMap() ) )
        {
            addError( reporter, E1041, organisationUnit, program );
        }

        if ( strategy.isUpdate() )
        {
            ProgramInstance pi = context.getProgramInstance( enrollment.getEnrollment() );
            Program existingProgram = pi.getProgram();
            if ( program != null && !existingProgram.getUid().equals( program.getUid() ) )
            {
                addError( reporter, E1094, pi, existingProgram );
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
        addErrorIfNull( organisationUnit, reporter, E1011, event.getOrgUnit() );

        Program program = context.getProgram( event.getProgram() );
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        if ( program != null )
        {
            addErrorIf( () -> program.isRegistration() && StringUtils.isEmpty( event.getEnrollment() ), reporter, E1033,
                    event.getEvent() );
        }
        
        if ( (program != null && organisationUnit != null)
            && !programHasOrgUnit( program, organisationUnit, context.getProgramWithOrgUnitsMap() ) )
        {
            addError( reporter, E1029, organisationUnit, program );
        }
        
        validateEventProgramAndProgramStage( reporter, event, context, strategy, bundle, program, programStage );
        validateDataElementForDataValues( reporter, event, context );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        RelationshipType relationshipType = context.getRelationShipType( relationship.getRelationshipType() );
        if ( relationshipType == null )
        {
            addError( reporter, E4006, relationship.getRelationshipType() );
        }
    }

    private void validateDataElementForDataValues( ValidationErrorReporter reporter, Event event,
        TrackerImportValidationContext context )
    {
        event.getDataValues()
            .stream()
            .map( DataValue::getDataElement )
            .forEach( de -> {
                DataElement dataElement = context.getBundle().getPreheat().get( DataElement.class, de );
                addErrorIfNull( dataElement,  reporter, E1087, event.getEvent(), de );
            } );
    }

    private void validateEventProgramAndProgramStage( ValidationErrorReporter reporter, Event event,
                                                      TrackerImportValidationContext context, TrackerImportStrategy strategy, TrackerBundle bundle, Program program,
                                                      ProgramStage programStage )
    {
        if ( program == null && programStage == null )
        {
            addError( reporter, E1088, event, event.getProgram(), event.getProgramStage() );
        }

        if ( program == null && programStage != null )
        {
            program = fetchProgramFromProgramStage( event, bundle, programStage );
        }

        if ( program != null && programStage == null && program.isRegistration() )
        {
            addError( reporter, E1086, event, program );
        }

        if ( program != null && programStage == null && program.isWithoutRegistration() )
        {
            addErrorIfNull( program.getProgramStageByStage( 1 ), reporter, E1035, event );
        }

        if ( program != null && programStage != null && !program.getUid().equals( programStage.getProgram().getUid() ) )
        {
            addError( reporter, E1089, event, programStage, program );
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

        if ( !existingProgram.getUid().equals( program.getUid() ) )
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

        // no need to add the program if already in preheat
        if ( bundle.getPreheat().get( Program.class, identifier.getIdentifier( program ) ) == null )
        {
            bundle.getPreheat().put( identifier, program );
        }
        event.setProgram( identifier.getIdentifier( program ) );
        return program;
    }

    private boolean programHasOrgUnit( Program program, OrganisationUnit orgUnit,
        Map<Long, List<Long>> programAndOrgUnitsMap )
    {
        return programAndOrgUnitsMap.containsKey( program.getId() )
            && programAndOrgUnitsMap.get( program.getId() ).contains( orgUnit.getId() );
    } 
    
    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
