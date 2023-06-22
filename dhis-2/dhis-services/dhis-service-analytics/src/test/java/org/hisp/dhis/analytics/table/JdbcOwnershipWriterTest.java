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
package org.hisp.dhis.analytics.table;

import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.JANUARY;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.ENDDATE;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.OU;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.STARTDATE;
import static org.hisp.dhis.analytics.table.JdbcOwnershipWriter.TEIUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hisp.dhis.jdbc.batchhandler.MappingBatchHandler;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.StatementDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@see JdbcOwnershipWriter} Tester.
 *
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
class JdbcOwnershipWriterTest
{
    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    private MappingBatchHandler batchHandler;

    private JdbcOwnershipWriter writer;

    private static final String teiA = "teiAaaaaaaa";

    private static final String teiB = "teiBbbbbbbb";

    private static final String ouA = "ouAaaaaaaaa";

    private static final String ouB = "ouBbbbbbbbb";

    private static final Date date_2022_01 = new GregorianCalendar( 2022, JANUARY, 1 ).getTime();

    private static final Date date_2022_02 = new GregorianCalendar( 2022, FEBRUARY, 1 ).getTime();

    private static final List<String> columns = List.of( TEIUID, OU, STARTDATE, ENDDATE );

    @BeforeEach
    public void setUp()
        throws SQLException
    {
        JdbcConfiguration config = new JdbcConfiguration( StatementDialect.POSTGRESQL, dataSource );

        when( dataSource.getConnection() ).thenReturn( connection );
        when( connection.createStatement() ).thenReturn( statement );

        batchHandler = MappingBatchHandler.builder()
            .jdbcConfiguration( config )
            .tableName( "analytics_ownership_programUidA" )
            .columns( columns ).build();

        batchHandler.init();

        writer = JdbcOwnershipWriter.getInstance( batchHandler );
    }

    @Test
    void testWriteNoOwnershipChanges()
        throws SQLException
    {
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, null ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouB, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouB, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouB, ENDDATE, null ) );

        batchHandler.flush();

        verify( statement, never() ).executeUpdate( any() );
    }

    @Test
    void testWriteOneOwnershipChange()
    {
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouB, ENDDATE, null ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, null ) );

        batchHandler.flush();

        assertEquals(
            "insert into analytics_ownership_programUidA (\"teiuid\",\"ou\",\"startdate\",\"enddate\") values " +
                "('teiAaaaaaaa','ouAaaaaaaaa','1000-01-01','2022-02-01')," +
                "('teiAaaaaaaa','ouBbbbbbbbb','2022-02-02','9999-12-31')",
            getUpdateSql() );
    }

    @Test
    void testWriteTwoOwnershipChanges()
    {
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouB, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, null ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, null ) );

        batchHandler.flush();

        assertEquals(
            "insert into analytics_ownership_programUidA (\"teiuid\",\"ou\",\"startdate\",\"enddate\") values " +
                "('teiAaaaaaaa','ouAaaaaaaaa','1000-01-01','2022-01-01')," +
                "('teiAaaaaaaa','ouBbbbbbbbb','2022-01-02','2022-02-01')," +
                "('teiAaaaaaaa','ouAaaaaaaaa','2022-02-02','9999-12-31')",
            getUpdateSql() );
    }

    @Test
    void testWriteThreeOwnershipChanges()
    {
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouB, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiA, OU, ouA, ENDDATE, null ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, date_2022_01 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouA, ENDDATE, date_2022_02 ) );
        writer.write( mapOf( TEIUID, teiB, OU, ouB, ENDDATE, null ) );

        batchHandler.flush();

        assertEquals(
            "insert into analytics_ownership_programUidA (\"teiuid\",\"ou\",\"startdate\",\"enddate\") values " +
                "('teiAaaaaaaa','ouAaaaaaaaa','1000-01-01','2022-01-01')," +
                "('teiAaaaaaaa','ouBbbbbbbbb','2022-01-02','2022-02-01')," +
                "('teiAaaaaaaa','ouAaaaaaaaa','2022-02-02','9999-12-31')," +
                "('teiBbbbbbbb','ouAaaaaaaaa','1000-01-01','2022-02-01')," +
                "('teiBbbbbbbb','ouBbbbbbbbb','2022-02-02','9999-12-31')",
            getUpdateSql() );
    }

    @Test
    void testWriteWhenEndDateIsNull()
        throws IllegalAccessException
    {
        JdbcOwnershipWriter writer = JdbcOwnershipWriter.getInstance( batchHandler );
        Map<String, Object> prevRow = new HashMap<>();
        writeField( writer, "prevRow", prevRow, true );

        writer.write( mapOf( TEIUID, teiB, OU, ouB, ENDDATE, null ) );

        assertNotNull( prevRow.get( ENDDATE ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Creates a map of three key/value pairs that allows nulls (because the
     * database can return nulls and the logic relies on that).
     */
    private <K, V> Map<K, V> mapOf( K key1, V value1, K key2, V value2, K key3, V value3 )
    {
        HashMap<K, V> map = new HashMap<>();
        map.put( key1, value1 );
        map.put( key2, value2 );
        map.put( key3, value3 );
        return map;
    }

    /**
     * Gets a list of invocations of a mocked object
     */
    private String getUpdateSql()
    {
        List<Invocation> invocations = new ArrayList<>( mockingDetails( statement ).getInvocations() );
        assertEquals( 2, invocations.size() );
        assertEquals( "executeUpdate", invocations.get( 0 ).getMethod().getName() );
        assertEquals( "close", invocations.get( 1 ).getMethod().getName() );

        return invocations.get( 0 ).getArgument( 0 );
    }
}
