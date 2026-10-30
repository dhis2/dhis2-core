/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.tracker.export.relationship;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.tracker.export.JdbcNotes;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads the notes of enrollments and events by their UID. Notes are not mapped by Hibernate, so the
 * relationship export uses this to get the notes of the enrollments and events it loads via
 * Hibernate.
 */
@Repository
@RequiredArgsConstructor
class JdbcNoteReader {
  // Built once from constants so each query has a single cacheable statement.
  private static final String ENROLLMENT_NOTES =
      query("enrollment", "enrollmentid", "enrollmentid");
  private static final String TRACKER_EVENT_NOTES =
      query("trackerevent", "eventid", "trackereventid");
  private static final String SINGLE_EVENT_NOTES = query("singleevent", "eventid", "singleeventid");

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * @return notes keyed by enrollment UID, enrollments without notes are absent
   */
  @Nonnull
  Map<String, List<Note>> findEnrollmentNotes(@Nonnull Collection<String> enrollments) {
    return find(ENROLLMENT_NOTES, enrollments);
  }

  /**
   * @return notes keyed by tracker event UID, events without notes are absent
   */
  @Nonnull
  Map<String, List<Note>> findTrackerEventNotes(@Nonnull Collection<String> events) {
    return find(TRACKER_EVENT_NOTES, events);
  }

  /**
   * @return notes keyed by single event UID, events without notes are absent
   */
  @Nonnull
  Map<String, List<Note>> findSingleEventNotes(@Nonnull Collection<String> events) {
    return find(SINGLE_EVENT_NOTES, events);
  }

  private static String query(String entityTable, String entityIdColumn, String noteColumn) {
    return "select e.uid, notes.jsonnotes as notes from "
        + entityTable
        + " e "
        + JdbcNotes.leftJoinLateral(noteColumn, "e." + entityIdColumn)
        + " where e.uid = any(:uids)";
  }

  private Map<String, List<Note>> find(String sql, Collection<String> uids) {
    if (uids.isEmpty()) {
      return Map.of();
    }

    Map<String, List<Note>> result = new HashMap<>();
    jdbcTemplate.query(
        sql,
        Map.of("uids", uids.toArray(String[]::new)),
        rs -> {
          List<Note> notes = JdbcNotes.fromJson(rs.getString("notes"));
          if (notes != null) {
            result.put(rs.getString("uid"), notes);
          }
        });
    return result;
  }
}
