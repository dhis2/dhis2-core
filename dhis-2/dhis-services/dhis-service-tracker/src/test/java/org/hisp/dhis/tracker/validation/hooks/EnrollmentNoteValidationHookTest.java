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
import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1119;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EnrollmentNoteValidationHookTest
{

    // Class under test
    private EnrollmentNoteValidationHook hook;

    private Enrollment enrollment;

    private final BeanRandomizer rnd = BeanRandomizer.create();

    @BeforeEach
    public void setUp()
    {
        this.hook = new EnrollmentNoteValidationHook();
        enrollment = rnd.nextObject( Enrollment.class );
    }

    @Test
    void testNoteWithExistingUidWarnings()
    {
        // Given
        final Note note = rnd.nextObject( Note.class );

        TrackerBundle trackerBundle = mock( TrackerBundle.class );
        TrackerImportValidationContext ctx = mock( TrackerImportValidationContext.class );
        TrackerPreheat preheat = mock( TrackerPreheat.class );
        when( ctx.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( trackerBundle.getPreheat() ).thenReturn( preheat );
        when( ctx.getNote( note.getNote() ) ).thenReturn( Optional.of( new TrackedEntityComment() ) );
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        enrollment.setNotes( Collections.singletonList( note ) );

        // When
        this.hook.validateEnrollment( reporter, enrollment );

        // Then
        assertTrue( reporter.hasWarnings() );
        assertTrue( reporter.hasWarningReport( warn -> E1119.equals( warn.getWarningCode() ) &&
            ENROLLMENT.equals( warn.getTrackerType() ) &&
            enrollment.getUid().equals( warn.getUid() ) ) );
        assertThat( enrollment.getNotes(), hasSize( 0 ) );
    }

    @Test
    void testNoteWithExistingUidAndNoTextIsIgnored()
    {
        // Given
        final Note note = rnd.nextObject( Note.class );
        note.setValue( null );
        TrackerBundle trackerBundle = mock( TrackerBundle.class );
        TrackerImportValidationContext ctx = mock( TrackerImportValidationContext.class );

        when( ctx.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( ctx.getNote( note.getNote() ) ).thenReturn( Optional.of( new TrackedEntityComment() ) );
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        enrollment.setNotes( Collections.singletonList( note ) );

        // When
        this.hook.validateEnrollment( reporter, enrollment );

        // Then
        assertFalse( reporter.hasErrors() );
        assertThat( enrollment.getNotes(), hasSize( 0 ) );
    }

    @Test
    void testNotesAreValidWhenUidDoesNotExist()
    {
        // Given
        final List<Note> notes = rnd.objects( Note.class, 5 ).collect( Collectors.toList() );
        TrackerBundle trackerBundle = mock( TrackerBundle.class );
        TrackerImportValidationContext ctx = mock( TrackerImportValidationContext.class );

        when( ctx.getBundle() ).thenReturn( trackerBundle );
        when( trackerBundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );

        enrollment.setNotes( notes );

        // When
        this.hook.validateEnrollment( reporter, enrollment );

        // Then
        assertFalse( reporter.hasErrors() );
        assertThat( enrollment.getNotes(), hasSize( 5 ) );
    }

}
