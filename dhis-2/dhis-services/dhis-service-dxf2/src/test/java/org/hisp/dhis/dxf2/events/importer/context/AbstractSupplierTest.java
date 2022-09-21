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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
public abstract class AbstractSupplierTest<T>
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    protected NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    protected ResultSet mockResultSet;

    @Captor
    protected ArgumentCaptor<String> sql;

    public void mockResultSetExtractor( ResultSet resultSetMock )
    {
        when( jdbcTemplate.query( sql.capture(), any( MapSqlParameterSource.class ), any( ResultSetExtractor.class ) ) )
            .thenAnswer( (Answer<Map<String, T>>) invocationOnMock -> {
                // Fetch the method arguments
                Object[] args = invocationOnMock.getArguments();

                // Fetch the row mapper instance from the arguments
                ResultSetExtractor<Map<String, T>> rm = (ResultSetExtractor<Map<String, T>>) args[2];

                // Invoke the row mapper
                return rm.extractData( resultSetMock );

            } );
    }

    public void mockResultSetExtractorWithoutParameters( ResultSet resultSetMock )
    {
        when( jdbcTemplate.query( anyString(), any( ResultSetExtractor.class ) ) )
            .thenAnswer( (Answer<Map<String, T>>) invocationOnMock -> {
                // Fetch the method arguments
                Object[] args = invocationOnMock.getArguments();

                // Fetch the row mapper instance from the arguments
                ResultSetExtractor<Map<String, T>> rm = (ResultSetExtractor<Map<String, T>>) args[1];

                // Invoke the row mapper
                return rm.extractData( resultSetMock );

            } );
    }

    @Test
    public void doVerifySupplier()
        throws SQLException
    {
        when( mockResultSet.next() ).thenReturn( true ).thenReturn( false );
        verifySupplier();
    }

    public abstract void verifySupplier()
        throws SQLException;
}
