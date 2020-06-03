package org.hisp.dhis.analytics.offset;

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
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;

public class PeriodOffsetUtils
{
    /**
     * Creates an associative Map between Period Types (e.g. Month, Quarter, etc.) and
     * Periods extracted from a {@see DataQueryParams} object.
     * 
     * Each map value may also contain periods that are derived from Period
     * offsets applied to elements from the "data" dimension of the {@see DataQueryParams}
     *
     * @param params a DataQueryParams object
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
     * 
     * Example:
     * 
     * Period: 202001 , Offset: 1 -> Period: 202002
     * Period: 2020 , Offset: -1 -> Period: 2019
     * 
     * @param period a Period
     * @param periodOffset a positive or negative integer
     * @return A Period
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
     *
     * @param map
     * @param periodOffset
     * @return
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
