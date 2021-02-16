/*
 * Copyright (c) 2004-2021, University of Oslo
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
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.skipValueType;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.valueTypeFiltering;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.VALUE_TYPES;

import org.junit.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Unit tests for FilteringStatement.
 *
 * @author maikel arabori
 */
public class FilteringStatementTest
{
    @Test
    public void testCommonFilteringUsingOneColumnAndIlikeFilterIsSet()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( NAME, "abc" );
        final String expectedStatement = " (anyColumn ILIKE :name)";

        // When
        final String resultStatement = nameFiltering( aColumn, theParameterSource );

        // Then
        assertThat( resultStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonFilteringUsingOneColumnAndIlikeFilterIsNotSet()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource noFiltersParameterSource = new MapSqlParameterSource();

        // When
        final String resultStatement = nameFiltering( aColumn, noFiltersParameterSource );

        // Then
        assertThat( resultStatement, is( EMPTY ) );
    }

    @Test
    public void testCommonFilteringUsingOneColumnAndIlikeFilterIsNull()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource filtersParameterSource = new MapSqlParameterSource()
            .addValue( NAME, null );

        // When
        final String actualResult = valueTypeFiltering( aColumn, filtersParameterSource );

        // Then
        assertThat( actualResult, is( EMPTY ) );
    }

    @Test
    public void testCommonFilteringUsingOneColumnAndIlikeFilterIsEmpty()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource filtersParameterSource = new MapSqlParameterSource()
            .addValue( NAME, EMPTY );

        // When
        final String actualResult = valueTypeFiltering( aColumn, filtersParameterSource );

        // Then
        assertThat( actualResult, is( EMPTY ) );
    }

    @Test
    public void testCommonFilteringUsingTwoColumnAndIlikeFilterIsSet()
    {
        // Given
        final String column1 = "anyColumn";
        final String column2 = "otherColumn";
        final MapSqlParameterSource filtersParameterSource = new MapSqlParameterSource()
            .addValue( NAME, "abc" );
        final String expectedStatement = " (anyColumn ILIKE :name OR otherColumn ILIKE :name)";

        // When
        final String resultStatement = nameFiltering( column1, column2, filtersParameterSource );

        // Then
        assertThat( resultStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonFilteringUsingTwoColumnAndIlikeFilterIsNotSet()
    {
        // Given
        final String column1 = "anyColumn";
        final String column2 = "otherColumn";
        final MapSqlParameterSource noFiltersParameterSource = new MapSqlParameterSource();
        final String expectedStatement = EMPTY;

        // When
        final String resultStatement = nameFiltering( column1, column2,
            noFiltersParameterSource );

        // Then
        assertThat( resultStatement, is( expectedStatement ) );
    }

    @Test
    public void testValueTypeFilteringUsingOneColumnWhenFilterIsSet()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( VALUE_TYPES, unmodifiableSet( "NUMBER", "INTEGER" ) );
        final String expectedStatement = " (anyColumn IN (:valueTypes))";

        // When
        final String resultStatement = valueTypeFiltering( aColumn, theParameterSource );

        // Then
        assertThat( resultStatement, is( expectedStatement ) );
    }

    @Test
    public void testValueTypeFilteringUsingOneColumnWhenFilterIsNotSet()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource noFiltersParameterSource = new MapSqlParameterSource();
        final String expectedStatement = EMPTY;

        // When
        final String resultStatement = valueTypeFiltering( aColumn, noFiltersParameterSource );

        // Then
        assertThat( resultStatement, is( expectedStatement ) );
    }

    @Test
    public void testValueTypeFilteringUsingOneColumnWhenFilterHasEmptySet()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource filtersParameterSource = new MapSqlParameterSource()
            .addValue( VALUE_TYPES, emptySet() );

        // When
        final String actualResult = valueTypeFiltering( aColumn, filtersParameterSource );

        // Then
        assertThat( actualResult, is( EMPTY ) );
    }

    @Test
    public void testValueTypeFilteringUsingOneColumnWhenFilterIsSetToNull()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource filtersParameterSource = new MapSqlParameterSource()
            .addValue( VALUE_TYPES, null );

        // When
        final String actualResult = valueTypeFiltering( aColumn, filtersParameterSource );

        // Then
        assertThat( actualResult, is( EMPTY ) );
    }

    @Test
    public void testValueTypeFilteringUsingOneColumnWhenFilterIsNotSetInstance()
    {
        // Given
        final String aColumn = "anyColumn";
        final MapSqlParameterSource filtersParameterSource = new MapSqlParameterSource()
            .addValue( VALUE_TYPES, "String" );

        // When
        final String actualResult = valueTypeFiltering( aColumn, filtersParameterSource );

        // Then
        assertThat( actualResult, is( EMPTY ) );
    }

    @Test
    public void testSkipNumberValueTypeWhenNumberTypeIsPresentInParameters()
    {
        // Given
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( VALUE_TYPES, unmodifiableSet( "NUMBER", "INTEGER" ) );

        // When
        final boolean actualResult = skipValueType( NUMBER, theParameterSource );

        // Then
        assertThat( actualResult, is( false ) );
    }

    @Test
    public void testSkipNumberValueTypeWhenNumberTypeIsNotPresentInParameters()
    {
        // Given
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( VALUE_TYPES, unmodifiableSet( "BOOLEAN", "INTEGER" ) );
        final boolean expectedResult = true;

        // When
        final boolean actualResult = skipValueType( NUMBER, theParameterSource );

        // Then
        assertThat( actualResult, is( expectedResult ) );
    }
}
