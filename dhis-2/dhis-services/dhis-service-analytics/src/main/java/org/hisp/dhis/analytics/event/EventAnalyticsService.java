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
package org.hisp.dhis.analytics.event;

import java.util.List;

import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.Grid;

/**
 * This interface is responsible for retrieving aggregated event data. Data will
 * be returned in a grid object or as a dimensional key-value mapping.
 *
 * @author Lars Helge Overland
 */
public interface EventAnalyticsService
{
    String ITEM_EVENT = "psi";

    String ITEM_TRACKED_ENTITY_INSTANCE = "tei";

    String ITEM_PROGRAM_INSTANCE = "pi";

    String ITEM_PROGRAM_STAGE = "ps";

    String ITEM_EVENT_DATE = "eventdate";

    String ITEM_STORED_BY = "storedby";

    String ITEM_CREATED_BY_DISPLAY_NAME = "createdbydisplayname";

    String ITEM_LAST_UPDATED_BY_DISPLAY_NAME = "lastupdatedbydisplayname";

    String ITEM_LAST_UPDATED = "lastupdated";

    String ITEM_SCHEDULED_DATE = "scheduleddate";

    String ITEM_ENROLLMENT_DATE = "enrollmentdate";

    String ITEM_INCIDENT_DATE = "incidentdate";

    String ITEM_GEOMETRY = "geometry";

    String ITEM_LONGITUDE = "longitude";

    String ITEM_LATITUDE = "latitude";

    String ITEM_ORG_UNIT_NAME = "ouname";

    String ITEM_ORG_UNIT_CODE = "oucode";

    String ITEM_COUNT = "count";

    String ITEM_CENTER = "center";

    String ITEM_EXTENT = "extent";

    String ITEM_POINTS = "points";

    String ITEM_PROGRAM_STATUS = "programstatus";

    String ITEM_EVENT_STATUS = "eventstatus";

    /**
     * Generates aggregated event data for the given query.
     *
     * @param params the event query parameters.
     * @return aggregated event data as a Grid object.
     */
    Grid getAggregatedEventData( EventQueryParams params );

    /**
     * Generates an aggregated value grid for the given query. The grid will
     * represent a table with dimensions used as columns and rows as specified
     * in columns and rows dimension arguments. If columns and rows are null or
     * empty, the normalized table will be returned.
     *
     * If meta data is included in the query, the meta data map of the grid will
     * contain keys described in {@link AnalyticsMetaDataKey}.
     *
     * @param params the event query parameters.
     * @param columns the identifiers of the dimensions to use as columns.
     * @param rows the identifiers of the dimensions to use as rows.
     * @return aggregated data as a Grid object.
     */
    Grid getAggregatedEventData( EventQueryParams params, List<String> columns, List<String> rows )
        throws Exception;

    /**
     * Generates aggregated event data for the given analytical object.
     *
     * @param params the event query parameters.
     * @return aggregated event data as a Grid object.
     */
    Grid getAggregatedEventData( AnalyticalObject params );

    /**
     * Returns a list of events matching the given query.
     *
     * @param params the event query parameters.
     * @return events as a Grid object.
     */
    Grid getEvents( EventQueryParams params );

    /**
     * Returns a list of event clusters matching the given query.
     *
     * @param params the event query parameters.
     * @return event clusters as a Grid object.
     */
    Grid getEventClusters( EventQueryParams params );

    /**
     * Returns a Rectangle with information about event count and extent of the
     * spatial rectangle for the given query.
     *
     * @param params the event query parameters.
     * @return event clusters as a Grid object.
     */
    Rectangle getRectangle( EventQueryParams params );

}
