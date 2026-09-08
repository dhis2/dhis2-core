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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.program.UserInfoSnapshot;

/**
 * Shared JDBC mechanics for the per-entity {@code *Writer} classes that make up {@link
 * EntityWriteBatch}: id pre-allocation, multi-row INSERT SQL assembly, chunking, nullable binds,
 * date/timestamp formatting, list truncation and the JSON serializers. Centralising the serializers
 * here also centralises the invariant that {@code eventdatavalues} / {@code ObjectStyle} must be
 * written with {@link JsonBinaryType#MAPPER} (a different {@code ObjectMapper} drops fields like
 * {@code UserInfoSnapshot.id} that the JDBC reader requires).
 */
final class JdbcBatchSupport {

  private JdbcBatchSupport() {}

  /** Cap on rows per multi-row INSERT/UPDATE statement, to bound peak array memory per chunk. */
  static final int BATCH_SIZE = 128;

  static final int SRID = 4326;

  /** Body invoked once per {@code BATCH_SIZE}-sized chunk of a list. */
  @FunctionalInterface
  interface ChunkConsumer<T> {
    void accept(List<T> chunk) throws SQLException;
  }

  /** Invokes {@code consumer} on successive {@link #BATCH_SIZE}-sized sublists of {@code list}. */
  static <T> void forEachChunk(List<T> list, ChunkConsumer<T> consumer) throws SQLException {
    for (int from = 0; from < list.size(); from += BATCH_SIZE) {
      int to = Math.min(from + BATCH_SIZE, list.size());
      consumer.accept(list.subList(from, to));
    }
  }

  /**
   * Extracts one column value from a row. Allows {@link SQLException} so JSON serializers ({@link
   * #toJson}, {@link #toEventDataValuesJson}) can be used directly as extractors.
   */
  @FunctionalInterface
  interface RowExtractor<E, R> {
    R apply(E row) throws SQLException;
  }

  /**
   * Builds a SQL {@code bigint[]} from one column of {@code rows} for binding into a {@code
   * ?::bigint[]} parameter of a constant-text {@code INSERT ... SELECT unnest(...)} statement. Null
   * elements (e.g. nullable FKs) become SQL NULLs.
   */
  static <E> Array bigintArray(Connection conn, List<E> rows, RowExtractor<E, Long> extractor)
      throws SQLException {
    Long[] values = new Long[rows.size()];
    for (int i = 0; i < values.length; i++) {
      values[i] = extractor.apply(rows.get(i));
    }
    return conn.createArrayOf("bigint", values);
  }

  /**
   * Builds a SQL {@code text[]} from one column of {@code rows}. Used both for genuine text columns
   * ({@code ?::text[]}) and for timestamps formatted via {@link #toTimestamptz} ({@code
   * ?::timestamptz[]}), mirroring the unnest UPDATE binding.
   */
  static <E> Array textArray(Connection conn, List<E> rows, RowExtractor<E, String> extractor)
      throws SQLException {
    String[] values = new String[rows.size()];
    for (int i = 0; i < values.length; i++) {
      values[i] = extractor.apply(rows.get(i));
    }
    return conn.createArrayOf("text", values);
  }

  /** Builds a SQL {@code boolean[]} from one column of {@code rows}. */
  static <E> Array booleanArray(Connection conn, List<E> rows, RowExtractor<E, Boolean> extractor)
      throws SQLException {
    Boolean[] values = new Boolean[rows.size()];
    for (int i = 0; i < values.length; i++) {
      values[i] = extractor.apply(rows.get(i));
    }
    return conn.createArrayOf("boolean", values);
  }

