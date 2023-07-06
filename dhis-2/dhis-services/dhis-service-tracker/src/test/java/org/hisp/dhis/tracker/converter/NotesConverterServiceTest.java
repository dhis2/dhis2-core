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
package org.hisp.dhis.tracker.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
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
  void verifyConvertCommentToNote() {
    Note note = rnd.nextObject(Note.class);
    final TrackedEntityComment comment = notesConverterService.from(preheat, note);
    assertNoteValues(comment, note);
  }

  @Test
  void verifyConvertCommentToNoteWithNoStoredByDefined() {
    Note note = rnd.nextObject(Note.class);
    note.setStoredBy(null);
    final TrackedEntityComment comment = notesConverterService.from(preheat, note);
    assertNoteValues(comment, note);
  }

  @Test
  void verifyConvertCommentsToNotes() {
    List<Note> notes = rnd.objects(Note.class, 10).collect(Collectors.toList());
    final List<TrackedEntityComment> comments = notesConverterService.from(preheat, notes);
    assertThat(comments, hasSize(10));
    for (Note note : notes) {
      assertNoteValues(
          comments.stream().filter(c -> c.getUid().equals(note.getNote())).findFirst().get(), note);
    }
  }

  @Test
  void verifyConvertNoteToComment() {
    TrackedEntityComment comment = rnd.nextObject(TrackedEntityComment.class);
    final Note note = notesConverterService.to(comment);
    assertCommentValues(note, comment);
  }

  @Test
  void verifyConvertNotesToComments() {
    List<TrackedEntityComment> comments =
        rnd.objects(TrackedEntityComment.class, 10).collect(Collectors.toList());
    final List<Note> notes = notesConverterService.to(comments);
    for (TrackedEntityComment comment : comments) {
      assertCommentValues(
          notes.stream().filter(n -> n.getNote().equals(comment.getUid())).findFirst().get(),
          comment);
    }
  }

  private void assertNoteValues(TrackedEntityComment comment, Note note) {
    assertThat(comment, is(notNullValue()));
    assertThat(comment.getUid(), is(note.getNote()));
    assertThat(comment.getCommentText(), is(note.getValue()));
    assertThat(comment.getCreator(), is(note.getStoredBy()));
    assertThat(comment.getLastUpdatedBy().getUsername(), is(CURRENT_USER));
  }

  private void assertCommentValues(Note note, TrackedEntityComment comment) {
    assertThat(note, is(notNullValue()));
    assertThat(note.getNote(), is(comment.getUid()));
    assertThat(note.getValue(), is(comment.getCommentText()));
    assertThat(note.getStoredBy(), is(comment.getCreator()));
    assertEquals(note.getStoredAt(), DateUtils.instantFromDate(comment.getCreated()));
    assertThat(comment.getLastUpdatedBy().getUsername(), is(note.getCreatedBy().getUsername()));
  }
}
