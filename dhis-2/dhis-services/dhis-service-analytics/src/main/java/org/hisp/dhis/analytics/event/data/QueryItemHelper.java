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
package org.hisp.dhis.analytics.event.data;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;

/**
 * @author Dusan Bernat
 */
@Slf4j
public class QueryItemHelper
{
    private static final String ITEM_NAME_SEP = ": ";

    private static final String NA = "[N/A]";

    private QueryItemHelper()
    {
    }

    /**
     * Returns an item value (legend) for OutputIdScheme (Code, Name, Id, Uid).
     *
     * @param itemValue the item value.
     * @param params the {@link EventQueryParams}.
     */
    public static String getItemOptionValue( String itemValue, EventQueryParams params )
    {
        Optional<Option> itemOption = params.getItemOptions().stream()
            .filter( option -> option.getDisplayName().equalsIgnoreCase( itemValue ) )
            .findFirst();

        return itemOption.map( option -> params.getOutputIdScheme() == IdScheme.UID ? option.getUid()
            : params.getOutputIdScheme() == IdScheme.CODE ? option.getCode()
                : params.getOutputIdScheme() == IdScheme.NAME ? option.getName()
                    : Long.toString( option.getId() ) )
            .orElse( null );
    }

    /**
     * Returns an item value (option) for OutputIdScheme (Code, Name, Id, Uid).
     *
     * @param itemValue the item value.
     * @param params the {@link EventQueryParams}.
     */
    public static String getItemLegendValue( String itemValue, EventQueryParams params )
    {
        Optional<Legend> itemLegend = params.getItemLegends().stream()
            .filter( legend -> legend.getDisplayName().equalsIgnoreCase( itemValue ) )
            .findFirst();

        return itemLegend.map( legend -> params.getOutputIdScheme() == IdScheme.UID ? legend.getUid()
            : params.getOutputIdScheme() == IdScheme.CODE ? legend.getCode()
                : params.getOutputIdScheme() == IdScheme.NAME ? legend.getName()
                    : Long.toString( legend.getId() ) )
            .orElse( null );
    }

    /**
     * Returns an item value for the given query, query item and value. Assumes
     * that data dimensions are collapsed for the given query. Returns the short
     * name of the given query item followed by the item value. If the given
     * query item has a legend set, the item value is treated as an id and
     * substituted with the matching legend name. If the given query item has an
     * option set, the item value is treated as a code and substituted with the
     * matching option name.
     *
     * @param item the {@link QueryItem}.
     * @param itemValue the item value.
     * @return an item value for the given query.
     */
    public static String getCollapsedDataItemValue( QueryItem item, String itemValue )
    {
        String value = item.getItem().getDisplayShortName() + ITEM_NAME_SEP;

        Legend legend;
        Option option;

        if ( item.hasLegendSet() && (legend = item.getLegendSet().getLegendByUid( itemValue )) != null )
        {
            return value + legend.getDisplayName();
        }
        else if ( item.hasOptionSet() && (option = item.getOptionSet().getOptionByCode( itemValue )) != null )
        {
            return value + option.getDisplayName();
        }
        else
        {
            itemValue = StringUtils.defaultString( itemValue, NA );

            return value + itemValue;
        }
    }

    /**
     * Returns a list of options {@link Option}.
     *
     * Based on the given Grid and EventQueryParams, this method will return the
     * options correct list of options.
     *
     * When the Grid has no rows, it will return the options specified as
     * element "filter", ie.: Zj7UnCAulEk.K6uUAvq500H:IN:A03, where "A03" is the
     * option code.
     *
     * When the Grid has rows, this method will return only the options that are
     * part of the row object.
     *
     * @param grid the {@link Grid}.
     * @param queryItems the list of {@link QueryItem}.
     * @return a map of list of options.
     */
    public static Map<String, List<Option>> getItemOptions( Grid grid, List<QueryItem> queryItems )
    {
        Map<String, List<Option>> options = new HashMap<>();

        for ( int i = 0; i < grid.getHeaders().size(); ++i )
        {
            GridHeader gridHeader = grid.getHeaders().get( i );

            if ( gridHeader.hasOptionSet() && isNotEmpty( grid.getRows() ) )
            {
                options.put( gridHeader.getName(), getItemOptionsThatMatchesRows( grid, i ) );
            }
            else if ( gridHeader.hasOptionSet() && isEmpty( grid.getRows() ) )
            {
                options.put( gridHeader.getName(), getItemOptionsForEmptyRows( queryItems ) );
            }
        }

        return options;
    }

    /**
     * Based on the given options, it returns a set of {@link Option} objects
     * which are referenced as filter by any one of the query items provided.
     *
     * @param options the set of {@link Option}.
     * @param queryItems the list of {@link QueryItem}.
     * @return the set of {@link Option} found.
     */
    public static Set<Option> getItemOptionsAsFilter( Set<Option> options, List<QueryItem> queryItems )
    {
        Set<Option> matchedOptions = new LinkedHashSet<>();

        options.stream().filter( Objects::nonNull ).forEach(
            option -> {
                boolean queryItemsFilterMatchOptionCode = queryItems.stream().anyMatch(
                    queryItem -> queryItem.hasFilter() && filtersContainOption( option, queryItem.getFilters() ) );

                if ( queryItemsFilterMatchOptionCode )
                {
                    matchedOptions.add( option );
                }
            } );

        return matchedOptions;
    }

