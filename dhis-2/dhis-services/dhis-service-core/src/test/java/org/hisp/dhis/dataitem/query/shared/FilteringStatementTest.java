/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dataitem.query.shared;

import static java.util.Collections.emptySet;
import static org.apache.commons.collections4.SetUtils.unmodifiableSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.valueTypeFiltering;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.ROOT_JUNCTION;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.VALUE_TYPES;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Unit tests for FilteringStatement.
 *
 * @author maikel arabori
 */
class FilteringStatementTest {

  @Test
  void testNameFilteringUsingOneColumnAndIlikeFilterIsSet() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(NAME, "abc");
    final String expectedStatement = " ( anyColumn ilike :name ) ";
    // When
    final String resultStatement = nameFiltering(aColumn, theParameterSource);
    // Then
    assertThat(resultStatement, is(expectedStatement));
  }

  @Test
  void testNameFilteringUsingOneColumnAndIlikeFilterIsNotSet() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource noFiltersParameterSource = new MapSqlParameterSource();
    // When
    final String resultStatement = nameFiltering(aColumn, noFiltersParameterSource);
    // Then
    assertThat(resultStatement, is(EMPTY));
  }

  @Test
  void testValueTypeFilteringUsingOneColumnAndIlikeFilterIsNull() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(NAME, null);
    // When
    final String actualResult = valueTypeFiltering(aColumn, filtersParameterSource);
    // Then
    assertThat(actualResult, is(EMPTY));
  }

  @Test
  void testValueTypeFilteringUsingOneColumnAndIlikeFilterIsEmpty() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(NAME, EMPTY);
    // When
    final String actualResult = valueTypeFiltering(aColumn, filtersParameterSource);
    // Then
    assertThat(actualResult, is(EMPTY));
  }

  @Test
  void testNameFilteringUsingTwoColumnAndIlikeFilterIsSet() {
    // Given
    final String column1 = "anyColumn";
    final String column2 = "otherColumn";
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(NAME, "abc");
    final String expectedStatement = " ( anyColumn ilike :name or otherColumn ilike :name ) ";
    // When
    final String resultStatement = nameFiltering(column1, column2, filtersParameterSource);
    // Then
    assertThat(resultStatement, is(expectedStatement));
  }

  @Test
  void testNameFilteringUsingTwoColumnAndIlikeFilterIsNotSet() {
    // Given
    final String column1 = "anyColumn";
    final String column2 = "otherColumn";
    final MapSqlParameterSource noFiltersParameterSource = new MapSqlParameterSource();
    final String expectedStatement = EMPTY;
    // When
    final String resultStatement = nameFiltering(column1, column2, noFiltersParameterSource);
    // Then
    assertThat(resultStatement, is(expectedStatement));
  }

  @Test
  void testValueTypeFilteringUsingOneColumnWhenFilterIsSet() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource theParameterSource =
        new MapSqlParameterSource().addValue(VALUE_TYPES, unmodifiableSet("NUMBER", "INTEGER"));
    final String expectedStatement = " ( anyColumn in (:valueTypes) ) ";
    // When
    final String resultStatement = valueTypeFiltering(aColumn, theParameterSource);
    // Then
    assertThat(resultStatement, is(expectedStatement));
  }

  @Test
  void testValueTypeFilteringUsingOneColumnWhenFilterIsNotSet() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource noFiltersParameterSource = new MapSqlParameterSource();
    final String expectedStatement = EMPTY;
    // When
    final String resultStatement = valueTypeFiltering(aColumn, noFiltersParameterSource);
    // Then
    assertThat(resultStatement, is(expectedStatement));
  }

  @Test
  void testValueTypeFilteringUsingOneColumnWhenFilterHasEmptySet() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(VALUE_TYPES, emptySet());
    // When
    final String actualResult = valueTypeFiltering(aColumn, filtersParameterSource);
    // Then
    assertThat(actualResult, is(EMPTY));
  }

  @Test
  void testValueTypeFilteringUsingOneColumnWhenFilterIsSetToNull() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(VALUE_TYPES, null);
    // When
    final String actualResult = valueTypeFiltering(aColumn, filtersParameterSource);
    // Then
    assertThat(actualResult, is(EMPTY));
  }

  @Test
  void testValueTypeFilteringUsingOneColumnWhenFilterIsNotSetInstance() {
    // Given
    final String aColumn = "anyColumn";
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(VALUE_TYPES, "String");
    // When
    final String actualResult = valueTypeFiltering(aColumn, filtersParameterSource);
    // Then
    assertThat(actualResult, is(EMPTY));
  }

  @Test
  void testRootJunctionFilteringWhenRootJunctionSetIsOR() {
    // Given
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(ROOT_JUNCTION, "or");
    // When
    final String actualResult = rootJunction(filtersParameterSource);
    // Then
    assertThat(actualResult, is("or"));
  }

  @Test
  void testRootJunctionFilteringWhenRootJunctionSetIsAND() {
    // Given
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(ROOT_JUNCTION, "and");
    // When
    final String actualResult = rootJunction(filtersParameterSource);
    // Then
    assertThat(actualResult, is("and"));
  }

  @Test
  void testRootJunctionFilteringWhenRootJunctionSetIsNotSet() {
    // Given
    final MapSqlParameterSource filtersParameterSource = new MapSqlParameterSource();
    // When
    final String actualResult = rootJunction(filtersParameterSource);
    // Then
    assertThat(actualResult, is("and"));
  }

  @Test
  void testRootJunctionFilteringWhenRootJunctionSetIsNull() {
    // Given
    final MapSqlParameterSource filtersParameterSource =
        new MapSqlParameterSource().addValue(ROOT_JUNCTION, null);
    // When
    final String actualResult = rootJunction(filtersParameterSource);
    // Then
    assertThat(actualResult, is("and"));
  }
}
