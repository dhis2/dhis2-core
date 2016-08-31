package org.hisp.dhis.commons.action;

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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.filter.PastAndCurrentPeriodFilter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.util.SessionUtils;

/**
 * @author Lars Helge Overland
 */
public class GetAvailablePeriodsAction
    extends ActionPagingSupport<Period>
{
    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String periodType;

    public void setPeriodType( String periodType )
    {
        this.periodType = periodType;
    }

    private int year;

    public void setYear( int year )
    {
        this.year = year;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<Period> periods;

    public List<Period> getPeriods()
    {
        return periods;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        periodType = periodType != null && !periodType.isEmpty() ? periodType : MonthlyPeriodType.NAME;

        CalendarPeriodType _periodType = (CalendarPeriodType) PeriodType.getPeriodTypeByName( periodType );

        int thisYear = Calendar.getInstance().get( Calendar.YEAR );

        int currentYear = (Integer) SessionUtils.getSessionVar( SessionUtils.KEY_CURRENT_YEAR, thisYear );

        Calendar cal = PeriodType.createCalendarInstance();

        // Cannot go to next year if current year equals this year
        
        if ( !( currentYear == thisYear && year > 0 ) )
        {
            cal.set( Calendar.YEAR, currentYear );
            cal.add( Calendar.YEAR, year );

            SessionUtils.setSessionVar( SessionUtils.KEY_CURRENT_YEAR, cal.get( Calendar.YEAR ) );
        }

        periods = _periodType.generatePeriods( cal.getTime() );

        FilterUtils.filter( periods, new PastAndCurrentPeriodFilter() );

        Collections.reverse( periods );

        if ( usePaging )
        {
            this.paging = createPaging( periods.size() );

            periods = periods.subList( paging.getStartPos(), paging.getEndPos() );
        }

        for ( Period period : periods )
        {
            period.setName( format.formatPeriod( period ) );
        }

        return SUCCESS;
    }
}