    /**
     * This method will check each filter in the given list of
     * {@link QueryFilter} objects. For each filter, it will try to match the
     * given option with any filter that contain an option or multiple options
     * (split by ";"). If a match is found, it will return true.
     *
     * Example of a possible filter: Zj7UnCAulEk.K6uUAvq500H:IN:A03;B01, where
     * "A03;B01" are the options codes.
     *
     * @param option the {@link Option}.
     * @param queryFilters the list of {@link QueryFilter}.
     * @return true if a match is found, false otherwise.
     */
    private static boolean filtersContainOption( Option option, List<QueryFilter> queryFilters )
    {
        for ( QueryFilter queryFilter : queryFilters )
        {
            String[] filterValues = defaultString( queryFilter.getFilter() ).split( ";" );

            for ( String filterValue : filterValues )
            {
                if ( filterValue.equalsIgnoreCase( option.getCode() ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * This method will extract the options (based on their codes) from the
     * element filter.
     *
     * @param queryItems the {@link EventQueryParams}.
     * @return the options for empty rows.
     */
    private static List<Option> getItemOptionsForEmptyRows( List<QueryItem> queryItems )
    {
        List<Option> options = new ArrayList<>();

        if ( isNotEmpty( queryItems ) )
        {
            List<QueryItem> items = queryItems;

            for ( QueryItem item : items )
            {
                boolean hasOptions = item.getOptionSet() != null
                    && isNotEmpty( item.getOptionSet().getOptions() );

                if ( hasOptions && isNotEmpty( item.getFilters() ) )
                {
                    options.addAll( getItemOptionsForFilter( item ) );
                }
            }
        }

        return options;
    }

    /**
     * For the list of rows, in the Grid, it will return only the options that
     * are part of each row object. It picks each option, from the list of all
     * options available, that matches the current header.
     *
     * @param grid the {@link Grid}.
     * @param columnIndex the column index.
     * @return a list of matching options.
     */
    private static List<Option> getItemOptionsThatMatchesRows( Grid grid, int columnIndex )
    {
        GridHeader gridHeader = grid.getHeaders().get( columnIndex );

        return gridHeader
            .getOptionSetObject()
            .getOptions()
            .stream()
            .filter( opt -> opt != null && grid.getRows().stream().anyMatch( r -> {
                Object o = r.get( columnIndex );

                return isItemOptionEqualToRowContent( opt.getCode(), o );
            } ) ).collect( Collectors.toList() );
    }

    /**
     * Compare option code and row content. Type of option value is derived from
     * ValueType enum
     *
     * @see org.hisp.dhis.common.ValueType
     * @param code
     * @param rowContent
     * @return true when equal
     */
    public static boolean isItemOptionEqualToRowContent( String code, Object rowContent )
    {
        if ( StringUtils.isBlank( code ) )
        {
            return false;
        }

        // String
        if ( rowContent instanceof String )
        {
            return ((String) rowContent).equalsIgnoreCase( code );
        }

        // Integer, Double
        if ( rowContent instanceof Number )
        {
            try
            {
                return ((Number) rowContent).doubleValue() == Double.parseDouble( code );
            }
            catch ( NumberFormatException e )
            {
                log.warn( String.format( "code %s is not Doublw", code ), e.getMessage() );
            }
        }

        // Boolean
        if ( rowContent instanceof Boolean )
        {
            return ((Boolean) rowContent) == Boolean.parseBoolean( code );
        }

        // LocalDate
        if ( rowContent instanceof LocalDate )
        {
            try
            {
                return ((LocalDate) rowContent).isEqual( LocalDate.parse( code ) );
            }
            catch ( DateTimeParseException e )
            {
                log.warn( String.format( "code %s is not LocalDate", code ), e.getMessage() );
            }
        }

        // LocalDateTime
        if ( rowContent instanceof LocalDateTime )
        {
            try
            {
                return ((LocalDateTime) rowContent).isEqual( LocalDateTime.parse( code ) );
            }
            catch ( DateTimeParseException e )
            {
                log.warn( String.format( "code %s is not LocalDateTime", code ), e.getMessage() );
            }
        }

        return false;
    }

    /**
     * Returns the options specified as element "filter" (option code), ie.:
     * Zj7UnCAulEk.K6uUAvq500H:IN:A03;B01, where "A03;B01" are the options
     * codes.
     *
     * The codes are split by the token ";" and the respective {@link Option}
     * objects are returned.
     *
     * @param item the {@link QueryItem}.
     * @return a list of options found in the filter.
     */
    private static List<Option> getItemOptionsForFilter( QueryItem item )
    {
        List<Option> options = new ArrayList<>();

        for ( Option option : item.getOptionSet().getOptions() )
        {
            for ( QueryFilter filter : item.getFilters() )
            {
                List<String> filterSplit = Arrays
                    .stream( trimToEmpty( filter.getFilter() ).split( ";" ) )
                    .collect( toList() );
                if ( filterSplit.contains( trimToEmpty( option.getCode() ) ) )
                {
                    options.add( option );
                }
            }
        }

        return options;
    }
}
