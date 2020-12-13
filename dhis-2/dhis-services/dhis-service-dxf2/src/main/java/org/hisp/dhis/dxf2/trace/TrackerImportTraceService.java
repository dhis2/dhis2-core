package org.hisp.dhis.dxf2.trace;

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

import static org.hisp.dhis.external.conf.ConfigurationKey.TRACKER_IMPORT_TRACING_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.TRACKER_IMPORT_TRACING_LOG_SIZE;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This service writes to a database table request and response information of a
 * Tracker Import operation. The write operation is async.
 * 
 * Upon initialization, the service checks if the target log table exists. If
 * the table is missing, the service shuts itself down.
 * 
 * The service also shuts itself down if an error occurs during persistence.
 * 
 * The service periodically checks the size of the log table and truncates older
 * entries. The default table size is 1000 entries. This value can be configured
 * using the 'tracker.import.tracing.log.size' configuration key.
 * 
 * The service must be enabled using the 'tracker.import.tracing.enabled'
 * configuration key.
 * 
 *
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TrackerImportTraceService
{
    private final JdbcTemplate jdbcTemplate;

    private final RenderService renderService;

    private final DhisConfigurationProvider config;

    private boolean enabled = false;

    private int logSize = 1000;

    private final static String TRACE_TABLE = "tracker_trace";

    @PostConstruct
    public void init()
    {
        this.enabled = config.isEnabled( TRACKER_IMPORT_TRACING_ENABLED ) && traceTableExists();
        try
        {
            this.logSize = Integer.parseInt( config.getProperty( TRACKER_IMPORT_TRACING_LOG_SIZE ) );
        }
        catch ( NumberFormatException e )
        {
        }
    }

    public boolean traceTableExists()
    {
        try
        {
            Integer cnt = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE  table_name = '" + TRACE_TABLE + "'",
                Integer.class );

            return cnt != null && cnt == 1;

        }
        catch ( Exception e )
        {
            log.warn( ":::: An error occurred trying to determine if the table " + TRACE_TABLE
                + " exists. Tracker Tracing is disabled.", e );
            enabled = false;
        }
        return false;
    }

    /**
     * Dump the request/response data to the database
     * 
     * @param elapsed request/response elapsed time (ms.)
     * @param request the JSON representation of the request payload
     * @param importSummaries the result of the Tracker Import
     * @param headers a Map containing the request headers
     * @param path the request path
     * @param user the current user
     */
    public void trace( long elapsed, String request, ImportSummaries importSummaries, Map<String, String> headers,
        String path, User user )
    {
        if ( !enabled )
        {
            return;
        }

        final String sql = "INSERT INTO " + TRACE_TABLE
            + " (ELAPSED, USERNAME, PAYLOAD, RESPONSE, IMPORT_STATUS, PATH, HEADERS) VALUES(?,?,?,?,?,?,?)";

        CompletableFuture.runAsync( () -> {
            try
            {
                jdbcTemplate.update( sql, elapsed, user.getUsername(), request,
                    renderService.toJsonAsString( importSummaries ), importSummaries.getStatus().name(),
                    StringUtils.left( path, 2000 ), renderService.toJsonAsString( headers ) );
            }
            catch ( Exception e )
            {
                log.error( ":::: Tracker tracing failed. Service will be disabled.", e );
                enabled = false;
            }
        } );
    }

    @Scheduled( fixedRate = 60000 )
    public void truncate()
    {
        try
        {
            jdbcTemplate.execute( "DELETE FROM " + TRACE_TABLE + " WHERE id < (SELECT id FROM " + TRACE_TABLE
                + " ORDER BY id DESC LIMIT 1 OFFSET " + logSize + " )" );
        }
        catch ( Exception e )
        {
            log.error( ":::: Tracker tracing failed during table clean-up. Service will be disabled.", e );
            enabled = false;
        }
    }

    public void toggle()
    {
        this.enabled = !enabled;
    }
}
