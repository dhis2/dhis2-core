package org.hisp.dhis.tracker.converter;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.util.DateUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

/**
 * @author Luciano Fiandesio
 */
public class NotesConverterServiceTest
{
    private NotesConverterService notesConverterService;

    private BeanRandomizer rnd;

    @Before
    public void setUp()
    {
        this.notesConverterService = new NotesConverterService();
        rnd = new BeanRandomizer();
    }

    @Test
    public void verifyConvertCommentToNote()
    {
        Note note = rnd.randomObject( Note.class );

        final TrackedEntityComment comment = notesConverterService.from( note );
        assertNoteValues( comment, note );
    }

    @Test
    public void verifyConvertCommentsToNotes()
    {
        List<Note> notes = rnd.randomObjects( Note.class, 10 );

        final List<TrackedEntityComment> comments = notesConverterService.from( notes );

        assertThat( comments, hasSize( 10 ) );

        for ( Note note : notes )
        {
            assertNoteValues( comments.stream().filter( c -> c.getUid().equals( note.getNote() ) ).findFirst().get(),
                note );
        }
    }

    @Test
    public void verifyConvertNoteToComment()
    {
        TrackedEntityComment comment = rnd.randomObject( TrackedEntityComment.class );

        final Note note = notesConverterService.to( comment );

        assertCommentValues(note, comment);
    }

    @Test
    public void verifyConvertNotesToComments()
    {
        List<TrackedEntityComment> comments = rnd.randomObjects( TrackedEntityComment.class, 10 );

        final List<Note> notes = notesConverterService.to( comments );

        for ( TrackedEntityComment comment : comments )
        {
            assertCommentValues( notes.stream().filter( n -> n.getNote().equals( comment.getUid() ) ).findFirst().get(),
                comment );
        }
    }

    private void assertNoteValues( TrackedEntityComment comment, Note note )
    {
        assertThat( comment, is( notNullValue() ) );
        assertThat( comment.getUid(), is( note.getNote() ) );
        assertThat( comment.getCommentText(), is( note.getValue() ) );
        // assertThat( comment.getCreator(), is( note.getStoredBy() ) ); // TODO check
        // if this is needed
    }

    private void assertCommentValues( Note note, TrackedEntityComment comment  )
    {
        assertThat( note, is( notNullValue() ) );
        assertThat( note.getNote(), is( comment.getUid() ) );
        assertThat( note.getValue(), is( comment.getCommentText() ) );
        assertThat( note.getStoredBy(), is( comment.getCreator() ) );
        assertThat( note.getStoredAt(), is( DateUtils.getIso8601NoTz( comment.getCreated() ) ) );
    }
}