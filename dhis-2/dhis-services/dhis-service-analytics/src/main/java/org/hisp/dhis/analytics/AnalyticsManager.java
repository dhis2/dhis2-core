package org.hisp.dhis.analytics;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Map;
import java.util.concurrent.Future;

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ListMap;

/**
 * Manager for queries for retrieval of analytics data.
 * 
 * @author Lars Helge Overland
 */
public interface AnalyticsManager
{
    /**
     * Retrieves aggregated data values for the given query. The data is returned
     * as a mapping where the key is concatenated from the dimension options for
     * all dimensions separated by "-", and the value is the data value. This 
     * method is invoked asynchronously. The value class can be Double or String.
     * 
     * @param params the query to retrieve aggregated data for.
     * @param maxLimit the max number of records to retrieve.
     * @return a map.
     * @throws IllegalQueryException if query result set exceeds the max limit.
     */
    Future<Map<String, Object>> getAggregatedDataValues( DataQueryParams params, int maxLimit );
    
    /**
     * Inserts entries for the aggregation periods mapped to each data period
     * in the given data value map. Removes the original entry for the data period.
     * 
     * @param dataValueMap map with entries for all data values produced for the query.
     * @param params the query.
     * @param dataPeriodAggregationPeriodMap the mapping between data periods and
     *        aggregation periods for this query.
     */
    void replaceDataPeriodsWithAggregationPeriods( Map<String, Object> dataValueMap, 
        DataQueryParams params, ListMap<DimensionalItemObject, DimensionalItemObject> dataPeriodAggregationPeriodMap );
}
