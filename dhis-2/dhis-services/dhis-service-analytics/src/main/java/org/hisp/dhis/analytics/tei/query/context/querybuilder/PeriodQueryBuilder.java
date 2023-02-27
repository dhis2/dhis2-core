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

import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifierConverterSupport.DIMENSION_SEPARATOR;
import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifierConverterSupport.getPrefix;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.Getter;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.tei.query.PeriodCondition;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilderAdaptor;
import org.hisp.dhis.period.Period;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for adding period conditions to the SQL query. By
 * design, "Period" conditions are grouped together in their own single group.
 * This means that the period conditions are not combined with other conditions
 * and are rendered as a single group of "OR" conditions.
 */
@Service
public class PeriodQueryBuilder extends SqlQueryBuilderAdaptor
{
    private static final String PERIOD_CONDITION_GROUP = "PERIOD_CONDITION";

    @Getter
    private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters = List
        .of( d -> d.getDimension().isPeriodDimension() );

    @Getter
    private final List<Predicate<AnalyticsSortingParams>> sortingFilters = List.of(
        sortingParams -> sortingParams.getOrderBy().getDimension().isPeriodDimension() );

    @Override
    public RenderableSqlQuery buildSqlQuery( QueryContext ctx,
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
        List<AnalyticsSortingParams> acceptedSortingParams )
    {
        RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

        Stream.concat( acceptedDimensions.stream(), acceptedSortingParams.stream()
            .map( AnalyticsSortingParams::getOrderBy ) )
            .map( dimensionIdentifier -> {
                String field = getTimeField( dimensionIdentifier );

                String prefix = getPrefix( dimensionIdentifier, false );

                return Field.ofUnquoted( doubleQuote( prefix ), () -> field, prefix + DIMENSION_SEPARATOR + field );
            } )
            .forEach( builder::selectField );

        acceptedDimensions.stream()
            .map( dimensionIdentifier -> PeriodCondition.of( dimensionIdentifier, ctx ) )
            .map( periodCondition -> GroupableCondition.of( PERIOD_CONDITION_GROUP, periodCondition ) )
            .forEach( builder::groupableCondition );

        acceptedSortingParams
            .forEach( sortingParam -> {
                DimensionIdentifier<DimensionParam> dimensionIdentifier = sortingParam.getOrderBy();
                String fieldName = getTimeField( dimensionIdentifier );

                Field field = Field.ofUnquoted(
                    getPrefix( sortingParam.getOrderBy() ),
                    () -> fieldName, StringUtils.EMPTY );
                builder.orderClause(
                    IndexedOrder.of(
                        sortingParam.getIndex(),
                        Order.of( field,
                            sortingParam.getSortDirection() ) ) );
            } );

        return builder.build();
    }

    private static String getTimeField( DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return Optional.of( dimensionIdentifier )
            .map( DimensionIdentifier::getDimension )
            .map( DimensionParam::getDimensionalObject )
            .map( d -> d.getItems().get( 0 ) )
            .map( Period.class::cast )
            .map( Period::getDateField )
            .map( TimeField::valueOf )
            .map( TimeField::getField )
            .orElseGet( () -> dimensionIdentifier.getDimension().getStaticDimension().getColumnName() );
    }
}
