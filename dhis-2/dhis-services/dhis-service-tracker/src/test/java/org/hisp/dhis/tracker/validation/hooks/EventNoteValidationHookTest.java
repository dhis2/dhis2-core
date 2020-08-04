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

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class EventNoteValidationHookTest
{
    // Class under test
    private EventNoteValidationHook hook;

    @Mock
    private TrackedEntityCommentService commentService;

    @Mock
    private TrackedEntityAttributeService teAttrService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd;

    private Event event;

    @Before
    public void setUp()
    {
        this.hook = new EventNoteValidationHook( teAttrService, commentService );
        rnd = new BeanRandomizer();
        event = rnd.randomObject( Event.class );
    }

    @Test
    public void verifyAllNotesArePersistedWhenNotesAreNotOnDatabase()
    {
        // Given
        final List<Note> notes = rnd.randomObjects( Note.class, 5, "newNote" );
        final List<String> notesUid = notes.stream().map( Note::getNote ).collect( Collectors.toList() );

        event.setNotes( notes );

        // When
        when( commentService.filterExistingNotes( notesUid ) ).thenReturn( notesUid );
        this.hook.validateEvent( mock( ValidationErrorReporter.class ), event );

        // Then
        assertThat( event.getNotes(), hasSize( 5 ) );
    }

    @Test
    public void verifyAllNewNotesArePersisted()
    {
        // Given
        final List<Note> notes = rnd.randomObjects( Note.class, 5 );
        makeAllNotesNew( notes );
        final List<String> notesUid = notes.stream().map( Note::getNote ).collect( Collectors.toList() );

        event.setNotes( notes );

        // When
        when( commentService.filterExistingNotes( notesUid ) ).thenReturn( new ArrayList<>() );
        this.hook.validateEvent( mock( ValidationErrorReporter.class ), event );

        // Then
        assertThat( event.getNotes(), hasSize( 5 ) );
    }

    @Test
    public void verifyOnlyNonExistingNoteArePersisted()
    {
        // Given
        final List<Note> notes = rnd.randomObjects( Note.class, 3, "newNote" );
        final List<Note> existingNotes = rnd.randomObjects( Note.class, 2, "newNote" );

        event.setNotes( newArrayList( concat( notes, existingNotes ) ) );

        // When
        when( commentService.filterExistingNotes( anyList() ) )
            .thenReturn( notes.stream().map( Note::getNote ).collect( Collectors.toList() ) );

        this.hook.validateEvent( mock( ValidationErrorReporter.class ), event );

        // Then
        assertThat( event.getNotes(), hasSize( 3 ) );
    }

    @Test
    public void verifyOnlyNonExistingAndNonEmptyNoteArePersisted()
    {
        // Given
        ArgumentCaptor<List<String>> argument = ArgumentCaptor.forClass( List.class );

        final List<Note> notes = rnd.randomObjects( Note.class, 5, "newNote" );
        final List<Note> emptyNotes = rnd.randomObjects( Note.class, 3, "newNote", "value" );
        final List<Note> existingNotes = rnd.randomObjects( Note.class, 2, "newNote" );

        event.setNotes( newArrayList( concat( notes, emptyNotes, existingNotes ) ) );

        // When
        when( commentService.filterExistingNotes( argument.capture() ) ).thenReturn(
                notes.stream().map( Note::getNote ).collect( Collectors.toList() ) );

        this.hook.validateEvent( mock( ValidationErrorReporter.class ), event );

        // Then
        assertThat( event.getNotes(), hasSize( 5 ) );
        List<String> value = argument.getValue();
        // make sure that the filterExistingNotes was called only with uid belonging to notes with text
        assertThat( value, containsInAnyOrder( newArrayList( concat( notes, existingNotes ) ).stream()
            .map( Note::getNote ).toArray() ) );

    }

    private void makeAllNotesNew( List<Note> notes )
    {
        notes.forEach( n -> n.setNewNote( true ) );
    }
}