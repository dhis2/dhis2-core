package org.hisp.dhis.dxf2.events.importer.context;

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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;

@RunWith( Parameterized.class )
public class ProgramSupplierTest extends AbstractSupplierTest<Program>
{
    private ProgramSupplier subject;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Environment env;

    @Parameterized.Parameters
    public static Collection<String> data()
    {
        return Arrays.asList( IdScheme.UID.name(), IdScheme.ID.name(), IdScheme.CODE.name(), IdScheme.NAME.name() );
    }

    @Parameterized.Parameter
    public String idScheme;

    @Before
    public void setUp()
    {
        this.subject = new ProgramSupplier( jdbcTemplate, objectMapper, env );
        when( env.getActiveProfiles() ).thenReturn( new String[] { "tets" } );
    }

    @Override
    public void verifySupplier()
        throws SQLException
    {
        when( mockResultSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( false );

        when( mockResultSet.getLong( "id" ) ).thenReturn( 100L );
        when( mockResultSet.getString( "uid" ) ).thenReturn( "abcded" );
        when( mockResultSet.getString( "code" ) ).thenReturn( "ALFA" );
        when( mockResultSet.getString( "name" ) ).thenReturn( "My Program" );
        when( mockResultSet.getString( "type" ) ).thenReturn( ProgramType.WITHOUT_REGISTRATION.getValue() );
        when( mockResultSet.getString( "publicaccess" ) ).thenReturn( "rw------" );

        when( mockResultSet.getLong( "catcombo_id" ) ).thenReturn( 200L );
        when( mockResultSet.getString( "catcombo_uid" ) ).thenReturn( "389dh83" );
        when( mockResultSet.getString( "catcombo_code" ) ).thenReturn( "BETA" );
        when( mockResultSet.getString( "catcombo_name" ) ).thenReturn( "My CatCombo" );

        when( mockResultSet.getLong( "ps_id" ) ).thenReturn( 5L, 6L );
        when( mockResultSet.getString( "ps_uid" ) ).thenReturn( "abcd5", "abcd6" );
        when( mockResultSet.getString( "ps_code" ) ).thenReturn( "cod5", "cod6" );
        when( mockResultSet.getString( "ps_name" ) ).thenReturn( "name5", "name6" );
        when( mockResultSet.getInt( "sort_order" ) ).thenReturn( 1, 2 );
        when( mockResultSet.getString( "ps_public_access" ) ).thenReturn( "rw------" );
        when( mockResultSet.getString( "ps_feature_type" ) ).thenReturn( null, "POINT" );
        when( mockResultSet.getBoolean( "ps_repeatable" ) ).thenReturn( true, false );
        when( mockResultSet.getString( "validationstrategy" ) ).thenReturn( "ON_COMPLETE" );

        when( mockResultSet.getObject( "uid" ) ).thenReturn( "abcded" );
        when( mockResultSet.getObject( "id" ) ).thenReturn( 100L );
        when( mockResultSet.getObject( "name" ) ).thenReturn( "My Program" );
        when( mockResultSet.getObject( "code" ) ).thenReturn( "ALFA" );

        // mock resultset extraction
        mockResultSetExtractorWithoutParameters( mockResultSet );

        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.getIdSchemes().setProgramIdScheme( this.idScheme );

        final Map<String, Program> map = subject.get( importOptions, null );

        Program program = map.get( getIdByScheme() );
        assertThat( program, is( notNullValue() ) );
        assertThat( program.getProgramStages(), hasSize( 2 ) );
        assertThat( program.getId(), is( 100L ) );
        assertThat( program.getCode(), is( "ALFA" ) );
        assertThat( program.getName(), is( "My Program" ) );
        assertThat( program.getProgramType(), is( ProgramType.WITHOUT_REGISTRATION ) );
        assertThat( program.getPublicAccess(), is( "rw------" ) );
        assertThat( program.getCategoryCombo(), is( notNullValue() ) );
        assertThat( program.getCategoryCombo().getId(), is( 200L ) );
        assertThat( program.getCategoryCombo().getUid(), is( "389dh83" ) );
        assertThat( program.getCategoryCombo().getName(), is( "My CatCombo" ) );
        assertThat( program.getCategoryCombo().getCode(), is( "BETA" ) );
        // TODO assert more data
    }

    private String getIdByScheme()
    {
        if ( this.idScheme.equals( IdScheme.UID.name() ) )
        {
            return "abcded";
        }
        else if ( this.idScheme.equals( IdScheme.ID.name() ) )
        {
            return "100";
        }
        else if ( this.idScheme.equals( IdScheme.CODE.name() ) )
        {
            return "ALFA";
        }
        else if ( this.idScheme.equals( IdScheme.NAME.name() ) )
        {
            return "My Program";
        }
        return null;
    }
}