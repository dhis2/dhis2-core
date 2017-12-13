package org.hisp.dhis.analytics;

import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.FinancialPeriodType;

/**
 * @author Henning Håkonsen
 */
public enum AnalyticsFinancialYearStartKey
{
    FINANCIAL_YEAR_APRIL( "FINANCIAL_YEAR_APRIL", new FinancialAprilPeriodType() ),
    FINANCIAL_YEAR_JULY( "FINANCIAL_YEAR_JULY", new FinancialJulyPeriodType() ),
    FINANCIAL_YEAR_OCTOBER( "FINANCIAL_YEAR_OCTOBER", new FinancialOctoberPeriodType() );

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
