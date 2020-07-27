package org.hisp.dhis.tracker.converter;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

/**
 * @author Luciano Fiandesio
 */
@Service
public class NotesConverterService implements TrackerConverterService<Note, TrackedEntityComment>
{
    @Override
    public Note to( TrackedEntityComment trackedEntityComment )
    {
        Note note = new Note();
        note.setNote( trackedEntityComment.getUid() );
        note.setValue( trackedEntityComment.getCommentText() );
        note.setStoredAt( DateUtils.getIso8601NoTz( trackedEntityComment.getCreated() ) );
        note.setStoredBy( trackedEntityComment.getCreator() );
        return note;
    }

    @Override
    public List<Note> to( List<TrackedEntityComment> trackedEntityComments )
    {
        return trackedEntityComments.stream().map( this::to ).collect( Collectors.toList() );
    }

    @Override
    public TrackedEntityComment from( Note note )
    {
        TrackedEntityComment comment = new TrackedEntityComment();
        comment.setAutoFields();
        comment.setCommentText( note.getValue() );
        comment.setUid( note.getNote() );

        // FIXME: what about the storedBy and lastUpdatedBy -> currently they are set to null
        return comment;
    }

    @Override
    public TrackedEntityComment from( TrackerPreheat preheat, Note note )
    {
        return from( note );
    }

    @Override
    public List<TrackedEntityComment> from( List<Note> notes )
    {
        return notes.stream().map( this::from ).collect( Collectors.toList() );
    }
}
