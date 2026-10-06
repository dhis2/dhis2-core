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
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// This class is annotated with @Component instead of @Repository because @Repository creates a
// proxy that can't be used to inject the class.
@Component("org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueStore")
class HibernateTrackedEntityAttributeValueStore
    extends HibernateGenericStore<TrackedEntityAttributeValue> {
  /**
   * Upper bound of values (or org unit/value pairs) bound in one query. A scoped query binds up to
   * three parameters per pair, which keeps it below the Postgres limit of 32767 bind parameters.
   */
  private static final int MAX_VALUES_PER_QUERY = 10_000;

  /**
   * Selects only the columns the uniqueness validation needs. Selecting entities instead would load
   * one tracked entity per matching row (the association from {@code TrackedEntityAttributeValue}
   * is eager) and the users referenced by each org unit.
   */
  private static final String SELECT_UNIQUE_VALUES =
      """
      select te.uid, v.plainValue, te.organisationUnit.id
      from TrackedEntityAttributeValue v
      join v.trackedEntity te
      where v.attribute = :attribute
      """;

  public HibernateTrackedEntityAttributeValueStore(
      EntityManager entityManager, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher) {
    super(entityManager, jdbcTemplate, publisher, TrackedEntityAttributeValue.class, false);
  }

  // -------------------------------------------------------------------------
  // Implementation methods
  // -------------------------------------------------------------------------

  public void saveVoid(TrackedEntityAttributeValue attributeValue) {
    getSession().save(attributeValue);
  }

  public List<TrackedEntityAttributeValue> get(TrackedEntity trackedEntity) {
    String query = " from TrackedEntityAttributeValue v where v.trackedEntity =:trackedEntity";

    Query<TrackedEntityAttributeValue> typedQuery =
        getQuery(query).setParameter("trackedEntity", trackedEntity);

    return getList(typedQuery);
  }

  /**
   * Finds the stored values of {@code attribute} matching any of {@code values} (case-insensitive),
   * in any org unit.
   */
  public List<UniqueAttributeValueMatch> getUniqueAttributeValues(
      TrackedEntityAttribute attribute, Set<String> values) {
    List<UniqueAttributeValueMatch> matches = new ArrayList<>();
    for (List<String> partition : Lists.partition(lowerCase(values), MAX_VALUES_PER_QUERY)) {
      String hql = SELECT_UNIQUE_VALUES + " and lower(v.plainValue) in (:values)";
      getSession()
          .createQuery(hql, Object[].class)
          .setParameter("attribute", attribute)
          .setParameterList("values", partition)
          .list()
          .forEach(
              row ->
                  matches.add(
                      new UniqueAttributeValueMatch(
                          UID.of((String) row[0]), (String) row[1], (Long) row[2])));
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

      String hql =
          SELECT_UNIQUE_VALUES
              + """
               and lower(v.plainValue) in (:values)
               and te.organisationUnit.id in (:orgUnitIds)
               and concat(lower(v.plainValue), '|', str(te.organisationUnit.id)) in (:keys)
              """;
      getSession()
          .createQuery(hql, Object[].class)
          .setParameter("attribute", attribute)
          .setParameterList("values", values)
          .setParameterList("orgUnitIds", partition.keySet())
          .setParameterList("keys", keys)
          .list()
          .forEach(
              row ->
                  matches.add(
                      new UniqueAttributeValueMatch(
                          UID.of((String) row[0]), (String) row[1], (Long) row[2])));
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
