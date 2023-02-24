/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei.query.context.sql;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SqlQueryCreatorService
{
    private final List<SqlQueryBuilder> providers;

    /**
     * Builds a SqlQueryCreator from the given TeiQueryParams.
     *
     * @param teiQueryParams the TeiQueryParams to build the SqlQueryCreator
     *        from.
     * @return a SqlQueryCreator
     */
    public SqlQueryCreator getSqlQueryCreator( TeiQueryParams teiQueryParams )
    {
        SqlParameterManager sqlParameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( teiQueryParams, sqlParameterManager );

        RenderableSqlQuery renderableSqlQuery = RenderableSqlQuery.builder()
            .countRequested( false )
            .build();

        for ( SqlQueryBuilder provider : providers )
        {
            List<DimensionIdentifier<DimensionParam>> acceptedDimensions = teiQueryParams.getCommonParams()
                .getDimensionIdentifiers().stream()
                .filter( provider.getDimensionFilters().stream().reduce( x -> true, Predicate::and ) )
                .collect( toList() );

            List<AnalyticsSortingParams> acceptedSortingParams = teiQueryParams.getCommonParams().getOrderParams()
                .stream()
                .filter( provider.getSortingFilters().stream().reduce( x -> true, Predicate::and ) )
                .collect( toList() );

            if ( provider.alwaysRun() ||
                !CollectionUtils.isEmpty( acceptedDimensions ) ||
                !CollectionUtils.isEmpty( acceptedSortingParams ) )
            {
                renderableSqlQuery = mergeQueries( renderableSqlQuery,
                    provider.buildSqlQuery( queryContext, acceptedDimensions, acceptedSortingParams ) );
            }
        }

        return SqlQueryCreator.of( queryContext, renderableSqlQuery );
    }

    private RenderableSqlQuery mergeQueries( RenderableSqlQuery initial, RenderableSqlQuery contribution )
    {
        RenderableSqlQuery.RenderableSqlQueryBuilder sqlQueryContextBuilder = initial.toBuilder();
        contribution.getSelectFields().forEach( sqlQueryContextBuilder::selectField );
        contribution.getLeftJoins().forEach( sqlQueryContextBuilder::leftJoin );
        contribution.getGroupableConditions().forEach( sqlQueryContextBuilder::groupableCondition );
        contribution.getOrderClauses().forEach( sqlQueryContextBuilder::orderClause );

        if ( contribution.getMainTable() != null )
        {
            sqlQueryContextBuilder.mainTable( contribution.getMainTable() );
        }

        if ( contribution.getLimitOffset() != null )
        {
            sqlQueryContextBuilder.limitOffset( contribution.getLimitOffset() );
        }

        return sqlQueryContextBuilder.build();
    }
}
