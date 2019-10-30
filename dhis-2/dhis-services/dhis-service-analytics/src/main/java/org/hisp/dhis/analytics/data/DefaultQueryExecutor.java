/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.logging.LogFactory.getLog;

import org.apache.commons.logging.Log;
import org.hisp.dhis.analytics.CacheableQuery;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.cache.CacheKeyBuilder;
import org.hisp.dhis.analytics.cache.Key;
import org.hisp.dhis.analytics.cache.KeyBuilder;
import org.hisp.dhis.analytics.cache.QueryCache;
import org.hisp.dhis.analytics.cache.TimeToLive;
import org.hisp.dhis.analytics.cache.UserWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * This component os responsible for fetching and caching data from the
 * respective database based in the JDBC template provided.
 */
@Component
public class DefaultQueryExecutor
    implements
    CacheableQuery
{
    private static final Log log = getLog( DefaultQueryExecutor.class );

    private final JdbcTemplate jdbcTemplate;

    private final KeyBuilder keyBuilder;

    private final QueryCache analyticsQueryCache;

    private final UserWrapper userWrapper;

    public DefaultQueryExecutor( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate, final QueryCache analyticsQueryCache, final CacheKeyBuilder cacheKeyBuilder,
        final UserWrapper userWrapper)
    {
        checkNotNull( jdbcTemplate );
        checkNotNull( analyticsQueryCache );
        checkNotNull( cacheKeyBuilder );
        checkNotNull(userWrapper);
        this.jdbcTemplate = jdbcTemplate;
        this.analyticsQueryCache = analyticsQueryCache;
        this.keyBuilder = cacheKeyBuilder;
        this.userWrapper = userWrapper;
    }

    public SqlRowSet fetch( final String sqlQuery, final DataQueryParams params )
    {
        final String userAuthorityGroups = userWrapper.getUserAuthorityGroupsName();
        final Key key = keyBuilder.build( sqlQuery, userAuthorityGroups );
        final SqlRowSet cachedSqlRowSet = analyticsQueryCache.get( key );

        if ( cachedSqlRowSet != null )
        {
            // Guarantees the cached row set cursor is set at the beginning.
            cachedSqlRowSet.beforeFirst();

            return cachedSqlRowSet;
        }
        else
        {
            final long ttl = new TimeToLive( params ).compute();
            log.debug( "# Cache TTL in minutes: " + ttl );

            final SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet( sqlQuery );
            analyticsQueryCache.put( key, sqlRowSet, ttl );

            return sqlRowSet;
        }
    }

    public SqlRowSet forceFetch( final String sqlQuery )
    {
        return jdbcTemplate.queryForRowSet( sqlQuery );
    }
}
