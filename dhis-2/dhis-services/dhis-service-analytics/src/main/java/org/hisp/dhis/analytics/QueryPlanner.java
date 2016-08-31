package org.hisp.dhis.analytics;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.List;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;

/**
 * @author Lars Helge Overland
 */
public interface QueryPlanner
{
    /**
     * Validates the given query. Throws an IllegalQueryException if the query
     * is not valid with a descriptive message. Returns normally if the query is
     * valid.
     * 
     * @param params the query.
     * @throws IllegalQueryException if the query is invalid.
     */
    void validate( DataQueryParams params )
        throws IllegalQueryException;
    
    /**
     * Validates whether the given table layout is valid for the given query. 
     * Throws an IllegalQueryException if the query is not valid with a 
     * descriptive message. Returns normally if the query is valid.
     * 
     * @param params the query.
     * @param columns the column dimension identifiers.
     * @param rows the row dimension identifiers.
     * @throws IllegalQueryException if the query is invalid.
     */
    void validateTableLayout( DataQueryParams params, List<String> columns, List<String> rows )
        throws IllegalQueryException;
    
    /**
     * Checks whether the analytics engine is in maintenance mode.
     * 
     * @throws MaintenanceModeException if analytics engine is in maintenance mode.
     */
    void validateMaintenanceMode()
        throws MaintenanceModeException;
    
    /**
     * Creates a DataQueryGroups object. It is mandatory to group the queries by
     * the following criteria: 1) partition / year 2) organisation  unit level
     * 3) period type 4) aggregation type. The DataQueryGroups contains groups of 
     * queries. The query groups should be run in sequence while the queries within
     * each group should be run in parallel for optimal performance.
     * 
     * If the number of queries produced by this grouping is equal or
     * larger than the number of optimal queries, those queries are returned. If
     * not it will split on the data element dimension, data set dimension and
     * organisation unit dimension, and return immediately after each step if
     * optimal queries are met.
     * 
     * It does not attempt to split on period dimension as splitting on columns 
     * with low cardinality typically does not improve performance.
     * 
     * @param params the data query parameters.
     * @param optimalQueries the number of optimal queries for the planner to 
     *        return for each query group.
     * @param tableName the base table name.
     * @return a DataQueryGroups object.
     */
    DataQueryGroups planQuery( DataQueryParams params, int optimalQueries, String tableName )
        throws IllegalQueryException;

    /**
     * If organisation units appear as dimensions; groups the given query into 
     * sub queries based on the level of the organisation units. Sets the organisation 
     * unit level on each query. If organisation units appear as filter; replaces
     * the organisation unit filter with one filter for each level. Sets the dimension
     * names and filter names respectively.
     */
    List<DataQueryParams> groupByOrgUnitLevel( DataQueryParams params );
    
    /**
     * Groups the given query into sub queries based on its periods and which 
     * partition it should be executed against. Sets the partition table name on
     * each query. Queries are grouped based on periods if appearing as a 
     * dimension.
     */
    List<DataQueryParams> groupByPartition( DataQueryParams params, String tableName, String tableSuffix );
    
    /**
     * If periods appear as dimensions in the given query; groups the query into 
     * sub queries based on the period type of the periods. Sets the period type 
     * name on each query. If periods appear as filters; replaces the period filter
     * with one filter for each period type. Sets the dimension names and filter
     * names respectively.
     */
    List<DataQueryParams> groupByPeriodType( DataQueryParams params );
}
