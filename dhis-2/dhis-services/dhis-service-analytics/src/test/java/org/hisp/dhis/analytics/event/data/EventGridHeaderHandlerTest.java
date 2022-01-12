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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_CENTER;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_COUNT;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_EVENT;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_EVENT_DATE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_EXTENT;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_GEOMETRY;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_INCIDENT_DATE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_LAST_UPDATED;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_LATITUDE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_LONGITUDE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_ORG_UNIT_CODE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_ORG_UNIT_NAME;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_POINTS;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_PROGRAM_INSTANCE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_PROGRAM_STAGE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_STORED_BY;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.ITEM_TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.createGridUsingParamHeaders;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.createGridWithAggregatedHeaders;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.createGridWithClusterHeaders;
import static org.hisp.dhis.analytics.event.data.EventGridHeaderHandler.createGridWithDefaultHeaders;
import static org.hisp.dhis.common.DimensionalObject.DATA_COLLAPSED_DIM_ID;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.program.ProgramType.WITH_REGISTRATION;

import java.util.List;

import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link EventGridHeaderHandler}
 */
class EventGridHeaderHandlerTest
{

    @Test
    void testCreateGridWithDefaultHeaders()
    {
        // Given
        final EventQueryParams anyQueryParams = new EventQueryParams.Builder().build();

        // When
        final Grid expected = createGridWithDefaultHeaders( anyQueryParams );

        // Then
        assertThat( expected.getHeaderWidth(), is( equalTo( 10 ) ) );
        assertThat( expected.getHeaders().size(), is( equalTo( 10 ) ) );
        assertThat( expected.getHeaders().get( 0 ).getName(), is( equalTo( ITEM_EVENT ) ) );
        assertThat( expected.getHeaders().get( 1 ).getName(), is( equalTo( ITEM_PROGRAM_STAGE ) ) );
        assertThat( expected.getHeaders().get( 2 ).getName(), is( equalTo( ITEM_EVENT_DATE ) ) );
        assertThat( expected.getHeaders().get( 3 ).getName(), is( equalTo( ITEM_STORED_BY ) ) );
        assertThat( expected.getHeaders().get( 4 ).getName(), is( equalTo( ITEM_LAST_UPDATED ) ) );
        assertThat( expected.getHeaders().get( 5 ).getName(), is( equalTo( ITEM_GEOMETRY ) ) );
        assertThat( expected.getHeaders().get( 6 ).getName(), is( equalTo( ITEM_LONGITUDE ) ) );
        assertThat( expected.getHeaders().get( 7 ).getName(), is( equalTo( ITEM_LATITUDE ) ) );
        assertThat( expected.getHeaders().get( 8 ).getName(), is( equalTo( ITEM_ORG_UNIT_NAME ) ) );
        assertThat( expected.getHeaders().get( 9 ).getName(), is( equalTo( ITEM_ORG_UNIT_CODE ) ) );
    }

    @Test
    void testCreateGridWithDefaultHeadersWithProgramRegistration()
    {
        // Given
        final Program programWithRegistration = new Program();
        programWithRegistration.setProgramType( WITH_REGISTRATION );

        final EventQueryParams queryParams = new EventQueryParams.Builder().withProgram( programWithRegistration )
            .build();

        // When
        final Grid expected = createGridWithDefaultHeaders( queryParams );

        // Then
        assertThat( expected.getHeaderWidth(), is( equalTo( 14 ) ) );
        assertThat( expected.getHeaders().size(), is( equalTo( 14 ) ) );
        assertThat( expected.getHeaders().get( 0 ).getName(), is( equalTo( ITEM_EVENT ) ) );
        assertThat( expected.getHeaders().get( 1 ).getName(), is( equalTo( ITEM_PROGRAM_STAGE ) ) );
        assertThat( expected.getHeaders().get( 2 ).getName(), is( equalTo( ITEM_EVENT_DATE ) ) );
        assertThat( expected.getHeaders().get( 3 ).getName(), is( equalTo( ITEM_STORED_BY ) ) );
        assertThat( expected.getHeaders().get( 4 ).getName(), is( equalTo( ITEM_LAST_UPDATED ) ) );
        assertThat( expected.getHeaders().get( 9 ).getName(), is( equalTo( ITEM_GEOMETRY ) ) );
        assertThat( expected.getHeaders().get( 10 ).getName(), is( equalTo( ITEM_LONGITUDE ) ) );
        assertThat( expected.getHeaders().get( 11 ).getName(), is( equalTo( ITEM_LATITUDE ) ) );
        assertThat( expected.getHeaders().get( 12 ).getName(), is( equalTo( ITEM_ORG_UNIT_NAME ) ) );
        assertThat( expected.getHeaders().get( 13 ).getName(), is( equalTo( ITEM_ORG_UNIT_CODE ) ) );

        // Program registration headers.
        assertThat( expected.getHeaders().get( 5 ).getName(), is( equalTo( ITEM_ENROLLMENT_DATE ) ) );
        assertThat( expected.getHeaders().get( 6 ).getName(), is( equalTo( ITEM_INCIDENT_DATE ) ) );
        assertThat( expected.getHeaders().get( 7 ).getName(), is( equalTo( ITEM_TRACKED_ENTITY_INSTANCE ) ) );
        assertThat( expected.getHeaders().get( 8 ).getName(), is( equalTo( ITEM_PROGRAM_INSTANCE ) ) );
    }

