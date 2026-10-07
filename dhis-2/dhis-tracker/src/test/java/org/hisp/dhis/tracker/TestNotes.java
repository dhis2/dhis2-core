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
package org.hisp.dhis.tracker;

import java.util.Date;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Saves notes of enrollments and events in tests. Notes are not mapped by Hibernate, so saving an
 * enrollment or event does not save its notes. Production code writes them via {@code NoteWriter}
 * and {@code JdbcNoteStore}.
 *
 * <p>Each method flushes first, so an enrollment or event saved via Hibernate in the same
 * transaction exists for the note's foreign key. Works with and without a test transaction.
 */
@Component
@RequiredArgsConstructor
public class TestNotes {
  private final IdentifiableObjectManager manager;

  private final JdbcTemplate jdbcTemplate;

  /** Saves a note with the given text and no {@code lastUpdatedBy}. */
  @Nonnull
  public Note save(@Nonnull Enrollment enrollment, @Nonnull String text) {
    return save("enrollmentid", enrollment.getId(), text);
  }

  /** Saves a note with the given text and no {@code lastUpdatedBy}. */
  @Nonnull
  public Note save(@Nonnull TrackerEvent event, @Nonnull String text) {
    return save("trackereventid", event.getId(), text);
  }

  /** Saves a note with the given text and no {@code lastUpdatedBy}. */
  @Nonnull
  public Note save(@Nonnull SingleEvent event, @Nonnull String text) {
    return save("singleeventid", event.getId(), text);
  }

  private Note save(String noteColumn, long entityId, String text) {
    manager.flush();
    Note note = new Note(text);
    note.setUid(UID.generate().getValue());
    note.setCreated(new Date());
    jdbcTemplate.update(
        "insert into note (uid, notetext, created, " + noteColumn + ") values (?, ?, ?, ?)",
        note.getUid(),
        note.getNoteText(),
        note.getCreated(),
        entityId);
    return note;
  }
}
