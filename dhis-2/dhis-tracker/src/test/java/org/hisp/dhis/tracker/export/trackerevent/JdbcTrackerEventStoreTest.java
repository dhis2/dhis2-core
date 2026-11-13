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
package org.hisp.dhis.tracker.export.trackerevent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.security.acl.AccessControlStore;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.test.TrackerTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * PostgreSQL only reorders the first {@code join_collapse_limit} (default 8) tables of explicit
 * joins. If an attribute filter join is written after that, the planner cannot start from it and
 * scans all events of the program (see DHIS2-21350). These tests fail if a join is added in front
 * of the attribute filter joins.
 */
class JdbcTrackerEventStoreTest extends TrackerTestBase {

  private static final int JOIN_COLLAPSE_LIMIT = 8;

  /** The attribute joins start right after ev, en, te, po and ou. */
  private static final int FIRST_ATTRIBUTE_JOIN_POSITION = 6;

  private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);

  private final AccessControlStore accessControlStore = mock(AccessControlStore.class);

  private final JdbcTrackerEventStore store =
      new JdbcTrackerEventStore(jdbcTemplate, accessControlStore);

  private final UserDetails user = UserDetails.fromUser(new User());

  @BeforeEach
  void setUp() {
    injectSecurityContextNoSettings(user);
    // a non-superuser who may read one attribute option combo, so the readable AOC filter is added
    when(accessControlStore.getDataReadableAttributeOptionComboIds(any(), any()))
        .thenReturn(new long[] {1L});
  }

  @AfterEach
  void tearDown() {
    clearSecurityContext();
  }

  @Test
  void shouldJoinFilteredAttributesWithinJoinCollapseLimitWhenFilteringByAttributes()
      throws BadRequestException {
    TrackerEventQueryParams params = new TrackerEventQueryParams();
    // 3 attributes fill positions 6 to 8, the last positions within the limit
    List<TrackedEntityAttribute> attributes =
        List.of(attribute('a', false), attribute('b', false), attribute('c', false));
    for (TrackedEntityAttribute attribute : attributes) {
      params.filterBy(attribute, new QueryFilter(QueryOperator.EQ, "value"));
    }

    String sql = getEventsSql(params);

    for (TrackedEntityAttribute attribute : attributes) {
      int attributeTableJoinPosition = tablePosition(sql, attribute);
      assertTrue(
          attributeTableJoinPosition <= JOIN_COLLAPSE_LIMIT,
          "join on attribute "
              + attribute.getUid()
              + " must be within the first "
              + JOIN_COLLAPSE_LIMIT
              + " tables but was at position "
              + attributeTableJoinPosition
              + " in: "
              + sql);
    }
  }

  @Test
  void shouldJoinUniqueAttributeFirstWhenFilteringByManyAttributes() throws BadRequestException {
    TrackerEventQueryParams params = new TrackerEventQueryParams();
    TrackedEntityAttribute unique = attribute('z', true);
    List<TrackedEntityAttribute> attributes =
        List.of(
            attribute('a', false),
            attribute('b', false),
            attribute('c', false),
            attribute('d', false),
            attribute('e', false),
            unique);
    for (TrackedEntityAttribute attribute : attributes) {
      params.filterBy(attribute, new QueryFilter(QueryOperator.EQ, "value"));
    }

    String sql = getEventsSql(params);

    assertEquals(
        FIRST_ATTRIBUTE_JOIN_POSITION,
        tablePosition(sql, unique),
        "join on the unique attribute must be the first attribute join in: " + sql);
  }

  @Test
  void shouldJoinUniqueAttributeWithSelectiveFilterFirstWhenFilteringByUniqueAttributes()
      throws BadRequestException {
    TrackerEventQueryParams params = new TrackerEventQueryParams();
    TrackedEntityAttribute nonUnique = attribute('a', false);
    TrackedEntityAttribute uniqueWithLike = attribute('b', true);
    TrackedEntityAttribute uniqueWithEq = attribute('c', true);
    params.filterBy(nonUnique, new QueryFilter(QueryOperator.EQ, "value"));
    params.filterBy(uniqueWithLike, new QueryFilter(QueryOperator.LIKE, "value"));
    params.filterBy(uniqueWithEq, new QueryFilter(QueryOperator.EQ, "value"));

    String sql = getEventsSql(params);

    assertEquals(
        List.of(
            FIRST_ATTRIBUTE_JOIN_POSITION,
            FIRST_ATTRIBUTE_JOIN_POSITION + 1,
            FIRST_ATTRIBUTE_JOIN_POSITION + 2),
        List.of(
            tablePosition(sql, uniqueWithEq),
            tablePosition(sql, uniqueWithLike),
            tablePosition(sql, nonUnique)),
        "joins must be ordered unique with eq, unique with like, then non unique in: " + sql);
  }

  @Test
  void shouldFilterByDataElementInWhereClauseWhenOrderingByAttribute() throws BadRequestException {
    TrackerEventQueryParams params = new TrackerEventQueryParams();
    DataElement dataElement = createDataElement('X');
    dataElement.setValueType(ValueType.TEXT);
    params.filterBy(dataElement, new QueryFilter(QueryOperator.EQ, "value"));
    params.orderBy(attribute('a', false), SortDirection.ASC);

    String sql = getEventsSql(params);

    int where = sql.indexOf(" where ");
    int dataElementPredicate = sql.indexOf("ev.eventdatavalues #>> '{" + dataElement.getUid());
    assertTrue(
        where >= 0 && dataElementPredicate > where,
        "data element filter must be in the WHERE clause, not in the condition of a LEFT JOIN, in: "
            + sql);
  }

  private TrackedEntityAttribute attribute(char uniqueChar, boolean unique) {
    TrackedEntityAttribute attribute = createTrackedEntityAttribute(uniqueChar);
    attribute.setValueType(ValueType.TEXT);
    attribute.setUnique(unique);
    return attribute;
  }

  /** Runs the event query against a mocked database and returns the SQL that was sent. */
  @SuppressWarnings("unchecked")
  private String getEventsSql(TrackerEventQueryParams params) {
    store.getEvents(params);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(jdbcTemplate)
        .query(sql.capture(), any(SqlParameterSource.class), any(ResultSetExtractor.class));
    return sql.getValue();
  }

  /**
   * Returns the position of the attribute's table in the FROM clause: the joins before it, plus 1
   * for {@code trackerevent ev} and 1 for the attribute itself.
   */
  private static int tablePosition(String sql, TrackedEntityAttribute attribute) {
    int from = sql.indexOf(" from trackerevent ev ");
    assertTrue(from >= 0, "no 'from trackerevent ev' in: " + sql);
    int attributeJoin =
        sql.indexOf(" join trackedentityattributevalue as \"" + attribute.getUid() + "\"");
    assertTrue(attributeJoin > from, "no join on attribute " + attribute.getUid() + " in: " + sql);
    return StringUtils.countMatches(sql.substring(from, attributeJoin), " join ") + 2;
  }
}
