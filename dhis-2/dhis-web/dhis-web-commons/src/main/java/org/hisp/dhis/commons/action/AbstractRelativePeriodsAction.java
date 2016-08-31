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

import org.hisp.dhis.period.RelativePeriods;
import com.opensymphony.xwork2.Action;

/**
 * @author Lars Helge Overland
 */
public abstract class AbstractRelativePeriodsAction
    implements Action
{
    protected boolean reportingMonth;

    public void setReportingMonth( boolean reportingMonth )
    {
        this.reportingMonth = reportingMonth;
    }

    protected boolean reportingBimonth;
    
    public void setReportingBimonth( boolean reportingBimonth )
    {
        this.reportingBimonth = reportingBimonth;
    }
    
    protected boolean reportingQuarter;

    public void setReportingQuarter( boolean reportingQuarter )
    {
        this.reportingQuarter = reportingQuarter;
    }

    protected boolean lastSixMonth;

    public void setLastSixMonth( boolean lastSixMonth )
    {
        this.lastSixMonth = lastSixMonth;
    }

    protected boolean monthsThisYear;

    public void setMonthsThisYear( boolean monthsThisYear )
    {
        this.monthsThisYear = monthsThisYear;
    }

    protected boolean quartersThisYear;

    public void setQuartersThisYear( boolean quartersThisYear )
    {
        this.quartersThisYear = quartersThisYear;
    }

    protected boolean thisYear;

    public void setThisYear( boolean thisYear )
    {
        this.thisYear = thisYear;
    }

    protected boolean monthsLastYear;

    public void setMonthsLastYear( boolean monthsLastYear )
    {
        this.monthsLastYear = monthsLastYear;
    }

    protected boolean quartersLastYear;

    public void setQuartersLastYear( boolean quartersLastYear )
    {
        this.quartersLastYear = quartersLastYear;
    }

    protected boolean last5Years;
    
    public void setLast5Years( boolean last5Years )
    {
        this.last5Years = last5Years;
    }

    protected boolean lastYear;
    
    public void setLastYear( boolean lastYear )
    {
        this.lastYear = lastYear;
    }
    
    protected boolean last12Months;

    public void setLast12Months( boolean last12Months )
    {
        this.last12Months = last12Months;
    }

    protected boolean last6Months;

    public void setLast6Months( boolean last6Months )
    {
        this.last6Months = last6Months;
    }

    protected boolean last4Quarters;

    public void setLast4Quarters( boolean last4Quarters )
    {
        this.last4Quarters = last4Quarters;
    }
    
    protected boolean last2SixMonths;

    public void setLast2SixMonths( boolean last2SixMonths )
    {
        this.last2SixMonths = last2SixMonths;
    }
    
    protected boolean thisFinancialYear;

    public void setThisFinancialYear( boolean thisFinancialYear )
    {
        this.thisFinancialYear = thisFinancialYear;
    }

    protected boolean lastFinancialYear;

    public void setLastFinancialYear( boolean lastFinancialYear )
    {
        this.lastFinancialYear = lastFinancialYear;
    }

    protected boolean last3Months;
    
    public void setLast3Months( boolean last3Months )
    {
        this.last3Months = last3Months;
    }

    protected boolean last6BiMonths;
    
    public void setLast6BiMonths( boolean last6BiMonths )
    {
        this.last6BiMonths = last6BiMonths;
    }

    protected boolean last5FinancialYears;

    public void setLast5FinancialYears( boolean last5FinancialYears )
    {
        this.last5FinancialYears = last5FinancialYears;
    }

    protected boolean lastWeek;
    
    public void setLastWeek( boolean lastWeek )
    {
        this.lastWeek = lastWeek;
    }

    protected boolean last4Weeks;
    
    public void setLast4Weeks( boolean last4Weeks )
    {
        this.last4Weeks = last4Weeks;
    }

    protected boolean last12Weeks;

    public void setLast12Weeks( boolean last12Weeks )
    {
        this.last12Weeks = last12Weeks;
    }

    protected boolean last52Weeks;
    
    public void setLast52Weeks( boolean last52Weeks )
    {
        this.last52Weeks = last52Weeks;
    }
    
    protected RelativePeriods getRelativePeriods()
    {
        RelativePeriods relatives = new RelativePeriods( false, reportingMonth, false, reportingBimonth, false, reportingQuarter, false, lastSixMonth,
            monthsThisYear, quartersThisYear, thisYear, 
            monthsLastYear, quartersLastYear, lastYear, 
            last5Years, last12Months, last6Months, last3Months, last6BiMonths, last4Quarters, last2SixMonths,
            thisFinancialYear, lastFinancialYear, last5FinancialYears, 
            false, lastWeek, last4Weeks, last12Weeks, last52Weeks );
        
        return relatives;
    }
}
