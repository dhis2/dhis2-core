package org.hisp.dhis.commons.action;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.period.RelativePeriods;
import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public abstract class AbstractRelativePeriodsAction
    implements Action
{
    protected boolean reportingMonth;
    protected boolean last3Days;
    protected boolean last7Days;
    protected boolean last14Days;
    protected boolean lastMonth;
    protected boolean reportingWeek;
    protected boolean lastQuarter;
    protected boolean reportingBimonth;
    protected boolean lastBiMonth;
    protected boolean reportingQuarter;
    protected boolean last6Months;
    protected boolean lastSixMonth;
    protected boolean reportingSixMonth;
    protected boolean weeksThisYear;
    protected boolean monthsThisYear;
    protected boolean biMonthsThisYear;
    protected boolean quartersThisYear;
    protected boolean thisYear;
    protected boolean reportingDay;
    protected boolean monthsLastYear;
    protected boolean quartersLastYear;
    protected boolean last5Years;
    protected boolean lastYear;
    protected boolean last4Quarters;
    protected boolean last2SixMonths;
    protected boolean thisFinancialYear;
    protected boolean lastFinancialYear;
    protected boolean last3Months;
    protected boolean last12Months;
    protected boolean last6BiMonths;
    protected boolean last5FinancialYears;
    protected boolean lastWeek;
    protected boolean reportingBiWeek;
    protected boolean lastBiWeek;
    protected boolean last4Weeks;
    protected boolean last4BiWeeks;
    protected boolean last12Weeks;
    protected boolean last52Weeks;
    protected boolean yesterday;


    public void setReportingMonth( boolean reportingMonth )
    {
        this.reportingMonth = reportingMonth;
    }

    public void setReportingBimonth( boolean reportingBimonth )
    {
        this.reportingBimonth = reportingBimonth;
    }

    public void setLastBiMonth( boolean lastBiMonth )
    {
        this.lastBiMonth = lastBiMonth;
    }

    public void setReportingQuarter( boolean reportingQuarter )
    {
        this.reportingQuarter = reportingQuarter;
    }

    public void setLast6Months( boolean last6Months )
    {
        this.last6Months = last6Months;
    }

    public void setReportingSixMonth( boolean reportingSixMonth )
    {
        this.reportingSixMonth = reportingSixMonth;
    }

    public void setWeeksThisYear( boolean weeksThisYear )
    {
        this.weeksThisYear = weeksThisYear;
    }

    public void setMonthsThisYear( boolean monthsThisYear )
    {
        this.monthsThisYear = monthsThisYear;
    }

    public void setBiMonthsThisYear( boolean biMonthsThisYear )
    {
        this.biMonthsThisYear = biMonthsThisYear;
    }

    public void setQuartersThisYear( boolean quartersThisYear )
    {
        this.quartersThisYear = quartersThisYear;
    }

    public void setThisYear( boolean thisYear )
    {
        this.thisYear = thisYear;
    }

    public void setMonthsLastYear( boolean monthsLastYear )
    {
        this.monthsLastYear = monthsLastYear;
    }

    public void setQuartersLastYear( boolean quartersLastYear )
    {
        this.quartersLastYear = quartersLastYear;
    }

    public void setLast5Years( boolean last5Years )
    {
        this.last5Years = last5Years;
    }

    public void setLastYear( boolean lastYear )
    {
        this.lastYear = lastYear;
    }

    public void setLast12Months( boolean last12Months )
    {
        this.last12Months = last12Months;
    }

    public void setLast4Quarters( boolean last4Quarters )
    {
        this.last4Quarters = last4Quarters;
    }

    public void setLast2SixMonths( boolean last2SixMonths )
    {
        this.last2SixMonths = last2SixMonths;
    }

    public void setThisFinancialYear( boolean thisFinancialYear )
    {
        this.thisFinancialYear = thisFinancialYear;
    }

    public void setLastFinancialYear( boolean lastFinancialYear )
    {
        this.lastFinancialYear = lastFinancialYear;
    }

    public void setLast3Months( boolean last3Months )
    {
        this.last3Months = last3Months;
    }

    public void setLast6BiMonths( boolean last6BiMonths )
    {
        this.last6BiMonths = last6BiMonths;
    }

    public void setLast5FinancialYears( boolean last5FinancialYears )
    {
        this.last5FinancialYears = last5FinancialYears;
    }

    public void setLastWeek( boolean lastWeek )
    {
        this.lastWeek = lastWeek;
    }

    public void setLast4Weeks( boolean last4Weeks )
    {
        this.last4Weeks = last4Weeks;
    }

    public void setLast12Weeks( boolean last12Weeks )
    {
        this.last12Weeks = last12Weeks;
    }

    public void setLast52Weeks( boolean last52Weeks )
    {
        this.last52Weeks = last52Weeks;
    }

    public void setReportingDay ( boolean reportingDay )
    {
        this.reportingDay = reportingDay;
    }

    public void setYesterday( boolean yesterday )
    {
        this.yesterday = yesterday;
    }

    public void setLast3Days( boolean last3Days )
    {
        this.last3Days = last3Days;
    }

    public void setLast7Days( boolean last7Days )
    {
        this.last7Days = last7Days;
    }

    public void setLast14Days( boolean last14Days )
    {
        this.last14Days = last14Days;
    }

    public void setLastMonth( boolean lastMonth )
    {
        this.lastMonth = lastMonth;
    }

    public void setReportingWeek( boolean reportingWeek )
    {
        this.reportingWeek = reportingWeek;
    }

    public void setLastQuarter( boolean lastQuarter )
    {
        this.lastQuarter = lastQuarter;
    }

    public void setLastSixMonth( boolean lastSixMonth )
    {
        this.lastSixMonth = lastSixMonth;
    }

    protected RelativePeriods getRelativePeriods()
    {
        RelativePeriods relatives = new RelativePeriods( reportingDay, yesterday, last3Days, last7Days, last14Days, reportingMonth, lastMonth,
            reportingBimonth, lastBiMonth, reportingQuarter, lastQuarter, reportingSixMonth, lastSixMonth,
            weeksThisYear, monthsThisYear, biMonthsThisYear, quartersThisYear, thisYear, 
            monthsLastYear, quartersLastYear, lastYear,
            last5Years, last12Months, last6Months, last3Months, last6BiMonths, last4Quarters, last2SixMonths,
            thisFinancialYear, lastFinancialYear, last5FinancialYears,
            reportingWeek, lastWeek, reportingBiWeek, lastBiWeek, last4Weeks, last4BiWeeks, last12Weeks, last52Weeks );

        return relatives;
    }
}
