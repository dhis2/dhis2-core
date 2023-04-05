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
package org.hisp.dhis.analytics.tei.query;

import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlParameterManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

class StatusConditionTest
{
    @Test
    void testProgramStatusCompletedProduceCorrectSql()
    {
        // SETUP
        List<String> values = List.of( "COMPLETED" );

        DimensionIdentifier<DimensionParam> dimensionIdentifier = getProgramAttributeDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.ENROLLMENT_STATUS, values );

        // CALL
        SqlParameterManager sqlParameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, sqlParameterManager );

        StatusCondition statusCondition = StatusCondition.of( dimensionIdentifier, queryContext );

        String rendered = statusCondition.render();

        assertEquals( "\"programUid[0]\".enrollmentstatus in (:1)", rendered );
        assertEquals( "COMPLETED", queryContext.getParametersPlaceHolder().get( "1" ) );
    }

    @Test
    void testProgramStatusCompletedActiveProduceCorrectSql()
    {
        // SETUP
        List<String> values = List.of( "COMPLETED", "ACTIVE" );

        DimensionIdentifier<DimensionParam> dimensionIdentifier = getProgramAttributeDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.ENROLLMENT_STATUS, values );

        // CALL
        SqlParameterManager sqlParameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, sqlParameterManager );

        StatusCondition statusCondition = StatusCondition.of( dimensionIdentifier, queryContext );

        String rendered = statusCondition.render();

        assertEquals( "\"programUid[0]\".enrollmentstatus in (:1)", rendered );
        assertEquals( values, queryContext.getParametersPlaceHolder().get( "1" ) );
    }

    @Test
    void testEventStatusCompletedProduceCorrectSql()
    {
        // SETUP
        List<String> values = List.of( "COMPLETED" );

        DimensionIdentifier<DimensionParam> dimensionIdentifier = getProgramAttributeDimensionIdentifier(
            "programUid", "programStageUid",
            DimensionParam.StaticDimension.EVENT_STATUS, values );

        // CALL
        SqlParameterManager sqlParameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, sqlParameterManager );

        StatusCondition statusCondition = StatusCondition.of( dimensionIdentifier, queryContext );

        String rendered = statusCondition.render();

        assertEquals( "\"programUid[0].programStageUid[0]\".status in (:1)", rendered );
        assertEquals( "COMPLETED", queryContext.getParametersPlaceHolder().get( "1" ) );
    }

    @Test
    void testEventStatusCompletedScheduleProduceCorrectSql()
    {
        // SETUP
        List<String> values = List.of( "COMPLETED", "SCHEDULE" );

        DimensionIdentifier<DimensionParam> dimensionIdentifier = getProgramAttributeDimensionIdentifier(
            "programUid", "programStageUid", DimensionParam.StaticDimension.EVENT_STATUS, values );

        // CALL
        SqlParameterManager sqlParameterManager = new SqlParameterManager();
        QueryContext queryContext = QueryContext.of( null, sqlParameterManager );

        StatusCondition statusCondition = StatusCondition.of( dimensionIdentifier, queryContext );

        String rendered = statusCondition.render();

        assertEquals( "\"programUid[0].programStageUid[0]\".status in (:1)", rendered );
        assertEquals( values, queryContext.getParametersPlaceHolder().get( "1" ) );
    }

    private DimensionIdentifier<DimensionParam> getProgramAttributeDimensionIdentifier(
        String programUid, String programStageUid,
        DimensionParam.StaticDimension dimension,
        List<String> items )
    {
        DimensionParam dimensionParam = DimensionParam.ofObject(
            dimension.name(),
            DimensionParamType.DIMENSIONS,
            items );

        Program program = new Program();
        program.setUid( programUid );

        ElementWithOffset<ProgramStage> programStageElementWithOffset = emptyElementWithOffset();

        if ( StringUtils.isNotBlank( programStageUid ) )
        {
            ProgramStage programStage = new ProgramStage();
            programStage.setUid( programStageUid );
            programStageElementWithOffset = ElementWithOffset.of( programStage, 0 );
        }

        return DimensionIdentifier.of(
            ElementWithOffset.of( program, 0 ),
            programStageElementWithOffset,
            dimensionParam );
    }
}
