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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.EventReportDimensionalItem;
import org.hisp.dhis.analytics.MetadataItem;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;

/**
 * @author Henning Haakonsen
 */
public class EventAnalyticsUtils
{
    /**
     * Get all combinations from map. Fill the result into list.
     * 
     * @param map the map with all values
     * @param list the resulting list
     */
    private static void getCombinations( Map<String, List<EventReportDimensionalItem>> map,
        List<Map<String, EventReportDimensionalItem>> list )
    {
        recurse( map, new LinkedList<>( map.keySet() ).listIterator(), new TreeMap<>(), list );
    }

    /**
     * A recursive method which finds all permutations of the elements in map.
     *
     * @param map the map with all values
     * @param iter iterator with keys
     * @param cur the current map
     * @param list the resulting list
     */
    public static void recurse( Map<String, List<EventReportDimensionalItem>> map, ListIterator<String> iter,
        TreeMap<String, EventReportDimensionalItem> cur, List<Map<String, EventReportDimensionalItem>> list )
    {
        if ( !iter.hasNext() )
        {
            Map<String, EventReportDimensionalItem> entry = new HashMap<>();

            for ( String key : cur.keySet() )
            {
                entry.put( key, cur.get( key ) );
            }

            list.add( entry );
        }
        else
        {
            String key = iter.next();
            List<EventReportDimensionalItem> set = map.get( key );

            for ( EventReportDimensionalItem value : set )
            {
                cur.put( key, value );
                recurse( map, iter, cur, list );
                cur.remove( key );
            }

            iter.previous();
        }
    }

    /**
     *  Get all permutations for event report dimensions.
     *
     * @param dataOptionMap the map to generate permutations from
     * @return a list of a map with a permutations
     */
    public static List<Map<String, EventReportDimensionalItem>> generateEventDataPermutations(
        Map<String, List<EventReportDimensionalItem>> dataOptionMap )
    {
        List<Map<String, EventReportDimensionalItem>> list = new LinkedList<>();
        getCombinations( dataOptionMap, list );
        return list;
    }

    /**
     * Get event data mapping for values.
     * 
     * @param grid the grid to collect data from
     * @return map with key and values
     */
    public static Map<String, Object> getAggregatedEventDataMapping( Grid grid )
    {
        Map<String, Object> map = new HashMap<>();

        int metaCols = grid.getWidth() - 1;
        int valueIndex = grid.getWidth() - 1;

        for ( List<Object> row : grid.getRows() )
        {
            List<String> ids = new ArrayList<>();

            for ( int index = 0; index < metaCols; index++ )
            {
                Object id = row.get( index );

                if ( id != null )
                {
                    ids.add( (String) row.get( index ) );
                }
            }

            Collections.sort( ids );

            String key = StringUtils.join( ids, DIMENSION_SEP );
            Object value = row.get( valueIndex );

            map.put( key, value );
        }

        return map;
    }

    /**
     * Send in a list of {@link EventReportDimensionalItem} and add properties from {@link EventDataItem} parameter.
     *
     * @param eventDataItem object to get properties from
     * @param objects the list with objects. We are adding objects to this list as well.
     * @param grid the grid from the event analytics request
     * @param dimension the dimension we are looking at
     * @throws Exception throws exception if the given dimension is invalid
     */
    @SuppressWarnings( "unchecked" )
    public static void addEventReportDimensionalItems( EventDataItem eventDataItem, List<EventReportDimensionalItem> objects, Grid grid, String dimension )
        throws Exception
    {
        if ( eventDataItem.isInvalid() )
        {
            throw new Exception( "Supplied data dimension '" + dimension + "' is invalid" );
        }

        String parentUid = eventDataItem.getParentUid();

        if ( eventDataItem.getValueType() == ValueType.BOOLEAN )
        {
            Option t = new Option();
            t.setCode( "1" );
            t.setName( "Yes" );

            Option f = new Option();
            f.setCode( "0" );
            f.setName( "No" );

            objects.add( new EventReportDimensionalItem( t, parentUid ) );
            objects.add( new EventReportDimensionalItem( f, parentUid ) );
        }

        if ( eventDataItem.hasOptionSet() )
        {
            for ( Option option : eventDataItem.getOptionSet().getOptions() )
            {
                objects.add( new EventReportDimensionalItem( option, parentUid ) );
            }
        }

        if ( eventDataItem.hasLegendSet() )
        {
            List<String> legendOptions = (List<String>) ((Map<String, Object>) grid.getMetaData()
                .get( DIMENSIONS.getKey() )).get( dimension );

            if ( legendOptions.isEmpty() )
            {

                List<Legend> legends = eventDataItem.getLegendSet().getSortedLegends();

                for ( Legend legend : legends )
                {
                    for ( int i = legend.getStartValue().intValue(); i < legend.getEndValue(); i++ )
                    {
                        objects.add(
                            new EventReportDimensionalItem( new Option( String.valueOf( i ), String.valueOf( i ) ),
                                parentUid ) );
                    }
                }
            }
            else
            {
                for ( String legend : legendOptions )
                {
                    MetadataItem metadataItem = (MetadataItem) ((Map<String, Object>) grid.getMetaData()
                        .get( ITEMS.getKey() )).get( legend );

                    objects.add(
                        new EventReportDimensionalItem( new Option( metadataItem.getName(), legend ), parentUid ) );
                }
            }
        }
    }

    public static void addValues( List<List<String>> ids, Grid grid, Grid outputGrid )
    {
        Map<String, Object> valueMap = getAggregatedEventDataMapping( grid );

        boolean hasValues = false;
        for ( List<String> idList : ids )
        {
            Collections.sort( idList );

            String key = StringUtils.join( idList, DIMENSION_SEP );
            Object value = valueMap.get( key );
            hasValues = hasValues || value != null;

            outputGrid.addValue( value );
        }

        if ( !hasValues )
        {
            outputGrid.removeCurrentWriteRow();
        }
    }
}
