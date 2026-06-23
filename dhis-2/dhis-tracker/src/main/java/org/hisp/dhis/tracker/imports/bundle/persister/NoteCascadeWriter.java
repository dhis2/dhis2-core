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

import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.allocateIds;
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
 * Cascade-inserts new {@link Note}s for an entity type that carries notes (Enrollment,
 * TrackerEvent, SingleEvent), replacing Hibernate's {@code @OneToMany(cascade=ALL)} behaviour.
 * Mirrors what Hibernate did on the owning collection: allocate ids for the new {@code note} rows
 * from {@code note_sequence}, insert the {@code note} rows, then insert the join rows in the
 * owner's notes join table ({@code enrollment_notes} / {@code trackerevent_notes} / {@code
 * singleevent_notes}) with {@code sort_order} taken from the 1-based list position (per
 * {@code @ListIndexBase(1)}).
 *
 * <p>On INSERT every Note is new; on UPDATE only Notes with {@code id == 0} are new (existing ones
 * were loaded by the preheat). Notes are append-only on UPDATE.
 *
 * <p>One instance is configured per join table; the three entity writers each compose one.
 */
final class NoteCascadeWriter {

  // Constant-text INSERT ... SELECT unnest(...) so pgjdbc's prepared-statement cache engages
  // regardless of row count.
  private static final String NOTE_INSERT_SQL =
      "insert into note (noteid, uid, created, lastupdatedby, notetext)"
          + " select noteid, uid, created, lastupdatedby, notetext"
          + " from ( select"
          + " unnest(?::bigint[]) as noteid,"
          + " unnest(?::text[]) as uid,"
          + " unnest(?::timestamptz[]) as created,"
          + " unnest(?::bigint[]) as lastupdatedby,"
          + " unnest(?::text[]) as notetext"
          + " ) v";

  /** A new {@link Note} to insert for a given owner row, with its 1-based sort order. */
  record NoteRow(long ownerId, Note note, int sortOrder) {}

  // Constant per join table (the table and owner-id column vary by instance), built once here so
  // each NoteCascadeWriter has a single cacheable join INSERT statement.
  private final String joinInsertSql;

  NoteCascadeWriter(String joinTable, String ownerIdColumn) {
    this.joinInsertSql =
        "insert into "
            + joinTable
            + " ("
            + ownerIdColumn
            + ", noteid, sort_order)"
            + " select owner_id, note_id, sort_order"
            + " from ( select"
            + " unnest(?::bigint[]) as owner_id,"
            + " unnest(?::bigint[]) as note_id,"
            + " unnest(?::bigint[]) as sort_order"
            + " ) v";
  }

  /**
   * Collects the new notes (no DB id) from the given inserted and updated owners and
   * cascade-inserts them. {@code ownerId} extracts the owner's pre-allocated/persisted id; {@code
   * notesGetter} returns the owner's notes list (may be {@code null}).
   */
  <E> void cascade(
      Connection conn,
      List<E> inserts,
      List<E> updates,
      ToLongFunction<E> ownerId,
      Function<E, List<Note>> notesGetter)
      throws SQLException {
    List<NoteRow> newNotes = new ArrayList<>();
    collectNewNotes(inserts, ownerId, notesGetter, newNotes);
    collectNewNotes(updates, ownerId, notesGetter, newNotes);
    if (newNotes.isEmpty()) {
      return;
    }

    long[] noteIds = allocateIds(conn, "note_sequence", newNotes.size());
    for (int i = 0; i < newNotes.size(); i++) {
      newNotes.get(i).note().setId(noteIds[i]);
    }

    insertNoteRows(conn, newNotes);
    insertJoinRows(conn, newNotes);
  }

  private static <E> void collectNewNotes(
      List<E> owners,
      ToLongFunction<E> ownerId,
      Function<E, List<Note>> notesGetter,
      List<NoteRow> out) {
    for (E owner : owners) {
      List<Note> notes = notesGetter.apply(owner);
      if (notes == null) {
        continue;
      }
      for (int i = 0; i < notes.size(); i++) {
        Note n = notes.get(i);
        if (n.getId() == 0) {
          out.add(new NoteRow(ownerId.applyAsLong(owner), n, i + 1));
        }
      }
    }
  }

  private void insertNoteRows(Connection conn, List<NoteRow> newNotes) throws SQLException {
    forEachChunk(
        newNotes,
        chunk -> {
          try (PreparedStatement ps = conn.prepareStatement(NOTE_INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, nr -> nr.note().getId()));
            ps.setArray(p++, textArray(conn, chunk, nr -> nr.note().getUid()));
            ps.setArray(p++, textArray(conn, chunk, nr -> toTimestamptz(nr.note().getCreated())));
            ps.setArray(p++, bigintArray(conn, chunk, nr -> lastUpdatedById(nr.note())));
            ps.setArray(p++, textArray(conn, chunk, nr -> nr.note().getNoteText()));
            ps.executeUpdate();
          }
        });
  }

  private static Long lastUpdatedById(Note note) {
    return note.getLastUpdatedBy() != null ? note.getLastUpdatedBy().getId() : null;
  }

  private void insertJoinRows(Connection conn, List<NoteRow> newNotes) throws SQLException {
    forEachChunk(
        newNotes,
        chunk -> {
          try (PreparedStatement ps = conn.prepareStatement(joinInsertSql)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, NoteRow::ownerId));
            ps.setArray(p++, bigintArray(conn, chunk, nr -> nr.note().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, nr -> (long) nr.sortOrder()));
            ps.executeUpdate();
          }
        });
  }
}
