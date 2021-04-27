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

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
@Slf4j
public class DebeziumService
{
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Autowired
    private DbChangeEventHandler dbChangeEventHandler;

    @PostConstruct
    protected void init()
    {
        Properties props = Configuration.create().build().asProperties();

        props.setProperty( "name", "engine2" );
        props.setProperty( "plugin.name", "pgoutput" );
        props.setProperty( "slot.name", "slot1" );
        props.setProperty( "connector.class", "io.debezium.connector.postgresql.PostgresConnector" );
        props.setProperty( "offset.storage", "org.apache.kafka.connect.storage.MemoryOffsetBackingStore" );
        props.setProperty( "offset.flush.interval.ms", "60000" );
        /* begin connector properties */
        props.setProperty( "database.hostname", "localhost" );
        props.setProperty( "database.tcpKeepAlive", "true" );
        props.setProperty( "database.port", "5432" );
        props.setProperty( "database.user", "postgres" );
        props.setProperty( "database.password", "uk67TYYA" );
        props.setProperty( "database.server.id", "85744" );
        props.setProperty( "database.server.name", "dhis2master_26041" );
        props.setProperty( "database.dbname", "dhis2master_26041" );
        props.setProperty( "snapshot.mode", "never" );

        // props.setProperty( "database.history",
        // "io.debezium.relational.history.FileDatabaseHistory" );
        // props.setProperty( "database.history.file.filename",
        // "/path/to/storage/dbhistory.dat" );

        // props.setProperty( "plugin.name", "pgoutput" );
        // props.setProperty( "slot.name", "myslut1" );
        // props.setProperty( "connector.class",
        // "io.debezium.connector.postgresql.PostgresConnector" );
        // props.setProperty( "offset.storage",
        // "org.apache.kafka.connect.storage.FileOffsetBackingStore" );
        // props.setProperty( "offset.storage.file.filename", "/tmp/offsets.dat"
        // );
        // props.setProperty( "offset.flush.interval.ms", "60000" );
        // /* begin connector properties */
        // props.setProperty( "database.hostname", "localhost" );
        // props.setProperty( "database.port", "5432" );
        // props.setProperty( "database.user", "postgres" );
        // props.setProperty( "database.password", "uk67TYYA" );
        // props.setProperty( "database.server.id", "85744" );
        // props.setProperty( "database.server.name", "dhis2master_26041" );
        // props.setProperty( "database.dbname", "dhis2master_26041" );
        // props.setProperty( "snapshot.mode", "never" );
        // props.setProperty( "database.history",
        // "io.debezium.relational.history.FileDatabaseHistory" );
        // props.setProperty( "database.history.file.filename",
        // "/path/to/storage/dbhistory.dat" );

        // io.debezium.config.Configuration conf =
        // io.debezium.config.Configuration.create()
        // .with( "connector.class",
        // "io.debezium.connector.postgresql.PostgresConnector" )
        // .with( "offset.storage",
        // "org.apache.kafka.connect.storage.MemoryOffsetBackingStore" )
        // .with( "offset.flush.interval.ms", 60000 )
        // .with( "name", "orders-postgres-connector" )
        // .with( "database.server.name", "dhis2master_2604" )
        // .with( "database.hostname", "localhost" )
        // .with( "database.port", 5432 )
        // .with( "database.user", "postgres" )
        // .with( "database.password", "uk67TYYA" )
        // .with( "database.dbname", "dhis2master_2604" )
        // .with( "table.whitelist", "public.users" )
        // .with( "snapshot.mode", "never" )
        // .build();

        try ( DebeziumEngine<RecordChangeEvent<SourceRecord>> engine = DebeziumEngine.create( ChangeEventFormat.of(
            Connect.class ) )
            .using( props )
            .notifying( dbChangeEventHandler::handleDbChange )
            .build() )
        {
            executor.execute( engine );
        }
        catch ( IOException e )
        {
            log.error( "Debezium failure!", e );
        }
    }

    private void handleDbChange( ChangeEvent<String, String> event )
    {

        log.info( "Db event:" + event );

    }

    // private void handleDbChangeEvent( SourceRecord record )
    // {
    // if ( record.topic().equals( "dbserver1.public.item" ) )
    // {
    // Long itemId = ((Struct) record.key()).getInt64( "id" );
    // Struct payload = (Struct) record.value();
    // Operation op = Operation.forCode( payload.getString( "op" ) );
    //
    // if ( op == Operation.UPDATE || op == Operation.DELETE )
    // {
    // emf.getCache().evict( Item.class, itemId );
    // }
    // }
    // }
    //
    // @Inject
    // private KnownTransactions knownTransactions;
    //
    // private void handleDbChangeEvent( SourceRecord record )
    // {
    // if ( record.topic().equals( "dbserver1.public.item" ) )
    // {
    // Long itemId = ((Struct) record.key()).getInt64( "id" );
    // Struct payload = (Struct) record.value();
    // Operation op = Operation.forCode( payload.getString( "op" ) );
    // Long txId = ((Struct) payload.get( "source" )).getInt64( "txId" );
    //
    // if ( !knownTransactions.isKnown( txId ) &&
    // (op == Operation.UPDATE || op == Operation.DELETE) )
    // {
    // emf.getCache().evict( Item.class, itemId );
    // }
    // }
    // }
}
