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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasSetPresence;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.DISPLAY_NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.NAME;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.PROGRAM_ID;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.ROOT_JUNCTION;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.UID;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.VALUE_TYPES;

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

    public static final String ILIKE = " ILIKE :";

    private FilteringStatement()
    {
    }

    public static String uidFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, UID ) )
        {
            return " (" + column + " = :" + UID + ")";
        }

        return EMPTY;
    }

    public static String nameFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, NAME ) )
        {
            return " (" + column + ILIKE + NAME + ")";
        }

        return EMPTY;
    }

    public static String nameFiltering( final String columnOne, final String columnTwo,
        final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, NAME ) )
        {
            return " (" + columnOne + ILIKE + NAME + " OR " + columnTwo + ILIKE + NAME + ")";
        }

        return EMPTY;
    }

    public static String displayFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_NAME ) )
        {
            return " (" + column + ILIKE + DISPLAY_NAME + ")";
        }

        return EMPTY;
    }

    public static String displayFiltering( final String columnOne, final String columnTwo,
        final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, DISPLAY_NAME ) )
        {
            return " (" + columnOne + ILIKE + DISPLAY_NAME + " OR " + columnTwo + ILIKE + DISPLAY_NAME
                + ")";
        }

        return EMPTY;
    }

    public static String valueTypeFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasSetPresence( paramsMap, VALUE_TYPES ) )
        {
            return " (" + column + " IN (:" + VALUE_TYPES + "))";
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
            return !valueTypeNames.contains( valueTypeToSkip.name() );
        }

        return false;
    }

    public static String programIdFiltering( final String column, final MapSqlParameterSource paramsMap )
    {
        if ( hasStringPresence( paramsMap, PROGRAM_ID ) )
        {
            return " " + column + " = :" + PROGRAM_ID;
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
        final String defaultRootJunction = "AND";

        if ( hasStringPresence( paramsMap, ROOT_JUNCTION ) )
        {
            return (String) paramsMap.getValue( ROOT_JUNCTION );
        }

        return defaultRootJunction;
    }
}
