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
package org.hisp.dhis.analytics.tei;

import static org.hisp.dhis.analytics.util.AnalyticsUtils.ERR_MSG_TABLE_NOT_EXISTING;
import static org.hisp.dhis.feedback.ErrorCode.E7131;
import static org.springframework.util.Assert.notNull;

import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.common.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.GridAdaptor;
import org.hisp.dhis.analytics.common.QueryExecutor;
import org.hisp.dhis.analytics.common.SqlQuery;
import org.hisp.dhis.analytics.common.SqlQueryResult;
import org.hisp.dhis.analytics.tei.query.TeiSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.QueryContext;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryRuntimeException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Service;

/**
 * Service responsible exclusively for querying. Methods present on this class
 * must not change any state.
 *
 * @author maikel arabori
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeiAnalyticsQueryService
{
    private final QueryExecutor<SqlQuery, SqlQueryResult> queryExecutor;

    private final GridAdaptor gridAdaptor;

    /**
     * This method will create a query, based on the teiParams, and execute it
     * against the underline data provider and return. The results found will be
     * returned encapsulated on a Grid object.
     *
     * @param teiQueryParams
     * @return the populated Grid object
     * @throws IllegalArgumentException if the given teiParams is null
     */
    // TODO: Remove CommonQueryRequest from here. The service and components should only see CommonParams.
    public Grid getGrid( @Nonnull TeiQueryParams teiQueryParams, @Nonnull CommonQueryRequest commonQueryRequest )
    {
        notNull( teiQueryParams, "The 'teiQueryParams' must not be null" );
        notNull( commonQueryRequest, "The 'commonQueryRequest' must not be null" );

        QueryContext queryContext = QueryContext.of( teiQueryParams );
        Optional<SqlQueryResult> result = Optional.empty();
        long rowsCount = 0;

        try
        {
            result = Optional.of( queryExecutor.find( new TeiSqlQuery( queryContext ).get() ) );

            AnalyticsPagingParams pagingParams = teiQueryParams.getCommonParams().getPagingParams();

            if ( pagingParams.showTotalPages() )
            {
                rowsCount = queryExecutor.count( new TeiSqlQuery( queryContext ).count() );
            }
        }
        catch ( BadSqlGrammarException ex )
        {
            log.info( ERR_MSG_TABLE_NOT_EXISTING, ex );
        }
        catch ( DataAccessResourceFailureException ex )
        {
            log.warn( E7131.getMessage(), ex );
            throw new QueryRuntimeException( E7131 );
        }

        return gridAdaptor.createGrid( result, rowsCount, teiQueryParams, commonQueryRequest );
    }
}
