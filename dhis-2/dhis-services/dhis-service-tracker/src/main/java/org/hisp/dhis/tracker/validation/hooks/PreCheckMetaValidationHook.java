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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1005;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1010;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1011;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1013;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1068;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1069;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1070;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4006;
import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.trackedEntityInstanceExist;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
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
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( tei.getUid() )
                .trackerType( tei.getTrackerType() )
                .errorCode( TrackerErrorCode.E1049 )
                .addArg( tei.getOrgUnit() )
                .build();
            reporter.addError( error );
        }

        TrackedEntityType entityType = context.getTrackedEntityType( tei.getTrackedEntityType() );
        if ( entityType == null )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( tei.getUid() )
                .trackerType( tei.getTrackerType() )
                .errorCode( E1005 )
                .addArg( tei.getTrackedEntityType() )
                .build();
            reporter.addError( error );
        }
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        OrganisationUnit organisationUnit = context.getOrganisationUnit( enrollment.getOrgUnit() );
        reporter.addErrorIf( () -> organisationUnit == null, () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) enrollment).getUid() )
            .trackerType( ((TrackerDto) enrollment).getTrackerType() )
            .errorCode( E1070 )
            .addArgs( enrollment.getOrgUnit() )
            .build() );

        Program program = context.getProgram( enrollment.getProgram() );
        reporter.addErrorIf( () -> program == null, () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) enrollment).getUid() )
            .trackerType( ((TrackerDto) enrollment).getTrackerType() )
            .errorCode( E1069 )
            .addArgs( enrollment.getProgram() )
            .build() );

        reporter.addErrorIf( () -> !trackedEntityInstanceExist( context, enrollment.getTrackedEntity() ),
            () -> TrackerErrorReport.builder()
                .uid( ((TrackerDto) enrollment).getUid() )
                .trackerType( ((TrackerDto) enrollment).getTrackerType() )
                .errorCode( E1068 )
                .addArgs( enrollment.getTrackedEntity() )
                .build() );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        OrganisationUnit organisationUnit = context.getOrganisationUnit( event.getOrgUnit() );
        reporter.addErrorIf( () -> organisationUnit == null, () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1011 )
            .addArgs( event.getOrgUnit() )
            .build() );

        Program program = context.getProgram( event.getProgram() );
        reporter.addErrorIf( () -> program == null, () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1010 )
            .addArgs( event.getProgram() )
            .build() );

        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        reporter.addErrorIf( () -> programStage == null, () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1013 )
            .addArgs( event.getProgramStage() )
            .build() );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        RelationshipType relationshipType = context.getRelationShipType( relationship.getRelationshipType() );

        reporter.addErrorIf( () -> relationshipType == null, () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) relationship).getUid() )
            .trackerType( ((TrackerDto) relationship).getTrackerType() )
            .errorCode( E4006 )
            .addArgs( relationship.getRelationshipType() )
            .build() );
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
