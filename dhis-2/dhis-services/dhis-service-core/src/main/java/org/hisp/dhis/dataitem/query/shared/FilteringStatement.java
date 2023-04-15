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
package org.hisp.dhis.dataitem.query.shared;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasSetPresence;
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
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_LEFT_PARENTHESIS;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_OR;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_RIGHT_PARENTHESIS;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.equalsFiltering;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.ilikeFiltering;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.ilikeOrFiltering;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.inFiltering;

import lombok.NoArgsConstructor;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class held common filtering SQL statements for data items.
 *
 * @author maikel arabori
 */
@NoArgsConstructor( access = PRIVATE )
public class FilteringStatement
{
    private static final String REGEX_FOR_WORDS_FILTERING = "[\\s@&.?$+-]+";

    /**
     * Returns a SQL string related to UID equality to be reused as part of data
     * items UID filtering.
     *
     * @param column the uid column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String uidFiltering( String column, MapSqlParameterSource paramsMap )
    {
        if ( hasNonBlankStringPresence( paramsMap, UID ) )
        {
            return equalsFiltering( column, UID );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to programId equality to be reused as part
     * of data items programId filtering.
     *
     * @param column the uid column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String programIdFiltering( String column, MapSqlParameterSource paramsMap )
    {
        if ( hasNonBlankStringPresence( paramsMap, PROGRAM_ID ) )
        {
            return equalsFiltering( column, PROGRAM_ID );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'name' "ilike" comparison to be reused as
     * part of data items 'name' filtering.
     *
     * @param column the name column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String nameFiltering( String column, MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, NAME ) )
        {
            return ilikeFiltering( column, NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'name' "ilike" comparison to be reused as
     * part of data items 'name' filtering. It required two columns so it can
     * compare two different names. It will always use 'or' condition, which
     * translates to "columnOne ilike :name OR columnTwo ilike :name".
     *
     * @param columnOne the name's first column
     * @param columnTwo the name's second column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String nameFiltering( String columnOne, String columnTwo, MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, NAME ) )
        {
            return ilikeOrFiltering( columnOne, columnTwo, NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'shortName' "ilike" comparison to be
     * reused as part of data items 'shortName' filtering.
     *
     * @param column the shortName column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String shortNameFiltering( String column, MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, SHORT_NAME ) )
        {
            return ilikeFiltering( column, SHORT_NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'shortName' "ilike" comparison to be
     * reused as part of data items 'shortName' filtering. It required two
     * columns so it can compare two different shortNames. It will always use
     * 'or' condition, which translates to "columnOne ilike :shortName OR
     * columnTwo ilike :shortName".
     *
     * @param columnOne the shortname's first column
     * @param columnTwo the shortname's second column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String shortNameFiltering( String columnOne, String columnTwo, MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, SHORT_NAME ) )
        {
            return ilikeOrFiltering( columnOne, columnTwo, SHORT_NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'displayname' "ilike" comparison to be
     * reused as part of data items 'displayname' filtering.
     *
     * @param column the displayname column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String displayNameFiltering( String column, MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_NAME ) )
        {
            return ilikeFiltering( column, DISPLAY_NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'displayName' "ilike" comparison to be
     * reused as part of data items 'displayName' filtering. It required two
     * columns so it can compare two different displayNames. It will always use
     * 'or' condition, which translates to "columnOne ilike :displayName OR
     * columnTwo ilike :displayName".
     *
     * @param columnOne the displayName's first column
     * @param columnTwo the displayName's second column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String displayNameFiltering( String columnOne, String columnTwo, MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_NAME ) )
        {
            return ilikeOrFiltering( columnOne, columnTwo, DISPLAY_NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'displayShortName' "ilike" comparison to
     * be reused as part of data items 'displayShortName' filtering.
     *
     * @param column the displayShortName column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String displayShortNameFiltering( String column, MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_SHORT_NAME ) )
        {
            return ilikeFiltering( column, DISPLAY_SHORT_NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'displayShortName' "ilike" comparison to
     * be reused as part of data items 'displayShortName' filtering. It required
     * two columns so it can compare two different displayShortNames. It will
     * always use 'or' condition, which translates to "columnOne ilike
     * :displayShortName OR columnTwo ilike :displayShortName".
     *
     * @param columnOne the displayShortName's first column
     * @param columnTwo the displayShortName's second column
     * @param paramsMap
     * @return the uid SQL comparison
     */
    public static String displayShortNameFiltering( String columnOne, String columnTwo,
        MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_SHORT_NAME ) )
        {
            return ilikeOrFiltering( columnOne, columnTwo, DISPLAY_SHORT_NAME );
        }

        return EMPTY;
    }

    /**
     * Returns a SQL string related to 'valueType' "in" filtering to be reused
     * as part of data items 'valueType' filtering.
     *
     * @param column the valueType column
     * @param paramsMap
     * @return the "in" SQL statement
     */
    public static String valueTypeFiltering( String column, MapSqlParameterSource paramsMap )
    {
        if ( hasSetPresence( paramsMap, VALUE_TYPES ) )
        {
            return inFiltering( column, VALUE_TYPES );
        }

        return EMPTY;
    }

    /**
     * Builds a valid SQL string based on a given identifiable token, so it can
     * be used as part of a SQL query.
     *
     * @param idColumn
     * @param codeColumn
     * @param displayNameColumn
     * @param programNameColumn
     * @param paramsMap
     * @return the SQL string
     */
    public static String identifiableTokenFiltering( String idColumn, String codeColumn, String displayNameColumn,
        String programNameColumn, MapSqlParameterSource paramsMap )
    {
        if ( hasNonBlankStringPresence( paramsMap, IDENTIFIABLE_TOKEN_COMPARISON ) )
        {
            String[] filteringWords = defaultIfNull(
                (String) paramsMap.getValue( IDENTIFIABLE_TOKEN_COMPARISON ), EMPTY ).split( "," );

            OptionalFilterBuilder optionalFilterBuilder = new OptionalFilterBuilder( paramsMap );

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

    private static String createRegexConditionForPhrase( String column, String[] filteringWords,
        String spacedAndOr, String regexMatch )
    {
        if ( filteringWords != null && filteringWords.length > 0 && isNotBlank( column ) )
        {
            StringBuilder orConditions = new StringBuilder( SPACED_LEFT_PARENTHESIS );

            for ( String word : filteringWords )
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

    private static String createRegexConditionForIdentifier( String column, String[] filteringWords,
        String spacedAndOr, String regexMatch )
    {
        // Should only trigger when there is no more than one word in the
        // filtering.
        if ( filteringWords != null && filteringWords.length == 1 && isNotBlank( column ) )
        {
            StringBuilder condition = new StringBuilder( SPACED_LEFT_PARENTHESIS );

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
    public static String ifAny( String anyString )
    {
        return anyString;
    }

    /**
     * Simple decoration used ONLY to provide better readability.
     *
     * @param anyString
     * @return the exact same value provided as argument.
     */
    public static String ifSet( String anyString )
    {
        return anyString;
    }

    /**
     * Simple decoration used ONLY to provide better readability.
     *
     * @param anyString
     * @return the exact same value provided as argument.
     */
    public static String always( String anyString )
    {
        return anyString;
    }

    /**
     * Simply returns the "root junction" set in the paramsMap to be used for
     * filtering purposes.
     *
     * @param paramsMap
     * @return the "root junction" ('and' OR 'or')
     */
    public static String rootJunction( MapSqlParameterSource paramsMap )
    {
        String defaultRootJunction = "and";

        if ( hasNonBlankStringPresence( paramsMap, ROOT_JUNCTION ) )
        {
            return (String) paramsMap.getValue( ROOT_JUNCTION );
        }

        return defaultRootJunction;
    }
}
