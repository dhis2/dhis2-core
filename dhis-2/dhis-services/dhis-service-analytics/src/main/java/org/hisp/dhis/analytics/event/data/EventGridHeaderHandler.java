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

import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DIVISOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.DIVISOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.MULTIPLIER_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.MULTIPLIER_ID;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.event.data.LabelMapper.getEnrollmentDateLabel;
import static org.hisp.dhis.analytics.event.data.LabelMapper.getEventDateLabel;
import static org.hisp.dhis.analytics.event.data.LabelMapper.getIncidentDateLabel;
import static org.hisp.dhis.common.DimensionalObject.DATA_COLLAPSED_DIM_ID;
import static org.hisp.dhis.common.ValueType.DATE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.system.grid.ListGrid;

public final class EventGridHeaderHandler
{
    private EventGridHeaderHandler()
    {
    }

    public static final String NAME_EVENT = "Event";

    public static final String NAME_TRACKED_ENTITY_INSTANCE = "Tracked entity instance";

    public static final String NAME_PROGRAM_INSTANCE = "Program instance";

    public static final String NAME_PROGRAM_STAGE = "Program stage";

    public static final String NAME_EVENT_DATE = "Event date";

    public static final String NAME_STORED_BY = "Stored by";

    public static final String NAME_LAST_UPDATED = "Last Updated";

    public static final String NAME_ENROLLMENT_DATE = "Enrollment date";

    public static final String NAME_INCIDENT_DATE = "Incident date";

    public static final String NAME_GEOMETRY = "Geometry";

    public static final String NAME_LONGITUDE = "Longitude";

    public static final String NAME_LATITUDE = "Latitude";

    public static final String NAME_ORG_UNIT_NAME = "Organisation unit name";

    public static final String NAME_ORG_UNIT_CODE = "Organisation unit code";

    public static final String NAME_COUNT = "Count";

    public static final String NAME_CENTER = "Center";

    public static final String NAME_EXTENT = "Extent";

    public static final String NAME_POINTS = "Points";

    public static final String ITEM_EVENT = "psi";

    public static final String ITEM_TRACKED_ENTITY_INSTANCE = "tei";

    public static final String ITEM_PROGRAM_INSTANCE = "pi";

    public static final String ITEM_PROGRAM_STAGE = "ps";

    public static final String ITEM_EVENT_DATE = "eventdate";

    public static final String ITEM_STORED_BY = "storedby";

    public static final String ITEM_LAST_UPDATED = "lastupdated";

    public static final String ITEM_ENROLLMENT_DATE = "enrollmentdate";

    public static final String ITEM_INCIDENT_DATE = "incidentdate";

    public static final String ITEM_GEOMETRY = "geometry";

    public static final String ITEM_LONGITUDE = "longitude";

    public static final String ITEM_LATITUDE = "latitude";

    public static final String ITEM_ORG_UNIT_NAME = "ouname";

    public static final String ITEM_ORG_UNIT_CODE = "oucode";

    public static final String ITEM_COUNT = "count";

    public static final String ITEM_CENTER = "center";

    public static final String ITEM_EXTENT = "extent";

    public static final String ITEM_POINTS = "points";

    private static final Map<String, GridHeader> ALLOWED_GRID_HEADER_MAP = new HashMap<>();

