/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.event.data.AnalyticsDimensionsTestSupport.allValueTypeDataElements;
import static org.hisp.dhis.analytics.event.data.AnalyticsDimensionsTestSupport.allValueTypeTEAs;
import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.AGGREGATE_ALLOWED_VALUE_TYPES;
import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.QUERY_DISALLOWED_VALUE_TYPES;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import org.hisp.dhis.analytics.event.EnrollmentAnalyticsDimensionsService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Before;
import org.junit.Test;

public class EnrollmentAnalyticsDimensionsServiceTest
{
    private EnrollmentAnalyticsDimensionsService enrollmentAnalyticsDimensionsService;

    @Before
    public void setup()
    {
        ProgramService programService = mock( ProgramService.class );

        Program program = mock( Program.class );

        when( programService.getProgram( any() ) ).thenReturn( program );
        when( program.getDataElements() ).thenReturn( allValueTypeDataElements() );
        when( program.getProgramIndicators() ).thenReturn( Collections.emptySet() );
        when( program.getTrackedEntityAttributes() ).thenReturn( allValueTypeTEAs() );

        enrollmentAnalyticsDimensionsService = new DefaultEnrollmentAnalyticsDimensionsService( programService );

    }

    @Test
    public void testQueryDoesntContainDisallowedValueTypes()
    {
        Collection<BaseIdentifiableObject> analyticsDimensions = enrollmentAnalyticsDimensionsService
            .getQueryDimensionsByProgramStageId( "anUid" );

        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof DataElement )
                .map( de -> ((DataElement) de).getValueType() )
                .noneMatch(
                    QUERY_DISALLOWED_VALUE_TYPES::contains ) );
        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof TrackedEntityAttribute )
                .map( tea -> ((TrackedEntityAttribute) tea).getValueType() )
                .noneMatch(
                    QUERY_DISALLOWED_VALUE_TYPES::contains ) );
    }

    @Test
    public void testAggregateOnlyContainsAllowedValueTypes()
    {
        Collection<BaseIdentifiableObject> analyticsDimensions = enrollmentAnalyticsDimensionsService
            .getAggregateDimensionsByProgramStageId( "anUid" );

        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof DataElement )
                .map( de -> ((DataElement) de).getValueType() )
                .allMatch(
                    AGGREGATE_ALLOWED_VALUE_TYPES::contains ) );
        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof TrackedEntityAttribute )
                .map( tea -> ((TrackedEntityAttribute) tea).getValueType() )
                .allMatch(
                    AGGREGATE_ALLOWED_VALUE_TYPES::contains ) );
    }

}
