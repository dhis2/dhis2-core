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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1002;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1030;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1032;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1063;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1080;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1081;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1082;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1113;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1114;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4015;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.Error;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.Warning;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckExistenceValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateTrackedEntity( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackedEntity trackedEntity )
    {
        TrackerImportStrategy importStrategy = context.getStrategy( trackedEntity );

        TrackedEntityInstance existingTe = context
            .getTrackedEntityInstance( trackedEntity.getTrackedEntity() );

        // If the tracked entity is soft-deleted no operation is allowed
        if ( existingTe != null && existingTe.isDeleted() )
        {
            Error error = Error.builder()
                .uid( trackedEntity.getUid() )
                .trackerType( trackedEntity.getTrackerType() )
                .errorCode( E1114 )
                .addArg( trackedEntity.getTrackedEntity() )
                .build();
            report.addError( error );
            return;
        }

        if ( existingTe != null && importStrategy.isCreate() )
        {
            Error error = Error.builder()
                .uid( trackedEntity.getUid() )
                .trackerType( trackedEntity.getTrackerType() )
                .errorCode( E1002 )
                .addArg( trackedEntity.getTrackedEntity() )
                .build();
            report.addError( error );
        }
        else if ( existingTe == null && importStrategy.isUpdateOrDelete() )
        {
            Error error = Error.builder()
                .uid( trackedEntity.getUid() )
                .trackerType( trackedEntity.getTrackerType() )
                .errorCode( E1063 )
                .addArg( trackedEntity.getTrackedEntity() )
                .build();
            report.addError( error );
        }
    }

    @Override
    public void validateEnrollment( TrackerValidationReport report, TrackerImportValidationContext context,
        Enrollment enrollment )
    {
        TrackerImportStrategy importStrategy = context.getStrategy( enrollment );

        ProgramInstance existingPi = context.getProgramInstance( enrollment.getEnrollment() );

        // If the tracked entity is soft-deleted no operation is allowed
        if ( existingPi != null && existingPi.isDeleted() )
        {
            Error error = Error.builder()
                .uid( enrollment.getUid() )
                .trackerType( enrollment.getTrackerType() )
                .errorCode( E1113 )
                .addArg( enrollment.getEnrollment() )
                .build();
            report.addError( error );
            return;
        }

        if ( existingPi != null && importStrategy.isCreate() )
        {
            Error error = Error.builder()
                .uid( enrollment.getUid() )
                .trackerType( enrollment.getTrackerType() )
                .errorCode( E1080 )
                .addArg( enrollment.getEnrollment() )
                .build();
            report.addError( error );
        }
        else if ( existingPi == null && importStrategy.isUpdateOrDelete() )
        {
            Error error = Error.builder()
                .uid( enrollment.getUid() )
                .trackerType( enrollment.getTrackerType() )
                .errorCode( E1081 )
                .addArg( enrollment.getEnrollment() )
                .build();
            report.addError( error );
        }
    }

    @Override
    public void validateEvent( TrackerValidationReport report, TrackerImportValidationContext context, Event event )
    {
        TrackerImportStrategy importStrategy = context.getStrategy( event );

        ProgramStageInstance existingPsi = context.getProgramStageInstance( event.getEvent() );

        // If the event is soft-deleted no operation is allowed
        if ( existingPsi != null && existingPsi.isDeleted() )
        {
            Error error = Error.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( E1082 )
                .addArg( event.getEvent() )
                .build();
            report.addError( error );
            return;
        }

        if ( existingPsi != null && importStrategy.isCreate() )
        {
            Error error = Error.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( E1030 )
                .addArg( event.getEvent() )
                .build();
            report.addError( error );
        }
        else if ( existingPsi == null && importStrategy.isUpdateOrDelete() )
        {
            Error error = Error.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( E1032 )
                .addArg( event.getEvent() )
                .build();
            report.addError( error );
        }
    }

    @Override
    public void validateRelationship( TrackerValidationReport report, TrackerImportValidationContext context,
        Relationship relationship )
    {
        org.hisp.dhis.relationship.Relationship existingRelationship = context.getRelationship( relationship );

        if ( existingRelationship != null )
        {
            Warning warn = Warning.builder()
                .uid( relationship.getUid() )
                .trackerType( relationship.getTrackerType() )
                .warningCode( E4015 )
                .addArg( relationship.getRelationship() )
                .build();
            report.addWarning( warn );
        }
    }

    @Override
    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return true;
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }
}
