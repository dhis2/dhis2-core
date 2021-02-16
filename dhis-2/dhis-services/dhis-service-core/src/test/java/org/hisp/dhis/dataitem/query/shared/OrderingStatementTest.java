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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_NAME_ORDER;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.NAME_ORDER;

import org.junit.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Unit tests for OrderingStatement.
 *
 * @author maikel arabori
 */
public class OrderingStatementTest
{
    @Test
    public void testCommonOrderingWhenOrderIsAsc()
    {
        // Given
        final String aGroupOfColumns = "anyColumn, otherColumn";
        final String otherGroupOfColumns = "anyColumn, otherColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( DISPLAY_NAME_ORDER, "ASC" );
        final String expectedStatement = " ORDER BY anyColumn ASC, otherColumn ASC";

        // When
        final String actualStatement = ordering( aGroupOfColumns, otherGroupOfColumns, theParameterSource );

        // Then
        assertThat( actualStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonOrderingWhenOrderIsDescAndNameOrderIsSet()
    {
        // Given
        final String displayOrderingColumns = "anyColumn, anyColumn2";
        final String nameOrderingColumns = "otherColumn, otherColumn2";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( NAME_ORDER, "DESC" );
        final String expectedStatement = " ORDER BY otherColumn DESC, otherColumn2 DESC";

        // When
        final String actualStatement = ordering( displayOrderingColumns, nameOrderingColumns, theParameterSource );

        // Then
        assertThat( actualStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonOrderingWhenOrderIsNotSet()
    {
        // Given
        final String displayOrderingColumn = "anyColumn";
        final String nameOrderingColumn = "otherColumn";
        final MapSqlParameterSource noParameterSource = new MapSqlParameterSource();

        // When
        final String actualStatement = ordering( displayOrderingColumn, nameOrderingColumn, noParameterSource );

        // Then
        assertThat( actualStatement, is( EMPTY ) );
    }

    @Test
    public void testCommonOrderingWhenParameterSourceIsNull()
    {
        // Given
        final String displayOrderingColumn = "anyColumn";
        final String nameOrderingColumn = "otherColumn";
        final MapSqlParameterSource nullParameterSource = null;

        // When
        final String actualStatement = ordering( displayOrderingColumn, nameOrderingColumn, nullParameterSource );

        // Then
        assertThat( actualStatement, is( EMPTY ) );
    }

    @Test
    public void testCommonOrderingWhenOrderFilterIsSetToNullAndNameOrderIsSet()
    {
        // Given
        final String displayOrderingColumn = "anyColumn";
        final String nameOrderingColumn = "otherColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( NAME_ORDER, null );

        // When
        final String actualStatement = ordering( displayOrderingColumn, nameOrderingColumn, theParameterSource );

        // Then
        assertThat( actualStatement, is( EMPTY ) );
    }

    @Test
    public void testCommonOrderingWhenOrderFilterIsSetToEmptyAndNameOrderIsSet()
    {
        // Given
        final String displayOrderingColumn = "anyColumn";
        final String nameOrderingColumn = "otherColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( NAME_ORDER, EMPTY );

        // When
        final String actualStatement = ordering( displayOrderingColumn, nameOrderingColumn, theParameterSource );

        // Then
        assertThat( actualStatement, is( EMPTY ) );
    }

    @Test
    public void testCommonOrderingWhenOneColumnIsNullAndNameOrderIsSet()
    {
        // Given
        final String aNullColumn = null;
        final String otherColumn = "otherColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( NAME_ORDER, "DESC" );
        final String expectedStatement = " ORDER BY otherColumn DESC";

        // When
        final String actualStatement = ordering( aNullColumn, otherColumn, theParameterSource );

        // Then
        assertThat( actualStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonOrderingWhenOneColumnIsEmptyAndNameOrderIsSet()
    {
        // Given
        final String anEmptyColumn = EMPTY;
        final String otherColumn = "otherColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( NAME_ORDER, "DESC" );
        final String expectedStatement = " ORDER BY otherColumn DESC";

        // When
        final String actualStatement = ordering( anEmptyColumn, otherColumn, theParameterSource );

        // Then
        assertThat( actualStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonOrderingWhenOneColumnIsNullAndDisplayNameOrderIsSet()
    {
        // Given
        final String aNullColumn = null;
        final String otherColumn = "otherColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( DISPLAY_NAME_ORDER, "ASC" );
        final String expectedStatement = EMPTY;

        // When
        final String actualStatement = ordering( aNullColumn, otherColumn, theParameterSource );

        // Then
        assertThat( actualStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonOrderingWhenOneColumnIsEmptyAndDisplayNameOrderIsSet()
    {
        // Given
        final String anEmptyColumn = EMPTY;
        final String otherColumn = "otherColumn";
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( DISPLAY_NAME_ORDER, "ASC" );
        final String expectedStatement = EMPTY;

        // When
        final String actualStatement = ordering( anEmptyColumn, otherColumn, theParameterSource );

        // Then
        assertThat( actualStatement, is( expectedStatement ) );
    }

    @Test
    public void testCommonOrderingWhenBothColumnsAreNullAndNameOrderIsSet()
    {
        // Given
        final String aNullColumn = null;
        final String anotherNullColumn = null;
        final MapSqlParameterSource theParameterSource = new MapSqlParameterSource()
            .addValue( NAME_ORDER, "DESC" );
        final String expectedStatement = EMPTY;

        // When
        final String actualStatement = ordering( aNullColumn, anotherNullColumn, theParameterSource );

        // Then
        assertThat( actualStatement, is( expectedStatement ) );
    }
}
