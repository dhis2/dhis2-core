/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class NotesConverterServiceTest extends DhisConvenienceTest {

  private static final String CURRENT_USER = "usernamea";

  private NotesConverterService notesConverterService;

  private TrackerPreheat preheat;

  private final BeanRandomizer rnd = BeanRandomizer.create();

  @BeforeEach
  void setUp() {
    this.notesConverterService = new NotesConverterService();
    User user = makeUser("A");
    this.preheat = new TrackerPreheat();
    preheat.setUser(user);
  }

  @Test
  void verifyConvertTrackerNoteToNote() {
    org.hisp.dhis.tracker.imports.domain.Note trackerNote =
        rnd.nextObject(org.hisp.dhis.tracker.imports.domain.Note.class);
    final Note note = notesConverterService.from(preheat, trackerNote);
    assertNoteValues(note, trackerNote);
  }

  @Test
  void verifyConvertTrackerNoteToNoteWithNoStoredByDefined() {
    org.hisp.dhis.tracker.imports.domain.Note trackerNote =
        rnd.nextObject(org.hisp.dhis.tracker.imports.domain.Note.class);
    trackerNote.setStoredBy(null);
    final Note note = notesConverterService.from(preheat, trackerNote);
    assertNoteValues(note, trackerNote);
  }

  @Test
  void verifyConvertTrackerNotesToNotes() {
    List<org.hisp.dhis.tracker.imports.domain.Note> trackerNotes =
        rnd.objects(org.hisp.dhis.tracker.imports.domain.Note.class, 10)
            .collect(Collectors.toList());
    final List<Note> notes = notesConverterService.from(preheat, trackerNotes);
    assertThat(notes, hasSize(10));
    for (org.hisp.dhis.tracker.imports.domain.Note note : trackerNotes) {
      assertNoteValues(
          notes.stream().filter(c -> c.getUid().equals(note.getNote())).findFirst().get(), note);
    }
  }

  @Test
  void verifyConvertNoteToTrackerNote() {
    Note note = rnd.nextObject(Note.class);
    final org.hisp.dhis.tracker.imports.domain.Note trackerNote = notesConverterService.to(note);
    assertNoteValues(trackerNote, note);
  }

  @Test
  void verifyConvertNotesToTrackerNotes() {
    List<Note> notes = rnd.objects(Note.class, 10).collect(Collectors.toList());
    final List<org.hisp.dhis.tracker.imports.domain.Note> tackerNotes =
        notesConverterService.to(notes);
    for (Note note : notes) {
      assertNoteValues(
          tackerNotes.stream().filter(n -> n.getNote().equals(note.getUid())).findFirst().get(),
          note);
    }
  }

  private void assertNoteValues(Note note, org.hisp.dhis.tracker.imports.domain.Note trackerNote) {
    assertThat(note, is(notNullValue()));
    assertThat(note.getUid(), is(trackerNote.getNote()));
    assertThat(note.getNoteText(), is(trackerNote.getValue()));
    assertThat(note.getCreator(), is(trackerNote.getStoredBy()));
    assertThat(note.getLastUpdatedBy().getUsername(), is(CURRENT_USER));
  }

  private void assertNoteValues(org.hisp.dhis.tracker.imports.domain.Note trackerNotes, Note note) {
    assertThat(trackerNotes, is(notNullValue()));
    assertThat(trackerNotes.getNote(), is(note.getUid()));
    assertThat(trackerNotes.getValue(), is(note.getNoteText()));
    assertThat(trackerNotes.getStoredBy(), is(note.getCreator()));
    assertEquals(trackerNotes.getStoredAt(), DateUtils.instantFromDate(note.getCreated()));
    assertThat(
        note.getLastUpdatedBy().getUsername(), is(trackerNotes.getCreatedBy().getUsername()));
  }
}
