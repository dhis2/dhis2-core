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
package org.hisp.dhis.analytics.common;

import static org.hisp.dhis.common.ValueType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.rowset.RowSetMetaDataImpl;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * // TODO: Improve unit tests and coverage
 *
 * Tests for {@link GridAdaptor}.
 *
 * @author maikel arabori
 */
class GridAdaptorTest extends DhisConvenienceTest
{
    private static GridAdaptor gridAdaptor;

    @Mock
    static private CurrentUserService currentUserService;

    @BeforeAll
    static void setUp()
    {
        gridAdaptor = new GridAdaptor( currentUserService );
    }

    @Test
    void testCreateGridSuccessfully()
        throws SQLException
    {
        // Given
        ResultSet resultSet = mock( ResultSet.class );
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        TeiQueryParams teiQueryParams = TeiQueryParams.builder().trackedEntityType( stubTrackedEntityType() )
            .commonParams( stubCommonParams() ).build();
        metaData.setColumnCount( 2 );
        metaData.setColumnName( 1, "col-1" );
        metaData.setColumnName( 2, "col-2" );

        when( resultSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( true ).thenReturn( false );
        when( resultSet.getMetaData() ).thenReturn( metaData );

        SqlRowSet sqlRowSet = new ResultSetWrappingSqlRowSet( resultSet );
        SqlQueryResult mockSqlResult = new SqlQueryResult( sqlRowSet );

        // When
        Grid grid = gridAdaptor.createGrid( mockSqlResult, teiQueryParams, new CommonQueryRequest() );

        // Then
        assertNotNull( grid, "Should not be null: grid" );
        assertFalse( grid.getHeaders().isEmpty(), "Should not be empty: headers" );
        assertFalse( grid.getRows().isEmpty(), "Should not be empty: rows" );
        assertEquals( 13, grid.getHeaders().size(), "Should have size of 13: headers" );
        assertEquals( 3, grid.getRows().size(), "Should have size of 3: rows" );
    }

    @Test
    void testCreateGridWithNullSqlQueryResult()
    {
        // Given
        SqlQueryResult nullSqlResult = null;

        // When
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> gridAdaptor.createGrid( nullSqlResult, TeiQueryParams.builder().build(),
                new CommonQueryRequest() ),
            "Expected exception not thrown: createGrid()" );

        // Then
        assertTrue( ex.getMessage().contains( "The 'sqlQueryResult' must not be null" ) );
    }

    private TrackedEntityType stubTrackedEntityType()
    {
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setTrackedEntityTypeAttributes( List.of(
            createTrackedEntityTypeAttribute( 'A', TEXT ),
            createTrackedEntityTypeAttribute( 'B', TEXT ) ) );

        return trackedEntityType;
    }

    private CommonParams stubCommonParams()
    {
        return CommonParams.builder().programs( List.of( createProgram( 'A' ) ) ).build();
    }
}
