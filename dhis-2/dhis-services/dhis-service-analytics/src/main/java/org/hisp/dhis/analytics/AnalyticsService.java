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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * <p>This interface is responsible for retrieving aggregated data. Data will be
 * returned in a grid object or as a dimensional key-value mapping.</p>
 * 
 * <p>Most objects accept a DataQueryParams object which encapsulates the query 
 * parameters. The dimensions in the response will appear in the same order as
 * they are set on the DataQueryParams object. You can use various methods for
 * setting indicators, data elements, data sets, periods, organisation units,
 * categories, data element group sets and organisation unit group sets on the
 * the DataQueryParams object. Objects can be defined as dimensions or filters.</p>
 * 
 * <p>Example usage for setting multiple indicators and a period as dimensions
 * and an organisation unit as filter. In the grid response the first column
 * will contain indicator identifiers, the second column will contain period
 * identifiers and the third column will contain aggregated values. Note that
 * the organisation unit is excluded since it is defined as a filter:</p>
 * 
 * <pre>
 * <code>
 * DataQueryParams params = new DataQueryParams();
 * 
 * params.setIndicators( indicators );
 * params.setPeriod( period );
 * params.setFilterOrganisationUnit( organisationUnit );
 * 
 * Grid grid = analyticsService.getAggregatedDataValues( params );
 * </code>
 * </pre>
 * 
 * <p>Example usage for including category option combos in the response. Note that 
 * the index position of category option combos will follow the order of when the
 * enableCategoryOptionCombos method was called. In the map response, the keys
 * will represent the dimensions defined in the DataQueryParams object and will
 * contain dimension identifiers separated by the "-" character. The key will
 * be of type String and contain a data element identifier, a category option 
 * combo identifier and an organisation unit identifier in that order. The map 
 * values will be the aggregated values of type Double:</p>
 * 
 * <pre>
 * <code>
 * DataQueryParams params = new DataQueryParams();
 * 
 * params.setDataElement( dataElement );
 * params.enableCategoryOptionCombos();
 * params.setOrganisationUnits( organisationUnits );
 * params.setFilterPeriod( period );
 * 
 * Map<String, Double> map = analyticsService.getAggregatedDataValueMapping( params );
 * </code>
 * </pre>
 * 
 * @author Lars Helge Overland
 */
public interface AnalyticsService
{
    final String NAMES_META_KEY = "names";
    final String PAGER_META_KEY = "pager";
    final String OU_HIERARCHY_KEY = "ouHierarchy";
    final String OU_NAME_HIERARCHY_KEY = "ouNameHierarchy";
    
    /**
     * Generates aggregated values for the given query.
     * 
     * @param params the data query parameters.
     * @return aggregated data as a Grid object.
     */
    Grid getAggregatedDataValues( DataQueryParams params );
    
    /**
     * Generates an aggregated value grid for the given query. The grid will
     * represent a table with dimensions used as columns and rows as specified
     * in columns and rows dimension arguments.
     * 
     * @param params the data query parameters.
     * @param tableLayout whether to render the grid as a table with columns and rows,
     *        or as a normalized plain data source.
     * @param columns the identifiers of the dimensions to use as columns.
     * @param rows the identifiers of the dimensions to use as rows.
     * @return aggregated data as a Grid object.
     */
    Grid getAggregatedDataValues( DataQueryParams params, boolean tableLayout, List<String> columns, List<String> rows );

    /**
     * Generates a mapping where the key represents the dimensional item identifiers
     * concatenated by "-" and the value is the corresponding aggregated data value
     * based on the given DataQueryParams.
     * 
     * @param params the DataQueryParams.
     * @return a mapping of dimensional items and aggregated data values.
     */
    Map<String, Object> getAggregatedDataValueMapping( DataQueryParams params );

    /**
     * Generates a mapping where the key represents the dimensional item identifiers
     * concatenated by "-" and the value is the corresponding aggregated data value
     * based on the given AnalyticalObject.
     * 
     * @param object the BaseAnalyticalObject.
     * @param format the I18nFormat, can be null.
     * @return a mapping of dimensional items and aggregated data values.
     */
    Map<String, Object> getAggregatedDataValueMapping( AnalyticalObject object, I18nFormat format );

