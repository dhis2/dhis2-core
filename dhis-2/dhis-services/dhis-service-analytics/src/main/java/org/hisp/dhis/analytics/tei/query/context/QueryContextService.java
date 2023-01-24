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

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_INDICATOR;

@Service
@RequiredArgsConstructor
public class QueryContextService
{
    private final ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder;

    public QueryContext of( TeiQueryParams teiQueryParams )
    {
        QueryContext queryContext = QueryContext.of( teiQueryParams);

        ProgramIndicatorContext programIndicatorContext =
                ProgramIndicatorContext.ProgramIndicatorContextBuilder.of(
                        getProgramIndicators( teiQueryParams ),
                        teiQueryParams.getTrackedEntityType(),
                        queryContext.getParameterManager())
                        .build();

        return queryContext.withProgramIndicatorContext( programIndicatorContext );
    }

    private List<DimensionIdentifier<Program, ProgramStage, ProgramIndicatorContext.DimensionParamProgramIndicatorQuery>> getProgramIndicators(TeiQueryParams teiQueryParams) {
        return teiQueryParams.getCommonParams().getDimensionIdentifiers().stream()
                .flatMap(Collection::stream)
                .filter( dim -> dim.getDimension().getDimensionParamObjectType() == PROGRAM_INDICATOR )
                .map(this::asDimensionParamProgramIndicatorQuery)
                .collect(Collectors.toList());
    }

    private DimensionIdentifier<Program, ProgramStage, ProgramIndicatorContext.DimensionParamProgramIndicatorQuery> asDimensionParamProgramIndicatorQuery(DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier) {
        return DimensionIdentifier.of(
                dimensionIdentifier.getProgram(),
                dimensionIdentifier.getProgramStage(),
                asDimensionParamProgramIndicatorQuery(dimensionIdentifier.getDimension()));
    }

    private ProgramIndicatorContext.DimensionParamProgramIndicatorQuery asDimensionParamProgramIndicatorQuery(DimensionParam dimension) {
        ProgramIndicator programIndicator = (ProgramIndicator) dimension.getQueryItem().getItem();
        return ProgramIndicatorContext.DimensionParamProgramIndicatorQuery.of(
                dimension,
                alias -> programIndicatorSubqueryBuilder.getAggregateClauseForProgramIndicator(
                        programIndicator,
                        programIndicator.getAnalyticsType(),
                        null,
                        null,
                        alias
                        ));
    }

}
