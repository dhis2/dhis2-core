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
package org.hisp.dhis.analytics.shared;

import static org.springframework.util.Assert.notNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.ColumnDataType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * @see QueryExecutor
 *
 * @author maikel arabori
 */
@Component
public class SqlQueryExecutor implements QueryExecutor<SqlQuery, SqlQueryResult>
{

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public SqlQueryExecutor( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate )
    {
        notNull( jdbcTemplate, "jdbcTemplate cannot be null" );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    /**
     * @see QueryExecutor#execute(Query)
     *
     * @throws IllegalArgumentException if the query argument is null
     */
    @Override
    public SqlQueryResult execute( final SqlQuery query )
    {
        notNull( query, "The 'query' must not be null" );

        final SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet( query.statement(),
            new MapSqlParameterSource().addValues( query.params() ) );

        List<Column> columns = Arrays.stream( rowSet.getMetaData().getColumnNames() )
            .map( s -> Column.builder()
                .value( s )
                .alias( s )
                .type( ColumnDataType.TEXT )
                .build() )
            .collect( Collectors.toList() );

        final Map<Column, List<Object>> resultMap = initializeResultMapWith( columns );

        while ( rowSet.next() )
        {
            for ( final Column column : columns )
            {
                final Object value = rowSet.getObject( column.getValue() );
                resultMap.get( column ).add( value );
            }
        }

        return new SqlQueryResult( resultMap );
    }

    /**
     * Simply creates a map initializing it with keys (based on the given
     * columns) and empty lists.
     *
     * It returns a TreeMap version to ensure the ordering.
     *
     * @param columns
     * @return the initialized map
     */
    private Map<Column, List<Object>> initializeResultMapWith( final List<Column> columns )
    {
        final Map<Column, List<Object>> resultMap = new TreeMap<>();

        for ( final Column column : columns )
        {
            resultMap.put( column, new ArrayList<>() );
        }

        return resultMap;
    }
}
