package org.hisp.dhis.calendar;

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

import org.hisp.dhis.period.BiMonthlyPeriodType;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.SixMonthlyAprilPeriodType;
import org.hisp.dhis.period.SixMonthlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum DateUnitType
{
    DAILY( DailyPeriodType.NAME, "\\b(\\d{4})(\\d{2})(\\d{2})\\b" ),
    WEEKLY( WeeklyPeriodType.NAME, "\\b(\\d{4})W(\\d[\\d]?)\\b" ),
    MONTHLY( MonthlyPeriodType.NAME, "\\b(\\d{4})[-]?(\\d{2})\\b" ),
    BI_MONTHLY( BiMonthlyPeriodType.NAME, "\\b(\\d{4})(\\d{2})B\\b" ),
    QUARTERLY( QuarterlyPeriodType.NAME, "\\b(\\d{4})Q(\\d)\\b" ),
    SIX_MONTHLY( SixMonthlyPeriodType.NAME, "\\b(\\d{4})S(\\d)\\b" ),
    SIX_MONTHLY_APRIL( SixMonthlyAprilPeriodType.NAME, "\\b(\\d{4})AprilS(\\d)\\b" ),
    YEARLY( YearlyPeriodType.NAME, "\\b(\\d{4})\\b" ),
    FINANCIAL_APRIL( FinancialAprilPeriodType.NAME, "\\b(\\d{4})April\\b" ),
    FINANCIAL_JULY( FinancialJulyPeriodType.NAME, "\\b(\\d{4})July\\b" ),
    FINANCIAL_OCTOBER( FinancialOctoberPeriodType.NAME, "\\b(\\d{4})Oct\\b" );

    private final String type;

    private final String format;

    public String getType()
    {
        return type;
    }

    public String getFormat()
    {
        return format;
    }

    DateUnitType( String type, String format )
    {
        this.type = type;
        this.format = format;
    }

    public static DateUnitType find( String format )
    {
        for ( DateUnitType type : DateUnitType.values() )
        {
            if ( format.matches( type.format ) )
            {
                return type;
            }
        }

        return null;
    }
}
