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

import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckUidValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateTrackedEntity( TrackerValidationReport report, TrackerImportValidationContext context,
        TrackedEntity trackedEntity )
    {
        if ( !CodeGenerator.isValidUid( trackedEntity.getTrackedEntity() ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( trackedEntity.getUid() )
                .trackerType( trackedEntity.getTrackerType() )
                .errorCode( TrackerErrorCode.E1048 )
                .addArg( trackedEntity.getTrackedEntity() )
                .addArg( trackedEntity )
                .addArg( trackedEntity.getTrackedEntity() )
                .build();
            report.addError( error );
        }
    }

    @Override
    public void validateEnrollment( TrackerValidationReport report, TrackerImportValidationContext context,
        Enrollment enrollment )
    {
        if ( !CodeGenerator.isValidUid( enrollment.getEnrollment() ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( enrollment.getUid() )
                .trackerType( enrollment.getTrackerType() )
                .errorCode( TrackerErrorCode.E1048 )
                .addArg( enrollment.getEnrollment() )
                .addArg( enrollment )
                .addArg( enrollment.getEnrollment() )
                .build();
            report.addError( error );
        }

        validateNotesUid( enrollment.getNotes(), report, enrollment );
    }

    @Override
    public void validateEvent( TrackerValidationReport report, TrackerImportValidationContext context, Event event )
    {
        if ( !CodeGenerator.isValidUid( event.getEvent() ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( event.getUid() )
                .trackerType( event.getTrackerType() )
                .errorCode( TrackerErrorCode.E1048 )
                .addArg( event.getEvent() )
                .addArg( event )
                .addArg( event.getEvent() )
                .build();
            report.addError( error );
        }

        validateNotesUid( event.getNotes(), report, event );
    }

    @Override
    public void validateRelationship( TrackerValidationReport report, TrackerImportValidationContext context,
        Relationship relationship )
    {
        if ( !CodeGenerator.isValidUid( relationship.getRelationship() ) )
        {
            TrackerErrorReport error = TrackerErrorReport.builder()
                .uid( relationship.getUid() )
                .trackerType( relationship.getTrackerType() )
                .errorCode( TrackerErrorCode.E1048 )
                .addArg( relationship.getRelationship() )
                .addArg( relationship )
                .addArg( relationship.getRelationship() )
                .build();
            report.addError( error );
        }
    }

    private void validateNotesUid( List<Note> notes, TrackerValidationReport report, TrackerDto dto )
    {
        for ( Note note : notes )
        {
            if ( !CodeGenerator.isValidUid( note.getNote() ) )
            {
                TrackerErrorReport error = TrackerErrorReport.builder()
                    .uid( dto.getUid() )
                    .trackerType( dto.getTrackerType() )
                    .errorCode( TrackerErrorCode.E1048 )
                    .addArg( note.getNote() )
                    .addArg( note )
                    .addArg( note.getNote() )
                    .build();
                report.addError( error );
            }
        }
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
