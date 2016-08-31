package org.hisp.dhis.period;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum RelativePeriodEnum
{
    THIS_MONTH,
    LAST_MONTH,
    THIS_BIMONTH,
    LAST_BIMONTH,
    THIS_QUARTER,
    LAST_QUARTER,
    THIS_SIX_MONTH,
    LAST_SIX_MONTH,
    MONTHS_THIS_YEAR,
    QUARTERS_THIS_YEAR,
    THIS_YEAR,
    MONTHS_LAST_YEAR,
    QUARTERS_LAST_YEAR,
    LAST_YEAR,
    LAST_5_YEARS,
    LAST_12_MONTHS,
    LAST_6_MONTHS,
    LAST_3_MONTHS,
    LAST_6_BIMONTHS,
    LAST_4_QUARTERS,
    LAST_2_SIXMONTHS,
    THIS_FINANCIAL_YEAR,
    LAST_FINANCIAL_YEAR,
    LAST_5_FINANCIAL_YEARS,
    THIS_WEEK,
    LAST_WEEK,
    LAST_4_WEEKS,
    LAST_12_WEEKS,
    LAST_52_WEEKS;
    
    public static List<String> OPTIONS = new ArrayList<String>() { {
        addAll( Arrays.asList( THIS_MONTH.toString(), LAST_MONTH.toString(), THIS_BIMONTH.toString(), LAST_BIMONTH.toString(), 
            THIS_QUARTER.toString(), LAST_QUARTER.toString(), THIS_SIX_MONTH.toString(), LAST_SIX_MONTH.toString(),
            MONTHS_THIS_YEAR.toString(), QUARTERS_THIS_YEAR.toString(), THIS_YEAR.toString(), MONTHS_LAST_YEAR.toString(), QUARTERS_LAST_YEAR.toString(),
            LAST_YEAR.toString(), LAST_5_YEARS.toString(), LAST_12_MONTHS.toString(), LAST_6_MONTHS.toString(), LAST_3_MONTHS.toString(), LAST_6_BIMONTHS.toString(), 
            LAST_4_QUARTERS.toString(), LAST_2_SIXMONTHS.toString(), THIS_FINANCIAL_YEAR.toString(), LAST_FINANCIAL_YEAR.toString(), 
            LAST_5_FINANCIAL_YEARS.toString(), THIS_WEEK.toString(), LAST_WEEK.toString(), LAST_4_WEEKS.toString(), LAST_12_WEEKS.toString(), LAST_52_WEEKS.toString() ) );
    } };
    
    public static boolean contains( String relativePeriod )
    {
        return OPTIONS.contains( relativePeriod );
    }
}
