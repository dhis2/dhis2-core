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

import static org.hisp.dhis.analytics.event.data.DefaultEventAnalyticsDimensionsService.AGGREGATE_ALLOWED_VALUE_TYPES;
import static org.hisp.dhis.analytics.event.data.DefaultEventAnalyticsDimensionsService.QUERY_DISALLOWED_VALUE_TYPES;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.analytics.event.AnalyticsDimensions;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Before;
import org.junit.Test;

public class DefaultEventAnalyticsDimensionsServiceTest
{

    private EventAnalyticsDimensionsService eventAnalyticsDimensionsService;

    @Before
    public void setup()
    {
        ProgramStageService programStageService = mock( ProgramStageService.class );
        CategoryService categoryService = mock( CategoryService.class );

        Program program = mock( Program.class );
        ProgramStage programStage = mock( ProgramStage.class );

        when( programStageService.getProgramStage( any() ) ).thenReturn( programStage );
        when( programStage.getProgram() ).thenReturn( program );
        when( program.getDataElements() ).thenReturn( allValueTypeDataElements() );
        when( program.getProgramIndicators() ).thenReturn( Collections.emptySet() );
        when( program.getTrackedEntityAttributes() ).thenReturn( allValueTypeTEAs() );

        eventAnalyticsDimensionsService = new DefaultEventAnalyticsDimensionsService( programStageService,
            categoryService );

    }

    @Test
    public void testAggregateOnlyContainsAllowedValueTypes()
    {
        AnalyticsDimensions analyticsDimensions = eventAnalyticsDimensionsService
            .getQueryDimensionsByProgramStageId( "anUid" );

        assertTrue(
            analyticsDimensions
                .getDataElements()
                .stream()
                .map( de -> ((DataElement) de).getValueType() )
                .noneMatch(
                    QUERY_DISALLOWED_VALUE_TYPES::contains ) );
        assertTrue(
            analyticsDimensions
                .getTrackedEntityAttributes()
                .stream()
                .map( tea -> ((TrackedEntityAttribute) tea).getValueType() )
                .noneMatch(
                    QUERY_DISALLOWED_VALUE_TYPES::contains ) );
    }

    @Test
    public void testQueryDoesntContainDisallowedValueTypes()
    {
        AnalyticsDimensions analyticsDimensions = eventAnalyticsDimensionsService
            .getAggregateDimensionsByProgramStageId( "anUid" );

        assertTrue(
            analyticsDimensions
                .getDataElements()
                .stream()
                .map( de -> ((DataElement) de).getValueType() )
                .allMatch(
                    AGGREGATE_ALLOWED_VALUE_TYPES::contains ) );
        assertTrue(
            analyticsDimensions
                .getTrackedEntityAttributes()
                .stream()
                .map( tea -> ((TrackedEntityAttribute) tea).getValueType() )
                .allMatch(
                    AGGREGATE_ALLOWED_VALUE_TYPES::contains ) );
    }

    private List<TrackedEntityAttribute> allValueTypeTEAs()
    {
        return buildWithAllValueTypes( valueType -> {
            TrackedEntityAttribute trackedEntityAttribute = new TrackedEntityAttribute();
            trackedEntityAttribute.setUid( "uid" + valueType.name() );
            trackedEntityAttribute.setValueType( valueType );
            return trackedEntityAttribute;
        } ).collect( Collectors.toList() );
    }

    private Set<DataElement> allValueTypeDataElements()
    {
        return buildWithAllValueTypes( valueType -> {
            DataElement dataElement = new DataElement();
            dataElement.setUid( "uid" + valueType.name() );
            dataElement.setValueType( valueType );
            return dataElement;
        } ).collect( Collectors.toSet() );
    }

    private <T> Stream<T> buildWithAllValueTypes( Function<ValueType, T> mapper )
    {
        return Arrays.stream( ValueType.values() )
            .map( mapper );
    }
}
