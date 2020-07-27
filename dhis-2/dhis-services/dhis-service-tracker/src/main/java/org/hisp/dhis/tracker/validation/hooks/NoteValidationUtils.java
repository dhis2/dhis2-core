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
