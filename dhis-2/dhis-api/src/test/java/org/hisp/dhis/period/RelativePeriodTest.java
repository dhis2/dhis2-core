package org.hisp.dhis.period;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.mock.MockI18nFormat;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

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
    public void testGetThisMonth()
    {
        RelativePeriods periods = new RelativePeriods().setThisMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 31 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetLastMonth()
    {
        RelativePeriods periods = new RelativePeriods().setLastMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetThisBiMonth()
    {
        RelativePeriods periods = new RelativePeriods().setThisBimonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 8, 31 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetLastBiMonth()
    {
        RelativePeriods periods = new RelativePeriods().setLastBimonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new BiMonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetThisQuarter()
    {
        RelativePeriods periods = new RelativePeriods().setThisQuarter( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 9, 30 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetLastQuarter()
    {
        RelativePeriods periods = new RelativePeriods().setLastQuarter( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetThisSixMonth()
    {
        RelativePeriods periods = new RelativePeriods().setThisSixMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 12, 31 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testLastSixMonth()
    {
        RelativePeriods periods = new RelativePeriods().setLastSixMonth( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetLast12MonthsRewinded()
    {
        RelativePeriods periods = new RelativePeriods().setLast12Months( true );

        List<Period> relatives = periods.getRewindedRelativePeriods( 1, getDate( 2001, 7, 15 ), I18N_FORMAT, false );

        assertEquals( 12, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 6, 1 ), getDate( 2000, 6, 30 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 7, 31 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 8, 1 ), getDate( 2000, 8, 31 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 9, 1 ), getDate( 2000, 9, 30 ) ), relatives.get( 3 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 10, 1 ), getDate( 2000, 10, 31 ) ), relatives.get( 4 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 11, 1 ), getDate( 2000, 11, 30 ) ), relatives.get( 5 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 12, 1 ), getDate( 2000, 12, 31 ) ), relatives.get( 6 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 1, 31 ) ), relatives.get( 7 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 2, 1 ), getDate( 2001, 2, 28 ) ), relatives.get( 8 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 3, 1 ), getDate( 2001, 3, 31 ) ), relatives.get( 9 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) ), relatives.get( 10 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) ), relatives.get( 11 ) );
    }

    @Test
    public void testGetLast12Months()
    {
        RelativePeriods periods = new RelativePeriods().setLast12Months( true );
            
        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 1, 1 ), I18N_FORMAT, false );

        assertEquals( 12, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 1, 31 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 2, 1 ), getDate( 2000, 2, 29 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 3, 1 ), getDate( 2000, 3, 31 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 4, 1 ), getDate( 2000, 4, 30 ) ), relatives.get( 3 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 5, 1 ), getDate( 2000, 5, 31 ) ), relatives.get( 4 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 6, 1 ), getDate( 2000, 6, 30 ) ), relatives.get( 5 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 7, 31 ) ), relatives.get( 6 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 8, 1 ), getDate( 2000, 8, 31 ) ), relatives.get( 7 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 9, 1 ), getDate( 2000, 9, 30 ) ), relatives.get( 8 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 10, 1 ), getDate( 2000, 10, 31 ) ), relatives.get( 9 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 11, 1 ), getDate( 2000, 11, 30 ) ), relatives.get( 10 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2000, 12, 1 ), getDate( 2000, 12, 31 ) ), relatives.get( 11 ) );
    }

    @Test
    public void testGetLast3Months()
    {
        RelativePeriods periods = new RelativePeriods().setLast3Months( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 1 ), I18N_FORMAT, false );

        assertEquals( 3, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 2 ) );
    }

    @Test
    public void testGetLast6Months()
    {
        RelativePeriods periods = new RelativePeriods().setLast6Months( true );

        List<Period> relatives = periods.getRelativePeriods( getDate( 2001, 7, 1 ), I18N_FORMAT, false );

        assertEquals( 6, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 1, 31 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 2, 1 ), getDate( 2001, 2, 28 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 3, 1 ), getDate( 2001, 3, 31 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) ), relatives.get( 3 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) ), relatives.get( 4 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 5 ) );
    }

    @Test
    public void testGetLast4Quarters()
    {
        RelativePeriods relativePeriods = new RelativePeriods().setLast4Quarters( true );
        
        List<Period> relatives = relativePeriods.getRelativePeriods( getDate( 2001, 1, 1 ), I18N_FORMAT, false );

        assertEquals( 4, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 3, 31 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 4, 1 ), getDate( 2000, 6, 30 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 9, 30 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2000, 10, 1 ), getDate( 2000, 12, 31 ) ), relatives.get( 3 ) );
    }

    @Test
    public void testGetLast2SixMonths()
    {
        List<Period> relatives = new RelativePeriods().setLast2SixMonths( true ).getRelativePeriods( getDate( 2001, 1, 1 ), I18N_FORMAT, false );

        assertEquals( 2, relatives.size() );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 6, 30 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new SixMonthlyPeriodType(), getDate( 2000, 7, 1 ), getDate( 2000, 12, 31 ) ), relatives.get( 1 ) );
    }

    @Test
    public void testGetLast5Years()
    {
        List<Period> relatives = new RelativePeriods().setLast5Years( true ).getRelativePeriods( getDate( 2001, 1, 1 ), I18N_FORMAT, false );

        assertEquals( 5, relatives.size() );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1996, 1, 1 ), getDate( 1996, 12, 31 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1997, 1, 1 ), getDate( 1997, 12, 31 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1998, 1, 1 ), getDate( 1998, 12, 31 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 1999, 1, 1 ), getDate( 1999, 12, 31 ) ), relatives.get( 3 ) );
        assertEquals( new Period( new YearlyPeriodType(), getDate( 2000, 1, 1 ), getDate( 2000, 12, 31 ) ), relatives.get( 4 ) );
    }

    @Test
    public void testGetMonthsThisYear()
    {
        List<Period> relatives = new RelativePeriods().setMonthsThisYear( true ).getRelativePeriods( getDate( 2001, 4, 1 ), I18N_FORMAT, false );

        assertEquals( 12, relatives.size() );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 1, 31 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 2, 1 ), getDate( 2001, 2, 28 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 3, 1 ), getDate( 2001, 3, 31 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 4, 30 ) ), relatives.get( 3 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 5, 1 ), getDate( 2001, 5, 31 ) ), relatives.get( 4 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 6, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 5 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 7, 31 ) ), relatives.get( 6 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 8, 1 ), getDate( 2001, 8, 31 ) ), relatives.get( 7 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 9, 1 ), getDate( 2001, 9, 30 ) ), relatives.get( 8 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 10, 1 ), getDate( 2001, 10, 31 ) ), relatives.get( 9 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 11, 1 ), getDate( 2001, 11, 30 ) ), relatives.get( 10 ) );
        assertEquals( new Period( new MonthlyPeriodType(), getDate( 2001, 12, 1 ), getDate( 2001, 12, 31 ) ), relatives.get( 11 ) );
    }

    @Test
    public void testGetLastWeek()
    {
        List<Period> relatives = new RelativePeriods().setLastWeek( true ).getRelativePeriods( getDate( 2012, 1, 20 ), I18N_FORMAT, false );

        assertEquals( 1, relatives.size() );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2012, 1, 9 ), getDate( 2012, 1, 15 ) ), relatives.get( 0 ) );
    }

    @Test
    public void testGetLast4Weeks()
    {
        List<Period> relatives = new RelativePeriods().setLast4Weeks( true ).getRelativePeriods( getDate( 2010, 5, 4 ), null, false );

        assertEquals( 4, relatives.size() );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 5 ), getDate( 2010, 4, 11 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 12 ), getDate( 2010, 4, 18 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 19 ), getDate( 2010, 4, 25 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 26 ), getDate( 2010, 5, 2 ) ), relatives.get( 3 ) );
    }

    @Test
    public void testGetLast12Weeks()
    {
        List<Period> relatives = new RelativePeriods().setLast12Weeks( true ).getRelativePeriods( getDate( 2010, 5, 4 ), null, false );

        assertEquals( 12, relatives.size() );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 2, 8 ), getDate( 2010, 2, 14 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 2, 15 ), getDate( 2010, 2, 21 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 2, 22 ), getDate( 2010, 2, 28 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 1 ), getDate( 2010, 3, 7 ) ), relatives.get( 3 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 8 ), getDate( 2010, 3, 14 ) ), relatives.get( 4 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 15 ), getDate( 2010, 3, 21 ) ), relatives.get( 5 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 22 ), getDate( 2010, 3, 28 ) ), relatives.get( 6 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 3, 29 ), getDate( 2010, 4, 4 ) ), relatives.get( 7 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 5 ), getDate( 2010, 4, 11 ) ), relatives.get( 8 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 12 ), getDate( 2010, 4, 18 ) ), relatives.get( 9 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 19 ), getDate( 2010, 4, 25 ) ), relatives.get( 10 ) );
        assertEquals( new Period( new WeeklyPeriodType(), getDate( 2010, 4, 26 ), getDate( 2010, 5, 2 ) ), relatives.get( 11 ) );
    }

    @Test
    public void testGetQuartersThisYear()
    {
        List<Period> relatives = new RelativePeriods().setQuartersThisYear( true ).getRelativePeriods( getDate( 2001, 4, 1 ), null, false );

        assertEquals( 4, relatives.size() );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 1, 1 ), getDate( 2001, 3, 31 ) ), relatives.get( 0 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 4, 1 ), getDate( 2001, 6, 30 ) ), relatives.get( 1 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 7, 1 ), getDate( 2001, 9, 30 ) ), relatives.get( 2 ) );
        assertEquals( new Period( new QuarterlyPeriodType(), getDate( 2001, 10, 1 ), getDate( 2001, 12, 31 ) ), relatives.get( 3 ) );
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
    public void testGetRelativePeriodsFromPeriodTypes()
    {
        Set<String> periodTypes = new HashSet<>();
        periodTypes.add( MonthlyPeriodType.NAME );
        periodTypes.add( BiMonthlyPeriodType.NAME );
        periodTypes.add( QuarterlyPeriodType.NAME );
        periodTypes.add( SixMonthlyPeriodType.NAME );
        periodTypes.add( YearlyPeriodType.NAME );
        periodTypes.add( FinancialOctoberPeriodType.NAME );

        List<Period> periods = new RelativePeriods().getLast12Months( periodTypes );

        assertEquals( 26, periods.size() );
    }
}
