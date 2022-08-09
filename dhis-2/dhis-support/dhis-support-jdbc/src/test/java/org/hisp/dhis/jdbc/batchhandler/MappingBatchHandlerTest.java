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
package org.hisp.dhis.jdbc.batchhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.StatementDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@see MappingBatchHandler} tester.
 *
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
class MappingBatchHandlerTest
{
    @Mock
    private DataSource dataSource;

    @Mock
    ResultSet resultSet;

    @Mock
    ResultSetMetaData metaData;

    private static final String testTableName = "tablename";

    private static final List<String> testColumns = List.of( "colA", "colB", "colC", "colD" );

    private static final String testAutoIncrementColumn = "colA";

    private static final boolean testInclusiveUniqueColumns = true;

    private static final List<String> testIdentifierColumns = List.of( "colA", "colB" );

    private static final List<String> testUniqueColumns = List.of( "colA", "colB", "colC" );

    private static final List<Object> testValues = List.of( 1, 2, "C", "D" );

    private static final List<Object> testIdentifierValues = testValues.subList( 0, 2 );

    private static final List<Object> testUniqueValues = testValues.subList( 0, 3 );

    private static final Map<String, Object> testRow = Map.of(
        "colA", testValues.get( 0 ),
        "colB", testValues.get( 1 ),
        "colC", testValues.get( 2 ),
        "colD", testValues.get( 3 ) );

    private MappingBatchHandler target;

    @BeforeEach
    public void setUp()
        throws SQLException
    {
        JdbcConfiguration config = new JdbcConfiguration( StatementDialect.POSTGRESQL, dataSource );

        target = MappingBatchHandler.builder()
            .jdbcConfiguration( config )
            .tableName( testTableName )
            .columns( testColumns )
            .autoIncrementColumn( testAutoIncrementColumn )
            .inclusiveUniqueColumns( testInclusiveUniqueColumns )
            .identifierColumns( testIdentifierColumns )
            .uniqueColumns( testUniqueColumns )
            .build();
    }

    @Test
    void testGetTableName()
    {
        assertEquals( testTableName, target.getTableName() );
    }

    @Test
    void testGetColumns()
    {
        assertEquals( testColumns, target.getColumns() );
    }

    @Test
    void testGetAutoIncrementColumn()
    {
        assertEquals( testAutoIncrementColumn, target.getAutoIncrementColumn() );
    }

    @Test
    void testIsInclusiveUniqueColumns()
    {
        assertEquals( testInclusiveUniqueColumns, target.isInclusiveUniqueColumns() );
    }

    @Test
    void testGetIdentifierColumns()
    {
        assertEquals( testIdentifierColumns, target.getIdentifierColumns() );
    }

    @Test
    void testGetUniqueColumns()
    {
        assertEquals( testUniqueColumns, target.getUniqueColumns() );
    }

    @Test
    void testGetIdentifierValues()
    {
        assertEquals( testIdentifierValues, target.getIdentifierValues( testRow ) );
    }

    @Test
    void testGetUniqueValues()
    {
        assertEquals( testUniqueValues, target.getUniqueValues( testRow ) );
    }

    @Test
    void testGetValues()
    {
        assertEquals( testValues, target.getValues( testRow ) );
    }

    @Test
    void testMapRow()
        throws SQLException
    {
        Mockito.when( resultSet.getMetaData() ).thenReturn( metaData );

        Mockito.when( metaData.getColumnCount() ).thenReturn( testColumns.size() );

        Mockito.when( metaData.getColumnName( anyInt() ) ).thenAnswer( invocation -> {
            return testColumns.get( (Integer) invocation.getArguments()[0] );
        } );

        Mockito.when( resultSet.getObject( anyInt() ) ).thenAnswer( invocation -> {
            return testValues.get( (Integer) invocation.getArguments()[0] );
        } );

        assertEquals( testRow, target.mapRow( resultSet ) );
    }
}
