package org.hisp.dhis.analytics.event;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;

import java.util.List;

/**
 * @author Lars Helge Overland
 */
public interface EventQueryValidator
{
    /**
     * Validates the given query. Throws an IllegalQueryException if the query
     * is not valid with a descriptive message. Returns normally if the query is
     * valid.
     *
     * @param params the event query parameters.
     * @throws IllegalQueryException if the query is invalid.
     */
    void validate( EventQueryParams params )
        throws IllegalQueryException, MaintenanceModeException;

    /**
     * Validates whether the given table layout is valid for the given query.
     * Throws an IllegalQueryException if the query is not valid with a
     * descriptive message. Returns normally if the query is valid.
     *
     * @param params the event query parameters.
     * @param columns the column dimension identifiers.
     * @param rows the row dimension identifiers.
     * @throws IllegalQueryException if the query is invalid.
     */
    void validateTableLayout( EventQueryParams params, List<String> columns, List<String> rows )
        throws IllegalQueryException;

    /**
     * Returns the max number of records to return. A value of 0 indicates no limit.
     *
     * @return the max number of recrods to return.
     */
    int getMaxLimit();
}
