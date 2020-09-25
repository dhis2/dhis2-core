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

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckValidateAndGenerateUidHook
    extends AbstractTrackerDtoValidationHook
{
    public PreCheckValidateAndGenerateUidHook( TrackedEntityAttributeService teAttrService )
    {
        super( teAttrService );
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        String uid = trackedEntity.getTrackedEntity();

        if ( isUidInvalid( uid, reporter, trackedEntity, trackedEntity.getTrackedEntity() ) )
        {
            return;
        }

        if ( uid == null )
        {
            trackedEntity.setUid( CodeGenerator.generateUid() );
        }
        else
        {
            trackedEntity.setUid( uid );
        }
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        final String uid = enrollment.getEnrollment();

        if ( isUidInvalid( uid, reporter, enrollment, enrollment.getEnrollment() ) )
        {
            return;
        }

        if ( uid == null )
        {
            enrollment.setUid( CodeGenerator.generateUid() );
        }
        else
        {
            enrollment.setUid( uid );
        }

        validateNotesUid( enrollment.getNotes(), reporter );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        final String uid = event.getEvent();

        if ( isUidInvalid( uid, reporter, event, event.getEvent() ) )
        {
            return;
        }

        if ( uid == null )
        {
            event.setUid( CodeGenerator.generateUid() );
        }
        else
        {
            event.setUid( uid );
        }

        // Generate UID for notes
        validateNotesUid( event.getNotes(), reporter );
    }

    private void validateNotesUid( List<Note> notes, ValidationErrorReporter reporter )
    {
        if ( isNotEmpty( notes ) )
        {
            for ( Note note : notes )
            {
                if ( isUidInvalid( note.getNote(), reporter, note, note.getNote() ) )
                {
                    return;
                }
                if ( note.getNote() == null )
                {
                    note.setNote( CodeGenerator.generateUid() );
                    note.setNewNote( true );
                }
            }
        }

    }

    /**
     * Check if the given UID has a valid format. A null UID is considered valid.
     * 
     * @param uid a UID. The UID string can be null.
     * @param reporter a {@see ValidationErrorReporter} to which the error is added
     * @param args list of arguments for the Error report
     * @return true, if the UID is invalid
     */
    private boolean isUidInvalid( String uid, ValidationErrorReporter reporter, Object... args )
    {
        if ( uid != null && !CodeGenerator.isValidUid( uid ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1048 )
                .addArg( args[0] )
                .addArg( args[1] ) );
            return true;
        }
        return false;
    }
}
