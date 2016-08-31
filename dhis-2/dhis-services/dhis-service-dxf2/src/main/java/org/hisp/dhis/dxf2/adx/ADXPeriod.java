package org.hisp.dhis.dxf2.adx;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.hisp.dhis.period.BiMonthlyPeriodType;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.SixMonthlyAprilPeriodType;
import org.hisp.dhis.period.SixMonthlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;

/**
 * ADXPeriod
 *
 * A simple wrapper class for parsing ISO 8601 <date>/<duration> period types
 *
 * @author bobj
 */
public class ADXPeriod
{

    public static enum Duration
    {
        P1D, // daily
        P7D, // weekly
        P1M, // monthly
        P2M, // bi-monthly
        P1Q, // quarterly
        P6M, // 6monthly (including 6monthlyApril)
        P1Y  // yearly, financialApril, financialJuly, financialOctober
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd" );

    public static Period parse( String periodString ) throws ADXException
    {
        String[] tokens = periodString.split( "/" );
        
        if ( tokens.length != 2 )
        {
            throw new ADXException( periodString + " not in valid <date>/<duration> format" );
        }

        try
        {
            Period period = null;
            PeriodType periodType = null;
            Date startDate = DATE_FORMAT.parse( tokens[0] );
            Calendar cal = Calendar.getInstance();
            cal.setTime( startDate );
            Duration duration = Duration.valueOf( tokens[1] );

            switch ( duration )
            {
                case P1D:
                    periodType = new DailyPeriodType();
                    break;
                case P7D:
                    periodType = new WeeklyPeriodType();
                    break;
                case P1M:
                    periodType = new MonthlyPeriodType();
                    break;
                case P2M:
                    periodType = new BiMonthlyPeriodType();
                    break;
                case P1Q:
                    periodType = new QuarterlyPeriodType();
                    break;
                case P6M:
                    switch ( cal.get( Calendar.MONTH ) )
                    {
                        case 0:
                            periodType = new SixMonthlyPeriodType();
                            break;
                        case 6:
                            periodType = new SixMonthlyAprilPeriodType();
                            break;
                        default:
                            throw new ADXException( periodString + "is invalid sixmonthly type" );
                    }
                case P1Y:
                    switch ( cal.get( Calendar.MONTH ) )
                    {
                        case 0:
                            periodType = new YearlyPeriodType();
                            break;
                        case 3:
                            periodType = new FinancialAprilPeriodType();
                            break;
                        case 6:
                            periodType = new FinancialJulyPeriodType();
                            break;
                        case 9:
                            periodType = new FinancialOctoberPeriodType();
                            break;
                        default:
                            throw new ADXException( periodString + "is invalid yearly type" );
                    }
            }

            if ( periodType != null )
            {
                period = periodType.createPeriod( startDate );
            } 
            else
            {
                throw new ADXException( "Failed to create period type from " + duration );
            }

            return period;

        } 
        catch ( ParseException ex )
        {
            throw new ADXException( tokens[0] + "is not a valid date in YYYY-MM-dd format" );
        } 
        catch ( IllegalArgumentException ex )
        {
            throw new ADXException( tokens[1] + " is not a supported duration type" );
        }
    }

    public static String serialize( Period period )
    {
        return DATE_FORMAT.format( period.getStartDate() ) + "/"
            + period.getPeriodType().getIso8601Duration();
    }
}
