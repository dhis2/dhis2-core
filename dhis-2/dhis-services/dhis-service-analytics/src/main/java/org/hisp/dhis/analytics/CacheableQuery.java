/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics;

import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Provides the basic methods responsible for fetching data from a given database.
 * The methods are coupled to the current JDBC abstraction.
 */
public interface CacheableQuery
{

    /**
     * Fetches the data based on the given sqlQuery parameter.
     * The data query params can be used for validation of specific rules or for any
     * other particularity.
     *
     * If one need caching capabilities, it should be implement through this method.
     *
     * @param sqlQuery the full sql query.
     * @param params the current data params.
     * @return a SQLRowSet object containing the results fetched.
     */
    SqlRowSet fetch( final String sqlQuery, final DataQueryParams params );

    /**
     * The main responsibility of this method is forcing a fetch directly
     * to the database bypassing any caching layer or previous states.
     *
     * No caching capability should be implemented here, so the "force"
     * behavior can be respected.
     *
     * @param sqlQuery
     * @return a SQLRowSet object containing the results fetched.
     */
    SqlRowSet forceFetch( final String sqlQuery );
}
