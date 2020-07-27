package org.hisp.dhis.tracker.validation.hooks;

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