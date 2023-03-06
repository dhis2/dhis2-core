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

import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifierHelper.DIMENSION_SEPARATOR;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_INDICATOR;
import static org.hisp.dhis.analytics.common.query.BinaryConditionRenderer.fieldsEqual;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;
import static org.hisp.dhis.analytics.tei.query.context.querybuilder.ContextUtils.enrollmentSelect;
import static org.hisp.dhis.analytics.tei.query.context.querybuilder.ContextUtils.eventSelect;
import static org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilders.isOfType;
import static org.hisp.dhis.commons.util.TextUtils.doubleQuote;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.query.LeftJoin;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery.RenderableSqlQueryBuilder;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for building the SQL statement for the main TEI
 * table.
 */
@Service
public class LeftJoinsQueryBuilder implements SqlQueryBuilder
{
    @Nonnull
    @Override
    public List<Predicate<DimensionIdentifier<DimensionParam>>> getDimensionFilters()
    {
        return List.of(
            dimensionIdentifier -> dimensionIdentifier.isEventDimension() ||
                dimensionIdentifier.isEnrollmentDimension(),
            dimensionIdentifier -> !isOfType( dimensionIdentifier, PROGRAM_INDICATOR ) );
    }

    @Nonnull
    @Override
    public List<Predicate<AnalyticsSortingParams>> getSortingFilters()
    {
        return List.of(
            sortingParams -> sortingParams.getOrderBy().isEventDimension() ||
                sortingParams.getOrderBy().isEnrollmentDimension(),
            sortingParams -> !isOfType( sortingParams.getOrderBy(), PROGRAM_INDICATOR ) );
    }

    @Override
    public RenderableSqlQuery buildSqlQuery( QueryContext queryContext,
        List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers,
        List<AnalyticsSortingParams> analyticsSortingParams )
    {
        RenderableSqlQueryBuilder renderableSqlQuery = RenderableSqlQuery.builder();

        List<DimensionIdentifier<DimensionParam>> allDimensions = Stream.concat(
            dimensionIdentifiers.stream(),
            analyticsSortingParams.stream()
                .map( AnalyticsSortingParams::getOrderBy ) )
            .filter( dimensionIdentifier -> !isOfType( dimensionIdentifier, PROGRAM_ATTRIBUTE ) )
            .collect( Collectors.toList() );

        Set<ElementWithOffset<Program>> allDeclaredPrograms = allDimensions.stream()
            .map( DimensionIdentifier::getProgram )
            .collect( Collectors.toSet() );

        Set<Pair<ElementWithOffset<Program>, ElementWithOffset<ProgramStage>>> allDeclaredProgramStages = allDimensions
            .stream()
            .filter( DimensionIdentifier::isEventDimension )
            .map( dimensionIdentifier -> Pair.of( dimensionIdentifier.getProgram(),
                dimensionIdentifier.getProgramStage() ) )
            .collect( Collectors.toSet() );

        TrackedEntityType trackedEntityType = queryContext.getTeiQueryParams().getTrackedEntityType();
        SqlParameterManager sqlParameterManager = queryContext.getSqlParameterManager();

        for ( ElementWithOffset<Program> program : allDeclaredPrograms )
        {
            String enrollmentAlias = doubleQuote( program.toString() );
            renderableSqlQuery.leftJoin(
                LeftJoin.of(
                    () -> "(" + enrollmentSelect(
                        program,
                        trackedEntityType,
                        sqlParameterManager ) + ") as " + enrollmentAlias,
                    fieldsEqual( TEI_ALIAS, TEI_UID, enrollmentAlias, TEI_UID ) ) );
        }

        for ( Pair<ElementWithOffset<Program>, ElementWithOffset<ProgramStage>> programStage : allDeclaredProgramStages )
        {
            String enrollmentAlias = doubleQuote( programStage.getLeft().toString() );
            String eventAlias = doubleQuote(
                programStage.getLeft().toString() + DIMENSION_SEPARATOR + programStage.getRight().toString() );
            renderableSqlQuery.leftJoin(
                LeftJoin.of(
                    () -> "(" + eventSelect(
                        programStage.getLeft(),
                        programStage.getRight(),
                        trackedEntityType,
                        sqlParameterManager ) + ") as " + eventAlias,
                    fieldsEqual(
                        enrollmentAlias, PI_UID, eventAlias, PI_UID ) ) );
        }
        return renderableSqlQuery.build();
    }
}