    /**
     * Creates a data query parameter object from the given URL.
     * 
     * @param dimensionParams the dimension URL parameters.
     * @param filterParams the filter URL parameters.
     * @param aggregationType the aggregation type.
     * @param measureCriteria the measure criteria.
     * @param skipMeta whether to skip the meta data part of the response.
     * @param skipData whether to skip the data part of the response.
     * @param skipRounding whether to skip rounding and provide full precision 
     *        for values.
     * @param completedOnly whether to only include completed events.
     * @param hierarchyMeta whether to include meta data about the organisation 
     *        units in the hierarchy.
     * @param ignoreLimit whether to ignore the max number of cells limit.
     * @param hideEmptyRows whether to hide rows without data values, applies to
     *        table layout.
     * @param showHierarchy whether to show the organisation unit hierarchy 
     *        together with the name.
     * @param displayProperty the property to display for meta-data.
     * @param outputIdScheme the identifier scheme to use in the query response.
     * @param approvalLevel the approval level identifier.
     * @param relativePeriodDate the date to use as basis for relative periods.
     * @param userOrgUnit the user organisation unit to use, overrides current user.
     * @param program the program identifier.
     * @param stage the program stage identifier.
     * @param format the i18n format.
     * @return a data query parameter object created based on the given URL info.
     */
    DataQueryParams getFromUrl( Set<String> dimensionParams, Set<String> filterParams, AggregationType aggregationType, String measureCriteria, 
        boolean skipMeta, boolean skipData, boolean skipRounding, boolean completedOnly, boolean hierarchyMeta, boolean ignoreLimit, 
        boolean hideEmptyRows, boolean showHierarchy, DisplayProperty displayProperty, IdentifiableProperty outputIdScheme, String approvalLevel, 
        Date relativePeriodDate, String userOrgUnit, String program, String stage, I18nFormat format );
    
    /**
     * Creates a data query parameter object from the given BaseAnalyticalObject.
     * 
     * @param object the BaseAnalyticalObject
     * @param format the i18n format.
     * @return a data query parameter object created based on the given BaseAnalyticalObject.
     */
    DataQueryParams getFromAnalyticalObject( AnalyticalObject object, I18nFormat format );
    
    /**
     * Creates a list of DimensionalObject from the given set of dimension params.
     * 
     * @param dimensionParams the dimension URL params.
     * @param relativePeriodDate the date to use as basis for relative periods.
     * @param userOrgUnit the user organisation unit param, overrides current
     *        user, can be null.
     * @param format the i18n format.
     * @return a list of DimensionalObject.
     */
    List<DimensionalObject> getDimensionalObjects( Set<String> dimensionParams, Date relativePeriodDate, String userOrgUnit, I18nFormat format );
    
    /**
     * Returns a persisted DimensionalObject generated from the given  dimension 
     * identifier and list of dimension options.
     * 
     * For the pe dimension items, relative periods represented by enums will be 
     * replaced by real ISO periods relative to the current date. For the ou 
     * dimension items, the user  organisation unit enums 
     * USER_ORG_UNIT|USER_ORG_UNIT_CHILDREN will be replaced by the persisted 
     * organisation units for the current user.
     * 
     * @param dimension the dimension identifier.
     * @param items the dimension items.
     * @param relativePeriodDate the date to use for generating relative periods, can be null.
     * @param userOrgUnits the list of user organisation units, overrides current
     *        user, can be null.
     * @param format the I18nFormat, can be null.
     * @param allowNull return null if no dimension was found.
     * @throws IllegalQueryException if no dimensions was found.
     * @return list of DimensionalObjects.
     */
    DimensionalObject getDimension( String dimension, List<String> items, Date relativePeriodDate, 
        List<OrganisationUnit> userOrgUnits, I18nFormat format, boolean allowNull );
    
    /**
     * Returns a list of user organisation units, looking first at the given user 
     * org unit parameter, second at the organisation units associated with the
     * current user. Returns an empty list if no organisation units are found.
     * 
     * @param userOrgUnit the user org unit parameter string.
     * @return a list of organisation units.
     */
    List<OrganisationUnit> getUserOrgUnits( String userOrgUnit );
}
