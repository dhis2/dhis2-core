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
package org.hisp.dhis.cacheinvalidation.debezium;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.source.SourceRecord;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.base.MoreObjects;

import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;

/**
 * Service responsible for starting the Debezium engine {@link DebeziumEngine}
 * used for cache invalidation in DHIS2.
 * <p>
 * The Debezium engine in this service will call the event handler
 * {@link DbChangeEventHandler} when a new replication event occurs.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @see <a href="https://debezium.io">https://debezium.io</a>
 */
@Slf4j
@Profile( { "!test", "!test-h2" } )
@Conditional( value = DebeziumCacheInvalidationEnabledCondition.class )
@Service
public class DebeziumService
{
    public static final long STARTUP_WAIT_TIMEOUT_SECONDS = 5L;

    public static final long SHUTDOWN_DELAY_SECONDS = 10L;

    public static final String SHUTDOWN_DISABLED_MSG = "shutdownOnConnectorStop is set to TRUE, the server will shutdown in "
        + SHUTDOWN_DELAY_SECONDS + " seconds...";

    public static final String DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED = "DebeziumEngine connectorStopped() event triggered! ";

    public static final String SHUTDOWN_ENABLED_MSG = "shutdownOnConnectorStop is set to FALSE, ignoring this event... "
        + "NOTE: This will result in cache invalidation not working properly!";

    public static final String SEE_LOG_MSG = "Check the log for errors causing the shutdown!";

    private static final Executor EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    private static final CountDownLatch COUNT_DOWN_LATCH = new CountDownLatch( 1 );

    @Autowired
    private DbChangeEventHandler dbChangeEventHandler;

    @Autowired
    private DhisConfigurationProvider dhisConfig;

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private boolean shutdownOnConnectorStopOrError = false;

    /**
     * Important that this is called before server shutdown so Debezium can
     * remove its replication slot from the database.
     */
    @PreDestroy
    public void stopDebezium()
        throws IOException
    {
        log.info( "Stopping the Debezium engine..." );
        engine.close();
    }

    public void startDebeziumEngine()
        throws InterruptedException
    {
        Properties configuration = getConfiguration();

        engine = DebeziumEngine
            .create( ChangeEventFormat.of( Connect.class ) )
            .using( configuration )
            .using( this::handleCompletionCallback )
            .using( connectorCallback )
            .notifying( dbChangeEventHandler::handleDbChange )
            .build();

        startupEngineOnExecutor();
    }

