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

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.NUMERIC;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_INDICATOR;
import static org.hisp.dhis.analytics.common.query.BinaryConditionRenderer.fieldsEqual;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.isOfType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.AndCondition;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupableCondition;
import org.hisp.dhis.analytics.common.query.IndexedOrder;
import org.hisp.dhis.analytics.common.query.LeftJoin;
import org.hisp.dhis.analytics.common.query.Order;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilder;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * a {@link SqlQueryBuilder} for {@link ProgramIndicator} dimensions. It will
 * generate a subquery for each program indicator dimension.
 */
@Service
@RequiredArgsConstructor
public class ProgramIndicatorQueryBuilder implements SqlQueryBuilder
{
    public static final String SUBQUERY_TABLE_ALIAS = "subax";

    private final ProgramIndicatorService programIndicatorService;

    @Getter
    private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters = List
        .of( d -> isOfType( d, PROGRAM_INDICATOR ) );

    @Getter
    private final List<Predicate<AnalyticsSortingParams>> sortingFilters = List
        .of( d -> isOfType( d.getOrderBy(), PROGRAM_INDICATOR ) );

    @Override
    public RenderableSqlQuery buildSqlQuery( QueryContext sqlQueryContext,
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
        List<AnalyticsSortingParams> acceptedSortingParams )
    {
        AtomicInteger counter = sqlQueryContext.getSequence();

        RenderableSqlQuery.RenderableSqlQueryBuilder builder = RenderableSqlQuery.builder();

        for ( ProgramIndicatorDimensionParam param : getProgramIndicatorDimensionParams( acceptedDimensions,
            acceptedSortingParams ) )
        {
            String assignedAlias = doubleQuote(
                param.getDimensionIdentifier().toString() + "_" + counter.incrementAndGet() );

            builder.selectField( Field.ofUnquoted(
                StringUtils.EMPTY,
                () -> "coalesce(" + assignedAlias + ".value, double precision 'NaN')",
                assignedAlias, param.getDimensionIdentifier().toString() ) );

            if ( param.getProgramIndicator().getAnalyticsType() == AnalyticsType.ENROLLMENT )
            {
                builder.leftJoin(
                    LeftJoin.of(
                        () -> "(" + enrollmentProgramIndicatorSelect(
                            param.getDimensionIdentifier().getProgram(),
                            param.getProgramIndicatorExpressionSql(),
                            param.getProgramIndicatorFilterSql(), true ) + ") as " + assignedAlias,
                        fieldsEqual( TEI_ALIAS, TEI_UID, assignedAlias, TEI_UID ) ) );
            }
            else
            {
                String enrollmentAlias = "ENR_" + counter.getAndIncrement();
                builder.leftJoin(
                    LeftJoin.of(
                        () -> "(" + enrollmentProgramIndicatorSelect(
                            param.getDimensionIdentifier().getProgram(),
                            param.getProgramIndicatorExpressionSql(),
                            param.getProgramIndicatorFilterSql(), false ) + ") as " + enrollmentAlias,
                        fieldsEqual( TEI_ALIAS, TEI_UID, enrollmentAlias, TEI_UID ) ) )
                    .leftJoin(
                        LeftJoin.of(
                            () -> "(" + eventProgramIndicatorSelect(
                                param.getDimensionIdentifier().getProgram(),
                                param.getProgramIndicatorExpressionSql(),
                                param.getProgramIndicatorFilterSql() ) + ") as " + assignedAlias,
                            fieldsEqual( enrollmentAlias, PI_UID, assignedAlias, PI_UID ) ) );
            }

            if ( param.hasRestrictions() )
            {
                builder.groupableCondition(
                    GroupableCondition.of(
                        param.getDimensionIdentifier().getGroupId(),
                        AndCondition.of(
                            param.getDimensionIdentifier().getDimension().getItems().stream()
                                .map( item -> BinaryConditionRenderer.of(
                                    () -> assignedAlias + ".value",
                                    item.getOperator(),
                                    item.getValues(),
                                    NUMERIC,
                                    sqlQueryContext ) )
                                .collect( toList() ) ) ) );
            }

            if ( param.isOrder() )
            {
                builder.orderClause(
                    IndexedOrder.of(
                        param.getOrderIndex(),
                        Order.of( () -> assignedAlias, param.getSortDirection() ) ) );
            }
        }
        return builder.build();
    }

