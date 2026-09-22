/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;

/**
 * JdbcNotes generates the SQL aggregating the notes of an enrollment or event into JSON and maps
 * that JSON back into {@link Note}s. Both live here so the SQL and its mapping cannot drift apart.
 *
 * <p>{@link #leftJoinLateral(String, String, String)} produces a lateral join aggregating all notes
 * of one owner into a single JSON array, exposed as column {@code notes.jsonnotes}. Aggregating in
 * the lateral instead of joining the notes directly keeps the result at one row per owner. A plain
 * join multiplies the owners projection by its number of notes, which then has to be deduplicated
 * in Java.
 *
 * <p>Add the aggregate to the select clause as
 *
 * <pre>{@code
 * , notes.jsonnotes as notes
 * }</pre>
 *
 * and join it <b>after</b> the owner it references is in scope. The join is a {@code left join} so
 * an owner without notes is still returned, with a {@code null} aggregate. Pass that aggregate to
 * {@link #fromJson(String)}.
 *
 * <p>{@code note.lastupdatedby} is nullable, so {@code userinfo} is joined with a {@code left join}
 * and the resulting {@link Note#getLastUpdatedBy()} may be {@code null}. Dropping such notes from
 * the export was <a href="https://dhis2.atlassian.net/browse/DHIS2-22148">DHIS2-22148</a>.
 */
public class JdbcNotes {
  private static final ObjectReader NOTES_READER = new ObjectMapper().readerForListOf(Row.class);

  private JdbcNotes() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Generates a lateral join aggregating the notes of one owner into the JSON column {@code
   * notes.jsonnotes}. Pair with {@link #fromJson(String)}.
   *
   * @param linkTable table linking the owner to its notes, e.g. {@code enrollment_notes}
   * @param linkColumn column in {@code linkTable} referencing the owner, e.g. {@code enrollmentid}
   * @param ownerColumn qualified owner id column the join correlates on, e.g. {@code
   *     e.enrollmentid}
   */
  @Nonnull
  public static String leftJoinLateral(
      @Nonnull String linkTable, @Nonnull String linkColumn, @Nonnull String ownerColumn) {
    return """
      left join lateral (
        select json_agg(json_build_object('uid', n.uid, 'text', n.notetext,
          'created', n.created, 'updatedByUid', u.uid,
          'updatedByUsername', u.username, 'updatedByFirstname', u.firstname,
          'updatedBySurname', u.surname, 'updatedByName', u.name)) as jsonnotes
          from %s ln
          join note n on n.noteid = ln.noteid
          left join userinfo u on u.userinfoid = n.lastupdatedby
          where ln.%s = %s
      ) notes on true
    """
        .formatted(linkTable, linkColumn, ownerColumn);
  }

  /**
   * Parses the JSON aggregate produced by {@link #leftJoinLateral(String, String, String)} into
   * notes.
   *
   * @param json value of the {@code notes} column, may be null if the owner has no notes
   * @return parsed notes, or null if the input is null
   */
  @CheckForNull
  public static List<Note> fromJson(@CheckForNull String json) {
    if (json == null) {
      return null;
    }

    List<Row> rows;
    try {
      rows = NOTES_READER.readValue(json);
    } catch (IOException e) {
      // the JSON is generated by leftJoinLateral, so failing to parse it is a broken invariant and
      // not bad user input. Do not log the payload as notes contain user data.
      throw new IllegalStateException("Notes cannot be mapped", e);
    }

    List<Note> notes = new ArrayList<>(rows.size());
    for (Row row : rows) {
      Note note = new Note();
      note.setUid(row.getUid());
      note.setNoteText(row.getText());
      note.setCreated(DateUtils.safeParseDate(row.getCreated()));
      if (row.getUpdatedByUid() != null) {
        User user = new User();
        user.setUid(row.getUpdatedByUid());
        user.setUsername(row.getUpdatedByUsername());
        user.setFirstName(row.getUpdatedByFirstname());
        user.setSurname(row.getUpdatedBySurname());
        user.setName(row.getUpdatedByName());
        note.setLastUpdatedBy(user);
      }
      notes.add(note);
    }
    return notes;
  }

  @Getter
  @Setter
  private static class Row {
    private String uid;
    private String text;
    private String created;
    private String updatedByUid;
    private String updatedByUsername;
    private String updatedByFirstname;
    private String updatedBySurname;
    private String updatedByName;
  }
}