    private Properties getConfiguration()
    {
        String username = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_CONNECTION_USERNAME );
        String password = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_CONNECTION_PASSWORD );
        String dbHostname = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_DB_HOSTNAME );
        String dbPort = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_DB_PORT );
        String dbName = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_DB_NAME );
        String excludeList = dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_EXCLUDE_LIST );
        String configSlotName = MoreObjects
            .firstNonNull( dhisConfig.getProperty( ConfigurationKey.DEBEZIUM_SLOT_NAME ).trim(), "" );

        this.shutdownOnConnectorStopOrError = dhisConfig
            .isEnabled( ConfigurationKey.DEBEZIUM_SHUTDOWN_ON_CONNECTOR_STOP );
        // Build replication slot name as:
        // "dhis2_[EPOCH_SECONDS]_[OPTIONAL_CONFIG_ID]_[UUID4]
        long timeNow = System.currentTimeMillis() / 1000L;
        String slotName = "dhis2_" + timeNow + "_" + configSlotName + "_"
            + UUID.randomUUID().toString().replace( "-", "" );

        Properties props = Configuration.create().build().asProperties();
        props.setProperty( "name", slotName );
        props.setProperty( "slot.name", slotName );
        props.setProperty( "plugin.name", "pgoutput" );
        props.setProperty( "publication.autocreate.mode", "filtered" );
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
        props.setProperty( "slot.drop.on.stop", "true" );
        props.setProperty( "heartbeat.interval.ms", "10000" );
        props.setProperty( "event.processing.failure.handling.mode", "warn" );
        props.setProperty( "table.exclude.list",
            "public.spatial_ref_sys,"
                + "postgis.spatial_ref_sys,"
                + "public.audit,"
                + "public.userroleauthorities,"
                + "public.oauth_access_token,"
                + "public.oauth_refresh_token,"
                + "public.userteisearchorgunits,"
                + "public.usermembership,"
                + "public.userdatavieworgunits,"
                + "public.userteisearchorgunits,"
                + "public.userrolemembers,"
                + "public.usergroupmembers,"
                + "public.previouspasswords,"
                + "public.program_organisationunits,"
                + "public.orgunitgroupmembers,"
                + "public.orgunitgroupsetmembers,"
                + "public.dataelementgroupmembers,"
                + "public.programinstancecomments,"
                + "public.eventcomments,"
                + "public.messageconversation_usermessages,"
                + "public.messageconversation_messages,"
                + "public.trackedentityinstanceaudit,"
                + excludeList );

        return props;
    }

    /**
     * Starts the engine and waits with a count down latch that will timeout if
     * startup time exceeds {@link DebeziumService#STARTUP_WAIT_TIMEOUT_SECONDS}
     *
     * @throws InterruptedException thrown if the CountDownLatch await is
     *         interrupted.
     */
    private void startupEngineOnExecutor()
        throws InterruptedException
    {
        EXECUTOR_SERVICE.execute( engine );

        if ( !COUNT_DOWN_LATCH.await( STARTUP_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS ) )
        {
            log.error( "Debezium engine startup timeout exceeded! " + SHUTDOWN_DISABLED_MSG );

            if ( shutdownOnConnectorStopOrError )
            {
                shutdown( "Debezium engine startup failed! " + SEE_LOG_MSG, SHUTDOWN_DELAY_SECONDS, null );
            }
        }
        else
        {
            log.info( "Debezium engine started successfully!" );
        }
    }

    /**
     * Callback connected to the engine that will react on connector stopped
     * events and shutdown the DHIS2 server if
     * {@link DebeziumService#shutdownOnConnectorStopOrError} is true, the
     * default is false. This can be configured with the
     * {@code debezium.shutdown_on.connector_stop = on/off}
     */
    private final DebeziumEngine.ConnectorCallback connectorCallback = new DebeziumEngine.ConnectorCallback()
    {
        @Override
        public void connectorStarted()
        {
            log.debug( "DebeziumEngine connectorStarted() event triggered! " );
            COUNT_DOWN_LATCH.countDown();
        }

        @Override
        public void connectorStopped()
        {
            if ( shutdownOnConnectorStopOrError )
            {
                log.error( DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED + SHUTDOWN_DISABLED_MSG );

                shutdown( (DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED + SEE_LOG_MSG), SHUTDOWN_DELAY_SECONDS,
                    null );
            }
            else
            {
                log.error( DEBEZIUM_ENGINE_CONNECTOR_STOPPED_EVENT_TRIGGERED + SHUTDOWN_ENABLED_MSG );
            }
        }
    };

    private void handleCompletionCallback( boolean success, String message, Throwable error )
    {
        if ( error != null )
        {
            if ( shutdownOnConnectorStopOrError )
            {
                shutdown( "DebeziumException:" + error.getMessage(), SHUTDOWN_DELAY_SECONDS, error );
            }

            log.error( "A Debezium engine error has occurred! Error message= " + message, error );
        }
    }

    private void shutdown( String msg, Long countDownSeconds, Throwable throwable )
    {
        if ( countDownSeconds != null )
        {
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool( 1 );
            scheduler.schedule( () -> shutdown( msg, null, throwable ), countDownSeconds, TimeUnit.SECONDS );
            return;
        }

        log.error( "Shutting down due to a critical error. Reason: " + msg, throwable );
        System.exit( 1 );
    }
}
