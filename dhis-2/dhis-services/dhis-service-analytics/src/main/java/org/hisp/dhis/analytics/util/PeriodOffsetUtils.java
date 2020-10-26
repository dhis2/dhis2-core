package org.hisp.dhis.analytics.util;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;

/**
 * @author Luciano Fiandesio
 */
public class PeriodOffsetUtils
{
    /**
     * Creates an associative Map between Period Types (e.g. Month, Quarter)
     * and Periods extracted from a {@link DataQueryParams} object.
     * <p>
     * Each map value may also contain periods that are derived from Period offsets
     * applied to elements from the "data" dimension of the {@link DataQueryParams}
     * The periods add because of the "periodOffset" directive, will have the
     * "shifted" property set to "true".
     *
     * @param params a DataQueryParams object.
     */
    public static ListMap<String, DimensionalItemObject> getPeriodTypePeriodMap( DataQueryParams params )
    {
        if ( params == null || params.getPeriods().isEmpty() )
        {
            return new ListMap<>();
        }

        ListMap<String, DimensionalItemObject> periodTypePeriodMap = PartitionUtils.getPeriodTypePeriodMap( params.getPeriods() );

        DimensionalObject dimension = params.getDimension( DATA_X_DIM_ID );

        if ( dimension == null )
        {
            return periodTypePeriodMap;
        }

        List<DimensionalItemObject> items = dimension.getItems();
        ListMap<String, DimensionalItemObject> shiftedMap = new ListMap<>();

        for ( DimensionalItemObject item : items )
        {
            if ( item.getPeriodOffset() != 0 )
            {
                shiftedMap.putAll( addPeriodOffset( periodTypePeriodMap, item.getPeriodOffset() ) );
            }
        }

        Set<DimensionalItemObject> dimensionalItemObjects = shiftedMap.uniqueValues();

        for ( DimensionalItemObject dimensionalItemObject : dimensionalItemObjects )
        {
            Period period = (Period) dimensionalItemObject;

            if ( !periodTypePeriodMap.containsValue( period.getPeriodType().getName(), dimensionalItemObject ) )
            {
                periodTypePeriodMap.putValue( period.getPeriodType().getName(), dimensionalItemObject );
            }
        }

        return periodTypePeriodMap;
    }

    /**
     * Shifts the given Period in the past or future based on the offset value.
     * <p>
     * Example:
     * <p>
     * Period: 202001 , Offset: 1 -> Period: 202002
     * Period: 2020 , Offset: -1 -> Period: 2019
     *
     * @param period a Period.
     * @param periodOffset a positive or negative integer.
     * @return A Period.
     */
    public static Period shiftPeriod( Period period, int periodOffset )
    {
        if ( periodOffset == 0 )
        {
            return period;
        }

        PeriodType periodType = period.getPeriodType();
        Period p;

        if ( periodOffset > 0 )
        {
            p = periodType.getNextPeriod( period, periodOffset );
        }
        else
        {
            p = periodType.getPreviousPeriod( period, periodOffset );
        }

        p.setShifted( true );
        return p;
    }

    /**
     * Remove Periods from a {@link DataQueryParams} object if these periods have
     * been added because of an "periodOffset" directive and the DataElement have no
     * offset specified. This can happen in case of an Indicator, where a numerator
     * formula is using an offset, and the denominator formula is not.
     *
     * @param params a {@link DataQueryParams} object
     * @return a {@link DataQueryParams} object
     */
    public static DataQueryParams removeOffsetPeriodsIfNotNeeded( DataQueryParams params )
    {
        final List<DimensionalItemObject> dimensionalItemObjects = params.getDataElements();

        final boolean hasOffset = dimensionalItemObjects.stream().filter( dio -> dio.getDimensionItemType() != null )
            .filter( dio -> dio.getDimensionItemType().equals( DimensionItemType.DATA_ELEMENT ) )
            .anyMatch( dio -> dio.getPeriodOffset() != 0 );

        if ( !hasOffset )
        {
            final List<DimensionalItemObject> nonShiftedPeriods = params.getPeriods().stream()
                .filter( dio -> ( !( (Period) dio).isShifted() ) )
                .collect( Collectors.toList() );

            return DataQueryParams.newBuilder( params )
                .withPeriods( params.getDimension( PERIOD_DIM_ID ).getDimensionName(), nonShiftedPeriods )
                .build();
        }
        return params;
    }

    /**
     * Given a Analytics {@link Grid}, this methods tries to extract the row from the Grid that matches the given
     * {@link DimensionalItemObject} and offset period. If there is no match, null is returned.
     *
     * @param grid a {@link Grid} object
     * @param dimItem a DimensionalItemObject object
     * @param isoPeriod a Period, in ISO format (e.g. 202001 - for January 2020)
     * @param offset an offset value
     * @return a row from the Grid (as List of Object) or null
     */
    public static List<Object> getPeriodOffsetRow( Grid grid, DimensionalItemObject dimItem, String isoPeriod,
        int offset )
    {
        if ( grid == null || dimItem == null )
        {
            return null;
        }

        BiFunction<Integer, Integer, Integer> replaceIndexIfMissing = (Integer index, Integer defaultIndex )
                -> index == -1 ? defaultIndex : index;

        final int dataIndex = replaceIndexIfMissing.apply( grid.getIndexOfHeader( DATA_X_DIM_ID ), 0 );
        final int periodIndex = replaceIndexIfMissing.apply( grid.getIndexOfHeader( PERIOD_DIM_ID ), 1 );

        Period shifted = offset != 0 ? shiftPeriod( PeriodType.getPeriodFromIsoString( isoPeriod ), offset ) :
            PeriodType.getPeriodFromIsoString( isoPeriod );

        for ( List<Object> row : grid.getRows() )
        {
            final String rowUid = (String) row.get( dataIndex );
            final String rowPeriod = (String) row.get( periodIndex );

            if ( rowUid.equals( dimItem.getUid() ) && rowPeriod.equals( shifted.getIsoDate() ) )
            {
                return row;
            }
        }

        return null;
    }

    /**
     * Adds the given period offset to the given periods.
     *
     * @param map a mapping between period type and periods.
     * @param periodOffset the period offset.
     * @return a mapping of period type name and shifted periods.
     */
    private static ListMap<String, DimensionalItemObject> addPeriodOffset( ListMap<String, DimensionalItemObject> map,
        int periodOffset )
    {
        ListMap<String, DimensionalItemObject> periodTypeOffsetMap = new ListMap<>();
        Collection<DimensionalItemObject> dimensionalItemObjects = map.allValues();

        for ( DimensionalItemObject dimensionalItemObject : dimensionalItemObjects )
        {
            Period currentPeriod = (Period) dimensionalItemObject;
            Period shifted = shiftPeriod( currentPeriod, periodOffset );

            if ( !map.containsValue( currentPeriod.getPeriodType().getName(), shifted ) )
            {
                periodTypeOffsetMap.putValue( currentPeriod.getPeriodType().getName(), shifted );
            }
        }

        return periodTypeOffsetMap;
    }
}
