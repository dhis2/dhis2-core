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

import static org.hisp.dhis.common.ValueType.DATE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.system.grid.ListGrid;

/**
 * Grid handler for enrollments. It encapsulates the logic to build and populate
 * Grid along with the respective GridHeader.
 *
 * @author maikel arabori
 */
public final class EnrollmentGridHeaderHandler
{
    private EnrollmentGridHeaderHandler()
    {
    }

    /**
     * Pretty column names used for displaying purposes.
     */
    private static final String NAME_TRACKED_ENTITY_INSTANCE = "Tracked entity instance";

    private static final String NAME_PROGRAM_INSTANCE = "Enrollment";

    private static final String NAME_GEOMETRY = "Geometry";

    private static final String NAME_ENROLLMENT_DATE = "Enrollment date";

    private static final String NAME_INCIDENT_DATE = "Incident date";

    private static final String NAME_STORED_BY = "Stored by";

    private static final String NAME_LAST_UPDATED = "Last Updated";

    private static final String NAME_LONGITUDE = "Longitude";

    private static final String NAME_LATITUDE = "Latitude";

    private static final String NAME_ORG_UNIT_NAME = "Organisation unit name";

    private static final String NAME_ORG_UNIT_CODE = "Organisation unit code";

    /**
     * Items descriptions that are used as column names for the headers.
     */
    public static final String ITEM_TRACKED_ENTITY_INSTANCE = "tei";

    public static final String ITEM_PROGRAM_INSTANCE = "pi";

    public static final String ITEM_ENROLLMENT_DATE = "enrollmentdate";

    public static final String ITEM_INCIDENT_DATE = "incidentdate";

    public static final String ITEM_STORED_BY = "storedby";

    public static final String ITEM_LAST_UPDATED = "lastupdated";

    public static final String ITEM_GEOMETRY = "geometry";

    public static final String ITEM_LONGITUDE = "longitude";

    public static final String ITEM_LATITUDE = "latitude";

    public static final String ITEM_ORG_UNIT_NAME = "ouname";

    public static final String ITEM_ORG_UNIT_CODE = "oucode";

    private static final Map<String, GridHeader> ALLOWED_GRID_HEADER_MAP = new HashMap<>();

    static
    {
        // @formatter:off
        ALLOWED_GRID_HEADER_MAP.put( ITEM_STORED_BY, new GridHeader( ITEM_STORED_BY, NAME_STORED_BY, TEXT, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_LAST_UPDATED, new GridHeader( ITEM_LAST_UPDATED, NAME_LAST_UPDATED, DATE, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_TRACKED_ENTITY_INSTANCE, new GridHeader( ITEM_TRACKED_ENTITY_INSTANCE, NAME_TRACKED_ENTITY_INSTANCE, TEXT, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_PROGRAM_INSTANCE, new GridHeader( ITEM_PROGRAM_INSTANCE, NAME_PROGRAM_INSTANCE, TEXT, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_GEOMETRY, new GridHeader( ITEM_GEOMETRY, NAME_GEOMETRY, TEXT, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_LONGITUDE, new GridHeader( ITEM_LONGITUDE, NAME_LONGITUDE, NUMBER, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_LATITUDE, new GridHeader( ITEM_LATITUDE, NAME_LATITUDE, NUMBER, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_ORG_UNIT_NAME, new GridHeader( ITEM_ORG_UNIT_NAME, NAME_ORG_UNIT_NAME, TEXT, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_ORG_UNIT_CODE, new GridHeader( ITEM_ORG_UNIT_CODE, NAME_ORG_UNIT_CODE, TEXT, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_INCIDENT_DATE, new GridHeader( ITEM_INCIDENT_DATE, NAME_INCIDENT_DATE, TEXT, false, true ) );
        ALLOWED_GRID_HEADER_MAP.put( ITEM_ENROLLMENT_DATE, new GridHeader( ITEM_ENROLLMENT_DATE, NAME_ENROLLMENT_DATE, TEXT, false, true ) );
        // @formatter:on
    }

    /**
     * Creates the default Grid and its headers for enrollments.
     *
     * @return the Grid along with its respective GridHeaders
     */
    static Grid createGridWithDefaultHeaders()
    {
        return new ListGrid()
            .addHeader( new GridHeader(
                ITEM_PROGRAM_INSTANCE, NAME_PROGRAM_INSTANCE, TEXT, false, true ) )
            .addHeader( new GridHeader(
                ITEM_TRACKED_ENTITY_INSTANCE, NAME_TRACKED_ENTITY_INSTANCE, TEXT, false, true ) )
            .addHeader( new GridHeader(
                ITEM_ENROLLMENT_DATE, NAME_ENROLLMENT_DATE, DATE, false, true ) )
            .addHeader( new GridHeader(
                ITEM_INCIDENT_DATE, NAME_INCIDENT_DATE, DATE, false, true ) )
            .addHeader( new GridHeader(
                ITEM_STORED_BY, NAME_STORED_BY, TEXT, false, true ) )
            .addHeader( new GridHeader(
                ITEM_LAST_UPDATED, NAME_LAST_UPDATED, DATE, false, true ) )
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
    }

    /**
     * Creates Grid and its headers for enrollments, based only on the given
     * params (some headers values are extracted from params).
     *
     * @param headers the list of headers
     * @return the Grid along with its respective GridHeaders
     */
    static Grid createGridUsingHeaders( final List<String> headers )
    {
        final Grid grid = new ListGrid( headers.size() );

        for ( final String header : headers )
        {
            if ( ALLOWED_GRID_HEADER_MAP.containsKey( header ) )
            {
                grid.replaceHeader( headers.indexOf( header ), ALLOWED_GRID_HEADER_MAP.get( header ) );
            }
        }

        return grid;
    }
}
