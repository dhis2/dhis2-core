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

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey.FINANCIAL_YEAR_OCTOBER;

import java.io.Serializable;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.hisp.dhis.analytics.AnalyticsFinancialYearStartKey;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.calendar.impl.Iso8601Calendar;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.i18n.I18nFormat;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@Accessors( chain = true )
@EqualsAndHashCode
@ToString
@JacksonXmlRootElement( localName = "relativePeriods", namespace = DxfNamespaces.DXF_2_0 )
public class RelativePeriods implements Serializable
{
    private static final String THISDAY = "thisDay";

    private static final String YESTERDAY = "yesterday";

    private static final String LAST_WEEK = "last_week";

    private static final String LAST_BIWEEK = "last_biweek";

    private static final String LAST_MONTH = "reporting_month";

    private static final String LAST_BIMONTH = "reporting_bimonth";

    private static final String LAST_QUARTER = "reporting_quarter";

    private static final String LAST_SIXMONTH = "last_sixmonth";

    private static final String THIS_YEAR = "year";

    private static final String LAST_YEAR = "last_year";

    private static final String THIS_FINANCIAL_YEAR = "financial_year";

    private static final String LAST_FINANCIAL_YEAR = "last_financial_year";

    private static final List<String> MONTHS_THIS_YEAR = List.of(
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
        "december" );

    // Generates an array containing Strings "day1" -> "day365"
    private static final List<String> DAYS_IN_YEAR = patternList1ToN( 365, "day%d" );

    // Generates an array containing Strings "january_last_year" ->
    // "december_last_year"
    private static final List<String> MONTHS_LAST_YEAR = MONTHS_THIS_YEAR.stream().map( name -> name + "_last_year" )
        .collect( toUnmodifiableList() );

    // Generates an array containing Strings "month1" -> "month12"
    private static final List<String> MONTHS_LAST_12 = patternList1ToN( 12, "month%d" );

    // Generates an array containing Strings "bimonth1" -> "bimonth6"
    private static final List<String> BIMONTHS_LAST_6 = patternList1ToN( 6, "bimonth%d" );

    // Generates an array containing Strings "bimonth1" -> "bimonth6"
    private static final List<String> BIMONTHS_THIS_YEAR = patternList1ToN( 6, "bimonth%d" );

    // Generates an array containing Strings "quarter1" -> "quarter4"
    private static final List<String> QUARTERS_THIS_YEAR = patternList1ToN( 4, "quarter%d" );

    // Generates an array containing Strings "sixmonth1" and "sixmonth2"
    private static final List<String> SIXMONHTS_LAST_2 = patternList1ToN( 2, "sixmonth%d" );

    // Generates an array containing Strings "quarter1_last_year" ->
    // "quarter4_last_year"
    private static final List<String> QUARTERS_LAST_YEAR = patternList1ToN( 4, "quarter%d_last_year" );

    // Generates an array containing "year_minus_4" -> "year_minus_1" +
    // "year_this"
    private static final List<String> LAST_5_YEARS = patternListNTo1( 4, "year_minus_%d", "year_this" );

    // Generates an array containing "year_minus_9" -> "year_minus_1" +
    // "year_this"
    private static final List<String> LAST_10_YEARS = patternListNTo1( 9, "year_minus_%d", "year_this" );

    // Generates an array containing "financial_year_minus_4" ->
    // "financial_year_minus_1" + "financial_year_this"
    private static final List<String> LAST_5_FINANCIAL_YEARS = patternListNTo1( 4, "financial_year_minus_%d",
        "financial_year_this" );

    // Generates an array containing "financial_year_minus_9" ->
    // "financial_year_minus_1" + "financial_year_this"
    private static final List<String> LAST_10_FINANCIAL_YEARS = patternListNTo1( 9, "financial_year_minus_%d",
        "financial_year_this" );

    // Generates and array containing "biweek1" -> "biweek26"
    private static final List<String> BIWEEKS_LAST_26 = patternList1ToN( 26, "biweek%d" );

    // Generates an array containing "w1" -> "w52"
    private static final List<String> WEEKS_LAST_52 = patternList1ToN( 52, "w%d" );

    // Generates an array containing "w1" -> "w53"
    private static final List<String> WEEKS_THIS_YEAR = patternList1ToN( 53, "w%d" );

