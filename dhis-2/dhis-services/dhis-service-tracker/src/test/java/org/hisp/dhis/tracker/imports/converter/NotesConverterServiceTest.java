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

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luciano Fiandesio
 */
class NotesConverterServiceTest extends TestBase {

  private static final String CURRENT_USER = "usernamea";

  private NotesConverterService notesConverterService;

  private TrackerPreheat preheat;

  private UserDetails currentUser;

  private User user;

  @BeforeEach
  void setUp() {
    this.notesConverterService = new NotesConverterService();
    user = makeUser("A");
    currentUser = UserDetails.fromUser(user);

    this.preheat = new TrackerPreheat();
    this.preheat.addUsers(Set.of(user));
  }

  @Test
  void verifyConvertTrackerNoteToNote() {
    org.hisp.dhis.tracker.imports.domain.Note trackerNote = trackerNote();
    final Note note = notesConverterService.from(preheat, trackerNote, currentUser);
    assertNoteValues(note, trackerNote);
  }

  @Test
  void verifyConvertTrackerNoteToNoteWithNoStoredByDefined() {
    org.hisp.dhis.tracker.imports.domain.Note trackerNote = trackerNote();
    trackerNote.setStoredBy(null);
    final Note note = notesConverterService.from(preheat, trackerNote, currentUser);
    assertNoteValues(note, trackerNote);
  }

  @Test
  void verifyConvertTrackerNotesToNotes() {
    List<org.hisp.dhis.tracker.imports.domain.Note> trackerNotes =
        List.of(trackerNote(), trackerNote());
    final List<Note> notes = notesConverterService.from(preheat, trackerNotes, currentUser);
    assertThat(notes, hasSize(2));
    for (org.hisp.dhis.tracker.imports.domain.Note note : trackerNotes) {
      assertNoteValues(
          notes.stream().filter(c -> c.getUid().equals(note.getNote())).findFirst().get(), note);
    }
  }

  private void assertNoteValues(Note note, org.hisp.dhis.tracker.imports.domain.Note trackerNote) {
    assertThat(note, is(notNullValue()));
    assertThat(note.getUid(), is(trackerNote.getNote()));
    assertThat(note.getNoteText(), is(trackerNote.getValue()));
    assertThat(note.getCreator(), is(trackerNote.getStoredBy()));
    assertThat(note.getLastUpdatedBy().getUsername(), is(CURRENT_USER));
  }

  private org.hisp.dhis.tracker.imports.domain.Note trackerNote() {
    String uid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.imports.domain.User trackerUser =
        org.hisp.dhis.tracker.imports.domain.User.builder()
            .uid(CodeGenerator.generateUid())
            .username(CURRENT_USER)
            .build();
    return org.hisp.dhis.tracker.imports.domain.Note.builder()
        .value("Note text for note: " + uid)
        .note(uid)
        .storedAt(new Date().toInstant())
        .storedBy(CURRENT_USER)
        .createdBy(trackerUser)
        .build();
  }
}
