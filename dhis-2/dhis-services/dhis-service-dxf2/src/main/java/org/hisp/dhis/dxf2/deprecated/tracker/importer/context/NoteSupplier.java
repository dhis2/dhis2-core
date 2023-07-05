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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component("workContextNotesSupplier")
public class NoteSupplier extends AbstractSupplier<Map<String, Note>> {
  public NoteSupplier(NamedParameterJdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public Map<String, Note> get(ImportOptions importOptions, List<Event> events) {
    Map<String, Note> persistableNotes = new HashMap<>();
    //
    // Collects all the notes' UID
    //
    // @formatter:off
    Set<String> notesUid =
        events.stream()
            .map(Event::getNotes)
            .flatMap(Collection::stream)
            .map(Note::getNote)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    // @formatter:on

    if (isNotEmpty(notesUid)) {
      final String sql = "select uid from trackedentitycomment where uid in  (:ids)";

      MapSqlParameterSource parameters = new MapSqlParameterSource();
      parameters.addValue("ids", notesUid);

      List<String> foundNotes = new ArrayList<>();

      //
      // finds all the notes that EXIST in the DB (by uid)
      //
      jdbcTemplate.query(
          sql,
          parameters,
          (ResultSet rs) -> {
            while (rs.next()) {
              foundNotes.add(rs.getString("uid"));
            }
          });

      for (Event event : events) {
        // @formatter:off
        List<Note> eventNotes =
            event.getNotes().stream()
                .filter(u -> !foundNotes.contains(u.getNote()))
                .collect(Collectors.toList());
        // @formatter:on
        if (isNotEmpty(eventNotes)) {
          persistableNotes.putAll(
              eventNotes.stream().collect(Collectors.toMap(Note::getNote, Function.identity())));
        }
      }
    }
    return persistableNotes;
  }
}
