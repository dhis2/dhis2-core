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

import static org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.calendar.impl.Iso8601Calendar;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.i18n.I18nFormat;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "relativePeriods", namespace = DxfNamespaces.DXF_2_0 )
public class RelativePeriods
    implements Serializable
{
    public static final String[] MONTH_NAMES = {
        "january",
        "february",
        "march",
        "april",
        "may",
        "june",
        "july",
        "august",
        "september",
        "october",
        "november",
        "december" };

    public static final String THISDAY = "thisDay";

    public static final String YESTERDAY = "yesterday";

    public static final String LAST_WEEK = "last_week";

    public static final String LAST_BIWEEK = "last_biweek";

    public static final String LAST_MONTH = "reporting_month";

    public static final String LAST_BIMONTH = "reporting_bimonth";

    public static final String LAST_QUARTER = "reporting_quarter";

    public static final String LAST_SIXMONTH = "last_sixmonth";

    public static final String THIS_YEAR = "year";

    public static final String LAST_YEAR = "last_year";

    public static final String THIS_FINANCIAL_YEAR = "financial_year";

    public static final String LAST_FINANCIAL_YEAR = "last_financial_year";

    public static final String[] MONTHS_THIS_YEAR = {
        "january",
        "february",
        "march",
        "april",
        "may",
        "june",
        "july",
        "august",
        "september",
        "october",
        "november",
        "december" };

    // Generates an array containing Strings "day1" -> "day365"
    public static final String[] DAYS_IN_YEAR = streamToStringArray( IntStream.rangeClosed( 1, 365 ).boxed(), "day",
        "" );

    // Generates an array containing Strings "january_last_year" ->
    // "december_last_year"
    public static final String[] MONTHS_LAST_YEAR = streamToStringArray( Arrays.stream( MONTHS_THIS_YEAR ), "",
        "_last_year" );

    // Generates an array containing Strings "month1" -> "month12"
    public static final String[] MONTHS_LAST_12 = streamToStringArray( IntStream.rangeClosed( 1, 12 ).boxed(), "month",
        "" );

    // Generates an array containing Strings "bimonth1" -> "bimonth6"
    public static final String[] BIMONTHS_LAST_6 = streamToStringArray( IntStream.rangeClosed( 1, 6 ).boxed(),
        "bimonth", "" );

    // Generates an array containing Strings "bimonth1" -> "bimonth6"
    public static final String[] BIMONTHS_THIS_YEAR = streamToStringArray( IntStream.rangeClosed( 1, 6 ).boxed(),
        "bimonth", "" );

    // Generates an array containing Strings "quarter1" -> "quarter4"
    public static final String[] QUARTERS_THIS_YEAR = streamToStringArray( IntStream.rangeClosed( 1, 4 ).boxed(),
        "quarter", "" );

    // Generates an array containing Strings "sixmonth1" and "sixmonth2"
    public static final String[] SIXMONHTS_LAST_2 = streamToStringArray( IntStream.rangeClosed( 1, 2 ).boxed(),
        "sixmonth", "" );

    // Generates an array containing Strings "quarter1_last_year" ->
    // "quarter4_last_year"
    public static final String[] QUARTERS_LAST_YEAR = streamToStringArray( IntStream.rangeClosed( 1, 4 ).boxed(),
        "quarter", "_last_year" );

    // Generates an array containing "year_minus_4" -> "year_minus_1" +
    // "year_this"
    public static final String[] LAST_5_YEARS = (String[]) ArrayUtils.addAll(
        streamToStringArray( IntStream.rangeClosed( 1, 4 ).map( i -> 4 - i + 1 ).boxed(), "year_minus_", "" ),
        Collections.singletonList( "year_this" ).toArray() );

    // Generates an array containing "year_minus_9" -> "year_minus_1" +
    // "year_this"
    public static final String[] LAST_10_YEARS = (String[]) ArrayUtils.addAll(
        streamToStringArray( IntStream.rangeClosed( 1, 9 ).map( i -> 9 - i + 1 ).boxed(), "year_minus_", "" ),
        Collections.singletonList( "year_this" ).toArray() );

    // Generates an array containing "financial_year_minus_4" ->
    // "financial_year_minus_1" + "financial_year_this"
    public static final String[] LAST_5_FINANCIAL_YEARS = (String[]) ArrayUtils.addAll(
        streamToStringArray( IntStream.rangeClosed( 1, 4 ).map( i -> 4 - i + 1 ).boxed(), "financial_year_minus_", "" ),
        Collections.singletonList( "financial_year_this" ).toArray() );

    // Generates an array containing "financial_year_minus_9" ->
    // "financial_year_minus_1" + "financial_year_this"
    public static final String[] LAST_10_FINANCIAL_YEARS = (String[]) ArrayUtils.addAll(
        streamToStringArray( IntStream.rangeClosed( 1, 9 ).map( i -> 9 - i + 1 ).boxed(), "financial_year_minus_", "" ),
        Collections.singletonList( "financial_year_this" ).toArray() );

    // Generates and array containing "biweek1" -> "biweek26"
    public static final String[] BIWEEKS_LAST_26 = streamToStringArray( IntStream.rangeClosed( 1, 26 ).boxed(),
        "biweek", "" );

    // Generates an array containing "w1" -> "w52"
    public static final String[] WEEKS_LAST_52 = streamToStringArray( IntStream.rangeClosed( 1, 52 ).boxed(), "w", "" );

    // Generates an array containing "w1" -> "w53"
    public static final String[] WEEKS_THIS_YEAR = streamToStringArray( IntStream.rangeClosed( 1, 53 ).boxed(), "w",
        "" );

    private static final int MONTHS_IN_YEAR = 12;

    private int id;

    private boolean thisDay = false;

    private boolean yesterday = false;

    private boolean last3Days = false;

    private boolean last7Days = false;

    private boolean last14Days = false;

    private boolean last30Days = false;

    private boolean last60Days = false;

    private boolean last90Days = false;

    private boolean last180Days = false;

    private boolean thisMonth = false;

    private boolean lastMonth = false;

    private boolean thisBimonth = false;

    private boolean lastBimonth = false;

    private boolean thisQuarter = false;

    private boolean lastQuarter = false;

    private boolean thisSixMonth = false;

    private boolean lastSixMonth = false;

    private boolean weeksThisYear = false;

    private boolean monthsThisYear = false;

    private boolean biMonthsThisYear = false;

    private boolean quartersThisYear = false;

    private boolean thisYear = false;

    private boolean monthsLastYear = false;

    private boolean quartersLastYear = false;

    private boolean lastYear = false;

    private boolean last5Years = false;

    private boolean last10Years = false;

    private boolean last12Months = false;

    private boolean last6Months = false;

    private boolean last3Months = false;

    private boolean last6BiMonths = false;

    private boolean last4Quarters = false;

    private boolean last2SixMonths = false;

    private boolean thisFinancialYear = false;

    private boolean lastFinancialYear = false;

    private boolean last5FinancialYears = false;

    private boolean last10FinancialYears = false;

    private boolean thisWeek = false;

    private boolean lastWeek = false;

    private boolean thisBiWeek = false;

    private boolean lastBiWeek = false;

    private boolean last4Weeks = false;

    private boolean last4BiWeeks = false;

    private boolean last12Weeks = false;

    private boolean last52Weeks = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public RelativePeriods()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Indicates whether this object contains at least one relative period.
     */
    public boolean isEmpty()
    {
        return getRelativePeriods().isEmpty();
    }

    /**
     * Returns the period type for the option with the lowest frequency.
     *
     * @return the period type.
     */
    public PeriodType getPeriodType()
    {
        if ( isThisDay() || isYesterday() || isLast3Days() || isLast7Days() || isLast14Days() ||
            isLast30Days() || isLast60Days() || isLast90Days() || isLast180Days() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.DAILY );
        }

        if ( isThisWeek() || isLastWeek() || isLast4Weeks() || isLast12Weeks() || isLast52Weeks() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.WEEKLY );
        }

        if ( isThisBiWeek() || isLastBiWeek() || isLast4BiWeeks() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.BI_WEEKLY );
        }

        if ( isThisMonth() || isLastMonth() || isLast12Months() || isLast6Months() || isLast3Months() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.MONTHLY );
        }

        if ( isThisBimonth() || isLastBimonth() || isLast6BiMonths() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.BI_MONTHLY );
        }

        if ( isThisQuarter() || isLastQuarter() || isLast4Quarters() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.QUARTERLY );
        }

        if ( isThisSixMonth() || isLastSixMonth() || isLast2SixMonths() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.SIX_MONTHLY );
        }

        if ( isThisFinancialYear() || isLastFinancialYear() || isLast5FinancialYears() || isLast10FinancialYears() )
        {
            return PeriodType.getPeriodType( PeriodTypeEnum.FINANCIAL_OCT );
        }

        return PeriodType.getPeriodType( PeriodTypeEnum.YEARLY );
    }

    /**
     * Gets a list of Periods relative to current date.
     */
    public List<Period> getRelativePeriods()
    {
        return getRelativePeriods( null, null, false, FINANCIAL_YEAR_OCTOBER );
    }

    /**
     * Gets a list of Periods based on the given input and the state of this
     * RelativePeriods. The current date is set to todays date minus one month.
     *
     * @param format the i18n format.
     * @return a list of relative Periods.
     */
    public List<Period> getRelativePeriods( I18nFormat format, boolean dynamicNames )
    {
        return getRelativePeriods( null, format, dynamicNames, FINANCIAL_YEAR_OCTOBER );
    }

    /**
     * Gets a list of Periods based on the given input and the state of this
     * RelativePeriods.
     *
     * @param date the date representing now. If null the current date will be
     *        used.
     * @param format the i18n format.
     * @param financialYearStart the start of a financial year. Configurable
     *        through system settings and should be one of the values in the
     *        enum {@link AnalyticsFinancialYearStartKey}
     * @return a list of relative Periods.
     */
    public List<Period> getRelativePeriods( Date date, I18nFormat format, boolean dynamicNames,
        AnalyticsFinancialYearStartKey financialYearStart )
    {
        date = (date != null) ? date : new Date();

        List<Period> periods = new ArrayList<>();

        if ( isThisFinancialPeriod() )
        {
            FinancialPeriodType financialPeriodType = financialYearStart.getFinancialPeriodType();

            periods.addAll( getRelativeFinancialPeriods( financialPeriodType, format, dynamicNames ) );
        }

        if ( isThisDay() )
        {
            periods.add( getRelativePeriod( new DailyPeriodType(), THISDAY, date, dynamicNames, format ) );
        }

        if ( isYesterday() )
        {
            periods.add( getRelativePeriod( new DailyPeriodType(), YESTERDAY,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ) );
        }

        if ( isLast3Days() )
        {
            periods.addAll( getRollingRelativePeriodList( new DailyPeriodType(), DAYS_IN_YEAR,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ).subList( 362, 365 ) );
        }

        if ( isLast7Days() )
        {
            periods.addAll( getRollingRelativePeriodList( new DailyPeriodType(), DAYS_IN_YEAR,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ).subList( 358, 365 ) );
        }

        if ( isLast14Days() )
        {
            periods.addAll( getRollingRelativePeriodList( new DailyPeriodType(), DAYS_IN_YEAR,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ).subList( 351, 365 ) );
        }

        if ( isLast30Days() )
        {
            periods.addAll( getRollingRelativePeriodList( new DailyPeriodType(), DAYS_IN_YEAR,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ).subList( 335, 365 ) );
        }

        if ( isLast60Days() )
        {
            periods.addAll( getRollingRelativePeriodList( new DailyPeriodType(), DAYS_IN_YEAR,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ).subList( 305, 365 ) );
        }

        if ( isLast90Days() )
        {
            periods.addAll( getRollingRelativePeriodList( new DailyPeriodType(), DAYS_IN_YEAR,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ).subList( 275, 365 ) );
        }

        if ( isLast180Days() )
        {
            periods.addAll( getRollingRelativePeriodList( new DailyPeriodType(), DAYS_IN_YEAR,
                new DateTime( date ).minusDays( 1 ).toDate(), dynamicNames, format ).subList( 185, 365 ) );
        }

        if ( isThisWeek() )
        {
            periods.add( getRelativePeriod( new WeeklyPeriodType(), LAST_WEEK, date, dynamicNames, format ) );
        }

        if ( isLastWeek() )
        {
            periods.add( getRelativePeriod( new WeeklyPeriodType(), LAST_WEEK,
                new DateTime( date ).minusWeeks( 1 ).toDate(), dynamicNames, format ) );
        }

        if ( isThisBiWeek() )
        {
            periods.add( getRelativePeriod( new BiWeeklyPeriodType(), LAST_BIWEEK, date, dynamicNames, format ) );
        }

        if ( isLastBiWeek() )
        {
            periods.add( getRelativePeriod( new BiWeeklyPeriodType(), LAST_BIWEEK,
                new DateTime( date ).minusWeeks( 2 ).toDate(), dynamicNames, format ) );
        }

        if ( isThisMonth() )
        {
            periods.add( getRelativePeriod( new MonthlyPeriodType(), LAST_MONTH, date, dynamicNames, format ) );
        }

        if ( isLastMonth() )
        {
            periods.add( getRelativePeriod( new MonthlyPeriodType(), LAST_MONTH,
                new DateTime( date ).minusMonths( 1 ).toDate(), dynamicNames, format ) );
        }

        if ( isThisBimonth() )
        {
            periods.add( getRelativePeriod( new BiMonthlyPeriodType(), LAST_BIMONTH, date, dynamicNames, format ) );
        }

        if ( isLastBimonth() )
        {
            periods.add( getRelativePeriod( new BiMonthlyPeriodType(), LAST_BIMONTH,
                new DateTime( date ).minusMonths( 2 ).toDate(), dynamicNames, format ) );
        }

        if ( isThisQuarter() )
        {
            periods.add( getRelativePeriod( new QuarterlyPeriodType(), LAST_QUARTER, date, dynamicNames, format ) );
        }

        if ( isLastQuarter() )
        {
            periods.add( getRelativePeriod( new QuarterlyPeriodType(), LAST_QUARTER,
                new DateTime( date ).minusMonths( 3 ).toDate(), dynamicNames, format ) );
        }

        if ( isThisSixMonth() )
        {
            periods.add( getRelativePeriod( new SixMonthlyPeriodType(), LAST_SIXMONTH, date, dynamicNames, format ) );
        }

        if ( isLastSixMonth() )
        {
            periods.add( getRelativePeriod( new SixMonthlyPeriodType(), LAST_SIXMONTH,
                new DateTime( date ).minusMonths( 6 ).toDate(), dynamicNames, format ) );
        }

        if ( isWeeksThisYear() )
        {
            periods
                .addAll( getRelativePeriodList( new WeeklyPeriodType(), WEEKS_THIS_YEAR, date, dynamicNames, format ) );
        }

        if ( isMonthsThisYear() )
        {
            periods.addAll(
                getRelativePeriodList( new MonthlyPeriodType(), MONTHS_THIS_YEAR, date, dynamicNames, format ) );
        }

        if ( isBiMonthsThisYear() )
        {
            periods.addAll(
                getRelativePeriodList( new BiMonthlyPeriodType(), BIMONTHS_THIS_YEAR, date, dynamicNames, format ) );
        }

        if ( isQuartersThisYear() )
        {
            periods.addAll(
                getRelativePeriodList( new QuarterlyPeriodType(), QUARTERS_THIS_YEAR, date, dynamicNames, format ) );
        }

        if ( isThisYear() )
        {
            periods.add( getRelativePeriod( new YearlyPeriodType(), THIS_YEAR, date, dynamicNames, format ) );
        }

        if ( isLast3Months() )
        {
            periods.addAll( getRollingRelativePeriodList( new MonthlyPeriodType(), MONTHS_LAST_12,
                new DateTime( date ).minusMonths( 1 ).toDate(), dynamicNames, format ).subList( 9, 12 ) );
        }

        if ( isLast6Months() )
        {
            periods.addAll( getRollingRelativePeriodList( new MonthlyPeriodType(), MONTHS_LAST_12,
                new DateTime( date ).minusMonths( 1 ).toDate(), dynamicNames, format ).subList( 6, 12 ) );
        }

        if ( isLast12Months() )
        {
            periods.addAll( getRollingRelativePeriodList( new MonthlyPeriodType(), MONTHS_LAST_12,
                new DateTime( date ).minusMonths( 1 ).toDate(), dynamicNames, format ) );
        }

        if ( isLast6BiMonths() )
        {
            periods.addAll( getRollingRelativePeriodList( new BiMonthlyPeriodType(), BIMONTHS_LAST_6,
                new DateTime( date ).minusMonths( 2 ).toDate(), dynamicNames, format ) );
        }

        if ( isLast4Quarters() )
        {
            periods.addAll( getRollingRelativePeriodList( new QuarterlyPeriodType(), QUARTERS_THIS_YEAR,
                new DateTime( date ).minusMonths( 3 ).toDate(), dynamicNames, format ) );
        }

        if ( isLast2SixMonths() )
        {
            periods.addAll( getRollingRelativePeriodList( new SixMonthlyPeriodType(), SIXMONHTS_LAST_2,
                new DateTime( date ).minusMonths( 6 ).toDate(), dynamicNames, format ) );
        }

        if ( isLast4Weeks() )
        {
            periods.addAll( getRollingRelativePeriodList( new WeeklyPeriodType(), WEEKS_LAST_52,
                new DateTime( date ).minusWeeks( 1 ).toDate(), dynamicNames, format ).subList( 48, 52 ) );
        }

        if ( isLast4BiWeeks() )
        {
            periods.addAll( getRollingRelativePeriodList( new BiWeeklyPeriodType(), BIWEEKS_LAST_26,
                new DateTime( date ).minusWeeks( 2 ).toDate(), dynamicNames, format ).subList( 22, 26 ) );
        }

        if ( isLast12Weeks() )
        {
            periods.addAll( getRollingRelativePeriodList( new WeeklyPeriodType(), WEEKS_LAST_52,
                new DateTime( date ).minusWeeks( 1 ).toDate(), dynamicNames, format ).subList( 40, 52 ) );
        }

        if ( isLast52Weeks() )
        {
            periods.addAll( getRollingRelativePeriodList( new WeeklyPeriodType(), WEEKS_LAST_52,
                new DateTime( date ).minusWeeks( 1 ).toDate(), dynamicNames, format ) );
        }

        // Rewind one year
        date = new DateTime( date ).minusMonths( MONTHS_IN_YEAR ).toDate();

        if ( isMonthsLastYear() )
        {
            periods.addAll(
                getRelativePeriodList( new MonthlyPeriodType(), MONTHS_LAST_YEAR, date, dynamicNames, format ) );
        }

        if ( isQuartersLastYear() )
        {
            periods.addAll(
                getRelativePeriodList( new QuarterlyPeriodType(), QUARTERS_LAST_YEAR, date, dynamicNames, format ) );
        }

        if ( isLastYear() )
        {
            periods.add( getRelativePeriod( new YearlyPeriodType(), LAST_YEAR, date, dynamicNames, format ) );
        }

        if ( isLast5Years() )
        {
            periods.addAll(
                getRollingRelativePeriodList( new YearlyPeriodType(), LAST_5_YEARS, date, dynamicNames, format ) );
        }

        if ( isLast10Years() )
        {
            periods.addAll(
                getRollingRelativePeriodList( new YearlyPeriodType(), LAST_10_YEARS,
                    Iso8601Calendar.getInstance().minusYears( DateTimeUnit.fromJdkDate( date ), 5 ).toJdkDate(),
                    dynamicNames, format ) );
            periods.addAll(
                getRollingRelativePeriodList( new YearlyPeriodType(), LAST_10_YEARS, date, dynamicNames, format ) );

        }

        return periods;
    }

    /**
     * Gets a list of financial periods based on the given input and the state
     * of this RelativePeriods.
     *
     * @param financialPeriodType The financial period type to get
     * @param format the i18n format.
     * @return a list of relative Periods.
     */
    private List<Period> getRelativeFinancialPeriods( FinancialPeriodType financialPeriodType, I18nFormat format,
        boolean dynamicNames )
    {
        Date date = new Date();
        List<Period> periods = new ArrayList<>();

        if ( isThisFinancialYear() )
        {
            periods.add( getRelativePeriod( financialPeriodType, THIS_FINANCIAL_YEAR, date, dynamicNames, format ) );
        }

        // Rewind one year
        date = new DateTime( date ).minusMonths( MONTHS_IN_YEAR ).toDate();

        if ( isLastFinancialYear() )
        {
            periods.add( getRelativePeriod( financialPeriodType, LAST_FINANCIAL_YEAR, date, dynamicNames, format ) );
        }

        if ( isLast5FinancialYears() )
        {
            periods.addAll( getRollingRelativePeriodList( financialPeriodType, LAST_5_FINANCIAL_YEARS, date,
                dynamicNames, format ) );
        }

        if ( isLast10FinancialYears() )
        {
            periods.addAll( getRollingRelativePeriodList( financialPeriodType, LAST_10_FINANCIAL_YEARS,
                Iso8601Calendar.getInstance().minusYears( DateTimeUnit.fromJdkDate( date ), 5 ).toJdkDate(),
                dynamicNames, format ) );
            periods.addAll( getRollingRelativePeriodList( financialPeriodType, LAST_10_FINANCIAL_YEARS, date,
                dynamicNames, format ) );
        }

        return periods;
    }

    /**
     * Returns a list of relative periods. The name will be dynamic depending on
     * the dynamicNames argument. The short name will always be dynamic.
     *
     * @param periodType the period type.
     * @param periodNames the array of period names.
     * @param date the current date.
     * @param dynamicNames indication of whether dynamic names should be used.
     * @param format the I18nFormat.
     * @return a list of periods.
     */
    private List<Period> getRelativePeriodList( CalendarPeriodType periodType, String[] periodNames, Date date,
        boolean dynamicNames, I18nFormat format )
    {
        return getRelativePeriodList( periodType.generatePeriods( date ), periodNames, dynamicNames, format );
    }

    /**
     * Returns a list of relative rolling periods. The name will be dynamic
     * depending on the dynamicNames argument. The short name will always be
     * dynamic.
     *
     * @param periodType the period type.
     * @param periodNames the array of period names.
     * @param date the current date.
     * @param dynamicNames indication of whether dynamic names should be used.
     * @param format the I18nFormat.
     * @return a list of periods.
     */
    private List<Period> getRollingRelativePeriodList( CalendarPeriodType periodType, String[] periodNames, Date date,
        boolean dynamicNames, I18nFormat format )
    {
        return getRelativePeriodList( periodType.generateRollingPeriods( date ), periodNames, dynamicNames, format );
    }

    /**
     * Returns a list of relative periods. The name will be dynamic depending on
     * the dynamicNames argument. The short name will always be dynamic.
     *
     * @param relatives the list of periods.
     * @param periodNames the array of period names.
     * @param dynamicNames indication of whether dynamic names should be used.
     * @param format the I18nFormat.
     * @return a list of periods.
     */
    private List<Period> getRelativePeriodList( List<Period> relatives, String[] periodNames, boolean dynamicNames,
        I18nFormat format )
    {
        List<Period> periods = new ArrayList<>();

        int c = 0;

        for ( Period period : relatives )
        {
            periods.add( setName( period, periodNames[c++], dynamicNames, format ) );
        }

        return periods;
    }

    /**
     * Returns relative period. The name will be dynamic depending on the
     * dynamicNames argument. The short name will always be dynamic.
     *
     * @param periodType the period type.
     * @param periodName the period name.
     * @param date the current date.
     * @param dynamicNames indication of whether dynamic names should be used.
     * @param format the I18nFormat.
     * @return a list of periods.
     */
    private Period getRelativePeriod( CalendarPeriodType periodType, String periodName, Date date, boolean dynamicNames,
        I18nFormat format )
    {
        return setName( periodType.createPeriod( date ), periodName, dynamicNames, format );
    }

    /**
     * Sets the name and short name of the given Period. The name will be
     * formatted to the real period name if the given dynamicNames argument is
     * true. The short name will be formatted in any case.
     *
     * @param period the period.
     * @param periodName the period name.
     * @param dynamicNames indication of whether dynamic names should be used.
     * @param format the I18nFormat.
     * @return a period.
     */
    public static Period setName( Period period, String periodName, boolean dynamicNames, I18nFormat format )
    {
        period.setName( dynamicNames && format != null ? format.formatPeriod( period ) : periodName );
        period.setShortName( format != null ? format.formatPeriod( period ) : null );
        return period;
    }

    /**
     * Returns a RelativePeriods instance based on the given
     * RelativePeriodsEnum.
     *
     * @param relativePeriod a list of RelativePeriodsEnum.
     * @param date the relative date to use for generating the relative periods.
     * @return a list of {@link Period}.
     */
    public static List<Period> getRelativePeriodsFromEnum( RelativePeriodEnum relativePeriod, Date date )
    {
        return getRelativePeriodsFromEnum( relativePeriod, date, null, false,
            AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER );
    }

    /**
     * Returns a RelativePeriods instance based on the given
     * RelativePeriodsEnum.
     *
     * @param relativePeriod a list of RelativePeriodsEnum.
     * @param date the relative date to use for generating the relative periods.
     * @param I18nFormat format the {@link I18nFormat}, can be null.
     * @param boolean dynamicNames indicates whether to set dynamic names on the
     *        periods.
     * @param financialYearStart the start of a financial year per
     *        {@link AnalyticsFinancialYearStartKey}.
     * @return a list of {@link Period}.
     */
    public static List<Period> getRelativePeriodsFromEnum( RelativePeriodEnum relativePeriod, Date date,
        I18nFormat format, boolean dynamicNames, AnalyticsFinancialYearStartKey financialYearStart )
    {
        Map<RelativePeriodEnum, RelativePeriods> map = new HashMap<>();

        map.put( RelativePeriodEnum.TODAY, new RelativePeriods().setThisDay( true ) );
        map.put( RelativePeriodEnum.YESTERDAY, new RelativePeriods().setYesterday( true ) );
        map.put( RelativePeriodEnum.LAST_3_DAYS, new RelativePeriods().setLast3Days( true ) );
        map.put( RelativePeriodEnum.LAST_7_DAYS, new RelativePeriods().setLast7Days( true ) );
        map.put( RelativePeriodEnum.LAST_14_DAYS, new RelativePeriods().setLast14Days( true ) );
        map.put( RelativePeriodEnum.LAST_30_DAYS, new RelativePeriods().setLast30Days( true ) );
        map.put( RelativePeriodEnum.LAST_60_DAYS, new RelativePeriods().setLast60Days( true ) );
        map.put( RelativePeriodEnum.LAST_90_DAYS, new RelativePeriods().setLast90Days( true ) );
        map.put( RelativePeriodEnum.LAST_180_DAYS, new RelativePeriods().setLast180Days( true ) );
        map.put( RelativePeriodEnum.THIS_MONTH, new RelativePeriods().setThisMonth( true ) );
        map.put( RelativePeriodEnum.LAST_MONTH, new RelativePeriods().setLastMonth( true ) );
        map.put( RelativePeriodEnum.THIS_BIMONTH, new RelativePeriods().setThisBimonth( true ) );
        map.put( RelativePeriodEnum.LAST_BIMONTH, new RelativePeriods().setLastBimonth( true ) );
        map.put( RelativePeriodEnum.THIS_QUARTER, new RelativePeriods().setThisQuarter( true ) );
        map.put( RelativePeriodEnum.LAST_QUARTER, new RelativePeriods().setLastQuarter( true ) );
        map.put( RelativePeriodEnum.THIS_SIX_MONTH, new RelativePeriods().setThisSixMonth( true ) );
        map.put( RelativePeriodEnum.LAST_SIX_MONTH, new RelativePeriods().setLastSixMonth( true ) );
        map.put( RelativePeriodEnum.WEEKS_THIS_YEAR, new RelativePeriods().setWeeksThisYear( true ) );
        map.put( RelativePeriodEnum.MONTHS_THIS_YEAR, new RelativePeriods().setMonthsThisYear( true ) );
        map.put( RelativePeriodEnum.BIMONTHS_THIS_YEAR, new RelativePeriods().setBiMonthsThisYear( true ) );
        map.put( RelativePeriodEnum.QUARTERS_THIS_YEAR, new RelativePeriods().setQuartersThisYear( true ) );
        map.put( RelativePeriodEnum.THIS_YEAR, new RelativePeriods().setThisYear( true ) );
        map.put( RelativePeriodEnum.MONTHS_LAST_YEAR, new RelativePeriods().setMonthsLastYear( true ) );
        map.put( RelativePeriodEnum.QUARTERS_LAST_YEAR, new RelativePeriods().setQuartersLastYear( true ) );
        map.put( RelativePeriodEnum.LAST_YEAR, new RelativePeriods().setLastYear( true ) );
        map.put( RelativePeriodEnum.LAST_5_YEARS, new RelativePeriods().setLast5Years( true ) );
        map.put( RelativePeriodEnum.LAST_10_YEARS, new RelativePeriods().setLast10Years( true ) );
        map.put( RelativePeriodEnum.LAST_12_MONTHS, new RelativePeriods().setLast12Months( true ) );
        map.put( RelativePeriodEnum.LAST_6_MONTHS, new RelativePeriods().setLast6Months( true ) );
        map.put( RelativePeriodEnum.LAST_3_MONTHS, new RelativePeriods().setLast3Months( true ) );
        map.put( RelativePeriodEnum.LAST_6_BIMONTHS, new RelativePeriods().setLast6BiMonths( true ) );
        map.put( RelativePeriodEnum.LAST_4_QUARTERS, new RelativePeriods().setLast4Quarters( true ) );
        map.put( RelativePeriodEnum.LAST_2_SIXMONTHS, new RelativePeriods().setLast2SixMonths( true ) );
        map.put( RelativePeriodEnum.THIS_FINANCIAL_YEAR, new RelativePeriods().setThisFinancialYear( true ) );
        map.put( RelativePeriodEnum.LAST_FINANCIAL_YEAR, new RelativePeriods().setLastFinancialYear( true ) );
        map.put( RelativePeriodEnum.LAST_5_FINANCIAL_YEARS, new RelativePeriods().setLast5FinancialYears( true ) );
        map.put( RelativePeriodEnum.LAST_10_FINANCIAL_YEARS, new RelativePeriods().setLast10FinancialYears( true ) );
        map.put( RelativePeriodEnum.THIS_WEEK, new RelativePeriods().setThisWeek( true ) );
        map.put( RelativePeriodEnum.LAST_WEEK, new RelativePeriods().setLastWeek( true ) );
        map.put( RelativePeriodEnum.THIS_BIWEEK, new RelativePeriods().setThisBiWeek( true ) );
        map.put( RelativePeriodEnum.LAST_BIWEEK, new RelativePeriods().setLastBiWeek( true ) );
        map.put( RelativePeriodEnum.LAST_4_WEEKS, new RelativePeriods().setLast4Weeks( true ) );
        map.put( RelativePeriodEnum.LAST_4_BIWEEKS, new RelativePeriods().setLast4BiWeeks( true ) );
        map.put( RelativePeriodEnum.LAST_12_WEEKS, new RelativePeriods().setLast12Weeks( true ) );
        map.put( RelativePeriodEnum.LAST_52_WEEKS, new RelativePeriods().setLast52Weeks( true ) );

        return map.containsKey( relativePeriod )
            ? map.get( relativePeriod ).getRelativePeriods( date, format, dynamicNames,
                financialYearStart )
            : new ArrayList<>();
    }

    /**
     * Returns a list of RelativePeriodEnums based on the state of this
     * RelativePeriods.
     *
     * @return a list of RelativePeriodEnums.
     */
    public List<RelativePeriodEnum> getRelativePeriodEnums()
    {
        List<RelativePeriodEnum> list = new ArrayList<>();

        add( list, RelativePeriodEnum.TODAY, thisDay );
        add( list, RelativePeriodEnum.YESTERDAY, yesterday );
        add( list, RelativePeriodEnum.LAST_3_DAYS, last3Days );
        add( list, RelativePeriodEnum.LAST_7_DAYS, last7Days );
        add( list, RelativePeriodEnum.LAST_14_DAYS, last14Days );
        add( list, RelativePeriodEnum.LAST_30_DAYS, last30Days );
        add( list, RelativePeriodEnum.LAST_60_DAYS, last60Days );
        add( list, RelativePeriodEnum.LAST_90_DAYS, last90Days );
        add( list, RelativePeriodEnum.LAST_180_DAYS, last180Days );
        add( list, RelativePeriodEnum.THIS_MONTH, thisMonth );
        add( list, RelativePeriodEnum.LAST_MONTH, lastMonth );
        add( list, RelativePeriodEnum.THIS_BIMONTH, thisBimonth );
        add( list, RelativePeriodEnum.LAST_BIMONTH, lastBimonth );
        add( list, RelativePeriodEnum.THIS_QUARTER, thisQuarter );
        add( list, RelativePeriodEnum.LAST_QUARTER, lastQuarter );
        add( list, RelativePeriodEnum.THIS_SIX_MONTH, thisSixMonth );
        add( list, RelativePeriodEnum.LAST_SIX_MONTH, lastSixMonth );
        add( list, RelativePeriodEnum.WEEKS_THIS_YEAR, weeksThisYear );
        add( list, RelativePeriodEnum.MONTHS_THIS_YEAR, monthsThisYear );
        add( list, RelativePeriodEnum.BIMONTHS_THIS_YEAR, biMonthsThisYear );
        add( list, RelativePeriodEnum.QUARTERS_THIS_YEAR, quartersThisYear );
        add( list, RelativePeriodEnum.THIS_YEAR, thisYear );
        add( list, RelativePeriodEnum.MONTHS_LAST_YEAR, monthsLastYear );
        add( list, RelativePeriodEnum.QUARTERS_LAST_YEAR, quartersLastYear );
        add( list, RelativePeriodEnum.LAST_YEAR, lastYear );
        add( list, RelativePeriodEnum.LAST_5_YEARS, last5Years );
        add( list, RelativePeriodEnum.LAST_10_YEARS, last10Years );
        add( list, RelativePeriodEnum.LAST_12_MONTHS, last12Months );
        add( list, RelativePeriodEnum.LAST_6_MONTHS, last6Months );
        add( list, RelativePeriodEnum.LAST_3_MONTHS, last3Months );
        add( list, RelativePeriodEnum.LAST_6_BIMONTHS, last6BiMonths );
        add( list, RelativePeriodEnum.LAST_4_QUARTERS, last4Quarters );
        add( list, RelativePeriodEnum.LAST_2_SIXMONTHS, last2SixMonths );
        add( list, RelativePeriodEnum.THIS_FINANCIAL_YEAR, thisFinancialYear );
        add( list, RelativePeriodEnum.LAST_FINANCIAL_YEAR, lastFinancialYear );
        add( list, RelativePeriodEnum.LAST_5_FINANCIAL_YEARS, last5FinancialYears );
        add( list, RelativePeriodEnum.LAST_10_FINANCIAL_YEARS, last10FinancialYears );
        add( list, RelativePeriodEnum.THIS_WEEK, thisWeek );
        add( list, RelativePeriodEnum.LAST_WEEK, lastWeek );
        add( list, RelativePeriodEnum.THIS_BIWEEK, thisBiWeek );
        add( list, RelativePeriodEnum.LAST_BIWEEK, lastBiWeek );
        add( list, RelativePeriodEnum.LAST_4_WEEKS, last4Weeks );
        add( list, RelativePeriodEnum.LAST_4_BIWEEKS, last4BiWeeks );
        add( list, RelativePeriodEnum.LAST_12_WEEKS, last12Weeks );
        add( list, RelativePeriodEnum.LAST_52_WEEKS, last52Weeks );

        return list;
    }

    public RelativePeriods setRelativePeriodsFromEnums( List<RelativePeriodEnum> relativePeriods )
    {
        if ( relativePeriods != null )
        {
            thisDay = relativePeriods.contains( RelativePeriodEnum.TODAY );
            yesterday = relativePeriods.contains( RelativePeriodEnum.YESTERDAY );
            last3Days = relativePeriods.contains( RelativePeriodEnum.LAST_3_DAYS );
            last7Days = relativePeriods.contains( RelativePeriodEnum.LAST_7_DAYS );
            last14Days = relativePeriods.contains( RelativePeriodEnum.LAST_14_DAYS );
            last30Days = relativePeriods.contains( RelativePeriodEnum.LAST_30_DAYS );
            last60Days = relativePeriods.contains( RelativePeriodEnum.LAST_60_DAYS );
            last90Days = relativePeriods.contains( RelativePeriodEnum.LAST_90_DAYS );
            last180Days = relativePeriods.contains( RelativePeriodEnum.LAST_180_DAYS );
            thisMonth = relativePeriods.contains( RelativePeriodEnum.THIS_MONTH );
            lastMonth = relativePeriods.contains( RelativePeriodEnum.LAST_MONTH );
            thisBimonth = relativePeriods.contains( RelativePeriodEnum.THIS_BIMONTH );
            lastBimonth = relativePeriods.contains( RelativePeriodEnum.LAST_BIMONTH );
            thisQuarter = relativePeriods.contains( RelativePeriodEnum.THIS_QUARTER );
            lastQuarter = relativePeriods.contains( RelativePeriodEnum.LAST_QUARTER );
            thisSixMonth = relativePeriods.contains( RelativePeriodEnum.THIS_SIX_MONTH );
            lastSixMonth = relativePeriods.contains( RelativePeriodEnum.LAST_SIX_MONTH );
            weeksThisYear = relativePeriods.contains( RelativePeriodEnum.WEEKS_THIS_YEAR );
            monthsThisYear = relativePeriods.contains( RelativePeriodEnum.MONTHS_THIS_YEAR );
            biMonthsThisYear = relativePeriods.contains( RelativePeriodEnum.BIMONTHS_THIS_YEAR );
            quartersThisYear = relativePeriods.contains( RelativePeriodEnum.QUARTERS_THIS_YEAR );
            thisYear = relativePeriods.contains( RelativePeriodEnum.THIS_YEAR );
            monthsLastYear = relativePeriods.contains( RelativePeriodEnum.MONTHS_LAST_YEAR );
            quartersLastYear = relativePeriods.contains( RelativePeriodEnum.QUARTERS_LAST_YEAR );
            lastYear = relativePeriods.contains( RelativePeriodEnum.LAST_YEAR );
            last5Years = relativePeriods.contains( RelativePeriodEnum.LAST_5_YEARS );
            last10Years = relativePeriods.contains( RelativePeriodEnum.LAST_10_YEARS );
            last12Months = relativePeriods.contains( RelativePeriodEnum.LAST_12_MONTHS );
            last6Months = relativePeriods.contains( RelativePeriodEnum.LAST_6_MONTHS );
            last3Months = relativePeriods.contains( RelativePeriodEnum.LAST_3_MONTHS );
            last6BiMonths = relativePeriods.contains( RelativePeriodEnum.LAST_6_BIMONTHS );
            last4Quarters = relativePeriods.contains( RelativePeriodEnum.LAST_4_QUARTERS );
            last2SixMonths = relativePeriods.contains( RelativePeriodEnum.LAST_2_SIXMONTHS );
            thisFinancialYear = relativePeriods.contains( RelativePeriodEnum.THIS_FINANCIAL_YEAR );
            lastFinancialYear = relativePeriods.contains( RelativePeriodEnum.LAST_FINANCIAL_YEAR );
            last5FinancialYears = relativePeriods.contains( RelativePeriodEnum.LAST_5_FINANCIAL_YEARS );
            last10FinancialYears = relativePeriods.contains( RelativePeriodEnum.LAST_10_FINANCIAL_YEARS );
            thisWeek = relativePeriods.contains( RelativePeriodEnum.THIS_WEEK );
            lastWeek = relativePeriods.contains( RelativePeriodEnum.LAST_WEEK );
            thisBiWeek = relativePeriods.contains( RelativePeriodEnum.THIS_BIWEEK );
            lastBiWeek = relativePeriods.contains( RelativePeriodEnum.LAST_BIWEEK );
            last4Weeks = relativePeriods.contains( RelativePeriodEnum.LAST_4_WEEKS );
            last4BiWeeks = relativePeriods.contains( RelativePeriodEnum.LAST_4_BIWEEKS );
            last12Weeks = relativePeriods.contains( RelativePeriodEnum.LAST_12_WEEKS );
            last52Weeks = relativePeriods.contains( RelativePeriodEnum.LAST_52_WEEKS );
        }

        return this;
    }

    // -------------------------------------------------------------------------
    // Getters & setters
    // -------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisDay()
    {
        return thisDay;
    }

    public RelativePeriods setThisDay( boolean thisDay )
    {
        this.thisDay = thisDay;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast3Days()
    {
        return last3Days;
    }

    public RelativePeriods setLast3Days( boolean last3Days )
    {
        this.last3Days = last3Days;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast7Days()
    {
        return last7Days;
    }

    public RelativePeriods setLast7Days( boolean last7Days )
    {
        this.last7Days = last7Days;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast14Days()
    {
        return last14Days;
    }

    public RelativePeriods setLast14Days( boolean last14Days )
    {
        this.last14Days = last14Days;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast30Days()
    {
        return last30Days;
    }

    public RelativePeriods setLast30Days( boolean last30Days )
    {
        this.last30Days = last30Days;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast60Days()
    {
        return last60Days;
    }

    public RelativePeriods setLast60Days( boolean last60Days )
    {
        this.last60Days = last60Days;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast90Days()
    {
        return last90Days;
    }

    public RelativePeriods setLast90Days( boolean last90Days )
    {
        this.last90Days = last90Days;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast180Days()
    {
        return last180Days;
    }

    public RelativePeriods setLast180Days( boolean last180Days )
    {
        this.last180Days = last180Days;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isYesterday()
    {
        return yesterday;
    }

    public RelativePeriods setYesterday( boolean yesterday )
    {
        this.yesterday = yesterday;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisMonth()
    {
        return thisMonth;
    }

    public RelativePeriods setThisMonth( boolean thisMonth )
    {
        this.thisMonth = thisMonth;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastMonth()
    {
        return lastMonth;
    }

    public RelativePeriods setLastMonth( boolean lastMonth )
    {
        this.lastMonth = lastMonth;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisBimonth()
    {
        return thisBimonth;
    }

    public RelativePeriods setThisBimonth( boolean thisBimonth )
    {
        this.thisBimonth = thisBimonth;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastBimonth()
    {
        return lastBimonth;
    }

    public RelativePeriods setLastBimonth( boolean reportingBimonth )
    {
        this.lastBimonth = reportingBimonth;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisQuarter()
    {
        return thisQuarter;
    }

    public RelativePeriods setThisQuarter( boolean thisQuarter )
    {
        this.thisQuarter = thisQuarter;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastQuarter()
    {
        return lastQuarter;
    }

    public RelativePeriods setLastQuarter( boolean reportingQuarter )
    {
        this.lastQuarter = reportingQuarter;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisSixMonth()
    {
        return thisSixMonth;
    }

    public RelativePeriods setThisSixMonth( boolean thisSixMonth )
    {
        this.thisSixMonth = thisSixMonth;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastSixMonth()
    {
        return lastSixMonth;
    }

    public RelativePeriods setLastSixMonth( boolean lastSixMonth )
    {
        this.lastSixMonth = lastSixMonth;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isWeeksThisYear()
    {
        return weeksThisYear;
    }

    public RelativePeriods setWeeksThisYear( boolean weeksThisYear )
    {
        this.weeksThisYear = weeksThisYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isMonthsThisYear()
    {
        return monthsThisYear;
    }

    public RelativePeriods setMonthsThisYear( boolean monthsThisYear )
    {
        this.monthsThisYear = monthsThisYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isBiMonthsThisYear()
    {
        return biMonthsThisYear;
    }

    public RelativePeriods setBiMonthsThisYear( boolean biMonthsThisYear )
    {
        this.biMonthsThisYear = biMonthsThisYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isQuartersThisYear()
    {
        return quartersThisYear;
    }

    public RelativePeriods setQuartersThisYear( boolean quartersThisYear )
    {
        this.quartersThisYear = quartersThisYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisYear()
    {
        return thisYear;
    }

    public RelativePeriods setThisYear( boolean thisYear )
    {
        this.thisYear = thisYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isMonthsLastYear()
    {
        return monthsLastYear;
    }

    public RelativePeriods setMonthsLastYear( boolean monthsLastYear )
    {
        this.monthsLastYear = monthsLastYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isQuartersLastYear()
    {
        return quartersLastYear;
    }

    public RelativePeriods setQuartersLastYear( boolean quartersLastYear )
    {
        this.quartersLastYear = quartersLastYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastYear()
    {
        return lastYear;
    }

    public RelativePeriods setLastYear( boolean lastYear )
    {
        this.lastYear = lastYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast5Years()
    {
        return last5Years;
    }

    public RelativePeriods setLast5Years( boolean last5Years )
    {
        this.last5Years = last5Years;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast10Years()
    {
        return last10Years;
    }

    public RelativePeriods setLast10Years( boolean last10Years )
    {
        this.last10Years = last10Years;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast12Months()
    {
        return last12Months;
    }

    public RelativePeriods setLast12Months( boolean last12Months )
    {
        this.last12Months = last12Months;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast6Months()
    {
        return last6Months;
    }

    public RelativePeriods setLast6Months( boolean last6Months )
    {
        this.last6Months = last6Months;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast3Months()
    {
        return last3Months;
    }

    public RelativePeriods setLast3Months( boolean last3Months )
    {
        this.last3Months = last3Months;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast6BiMonths()
    {
        return last6BiMonths;
    }

    public RelativePeriods setLast6BiMonths( boolean last6BiMonths )
    {
        this.last6BiMonths = last6BiMonths;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast4Quarters()
    {
        return last4Quarters;
    }

    public RelativePeriods setLast4Quarters( boolean last4Quarters )
    {
        this.last4Quarters = last4Quarters;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast2SixMonths()
    {
        return last2SixMonths;
    }

    public RelativePeriods setLast2SixMonths( boolean last2SixMonths )
    {
        this.last2SixMonths = last2SixMonths;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisFinancialYear()
    {
        return thisFinancialYear;
    }

    public RelativePeriods setThisFinancialYear( boolean thisFinancialYear )
    {
        this.thisFinancialYear = thisFinancialYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastFinancialYear()
    {
        return lastFinancialYear;
    }

    public RelativePeriods setLastFinancialYear( boolean lastFinancialYear )
    {
        this.lastFinancialYear = lastFinancialYear;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast5FinancialYears()
    {
        return last5FinancialYears;
    }

    public RelativePeriods setLast5FinancialYears( boolean last5FinancialYears )
    {
        this.last5FinancialYears = last5FinancialYears;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast10FinancialYears()
    {
        return last10FinancialYears;
    }

    public RelativePeriods setLast10FinancialYears( boolean last10FinancialYears )
    {
        this.last10FinancialYears = last10FinancialYears;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisWeek()
    {
        return thisWeek;
    }

    public RelativePeriods setThisWeek( boolean thisWeek )
    {
        this.thisWeek = thisWeek;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastWeek()
    {
        return lastWeek;
    }

    public RelativePeriods setLastWeek( boolean lastWeek )
    {
        this.lastWeek = lastWeek;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isThisBiWeek()
    {
        return thisBiWeek;
    }

    public RelativePeriods setThisBiWeek( boolean thisBiWeek )
    {
        this.thisBiWeek = thisBiWeek;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLastBiWeek()
    {
        return lastBiWeek;
    }

    public RelativePeriods setLastBiWeek( boolean lastBiWeek )
    {
        this.lastBiWeek = lastBiWeek;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast4Weeks()
    {
        return last4Weeks;
    }

    public RelativePeriods setLast4Weeks( boolean last4Weeks )
    {
        this.last4Weeks = last4Weeks;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast4BiWeeks()
    {
        return last4BiWeeks;
    }

    public RelativePeriods setLast4BiWeeks( boolean last4BiWeeks )
    {
        this.last4BiWeeks = last4BiWeeks;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast12Weeks()
    {
        return last12Weeks;
    }

    public RelativePeriods setLast12Weeks( boolean last12Weeks )
    {
        this.last12Weeks = last12Weeks;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLast52Weeks()
    {
        return last52Weeks;
    }

    public RelativePeriods setLast52Weeks( boolean last52Weeks )
    {
        this.last52Weeks = last52Weeks;
        return this;
    }

    public boolean isThisFinancialPeriod()
    {
        return isThisFinancialYear() || isLastFinancialYear() || isLast5FinancialYears() || isLast10FinancialYears();
    }

    // -------------------------------------------------------------------------
    // Equals, hashCode, and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;

        int result = 1;

        result = prime * result + (thisDay ? 1 : 0);
        result = prime * result + (yesterday ? 1 : 0);
        result = prime * result + (last3Days ? 1 : 0);
        result = prime * result + (last7Days ? 1 : 0);
        result = prime * result + (last14Days ? 1 : 0);
        result = prime * result + (last30Days ? 1 : 0);
        result = prime * result + (last60Days ? 1 : 0);
        result = prime * result + (last90Days ? 1 : 0);
        result = prime * result + (last180Days ? 1 : 0);
        result = prime * result + (lastMonth ? 1 : 0);
        result = prime * result + (lastBimonth ? 1 : 0);
        result = prime * result + (lastQuarter ? 1 : 0);
        result = prime * result + (lastSixMonth ? 1 : 0);
        result = prime * result + (weeksThisYear ? 1 : 0);
        result = prime * result + (monthsThisYear ? 1 : 0);
        result = prime * result + (biMonthsThisYear ? 1 : 0);
        result = prime * result + (quartersThisYear ? 1 : 0);
        result = prime * result + (thisYear ? 1 : 0);
        result = prime * result + (monthsLastYear ? 1 : 0);
        result = prime * result + (quartersLastYear ? 1 : 0);
        result = prime * result + (lastYear ? 1 : 0);
        result = prime * result + (last5Years ? 1 : 0);
        result = prime * result + (last10Years ? 1 : 0);
        result = prime * result + (last12Months ? 1 : 0);
        result = prime * result + (last6Months ? 1 : 0);
        result = prime * result + (last3Months ? 1 : 0);
        result = prime * result + (last6BiMonths ? 1 : 0);
        result = prime * result + (last4Quarters ? 1 : 0);
        result = prime * result + (last2SixMonths ? 1 : 0);
        result = prime * result + (thisFinancialYear ? 1 : 0);
        result = prime * result + (lastFinancialYear ? 1 : 0);
        result = prime * result + (last5FinancialYears ? 1 : 0);
        result = prime * result + (last10FinancialYears ? 1 : 0);
        result = prime * result + (last4Weeks ? 1 : 0);
        result = prime * result + (last4BiWeeks ? 1 : 0);
        result = prime * result + (last12Weeks ? 1 : 0);
        result = prime * result + (last52Weeks ? 1 : 0);

        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        final RelativePeriods other = (RelativePeriods) object;

        if ( !thisDay == other.thisDay )
        {
            return false;
        }

        if ( !yesterday == other.yesterday )
        {
            return false;
        }

        if ( !last3Days == other.last3Days )
        {
            return false;
        }

        if ( !last7Days == other.last7Days )
        {
            return false;
        }

        if ( !last14Days == other.last14Days )
        {
            return false;
        }

        if ( !last30Days == other.last30Days )
        {
            return false;
        }

        if ( !last60Days == other.last60Days )
        {
            return false;
        }

        if ( !last90Days == other.last90Days )
        {
            return false;
        }

        if ( !last180Days == other.last180Days )
        {
            return false;
        }

        if ( !lastMonth == other.lastMonth )
        {
            return false;
        }

        if ( !lastBimonth == other.lastBimonth )
        {
            return false;
        }

        if ( !lastQuarter == other.lastQuarter )
        {
            return false;
        }

        if ( !lastSixMonth == other.last2SixMonths )
        {
            return false;
        }

        if ( !weeksThisYear == other.weeksThisYear )
        {
            return false;
        }

        if ( !monthsThisYear == other.monthsThisYear )
        {
            return false;
        }

        if ( !biMonthsThisYear == other.biMonthsThisYear )
        {
            return false;
        }

        if ( !quartersThisYear == other.quartersThisYear )
        {
            return false;
        }

        if ( !thisYear == other.thisYear )
        {
            return false;
        }

        if ( !monthsLastYear == other.monthsLastYear )
        {
            return false;
        }

        if ( !quartersLastYear == other.quartersLastYear )
        {
            return false;
        }

        if ( !lastYear == other.lastYear )
        {
            return false;
        }

        if ( !last5Years == other.last5Years )
        {
            return false;
        }

        if ( !last10Years == other.last10Years )
        {
            return false;
        }

        if ( !last12Months == other.last12Months )
        {
            return false;
        }

        if ( !last6Months == other.last6Months )
        {
            return false;
        }

        if ( !last3Months == other.last3Months )
        {
            return false;
        }

        if ( !last6BiMonths == other.last6BiMonths )
        {
            return false;
        }

        if ( !last4Quarters == other.last4Quarters )
        {
            return false;
        }

        if ( !last2SixMonths == other.last2SixMonths )
        {
            return false;
        }

        if ( !thisFinancialYear == other.thisFinancialYear )
        {
            return false;
        }

        if ( !lastFinancialYear == other.lastFinancialYear )
        {
            return false;
        }

        if ( !last5FinancialYears == other.last5FinancialYears )
        {
            return false;
        }

        if ( !last10FinancialYears == other.last10FinancialYears )
        {
            return false;
        }

        if ( !lastWeek == other.lastWeek )
        {
            return false;
        }

        if ( !lastBiWeek == other.lastBiWeek )
        {
            return false;
        }

        if ( !last4Weeks == other.last4Weeks )
        {
            return false;
        }

        if ( !last4BiWeeks == other.last4BiWeeks )
        {
            return false;
        }

        if ( !last12Weeks == other.last12Weeks )
        {
            return false;
        }

        if ( !last52Weeks == other.last52Weeks )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private static <T> void add( List<T> list, T element, boolean add )
    {
        if ( add )
        {
            list.add( element );
        }
    }

    private static <T> String[] streamToStringArray( Stream<T> stream, String prefix, String suffix )
    {
        return stream.map( o -> prefix + o.toString() + suffix ).toArray( String[]::new );
    }
}