    private List<ProgramIndicatorDimensionParam> getProgramIndicatorDimensionParams(
        List<DimensionIdentifier<DimensionParam>> acceptedDimensions,
        List<AnalyticsSortingParams> acceptedSortingParams )
    {
        return Stream.concat(
            acceptedDimensions.stream()
                .map( this::asDimensionParamProgramIndicatorQuery ),
            acceptedSortingParams.stream()
                .map( this::asDimensionParamProgramIndicatorQuery ) )
            .collect( toList() );
    }

    private static String enrollmentProgramIndicatorSelect( ElementWithOffset<Program> program,
        String expression, String filter, boolean needsExpressions )
    {
        return "select innermost_enr.*" +
            " from (select tei as " + TEI_UID + ", pi as " + PI_UID + ", " +
            (needsExpressions ? expression + " as value, " : "") +
            " row_number() over (partition by tei order by enrollmentdate desc) as rn " +
            " from analytics_enrollment_" + program.getElement().getUid() + " as " + SUBQUERY_TABLE_ALIAS +
            (needsExpressions ? " where " + filter : "") + ") innermost_enr" +
            " where innermost_enr.rn = 1";
    }

    static String eventProgramIndicatorSelect( ElementWithOffset<Program> program,
        String expression, String filter )
    {
        return "select innermost_evt.*" +
            " from (select pi as " + PI_UID + ", " + expression + " as value, " +
            " row_number() over (partition by pi order by executiondate desc) as rn " +
            " from analytics_event_" + program.getElement().getUid() + " as " + SUBQUERY_TABLE_ALIAS +
            " where " + filter + ") innermost_evt" +
            " where innermost_evt.rn = 1";
    }

    private ProgramIndicatorDimensionParam asDimensionParamProgramIndicatorQuery(
        DimensionIdentifier<DimensionParam> dimensionIdentifier )
    {
        return asDimensionParamProgramIndicatorQuery( dimensionIdentifier, null, null );
    }

    private ProgramIndicatorDimensionParam asDimensionParamProgramIndicatorQuery(
        AnalyticsSortingParams analyticsSortingParams )
    {
        return asDimensionParamProgramIndicatorQuery( analyticsSortingParams.getOrderBy(),
            analyticsSortingParams.getSortDirection(), analyticsSortingParams.getIndex() );
    }

    private ProgramIndicatorDimensionParam asDimensionParamProgramIndicatorQuery(
        DimensionIdentifier<DimensionParam> dimensionIdentifier,
        SortDirection sortDirection,
        Long sortIndex )
    {
        ProgramIndicator programIndicator = (ProgramIndicator) dimensionIdentifier.getDimension().getQueryItem()
            .getItem();

        return ProgramIndicatorDimensionParam.of(
            dimensionIdentifier,
            programIndicator,
            // PI Expression
            programIndicatorService.getAnalyticsSql(
                programIndicator.getExpression(),
                DataType.NUMERIC,
                programIndicator,
                null,
                null,
                SUBQUERY_TABLE_ALIAS ),
            // PI Filter
            programIndicatorService.getAnalyticsSql(
                programIndicator.getFilter(),
                DataType.BOOLEAN,
                programIndicator,
                null,
                null,
                SUBQUERY_TABLE_ALIAS ),
            sortDirection,
            sortIndex );
    }

    @Data
    @RequiredArgsConstructor( staticName = "of" )
    static class ProgramIndicatorDimensionParam
    {
        private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

        private final ProgramIndicator programIndicator;

        private final String programIndicatorExpressionSql;

        private final String programIndicatorFilterSql;

        private final SortDirection sortDirection;

        private final Long orderIndex;

        boolean hasRestrictions()
        {
            return SqlQueryBuilders.hasRestrictions( dimensionIdentifier );
        }

        boolean isOrder()
        {
            return sortDirection != null;
        }
    }
}
