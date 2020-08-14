package org.hisp.dhis.tracker.converter;

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

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
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

    private TrackerPreheat preheat;

    private BeanRandomizer rnd;

    @Before
    public void setUp()
    {
        this.notesConverterService = new NotesConverterService();
        this.preheat = new TrackerPreheat();
        rnd = new BeanRandomizer();
    }

    @Test
    public void verifyConvertCommentToNote()
    {
        Note note = rnd.randomObject( Note.class );

        final TrackedEntityComment comment = notesConverterService.from( preheat, note );
        assertNoteValues( comment, note );
    }

    @Test
    public void verifyConvertCommentsToNotes()
    {
        List<Note> notes = rnd.randomObjects( Note.class, 10 );

        final List<TrackedEntityComment> comments = notesConverterService.from( preheat, notes );

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

        assertCommentValues( note, comment );
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

    private void assertCommentValues( Note note, TrackedEntityComment comment )
    {
        assertThat( note, is( notNullValue() ) );
        assertThat( note.getNote(), is( comment.getUid() ) );
        assertThat( note.getValue(), is( comment.getCommentText() ) );
        assertThat( note.getStoredBy(), is( comment.getCreator() ) );
        assertThat( note.getStoredAt(), is( DateUtils.getIso8601NoTz( comment.getCreated() ) ) );
    }
}