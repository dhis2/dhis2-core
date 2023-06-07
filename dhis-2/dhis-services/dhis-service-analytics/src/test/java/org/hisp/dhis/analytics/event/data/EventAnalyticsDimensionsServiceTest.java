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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.common.AnalyticsDimensionsTestSupport.allValueTypeDataElements;
import static org.hisp.dhis.analytics.common.AnalyticsDimensionsTestSupport.allValueTypeTEAs;
import static org.hisp.dhis.analytics.common.DimensionServiceCommonTest.aggregateAllowedValueTypesPredicate;
import static org.hisp.dhis.analytics.common.DimensionServiceCommonTest.queryDisallowedValueTypesPredicate;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventAnalyticsDimensionsServiceTest
{
    private EventAnalyticsDimensionsService eventAnalyticsDimensionsService;

    @BeforeEach
    void setup()
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
            categoryService, mock( AclService.class ), mock( CurrentUserService.class ) );
    }

    @Test
    void testQueryDoesntContainDisallowedValueTypes()
    {
        Collection<BaseIdentifiableObject> analyticsDimensions = eventAnalyticsDimensionsService
            .getQueryDimensionsByProgramStageId( "anUid" ).stream()
            .map( PrefixedDimension::getItem )
            .collect( Collectors.toList() );

        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof DataElement )
                .map( de -> ((DataElement) de).getValueType() )
                .noneMatch( queryDisallowedValueTypesPredicate() ) );
        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof TrackedEntityAttribute )
                .map( tea -> ((TrackedEntityAttribute) tea).getValueType() )
                .noneMatch( queryDisallowedValueTypesPredicate() ) );
    }

    @Test
    void testAggregateOnlyContainsAllowedValueTypes()
    {
        Collection<BaseIdentifiableObject> analyticsDimensions = eventAnalyticsDimensionsService
            .getAggregateDimensionsByProgramStageId( "anUid" ).stream()
            .map( PrefixedDimension::getItem )
            .collect( Collectors.toList() );

        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof DataElement )
                .map( de -> ((DataElement) de).getValueType() )
                .allMatch( aggregateAllowedValueTypesPredicate() ) );
        assertTrue(
            analyticsDimensions
                .stream()
                .filter( b -> b instanceof TrackedEntityAttribute )
                .map( tea -> ((TrackedEntityAttribute) tea).getValueType() )
                .allMatch( aggregateAllowedValueTypesPredicate() ) );
    }
}
