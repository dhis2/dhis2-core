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
package org.hisp.dhis.statistics.jdbc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.Objects;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Service( "org.hisp.dhis.statistics.StatisticsProvider" )
public class JdbcStatisticsProvider
    implements StatisticsProvider
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    /**
     * Read only JDBC template.
     */
    private final JdbcTemplate jdbcTemplate;

    public JdbcStatisticsProvider( JdbcTemplate jdbcTemplate )
    {
        checkNotNull( jdbcTemplate );
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // StatisticsProvider implementation
    // -------------------------------------------------------------------------

    @Override
    public Map<Objects, Long> getObjectCounts()
    {
        final Map<Objects, Long> objectCounts = new HashMap<>();

        // Metadata, use exact counts

        objectCounts.put( Objects.DATAELEMENT, query( "select count(*) from dataelement;" ) );
        objectCounts.put( Objects.DATAELEMENTGROUP, query( "select count(*) from dataelementgroup;" ) );
        objectCounts.put( Objects.INDICATORTYPE, query( "select count(*) from indicatortype;" ) );
        objectCounts.put( Objects.INDICATOR, query( "select count(*) from indicator;" ) );
        objectCounts.put( Objects.INDICATORGROUP, query( "select count(*) from indicatorgroup;" ) );
        objectCounts.put( Objects.DATASET, query( "select count(*) from dataset;" ) );
        objectCounts.put( Objects.ORGANISATIONUNIT, query( "select count(*) from organisationunit;" ) );
        objectCounts.put( Objects.ORGANISATIONUNITGROUP, query( "select count(*) from orgunitgroup;" ) );
        objectCounts.put( Objects.VALIDATIONRULE, query( "select count(*) from validationrule;" ) );
        objectCounts.put( Objects.PROGRAM, query( "select count(*) from program;" ) );
        objectCounts.put( Objects.PERIOD, query( "select count(*) from period;" ) );
        objectCounts.put( Objects.USER, query( "select count(*) from users;" ) );
        objectCounts.put( Objects.USERGROUP, query( "select count(*) from usergroup;" ) );
        objectCounts.put( Objects.REPORTTABLE,
            query( "select count(*) from visualization where type = 'PIVOT_TABLE';" ) );
        objectCounts.put( Objects.VISUALIZATION, query( "select count(*) from visualization;" ) );
        objectCounts.put( Objects.CHART, query( "select count(*) from visualization where type <> 'PIVOT_TABLE';" ) );
        objectCounts.put( Objects.MAP, query( "select count(*) from map;" ) );
        objectCounts.put( Objects.DASHBOARD, query( "select count(*) from dashboard;" ) );

        // Data, use approximate counts

        objectCounts.put( Objects.DATAVALUE, approximateCount( "datavalue" ) );
        objectCounts.put( Objects.TRACKEDENTITYINSTANCE, approximateCount( "trackedentityinstance" ) );
        objectCounts.put( Objects.PROGRAMINSTANCE, approximateCount( "programinstance" ) );
        objectCounts.put( Objects.PROGRAMSTAGEINSTANCE, approximateCount( "programstageinstance" ) );

        return objectCounts;
    }

    /**
     * Returns the response of the given SQL query as a long value.
     *
     * @param sql the SQL query.
     * @return the response of the given SQL query as a long value.
     */
    private Long query( final String sql )
    {
        return jdbcTemplate.queryForObject( sql, Long.class );
    }

    /**
     * Returns the approximate count of rows in the given table.
     *
     * @param table the table name.
     * @return the approximate count of rows in the given table.
     */
    private Long approximateCount( final String table )
    {
        final String sql = "select reltuples::bigint from pg_class where relname = '%s';";

        return jdbcTemplate.queryForObject( String.format( sql, table ), Long.class );
    }
}
