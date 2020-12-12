package org.hisp.dhis.container;

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

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Custom {@link PostgreSQLContainer} that provides additional fluent API to
 * customize PostgreSQL configuration.
 *
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
public class DhisPostgreSQLContainer<SELF extends DhisPostgreSQLContainer<SELF>> extends PostgreSQLContainer<SELF>
{

    private static final String NAME = "postgis";

//    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse( "postgis/postgis" ).asCompatibleSubstituteFor( "postgres" );

    private static final String DEFAULT_TAG = "12-3.0";

    public static final String USER_PARAM = "user";

    public static final String PASSWORD_PARAM = "password";

    private Set<String> customPostgresConfigs = new HashSet<>();

    //    public DhisPostgreSQLContainer( final String dockerImageName )
//    {
//        super( dockerImageName );
//    }
//    private static final String FSYNC_OFF_OPTION = "fsync=off";
//
//    public DhisPostgreSQLContainer( final DockerImageName dockerImageName )
//    {
//        super(dockerImageName);
//        dockerImageName.assertCompatibleWith( DEFAULT_IMAGE_NAME );
//
//        this.waitStrategy = new LogMessageWaitStrategy()
//            .withRegEx( ".*database system is ready to accept connections.*\\s" )
//            .withTimes( 2 )
//            .withStartupTimeout( Duration.of( 60, SECONDS ) );
//        this.setCommand( "postgres", "-c", FSYNC_OFF_OPTION );
//
//        addExposedPort( POSTGRESQL_PORT );
//    }

    public DhisPostgreSQLContainer( DockerImageName dockerImageName )
    {
        super( dockerImageName );
    }
//public DhisPostgreSQLContainer( final String dockerImageName )
//{
//    super( dockerImageName );
//}

    @Override
    protected void configure()
    {
        addExposedPort( POSTGRESQL_PORT );
        addEnv( "POSTGRES_DB", getDatabaseName() );
        addEnv( "POSTGRES_USER", getUsername() );
        addEnv( "POSTGRES_PASSWORD", getPassword() );
        setCommand( getPostgresCommandWithCustomConfigs() );
    }

    private String getPostgresCommandWithCustomConfigs()
    {
        StringBuilder builder = new StringBuilder();
        builder.append( "postgres" );

        if ( !this.customPostgresConfigs.isEmpty() )
        {
            this.customPostgresConfigs.forEach( config -> {
                builder.append( " -c " );
                builder.append( config );
            } );
        }
        return builder.toString();
    }

    /**
     * Append custom postgres configuration to be customized when starting the
     * container. The configAndValue should be of the form
     * "configName=configValue". This method can be invoked multiple times to
     * add multiple custom commands.
     *
     * @param configAndValue The configuration and value of the form
     *                       "configName=configValue"
     * @return the DhisPostgreSQLContainer
     */
    public SELF appendCustomPostgresConfig( String configAndValue )
    {
        if ( !StringUtils.isBlank( configAndValue ) )
        {
            this.customPostgresConfigs.add( configAndValue );
        }
        return self();
    }

}
