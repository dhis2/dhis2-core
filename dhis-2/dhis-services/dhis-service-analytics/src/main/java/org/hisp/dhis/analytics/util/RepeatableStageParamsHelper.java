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
package org.hisp.dhis.analytics.util;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.exception.InvalidRepeatableStageParamsException;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.period.RelativePeriods;
import org.hisp.dhis.util.DateUtils;

public class RepeatableStageParamsHelper
{
    private static final String SEPARATOR = "~";

    // [-1]
    private static final String PS_INDEX_REGEX = "\\[-?\\d+\\]";

    // [*]
    private static final String PS_ASTERISK_REGEX = "\\[\\*\\]";

    // [1, 2]
    private static final String PS_INDEX_COUNT_REGEX = "\\[-?\\d+" + SEPARATOR + "\\s*\\d+\\]";

    // [1, 2, 2022-01-01, 2022-03-31]
    private static final String PS_INDEX_COUNT_START_DATE_END_DATE_REGEX = "\\[-?\\d+" +
        SEPARATOR + "\\s*\\d+" +
        SEPARATOR + "\\s*\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])" +
        SEPARATOR + "\\s*\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])\\]";

    // [1,30, LAST_YEAR]
    private static final String PS_INDEX_COUNT_RELATIVE_PERIOD_REGEX = "\\[-?\\d+" +
        SEPARATOR + "\\s*\\d+" +
        SEPARATOR + "\\s*\\w+\\]";

    // [2022-01-01, 2022-03-31]
    private static final String PS_START_DATE_END_DATE_REGEX = "\\[\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])"
        +
        SEPARATOR + "\\s*\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])\\]";

    // [LAST_3_MONTHS]
    private static final String PS_RELATIVE_PERIOD_REGEX = "\\[\\w+\\]";

    private static final Pattern[] PS_PARAMS_PATTERN_LIST = {
        Pattern.compile( PS_INDEX_REGEX ),
        Pattern.compile( PS_ASTERISK_REGEX ),
        Pattern.compile( PS_INDEX_COUNT_REGEX ),
        Pattern.compile( PS_INDEX_COUNT_START_DATE_END_DATE_REGEX ),
        Pattern.compile( PS_INDEX_COUNT_RELATIVE_PERIOD_REGEX ),
        Pattern.compile( PS_START_DATE_END_DATE_REGEX ),
        Pattern.compile( PS_RELATIVE_PERIOD_REGEX )
    };

    /**
     * private constructor
     */
    private RepeatableStageParamsHelper()
    {
    }

    /**
     *
     * @param dimension
     * @return RepeatableStageParams
     * @throws InvalidRepeatableStageParamsException
     */
    public static RepeatableStageParams getRepeatableStageParams( String dimension )
        throws InvalidRepeatableStageParamsException
    {
        Pattern pattern = Arrays.stream( PS_PARAMS_PATTERN_LIST )
            .filter( p -> p.matcher( dimension ).find() )
            .findFirst()
            .orElse( Pattern.compile( "" ) );

        Matcher matcher = pattern.matcher( dimension );

        List<String> tokens;

        switch ( pattern.toString() )
        {
            case PS_ASTERISK_REGEX:

                return getRepeatableStageParams( 0, Integer.MAX_VALUE );
            case PS_INDEX_COUNT_REGEX:
                tokens = getMatchedRepeatableStageParamTokens( matcher, 2 );

                return getRepeatableStageParams( Integer.parseInt( tokens.get( 0 ) ),
                    Integer.parseInt( tokens.get( 1 ) ) );
            case PS_INDEX_REGEX:

                return getRepeatableStageParams( Integer.parseInt( getDefaultIndex( matcher ) ), 1 );
            case PS_INDEX_COUNT_START_DATE_END_DATE_REGEX:
                tokens = getMatchedRepeatableStageParamTokens( matcher, 4 );

                return getRepeatableStageParams( Integer.parseInt( tokens.get( 0 ) ),
                    Integer.parseInt( tokens.get( 1 ) ),
                    DateUtils.parseDate( tokens.get( 2 ) ), DateUtils.parseDate( tokens.get( 3 ) ) );
            case PS_INDEX_COUNT_RELATIVE_PERIOD_REGEX:
                tokens = getMatchedRepeatableStageParamTokens( matcher, 3 );

                return getRepeatableStageParams( Integer.parseInt( tokens.get( 0 ) ),
                    Integer.parseInt( tokens.get( 1 ) ),
                    getRelativePeriods( tokens.get( 2 ) ) );
            case PS_START_DATE_END_DATE_REGEX:
                tokens = getMatchedRepeatableStageParamTokens( matcher, 2 );

                return getRepeatableStageParams( DateUtils.parseDate( tokens.get( 0 ) ),
                    DateUtils.parseDate( tokens.get( 1 ).trim() ) );
            case PS_RELATIVE_PERIOD_REGEX:
                tokens = getMatchedRepeatableStageParamTokens( matcher, 1 );

                return getRepeatableStageParams( getRelativePeriods( tokens.get( 0 ) ) );
            default:
                return new RepeatableStageParams();
        }
    }

