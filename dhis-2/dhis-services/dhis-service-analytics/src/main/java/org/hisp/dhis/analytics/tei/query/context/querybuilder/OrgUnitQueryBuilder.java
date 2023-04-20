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

import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getPrefix;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType.ORGANISATION_UNIT;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.isOfType;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;

import org.hisp.dhis.analytics.common.params.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamObjectType;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.tei.query.OrganisationUnitCondition;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilder;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders;
import org.springframework.stereotype.Service;

/**
 * A {@link SqlQueryBuilder} that builds a {@link RenderableSqlQuery} for
 * {@link DimensionParam} of type
 * {@link DimensionParamObjectType#ORGANISATION_UNIT}.
 */
@Service
@org.springframework.core.annotation.Order( 2 )
public class OrgUnitQueryBuilder implements SqlQueryBuilder
{
    @Getter
    private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters = List
        .of( OrgUnitQueryBuilder::isOu );

    @Getter
    private final List<Predicate<AnalyticsSortingParams>> sortingFilters = List.of( OrgUnitQueryBuilder::isOuOrder );

    @Override
    public RenderableSqlQuery buildSqlQuery( QueryContext queryContext,
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
        List<AnalyticsSortingParams> acceptedSortingParams )
    {
        RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

        Stream.concat( acceptedDimensions.stream(), acceptedSortingParams.stream()
            .map( AnalyticsSortingParams::getOrderBy ) )
            .filter( dimensionIdentifier -> dimensionIdentifier.isEventDimension()
                || dimensionIdentifier.isEnrollmentDimension() )
            .map( dimensionIdentifier -> Field.ofUnquoted(
                getPrefix( dimensionIdentifier ),
                () -> dimensionIdentifier.getDimension().getUid(),
                dimensionIdentifier.toString() ) )
            .forEach( builder::selectField );

        acceptedDimensions
            .stream()
            .filter( SqlQueryBuilders::hasRestrictions )
            .map( dimId -> GroupableCondition.of(
                dimId.getGroupId(),
                OrganisationUnitCondition.of( dimId, queryContext ) ) )
            .forEach( builder::groupableCondition );

        acceptedSortingParams
            .forEach( sortingParam -> builder.orderClause(
                IndexedOrder.of(
                    sortingParam.getIndex(),
                    Order.of(
                        Field.ofDimensionIdentifier( sortingParam.getOrderBy() ),
                        sortingParam.getSortDirection() ) ) ) );

        return builder.build();
    }

    private static boolean isOuOrder( AnalyticsSortingParams analyticsSortingParams )
    {
        return isOu( analyticsSortingParams.getOrderBy() );
    }

    /**
     * Checks if the given {@link DimensionIdentifier} is of type
     * {@link DimensionParamObjectType#ORGANISATION_UNIT}.
     *
     * @param dimensionIdentifier the {@link DimensionIdentifier} to check.
     * @return true if the given {@link DimensionIdentifier} is of type
     *         {@link DimensionParamObjectType#ORGANISATION_UNIT}. False
     *         otherwise.
     */
    public static boolean isOu( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return isOfType( dimensionIdentifier, ORGANISATION_UNIT );
    }

    public static boolean isNotOuDimension( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return !isOu( dimensionIdentifier );
    }
}
