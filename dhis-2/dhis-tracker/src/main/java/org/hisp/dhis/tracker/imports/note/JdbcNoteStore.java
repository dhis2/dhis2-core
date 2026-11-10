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
import org.hisp.dhis.tracker.imports.domain.Note;
import org.hisp.dhis.user.UserDetails;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JdbcNoteStore {
  private static final String INSERT_ENROLLMENT_NOTE =
      """
      insert into note (notetext, lastupdatedby, uid, created, enrollmentid)
      values (:text,
              (select userinfoid from userinfo where uid = :lastUpdatedBy),
              :uid,
              :created,
              (select enrollmentid from enrollment where uid = :entity))
      """;

  private static final String INSERT_TRACKER_EVENT_NOTE =
      """
      insert into note (notetext, lastupdatedby, uid, created, trackereventid)
      values (:text,
              (select userinfoid from userinfo where uid = :lastUpdatedBy),
              :uid,
              :created,
              (select eventid from trackerevent where uid = :entity))
      """;

  private static final String INSERT_SINGLE_EVENT_NOTE =
      """
      insert into note (notetext, lastupdatedby, uid, created, singleeventid)
      values (:text,
              (select userinfoid from userinfo where uid = :lastUpdatedBy),
              :uid,
              :created,
              (select eventid from singleevent where uid = :entity))
      """;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  void saveEnrollmentNote(@Nonnull UID enrollment, @Nonnull Note note, @Nonnull UserDetails user) {
    saveNote(INSERT_ENROLLMENT_NOTE, enrollment, note, user);
  }

  void saveTrackerEventNote(@Nonnull UID event, @Nonnull Note note, @Nonnull UserDetails user) {
    saveNote(INSERT_TRACKER_EVENT_NOTE, event, note, user);
  }

  void saveSingleEventNote(@Nonnull UID event, @Nonnull Note note, @Nonnull UserDetails user) {
    saveNote(INSERT_SINGLE_EVENT_NOTE, event, note, user);
  }

  boolean exists(@Nonnull UID note) {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(1) from note where uid = :uid",
            Map.of("uid", note.getValue()),
            Integer.class);
    return count != null && count > 0;
  }

  private void saveNote(
      @Nonnull String sql, @Nonnull UID entity, @Nonnull Note note, @Nonnull UserDetails user) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("text", note.getValue());
    params.addValue("lastUpdatedBy", user.getUid());
    params.addValue("uid", note.getNote().getValue());
    params.addValue("created", new Date());
    params.addValue("entity", entity.getValue());

    jdbcTemplate.update(sql, params);
  }
}
