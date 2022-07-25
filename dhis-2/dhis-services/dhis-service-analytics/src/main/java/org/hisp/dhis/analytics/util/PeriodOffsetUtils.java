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
package org.hisp.dhis.analytics.util;

import static java.lang.Math.abs;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;

/**
 * @author Luciano Fiandesio
 * @author Jim Grace
 */
public class PeriodOffsetUtils
{
    /**
     * If the query parameters contain any dimensional item objects with a
     * periodOffset, return query parameters with the extra periods added.
     * <p>
     * Any added periods are added to the end of the list of query periods, for
     * the convenience of debugging, SQL query log reading, etc.
     *
     * @param params data query parameters
     * @return params with extra shifted periods added, if any
     */
    public static DataQueryParams addShiftedPeriods( DataQueryParams params )
    {
        DimensionalObject dimension = params.getDimension( DATA_X_DIM_ID );

        if ( dimension == null )
        {
            return params;
        }

        List<DimensionalItemObject> periods = new ArrayList<>( params.getPeriods() );

        for ( DimensionalItemObject item : dimension.getItems() )
        {
            if ( item.getQueryMods() != null && item.getQueryMods().getPeriodOffset() != 0 )
            {
                for ( DimensionalItemObject period : params.getPeriods() )
                {
                    Period shiftedPeriod = shiftPeriod( (Period) period, item.getQueryMods().getPeriodOffset() );

                    if ( !periods.contains( shiftedPeriod ) )
                    {
                        periods.add( shiftedPeriod );
                    }
                }
            }
        }

        if ( periods.equals( params.getPeriods() ) )
        {
            return params;
        }

        return DataQueryParams.newBuilder( params )
            .withPeriods( periods )
            .build();
    }

    /**
     * Shifts the given Period in the past or future based on the offset value.
     * <p>
     * Examples:
     * <p>
     * Period: 202001, Offset: 1 -> Period: 202002
     * <p>
     * Period: 2020, Offset: -1 -> Period: 2019
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
            p = periodType.getPreviousPeriod( period, abs( periodOffset ) );
        }

        return p;
    }

    /**
     * Given an Analytics {@link Grid} row, adjust the date in the row according
     * to the period offset.
     *
     * @param row the current grid row
     * @param periodIndex the current grid row period index.
     * @param offset an offset value
     * @return a new row with adjusted date
     */
    public static List<Object> getPeriodOffsetRow( List<Object> row, int periodIndex, int offset )
    {
        String isoPeriod = (String) row.get( periodIndex );
        Period shifted = shiftPeriod( PeriodType.getPeriodFromIsoString( isoPeriod ), -offset );

        List<Object> adjustedRow = new ArrayList<>( row );
        adjustedRow.set( periodIndex, shifted.getIsoDate() );

        return adjustedRow;
    }
}
