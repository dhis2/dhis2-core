package org.hisp.dhis.tracker.validation.hooks;

import static org.apache.commons.lang3.StringUtils.*;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1119;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;

/**
 * @author Luciano Fiandesio
 */
public class NoteValidationUtils
{
    protected static List<Note> validate( ValidationErrorReporter reporter, List<Note> notesToCheck )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        final List<Note> notes = new ArrayList<>();
        for ( Note note : notesToCheck )
        {
            if ( isNotEmpty( note.getValue() ) ) // Ignore notes with no text
            {
                // If a note having the same UID already exist in the db, raise error
                if ( isNotEmpty( note.getNote() ) && context.getNote( note.getNote() ).isPresent() )
                {
                    reporter.addError( newReport( E1119 ).addArgs( note.getNote() ) );
                }
                else
                {
                    notes.add( note );
                }
            }
        }
        return notes;
    }
}
