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
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.buildMultiRowInsertSql;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.forEachChunk;
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.toTimestamp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
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

  private static final String NOTE_INSERT_PREFIX =
      "insert into note (noteid, uid, created, lastupdatedby, notetext) values ";
  private static final String NOTE_INSERT_ROW = "(?, ?, ?, ?, ?)";

  private static final String JOIN_INSERT_ROW = "(?, ?, ?)";

  /** A new {@link Note} to insert for a given owner row, with its 1-based sort order. */
  record NoteRow(long ownerId, Note note, int sortOrder) {}

  private final String joinInsertPrefix;

  NoteCascadeWriter(String joinTable, String ownerIdColumn) {
    this.joinInsertPrefix =
        "insert into " + joinTable + " (" + ownerIdColumn + ", noteid, sort_order) values ";
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
          String sql = buildMultiRowInsertSql(NOTE_INSERT_PREFIX, NOTE_INSERT_ROW, chunk.size());
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int p = 1;
            for (NoteRow item : chunk) {
              Note n = item.note();
              ps.setLong(p++, n.getId());
              ps.setString(p++, n.getUid());
              ps.setTimestamp(p++, toTimestamp(n.getCreated()));
              if (n.getLastUpdatedBy() != null) {
                ps.setLong(p++, n.getLastUpdatedBy().getId());
              } else {
                ps.setNull(p++, Types.BIGINT);
              }
              if (n.getNoteText() != null) {
                ps.setString(p++, n.getNoteText());
              } else {
                ps.setNull(p++, Types.VARCHAR);
              }
            }
            ps.executeUpdate();
          }
        });
  }

  private void insertJoinRows(Connection conn, List<NoteRow> newNotes) throws SQLException {
    forEachChunk(
        newNotes,
        chunk -> {
          String sql = buildMultiRowInsertSql(joinInsertPrefix, JOIN_INSERT_ROW, chunk.size());
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int p = 1;
            for (NoteRow item : chunk) {
              ps.setLong(p++, item.ownerId());
              ps.setLong(p++, item.note().getId());
              ps.setInt(p++, item.sortOrder());
            }
            ps.executeUpdate();
          }
        });
  }
}
