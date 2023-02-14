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
package org.hisp.dhis.analytics.common;

import static org.springframework.util.Assert.notNull;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @see org.hisp.dhis.analytics.common.QueryExecutor
 * @author maikel arabori
 */
@Component
public class SqlQueryExecutor implements QueryExecutor<SqlQuery, SqlQueryResult>
{
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public SqlQueryExecutor( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate )
    {
        notNull( jdbcTemplate, "jdbcTemplate cannot be null" );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    /**
     * @throws IllegalArgumentException if the query argument is null
     */
    @Override
    public SqlQueryResult find( @Nonnull SqlQuery query )
    {
        notNull( query, "The 'query' must not be null" );

        SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet( query.getStatement(),
            new MapSqlParameterSource().addValues( query.getParams() ) );

        return new SqlQueryResult( rowSet );
    }

    /**
     * @throws IllegalArgumentException if the query argument is null
     */
    @Override
    public long count( @Nonnull SqlQuery query )
    {
        notNull( query, "The 'query' must not be null" );

        return Optional.ofNullable(
                namedParameterJdbcTemplate.queryForObject(
                        query.getStatement(),
                        new MapSqlParameterSource()
                                .addValues( query.getParams() ),
                        Long.class ) )
                .orElse(0L);
    }
}
