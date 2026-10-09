/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.trackedentityattributevalue;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads and deletes {@link TrackedEntityAttributeValue}s via JDBC, bypassing the Hibernate
 * persistence context. Values are written by the tracker importer (see {@code TeavWriter}).
 */
// This class is annotated with @Component instead of @Repository because @Repository creates a
// proxy that can't be used to inject the class.
@Component
@RequiredArgsConstructor
class JdbcTrackedEntityAttributeValueStore {
  /**
   * Upper bound of values (or org unit/value pairs) bound in one query. A scoped query binds up to
   * three parameters per pair, which keeps it below the Postgres limit of 32767 bind parameters.
   */
  private static final int MAX_VALUES_PER_QUERY = 10_000;

  private static final String SELECT_BY_TRACKED_ENTITY =
      """
      select trackedentityattributeid, value, created, lastupdated, updatedby
      from trackedentityattributevalue
      where trackedentityid = :trackedEntityId
      """;

  private static final String DELETE_BY_TRACKED_ENTITY =
      """
      delete from trackedentityattributevalue
      where trackedentityid = :trackedEntityId
      """;

  /** Selects only the columns the uniqueness validation needs. */
  private static final String SELECT_UNIQUE_VALUES =
      """
      select te.uid, v.value, te.organisationunitid
      from trackedentityattributevalue v
      join trackedentity te on te.trackedentityid = v.trackedentityid
      where v.trackedentityattributeid = :attributeId
      and lower(v.value) in (:values)
      """;

  private static final RowMapper<UniqueAttributeValueMatch> UNIQUE_VALUE_MATCH_MAPPER =
      (rs, rowNum) ->
          new UniqueAttributeValueMatch(UID.of(rs.getString(1)), rs.getString(2), rs.getLong(3));

  private final NamedParameterJdbcTemplate jdbcTemplate;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  /**
   * Returns the values of {@code trackedEntity}. The values are not managed by Hibernate: changing
   * them has no effect on the database.
   */
  public List<TrackedEntityAttributeValue> get(TrackedEntity trackedEntity) {
    return jdbcTemplate.query(
        SELECT_BY_TRACKED_ENTITY,
        new MapSqlParameterSource("trackedEntityId", trackedEntity.getId()),
        (rs, rowNum) -> {
          TrackedEntityAttributeValue value =
              new TrackedEntityAttributeValue(
                  trackedEntityAttributeService.getTrackedEntityAttribute(rs.getLong(1)),
                  trackedEntity,
                  rs.getString(2));
          value.setCreated(rs.getTimestamp(3));
          value.setLastUpdated(rs.getTimestamp(4));
          value.setUpdatedBy(rs.getString(5));
          return value;
        });
  }

  /** Deletes all the values of {@code trackedEntity}. */
  public void delete(TrackedEntity trackedEntity) {
    jdbcTemplate.update(
        DELETE_BY_TRACKED_ENTITY,
        new MapSqlParameterSource("trackedEntityId", trackedEntity.getId()));
  }

  /**
   * Finds the stored values of {@code attribute} matching any of {@code values} (case-insensitive),
   * in any org unit.
   */
  public List<UniqueAttributeValueMatch> getUniqueAttributeValues(
      TrackedEntityAttribute attribute, Set<String> values) {
    List<UniqueAttributeValueMatch> matches = new ArrayList<>();
    for (List<String> partition : Lists.partition(lowerCase(values), MAX_VALUES_PER_QUERY)) {
      matches.addAll(
          jdbcTemplate.query(
              SELECT_UNIQUE_VALUES,
              new MapSqlParameterSource()
                  .addValue("attributeId", attribute.getId())
                  .addValue("values", partition),
              UNIQUE_VALUE_MATCH_MAPPER));
    }
    return matches;
  }

  /**
   * Finds the stored values of {@code attribute} matching exactly one of the (org unit, value)
   * pairs in {@code valuesByOrgUnitId} (value compared case-insensitively).
   *
   * <p>The value and org unit {@code in} filters let Postgres use the value or the org unit index,
   * whichever is more selective. The key filter on {@code lower(value)|orgUnitId} then drops the
   * cross product combinations that were not requested, e.g. a value only sent for org unit A that
   * exists in org unit B. Postgres hashes the key list, so it does not slow the query down.
   */
  public List<UniqueAttributeValueMatch> getUniqueAttributeValues(
      TrackedEntityAttribute attribute, Map<Long, Set<String>> valuesByOrgUnitId) {
    String sql =
        SELECT_UNIQUE_VALUES
            + """
            and te.organisationunitid in (:orgUnitIds)
            and lower(v.value) || '|' || te.organisationunitid in (:keys)
            """;
    List<UniqueAttributeValueMatch> matches = new ArrayList<>();
    for (Map<Long, Set<String>> partition : partition(valuesByOrgUnitId)) {
      Set<String> values = new HashSet<>();
      List<String> keys = new ArrayList<>();
      for (Map.Entry<Long, Set<String>> entry : partition.entrySet()) {
        for (String value : lowerCase(entry.getValue())) {
          values.add(value);
          keys.add(value + "|" + entry.getKey());
        }
      }

      matches.addAll(
          jdbcTemplate.query(
              sql,
              new MapSqlParameterSource()
                  .addValue("attributeId", attribute.getId())
                  .addValue("values", values)
                  .addValue("orgUnitIds", partition.keySet())
                  .addValue("keys", keys),
              UNIQUE_VALUE_MATCH_MAPPER));
    }
    return matches;
  }

  /**
   * Splits the pairs into chunks of at most {@link #MAX_VALUES_PER_QUERY} pairs, so every query
   * stays below the Postgres bind parameter limit (values + org units + keys per query).
   */
  private static List<Map<Long, Set<String>>> partition(Map<Long, Set<String>> valuesByOrgUnitId) {
    List<Map<Long, Set<String>>> partitions = new ArrayList<>();
    Map<Long, Set<String>> current = new HashMap<>();
    int pairs = 0;
    for (Map.Entry<Long, Set<String>> entry : valuesByOrgUnitId.entrySet()) {
      for (String value : entry.getValue()) {
        if (pairs == MAX_VALUES_PER_QUERY) {
          partitions.add(current);
          current = new HashMap<>();
          pairs = 0;
        }
        current.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).add(value);
        pairs++;
      }
    }
    if (!current.isEmpty()) {
      partitions.add(current);
    }
    return partitions;
  }

  private static List<String> lowerCase(Set<String> values) {
    return values.stream().map(StringUtils::lowerCase).distinct().toList();
  }
}
