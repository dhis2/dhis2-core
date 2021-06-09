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
package org.hisp.dhis.cacheinvalidation;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.source.SourceRecord;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Profile( { "!test", "!test-h2" } )
@Service
@Slf4j
public class DebeziumService
{
    @Autowired
    private DbChangeEventHandler dbChangeEventHandler;

    @Autowired
    private DhisConfigurationProvider dhisConfig;

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private final Executor executor = Executors.newSingleThreadExecutor();

    @PostConstruct
    protected void debeziumEngine()
    {
        String username = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_CONNECTION_USERNAME );
        String password = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_CONNECTION_PASSWORD );
        String dbHostname = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_DB_HOSTNAME );
        String dbPort = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_DB_PORT );
        String dbName = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_DB_NAME );

        String slotName = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_SLOT_NAME ).toLowerCase();

        Properties props = Configuration.create().build().asProperties();
        props.setProperty( "name", slotName );
        props.setProperty( "slot.name", slotName );
        props.setProperty( "plugin.name", "pgoutput" );
        props.setProperty( "connector.class", "io.debezium.connector.postgresql.PostgresConnector" );
        props.setProperty( "offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore" );
        props.setProperty( "offset.flush.interval.ms", "60000" );
        props.setProperty( "database.tcpKeepAlive", "true" );
        props.setProperty( "database.hostname", dbHostname );
        props.setProperty( "database.port", dbPort );
        props.setProperty( "database.user", username );
        props.setProperty( "database.password", password );
        props.setProperty( "database.server.name", dbName );
        props.setProperty( "database.dbname", dbName );
        props.setProperty( "snapshot.mode", "never" );

        engine = DebeziumEngine
            .create( ChangeEventFormat.of( Connect.class ) )
            .using( props )
            .notifying( dbChangeEventHandler::handleDbChange )
            .build();

        executor.execute( engine );

        log.info( "Debezium engine started!" );
    }

    @PreDestroy
    public void stopDebezium()
        throws IOException
    {
        engine.close();
    }

    // TODO: Impl. keep alive/watchdog to make sure connection is alive...
}
