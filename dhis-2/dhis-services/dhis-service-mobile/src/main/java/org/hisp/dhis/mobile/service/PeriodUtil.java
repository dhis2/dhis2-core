package org.hisp.dhis.mobile.service;

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

import java.util.Calendar;
import java.util.Vector;

import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.system.util.DateUtils;

public class PeriodUtil
{
    public static Period getPeriod( String periodName, PeriodType periodType )
        throws InvalidIdentifierReferenceException
    {
        if ( periodType instanceof DailyPeriodType )
        {
            return periodType.createPeriod( DateUtils.getMediumDate( periodName ) );
        }

        if ( periodType instanceof WeeklyPeriodType )
        {
            return periodType.createPeriod( DateUtils.getMediumDate( periodName ) );
        }

        if ( periodType instanceof MonthlyPeriodType )
        {
            int dashIndex = periodName.indexOf( '-' );

            if ( dashIndex < 0 )
            {
                return null;
            }

            int month = Integer.parseInt( periodName.substring( 0, dashIndex ) );
            int year = Integer.parseInt( periodName.substring( dashIndex + 1, periodName.length() ) );

            Calendar cal = Calendar.getInstance();
            cal.set( Calendar.YEAR, year );
            cal.set( Calendar.MONTH, month );

            return periodType.createPeriod( cal.getTime() );
        }

        if ( periodType instanceof YearlyPeriodType )
        {
            Calendar cal = Calendar.getInstance();
            cal.set( Calendar.YEAR, Integer.parseInt( periodName ) );

            return periodType.createPeriod( cal.getTime() );
        }

        if ( periodType instanceof QuarterlyPeriodType )
        {
            Calendar cal = Calendar.getInstance();

            int month = 0;
            if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Jan" ) )
            {
                month = 1;
            }
            else if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Apr" ) )
            {
                month = 4;
            }
            else if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Jul" ) )
            {
                month = 6;
            }
            else if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Oct" ) )
            {
                month = 10;
            }

            int year = Integer.parseInt( periodName.substring( periodName.lastIndexOf( " " ) + 1 ) );

            cal.set( Calendar.MONTH, month );
            cal.set( Calendar.YEAR, year );

            if ( month != 0 )
            {
                return periodType.createPeriod( cal.getTime() );
            }

        }

        throw new InvalidIdentifierReferenceException( "Couldn't make a period of type " + periodType.getName() + " and name "
            + periodName );
    }

    public static String convertDateFormat( String standardDate )
    {
        try
        {
            String[] tokens = standardDate.split( "-" );
            return tokens[2] + "-" + tokens[1] + "-" + tokens[0];
        }
        catch ( Exception e )
        {
            return standardDate;
        }
    }

    public static Vector<String> generatePeriods( String periodType )
    {
        Vector<String> periods = null;
        if ( periodType.equals( "Monthly" ) )
        {
            periods = PeriodUtil.generateMonthlyPeriods();
        }
        else if ( periodType.equals( "Yearly" ) )
        {
            periods = PeriodUtil.generateYearlyPeriods();
        }
        else if ( periodType.equals( "Quarterly" ) )
        {
            periods = PeriodUtil.generateQuaterlyPeriods();
        }
        return periods;
    }

    public static Vector<String> generateMonthlyPeriods()
    {
        Vector<String> months = new Vector<>();
        Calendar cal = Calendar.getInstance();

        // Display only 12 previous periods including the current one
        cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) );

        for ( int i = 0; i < 11; i++ )
        {
            if ( cal.get( Calendar.MONTH ) < 0 )
            {
                cal.set( Calendar.MONTH, 11 );
                cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - 1 );
            }
            months.addElement( cal.get( Calendar.MONTH ) + "-" + cal.get( Calendar.YEAR ) );
            cal.set( Calendar.MONTH, cal.get( Calendar.MONTH ) - 1 );
        }

        return months;
    }

    public static Vector<String> generateYearlyPeriods()
    {
        Vector<String> years = new Vector<>();
        Calendar cal = Calendar.getInstance();

        // Display only 12 previous periods including the current one
        cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) );

        for ( int i = 0; i < 2; i++ )
        {
            years.addElement( Integer.toString( cal.get( Calendar.YEAR ) ) );
            cal.set( Calendar.YEAR, cal.get( Calendar.YEAR ) - 1 );
        }

        return years;
    }
    
    public static Vector<String> generateQuaterlyPeriods()
    {
        Vector<String> quarters = new Vector<>();
        Calendar cal = Calendar.getInstance();
        String[] quatersStr = { "Jan to Mar", "Apr to Jun", "Jul to Sep", "Oct to Dec" };

        if ( cal.get( Calendar.MONTH ) >= 0 && cal.get( Calendar.MONTH ) <= 2 )
        {
            quarters.addElement( quatersStr[0] + " " + cal.get( Calendar.YEAR ) );
        }
        else if ( cal.get( Calendar.MONTH ) >= 3 && cal.get( Calendar.MONTH ) <= 5 )
        {
            quarters.addElement( quatersStr[1] + " " + cal.get( Calendar.YEAR ) );
            quarters.addElement( quatersStr[0] + " " + cal.get( Calendar.YEAR ) );
        }
        else if ( cal.get( Calendar.MONTH ) >= 6 && cal.get( Calendar.MONTH ) <= 8 )
        {
            quarters.addElement( quatersStr[2] + " " + cal.get( Calendar.YEAR ) );
            quarters.addElement( quatersStr[1] + " " + cal.get( Calendar.YEAR ) );
            quarters.addElement( quatersStr[0] + " " + cal.get( Calendar.YEAR ) );
        }
        else if ( cal.get( Calendar.MONTH ) >= 9 && cal.get( Calendar.MONTH ) <= 11 )
        {
            quarters.addElement( quatersStr[3] + " " + cal.get( Calendar.YEAR ) );
            quarters.addElement( quatersStr[2] + " " + cal.get( Calendar.YEAR ) );
            quarters.addElement( quatersStr[1] + " " + cal.get( Calendar.YEAR ) );
            quarters.addElement( quatersStr[0] + " " + cal.get( Calendar.YEAR ) );
        }
        return quarters;
    }
    
}
