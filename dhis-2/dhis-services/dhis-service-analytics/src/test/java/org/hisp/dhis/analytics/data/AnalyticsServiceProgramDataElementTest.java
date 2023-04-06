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
package org.hisp.dhis.analytics.data;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luciano Fiandesio
 */
class AnalyticsServiceProgramDataElementTest
    extends
    AnalyticsServiceBaseTest
{
    /**
     * This test verifies that a call to the Analytics Service with a Data
     * Element of type Program Data Element, triggers a call to the Event
     * Analytics Service
     */
    @Test
    void verifyProgramDataElementInQueryCallsEventsAnalytics()
    {
        ArgumentCaptor<EventQueryParams> capturedParams = ArgumentCaptor.forClass( EventQueryParams.class );

        DataElement de1 = createDataElement( 'A' );
        Program pr1 = createProgram( 'P' );
        ProgramDataElementDimensionItem pded1 = new ProgramDataElementDimensionItem( pr1, de1 );

        DataQueryParams params = DataQueryParams.newBuilder().withAggregationType( AnalyticsAggregationType.AVERAGE )
            .withPeriod( new Period( YearlyPeriodType.getPeriodFromIsoString( "2017W10" ) ) )
            .withDataElements( newArrayList( pded1 ) ).withIgnoreLimit( true )
            .withFilters( List.of( new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT, null,
                DISPLAY_NAME_ORGUNIT, List.of( new OrganisationUnit( "bbb", "bbb", "OU_2", null, null, "c2" ) ) ) ) )
            .build();

        initMock( params );

        Map<String, Object> emptyData = new HashMap<>();

        when( analyticsManager.getAggregatedDataValues( any( DataQueryParams.class ),
            eq( AnalyticsTableType.DATA_VALUE ), eq( 0 ) ) )
                .thenReturn( CompletableFuture.completedFuture( emptyData ) );

        when( eventAnalyticsService.getAggregatedEventData( any( EventQueryParams.class ) ) )
            .thenReturn( new ListGrid() );

        target.getAggregatedDataValueGrid( params );

        verify( eventAnalyticsService ).getAggregatedEventData( capturedParams.capture() );
        EventQueryParams data = capturedParams.getValue();

        assertThat( data.hasValueDimension(), is( false ) );
        assertThat( data.getItems(), hasSize( 1 ) );
        assertThat( data.getItems().get( 0 ).getItemId(), is( de1.getUid() ) );
        assertThat( data.getDimensions(), hasSize( 1 ) );
        assertThat( data.getDimensions().get( 0 ).getDimensionType(), is( DimensionType.PERIOD ) );
        assertThat( data.getFilters(), hasSize( 1 ) );
        assertThat( data.getFilters().get( 0 ).getDimensionType(), is( DimensionType.ORGANISATION_UNIT ) );
        assertThat( data.getAggregationType(), is( AnalyticsAggregationType.AVERAGE ) );
    }
}
