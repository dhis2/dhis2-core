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

import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;

import java.util.List;
import java.util.Map;

/**
 * This interface is responsible for retrieving aggregated data. Data will be
 * returned in a grid object or as a dimensional key-value mapping.
 * <p>
 * Most objects accept a DataQueryParams object which encapsulates the query
 * parameters. The dimensions in the response will appear in the same order as
 * they are set on the DataQueryParams object. You can use various methods for
 * setting indicators, data elements, data sets, periods, organisation units,
 * categories, data element group sets and organisation unit group sets on the
 * the DataQueryParams object. Objects can be defined as dimensions or filters.
 * <p>
 * Example usage for setting multiple indicators and a period as dimensions and
 * an organisation unit as filter. In the grid response the first column will
 * contain indicator identifiers, the second column will contain period
 * identifiers and the third column will contain aggregated values. Note that
 * the organisation unit is excluded since it is defined as a filter:
 * </p>
 * <pre>
 * {@code
 * DataQueryParams params = new DataQueryParams();
 * 
 * params.setIndicators( indicators );
 * params.setPeriod( period );
 * params.setFilterOrganisationUnit( organisationUnit );
 * 
 * Grid grid = analyticsService.getAggregatedDataValues( params );
 * }
 * </pre>
 * <p>
 * The returned grid has a metaData object which contains metadata about the
 * response, such as a mapping between the UIDs and names of metadata objects.
 * For valid keys refer to the key property of {@link AnalyticsMetaDataKey}.</p>
 * <p>
 * Example usage for including category option combos in the response. Note that
 * the index position of category option combos will follow the order of when
 * the enableCategoryOptionCombos method was called. In the map response, the
 * keys will represent the dimensions defined in the DataQueryParams object and
 * will contain dimension identifiers separated by the "-" character. The key
 * will be of type String and contain a data element identifier, a category
 * option combo identifier and an organisation unit identifier in that order.
 * The map values will be the aggregated values of type Double:
 * </p>
 * <pre>
 * {@code
 * DataQueryParams params = DataQueryParams.newBuilder();
 *      .withDataElements( deA, deB )
 *      .withOrganisationUnits( ouA, ouB )
 *      .withFilterPeriods( peA, peB )
 *      .build();
 * 
 * Map<String, Double> map = analyticsService.getAggregatedDataValueMapping( params );
 * }
 * </pre>
 * 
 * @author Lars Helge Overland
 */
public interface AnalyticsService
{
    /**
     * Generates aggregated values for the given query. 
     * 
     * If meta data is included in the query, the meta data map of the grid
     * will contain keys described in {@link AnalyticsMetaDataKey}.
     * 
     * @param params the data query parameters.
     * @return aggregated data as a Grid object.
     */
    Grid getAggregatedDataValues( DataQueryParams params );

    /**
     * Generates an aggregated value grid for the given query. The grid will
     * represent a table with dimensions used as columns and rows as specified
     * in columns and rows dimension arguments. If columns and rows are null or
     * empty, the normalized table will be returned.
     * 
     * If meta data is included in the query, the meta data map of the grid
     * will contain keys described in {@link AnalyticsMetaDataKey}.
     * 
     * @param params the data query parameters.
     * @param columns the identifiers of the dimensions to use as columns.
     * @param rows the identifiers of the dimensions to use as rows.
     * @return aggregated data as a Grid object.
     */
    Grid getAggregatedDataValues( DataQueryParams params, List<String> columns, List<String> rows );
    
    /**
     * Generates a raw data value grid for the given query. The grid will
     * represent a table with denormalized raw data. This means that no 
     * aggregation will be performed on the data, and dimensions specified
     * in the query will be present for each row.
     * 
     * @param params the data query parameters.
     * @return raw data as a Grid object.
     */
    Grid getRawDataValues( DataQueryParams params );
    
    /**
     * Generates a data value set for the given query. The query must contain
     * a data, period and organisation unit dimension.
     * 
     * @param params the data query parameters.
     * @return a data value set representing aggregated data.
     */
    DataValueSet getAggregatedDataValueSet( DataQueryParams params );

    /**
     * Generates an aggregated value grid for the given query based on the given
     * analytical object.
     * 
     * @param object the analytical object.
     * @return aggregated data as a Grid object.
     */
    Grid getAggregatedDataValues( AnalyticalObject object );
    
    /**
     * Generates a mapping where the key represents the dimensional item
     * identifiers concatenated by "-" and the value is the corresponding
     * aggregated data value based on the given DataQueryParams.
     * 
     * @param params the DataQueryParams.
     * @return a mapping of dimensional items and aggregated data values.
     */
    Map<String, Object> getAggregatedDataValueMapping( DataQueryParams params );

    /**
     * Generates a mapping where the key represents the dimensional item
     * identifiers concatenated by "-" and the value is the corresponding
     * aggregated data value based on the given AnalyticalObject.
     * 
     * @param object the BaseAnalyticalObject.
     * @return a mapping of dimensional items and aggregated data values.
     */
    Map<String, Object> getAggregatedDataValueMapping( AnalyticalObject object );
}
