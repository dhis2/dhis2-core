/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dataitem.query.shared;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasSetPresence;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringNonBlankPresence;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_SHORT_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.IDENTIFIABLE_TOKEN_COMPARISON;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.PROGRAM_ID;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.ROOT_JUNCTION;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.SHORT_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.UID;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.VALUE_TYPES;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_AND;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_OR;

import java.util.Set;

import org.hisp.dhis.common.ValueType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class held common filtering SQL statements for data items.
 *
 * @author maikel arabori
 */
public class FilteringStatement
{
    private static final String EQUALS = " = :";

    private static final String ILIKE = " ilike :";

    private static final String SPACED_LEFT_PARENTHESIS = " ( ";

    private static final String SPACED_RIGHT_PARENTHESIS = " ) ";

    private static final String REGEX_FOR_WORDS_FILTERING = "[\\s@&.?$+-]+";

    private FilteringStatement()
    {
    }

    public static String uidFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringNonBlankPresence( paramsMap, UID ) )
        {
            return SPACED_LEFT_PARENTHESIS + column + EQUALS + UID + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String nameFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + column + ILIKE + NAME + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String nameFiltering( final String columnOne, final String columnTwo,
                                        final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + columnOne + ILIKE + NAME + " or " + columnTwo + ILIKE + NAME
                + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String shortNameFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, SHORT_NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + column + ILIKE + SHORT_NAME + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String shortNameFiltering( final String columnOne, final String columnTwo,
                                             final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, SHORT_NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + columnOne + ILIKE + SHORT_NAME + " or " + columnTwo + ILIKE + SHORT_NAME
                + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String displayNameFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + column + ILIKE + DISPLAY_NAME + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String displayNameFiltering( final String columnOne, final String columnTwo,
                                               final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + columnOne + ILIKE + DISPLAY_NAME + " or " + columnTwo + ILIKE
                + DISPLAY_NAME + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String displayShortNameFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_SHORT_NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + column + ILIKE + DISPLAY_SHORT_NAME + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String displayShortNameFiltering( final String columnOne, final String columnTwo,
                                                    final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_SHORT_NAME ) )
        {
            return SPACED_LEFT_PARENTHESIS + columnOne + ILIKE + DISPLAY_SHORT_NAME + " or " + columnTwo + ILIKE
                + DISPLAY_SHORT_NAME + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static String valueTypeFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasSetPresence( paramsMap, VALUE_TYPES ) )
        {
            return SPACED_LEFT_PARENTHESIS + column + " in (:" + VALUE_TYPES + ")" + SPACED_RIGHT_PARENTHESIS;
        }

        return EMPTY;
    }

    public static boolean skipValueType( final ValueType valueTypeToSkip, final MapSqlParameterSource paramsMap )
    {
        if ( hasSetPresence( paramsMap, VALUE_TYPES ) )
        {
            final Set<String> valueTypeNames = (Set<String>) paramsMap.getValue( VALUE_TYPES );

            // Skip WHEN the value type list does NOT contain the given type.
            // This is mainly used for Indicator's types, as they don't have a
            // value type, but are always interpreted as NUMBER.
            return valueTypeNames != null && !valueTypeNames.contains( valueTypeToSkip.name() );
        }

        return false;
    }

    public static String programIdFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringNonBlankPresence( paramsMap, PROGRAM_ID ) )
        {
            return SPACE + column + EQUALS + PROGRAM_ID + SPACE;
        }

        return EMPTY;
    }

    public static String identifiableTokenFiltering( final String idColumn, final String codeColumn,
                                                     final String displayNameColumn, final String programNameColumn, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringNonBlankPresence( paramsMap, IDENTIFIABLE_TOKEN_COMPARISON ) )
        {
            final String[] filteringWords = defaultIfNull(
                (String) paramsMap.getValue( IDENTIFIABLE_TOKEN_COMPARISON ), EMPTY ).split( "," );

            final OptionalFilterBuilder optionalFilterBuilder = new OptionalFilterBuilder( paramsMap );

            optionalFilterBuilder
                .append( ifAny( createRegexConditionForIdentifier( idColumn, filteringWords, SPACED_OR, ".*" ) ),
                    SPACED_OR )
                .append( ifAny( createRegexConditionForPhrase( codeColumn, filteringWords, SPACED_AND, ".*" ) ),
                    SPACED_OR )
                .append( ifAny( createRegexConditionForPhrase( displayNameColumn, filteringWords, SPACED_AND, ".*" ) ),
                    SPACED_OR )
                .append( ifAny( createRegexConditionForPhrase( programNameColumn, filteringWords, SPACED_AND, ".*" ) ),
                    SPACED_OR );

            return optionalFilterBuilder.toString().replaceFirst( SPACED_OR, EMPTY );
        }

        return EMPTY;
    }

    private static String createRegexConditionForPhrase( final String column, final String[] filteringWords,
                                                         final String spacedAndOr, final String regexMatch )
    {
        if ( filteringWords != null && filteringWords.length > 0 && isNotBlank( column ) )
        {
            final StringBuilder orConditions = new StringBuilder( SPACED_LEFT_PARENTHESIS );

            for ( final String word : filteringWords )
            {
                orConditions
                    .append(
                        "regexp_replace(" + column + ", '" + REGEX_FOR_WORDS_FILTERING + "', ' ', 'g') ~* '"
                            + regexMatch + word + "'"
                            + spacedAndOr );
            }

            orConditions.append( SPACED_RIGHT_PARENTHESIS );

            // Remove last unused AND/OR condition and returns.
            return orConditions.toString().replace( spacedAndOr + SPACED_RIGHT_PARENTHESIS, SPACED_RIGHT_PARENTHESIS );
        }

        return EMPTY;
    }

    private static String createRegexConditionForIdentifier( final String column, final String[] filteringWords,
                                                             final String spacedAndOr, final String regexMatch )
    {
        // Should only trigger when there is no more than one word in the
        // filtering.
        if ( filteringWords != null && filteringWords.length == 1 && isNotBlank( column ) )
        {
            final StringBuilder condition = new StringBuilder( SPACED_LEFT_PARENTHESIS );

            condition
                .append(
                    "regexp_replace(" + column + ", '" + REGEX_FOR_WORDS_FILTERING + "', ' ', 'g') ~* '" + regexMatch
                        + filteringWords[0] + "'" + spacedAndOr );

            condition.append( SPACED_RIGHT_PARENTHESIS );

            // Remove last unused AND/OR condition and returns.
            return condition.toString().replace( spacedAndOr + SPACED_RIGHT_PARENTHESIS, SPACED_RIGHT_PARENTHESIS );
        }

        return EMPTY;
    }

    /**
     * Simple decoration used ONLY to provide better readability.
     *
     * @param anyString
     * @return the exact same value provided as argument.
     */
    public static String ifAny( final String anyString )
    {
        return anyString;
    }

    /**
     * Simple decoration used ONLY to provide better readability.
     *
     * @param anyString
     * @return the exact same value provided as argument.
     */
    public static String ifSet( final String anyString )
    {
        return anyString;
    }

    /**
     * Simple decoration used ONLY to provide better readability.
     *
     * @param anyString
     * @return the exact same value provided as argument.
     */
    public static String always( final String anyString )
    {
        return anyString;
    }

    public static String rootJunction( final MapSqlParameterSource paramsMap )
    {
        final String defaultRootJunction = "and";

        if ( hasStringNonBlankPresence( paramsMap, ROOT_JUNCTION ) )
        {
            return (String) paramsMap.getValue( ROOT_JUNCTION );
        }

        return defaultRootJunction;
    }
}
