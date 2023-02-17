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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.common.query.BinaryConditionRenderer.fieldsEqual;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ENR_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;
import static org.hisp.dhis.analytics.tei.query.context.querybuilder.ContextUtils.enrollmentSelect;
import static org.hisp.dhis.analytics.tei.query.context.querybuilder.ContextUtils.eventSelect;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import lombok.NoArgsConstructor;

import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.LeftJoin;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.hisp.dhis.trackedentity.TrackedEntityType;

@NoArgsConstructor( access = PRIVATE )
public class EventSortingQueryBuilders
{
    /**
     * This method will add to the builder the "order" and "left joins" needed
     * for the given param.
     *
     * @param param the order param
     * @param queryContext the context
     * @param builder the builder
     */
    public static void handleEventOrder(
        AnalyticsSortingParams param,
        QueryContext queryContext,
        RenderableSqlQuery.RenderableSqlQueryBuilder builder,
        BiFunction<String, DimensionIdentifier<DimensionParam>, Renderable> renderableSupplier )
    {
        int sequence = queryContext.getSequence().getAndIncrement();

        DimensionIdentifier<DimensionParam> di = param.getOrderBy();

        DimensionParam sortingDimension = di.getDimension();
        String uniqueAlias = doubleQuote( sortingDimension.getUid() + "_" + sequence );
        String enrollmentAlias = ENR_ALIAS + "_" + sequence;

        toLeftJoin( param, enrollmentAlias, uniqueAlias, queryContext )
            .forEach( builder::leftJoin );

        builder.orderClause( IndexedOrder.of( param.getIndex(),
            Order.of(
                renderableSupplier.apply( uniqueAlias, di ),
                param.getSortDirection() ) ) );
    }

    /**
     * Builds the needed left joins for the given param.
     *
     * @param param the order param
     * @param enrollmentAlias the alias for the enrollment query
     * @param uniqueAlias the alias for the event query
     * @param queryContext the context
     * @return the left joins
     */
    private static Stream<LeftJoin> toLeftJoin(
        AnalyticsSortingParams param,
        String enrollmentAlias,
        String uniqueAlias,
        QueryContext queryContext )
    {
        TrackedEntityType trackedEntityType = queryContext.getTeiQueryParams().getTrackedEntityType();
        SqlParameterManager sqlParameterManager = queryContext.getSqlParameterManager();
        return Stream.of(
            LeftJoin.of(
                () -> "(" + enrollmentSelect(
                    param.getOrderBy().getProgram(),
                    trackedEntityType,
                    sqlParameterManager ) + ") " + enrollmentAlias,
                fieldsEqual( TEI_ALIAS, TEI_UID, enrollmentAlias, TEI_UID ) ),
            LeftJoin.of(
                () -> "(" + eventSelect(
                    param.getOrderBy().getProgram(),
                    param.getOrderBy().getProgramStage(),
                    trackedEntityType,
                    sqlParameterManager ) + ") "
                    + uniqueAlias,
                fieldsEqual( enrollmentAlias, PI_UID, uniqueAlias, PI_UID ) ) );
    }
}
