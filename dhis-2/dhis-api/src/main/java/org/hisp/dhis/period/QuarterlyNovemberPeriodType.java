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
package org.hisp.dhis.period;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public class QuarterlyNovemberPeriodType
    extends QuarterlyAbstractPeriodType
{
    private static final String ISO_FORMAT = "yyyyQn";

    public static final String NAME = "QuarterlyNov";

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        return NAME;
    }

    /**
     * n refers to the quarter, can be [1-4].
     */
    @Override
    public String getIsoFormat()
    {
        return ISO_FORMAT;
    }

    @Override
    public Period createPeriod( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        int startMonth = ((dateTimeUnit.getMonth() - 2) - ((dateTimeUnit.getMonth() - 2) % 3)) + 2;
        int startYear = dateTimeUnit.getYear();

        if ( dateTimeUnit.getMonth() == 1 )
        {
            startMonth = -1;
        }

        if ( startMonth > 12 )
        {
            startMonth = 1;
            startYear = startYear + 1;
        }
        else if ( startMonth < 1 )
        {
            startMonth = startMonth + 12;
            startYear = startYear - 1;
        }

        DateTimeUnit start = new DateTimeUnit( startYear, startMonth, 1, dateTimeUnit.isIso8601() );

        DateTimeUnit end = new DateTimeUnit( start );
        end = calendar.plusMonths( end, 2 );
        end.setDay( calendar.daysInMonth( end.getYear(), end.getMonth() ) );

        return toIsoPeriod( start, end, calendar );
    }

    @Override
    public String getIsoDate( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        DateTimeUnit newUnit = dateTimeUnit;

        if ( !calendar.name().equals( ISO_CALENDAR_NAME ) && newUnit.isIso8601() )
        {
            newUnit = calendar.fromIso( newUnit );
        }

        switch ( newUnit.getMonth() )
        {
        case 11:
            return newUnit.getYear() + 1 + "NovQ1";
        case 2:
            return newUnit.getYear() + "NovQ2";
        case 5:
            return newUnit.getYear() + "NovQ3";
        case 8:
            return newUnit.getYear() + "NovQ4";
        default:
            throw new IllegalArgumentException( "Month not valid [11,2,5,8], was given " + dateTimeUnit.getMonth() );
        }
    }

}