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
package org.hisp.dhis.resourcetable.jdbc;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.analytics.AnalyticsTableHook;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTablePhase;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableStore;
import org.hisp.dhis.system.util.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.resourcetable.ResourceTableStore" )
public class JdbcResourceTableStore implements ResourceTableStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final AnalyticsTableHookService analyticsTableHookService;

    private final JdbcTemplate jdbcTemplate;

    private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

    // -------------------------------------------------------------------------
    // ResourceTableStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void generateResourceTable( ResourceTable<?> resourceTable )
    {
        log.info( "Generating resource table: '{}'", resourceTable.getTableName() );

        final Clock clock = new Clock().startClock();
        final Table stagingTable = resourceTable.getTable();
        final String tableName = Table.fromStaging( stagingTable.getName() );

        jdbcTemplate.execute( sqlBuilder.dropTableIfExists( stagingTable ) );

        jdbcTemplate.execute( sqlBuilder.createTable( stagingTable ) );

        populateStagingTable( resourceTable );

        invokeHooks( resourceTable );

        for ( Index index : stagingTable.getIndexes() )
        {
            jdbcTemplate.execute( sqlBuilder.createIndex( stagingTable, index ) );
        }

        jdbcTemplate.execute( sqlBuilder.analyzeTable( stagingTable ) );

        log.debug( "Analyzed resource table: '{}'", resourceTable.getTempTableName() );

        // ---------------------------------------------------------------------
        // Swap tables
        // ---------------------------------------------------------------------

        jdbcTemplate.execute( resourceTable.getDropTableIfExistsStatement() );

        jdbcTemplate.execute( resourceTable.getRenameTempTableStatement() );

        log.debug( "Swapped resource table: '{}'", resourceTable.getTableName() );

        log.info( "Resource table '{}' update done: '{}'", resourceTable.getTableName(), clock.time() );
    }

    private void populateStagingTable( ResourceTable<?> resourceTable )
    {
        Optional<String> populateTableSql = resourceTable.getPopulateTempTableStatement();
        Optional<List<Object[]>> populateTableContent = resourceTable.getPopulateTempTableContent();

        if ( populateTableSql.isPresent() )
        {
            log.debug( "Populate table SQL: '{}'", populateTableSql.get() );

            jdbcTemplate.execute( populateTableSql.get() );
        }
        else if ( populateTableContent.isPresent() )
        {
            List<Object[]> content = populateTableContent.get();

            log.debug( "Populate table content rows: {}", content.size() );

            if ( content.size() > 0 )
            {
                int columns = content.get( 0 ).length;

                batchUpdate( columns, resourceTable.getTempTableName(), content );
            }
        }
    }

    private void invokeHooks( ResourceTable<?> resourceTable )
    {
        List<AnalyticsTableHook> hooks = analyticsTableHookService.getByPhaseAndResourceTableType(
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED, resourceTable.getTableType() );

        if ( !hooks.isEmpty() )
        {
            analyticsTableHookService.executeAnalyticsTableSqlHooks( hooks );

            log.info( "Invoked resource table hooks: '{}'", hooks.size() );
        }
    }

    @Override
    public void batchUpdate( int columns, String tableName, List<Object[]> batchArgs )
    {
        if ( columns == 0 || tableName == null )
        {
            return;
        }

        StringBuilder builder = new StringBuilder( "insert into " + tableName + " values (" );

        for ( int i = 0; i < columns; i++ )
        {
            builder.append( "?," );
        }

        builder.deleteCharAt( builder.length() - 1 ).append( ")" );

        jdbcTemplate.batchUpdate( builder.toString(), batchArgs );
    }
}
