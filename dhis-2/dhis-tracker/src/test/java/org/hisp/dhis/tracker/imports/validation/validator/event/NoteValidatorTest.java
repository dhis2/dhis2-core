/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1119;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasWarning;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class NoteValidatorTest {

  // Class under test
  private NoteValidator validator;

  private Event event;

  private TrackerBundle bundle;

  private TrackerPreheat preheat;

  private Reporter reporter;

  @BeforeEach
  void setUp() {
    this.validator = new NoteValidator();
    event = TrackerEvent.builder().event(UID.generate()).build();

    bundle = mock(TrackerBundle.class);
    preheat = mock(TrackerPreheat.class);
    when(bundle.getPreheat()).thenReturn(preheat);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void testNoteWithExistingUidWarnings() {
    // Given
    final Note note = note();
    when(preheat.hasNote(note.getNote())).thenReturn(true);

    event.setNotes(Collections.singletonList(note));

    // When
    validator.validate(reporter, bundle, event);

    // Then
    assertHasWarning(reporter, event, E1119);
    assertIsEmpty(event.getNotes());
  }

  @Test
  void testNoteWithExistingUidAndNoTextIsIgnored() {
    // Given
    final Note note = note();
    note.setValue(null);

    event.setNotes(Collections.singletonList(note));

    // When
    validator.validate(reporter, bundle, event);

    // Then
    assertIsEmpty(reporter.getErrors());
    assertIsEmpty(event.getNotes());
  }

  @Test
  void testNotesAreValidWhenUidDoesNotExist() {
    // Given
    Note note = note();
    Note note2 = note();
    final List<Note> notes = List.of(note, note2);

    event.setNotes(notes);

    // When
    validator.validate(reporter, bundle, event);

    // Then
    assertIsEmpty(reporter.getErrors());
    assertThat(event.getNotes(), hasSize(2));
  }

  private Note note() {
    return Note.builder().note(UID.generate()).value(UID.generate().getValue()).build();
  }
}
