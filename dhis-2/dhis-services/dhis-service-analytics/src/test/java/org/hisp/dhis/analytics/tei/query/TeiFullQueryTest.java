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
package org.hisp.dhis.analytics.tei.query;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.common.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamType;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.Test;

/**
 * TeiFullQuery unit tests.
 *
 * @author Dusan Bernat
 */
class TeiFullQueryTest extends DhisConvenienceTest
{
    @Test
    void testSqlQueryRenderingWithOrgUnitDimObject()
    {
        // given
        DimensionalObject dimensionalObject = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT, new ArrayList<>() );

        TeiQueryParams teiQueryParams = TeiQueryParams.builder()
            .trackedEntityType( createTrackedEntityType( 'A' ) )
            .commonParams( stubSortingCommonParams( null, StringUtils.EMPTY, dimensionalObject ) )
            .build();

        TeiFullQuery query = TeiFullQuery.builder()
            .teiQueryParams( teiQueryParams )
            .queryContext( QueryContext.of( teiQueryParams ) )
            .build();

        // when
        String sql = query.render();

        // then
        assertTrue( sql.contains( "ou from" ) );

        assertTrue( sql.contains( "order by ou ASC" ) );
    }

    @Test
    void testSqlQueryRenderingWithCommonDimensionalObject()
    {
        // when
        DimensionalObject dimensionalObject = new BaseDimensionalObject( "abc" );

        TeiQueryParams teiQueryParams = TeiQueryParams.builder()
            .trackedEntityType( createTrackedEntityType( 'A' ) )
            .commonParams( stubSortingCommonParams( createProgram( 'A' ), "0", dimensionalObject ) )
            .build();

        TeiFullQuery query = TeiFullQuery.builder()
            .teiQueryParams( teiQueryParams )
            .queryContext( QueryContext.of( teiQueryParams ) )
            .build();

        ProgramStage ps = query.getQueryContext().getTeiQueryParams().getCommonParams().getOrderParams().get( 0 )
            .getOrderBy().getProgramStage().getElement();

        // when
        String sql = query.render();

        // then
        assertTrue( sql.contains(
            "\"" + ps.getProgram().getUid() + "[0]." + ps.getUid() + "[0]." + "abc\".VALUE as VALUE from" ) );

        assertTrue(
            sql.contains( "order by \"" + ps.getProgram().getUid() + "[0]." + ps.getUid() + "[0]." + "abc\" ASC" ) );
    }

    private CommonParams stubSortingCommonParams( Program program, String offset, DimensionalObject dimensionalObject )
    {
        DimensionIdentifier.ElementWithOffset<Program> prg = program == null
            ? DimensionIdentifier.ElementWithOffset.emptyElementWithOffset()
            : DimensionIdentifier.ElementWithOffset.of( program, offset );

        DimensionIdentifier.ElementWithOffset<ProgramStage> programStage = program == null
            ? DimensionIdentifier.ElementWithOffset.emptyElementWithOffset()
            : DimensionIdentifier.ElementWithOffset.of( createProgramStage( 'S', program ), offset );

        DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier = DimensionIdentifier.of( prg,
            programStage,
            DimensionParam.ofObject( dimensionalObject, DimensionParamType.SORTING, List.of( StringUtils.EMPTY ) ) );

        AnalyticsSortingParams analyticsSortingParams = AnalyticsSortingParams.builder()
            .sortDirection( SortDirection.ASC )
            .orderBy( dimensionIdentifier )
            .build();

        AnalyticsPagingParams analyticsPagingParams = AnalyticsPagingParams.builder()
            .pageSize( 10 )
            .page( 1 )
            .build();

        return CommonParams.builder()
            .orderParams( List.of( analyticsSortingParams ) )
            .pagingAndSortingParams( analyticsPagingParams )
            .build();
    }
}
