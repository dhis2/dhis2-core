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
package org.hisp.dhis.tracker.imports.bundle.persister;

import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.bigintArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.forEachChunk;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.textArray;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamptz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import org.hisp.dhis.note.Note;

/**
 * Inserts the new {@link Note}s of the enrollments, tracker events or single events written in one
 * flush, with the entity's id in the column referencing it ({@code enrollmentid} / {@code
 * trackereventid} / {@code singleeventid}). The database assigns {@code noteid}.
 *
 * <p>Notes are append-only. An entity's notes list only holds the notes of its payload, and
 * validation already dropped the ones whose UID exists.
 *
 * <p>One instance is configured per entity type; the three entity writers each compose one.
 */
final class NoteWriter {

  /** A new {@link Note} to insert for a given entity row. */
  record NoteRow(long entityId, Note note) {}

  // Constant per entity type, built once here so each NoteWriter has a single cacheable
  // INSERT ... SELECT unnest(...) statement regardless of row count.
  private final String insertSql;

  NoteWriter(String noteColumn) {
    this.insertSql =
        "insert into note (uid, created, lastupdatedby, notetext, "
            + noteColumn
            + ")"
            + " select uid, created, lastupdatedby, notetext, entity_id"
            + " from ( select"
            + " unnest(?::text[]) as uid,"
            + " unnest(?::timestamptz[]) as created,"
            + " unnest(?::bigint[]) as lastupdatedby,"
            + " unnest(?::text[]) as notetext,"
            + " unnest(?::bigint[]) as entity_id"
            + " ) v";
  }

  /**
   * Inserts the notes of the given inserted and updated entities. {@code entityId} extracts the
   * entity's pre-allocated/persisted id; {@code notesGetter} returns the entity's notes list (may
   * be {@code null}).
   */
  <E> void write(
      Connection conn,
      List<E> inserts,
      List<E> updates,
      ToLongFunction<E> entityId,
      Function<E, List<Note>> notesGetter)
      throws SQLException {
    List<NoteRow> rows = new ArrayList<>();
    collect(inserts, entityId, notesGetter, rows);
    collect(updates, entityId, notesGetter, rows);
    if (rows.isEmpty()) {
      return;
    }

    insertNoteRows(conn, rows);
  }

  private static <E> void collect(
      List<E> entities,
      ToLongFunction<E> entityId,
      Function<E, List<Note>> notesGetter,
      List<NoteRow> out) {
    for (E entity : entities) {
      List<Note> notes = notesGetter.apply(entity);
      if (notes == null) {
        continue;
      }
      for (Note n : notes) {
        out.add(new NoteRow(entityId.applyAsLong(entity), n));
      }
    }
  }

  private void insertNoteRows(Connection conn, List<NoteRow> rows) throws SQLException {
    forEachChunk(
        rows,
        chunk -> {
          try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            int p = 1;
            ps.setArray(p++, textArray(conn, chunk, nr -> nr.note().getUid()));
            ps.setArray(p++, textArray(conn, chunk, nr -> toTimestamptz(nr.note().getCreated())));
            ps.setArray(p++, bigintArray(conn, chunk, nr -> lastUpdatedById(nr.note())));
            ps.setArray(p++, textArray(conn, chunk, nr -> nr.note().getNoteText()));
            ps.setArray(p++, bigintArray(conn, chunk, NoteRow::entityId));
            ps.executeUpdate();
          }
        });
  }

  private static Long lastUpdatedById(Note note) {
    return note.getLastUpdatedBy() != null ? note.getLastUpdatedBy().getId() : null;
  }
}
