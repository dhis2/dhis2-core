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

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.dxf2.events.importer.context.AttributeOptionComboLoader.SQL_GET_CATEGORYOPTIONCOMBO;
import static org.hisp.dhis.dxf2.events.importer.context.AttributeOptionComboLoader.SQL_GET_CATEGORYOPTIONCOMBO_BY_CATEGORYIDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IllegalQueryException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * @author Luciano Fiandesio
 */
public class AttributeOptionComboLoaderTest
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    protected JdbcTemplate jdbcTemplate;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AttributeOptionComboLoader subject;

    @Before
    public void setUp()
    {
        subject = new AttributeOptionComboLoader( jdbcTemplate );
    }

    @Test
    public void verifyGetDefaultCategoryOptionCombo()
    {
        when( jdbcTemplate.queryForObject( anyString(), any( RowMapper.class ) ) )
            .thenReturn( new CategoryOptionCombo() );

        CategoryOptionCombo categoryOptionCombo = subject.getDefault();

        assertThat( categoryOptionCombo, is( notNullValue() ) );
        verify( jdbcTemplate ).queryForObject( sqlCaptor.capture(), any( RowMapper.class ) );

        final String sql = sqlCaptor.getValue();

        assertThat( sql, is( replace( SQL_GET_CATEGORYOPTIONCOMBO, "key", "categoryoptioncomboid", "resolvedScheme",
            "name = 'default'" ) ) );
    }

    @Test
    public void verifyGetCategoryOption()
    {
        get( IdScheme.ID, "12345", "categoryoptioncomboid = 12345" );
        get( IdScheme.UID, "abcdef", "uid = 'abcdef'" );
        get( IdScheme.NAME, "alfa", "name = 'alfa'" );

    }

    @Test
    public void verifyGetAttributeOptionComboWithNullCategoryCombo()
    {
        thrown.expect( IllegalQueryException.class );
        thrown.expectMessage( "Illegal category combo" );
        subject.getAttributeOptionCombo( null, "", "", IdScheme.UID );
    }

    @Test
    public void verifyGetAttributeOptionComboWithNonExistingCategoryOption()
    {
        thrown.expect( IllegalQueryException.class );
        thrown.expectMessage( "Illegal category option identifier: abcdef" );
        CategoryCombo cc = new CategoryCombo();
        subject.getAttributeOptionCombo( cc, "abcdef", "", IdScheme.UID );
    }

    @Test
    public void verifyGetAttributeOptionCombo()
    {
        // prepare data
        CategoryCombo cc = createCategoryCombo( 'B' );
        CategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.setId( 100L );

        when( jdbcTemplate.queryForObject(
            eq( "select categoryoptionid, uid, code, name from dataelementcategoryoption where uid = 'abcdef'" ),
            any( RowMapper.class ) ) ).thenReturn( categoryOption );

        when( jdbcTemplate.query( anyString(), any( RowMapper.class ) ) )
            .thenReturn( singletonList( createCategoryOptionCombo( cc, categoryOption ) ) );

        // method under test
        CategoryOptionCombo categoryOptionCombo = subject.getAttributeOptionCombo( cc, "abcdef", "", IdScheme.UID );

        // assertions
        assertThat( categoryOptionCombo, is( notNullValue() ) );
        verify( jdbcTemplate ).query( sqlCaptor.capture(), any( RowMapper.class ) );

        final String sql = sqlCaptor.getValue();
        assertThat( sql, is( replace( SQL_GET_CATEGORYOPTIONCOMBO_BY_CATEGORYIDS, "resolvedScheme",
            "uid = '" + cc.getUid() + "'", "option_ids", "'100'" ) ) );
    }

    private void get( IdScheme idScheme, String key, String resolvedId )
    {
        when( jdbcTemplate.queryForObject( anyString(), any( RowMapper.class ) ) )
            .thenReturn( new CategoryOptionCombo() );

        CategoryOptionCombo categoryOptionCombo = subject.getCategoryOptionCombo( idScheme, key );

        assertThat( categoryOptionCombo, is( notNullValue() ) );
        verify( jdbcTemplate ).queryForObject( sqlCaptor.capture(), any( RowMapper.class ) );

        final String sql = sqlCaptor.getValue();

        assertThat( sql, is(
            replace( SQL_GET_CATEGORYOPTIONCOMBO, "key", "categoryoptioncomboid", "resolvedScheme", resolvedId ) ) );
        reset( jdbcTemplate );
    }

    private String replace( String sql, String... keyVal )
    {

        Map<String, String> vals = new HashMap<>();

        for ( int i = 0; i < keyVal.length - 1; i++ )
        {
            vals.put( keyVal[i], keyVal[i + 1] );
        }
        StrSubstitutor sub = new StrSubstitutor( vals );
        return sub.replace( sql );
    }
}