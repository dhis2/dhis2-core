/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.tracker.report.TrackerErrorReport.newReport;

import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class PreCheckUidValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity trackedEntity )
    {
        checkUidFormat( trackedEntity.getTrackedEntity(), reporter, trackedEntity, trackedEntity.getTrackedEntity() );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        checkUidFormat( enrollment.getEnrollment(), reporter, enrollment, enrollment.getEnrollment() );

        validateNotesUid( enrollment.getNotes(), reporter );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        checkUidFormat( event.getEvent(), reporter, event, event.getEvent() );

        validateNotesUid( event.getNotes(), reporter );
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        checkUidFormat( relationship.getRelationship(), reporter, relationship, relationship.getRelationship() );
    }

    private void validateNotesUid( List<Note> notes, ValidationErrorReporter reporter )
    {
        for ( Note note : notes )
        {
            checkUidFormat( note.getNote(), reporter, note, note.getNote() );
        }
    }

    /**
     * Check if the given UID has a valid format.
     *
     * @param uid a UID
     * @param reporter a {@see ValidationErrorReporter} to which the error is
     *        added
     * @param args list of arguments for the Error report
     */
    private void checkUidFormat( String uid, ValidationErrorReporter reporter, Object... args )
    {
        if ( !CodeGenerator.isValidUid( uid ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1048 )
                .addArg( args[0] )
                .addArg( args[1] ) );
        }
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }

}
