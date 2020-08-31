package org.hisp.dhis.dxf2.events.event;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.jdbc.statementbuilder.PostgreSQLStatementBuilder;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Luciano Fiandesio
 */
public class JdbcEventStoreTest
{
    private JdbcEventStore subject;

    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    protected SqlRowSet rowSet;

    @Mock
    private Environment env;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        when( jdbcTemplate.queryForRowSet( anyString() ) ).thenReturn( this.rowSet );

        when( jdbcTemplate.getDataSource() ).thenReturn( mock( DataSource.class ) );

        ObjectMapper objectMapper = new ObjectMapper();
        subject = new JdbcEventStore( new PostgreSQLStatementBuilder(), jdbcTemplate, objectMapper, currentUserService,
            manager, env );
    }

    @Test
    public void verifyEventDataValuesAreProcessedOnceForEachPSI()
    {
        mockRowSet();
        EventSearchParams eventSearchParams = new EventSearchParams();

        List<EventRow> rows = subject.getEventRows( eventSearchParams, new ArrayList<>() );
        assertThat( rows, hasSize( 1 ) );
        verify( rowSet, times( 4 ) ).getString( "psi_eventdatavalues" );
    }

    private void mockRowSet()
    {
        // Simulate 3 rows
        when( rowSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( true ).thenReturn( false );

        when( rowSet.getString( "psi_uid" ) ).thenReturn( "iuDUBa26aHN" );
        when( rowSet.getString( "ps_identifier" ) ).thenReturn( "PsUID000001" );
        when( rowSet.getString( "p_identifier" ) ).thenReturn( "PrgUID00001" );
        when( rowSet.getString( "ou_identifier" ) ).thenReturn( "OuUID000001" );
        when( rowSet.getString( "tei_uid" ) ).thenReturn( "iuXUBa26aHN" );
        when( rowSet.getString( "tei_ou" ) ).thenReturn( "" );
        when( rowSet.getString( "tei_ou_name" ) ).thenReturn( "Ngelehun CHC" );
        when( rowSet.getString( "tei_created" ) ).thenReturn( "2019-06-14 09:57:09.69" );

        when( rowSet.getBoolean( "tei_inactive" ) ).thenReturn( false );
        when( rowSet.getBoolean( "psi_deleted" ) ).thenReturn( false );

        when( rowSet.getString( "p_type" ) ).thenReturn( "with_registration" );

        when( rowSet.getString( "psi_eventdatavalues" ) ).thenReturn(
            "{\"hUQ5Hfcx1JA\": {\"value\": \"g8upMTyEZGZ\", \"created\": \"2019-06-14T09:57:30.564\", \"storedBy\": \"admin\", \"lastUpdated\": \"2019-06-14T09:57:30.564\", \"providedElsewhere\": false}}" );
    }
}