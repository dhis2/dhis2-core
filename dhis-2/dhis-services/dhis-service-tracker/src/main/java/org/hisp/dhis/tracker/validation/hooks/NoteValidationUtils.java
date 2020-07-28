package org.hisp.dhis.tracker.validation.hooks;

import com.google.common.collect.Streams;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.domain.Note;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Luciano Fiandesio
 */
public class NoteValidationUtils {

    private NoteValidationUtils()
    {
    }

    /**
     * Filters out from a List of {@see Note}, notes that have no value (empty note)
     * and notes with an uid that already exist in the database.
     *
     * @param commentService an instance of {@see TrackedEntityCommentService},
     *        required to check if a note uid already exist in the db
     * @param notes the list of {@see Note} to filter
     * @return a filtered list of {@see Note}}
     */
    static List<Note> getPersistableNotes( TrackedEntityCommentService commentService, List<Note> notes )
    {
        // Check which notes are already on the DB and skip them
        // Only check notes that are marked NOT marked as "new note"
        // FIXME: do we really need this? Currently trackedentitycomment uid is a unique
        // key in the db, can't we simply catch the exception
        List<String> nonExistingUid = commentService.filterExistingNotes( notes.stream()
            .filter( n -> StringUtils.isNotEmpty( n.getValue() ) )
            .filter( n -> !n.isNewNote() )
            .map( Note::getNote )
            .collect( Collectors.toList() ) );

        return Streams.concat(
            notes.stream()
                .filter( Note::isNewNote )
                .filter( n -> StringUtils.isNotEmpty( n.getValue() ) ),
            notes.stream()
                .filter( n -> nonExistingUid.contains( n.getNote() ) ) )
            .collect( Collectors.toList() );
    }
}
