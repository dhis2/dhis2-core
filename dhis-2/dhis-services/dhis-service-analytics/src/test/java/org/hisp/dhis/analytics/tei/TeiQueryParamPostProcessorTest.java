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
package org.hisp.dhis.analytics.tei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestMapper;
import org.hisp.dhis.analytics.common.processing.DimensionIdentifierConverter;
import org.hisp.dhis.analytics.data.DefaultDataQueryService;
import org.hisp.dhis.analytics.event.data.DefaultEventDataQueryService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.DefaultProgramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TeiQueryParamPostProcessor}.
 */
class TeiQueryParamPostProcessorTest
{
    private TeiQueryParamPostProcessor teiQueryParamPostProcessor;

    @BeforeEach
    void setup()
    {
        DefaultDataQueryService dataQueryService = mock( DefaultDataQueryService.class );
        DefaultEventDataQueryService eventDataQueryService = mock( DefaultEventDataQueryService.class );
        DefaultProgramService programService = mock( DefaultProgramService.class );

        doAnswer( invocationOnMock -> mockPeriod() )
            .when( dataQueryService )
            .getDimension( any(), any(), any(), any(), anyBoolean(), any(), any() );

        CommonQueryRequestMapper commonQueryRequestMapper = new CommonQueryRequestMapper(
            dataQueryService,
            eventDataQueryService,
            programService,
            new DimensionIdentifierConverter() );

        teiQueryParamPostProcessor = new TeiQueryParamPostProcessor( commonQueryRequestMapper );
    }

    @Nonnull
    private static DimensionalObject mockPeriod()
    {
        return new BaseDimensionalObject( "pe", DimensionType.PERIOD, List.of( new Period() ) );
    }

    @Test
    void verifyDefaultPeriodIsAddedIfPeriodIsMissing()
    {
        // Given
        TeiQueryParams params = TeiQueryParams.builder()
            .commonParams( CommonParams.builder()
                .dimensionIdentifiers( List.of() )
                .build() )
            .build();

        // When
        params = teiQueryParamPostProcessor.process( params );

        // Then
        assertEquals( 1, params.getCommonParams().getDimensionIdentifiers().size() );
    }

    @Test
    void verifyDefaultPeriodIsNotAddedIfPeriodIsPresent()
    {
        // Given
        TeiQueryParams params = TeiQueryParams.builder()
            .commonParams( CommonParams.builder()
                .dimensionIdentifiers(
                    List.of(
                        DimensionIdentifier.of(
                            ElementWithOffset.of( null, null ),
                            ElementWithOffset.of( null, null ),
                            DimensionParam.ofObject(
                                mockPeriod(),
                                DimensionParamType.DIMENSIONS,
                                List.of( "LAST_12_MONTHS" ) ) ) ) )
                .build() )
            .build();

        // When
        params = teiQueryParamPostProcessor.process( params );

        // Then
        assertEquals( 1, params.getCommonParams().getDimensionIdentifiers().size() );
    }

    @Test
    void verifyDefaultPeriodIsAddedIfPeriodIsPresentWithNoItems()
    {
        // Given
        TeiQueryParams params = TeiQueryParams.builder()
            .commonParams( CommonParams.builder()
                .dimensionIdentifiers(
                    List.of(
                        DimensionIdentifier.of(
                            ElementWithOffset.of( null, null ),
                            ElementWithOffset.of( null, null ),
                            DimensionParam.ofObject(
                                mockPeriod(),
                                DimensionParamType.DIMENSIONS,
                                Collections.emptyList() ) ) ) )
                .build() )
            .build();

        // When
        params = teiQueryParamPostProcessor.process( params );

        // Then
        assertEquals( 2, params.getCommonParams().getDimensionIdentifiers().size() );
    }
}