    /**
     *
     * @param matcher
     * @return the string representation of start index
     * @throws InvalidRepeatableStageParamsException
     */
    private static String getDefaultIndex( Matcher matcher )
        throws InvalidRepeatableStageParamsException
    {
        if ( !matcher.find() )
        {
            throw new InvalidRepeatableStageParamsException();
        }

        return matcher.group( 0 )
            .replace( "[", "" )
            .replace( "]", "" );
    }

    /**
     *
     * @param period
     * @return relative period
     * @throws InvalidRepeatableStageParamsException
     */
    private static List<Period> getRelativePeriods( String period )
        throws InvalidRepeatableStageParamsException
    {
        if ( !RelativePeriodEnum.contains( period ) )
        {
            throw new InvalidRepeatableStageParamsException();
        }

        List<Period> periods = RelativePeriods.getRelativePeriodsFromEnum( RelativePeriodEnum.valueOf( period ),
            new Date() );

        if ( periods.isEmpty() )
        {
            throw new InvalidRepeatableStageParamsException();
        }

        return periods;
    }

    /**
     *
     * @param dimension
     * @return dimension without params like
     *         edqlbukwRfQ[2021-01-01,2022-05-31].vANAXwtLwcT ->
     *         edqlbukwRfQ.vANAXwtLwcT
     */
    public static String removeRepeatableStageParams( String dimension )
    {
        Optional<Pattern> pattern = Arrays.stream( PS_PARAMS_PATTERN_LIST )
            .filter( p -> p.matcher( dimension ).find() )
            .findFirst();

        if ( pattern.isEmpty() )
        {
            return dimension;
        }

        Matcher matcher = pattern.get().matcher( dimension );

        if ( matcher.find() )
        {
            return dimension.replace( matcher.group( 0 ), "" );
        }

        return dimension;
    }

    /**
     *
     * @param startIndex
     * @param count
     * @return RepeatableStageParams instance
     */
    private static RepeatableStageParams getRepeatableStageParams( int startIndex, int count )
    {
        RepeatableStageParams repeatableStageParams = new RepeatableStageParams();
        repeatableStageParams.setStartIndex( startIndex );
        repeatableStageParams.setCount( count );
        repeatableStageParams.setDefaultObject( false );

        return repeatableStageParams;
    }

    /**
     *
     * @param startIndex
     * @param count
     * @param periods
     * @return RepeatableStageParams instance
     * @throws InvalidRepeatableStageParamsException
     */
    private static RepeatableStageParams getRepeatableStageParams( int startIndex, int count, List<Period> periods )
        throws InvalidRepeatableStageParamsException
    {
        if ( periods.isEmpty() )
        {
            throw new InvalidRepeatableStageParamsException();
        }

        return getRepeatableStageParams( startIndex, count, periods.get( 0 ).getStartDate(),
            periods.get( periods.size() - 1 ).getEndDate() );
    }

    /**
     *
     * @param startIndex
     * @param count
     * @param startDate
     * @param endDate
     * @return RepeatableStageParams instance
     */
    private static RepeatableStageParams getRepeatableStageParams( int startIndex, int count, Date startDate,
        Date endDate )
    {
        RepeatableStageParams repeatableStageParams = new RepeatableStageParams();
        repeatableStageParams.setStartIndex( startIndex );
        repeatableStageParams.setCount( count );
        repeatableStageParams.setStartDate( startDate );
        repeatableStageParams.setEndDate( endDate );
        repeatableStageParams.setDefaultObject( false );

        return repeatableStageParams;
    }

    /**
     *
     * @param periods
     * @return RepeatableStageParams instance
     * @throws InvalidRepeatableStageParamsException
     */
    private static RepeatableStageParams getRepeatableStageParams( List<Period> periods )
        throws InvalidRepeatableStageParamsException
    {
        if ( periods.isEmpty() )
        {
            throw new InvalidRepeatableStageParamsException();
        }

        return getRepeatableStageParams( periods.get( 0 ).getStartDate(),
            periods.get( periods.size() - 1 ).getEndDate() );
    }

    /**
     *
     * @param startDate
     * @param endDate
     * @return RepeatableStageParams instance
     */
    private static RepeatableStageParams getRepeatableStageParams( Date startDate, Date endDate )
    {
        RepeatableStageParams repeatableStageParams = new RepeatableStageParams();
        repeatableStageParams.setStartIndex( 0 );
        repeatableStageParams.setCount( Integer.MAX_VALUE );
        repeatableStageParams.setStartDate( startDate );
        repeatableStageParams.setEndDate( endDate );
        repeatableStageParams.setDefaultObject( false );

        return repeatableStageParams;
    }

    /**
     *
     * @param matcher
     * @param expectedTokenCount
     * @return RepeatableStageParams instance
     * @throws InvalidRepeatableStageParamsException
     */
    private static List<String> getMatchedRepeatableStageParamTokens( Matcher matcher, int expectedTokenCount )
        throws InvalidRepeatableStageParamsException
    {
        String params = "0";

        if ( matcher.find() )
        {
            params = matcher.group( 0 )
                .replace( "[", "" )
                .replace( "]", "" );
        }

        String[] tokens = params.split( SEPARATOR );

        if ( tokens.length != expectedTokenCount )
        {
            throw new InvalidRepeatableStageParamsException();
        }

        return Arrays.stream( tokens ).map( String::trim ).collect( Collectors.toList() );
    }
}
