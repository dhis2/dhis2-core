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
package org.hisp.dhis.analytics.tei.query.context;

import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_INDICATOR;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueryContextService
{
    public final static String SUBQUERY_TABLE_ALIAS = "subax";

    private final ProgramIndicatorService programIndicatorService;

    public QueryContext of( TeiQueryParams teiQueryParams )
    {
        QueryContext queryContext = QueryContext.of( teiQueryParams );

        ProgramIndicatorContext programIndicatorContext = ProgramIndicatorContext.ProgramIndicatorContextBuilder.of(
            getProgramIndicatorDimensionParams( teiQueryParams ), queryContext )
            .build();

        return queryContext.withProgramIndicatorContext( programIndicatorContext );
    }

    private List<ProgramIndicatorContext.ProgramIndicatorDimensionParam> getProgramIndicatorDimensionParams(
        TeiQueryParams teiQueryParams )
    {
        return Stream.concat(
            teiQueryParams.getCommonParams().getDimensionIdentifiers().stream()
                .flatMap( Collection::stream )
                .filter( dim -> dim.getDimension().isOfType( PROGRAM_INDICATOR ) )
                .map( this::asDimensionParamProgramIndicatorQuery ),
            teiQueryParams.getCommonParams().getOrderParams().stream()
                .filter( analyticsSortingParams -> analyticsSortingParams.getOrderBy().getDimension()
                    .isOfType( PROGRAM_INDICATOR ) )
                .map( this::asDimensionParamProgramIndicatorQuery ) )
            .collect( Collectors.toList() );
    }

    private ProgramIndicatorContext.ProgramIndicatorDimensionParam asDimensionParamProgramIndicatorQuery(
        DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier )
    {
        return asDimensionParamProgramIndicatorQuery( dimensionIdentifier, null );
    }

    private ProgramIndicatorContext.ProgramIndicatorDimensionParam asDimensionParamProgramIndicatorQuery(
        AnalyticsSortingParams analyticsSortingParams )
    {
        return asDimensionParamProgramIndicatorQuery( analyticsSortingParams.getOrderBy(),
            analyticsSortingParams.getSortDirection() );
    }

    private ProgramIndicatorContext.ProgramIndicatorDimensionParam asDimensionParamProgramIndicatorQuery(
        DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier, SortDirection sortDirection )
    {
        ProgramIndicator programIndicator = (ProgramIndicator) dimensionIdentifier.getDimension().getQueryItem()
            .getItem();

        return ProgramIndicatorContext.ProgramIndicatorDimensionParam.of(
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
            sortDirection );
    }
}
