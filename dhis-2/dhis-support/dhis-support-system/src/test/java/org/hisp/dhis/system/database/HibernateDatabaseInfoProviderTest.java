package org.hisp.dhis.system.database;

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

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.env.MockEnvironment;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Unit tests for {@link HibernateDatabaseInfoProvider}.
 *
 * @author Volker Schmidt
 */
public class HibernateDatabaseInfoProviderTest
{
    @Mock
    private DhisConfigurationProvider config;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MockEnvironment environment = new MockEnvironment();

    @Mock
    private ResultSet resultSet;

    private HibernateDatabaseInfoProvider provider;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        environment.setActiveProfiles( "prod" );
        provider = new HibernateDatabaseInfoProvider( config, jdbcTemplate, environment );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void init() throws SQLException
    {
        Mockito.when( jdbcTemplate.queryForObject( Mockito.eq( "select postgis_full_version();" ), Mockito.eq( String.class ) ) ).thenReturn( "2" );

        Mockito.when( config.getProperty( Mockito.eq( ConfigurationKey.CONNECTION_URL ) ) ).thenReturn( "jdbc:postgresql:dhisx" );
        Mockito.when( config.getProperty( Mockito.eq( ConfigurationKey.CONNECTION_USERNAME ) ) ).thenReturn( "dhisy" );
        Mockito.when( config.getProperty( Mockito.eq( ConfigurationKey.CONNECTION_PASSWORD ) ) ).thenReturn( "dhisz" );

        Mockito.when( resultSet.getString( Mockito.eq( 1 ) ) ).thenReturn( "PostgreSQL 10.5, compiled by Visual C++ build 1800, 64-bit" );
        Mockito.when( resultSet.getString( Mockito.eq( 2 ) ) ).thenReturn( "dhis2" );
        Mockito.when( resultSet.getString( Mockito.eq( 3 ) ) ).thenReturn( "dhis" );

        Mockito.when( jdbcTemplate.queryForObject( Mockito.eq( "select version(),current_catalog,current_user" ), Mockito.isA( RowMapper.class ) ) )
            .thenAnswer( invocation -> ( (RowMapper<?>) invocation.getArgument( 1 ) ).mapRow( resultSet, 1 ) );

        provider.init();

        final DatabaseInfo databaseInfo = provider.getDatabaseInfo();
        Assert.assertEquals( "jdbc:postgresql:dhisx", databaseInfo.getUrl() );
        Assert.assertEquals( "dhis2", databaseInfo.getName() );
        Assert.assertEquals( "dhis", databaseInfo.getUser() );
        Assert.assertEquals( "dhisz", databaseInfo.getPassword() );
        Assert.assertEquals( "PostgreSQL 10.5", databaseInfo.getDatabaseVersion() );
        Assert.assertTrue( databaseInfo.isSpatialSupport() );
    }
}