    private static final int MONTHS_IN_YEAR = 12;

    private static List<String> patternList1ToN( int n, String pattern )
    {
        return IntStream.rangeClosed( 1, n ).mapToObj( nr -> format( pattern, nr ) )
            .collect( toUnmodifiableList() );
    }

    private static List<String> patternListNTo1( int n, String pattern, String specialValue )
    {
        return Stream.concat(
            IntStream.rangeClosed( 1, n ).map( i -> n - i + 1 ).mapToObj( nr -> format( pattern, nr ) ),
            Stream.of( specialValue ) )
            .collect( toUnmodifiableList() );
    }

    @EqualsAndHashCode.Exclude
    private int id;

    @JsonProperty
    private boolean thisDay = false;

    @JsonProperty
    private boolean yesterday = false;

    @JsonProperty
    private boolean last3Days = false;

    @JsonProperty
    private boolean last7Days = false;

    @JsonProperty
    private boolean last14Days = false;

    @JsonProperty
    private boolean last30Days = false;

    @JsonProperty
    private boolean last60Days = false;

    @JsonProperty
    private boolean last90Days = false;

    @JsonProperty
    private boolean last180Days = false;

    @JsonProperty
    private boolean thisMonth = false;

    @JsonProperty
    private boolean lastMonth = false;

    @JsonProperty
    private boolean thisBimonth = false;

    @JsonProperty
    private boolean lastBimonth = false;

    @JsonProperty
    private boolean thisQuarter = false;

    @JsonProperty
    private boolean lastQuarter = false;

    @JsonProperty
    private boolean thisSixMonth = false;

    @JsonProperty
    private boolean lastSixMonth = false;

    @JsonProperty
    private boolean weeksThisYear = false;

    @JsonProperty
    private boolean monthsThisYear = false;

    @JsonProperty
    private boolean biMonthsThisYear = false;

    @JsonProperty
    private boolean quartersThisYear = false;

    @JsonProperty
    private boolean thisYear = false;

    @JsonProperty
    private boolean monthsLastYear = false;

    @JsonProperty
    private boolean quartersLastYear = false;

    @JsonProperty
    private boolean lastYear = false;

    @JsonProperty
    private boolean last5Years = false;

    @JsonProperty
    private boolean last10Years = false;

    @JsonProperty
    private boolean last12Months = false;

    @JsonProperty
    private boolean last6Months = false;

    @JsonProperty
    private boolean last3Months = false;

    @JsonProperty
    private boolean last6BiMonths = false;

    @JsonProperty
    private boolean last4Quarters = false;

    @JsonProperty
    private boolean last2SixMonths = false;

    @JsonProperty
    private boolean thisFinancialYear = false;

    @JsonProperty
    private boolean lastFinancialYear = false;

    @JsonProperty
    private boolean last5FinancialYears = false;

    @JsonProperty
    private boolean last10FinancialYears = false;

    @JsonProperty
    private boolean thisWeek = false;

    @JsonProperty
    private boolean lastWeek = false;

    @JsonProperty
    private boolean thisBiWeek = false;

    @JsonProperty
    private boolean lastBiWeek = false;

    @JsonProperty
    private boolean last4Weeks = false;

    @JsonProperty
    private boolean last4BiWeeks = false;

    @JsonProperty
    private boolean last12Weeks = false;

    @JsonProperty
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
    private List<Period> getRelativePeriodList( CalendarPeriodType periodType, List<String> periodNames, Date date,
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
    private List<Period> getRollingRelativePeriodList( CalendarPeriodType periodType, List<String> periodNames,
        Date date,
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
    private List<Period> getRelativePeriodList( List<Period> relatives, List<String> periodNames, boolean dynamicNames,
        I18nFormat format )
    {
        List<Period> periods = new ArrayList<>();

        int c = 0;

        for ( Period period : relatives )
        {
            periods.add( setName( period, periodNames.get( c++ ), dynamicNames, format ) );
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
     * @param format the {@link I18nFormat}, can be null.
     * @param dynamicNames indicates whether to set dynamic names on the
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

    public boolean isThisFinancialPeriod()
    {
        return isThisFinancialYear() || isLastFinancialYear() || isLast5FinancialYears() || isLast10FinancialYears();
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
}
