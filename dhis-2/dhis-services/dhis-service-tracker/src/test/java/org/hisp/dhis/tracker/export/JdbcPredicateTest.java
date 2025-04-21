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

import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.tracker.export.JdbcPredicate.mapPredicatesToSql;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.JdbcPredicate.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

class JdbcPredicateTest {
  private TrackedEntityAttribute tea;
  private DataElement de;

  @BeforeEach
  void setUp() {
    tea = trackedEntityAttribute();

    de = new DataElement();
    de.setValueType(ValueType.TEXT);
    de.setUid(UID.generate().getValue());
  }

  @Test
  void shouldCreateFilterGivenUnaryOperatorOnAttribute() {
    tea.setValueType(ValueType.NUMBER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.NNULL);

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertEquals(
"""
lower("%s".value) is not null"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertNoParameter(filter);
  }

  @Test
  void shouldCreateFilterGivenUnaryOperatorOnDataElement() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.NNULL);

    JdbcPredicate filter = JdbcPredicate.of(de, queryFilter, "ev");

    assertEquals(
"""
ev.eventdatavalues -> '%s' is not null"""
            .formatted(de.getUid()),
        filter.getSql());
    assertNoParameter(filter);
  }

  @Test
  void shouldCreateFilterGivenTextInputWithInOperatorForValueTypeText() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.IN, "summer;Winter;SPRING");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) in (:"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "summer", "winter", "spring");
  }

  @Test
  void shouldCreateFilterGivenTextInputWithInOperatorForValueTypeTextAndDataElement() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.IN, "summer;Winter;SPRING");

    JdbcPredicate filter = JdbcPredicate.of(de, queryFilter, "ev");

    assertStartsWith(
"""
lower(ev.eventdatavalues #>> '{%s, value}') in (:"""
            .formatted(de.getUid()),
        filter.getSql());
    assertParameter(de, filter, Types.VARCHAR, "summer", "winter", "spring");
  }

  @Test
  void shouldCreateFilterGivenNumericInputWithInOperatorForValueTypeInteger() {
    tea.setValueType(ValueType.INTEGER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.IN, "42;17;7");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
cast ("%s".value as integer) in (:"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.INTEGER, 42, 17, 7);
  }

  @Test
  void shouldCreateFilterGivenNumericInputWithInOperatorForValueTypeIntegerAndDataElement() {
    de.setValueType(ValueType.INTEGER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.IN, "42;17;7");

    JdbcPredicate filter = JdbcPredicate.of(de, queryFilter, "ev");

    assertStartsWith(
"""
cast (ev.eventdatavalues #>> '{%s, value}' as integer) in (:"""
            .formatted(de.getUid()),
        filter.getSql());
    assertParameter(de, filter, Types.INTEGER, 42, 17, 7);
  }

  @Test
  void shouldCreateFilterGivenNumericInputWithInOperatorForValueTypeNumber() {
    tea.setValueType(ValueType.NUMBER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.IN, "42.5;17.2;7");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
cast ("%s".value as numeric) in (:"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(
        tea,
        filter,
        Types.NUMERIC,
        new java.math.BigDecimal("42.5"),
        new java.math.BigDecimal("17.2"),
        new java.math.BigDecimal("7"));
  }

  @Test
  void
      shouldCreateFilterGivenTextInputWithLikeBasedOperatorForValueTypeTextAndWildcardCharacters() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EW, "80%_60%");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) like :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "%80\\%\\_60\\%");
  }

  @Test
  void shouldCreateFilterGivenTextInputWithEqOperatorForValueTypeText() {
    // % is not a wildcard in operators other than SQL like so will not be escaped
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "summer % DAY");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) = :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "summer % day");
  }

  @Test
  void shouldCreateFilterGivenNumericInputWithEqOperatorForValueTypeText() {
    tea.setValueType(ValueType.TEXT);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "42.5");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) = :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "42.5");
  }

  @Test
  void shouldCreateFilterGivenNumericInputWithEqOperatorForValueTypeNumber() {
    tea.setValueType(ValueType.NUMBER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "42.5");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
cast ("%s".value as numeric) = :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.NUMERIC, new java.math.BigDecimal("42.5"));
  }

  @Test
  void shouldCreateFilterGivenNumericInputWithEqOperatorForValueTypeInteger() {
    tea.setValueType(ValueType.INTEGER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "42");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
cast ("%s".value as integer) = :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.INTEGER, 42);
  }

  @Test
  void shouldCreateFilterGivenDateInputWithEqOperatorForValueTypeDate() {
    tea.setValueType(ValueType.DATE);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.GT, "2013-04-01");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) > :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "2013-04-01");
  }

  @Test
  void shouldCreateFilterGivenTextInputWithLikeOperatorForValueTypeText() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.LIKE, "summer");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) like :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "%summer%");
  }

  @Test
  void shouldCreateFilterGivenTextInputWithSWOperatorForValueTypeText() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.SW, "summer");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) like :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "summer%");
  }

  @Test
  void shouldCreateFilterGivenTextInputWithEWOperatorForValueTypeText() {
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EW, "summer");

    JdbcPredicate filter = JdbcPredicate.of(tea, queryFilter);

    assertStartsWith(
"""
lower("%s".value) like :"""
            .formatted(tea.getUid()),
        filter.getSql());
    assertParameter(tea, filter, Types.VARCHAR, "%summer");
  }

  @Test
  void shouldFailIfValueTypeIsIntegerButInputIsNot() {
    tea.setValueType(ValueType.INTEGER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "42.5");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JdbcPredicate.of(tea, queryFilter));

    assertContains("attribute", exception.getMessage());
    assertContains("value type is INTEGER but the value `42.5`", exception.getMessage());
  }

  @Test
  void shouldFailIfValueTypeIsIntegerButInInputIsNot() {
    tea.setValueType(ValueType.INTEGER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.IN, "42;17.5;7");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JdbcPredicate.of(tea, queryFilter));

    assertContains("value type is INTEGER but the value `17.5`", exception.getMessage());
  }

  @Test
  void shouldFailIfValueTypeIsNumberButInputIsNot() {
    tea.setValueType(ValueType.NUMBER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.EQ, "not a number");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JdbcPredicate.of(tea, queryFilter));

    assertContains("value type is NUMBER but the value `not a number`", exception.getMessage());
  }

  @Test
  void shouldFailIfValueTypeIsNumberButInInputIsNot() {
    tea.setValueType(ValueType.NUMBER);
    QueryFilter queryFilter = new QueryFilter(QueryOperator.IN, "42.5;not a number;7");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> JdbcPredicate.of(tea, queryFilter));

    assertContains("value type is NUMBER but the value `not a number`", exception.getMessage());
  }

  @Test
  void shouldMapPredicatesMap() {
    MapSqlParameterSource params = new MapSqlParameterSource();

    String sql = mapPredicatesToSql(Map.of(), params);

    assertEquals("", sql);
  }

  @Test
  void shouldMapPredicatesMapWithEmptyPredicates() {
    TrackedEntityAttribute tea2 = trackedEntityAttribute();
    MapSqlParameterSource params = new MapSqlParameterSource();

    String sql = mapPredicatesToSql(Map.of(tea, List.of(), tea2, List.of()), params);

    assertEquals("", sql);
  }

  @Test
  void shouldMapPredicates() {
    QueryFilter notNull = new QueryFilter(QueryOperator.NNULL);
    TrackedEntityAttribute tea2 = trackedEntityAttribute();
    TrackedEntityAttribute tea3 = trackedEntityAttribute();

    // use LinkedHashMap for deterministic order so assertion is not flaky
    Map<TrackedEntityAttribute, List<JdbcPredicate>> predicates = new LinkedHashMap<>();
    predicates.put(tea, List.of(JdbcPredicate.of(tea, notNull), JdbcPredicate.of(tea, notNull)));
    predicates.put(tea2, List.of(JdbcPredicate.of(tea2, notNull)));
    predicates.put(tea3, List.of());

    MapSqlParameterSource params = new MapSqlParameterSource();

    String sql = mapPredicatesToSql(predicates, params);

    assertEquals(
"""
lower("%s".value) is not null and lower("%s".value) is not null and lower("%s".value) is not null"""
            .formatted(tea.getUid(), tea.getUid(), tea2.getUid()),
        sql);
  }

  @Test
  void shouldMapPredicateParameters() {
    QueryFilter eq = new QueryFilter(QueryOperator.EQ, "blue");

    // use LinkedHashMap for deterministic order so assertion is not flaky
    Map<TrackedEntityAttribute, List<JdbcPredicate>> predicates = new LinkedHashMap<>();
    JdbcPredicate predicate = JdbcPredicate.of(tea, eq);
    predicates.put(tea, List.of(predicate));

    MapSqlParameterSource params = new MapSqlParameterSource();

    String sql = mapPredicatesToSql(predicates, params);

    assertStartsWith(
"""
lower("%s".value) = :"""
            .formatted(tea.getUid()),
        sql);
    assertTrue(predicate.getParameter().isPresent(), "expected a getParameter in the predicate");
    Parameter parameter = predicate.getParameter().get();
    assertTrue(
        params.hasValue(parameter.name()),
        "expected the predicates getParameter in the getParameter source");
    assertEquals(
        predicate.getParameter().get().value(),
        params.getValue(predicate.getParameter().get().name()),
        "expected the predicates getParameter in the getParameter source");
  }

  private static TrackedEntityAttribute trackedEntityAttribute() {
    TrackedEntityAttribute tea = new TrackedEntityAttribute();
    tea.setValueType(ValueType.TEXT);
    tea.setUid(UID.generate().getValue());
    return tea;
  }

  private static void assertNoParameter(JdbcPredicate filter) {
    assertTrue(
        filter.getParameter().isEmpty(),
        () -> "getParameter should be empty but got " + filter.getParameter().get());
  }

  private static void assertParameter(
      ValueTypedDimensionalItemObject valueTypedObject,
      JdbcPredicate filter,
      int expectedType,
      Object... expectedValue) {
    assertTrue(filter.getParameter().isPresent(), "expected a getParameter but got none");
    Parameter parameter = filter.getParameter().get();
    assertStartsWith("filter_" + valueTypedObject.getUid(), parameter.name());
    assertContains(
        ":" + parameter.name(),
        filter.getSql(),
        ("SQL `%s` must reference getParameter `%s`").formatted(filter.getSql(), parameter.name()));

    SqlParameterValue sqlParameterValue = parameter.value();
    assertAll(
        "assert SqlParameterValue",
        () ->
            assertEquals(
                expectedType, sqlParameterValue.getSqlType(), "mismatch in java.getSql.Types"),
        () -> assertEquals(List.of(expectedValue), sqlParameterValue.getValue()));
  }
}
