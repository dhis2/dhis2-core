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

import java.util.Date;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.imports.bundle.persister.PersistenceException;
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JdbcNoteStore {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public void saveEnrollmentNote(
      @Nonnull UID enrollment, @Nonnull Note note, @Nonnull UserDetails user) {
    long noteId = saveNote(note, user);
    String sql =
        """
                INSERT INTO enrollment_notes(enrollmentid, noteid, sort_order)
                VALUES ((select enrollmentid from enrollment where uid = :enrollment),
                        :noteId,
                        coalesce(
                                    (select max(sort_order) + 1
                                    from enrollment_notes
                                    where enrollmentid = (select enrollmentid from enrollment where uid = :enrollment)
                                    ),
                                1)
                        )
            """;
    jdbcTemplate.update(sql, Map.of("enrollment", enrollment.getValue(), "noteId", noteId));
  }

  public void saveTrackerEventNote(
      @Nonnull UID event, @Nonnull Note note, @Nonnull UserDetails user) {
    long noteId = saveNote(note, user);
    String sql =
        """
            INSERT INTO trackerevent_notes(eventid, noteid, sort_order)
            VALUES ((select eventid from trackerevent where uid = :event),
                    :noteId,
                    coalesce(
                                (select max(sort_order) + 1
                                from trackerevent_notes
                                where eventid = (select eventid from trackerevent where uid = :event)
                                ),
                            1)
                    )
        """;
    jdbcTemplate.update(sql, Map.of("event", event.getValue(), "noteId", noteId));
  }

  public void saveSingleEventNote(
      @Nonnull UID event, @Nonnull Note note, @Nonnull UserDetails user) {
    long noteId = saveNote(note, user);
    String sql =
        """
                INSERT INTO singleevent_notes(eventid, noteid, sort_order)
                VALUES ((select eventid from singleevent where uid = :event),
                        :noteId,
                        coalesce(
                                    (select max(sort_order) + 1
                                    from singleevent_notes
                                    where eventid = (select eventid from singleevent where uid = :event)
                                    ),
                                1)
                        )
            """;
    jdbcTemplate.update(sql, Map.of("event", event.getValue(), "noteId", noteId));
  }

  boolean exists(@Nonnull UID note) {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(1) from note where uid = :uid",
            Map.of("uid", note.getValue()),
            Integer.class);
    return count != null && count > 0;
  }

  private long saveNote(@Nonnull Note note, @Nonnull UserDetails user) {
    String sql =
        """
            INSERT INTO public.note(noteid, notetext, creator, lastupdatedby, uid, created)
            VALUES (nextVal('note_sequence'),
                    :text,
                    :creator,
                    (select userinfoid from userinfo where uid = :lastUpdatedBy),
                    :uid,
                    :created)
            RETURNING noteid
        """;

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("text", note.getValue());
    params.addValue("creator", note.getStoredBy());
    params.addValue("lastUpdatedBy", user.getUid());
    params.addValue("uid", note.getNote().getValue());
    params.addValue("created", new Date());

    Long noteId = jdbcTemplate.queryForObject(sql, params, Long.class);

    if (noteId == null) {
      throw new PersistenceException("Note could not be saved");
    }

    return noteId;
  }
}
