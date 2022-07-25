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
package org.hisp.dhis.calendar;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.hisp.dhis.period.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum DateUnitType
{
    DAILY( DailyPeriodType.NAME, List.of( "\\b(\\d{4})(\\d{2})(\\d{2})\\b", "\\b(\\d{4})-(\\d{2})-(\\d{2})\\b" ) ),
    WEEKLY( WeeklyPeriodType.NAME, "\\b(\\d{4})W(\\d[\\d]?)\\b" ),
    WEEKLY_WEDNESDAY( WeeklyWednesdayPeriodType.NAME, "\\b(\\d{4})WedW(\\d[\\d]?)\\b" ),
    WEEKLY_THURSDAY( WeeklyThursdayPeriodType.NAME, "\\b(\\d{4})ThuW(\\d[\\d]?)\\b" ),
    WEEKLY_SATURDAY( WeeklySaturdayPeriodType.NAME, "\\b(\\d{4})SatW(\\d[\\d]?)\\b" ),
    WEEKLY_SUNDAY( WeeklySundayPeriodType.NAME, "\\b(\\d{4})SunW(\\d[\\d]?)\\b" ),
    BI_WEEKLY( BiWeeklyPeriodType.NAME, "\\b(\\d{4})BiW(\\d[\\d]?)\\b" ),
    MONTHLY( MonthlyPeriodType.NAME, "\\b(\\d{4})[-]?(\\d{2})\\b" ),
    BI_MONTHLY( BiMonthlyPeriodType.NAME, "\\b(\\d{4})(\\d{2})B\\b" ),
    QUARTERLY( QuarterlyPeriodType.NAME, "\\b(\\d{4})Q(\\d)\\b" ),
    SIX_MONTHLY( SixMonthlyPeriodType.NAME, "\\b(\\d{4})S(\\d)\\b" ),
    SIX_MONTHLY_APRIL( SixMonthlyAprilPeriodType.NAME, "\\b(\\d{4})AprilS(\\d)\\b" ),
    SIX_MONTHLY_NOVEMBER( SixMonthlyNovemberPeriodType.NAME, "\\b(\\d{4})NovS(\\d)\\b" ),
    YEARLY( YearlyPeriodType.NAME, "\\b(\\d{4})\\b" ),
    FINANCIAL_APRIL( FinancialAprilPeriodType.NAME, "\\b(\\d{4})April\\b" ),
    FINANCIAL_JULY( FinancialJulyPeriodType.NAME, "\\b(\\d{4})July\\b" ),
    FINANCIAL_OCTOBER( FinancialOctoberPeriodType.NAME, "\\b(\\d{4})Oct\\b" ),
    FINANCIAL_NOVEMBER( FinancialNovemberPeriodType.NAME, "\\b(\\d{4})Nov\\b" );

    @Getter
    private final String name;

    @Getter
    private final Collection<Pattern> patterns;

    DateUnitType( String name, String patterns )
    {
        this.name = name;
        this.patterns = List.of( Pattern.compile( patterns ) );
    }

    DateUnitType( String name, Collection<String> patterns )
    {
        this.name = name;
        this.patterns = patterns.stream()
            .map( Pattern::compile )
            .collect( Collectors.toList() );
    }

    public static Optional<DateUnitTypeWithPattern> find( String isoString )
    {
        for ( DateUnitType type : DateUnitType.values() )
        {
            for ( Pattern pattern : type.getPatterns() )
            {
                if ( pattern.matcher( isoString ).matches() )
                {
                    return Optional.of( DateUnitTypeWithPattern.of( type, pattern ) );
                }
            }
        }
        return Optional.empty();
    }

    @Getter
    @AllArgsConstructor( staticName = "of" )
    public static class DateUnitTypeWithPattern
    {
        private final DateUnitType dateUnitType;

        private final Pattern pattern;
    }
}
