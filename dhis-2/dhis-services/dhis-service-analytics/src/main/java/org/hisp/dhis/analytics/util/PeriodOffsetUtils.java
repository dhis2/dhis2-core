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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.lang3.ArrayUtils.remove;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.hasPeriod;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.commons.collection.CollectionUtils.addAllUnique;
import static org.hisp.dhis.commons.collection.CollectionUtils.addUnique;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.period.BiWeeklyAbstractPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyAbstractPeriodType;

/**
 * @author Luciano Fiandesio
 * @author Jim Grace
 */
public class PeriodOffsetUtils
{
    private PeriodOffsetUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }

    /**
     * If the query parameters contain any dimensional item objects with a
     * periodOffset, return query parameters with the extra periods added.
     * <p>
     * If any dimensional item objects have a yearToDate modifier, add any extra
     * periods required from the start of the year.
     * <p>
     * If any dimensional item objects have both periodOffset and yearToDate,
     * include all the year-to-date periods for the offset periods as well.
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
            QueryModifiers mods = item.getQueryMods();

            if ( mods != null )
            {
                if ( mods.getPeriodOffset() != 0 )
                {
                    // Add periodOffsets only for parameter periods
                    addAllUnique( periods, shiftPeriods( params.getPeriods(), mods.getPeriodOffset() ) );
                }

                if ( mods.isYearToDate() )
                {
                    // Add yearToDate for all periods including periodOffsets
                    addAllUnique( periods, yearToDatePeriods( periods ) );
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
        return period.getPeriodType().getShiftedPeriod( period, periodOffset );
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

    /**
     * Does a {@link DimensionalItemObject} have the year to date property?
     *
     * @param dimensionalItem the {@link DimensionalItemObject}.
     * @return true if year to date, otherwise false.
     */
    public static boolean isYearToDate( DimensionalItemObject dimensionalItem )
    {
        return dimensionalItem.getQueryMods() != null && dimensionalItem.getQueryMods().isYearToDate();
    }

    /**
     * Build a list of year-to-date rows. For each value that adds to a
     * year-to-date result, save that value and add it to other such values.
     *
     * @param periodIndex the current grid row period index.
     * @param valueIndex the current grid row value index.
     * @param row the current grid row.
     * @param yearToDateItems the yearToDate items this row contributes to.
     * @param basePeriods the periods wanted by the user.
     * @param yearToDateRows the year-to-date rows we are building.
     */
    public static void buildYearToDateRows( int periodIndex, int valueIndex, List<Object> row,
        List<DimensionalItemObject> yearToDateItems, List<DimensionalItemObject> basePeriods,
        Map<String, List<Object>> yearToDateRows )
    {
        if ( !hasPeriod( row, periodIndex ) )
        {
            return;
        }

        List<Period> targetPeriods = getTargetPeriodsFromYearToDateItems( yearToDateItems, basePeriods );

        String rowPeriod = (String) row.get( periodIndex );

        for ( Period targetPeriod : targetPeriods )
        {
            Set<String> inputPeriods = yearToDatePeriods( targetPeriod ).stream()
                .map( Period::getIsoDate )
                .collect( toUnmodifiableSet() );

            if ( inputPeriods.contains( rowPeriod ) )
            {
                addYearToDateRow( periodIndex, valueIndex, row, targetPeriod, yearToDateRows );
            }
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * From a list of yearToDate items, create a list of target periods for
     * which yearToDate totals are needed. If the items have periodOffset 0,
     * then the target periods will be the same as the base periods (the periods
     * supplied by the parameters). If the items have non-zero periodOffsets,
     * the target periods may be different from the base periods.
     * <p>
     * Because these are yearToDate items, we know that they have queryMods.
     */
    private static List<Period> getTargetPeriodsFromYearToDateItems( List<DimensionalItemObject> yearToDateItems,
        List<DimensionalItemObject> basePeriods )
    {
        List<Period> targetPeriods = new ArrayList<>();

        for ( DimensionalItemObject item : yearToDateItems )
        {
            addAllUnique( targetPeriods, shiftPeriods( basePeriods, item.getQueryMods().getPeriodOffset() ) );
        }

        return targetPeriods;
    }

    /**
     * Shifts a list of periods according to a period offset. The list order is
     * preserved.
     */
    private static List<Period> shiftPeriods( List<DimensionalItemObject> periods, int periodOffset )
    {
        List<Period> offsetPeriods = new ArrayList<>();

        for ( DimensionalItemObject period : periods )
        {
            addUnique( offsetPeriods, shiftPeriod( (Period) period, periodOffset ) );
        }

        return offsetPeriods;
    }

    /**
     * Finds all periods needed for a year-to-date period. Periods are added in
     * list order.
     */
    private static List<Period> yearToDatePeriods( List<DimensionalItemObject> periods )
    {
        List<Period> ytdPeriods = new ArrayList<>();

        for ( DimensionalItemObject period : periods )
        {
            addAllUnique( ytdPeriods, yearToDatePeriods( (Period) period ) );
        }

        return ytdPeriods;
    }

    /**
     * Generates the periods needed for one year-to-date period.
     */
    private static List<Period> yearToDatePeriods( Period period )
    {
        int reportingYear = getReportingYear( period );

        List<Period> periods = new ArrayList<>();

        do
        {
            periods.add( period );
            period = period.getPeriodType().getPreviousPeriod( period );
        }
        while ( getReportingYear( period ) == reportingYear );

        return periods;
    }

    /**
     * Adds or updates a year-to-date row we are building.
     */
    private static void addYearToDateRow( int periodIndex, int valueIndex, List<Object> row, Period targetPeriod,
        Map<String, List<Object>> yearToDateRows )
    {
        List<Object> targetRow = new ArrayList<>( row );

        targetRow.set( periodIndex, targetPeriod.getIsoDate() );

        String key = join( remove( targetRow.toArray( new Object[0] ), valueIndex ), DIMENSION_SEP );

        List<Object> existingRow = yearToDateRows.get( key );

        if ( existingRow != null )
        {
            double existingValue = ((Number) existingRow.get( valueIndex )).doubleValue();
            double newValue = ((Number) targetRow.get( valueIndex )).doubleValue();
            existingRow.set( valueIndex, existingValue + newValue );
        }
        else
        {
            yearToDateRows.put( key, targetRow );
        }
    }

    /**
     * Gets the analytics reporting year for a period.
     * <p>
     * A weekly or biweekly period starting on or after December 29 has three or
     * fewer days in the current year, and so is considered to be the first week
     * reported in the following year. (This doesn't make sense for biweekly
     * periods, but it is how DHIS2 currently operates.)
     * <p>
     * A biweekly period starting on or after December 22 has
     * <p>
     * For all other periods, the period start year is returned.
     */
    private static int getReportingYear( Period period )
    {
        DateTimeUnit periodStart = DateTimeUnit.fromJdkDate( period.getStartDate() );

        if ( (period.getPeriodType() instanceof WeeklyAbstractPeriodType
            || period.getPeriodType() instanceof BiWeeklyAbstractPeriodType)
            && periodStart.getMonth() == 12 && periodStart.getDay() >= 29 )
        {
            return periodStart.getYear() + 1;
        }

        return periodStart.getYear();
    }
}
