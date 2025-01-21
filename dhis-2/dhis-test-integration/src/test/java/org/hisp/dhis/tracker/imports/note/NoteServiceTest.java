/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.note;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteServiceTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private EventService eventService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private NoteService noteService;

  private UserDetails userDetails;

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    User importUser = userService.getUser("tTgjgobT1oS");
    userDetails = UserDetails.fromUser(importUser);
    injectSecurityContext(userDetails);

    TrackerImportParams params = TrackerImportParams.builder().build();
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/event_and_enrollment.json")));
  }

  @BeforeEach
  void initUser() {
    injectSecurityContext(userDetails);
  }

  @Test
  void shouldCreateEnrollmentNote()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Note note = note();
    noteService.addNoteForEnrollment(note, UID.of("nxP7UnKhomJ"));

    manager.clear();
    manager.flush();

    Enrollment dbEnrollment = enrollmentService.getEnrollment(UID.of("nxP7UnKhomJ"));
    assertNotes(List.of(note), dbEnrollment.getNotes(), userDetails);
  }

  @Test
  void shouldFailToCreateEnrollmentNoteWhenNoteValueIsNull() {
    Note note = note();
    note.setValue(null);

    assertThrows(
        BadRequestException.class,
        () -> noteService.addNoteForEnrollment(note, UID.of("nxP7UnKhomJ")));
  }

  @Test
  void shouldFailToCreateEnrollmentNoteWhenEnrollmentIsNotPresent() {
    Note note = note();
    assertThrows(
        NotFoundException.class,
        () -> noteService.addNoteForEnrollment(note, UID.of("jPP9AnKh34U")));
  }

  @Test
  void shouldFailToCreateDuplicateEnrollmentNote()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Note note = note();
    noteService.addNoteForEnrollment(note, UID.of("nxP8UnKhomJ"));
    assertThrows(
        BadRequestException.class,
        () -> noteService.addNoteForEnrollment(note, UID.of("nxP8UnKhomJ")));
  }

  @Test
  void shouldFailToCreateEnrollmentNoteIfUserHasNoAccessToEnrollment() {
    User importUser = userService.getUser("nIidJVYpQQK");
    injectSecurityContext(UserDetails.fromUser(importUser));

    Note note = note();

    assertThrows(
        NotFoundException.class,
        () -> noteService.addNoteForEnrollment(note, UID.of("nxP7UnKhomJ")));
  }

  @Test
  void shouldCreateEventNote() throws ForbiddenException, NotFoundException, BadRequestException {
    Note note = note();
    noteService.addNoteForEvent(note, UID.of("pTzf9KYMk72"));

    manager.clear();
    manager.flush();

    Event dbEvent = eventService.getEvent(UID.of("pTzf9KYMk72"));
    assertNotes(List.of(note), dbEvent.getNotes(), userDetails);
  }

  @Test
  void shouldFailToCreateEventNoteWhenNoteValueIsNull() {
    Note note = note();
    note.setValue(null);

    assertThrows(
        BadRequestException.class, () -> noteService.addNoteForEvent(note, UID.of("pTzf9KYMk72")));
  }

  @Test
  void shouldFailToCreateEventNoteWhenEnrollmentIsNotPresent() {
    Note note = note();
    assertThrows(
        NotFoundException.class, () -> noteService.addNoteForEvent(note, UID.of("jPP9AnKh34U")));
  }

  @Test
  void shouldFailToCreateDuplicateEventNote()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Note note = note();
    noteService.addNoteForEvent(note, UID.of("D9PbzJY8bJM"));
    assertThrows(
        BadRequestException.class, () -> noteService.addNoteForEvent(note, UID.of("D9PbzJY8bJM")));
  }

  @Test
  void shouldFailToCreateEventNoteIfUserHasNoAccessToEvent() {
    User importUser = userService.getUser("nIidJVYpQQK");
    injectSecurityContext(UserDetails.fromUser(importUser));

    Note note = note();

    assertThrows(
        NotFoundException.class, () -> noteService.addNoteForEvent(note, UID.of("pTzf9KYMk72")));
  }

  private void assertNotes(
      List<Note> notes, List<org.hisp.dhis.note.Note> dbNotes, UserDetails updatedBy) {
    for (org.hisp.dhis.tracker.imports.domain.Note note : notes) {
      org.hisp.dhis.note.Note dbNote =
          dbNotes.stream()
              .filter(n -> n.getUid().equals(note.getNote().getValue()))
              .findFirst()
              .orElse(null);
      assertNotNull(dbNote);
      assertEquals(note.getValue(), dbNote.getNoteText());
      assertEquals(note.getStoredBy(), dbNote.getCreator());
      assertEquals(updatedBy.getUid(), dbNote.getLastUpdatedBy().getUid());
    }
  }

  private Note note() {
    return Note.builder()
        .note(UID.generate())
        .storedBy("This is the creator")
        .value("This is a note")
        .build();
  }
}
