package org.hisp.dhis.analytics;

import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.FinancialPeriodType;

/**
 * Created by henninghakonsen on 15/05/2017.
 * Project: dhis-2.
 */
public enum AnalyticsFinancialYearStartKey
{
    FINANCIAL_PERIOD_APRIL( "FINANCIAL_PERIOD_APRIL", new FinancialAprilPeriodType() ),
    FINANCIAL_PERIOD_JULY( "FINANCIAL_PERIOD_JULY", new FinancialJulyPeriodType() ),
    FINANCIAL_PERIOD_OCTOBER( "FINANCIAL_PERIOD_OCTOBER", new FinancialOctoberPeriodType() );

    private final String name;

    private final FinancialPeriodType financialPeriodType;

    AnalyticsFinancialYearStartKey( String name, FinancialPeriodType financialPeriodType )
    {
        this.name = name;
        this.financialPeriodType = financialPeriodType;
    }

    public String getName()
    {
        return name;
    }

    public FinancialPeriodType getFinancialPeriodType()
    {
        return financialPeriodType;
    }
}
