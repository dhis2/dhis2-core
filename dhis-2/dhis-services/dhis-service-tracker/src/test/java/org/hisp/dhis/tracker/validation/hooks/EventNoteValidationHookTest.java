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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class EventNoteValidationHookTest
{
    // Class under test
    private EventNoteValidationHook hook;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd;

    private Event event;

    @Before
    public void setUp()
    {
        this.hook = new EventNoteValidationHook();
        rnd = new BeanRandomizer();
        event = rnd.randomObject( Event.class );
    }

    @Test
    public void testNoteWithExistingUidWarnings()
    {
        // Given
        final Note note = rnd.randomObject( Note.class );

        TrackerBundle trackerBundle = mock( TrackerBundle.class );
        TrackerImportValidationContext ctx = mock( TrackerImportValidationContext.class );
        TrackerPreheat preheat = mock( TrackerPreheat.class );
        when( ctx.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( trackerBundle.getPreheat() ).thenReturn( preheat );
        when( ctx.getNote( note.getNote() ) ).thenReturn( Optional.of( new TrackedEntityComment() ) );
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );

        event.setNotes( Collections.singletonList( note ) );

        // When
        this.hook.validateEvent( reporter, event );

        // Then
        assertTrue( reporter.hasWarnings() );
        assertThat( reporter.getWarningsReportList().get( 0 ).getWarningCode(), is( TrackerErrorCode.E1119 ) );
        assertThat( event.getNotes(), hasSize( 0 ) );
    }

    @Test
    public void testNoteWithExistingUidAndNoTextIsIgnored()
    {
        // Given
        final Note note = rnd.randomObject( Note.class );
        note.setValue( null );
        TrackerBundle trackerBundle = mock( TrackerBundle.class );
        TrackerImportValidationContext ctx = mock( TrackerImportValidationContext.class );

        when( ctx.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( ctx.getNote( note.getNote() ) ).thenReturn( Optional.of( new TrackedEntityComment() ) );
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );

        event.setNotes( Collections.singletonList( note ) );

        // When
        this.hook.validateEvent( reporter, event );

        // Then
        assertFalse( reporter.hasErrors() );
        assertThat( event.getNotes(), hasSize( 0 ) );
    }

    @Test
    public void testNotesAreValidWhenUidDoesNotExist()
    {
        // Given
        final List<Note> notes = rnd.randomObjects( Note.class, 5 );
        TrackerBundle trackerBundle = mock( TrackerBundle.class );
        TrackerImportValidationContext ctx = mock( TrackerImportValidationContext.class );

        when( ctx.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );

        event.setNotes( notes );

        // When
        this.hook.validateEvent( reporter, event );

        // Then
        assertFalse( reporter.hasErrors() );
        assertThat( event.getNotes(), hasSize( 5 ) );
    }

}