package org.hisp.dhis.query;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hisp.dhis.system.util.DateUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class QueryUtils
{
    static public <T> T parseValue( Class<T> klass, Object objectValue )
    {
        return parseValue( klass, null, objectValue );
    }

    @SuppressWarnings( "unchecked" )
    static public <T> T parseValue( Class<T> klass, Class<?> secondaryKlass, Object objectValue )
    {
        if ( klass.isInstance( objectValue ) )
        {
            return (T) objectValue;
        }

        if ( !String.class.isInstance( objectValue ) )
        {
            return (T) objectValue;
        }

        String value = (String) objectValue;

        if ( Integer.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Integer.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Integer`." );
            }
        }
        else if ( Boolean.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Boolean.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Boolean`." );
            }
        }
        else if ( Float.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Float.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Float`." );
            }
        }
        else if ( Double.class.isAssignableFrom( klass ) )
        {
            try
            {
                return (T) Double.valueOf( value );
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Double`." );
            }
        }
        else if ( Date.class.isAssignableFrom( klass ) )
        {
            try
            {
                Date date = DateUtils.parseDate( value );
                return (T) date;
            }
            catch ( Exception ex )
            {
                throw new QueryParserException( "Unable to parse `" + value + "` as `Date`." );
            }
        }
        else if ( Enum.class.isAssignableFrom( klass ) )
        {
            T enumValue = getEnumValue( klass, value );

            if ( enumValue != null )
            {
                return enumValue;
            }
        }
        else if ( Collection.class.isAssignableFrom( klass ) )
        {
            if ( !value.startsWith( "[" ) || !value.endsWith( "]" ) )
            {
                return null;
            }

            String[] split = value.substring( 1, value.length() - 1 ).split( "," );
            List<String> items = Lists.newArrayList( split );

            if ( secondaryKlass != null )
            {
                List<Object> convertedList = new ArrayList<>();

                for ( String item : items )
                {
                    Object convertedValue = parseValue( secondaryKlass, null, item );

                    if ( convertedValue != null )
                    {
                        convertedList.add( convertedValue );
                    }
                }

                return (T) convertedList;
            }

            return (T) items;
        }

        return null;
    }

    /**
     * Try and parse `value` as Enum. Throws `QueryException` if invalid value.
     *
     * @param klass Enum class
     * @param value value
     */
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public static <T> T getEnumValue( Class<T> klass, String value )
    {
        Optional<? extends Enum<?>> enumValue = Enums.getIfPresent( (Class<? extends Enum>) klass, value );

        if ( enumValue.isPresent() )
        {
            return (T) enumValue.get();
        }
        else
        {
            Object[] possibleValues = klass.getEnumConstants();
            throw new QueryParserException( "Unable to parse `" + value + "` as `" + klass + "`, available values are: " + Arrays.toString( possibleValues ) );
        }
    }

    private QueryUtils()
    {
    }

    public static Object parseValue( String value )
    {
        if ( value == null || StringUtils.isEmpty( value ) )
        {
            return null;
        }
        else if ( NumberUtils.isNumber( value ) )
        {
            return value;
        }
        else
        {
            return "'" + value + "'";
        }
    }

    /**
     * Convert a List of select fields into a string as in sql select query.
     *
     * @param fields: list of fields in a select query
     * @return a string which is concat of list fields, separate by comma character.
     * If input is null, return "*" means the query will select all fields.
     */
    public static String parseSelectFields( List<String> fields )
    {
        if ( fields == null || fields.isEmpty() )
        {
            return " * ";
        }
        else
        {
            String str = StringUtils.EMPTY;
            for ( int i = 0; i < fields.size(); i++ )
            {
                str += fields.get( i );
                if ( i < fields.size() - 1 )
                {
                    str += ",";
                }
            }
            return str;
        }
    }


    /**
     * Convert a String with json format [x,y,z] into sql query collection format (x,y,z)
     *
     * @param value a string contains a collection with json format [x,y,z]
     * @return a string contains a collection with sql query format (x,y,z)
     */
    public static String convertCollectionValue( String value )
    {
        if ( StringUtils.isEmpty( value ) )
        {
            throw new QueryParserException( "Value is null" );
        }

        if ( !value.startsWith( "[" ) || !value.endsWith( "]" ) )
        {
            throw new QueryParserException( "Invalid query value" );
        }

        String[] split = value.substring( 1, value.length() - 1 ).split( "," );
        List<String> items = Lists.newArrayList( split );
        String str = "(";

        for ( int i = 0; i < items.size(); i++ )
        {
            Object item = QueryUtils.parseValue( items.get( i ) );
            if ( item != null )
            {
                str += item;
                if ( i < items.size() - 1 )
                {
                    str += ",";
                }
            }
        }

        str += ")";

        return str;
    }


    /**
     * Convert a DHIS2 filter operator into SQl operator
     *
     * @param operator the filter operator of DHIS2
     * @param value    value of the current sql query condition
     * @return a string contains an sql expression with operator and value.
     * Example parseFilterOperator('eq', 5)  will return "=5"
     */
    public static String parseFilterOperator( String operator, String value )
    {

        if ( StringUtils.isEmpty( operator ) )
        {
            throw new QueryParserException( "Filter Operator is null" );
        }

        switch ( operator )
        {
            case "eq":
            {
                return "= " + QueryUtils.parseValue( value );
            }
            case "!eq":
            {
                return "!= " + QueryUtils.parseValue( value );
            }
            case "ne":
            {
                return "!= " + QueryUtils.parseValue( value );
            }
            case "neq":
            {
                return "!= " + QueryUtils.parseValue( value );
            }
            case "gt":
            {
                return "> " + QueryUtils.parseValue( value );
            }
            case "lt":
            {
                return "< " + QueryUtils.parseValue( value );
            }
            case "gte":
            {
                return ">= " + QueryUtils.parseValue( value );
            }
            case "ge":
            {
                return ">= " + QueryUtils.parseValue( value );
            }
            case "lte":
            {
                return "<= " + QueryUtils.parseValue( value );
            }
            case "le":
            {
                return "<= " + QueryUtils.parseValue( value );
            }
            case "like":
            {
                return "like '%" + value + "%'";
            }
            case "!like":
            {
                return "not like '%" + value + "%'";
            }
            case "^like":
            {
                return " like '" + value + "%'";
            }
            case "!^like":
            {
                return " not like '" + value + "%'";
            }
            case "$like":
            {
                return " like '%" + value + "'";
            }
            case "!$like":
            {
                return " not like '%" + value + "'";
            }
            case "ilike":
            {
                return " ilike '%" + value + "%'";
            }
            case "!ilike":
            {
                return " not ilike '%" + value + "%'";
            }
            case "^ilike":
            {
                return " ilike '" + value + "%'";
            }
            case "!^ilike":
            {
                return " not ilike '" + value + "%'";
            }
            case "$ilike":
            {
                return " ilike '%" + value + "'";
            }
            case "!$ilike":
            {
                return " not ilike '%" + value + "'";
            }
            case "in":
            {
                return "in " + QueryUtils.convertCollectionValue( value );
            }
            case "!in":
            {
                return " not in " + QueryUtils.convertCollectionValue( value );
            }
            case "null":
            {
                return "is null";
            }
            case "!null":
            {
                return "is not null";
            }
            default:
            {
                throw new QueryParserException( "`" + operator + "` is not a valid operator." );
            }
        }
    }

}
