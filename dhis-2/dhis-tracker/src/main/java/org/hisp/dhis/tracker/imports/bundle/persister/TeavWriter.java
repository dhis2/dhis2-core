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
import static org.hisp.dhis.tracker.imports.bundle.persister.JdbcBatchSupport.truncate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.jasypt.encryption.pbe.PBEStringEncryptor;

/**
 * Flushes staged {@link TrackedEntityAttributeValue} writes via JDBC against {@code
 * trackedentityattributevalue}: a multi-row INSERT, a single unnest UPDATE keyed on the composite
 * ({@code trackedentityid}, {@code trackedentityattributeid}) PK, and a single unnest DELETE on the
 * same key. TEAV has a composite PK and no sequence, so no id pre-allocation is needed.
 *
 * <p>Mirrors the former Hibernate mapping: the value is always stored as plain text in {@code
 * value}, and for confidential attributes it is additionally stored AES-128 encrypted in {@code
 * encryptedvalue} using the same encryptor as the {@code Aes128EncryptedString} Hibernate type.
 */
final class TeavWriter {

  // Constant-text INSERT ... SELECT unnest(...) so pgjdbc's prepared-statement cache engages
  // regardless of row count.
  private static final String INSERT_SQL =
      "insert into trackedentityattributevalue ("
          + "trackedentityid, trackedentityattributeid, created, lastupdated, value, encryptedvalue,"
          + " storedby)"
          + " select trackedentityid, trackedentityattributeid, created, lastupdated, value,"
          + " encryptedvalue, storedby"
          + " from ( select"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::bigint[]) as trackedentityattributeid,"
          + " unnest(?::timestamptz[]) as created,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::text[]) as value,"
          + " unnest(?::text[]) as encryptedvalue,"
          + " unnest(?::text[]) as storedby"
          + " ) v";

  // `created` is insert-only and deliberately excluded from the UPDATE column set.
  private static final String UPDATE_SQL =
      "update trackedentityattributevalue teav set"
          + " lastupdated = v.lastupdated,"
          + " value = v.value,"
          + " encryptedvalue = v.encryptedvalue,"
          + " storedby = v.storedby"
          + " from ( select"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::bigint[]) as trackedentityattributeid,"
          + " unnest(?::timestamptz[]) as lastupdated,"
          + " unnest(?::text[]) as value,"
          + " unnest(?::text[]) as encryptedvalue,"
          + " unnest(?::text[]) as storedby"
          + " ) v where teav.trackedentityid = v.trackedentityid"
          + " and teav.trackedentityattributeid = v.trackedentityattributeid";

  // Unnest DELETE on the composite key, mirroring the unnest UPDATE shape rather than an
  // IN (VALUES ...) form, to stay consistent with the other batch statements and avoid dynamic SQL.
  private static final String DELETE_SQL =
      "delete from trackedentityattributevalue teav"
          + " using ( select"
          + " unnest(?::bigint[]) as trackedentityid,"
          + " unnest(?::bigint[]) as trackedentityattributeid"
          + " ) v where teav.trackedentityid = v.trackedentityid"
          + " and teav.trackedentityattributeid = v.trackedentityattributeid";

  private final PBEStringEncryptor encryptor;

  private final List<TrackedEntityAttributeValue> inserts = new ArrayList<>();
  private final List<TrackedEntityAttributeValue> updates = new ArrayList<>();
  private final List<TrackedEntityAttributeValue> deletes = new ArrayList<>();

  TeavWriter(PBEStringEncryptor encryptor) {
    this.encryptor = encryptor;
  }

  void stageInsert(TrackedEntityAttributeValue value) {
    inserts.add(value);
  }

  void stageUpdate(TrackedEntityAttributeValue value) {
    updates.add(value);
  }

  void stageDelete(TrackedEntityAttributeValue value) {
    deletes.add(value);
  }

  Mark mark() {
    return new Mark(inserts.size(), updates.size(), deletes.size());
  }

  void rollbackTo(Mark mark) {
    truncate(inserts, mark.inserts());
    truncate(updates, mark.updates());
    truncate(deletes, mark.deletes());
  }

  void clear() {
    inserts.clear();
    updates.clear();
    deletes.clear();
  }

  boolean isEmpty() {
    return inserts.isEmpty() && updates.isEmpty() && deletes.isEmpty();
  }

  void flush(Connection conn) throws SQLException {
    insert(conn);
    update(conn);
    delete(conn);
  }

  private void insert(Connection conn) throws SQLException {
    forEachChunk(
        inserts,
        chunk -> {
          try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            int p = 1;
            ps.setArray(p++, bigintArray(conn, chunk, v -> v.getTrackedEntity().getId()));
            ps.setArray(p++, bigintArray(conn, chunk, v -> v.getAttribute().getId()));
            ps.setArray(p++, textArray(conn, chunk, v -> toTimestamptz(v.getCreated())));
            ps.setArray(p++, textArray(conn, chunk, v -> toTimestamptz(v.getLastUpdated())));
            ps.setArray(p++, textArray(conn, chunk, TrackedEntityAttributeValue::getPlainValue));
            ps.setArray(p++, textArray(conn, chunk, this::encryptedValue));
            ps.setArray(p++, textArray(conn, chunk, TrackedEntityAttributeValue::getStoredBy));
            ps.executeUpdate();
          }
        });
  }

  private void update(Connection conn) throws SQLException {
    forEachChunk(
        updates,
        chunk -> {
          int n = chunk.size();

          Long[] trackedEntityIds = new Long[n];
          Long[] attributeIds = new Long[n];
          String[] lastUpdated = new String[n];
          String[] value = new String[n];
          String[] encryptedValue = new String[n];
          String[] storedBy = new String[n];

          for (int i = 0; i < n; i++) {
            TrackedEntityAttributeValue v = chunk.get(i);
            trackedEntityIds[i] = v.getTrackedEntity().getId();
            attributeIds[i] = v.getAttribute().getId();
            lastUpdated[i] = toTimestamptz(v.getLastUpdated());
            value[i] = v.getPlainValue();
            encryptedValue[i] = encryptedValue(v);
            storedBy[i] = v.getStoredBy();
          }

          try (PreparedStatement ps = conn.prepareStatement(UPDATE_SQL)) {
            ps.setArray(1, conn.createArrayOf("bigint", trackedEntityIds));
            ps.setArray(2, conn.createArrayOf("bigint", attributeIds));
            ps.setArray(3, conn.createArrayOf("text", lastUpdated));
            ps.setArray(4, conn.createArrayOf("text", value));
            ps.setArray(5, conn.createArrayOf("text", encryptedValue));
            ps.setArray(6, conn.createArrayOf("text", storedBy));
            ps.executeUpdate();
          }
        });
  }

  private void delete(Connection conn) throws SQLException {
    forEachChunk(
        deletes,
        chunk -> {
          int n = chunk.size();

          Long[] trackedEntityIds = new Long[n];
          Long[] attributeIds = new Long[n];
          for (int i = 0; i < n; i++) {
            TrackedEntityAttributeValue v = chunk.get(i);
            trackedEntityIds[i] = v.getTrackedEntity().getId();
            attributeIds[i] = v.getAttribute().getId();
          }

          try (PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
            ps.setArray(1, conn.createArrayOf("bigint", trackedEntityIds));
            ps.setArray(2, conn.createArrayOf("bigint", attributeIds));
            ps.executeUpdate();
          }
        });
  }

  /**
   * Encrypts the value of a confidential attribute for the {@code encryptedvalue} column. Returns
   * null for non-confidential attributes, clearing any ciphertext left over from when the attribute
   * was confidential. Hibernate kept that stale ciphertext, but it holds an outdated value of an
   * attribute that is no longer confidential.
   */
  @CheckForNull
  private String encryptedValue(TrackedEntityAttributeValue value) {
    if (!value.getAttribute().isConfidentialBool() || value.getValue() == null) {
      return null;
    }
    return encryptor.encrypt(value.getValue());
  }

  record Mark(int inserts, int updates, int deletes) {}
}