    @Test
    void testCreateGridUsingParamHeaders()
    {
        // Given
        final EventQueryParams anyQueryParams = new EventQueryParams.Builder().build();
        final List<String> paramHeaders = List.of( ITEM_STORED_BY, ITEM_GEOMETRY, ITEM_ORG_UNIT_NAME, ITEM_EVENT_DATE );

        // When
        final Grid expected = createGridUsingParamHeaders( paramHeaders, anyQueryParams );

        // Then
        assertThat( expected.getHeaderWidth(), is( equalTo( 4 ) ) );
        assertThat( expected.getHeaders().size(), is( equalTo( 4 ) ) );
        assertThat( expected.getHeaders().get( 0 ).getName(), is( equalTo( ITEM_STORED_BY ) ) );
        assertThat( expected.getHeaders().get( 1 ).getName(), is( equalTo( ITEM_GEOMETRY ) ) );
        assertThat( expected.getHeaders().get( 2 ).getName(), is( equalTo( ITEM_ORG_UNIT_NAME ) ) );
        assertThat( expected.getHeaders().get( 3 ).getName(), is( equalTo( ITEM_EVENT_DATE ) ) );
    }

    @Test
    void testCreateGridWithClusterHeaders()
    {
        // When
        final Grid expected = createGridWithClusterHeaders();

        // Then
        assertThat( expected.getHeaderWidth(), is( equalTo( 4 ) ) );
        assertThat( expected.getHeaders().size(), is( equalTo( 4 ) ) );
        assertThat( expected.getHeaders().get( 0 ).getName(), is( equalTo( ITEM_COUNT ) ) );
        assertThat( expected.getHeaders().get( 1 ).getName(), is( equalTo( ITEM_CENTER ) ) );
        assertThat( expected.getHeaders().get( 2 ).getName(), is( equalTo( ITEM_EXTENT ) ) );
        assertThat( expected.getHeaders().get( 3 ).getName(), is( equalTo( ITEM_POINTS ) ) );
    }

    @Test
    void testCreateGridWithAggregatedHeadersWhenIsCollapsedOrAggregateData()
    {
        // Given
        final EventQueryParams queryParams = new EventQueryParams.Builder()
            .withAggregateData( true )
            .withCollapseDataDimensions( true )
            .addItem( new QueryItem( new BaseDimensionalItemObject( "queryItem" ) ) )
            .addDimension( new BaseDimensionalObject( "dimension" ) )
            .build();

        // When
        final Grid expected = createGridWithAggregatedHeaders( queryParams );

        // Then
        assertThat( expected.getHeaderWidth(), is( equalTo( 3 ) ) );
        assertThat( expected.getHeaders().size(), is( equalTo( 3 ) ) );
        assertThat( expected.getHeaders().get( 0 ).getName(), is( equalTo( DATA_COLLAPSED_DIM_ID ) ) );
        assertThat( expected.getHeaders().get( 1 ).getName(), is( equalTo( "dimension" ) ) );
        assertThat( expected.getHeaders().get( 2 ).getName(), is( equalTo( VALUE_ID ) ) );
    }

    @Test
    void testCreateGridWithAggregatedHeaders()
    {
        // Given
        final EventQueryParams queryParams = new EventQueryParams.Builder()
            .addItem( new QueryItem( new BaseDimensionalItemObject( "queryItem" ), null, TEXT, null, null ) )
            .addDimension( new BaseDimensionalObject( "dimension" ) )
            .build();

        // When
        final Grid expected = createGridWithAggregatedHeaders( queryParams );

        // Then
        assertThat( expected.getHeaderWidth(), is( equalTo( 3 ) ) );
        assertThat( expected.getHeaders().size(), is( equalTo( 3 ) ) );
        assertThat( expected.getHeaders().get( 0 ).getName(), is( equalTo( "queryItem" ) ) );
        assertThat( expected.getHeaders().get( 1 ).getName(), is( equalTo( "dimension" ) ) );
        assertThat( expected.getHeaders().get( 2 ).getName(), is( equalTo( VALUE_ID ) ) );
    }
}
