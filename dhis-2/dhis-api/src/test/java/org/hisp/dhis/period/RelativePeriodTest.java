/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.period;

import static org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.calendar.impl.Iso8601Calendar;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.mock.MockI18nFormat;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class RelativePeriodTest
{
    private static final I18nFormat I18N_FORMAT = new MockI18nFormat();

    private static Date getDate( int year, int month, int day )
    {
        return new DateTimeUnit( year, month, day, true ).toJdkDate();
    }

    @Test
    public void testGetThisToday()
    {
        RelativePeriods periods = new RelativePeriods().setThisDay( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 1 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetYesterday()
    {
        RelativePeriods periods = new RelativePeriods().setYesterday( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 2 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 1 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetLast3Days()
    {
        RelativePeriods periods = new RelativePeriods().setLast3Days( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 4 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 3, relatives.size() );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 1 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 2 ), getDate( 2001, 7, 2 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 3 ), getDate( 2001, 7, 3 ) ),
            relatives.get( 2 ) );
    }

    @Test
    public void testGetLast7Days()
    {
        RelativePeriods periods = new RelativePeriods().setLast7Days( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 8 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 7, relatives.size() );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 1 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 2 ), getDate( 2001, 7, 2 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 3 ), getDate( 2001, 7, 3 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 4 ), getDate( 2001, 7, 4 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 5 ), getDate( 2001, 7, 5 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 6 ), getDate( 2001, 7, 6 ) ),
            relatives.get( 5 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 7 ), getDate( 2001, 7, 7 ) ),
            relatives.get( 6 ) );
    }

    @Test
    public void testGetLast14Days()
    {
        RelativePeriods periods = new RelativePeriods().setLast14Days( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 14, relatives.size() );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 1 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 2 ), getDate( 2001, 7, 2 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 3 ), getDate( 2001, 7, 3 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 4 ), getDate( 2001, 7, 4 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 5 ), getDate( 2001, 7, 5 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 6 ), getDate( 2001, 7, 6 ) ),
            relatives.get( 5 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 7 ), getDate( 2001, 7, 7 ) ),
            relatives.get( 6 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 8 ), getDate( 2001, 7, 8 ) ),
            relatives.get( 7 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 9 ), getDate( 2001, 7, 9 ) ),
            relatives.get( 8 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 10 ), getDate( 2001, 7, 10 ) ),
            relatives.get( 9 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 11 ), getDate( 2001, 7, 11 ) ),
            relatives.get( 10 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 12 ), getDate( 2001, 7, 12 ) ),
            relatives.get( 11 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 13 ), getDate( 2001, 7, 13 ) ),
            relatives.get( 12 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 14 ), getDate( 2001, 7, 14 ) ),
            relatives.get( 13 ) );
    }

    @Test
    public void testGetLast30Days()
    {
        RelativePeriods periods = new RelativePeriods().setLast30Days( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 30, relatives.size() );

        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 15 ), getDate( 2001, 6, 15 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 16 ), getDate( 2001, 6, 16 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 17 ), getDate( 2001, 6, 17 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 18 ), getDate( 2001, 6, 18 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 19 ), getDate( 2001, 6, 19 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 20 ), getDate( 2001, 6, 20 ) ),
            relatives.get( 5 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 21 ), getDate( 2001, 6, 21 ) ),
            relatives.get( 6 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 22 ), getDate( 2001, 6, 22 ) ),
            relatives.get( 7 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 23 ), getDate( 2001, 6, 23 ) ),
            relatives.get( 8 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 24 ), getDate( 2001, 6, 24 ) ),
            relatives.get( 9 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 25 ), getDate( 2001, 6, 25 ) ),
            relatives.get( 10 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 26 ), getDate( 2001, 6, 26 ) ),
            relatives.get( 11 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 27 ), getDate( 2001, 6, 27 ) ),
            relatives.get( 12 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 28 ), getDate( 2001, 6, 28 ) ),
            relatives.get( 13 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 29 ), getDate( 2001, 6, 29 ) ),
            relatives.get( 14 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 6, 30 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 15 ) );

        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 1 ) ),
            relatives.get( 16 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 2 ), getDate( 2001, 7, 2 ) ),
            relatives.get( 17 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 3 ), getDate( 2001, 7, 3 ) ),
            relatives.get( 18 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 4 ), getDate( 2001, 7, 4 ) ),
            relatives.get( 19 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 5 ), getDate( 2001, 7, 5 ) ),
            relatives.get( 20 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 6 ), getDate( 2001, 7, 6 ) ),
            relatives.get( 21 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 7 ), getDate( 2001, 7, 7 ) ),
            relatives.get( 22 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 8 ), getDate( 2001, 7, 8 ) ),
            relatives.get( 23 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 9 ), getDate( 2001, 7, 9 ) ),
            relatives.get( 24 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 10 ), getDate( 2001, 7, 10 ) ),
            relatives.get( 25 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 11 ), getDate( 2001, 7, 11 ) ),
            relatives.get( 26 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 12 ), getDate( 2001, 7, 12 ) ),
            relatives.get( 27 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 13 ), getDate( 2001, 7, 13 ) ),
            relatives.get( 28 ) );
        assertEquals( new Period( new DailyPeriodType(), getDate( 2001, 7, 14 ), getDate( 2001, 7, 14 ) ),
            relatives.get( 29 ) );
    }

    @Test
    public void testGetThisMonth()
    {
        RelativePeriods periods = new RelativePeriods().setThisMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 31 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetLastMonth()
    {
        RelativePeriods periods = new RelativePeriods().setLastMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetThisBiWeek()
    {
        RelativePeriods periods = new RelativePeriods().setThisBiWeek( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 1, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new BiWeeklyPeriodType(), getDate( 2001, 1, 15 ), getDate( 2001, 1, 28 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetLastBiWeek()
    {
        RelativePeriods periods = new RelativePeriods().setLastBiWeek( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 1, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new BiWeeklyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 1, 14 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetThisBiMonth()
    {
        RelativePeriods periods = new RelativePeriods().setThisBimonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 8, 31 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetLastBiMonth()
    {
        RelativePeriods periods = new RelativePeriods().setLastBimonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetThisQuarter()
    {
        RelativePeriods periods = new RelativePeriods().setThisQuarter( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 9, 30 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetLastQuarter()
    {
        RelativePeriods periods = new RelativePeriods().setLastQuarter( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetThisSixMonth()
    {
        RelativePeriods periods = new RelativePeriods().setThisSixMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 12, 31 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testLastSixMonth()
    {
        RelativePeriods periods = new RelativePeriods().setLastSixMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetLast12Months()
    {
        RelativePeriods periods = new RelativePeriods().setLast12Months( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 1, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 12, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 2, 1 ), getDate( 2000, 2, 29 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 4, 1 ), getDate( 2000, 4, 30 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 5, 1 ), getDate( 2000, 5, 31 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 6, 1 ), getDate( 2000, 6, 30 ) ),
            relatives.get( 5 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 7, 31 ) ),
            relatives.get( 6 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 8, 1 ), getDate( 2000, 8, 31 ) ),
            relatives.get( 7 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 9, 1 ), getDate( 2000, 9, 30 ) ),
            relatives.get( 8 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 10, 1 ), getDate( 2000, 10, 31 ) ),
            relatives.get( 9 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 11, 1 ), getDate( 2000, 11, 30 ) ),
            relatives.get( 10 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 12, 1 ), getDate( 2000, 12, 31 ) ),
            relatives.get( 11 ) );
    }

    @Test
    public void testGetLast3Months()
    {
        RelativePeriods periods = new RelativePeriods().setLast3Months( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 3, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 2 ) );
    }

    @Test
    public void testGetLast6Months()
    {
        RelativePeriods periods = new RelativePeriods().setLast6Months( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 6, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 1, 31 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 2, 1 ), getDate( 2001, 2, 28 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 3, 1 ), getDate( 2001, 3, 31 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 5 ) );
    }

    @Test
    public void testGetLast4Quarters()
    {
        RelativePeriods relativePeriods = new RelativePeriods().setLast4Quarters( true );

        List<Period> relatives = relativePeriods.getRelativePeriods( getDate( 2001, 1, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 4, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 3, 31 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 4, 1 ), getDate( 2000, 6, 30 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 9, 30 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 10, 1 ), getDate( 2000, 12, 31 ) ),
            relatives.get( 3 ) );
    }

    @Test
    public void testGetLast4BiWeeks()
    {
        RelativePeriods relativePeriods = new RelativePeriods().setLast4BiWeeks( true );
        relativePeriods.setLastBiWeek( false );
        relativePeriods.setThisBiWeek( false );

        List<Period> relatives = relativePeriods.getRelativePeriods( getDate( 2002, 1, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 4, relatives.size() );
        assertEquals( new Period( new BiWeeklyPeriodType(), getDate( 2001, 11, 5 ), getDate( 2001, 11, 18 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new BiWeeklyPeriodType(), getDate( 2001, 11, 19 ), getDate( 2001, 12, 2 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new BiWeeklyPeriodType(), getDate( 2001, 12, 3 ), getDate( 2001, 12, 16 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new BiWeeklyPeriodType(), getDate( 2001, 12, 17 ), getDate( 2001, 12, 30 ) ),
            relatives.get( 3 ) );
    }

    @Test
    public void testGetLast2SixMonths()
    {
        List<Period> relatives = new RelativePeriods().setLast2SixMonths( true ).getRelativePeriods(
            getDate( 2001, 1, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 2, relatives.size() );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 6, 30 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 12, 31 ) ),
            relatives.get( 1 ) );
    }

    @Test
    public void testGetLast5Years()
    {
        List<Period> relatives = new RelativePeriods().setLast5Years( true ).getRelativePeriods( getDate( 2001, 1, 1 ),
            I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 5, relatives.size() );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1996, 1, 1 ), getDate( 1996, 12, 31 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1997, 1, 1 ), getDate( 1997, 12, 31 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1998, 1, 1 ), getDate( 1998, 12, 31 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1999, 1, 1 ), getDate( 1999, 12, 31 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 12, 31 ) ),
            relatives.get( 4 ) );
    }

    @Test
    public void testGetLast10Years()
    {
        List<Period> relatives = new RelativePeriods().setLast10Years( true ).getRelativePeriods( getDate( 2001, 1, 1 ),
            I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 10, relatives.size() );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1991, 1, 1 ), getDate( 1991, 12, 31 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1992, 1, 1 ), getDate( 1992, 12, 31 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1993, 1, 1 ), getDate( 1993, 12, 31 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1994, 1, 1 ), getDate( 1994, 12, 31 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1995, 1, 1 ), getDate( 1995, 12, 31 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1996, 1, 1 ), getDate( 1996, 12, 31 ) ),
            relatives.get( 5 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1997, 1, 1 ), getDate( 1997, 12, 31 ) ),
            relatives.get( 6 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1998, 1, 1 ), getDate( 1998, 12, 31 ) ),
            relatives.get( 7 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1999, 1, 1 ), getDate( 1999, 12, 31 ) ),
            relatives.get( 8 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 12, 31 ) ),
            relatives.get( 9 ) );
    }

    @Test
    public void testGetLast10FinancialYears()
    {
        List<Period> relatives = new RelativePeriods().setLast10FinancialYears( true ).getRelativePeriods(
            getDate( 2001, 1, 1 ),
            I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        int year = Iso8601Calendar.getInstance().today().getYear()
            - (Iso8601Calendar.getInstance().today().getMonth() > 10 ? 10 : 11);
        assertEquals( 10, relatives.size() );
        for ( int i = 0; i < 10; i++ )
        {
            assertEquals(
                new Period( new FinancialOctoberPeriodType(), getDate( year, 10, 1 ), getDate( ++year, 9, 30 ) ),
                relatives.get( i ) );
        }
    }

    @Test
    public void testGetMonthsThisYear()
    {
        List<Period> relatives = new RelativePeriods().setMonthsThisYear( true ).getRelativePeriods(
            getDate( 2001, 4, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 12, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 1, 31 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 2, 1 ), getDate( 2001, 2, 28 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 3, 1 ), getDate( 2001, 3, 31 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 5 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 31 ) ),
            relatives.get( 6 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 8, 1 ), getDate( 2001, 8, 31 ) ),
            relatives.get( 7 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 9, 1 ), getDate( 2001, 9, 30 ) ),
            relatives.get( 8 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 10, 1 ), getDate( 2001, 10, 31 ) ),
            relatives.get( 9 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 11, 1 ), getDate( 2001, 11, 30 ) ),
            relatives.get( 10 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 12, 1 ), getDate( 2001, 12, 31 ) ),
            relatives.get( 11 ) );
    }

    @Test
    public void testGetBiMonthsThisYear()
    {
        List<Period> relatives = new RelativePeriods().setBiMonthsThisYear( true ).getRelativePeriods(
            getDate( 2001, 4, 1 ), I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 6, relatives.size() );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 2, 28 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 3, 1 ), getDate( 2001, 4, 30 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 8, 31 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 9, 1 ), getDate( 2001, 10, 31 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 11, 1 ), getDate( 2001, 12, 31 ) ),
            relatives.get( 5 ) );
    }

    @Test
    public void testGetLastWeek()
    {
        List<Period> relatives = new RelativePeriods().setLastWeek( true ).getRelativePeriods( getDate( 2012, 1, 20 ),
            I18N_FORMAT, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2012, 1, 9 ), getDate( 2012, 1, 15 ) ),
            relatives.get( 0 ) );
    }

    @Test
    public void testGetLast4Weeks()
    {
        List<Period> relatives = new RelativePeriods().setLast4Weeks( true ).getRelativePeriods( getDate( 2010, 5, 4 ),
            null, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 4, relatives.size() );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 5 ), getDate( 2010, 4, 11 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 12 ), getDate( 2010, 4, 18 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 19 ), getDate( 2010, 4, 25 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 26 ), getDate( 2010, 5, 2 ) ),
            relatives.get( 3 ) );
    }

    @Test
    public void testGetLast12Weeks()
    {
        List<Period> relatives = new RelativePeriods().setLast12Weeks( true ).getRelativePeriods( getDate( 2010, 5, 4 ),
            null, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 12, relatives.size() );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 2, 8 ), getDate( 2010, 2, 14 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 2, 15 ), getDate( 2010, 2, 21 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 2, 22 ), getDate( 2010, 2, 28 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 1 ), getDate( 2010, 3, 7 ) ),
            relatives.get( 3 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 8 ), getDate( 2010, 3, 14 ) ),
            relatives.get( 4 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 15 ), getDate( 2010, 3, 21 ) ),
            relatives.get( 5 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 22 ), getDate( 2010, 3, 28 ) ),
            relatives.get( 6 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 29 ), getDate( 2010, 4, 4 ) ),
            relatives.get( 7 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 5 ), getDate( 2010, 4, 11 ) ),
            relatives.get( 8 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 12 ), getDate( 2010, 4, 18 ) ),
            relatives.get( 9 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 19 ), getDate( 2010, 4, 25 ) ),
            relatives.get( 10 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 26 ), getDate( 2010, 5, 2 ) ),
            relatives.get( 11 ) );
    }

    @Test
    public void testGetQuartersThisYear()
    {
        List<Period> relatives = new RelativePeriods().setQuartersThisYear( true ).getRelativePeriods(
            getDate( 2001, 4, 1 ), null, false,
            FINANCIAL_YEAR_OCTOBER );

        assertEquals( 4, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 3, 31 ) ),
            relatives.get( 0 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 6, 30 ) ),
            relatives.get( 1 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 9, 30 ) ),
            relatives.get( 2 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 10, 1 ), getDate( 2001, 12, 31 ) ),
            relatives.get( 3 ) );
    }

    @Test
    public void testGetRelativePeriods()
    {
        List<Period> relatives = new RelativePeriods().setLast12Months( true ).getRelativePeriods();

        assertEquals( 12, relatives.size() );

        relatives = new RelativePeriods().setLast4Quarters( true ).getRelativePeriods( I18N_FORMAT, true );

        assertEquals( 4, relatives.size() );
    }

    @Test
    public void testGetRelativePeriodsFromEnumA()
    {
        List<Period> periods = RelativePeriods.getRelativePeriodsFromEnum( RelativePeriodEnum.THIS_YEAR,
            getDate( 2020, 10, 15 ) );

        assertEquals( 1, periods.size() );
        assertEquals( new YearlyPeriodType(), periods.get( 0 ).getPeriodType() );
        assertEquals( getDate( 2020, 1, 1 ), periods.get( 0 ).getStartDate() );
    }

    @Test
    public void testGetRelativePeriodsFromEnumB()
    {
        List<Period> periods = RelativePeriods.getRelativePeriodsFromEnum( RelativePeriodEnum.THIS_QUARTER,
            getDate( 2020, 1, 15 ) );

        assertEquals( 1, periods.size() );
        assertEquals( new QuarterlyPeriodType(), periods.get( 0 ).getPeriodType() );
        assertEquals( getDate( 2020, 1, 1 ), periods.get( 0 ).getStartDate() );
    }

    @Test
    public void testEnumContains()
    {
        assertTrue( RelativePeriodEnum.contains( "LAST_30_DAYS" ) );
        assertTrue( RelativePeriodEnum.contains( "THIS_YEAR" ) );

        assertFalse( RelativePeriodEnum.contains( "LAST_CHRISTMAS" ) );
        assertFalse( RelativePeriodEnum.contains( "LAST_VACATION" ) );
    }
}
