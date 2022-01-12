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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1008;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1121;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1122;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1123;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1124;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Enrico Colasante
 */
@Component
public class PreCheckMandatoryFieldsValidationHook
    extends AbstractTrackerDtoValidationHook
{
    private static final String ORG_UNIT = "orgUnit";

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        reporter.addErrorIf( () -> StringUtils.isEmpty( trackedEntity.getTrackedEntityType() ),
            () -> TrackerErrorReport.builder()
                .uid( ((TrackerDto) trackedEntity).getUid() )
                .trackerType( ((TrackerDto) trackedEntity).getTrackerType() )
                .errorCode( E1121 )
                .addArgs( "trackedEntityType" )
                .build() );
        reporter.addErrorIf( () -> StringUtils.isEmpty( trackedEntity.getOrgUnit() ), () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) trackedEntity).getUid() )
            .trackerType( ((TrackerDto) trackedEntity).getTrackerType() )
            .errorCode( E1121 )
            .addArgs( ORG_UNIT )
            .build() );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        reporter.addErrorIf( () -> StringUtils.isEmpty( enrollment.getOrgUnit() ), () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) enrollment).getUid() )
            .trackerType( ((TrackerDto) enrollment).getTrackerType() )
            .errorCode( E1122 )
            .addArgs( ORG_UNIT )
            .build() );
        reporter.addErrorIf( () -> StringUtils.isEmpty( enrollment.getProgram() ), () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) enrollment).getUid() )
            .trackerType( ((TrackerDto) enrollment).getTrackerType() )
            .errorCode( E1122 )
            .addArgs( "program" )
            .build() );
        reporter.addErrorIf( () -> StringUtils.isEmpty( enrollment.getTrackedEntity() ),
            () -> TrackerErrorReport.builder()
                .uid( ((TrackerDto) enrollment).getUid() )
                .trackerType( ((TrackerDto) enrollment).getTrackerType() )
                .errorCode( E1122 )
                .addArgs( "trackedEntity" )
                .build() );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        reporter.addErrorIf( () -> StringUtils.isEmpty( event.getOrgUnit() ), () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1123 )
            .addArgs( ORG_UNIT )
            .build() );
        reporter.addErrorIf( () -> StringUtils.isEmpty( event.getProgramStage() ), () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1123 )
            .addArgs( "programStage" )
            .build() );

        // TODO remove if once metadata import is fixed
        TrackerImportValidationContext context = reporter.getValidationContext();
        ProgramStage programStage = context.getProgramStage( event.getProgramStage() );
        if ( programStage != null )
        {
            // Program stages should always have a program! Due to how metadata
            // import is currently implemented
            // it's possible that users run into the edge case that a program
            // stage does not have an associated
            // program. Tell the user it's an issue with the metadata and not
            // the event itself. This should be
            // fixed in the metadata import. For more see
            // https://jira.dhis2.org/browse/DHIS2-12123
            addErrorIfNull( programStage.getProgram(), reporter, event, E1008, event.getProgramStage() );
            // return since program is not a required field according to our API
            // and the issue is with the missing reference in
            // the DB entry of the program stage and not the payload itself
            return;
        }

        reporter.addErrorIf( () -> StringUtils.isEmpty( event.getProgram() ), () -> TrackerErrorReport.builder()
            .uid( ((TrackerDto) event).getUid() )
            .trackerType( ((TrackerDto) event).getTrackerType() )
            .errorCode( E1123 )
            .addArgs( "program" )
            .build() );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        addErrorIfNull( relationship.getFrom(), reporter, relationship, E1124, "from" );
        addErrorIfNull( relationship.getTo(), reporter, relationship, E1124, "to" );
        reporter.addErrorIf( () -> StringUtils.isEmpty( relationship.getRelationshipType() ),
            () -> TrackerErrorReport.builder()
                .uid( ((TrackerDto) relationship).getUid() )
                .trackerType( ((TrackerDto) relationship).getTrackerType() )
                .errorCode( E1124 )
                .addArgs( "relationshipType" )
                .build() );
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }
}
