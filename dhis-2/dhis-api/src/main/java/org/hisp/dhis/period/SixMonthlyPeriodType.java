package org.hisp.dhis.period;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.joda.time.DateTimeConstants;

/**
 * PeriodType for six-monthly Periods. A valid six-monthly Period has startDate
 * set to either January 1st or July 1st, and endDate set to the last day of the
 * fifth month after the startDate.
 *
 * @author Torgeir Lorange Ostby
 * @version $Id: SixMonthlyPeriodType.java 2971 2007-03-03 18:54:56Z torgeilo $
 */
public class SixMonthlyPeriodType
    extends SixMonthlyAbstractPeriodType
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 5709134010793412705L;

    private static final String ISO_FORMAT = "yyyySn";

    private static final String ISO8601_DURATION = "P6M";

    private static final int BASE_MONTH = DateTimeConstants.JANUARY;

    /**
     * The name of the SixMonthlyPeriodType, which is "SixMonthly".
     */
    public static final String NAME = "SixMonthly";

    // -------------------------------------------------------------------------
    // PeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public int getBaseMonth()
    {
        return BASE_MONTH;
    }

    // -------------------------------------------------------------------------
    // CalendarPeriodType functionality
    // -------------------------------------------------------------------------

    @Override
    public String getIsoDate( DateTimeUnit dateTimeUnit, Calendar calendar )
    {
        int month = dateTimeUnit.getMonth();

        if ( dateTimeUnit.isIso8601() )
        {
            month = calendar.fromIso( dateTimeUnit ).getMonth();
        }

        switch ( month )
        {
            case 1:
                return dateTimeUnit.getYear() + "S1";
            case 7:
                return dateTimeUnit.getYear() + "S2";
            default:
                throw new IllegalArgumentException( "Month not valid [1,7]" );
        }
    }

    /**
     * n refers to the semester, can be [1-2].
     */
    @Override
    public String getIsoFormat()
    {
        return ISO_FORMAT;
    }

    @Override
    public String getIso8601Duration()
    {
        return ISO8601_DURATION;
    }

}
