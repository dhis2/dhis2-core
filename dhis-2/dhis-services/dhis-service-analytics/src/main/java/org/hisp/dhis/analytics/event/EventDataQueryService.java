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

import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.Program;

/**
 * @author Lars Helge Overland
 */
public interface EventDataQueryService
{
    /**
     * Creates an {@link EventQueryParams} based on the given request.
     *
     * @param request the {@link EventDataQueryRequest} containing the URL
     *        parameters.
     * @return an {@link EventQueryParams}.
     */
    EventQueryParams getFromRequest( EventDataQueryRequest request );

    /**
     * Creates an {@link EventQueryParams} based on the given request.
     *
     * @param request the {@link EventDataQueryRequest} containing the URL
     *        parameters.
     * @return an {@link EventQueryParams}.
     */
    EventQueryParams getFromRequest( EventDataQueryRequest request, boolean analyzeOnly );

    /**
     * Creates an {@link EventQueryParams} based on the given event analytical
     * object.
     *
     * @param request the {@link EventAnalyticalObject}.
     * @return an {@link EventQueryParams}.
     */
    EventQueryParams getFromAnalyticalObject( EventAnalyticalObject object );

    /**
     * Returns the coordinate column field to use for the given coordinate
     * field. Coordinate field must match EVENT, a data element identifier or an
     * attribute identifier.
     *
     * @param coordinate the coordinate field.
     * @return the coordinate column field.
     * @throws IllegalQueryException if the coordinate field is not valid.
     */
    String getCoordinateField( String coordinate );

    /**
     * Returns the coordinate column field to use for the given coordinate
     * field. Coordinate field must match EVENT, a data element identifier or an
     * attribute identifier.
     *
     * @param coordinate the coordinate field.
     * @return the coordinate column field.
     * @throws IllegalQueryException if the coordinate field is not valid.
     */
    String getFallbackCoordinateField( String coordinate );

    /**
     * Returns a QueryItem
     *
     * @param dimensionString
     * @param program
     * @param type
     * @return
     */
    QueryItem getQueryItem( String dimensionString, Program program, EventOutputType type );
}
