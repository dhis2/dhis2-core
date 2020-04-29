package org.hisp.dhis.dxf2.events.event.context;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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

/**
 * @author Luciano Fiandesio
 */
public class OrganisationUnitSupplierTest
{

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private OrganisationUnitSupplier subject;

    @Before
    public void setUp()
    {
        this.subject = new OrganisationUnitSupplier( jdbcTemplate );
    }

    @Test
    public void handleNullEvents()
    {
        assertNotNull( subject.get( null ) );
    }

    @Test
    public void h1() throws SQLException {
        ResultSet resultSetMock = Mockito.mock(ResultSet.class);
        Mockito.when(resultSetMock.next()).thenReturn(true).thenReturn(false);
        Event event = new Event();
        event.setUid(CodeGenerator.generateUid());
        event.setOrgUnit( "abcded" );

        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(ResultSetExtractor.class)))
                .thenAnswer((Answer<Map<String, OrganisationUnit>>) invocationOnMock -> {
                // Fetch the method arguments
                Object[] args = invocationOnMock.getArguments();

                // Fetch the row mapper instance from the arguments
                ResultSetExtractor<Map<String, OrganisationUnit>> rm = (ResultSetExtractor<Map<String, OrganisationUnit>>) args[2];

                // Create a mock result set and setup an expectation on it
                when( resultSetMock.getString( "uid" ) ).thenReturn( "abcded" );
                when( resultSetMock.getString( "path" ) ).thenReturn( "/aaaa/bbbb/cccc/abcded" );

                // Invoke the row mapper
                Map<String, OrganisationUnit> actual = rm.extractData( resultSetMock );


                // Assert the result of the row mapper execution
                // assertEquals(expected, actual);

                // Return your created list for the template#query call
                return actual;
                });


        Map<String, OrganisationUnit> stringOrganisationUnitMap = subject.get(Collections.singletonList(event));

        System.out.println(stringOrganisationUnitMap);
    }

}