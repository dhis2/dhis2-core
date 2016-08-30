package org.hisp.dhis.analytics.event;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.Grid;

import org.hisp.dhis.analytics.Rectangle;

/**
 * <p>
 * This interface is responsible for retrieving aggregated event data. Data will 
 * be returned in a grid object or as a dimensional key-value mapping.
 * </p>
 * 
 * @author Lars Helge Overland
 */
public interface EventAnalyticsService
{    
    String ITEM_EVENT = "psi";
    String ITEM_PROGRAM_STAGE = "ps";
    String ITEM_EXECUTION_DATE = "eventdate";
    String ITEM_LONGITUDE = "longitude";
    String ITEM_LATITUDE = "latitude";
    String ITEM_ORG_UNIT_NAME = "ouname";
    String ITEM_ORG_UNIT_CODE = "oucode";
    String ITEM_COUNT = "count";
    String ITEM_CENTER = "center";
    String ITEM_EXTENT = "extent";
    String ITEM_POINTS = "points";
    
    /**
     * Generates aggregated event data for the given query.
     * 
     * @param params the event query parameters.
     * @return aggregated event data as a Grid object.
     */
    Grid getAggregatedEventData( EventQueryParams params );
    
    /**
     * Generates aggregated event data for the given analytical object.
     * 
     * @param params the event query parameters.
     * @return aggregated event data as a Grid object.
     */
    Grid getAggregatedEventData( AnalyticalObject object );

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
