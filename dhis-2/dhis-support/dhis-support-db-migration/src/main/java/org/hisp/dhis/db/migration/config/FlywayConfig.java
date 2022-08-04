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
package org.hisp.dhis.db.migration.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.FLYWAY_OUT_OF_ORDER_MIGRATION;
import static org.hisp.dhis.external.conf.ConfigurationKey.FLYWAY_REPAIR_BEFORE_MIGRATION;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.hisp.dhis.db.migration.helper.NoOpFlyway;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class FlywayConfig
{

    private final static String FLYWAY_MIGRATION_FOLDER = "org/hisp/dhis/db/migration";

    @Bean( value = "flyway", initMethod = "migrate" )
    @Profile( "!test-h2" )
    @DependsOn( "dataSource" )
    public Flyway flyway( DhisConfigurationProvider configurationProvider, DataSource dataSource )
    {
        ClassicConfiguration classicConfiguration = new ClassicConfiguration();

        classicConfiguration.setDataSource( dataSource );
        classicConfiguration.setBaselineOnMigrate( true );
        classicConfiguration.setOutOfOrder( configurationProvider.isEnabled( FLYWAY_OUT_OF_ORDER_MIGRATION ) );
        classicConfiguration.setIgnoreMigrationPatterns( "*:missing" );
        classicConfiguration.setGroup( true );
        classicConfiguration.setLocations( new Location( FLYWAY_MIGRATION_FOLDER ) );
        classicConfiguration.setMixed( true );

        return new DhisFlyway( classicConfiguration,
            configurationProvider.isEnabled( FLYWAY_REPAIR_BEFORE_MIGRATION ) );

    }

    @Bean( "flyway" )
    @Profile( "test-h2" )
    public NoOpFlyway noFlyway()
    {
        return new NoOpFlyway();
    }
}
