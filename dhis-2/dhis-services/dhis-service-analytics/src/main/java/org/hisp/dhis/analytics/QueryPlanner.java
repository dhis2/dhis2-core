package org.hisp.dhis.analytics;

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

import java.util.List;

import org.hisp.dhis.common.IllegalQueryException;

/**
 * Service interface which provides methods for validating and planning
 * analytics queries.
 *
 * @author Lars Helge Overland
 */
public interface QueryPlanner
{
    /**
     * Creates a DataQueryGroups object. It is mandatory to group the queries by
     * the following criteria: 1) partition / year 2) organisation unit level
     * 3) period type 4) aggregation type. The DataQueryGroups contains groups of
     * queries. The query groups should be run in sequence while the queries within
     * each group should be run in parallel for optimal performance. Currently
     * queries with different {@link AnalyticsAggregationType} are run in sequence.
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
     * @param plannerParams the query planner parameters.
     * @return a {@link DataQueryGroups} object.
     */
    DataQueryGroups planQuery( DataQueryParams params, QueryPlannerParams plannerParams  )
        throws IllegalQueryException;

    /**
     * Sets the table name and partitions on the given query.
     *
     * @param params the data query parameters.
     * @param plannerParams the query planner parameters.
     * @return a data query parameters.
     */
    DataQueryParams withTableNameAndPartitions( DataQueryParams params, QueryPlannerParams plannerParams );

    /**
     * If organisation units appear as dimensions; groups the given query into
     * sub queries based on the level of the organisation units. Sets the organisation
     * unit level on each query. If organisation units appear as filter; replaces
     * the organisation unit filter with one filter for each level. Sets the dimension
     * names and filter names respectively.
     *
     * @param params the data query parameters.
     * @return a list of data query parameters.
     */
    List<DataQueryParams> groupByOrgUnitLevel( DataQueryParams params );

    /**
     * If periods appear as dimensions in the given query; groups the query into
     * sub queries based on the period type of the periods. Sets the period type
     * name on each query. If periods appear as filters; replaces the period filter
     * with one filter for each period type. Sets the dimension names and filter
     * names respectively.
     *
     * @param params the data query parameters.
     * @return a list of data query parameters.
     */
    List<DataQueryParams> groupByPeriodType( DataQueryParams params );

    /**
     * If periods appear as dimensions in the given query; groups the given query
     * into sub queries based on start and end dates, i.e. one query per period.
     * Marks the period dimension as fixed and sets the dimension name to the period
     * ISO name. Sets the start date and end date properties. If periods appear
     * as filters in the given query; sets the start date and end date properties
     * based on the first period and removes the period dimension.
     *
     * @param params the data query parameters.
     * @return a list of data query parameters.
     */
    List<DataQueryParams> groupByStartEndDateRestriction( DataQueryParams params );
}