  /**
   * Fetches {@code count} ids from {@code sequenceName} in a single round-trip. The sequence name
   * is interpolated into the SQL (not a bind parameter) because PostgreSQL's {@code nextval} takes
   * a {@code regclass}; the value is always a static literal controlled by us.
   */
  static long[] allocateIds(Connection conn, String sequenceName, int count) throws SQLException {
    long[] ids = new long[count];
    String sql = "select nextval('" + sequenceName + "') from generate_series(1, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, count);
      try (ResultSet rs = ps.executeQuery()) {
        int i = 0;
        while (rs.next()) {
          ids[i++] = rs.getLong(1);
        }
        if (i != count) {
          throw new SQLException(
              "Allocated " + i + " ids from " + sequenceName + ", expected " + count);
        }
      }
    }
    return ids;
  }

  /**
   * Formats a date as an ISO-8601 instant with explicit UTC offset, for binding into a {@code
   * ?::timestamptz[]} parameter as a text array. Binding {@code createArrayOf("timestamptz",
   * Timestamp[])} is not safe: pgjdbc stringifies each element via {@code Timestamp.toString()},
   * which renders the JVM-local wall-clock time without an offset, so the server re-interprets it
   * under the session {@code TimeZone} -- skewing every value when the session zone differs from
   * the JVM zone (e.g. {@code options=-c TimeZone=UTC}) and in the DST fall-back hour. An explicit
   * offset makes the parse unambiguous.
   */
  static String toTimestamptz(Date date) {
    return date != null
        ? OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).toString()
        : null;
  }

  /**
   * The WKT text of a geometry, or {@code null}. Bound into a {@code ?::text[]} array and converted
   * back by {@code ST_GeomFromText} in the INSERT/UPDATE projection.
   */
  static String geometryText(org.locationtech.jts.geom.Geometry geometry) {
    return geometry != null ? geometry.toText() : null;
  }

  static <T> void truncate(List<T> list, int size) {
    if (list.size() > size) {
      list.subList(size, list.size()).clear();
    }
  }

  static String toJson(ObjectMapper objectMapper, UserInfoSnapshot info) throws SQLException {
    if (info == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(info);
    } catch (JsonProcessingException e) {
      throw new SQLException("Failed to serialize UserInfoSnapshot to JSON", e);
    }
  }

  // Must mirror JsonEventDataValueSetBinaryType exactly: same MAPPER (configured with
  // IgnoreJsonPropertyWriteOnlyAccessJacksonAnnotationIntrospector, NON_NULL inclusion, etc.) and
  // same per-value writer. Using a different ObjectMapper (e.g. the Spring-injected default) drops
  // fields like UserInfoSnapshot.id that the JDBC reader requires.
  private static final ObjectWriter EVENT_DATA_VALUE_WRITER =
      JsonBinaryType.MAPPER.writerFor(EventDataValue.class);

  /**
   * Serializes the EventDataValues set as a JSON object keyed by {@code dataElement} uid, matching
   * the on-disk shape produced by {@code JsonEventDataValueSetBinaryType}. An empty or null set is
   * serialized as {@code "{}"} to match the column's NOT NULL default.
   */
  static String toEventDataValuesJson(Set<EventDataValue> values) throws SQLException {
    try {
      StringWriter sw = new StringWriter();
      try (JsonGenerator gen = JsonBinaryType.MAPPER.getFactory().createGenerator(sw)) {
        gen.writeStartObject();
        if (values != null) {
          for (EventDataValue edv : values) {
            gen.writeFieldName(edv.getDataElement());
            EVENT_DATA_VALUE_WRITER.writeValue(gen, edv);
          }
        }
        gen.writeEndObject();
      }
      return sw.toString();
    } catch (IOException e) {
      throw new SQLException("Failed to serialize EventDataValues to JSON", e);
    }
  }

  /**
   * Serializes the relationship's {@link ObjectStyle} via {@link JsonBinaryType#MAPPER} to match
   * the Hibernate {@code jbObjectStyle} UserType. Returns {@code null} for a null style so the
   * caller can pass it straight to a {@code ?::jsonb} parameter as SQL NULL.
   */
  static String toObjectStyleJson(ObjectStyle style) throws SQLException {
    if (style == null) {
      return null;
    }
    try {
      return JsonBinaryType.MAPPER.writeValueAsString(style);
    } catch (JsonProcessingException e) {
      throw new SQLException("Failed to serialize ObjectStyle to JSON", e);
    }
  }
}
