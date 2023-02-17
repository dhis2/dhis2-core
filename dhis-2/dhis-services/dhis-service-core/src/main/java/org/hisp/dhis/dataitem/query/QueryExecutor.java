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
package org.hisp.dhis.dataitem.query;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.dataitem.query.ResultProcessor.process;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;

import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataitem.DataItem;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for executing the respective data item query
 * for a given entity.
 *
 * @author maikel arabori
 */
@Slf4j
@Component
public class QueryExecutor
{
    private static final String SPACED_UNION = " UNION ";

    private final List<DataItemQuery> dataItemQueries;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public QueryExecutor(
        @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate, List<DataItemQuery> dataItemQueries )
    {
        checkNotNull( jdbcTemplate );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
        this.dataItemQueries = dataItemQueries;
    }

    /**
     * Responsible for building the respective query statement and executing it
     * in order to find the list of items based on the given parameter map.
     *
     * @param targetEntities
     * @param paramsMap
     * @return the data items found
     */
    public List<DataItem> find( final Set<Class<? extends BaseIdentifiableObject>> targetEntities,
        final MapSqlParameterSource paramsMap )
    {
        final String unionQuery = unionQuery( targetEntities, paramsMap );

        if ( !unionQuery.isEmpty() )
        {
            final SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet( unionQuery, paramsMap );

            return process( rowSet );
        }

        return emptyList();
    }

    /**
     * Responsible for building the respective count SQL statement and executing
     * it in order to find the total of data items for the given parameter map.
     *
     * @param targetEntities
     * @param paramsMap
     * @return the items found
     */
    public int count( final Set<Class<? extends BaseIdentifiableObject>> targetEntities,
        final MapSqlParameterSource paramsMap )
    {
        final String unionQuery = unionQuery( targetEntities, paramsMap );
        final StringBuilder countQuery = new StringBuilder();

        if ( !unionQuery.isEmpty() )
        {
            countQuery.append( SPACED_SELECT + "count(*) from (" )
                .append( unionQuery.replaceAll( maxLimit( paramsMap ), EMPTY ) )
                .append( ") t" );

            return namedParameterJdbcTemplate.queryForObject( countQuery.toString(), paramsMap, Integer.class );
        }

        return 0;
    }

    private String unionQuery( final Set<Class<? extends BaseIdentifiableObject>> targetEntities,
        final MapSqlParameterSource paramsMap )
    {
        final StringBuilder unionQuery = new StringBuilder();

        // Iterates through all implementations of DataItemQuery and get the
        // respective SQL query of each "entity".
        for ( final DataItemQuery dataItemQuery : dataItemQueries )
        {
            if ( targetEntities.contains( dataItemQuery.getRootEntity() )
                && dataItemQuery.matchQueryRules( paramsMap ) )
            {
                // Linking queries together through UNION.
                unionQuery.append( dataItemQuery.getStatement( paramsMap ) );
                unionQuery.append( SPACED_UNION );
            }
        }

        if ( unionQuery.length() > 0 )
        {
            final boolean hasMultipleEntities = targetEntities.size() > 1;

            if ( hasMultipleEntities )
            {
                // Applying general sorting and limit over the final results.
                unionQuery.append(
                    ifSet( ordering( "i18n_first_name, i18n_second_name, item_uid",
                        "item_name, item_uid", "i18n_first_shortname, i18n_second_shortname, item_uid",
                        "item_shortname, item_uid", paramsMap ) ) );
                unionQuery.append( ifSet( maxLimit( paramsMap ) ) );
            }

            // Removes last "UNION" keyword.
            final int fromIndex = unionQuery.lastIndexOf( SPACED_UNION );
            final int untilIndex = fromIndex + SPACED_UNION.length();

            unionQuery.delete( fromIndex, untilIndex ).toString();
        }

        final String fullStatement = unionQuery.toString();

        log.trace( "Full UNION SQL: " + fullStatement );

        return fullStatement;
    }
}