    static
    {
        // @formatter:off
            ALLOWED_GRID_HEADER_MAP.put( ITEM_EVENT, new GridHeader( ITEM_EVENT, NAME_EVENT, TEXT, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_PROGRAM_STAGE, new GridHeader( ITEM_PROGRAM_STAGE, NAME_PROGRAM_STAGE, TEXT, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_STORED_BY, new GridHeader( ITEM_STORED_BY, NAME_STORED_BY, TEXT, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_LAST_UPDATED, new GridHeader( ITEM_LAST_UPDATED, NAME_LAST_UPDATED, DATE, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_TRACKED_ENTITY_INSTANCE, new GridHeader( ITEM_TRACKED_ENTITY_INSTANCE, NAME_TRACKED_ENTITY_INSTANCE, TEXT, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_PROGRAM_INSTANCE, new GridHeader( ITEM_PROGRAM_INSTANCE, NAME_PROGRAM_INSTANCE, TEXT, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_GEOMETRY, new GridHeader( ITEM_GEOMETRY, NAME_GEOMETRY, TEXT, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_LONGITUDE, new GridHeader( ITEM_LONGITUDE, NAME_LONGITUDE, NUMBER, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_LATITUDE, new GridHeader( ITEM_LATITUDE, NAME_LATITUDE, NUMBER, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_ORG_UNIT_NAME, new GridHeader( ITEM_ORG_UNIT_NAME, NAME_ORG_UNIT_NAME, TEXT, false, true ) );
            ALLOWED_GRID_HEADER_MAP.put( ITEM_ORG_UNIT_CODE, new GridHeader( ITEM_ORG_UNIT_CODE, NAME_ORG_UNIT_CODE, TEXT, false, true ) );
            // @formatter:on
    }

    static Grid createGridWithDefaultHeaders( final EventQueryParams params )
    {
        final Grid grid = new ListGrid();

        grid
            .addHeader( new GridHeader( ITEM_EVENT, NAME_EVENT, TEXT, false, true ) )
            .addHeader( new GridHeader( ITEM_PROGRAM_STAGE, NAME_PROGRAM_STAGE, TEXT, false, true ) )
            .addHeader( new GridHeader( ITEM_EVENT_DATE,
                getEventDateLabel( params.getProgramStage(), NAME_EVENT_DATE ), DATE, false, true ) )
            .addHeader( new GridHeader( ITEM_STORED_BY, NAME_STORED_BY, TEXT, false, true ) )
            .addHeader( new GridHeader( ITEM_LAST_UPDATED, NAME_LAST_UPDATED, DATE, false, true ) );

        if ( params.getProgram().isRegistration() )
        {
            grid
                .addHeader( new GridHeader( ITEM_ENROLLMENT_DATE,
                    getEnrollmentDateLabel( params.getProgramStage(), NAME_ENROLLMENT_DATE ), DATE, false, true ) )
                .addHeader( new GridHeader(
                    ITEM_INCIDENT_DATE,
                    getIncidentDateLabel( params.getProgramStage(), NAME_INCIDENT_DATE ), DATE, false, true ) )
                .addHeader( new GridHeader(
                    ITEM_TRACKED_ENTITY_INSTANCE, NAME_TRACKED_ENTITY_INSTANCE, TEXT, false, true ) )
                .addHeader( new GridHeader(
                    ITEM_PROGRAM_INSTANCE, NAME_PROGRAM_INSTANCE, TEXT, false, true ) );
        }

        grid
            .addHeader( new GridHeader(
                ITEM_GEOMETRY, NAME_GEOMETRY, TEXT, false, true ) )
            .addHeader( new GridHeader(
                ITEM_LONGITUDE, NAME_LONGITUDE, NUMBER, false, true ) )
            .addHeader( new GridHeader(
                ITEM_LATITUDE, NAME_LATITUDE, NUMBER, false, true ) )
            .addHeader( new GridHeader(
                ITEM_ORG_UNIT_NAME, NAME_ORG_UNIT_NAME, TEXT, false, true ) )
            .addHeader( new GridHeader(
                ITEM_ORG_UNIT_CODE, NAME_ORG_UNIT_CODE, TEXT, false, true ) );

        return grid;
    }

    static Grid createGridWithParamHeaders( final List<String> headers, final EventQueryParams params )
    {
        final Grid grid = new ListGrid( headers.size() );
        final Map<String, GridHeader> gridHeadersWithDynamicNames = getGridHeadersWithDynamicNames( params );

        for ( final String header : headers )
        {
            if ( ALLOWED_GRID_HEADER_MAP.containsKey( header ) )
            {
                grid.replaceHeader( headers.indexOf( header ), ALLOWED_GRID_HEADER_MAP.get( header ) );
            }
            else if ( gridHeadersWithDynamicNames.containsKey( header ) )
            {
                grid.replaceHeader( headers.indexOf( header ), gridHeadersWithDynamicNames.get( header ) );
            }
        }

        return grid;
    }

    static Grid createGridWithClusterHeaders()
    {
        final Grid grid = new ListGrid();

        grid
            .addHeader( new GridHeader( ITEM_COUNT, NAME_COUNT, NUMBER, false, false ) )
            .addHeader( new GridHeader( ITEM_CENTER, NAME_CENTER, TEXT, false, false ) )
            .addHeader( new GridHeader( ITEM_EXTENT, NAME_EXTENT, TEXT, false, false ) )
            .addHeader( new GridHeader( ITEM_POINTS, NAME_POINTS, TEXT, false, false ) );

        return grid;
    }

    static Grid createGridWithAggregatedHeaders( final EventQueryParams params )
    {
        final Grid grid = new ListGrid();

        if ( params.isCollapseDataDimensions() || params.isAggregateData() )
        {
            grid.addHeader( new GridHeader( DATA_COLLAPSED_DIM_ID, DISPLAY_NAME_DATA_X, TEXT, false, true ) );
        }
        else
        {
            for ( QueryItem item : params.getItems() )
            {
                grid.addHeader( new GridHeader(
                    item.getItem().getUid(), item.getItem().getDisplayProperty( params.getDisplayProperty() ),
                    item.getValueType(), false, true, item.getOptionSet(), item.getLegendSet() ) );
            }
        }

        for ( DimensionalObject dimension : params.getDimensions() )
        {
            grid.addHeader( new GridHeader(
                dimension.getDimension(), dimension.getDisplayProperty( params.getDisplayProperty() ),
                TEXT, false, true ) );
        }

        grid.addHeader( new GridHeader( VALUE_ID, VALUE_HEADER_NAME, NUMBER, false, false ) );

        if ( params.isIncludeNumDen() )
        {
            grid
                .addHeader( new GridHeader( NUMERATOR_ID, NUMERATOR_HEADER_NAME, NUMBER, false, false ) )
                .addHeader( new GridHeader( DENOMINATOR_ID, DENOMINATOR_HEADER_NAME, NUMBER, false, false ) )
                .addHeader( new GridHeader( FACTOR_ID, FACTOR_HEADER_NAME, NUMBER, false, false ) )
                .addHeader( new GridHeader( MULTIPLIER_ID, MULTIPLIER_HEADER_NAME, NUMBER, false, false ) )
                .addHeader( new GridHeader( DIVISOR_ID, DIVISOR_HEADER_NAME, NUMBER, false, false ) );
        }

        return grid;
    }

    private static Map<String, GridHeader> getGridHeadersWithDynamicNames( final EventQueryParams params )
    {
        // @formatter:off
        return Map.of(
            ITEM_ENROLLMENT_DATE, new GridHeader( ITEM_ENROLLMENT_DATE,
                getEnrollmentDateLabel( params.getProgramStage(), NAME_ENROLLMENT_DATE ), DATE, false, true ),

            ITEM_INCIDENT_DATE, new GridHeader( ITEM_INCIDENT_DATE,
                LabelMapper.getIncidentDateLabel( params.getProgramStage(), NAME_INCIDENT_DATE ), DATE, false, true ),

            ITEM_EVENT_DATE, new GridHeader( ITEM_EVENT_DATE,
                LabelMapper.getEventDateLabel( params.getProgramStage(), NAME_EVENT_DATE ), DATE, false, true ) );
        // @formatter:on
    }
}
