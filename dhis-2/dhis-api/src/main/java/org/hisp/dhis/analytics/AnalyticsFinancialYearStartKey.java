package org.hisp.dhis.analytics;

import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;

/**
 * Created by henninghakonsen on 15/05/2017.
 * Project: dhis-2.
 */
public enum AnalyticsFinancialYearStartKey
{
    FINANCIAL_PERIOD_APRIL("FinancialPeriodApril", FinancialAprilPeriodType.class),
    FINANCIAL_PERIOD_JULY("FinancialPeriodJuly", FinancialJulyPeriodType.class),
    FINANCIAL_PERIOD_OCTOBER("FinancialPeriodOctober", FinancialOctoberPeriodType.class);

    private final String name;

    private final Class<?> clazz;

    AnalyticsFinancialYearStartKey( String name, Class<?>  clazz )
    {
        this.name = name;
        this.clazz = clazz;
    }

    public String getName()
    {
        return name;
    }

    public Class<?> getClazz()
    {
        return clazz;
    }
}
