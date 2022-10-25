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
package org.hisp.dhis.tracker;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;
import org.springframework.stereotype.Service;

import io.debezium.config.Configuration;
import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.connector.postgresql.PostgresConnectorConfig;
import io.debezium.embedded.Connect;
import io.debezium.embedded.EmbeddedEngine;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;

@Slf4j
@Service
public class PushNotificationChangeEventListener extends AbstractStartupRoutine
{
    private final DhisConfigurationProvider dhisConfig;

    private final Executor executor = Executors.newSingleThreadExecutor();

    // private EmbeddedEngine engine;
    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    public PushNotificationChangeEventListener( DhisConfigurationProvider dhisConfig )
    {
        this.dhisConfig = dhisConfig;
    }

    @Override
    public void execute()
        throws Exception
    {

        Configuration config = Configuration.create()
            .with( EmbeddedEngine.ENGINE_NAME, "tracker-push" )
            .with( EmbeddedEngine.CONNECTOR_CLASS, PostgresConnector.class )
            .with( "plugin.name", "pgoutput" )
            .with( EmbeddedEngine.OFFSET_STORAGE, MemoryOffsetBackingStore.class )
            .with( "offset.flush.interval.ms", "100" )
            .with( "database.hostname", dhisConfig.getProperty( ConfigurationKey.CONNECTION_HOSTNAME ) )
            .with( "database.port", "5432" )
            .with( "database.user", dhisConfig.getProperty( ConfigurationKey.CONNECTION_USERNAME ) )
            .with( "database.password", dhisConfig.getProperty( ConfigurationKey.CONNECTION_PASSWORD ) )
            .with( "database.dbname", "dhis" )
            .with( "topic.prefix", "tracker" )
            .with( "table.include.list", "public.programinstance" ) // enrollments
            .with( "include.schema.changes", "false" )
            .with( PostgresConnectorConfig.SNAPSHOT_MODE, PostgresConnectorConfig.SnapshotMode.NEVER )
            .build();

        // this.engine = EmbeddedEngine.create()
        // .using( config )
        // .notifying( this::handleDbChangeEvent )
        // .build();
        this.engine = DebeziumEngine.create( ChangeEventFormat.of( Connect.class ) )
            .using( config.asProperties() )
            .notifying( this::handleChangeEvent )
            .build();

        this.executor.execute( engine );

        log.info( "Listening for tracker DB events" );
    }

    @PreDestroy
    public void shutdownEngine()
        throws IOException
    {
        log.info( "Stopping Debezium embedded engine" );
        engine.close();
    }

    private void handleChangeEvent( RecordChangeEvent<SourceRecord> record )
    {
        log.info( "Handling DB change event " + record );
    }
}
