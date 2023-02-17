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

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.analytics.util.ReportRatesHelper.getCalculatedTarget;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReportRatesHelper.
 *
 * @author maikel arabori
 */
class ReportRatesHelperTest
{
    @Test
    void testGetCalculatedTargetWhenDataSetIsDailyAndHasPeriodInDimension()
    {
        Double theTarget = 10d;
        List<DimensionalItemObject> theFilterPeriods = asList( stubPeriod() );
        PeriodType theDailyPeriodType = DailyPeriodType.getByNameIgnoreCase( "Daily" );
        // Relates to "aDailyDataPeriodRow" objects
        int anyPositivePeriodInDimensionIndex = 1;
        List<String> aDailyDataPeriodRow = asList( "TuL8IOPzpHh", "20210415" );
        int anyTimeUnits = 2;
        PeriodType aDataSetDailyPeriodType = DailyPeriodType.getByNameIgnoreCase( "Daily" );

        Double actualResult = getCalculatedTarget( anyPositivePeriodInDimensionIndex, anyTimeUnits,
            aDailyDataPeriodRow, theTarget, theDailyPeriodType, aDataSetDailyPeriodType, theFilterPeriods );

        assertThat( actualResult, is( 20.0d ) );
    }

    @Test
    void testGetCalculatedTargetWhenDataSetIsDailyAndHasPeriodInFilter()
    {
        Double theTarget = 10d;
        List<DimensionalItemObject> theFilterPeriods = asList( stubPeriod() );
        PeriodType theDailyPeriodType = DailyPeriodType.getByNameIgnoreCase( "Daily" );
        int anyNegativePeriodInDimensionIndex = -1;
        int anyTimeUnits = 1;
        List<String> anyDataRow = asList( "TuL8IOPzpHh" );
        PeriodType aDataSetDailyPeriodType = DailyPeriodType.getByNameIgnoreCase( "Daily" );

        Double actualResult = getCalculatedTarget( anyNegativePeriodInDimensionIndex, anyTimeUnits, anyDataRow,
            theTarget, theDailyPeriodType, aDataSetDailyPeriodType, theFilterPeriods );

        assertThat( actualResult, is( 10.0d ) );
    }

    @Test
    void testGetCalculatedTargetWhenDataSetIsDailyAndHasPeriodInFilterAndMultiplePeriods()
    {
        Double theTarget = 10d;
        List<DimensionalItemObject> multipleFilterPeriods = asList( stubPeriod(), stubPeriod(), stubPeriod() );
        PeriodType theDailyPeriodType = DailyPeriodType.getByNameIgnoreCase( "Daily" );
        int anyNegativePeriodInDimensionIndex = -1;
        int anyTimeUnits = 2;
        List<String> anyDataRow = asList( "TuL8IOPzpHh" );
        PeriodType aDataSetDailyPeriodType = DailyPeriodType.getByNameIgnoreCase( "Daily" );

        Double actualResult = getCalculatedTarget( anyNegativePeriodInDimensionIndex, anyTimeUnits, anyDataRow,
            theTarget, theDailyPeriodType, aDataSetDailyPeriodType, multipleFilterPeriods );

        assertThat( actualResult, is( 60.0d ) );
    }

    public Period stubPeriod()
    {
        Period p = new Period();
        p.setStartDate( new Date() );
        p.setEndDate( new Date() );
        p.setPeriodType( DailyPeriodType.getByNameIgnoreCase( "daily" ) );
        return p;
    }
}
