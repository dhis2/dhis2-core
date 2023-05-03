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
package org.hisp.dhis.dxf2.events.importer.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class ProgramInstanceSupplierTest extends AbstractSupplierTest<Enrollment>
{

    private ProgramInstanceSupplier subject;

    @Mock
    private ProgramSupplier programSupplier;

    @BeforeEach
    void setUp()
    {
        this.subject = new ProgramInstanceSupplier( jdbcTemplate, programSupplier );
    }

    @Test
    void handleNullEvents()
    {
        assertNotNull( subject.get( ImportOptions.getDefaultImportOptions(), new HashMap<>(), null ) );
    }

    @Test
    void verifySupplier()
        throws SQLException
    {
        // mock resultset data
        when( mockResultSet.getLong( "programinstanceid" ) ).thenReturn( 100L );
        when( mockResultSet.getString( "uid" ) ).thenReturn( "abcded" );
        when( mockResultSet.getString( "tei_uid" ) ).thenReturn( "efghil" );
        when( mockResultSet.getString( "tei_ou_uid" ) ).thenReturn( "ouabcde" );
        when( mockResultSet.getString( "tei_ou_path" ) ).thenReturn( "/ouabcde" );
        when( mockResultSet.getLong( "programid" ) ).thenReturn( 999L );
        // create event to import
        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setEnrollment( "abcded" );
        // mock resultset extraction
        mockResultSetExtractor( mockResultSet );
        // create a Program for the ProgramSupplier
        Program program = new Program();
        program.setId( 999L );
        program.setUid( "prabcde" );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( "prabcde", program );
        final ImportOptions defaultImportOptions = ImportOptions.getDefaultImportOptions();
        when( programSupplier.get( defaultImportOptions, Collections.singletonList( event ) ) )
            .thenReturn( programMap );
        Map<String, Enrollment> map = subject.get( defaultImportOptions, new HashMap<>(),
            Collections.singletonList( event ) );
        Enrollment enrollment = map.get( event.getUid() );
        assertThat( enrollment, is( notNullValue() ) );
        assertThat( enrollment.getId(), is( 100L ) );
        assertThat( enrollment.getUid(), is( "abcded" ) );
        assertThat( enrollment.getEntityInstance(), is( notNullValue() ) );
        assertThat( enrollment.getEntityInstance().getUid(), is( "efghil" ) );
        assertThat( enrollment.getEntityInstance().getOrganisationUnit(), is( notNullValue() ) );
        assertThat( enrollment.getEntityInstance().getOrganisationUnit().getUid(), is( "ouabcde" ) );
        assertThat( enrollment.getProgram(), is( notNullValue() ) );
        assertThat( enrollment.getProgram().getUid(), is( "prabcde" ) );
    }
}
