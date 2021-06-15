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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    public static final String DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED = "DebeziumEngine connectorStopped() event triggered! ";

    public static final String SHUTDOWN_ENABLED_MSG = "shutdownOnConnectorStop is set to FALSE, ignoring this event... NOTE: This will result in cache invalidation not working properly!";

    public static final String SHUTDOWN_DISABLED_MSG = "shutdownOnConnectorStop is set to TRUE, the server will shutdown in 10 seconds...";

    public static final String SEE_LOG_MSG = "Check the log for errors causing the shutdown!";

    private static final Executor executor = Executors.newSingleThreadExecutor();

    private static final CountDownLatch loginLatch = new CountDownLatch( 1 );

    // We need to delay shutdown a little bit to be able to log the exception
    // that caused it. The engine is running in it's own thread.
    public static final long SHUTDOWN_DELAY_SECONDS = 2L;

    public static final long STARTUP_WAIT_TIMEOUT = 5L;

    private boolean shutdownOnConnectorStop = true;

    @Autowired
    private DbChangeEventHandler dbChangeEventHandler;

    @Autowired
    private DhisConfigurationProvider dhisConfig;

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private void shutdown( String msg )
    {
        shutdown( msg, null, null );
    }

    private void shutdown( String msg, Long countDown, Throwable throwable )
    {
        if ( countDown != null )
        {
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );
            scheduler.schedule( () -> shutdown( msg ), countDown, TimeUnit.SECONDS );
            return;
        }

        log.error( "Shutting down due to a critical error. Reason: " + msg, throwable );
        System.exit( 1 );
    }

    private final DebeziumEngine.ConnectorCallback connectorCallback = new DebeziumEngine.ConnectorCallback()
    {
        @Override
        public void connectorStarted()
        {
            loginLatch.countDown();
            log.debug( "DebeziumEngine connectorStarted() event triggered! " );
        }

        @Override
        public void connectorStopped()
        {
            if ( shutdownOnConnectorStop )
            {
                log.error( DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED + SHUTDOWN_DISABLED_MSG );
                shutdown( DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED + SEE_LOG_MSG, SHUTDOWN_DELAY_SECONDS,
                    null );
            }
            else
            {
                log.warn( DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED + SHUTDOWN_ENABLED_MSG );
            }
        }

        @Override
        public void taskStarted()
        {
            log.info( "task start" );
        }

        @Override
        public void taskStopped()
        {
            log.info( "task stop" );
        }
    };

    private void handleCompletionCallback( boolean success, String message, Throwable error )
    {
        if ( success )
        {
            log.warn( "Success: " + message );
        }
        else
        {
            // log.warn( "No success: " + message );
        }

        if ( error != null )
        {
            // if ( error instanceof DebeziumException )
            // {
            // if ( shutdownOnConnectorStop )
            // {
            // shutdown( "DebeziumException:" + error.getMessage() );
            // }
            // }

            log.error( "A Debezium engine error has occurred! Error message: " + message, error );
        }
    }

    @PostConstruct
    protected void debeziumEngine()
        throws InterruptedException
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
            .using( this::handleCompletionCallback )
            .using( connectorCallback )
            .notifying( dbChangeEventHandler::handleDbChange )
            .build();

        executor.execute( engine );

        if ( !loginLatch.await( STARTUP_WAIT_TIMEOUT, TimeUnit.SECONDS ) )
        {
            if ( shutdownOnConnectorStop )
            {
                shutdown( "Debezium engine startup timeout exceeded! " + SEE_LOG_MSG );
            }
            else
            {
                log.warn( "Debezium engine startup timeout exceeded! " + SHUTDOWN_DISABLED_MSG );
            }
        }

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
