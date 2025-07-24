/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.note;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.tracker.export.trackerevent.TrackerEventService;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoteServiceTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerEventService trackerEventService;

  @Autowired private SingleEventService singleEventService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private NoteService noteService;

  private UserDetails userDetails;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    userDetails = UserDetails.fromUser(importUser);
    injectSecurityContext(userDetails);

    testSetup.importTrackerData();
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
  void shouldCreateTrackerEventNote()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Note note = note();
    noteService.addNoteForEvent(note, UID.of("pTzf9KYMk72"));

    manager.clear();
    manager.flush();

    TrackerEvent dbEvent = trackerEventService.getEvent(UID.of("pTzf9KYMk72"));
    assertNotes(List.of(note), dbEvent.getNotes(), userDetails);
  }

  @Test
  void shouldCreateSingleEventNote()
      throws ForbiddenException, NotFoundException, BadRequestException {
    Note note = note();
    noteService.addNoteForEvent(note, UID.of("QRYjLTiJTrA"));

    manager.clear();
    manager.flush();

    SingleEvent dbEvent = singleEventService.getEvent(UID.of("QRYjLTiJTrA"));
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
  void shouldFailToCreateEventNoteWhenEventIsNotPresent() {
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
  void shouldFailToCreateTrackerEventNoteIfUserHasNoAccessToEvent() {
    User importUser = userService.getUser("nIidJVYpQQK");
    injectSecurityContext(UserDetails.fromUser(importUser));

    Note note = note();

    assertThrows(
        NotFoundException.class, () -> noteService.addNoteForEvent(note, UID.of("pTzf9KYMk72")));
  }

  @Test
  void shouldFailToCreateSingleEventNoteIfUserHasNoAccessToEvent() {
    User importUser = userService.getUser("nIidJVYpQQK");
    injectSecurityContext(UserDetails.fromUser(importUser));

    Note note = note();

    assertThrows(
        NotFoundException.class, () -> noteService.addNoteForEvent(note, UID.of("QRYjLTiJTrA")));
  }

  private void assertNotes(
      List<Note> notes, List<org.hisp.dhis.note.Note> dbNotes, UserDetails updatedBy) {
    for (Note note : notes) {
      org.hisp.dhis.note.Note dbNote =
          dbNotes.stream()
              .filter(n -> n.getUid().equals(note.getNote().getValue()))
              .findFirst()
              .orElse(null);
      assertNotNull(dbNote);
      assertEquals(note.getValue(), dbNote.getNoteText());
      assertEquals(note.getStoredBy(), dbNote.getCreator());
      assertEquals(updatedBy.getUid(), dbNote.getLastUpdatedBy().getUid());
      assertEquals(updatedBy.getUsername(), dbNote.getLastUpdatedBy().getUsername());
      assertEquals(updatedBy.getFirstName(), dbNote.getLastUpdatedBy().getFirstName());
      assertEquals(updatedBy.getSurname(), dbNote.getLastUpdatedBy().getSurname());
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
