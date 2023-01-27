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
package org.hisp.dhis.analytics.tei.query.context;

import static lombok.AccessLevel.PACKAGE;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.NUMERIC;
import static org.hisp.dhis.analytics.common.query.BinaryConditionRenderer.fieldsEqual;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;
import static org.hisp.dhis.analytics.tei.query.context.ContextUtils.enrollmentProgramIndicatorSelect;
import static org.hisp.dhis.analytics.tei.query.context.ContextUtils.eventProgramIndicatorSelect;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.AndCondition;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.GroupRenderable;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;

@Builder( access = PACKAGE, builderClassName = "PrivateBuilder", toBuilder = true )
public class ProgramIndicatorContext
{
    static final ProgramIndicatorContext EMPTY_PROGRAM_INDICATOR_CONTEXT = ProgramIndicatorContext.builder().build();

    @Getter
    @Singular
    private final List<Field> fields;

    @Getter
    @Singular
    private final List<Pair<Renderable, Renderable>> leftJoins;

    @Getter
    @Singular
    private final List<Renderable> orders;

    @Getter
    @Singular
    private final List<GroupRenderable> conditions;

    @RequiredArgsConstructor( staticName = "of" )
    public static class ProgramIndicatorContextBuilder
    {
        private final List<ProgramIndicatorDimensionParam> programIndicatorDimensionParams;

        private final QueryContext queryContext;

        private final AtomicInteger counter = new AtomicInteger( 0 );

        public ProgramIndicatorContext build()
        {
            ProgramIndicatorContext.PrivateBuilder builder = ProgramIndicatorContext.builder();

            for ( ProgramIndicatorDimensionParam param : programIndicatorDimensionParams )
            {
                String assignedAlias = doubleQuote(
                    param.getDimensionIdentifier().toString() + "_" + counter.getAndIncrement() );

                builder.field( Field.ofUnquoted(
                    StringUtils.EMPTY,
                    () -> "coalesce(" + assignedAlias + ".value, double precision 'NaN')",
                    assignedAlias ) );

                if ( param.getProgramIndicator().getAnalyticsType() == AnalyticsType.ENROLLMENT )
                {
                    builder.leftJoin(
                        Pair.of(
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
                        Pair.of(
                            () -> "(" + enrollmentProgramIndicatorSelect(
                                param.getDimensionIdentifier().getProgram(),
                                param.getProgramIndicatorExpressionSql(),
                                param.getProgramIndicatorFilterSql(), false ) + ") as " + enrollmentAlias,
                            fieldsEqual( TEI_ALIAS, TEI_UID, enrollmentAlias, TEI_UID ) ) )
                        .leftJoin(
                            Pair.of(
                                () -> "(" + eventProgramIndicatorSelect(
                                    param.getDimensionIdentifier().getProgram(),
                                    param.getProgramIndicatorExpressionSql(),
                                    param.getProgramIndicatorFilterSql() ) + ") as " + assignedAlias,
                                fieldsEqual( enrollmentAlias, PI_UID, assignedAlias, PI_UID ) ) );
                }

                if ( param.isFilter() )
                {
                    builder.condition(
                        GroupRenderable.of(
                            param.getDimensionIdentifier().getGroupId(),
                            AndCondition.of(
                                param.getDimensionIdentifier().getDimension().getItems().stream()
                                    .map( item -> BinaryConditionRenderer.of(
                                        () -> assignedAlias + ".value",
                                        item.getOperator(),
                                        item.getValues(),
                                        NUMERIC,
                                        queryContext ) )
                                    .collect( Collectors.toList() ) ) ) );
                }

                if ( param.isOrder() )
                {
                    builder.order( () -> assignedAlias );
                }
            }

            return builder.build();
        }
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

        boolean isFilter()
        {
            return dimensionIdentifier.getDimension().isFilter();
        }

        boolean isOrder()
        {
            return sortDirection != null;
        }
    }
}
