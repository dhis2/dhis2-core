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
        final Map<Objects, Long> map = new HashMap<>();

        // Metadata, use exact counts

        map.put( Objects.DATAELEMENT, query( "select count(*) from dataelement;" ) );
        map.put( Objects.DATAELEMENTGROUP, query( "select count(*) from dataelementgroup;" ) );
        map.put( Objects.INDICATORTYPE, query( "select count(*) from indicatortype;" ) );
        map.put( Objects.INDICATOR, query( "select count(*) from indicator;" ) );
        map.put( Objects.INDICATORGROUP, query( "select count(*) from indicatorgroup;" ) );
        map.put( Objects.DATASET, query( "select count(*) from dataset;" ) );
        map.put( Objects.ORGANISATIONUNIT, query( "select count(*) from organisationunit;" ) );
        map.put( Objects.ORGANISATIONUNITGROUP, query( "select count(*) from orgunitgroup;" ) );
        map.put( Objects.VALIDATIONRULE, query( "select count(*) from validationrule;" ) );
        map.put( Objects.PROGRAM, query( "select count(*) from program;" ) );
        map.put( Objects.PERIOD, query( "select count(*) from period;" ) );
        map.put( Objects.USER, query( "select count(*) from userinfo;" ) );
        map.put( Objects.USERGROUP, query( "select count(*) from usergroup;" ) );
        map.put( Objects.VISUALIZATION, query( "select count(*) from visualization;" ) );
        map.put( Objects.EVENTVISUALIZATION, query( "select count(*) from eventvisualization;" ) );
        map.put( Objects.MAP, query( "select count(*) from map;" ) );
        map.put( Objects.DASHBOARD, query( "select count(*) from dashboard;" ) );

        // Data, use approximate counts

        map.put( Objects.DATAVALUE, approximateCount( "datavalue" ) );
        map.put( Objects.TRACKEDENTITYINSTANCE, approximateCount( "trackedentityinstance" ) );
        map.put( Objects.PROGRAMINSTANCE, approximateCount( "programinstance" ) );
        map.put( Objects.PROGRAMSTAGEINSTANCE, approximateCount( "programstageinstance" ) );

        return map;
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
