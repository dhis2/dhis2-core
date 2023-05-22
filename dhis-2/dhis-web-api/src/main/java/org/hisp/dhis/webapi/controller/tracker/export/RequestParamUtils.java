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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.CheckedFunction;

/**
 * RequestParamUtils are functions used to parse and transform tracker request
 * parameters. This class is intended to only house functions without any
 * dependencies on services or components.
 */
class RequestParamUtils
{
    private RequestParamUtils()
    {
        throw new IllegalStateException( "Utility class" );
    }

    private static final String FILTER_ITEM_SPLIT = "(?<!\\\\)" + DIMENSION_NAME_SEP;

    private static final String FILTER_LIST_SPLIT = "(?<!\\\\),";

    /**
     * Apply func to given arg only if given arg is not empty otherwise return
     * null.
     *
     * @param func function to be called if arg is not empty
     * @param arg arg to be checked
     * @return result of func
     * @param <T> base identifiable object to be returned from func
     */
    static <T extends BaseIdentifiableObject> T applyIfNonEmpty( Function<String, T> func, String arg )
    {
        if ( StringUtils.isEmpty( arg ) )
        {
            return null;
        }

        return func.apply( arg );
    }

    /**
     * Parse semicolon separated string of UIDs. Filters out invalid UIDs.
     *
     * @param input string to parse
     * @return set of uids
     */
    static Set<String> parseAndFilterUids( String input )
    {
        return parseUidString( input )
            .filter( CodeGenerator::isValidUid )
            .collect( Collectors.toSet() );
    }

    /**
     * Parse semicolon separated string of UIDs.
     *
     * @param input string to parse
     * @return set of uids
     */
    static Set<String> parseUids( String input )
    {
        return parseUidString( input )
            .collect( Collectors.toSet() );
    }

    private static Stream<String> parseUidString( String input )
    {
        return CollectionUtils.emptyIfNull( TextUtils.splitToSet( input, TextUtils.SEMICOLON ) )
            .stream();
    }

    /**
     * Parse request parameter to filter tracked entity attributes using
     * identifier, operator and values. Refer to
     * {@link #parseQueryItem(String, CheckedFunction)} for details on the
     * expected item format.
     *
     * @param queryItem query item strings each composed of identifier, operator
     *        and value
     * @param attributes tracked entity attribute map from identifiers to
     *        attributes
     * @return query items each of a tracked entity attribute with attached
     *         query filters
     */
    public static List<QueryItem> parseAttributeQueryItems( String queryItem,
        Map<String, TrackedEntityAttribute> attributes )
        throws BadRequestException
    {
        if ( StringUtils.isEmpty( queryItem ) )
        {
            return List.of();
        }

        String[] idOperatorValues = queryItem.split( FILTER_LIST_SPLIT );

        List<QueryItem> itemList = new ArrayList<>();
        for ( String idOperatorValue : idOperatorValues )
        {
            itemList.add( parseAttributeQueryItem( idOperatorValue, attributes ) );
        }

        return itemList;
    }

    /**
     * Parse request parameter to filter tracked entity attributes using
     * identifier, operator and values. Refer to
     * {@link #parseQueryItem(String, CheckedFunction)} for details on the
     * expected item format.
     *
     * @param item query item string composed of identifier, operator and value
     * @param attributes tracked entity attribute map from identifiers to
     *        attributes
     * @return query item of tracked entity attribute with attached query
     *         filters
     */

    public static QueryItem parseAttributeQueryItem( String item, Map<String, TrackedEntityAttribute> attributes )
        throws BadRequestException
    {
        return parseQueryItem( item, id -> attributeToQueryItem( id, attributes ) );
    }

    private static QueryItem attributeToQueryItem( String identifier, Map<String, TrackedEntityAttribute> attributes )
        throws BadRequestException
    {
        if ( attributes.isEmpty() )
        {
            throw new BadRequestException( "Attribute does not exist: " + identifier );
        }

        TrackedEntityAttribute at = attributes.get( identifier );
        if ( at == null )
        {
            throw new BadRequestException( "Attribute does not exist: " + identifier );
        }

        return new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(), at.isUnique() );
    }

    /**
     * Creates a QueryItem with QueryFilters from the given item string.
     * Expected item format is
     * {identifier}:{operator}:{value}[:{operator}:{value}]. Only the identifier
     * is mandatory. Multiple operator:value pairs are allowed, If is not a
     * multiple or single operator the query item is not valid.
     * <p>
     * The identifier is passed to given map function which translates the
     * identifier to a QueryItem. A QueryFilter for each operator:value pair is
     * then added to this QueryItem.
     *
     * @throws BadRequestException given invalid query item
     */
    public static QueryItem parseQueryItem( String items, CheckedFunction<String, QueryItem> map )
        throws BadRequestException
    {
        int identifierIndex = items.indexOf( DIMENSION_NAME_SEP ) + 1;

        if ( identifierIndex == 0 || items.length() == identifierIndex )
        {
            return map.apply( items.replace( DIMENSION_NAME_SEP, "" ) );
        }

        QueryItem queryItem = map.apply( items.substring( 0, identifierIndex - 1 ) );

        String[] filters = items.substring( identifierIndex ).split( FILTER_ITEM_SPLIT );

        // single operator
        if ( filters.length == 2 )
        {
            queryItem.getFilters()
                .add( parseSingleOperatorValueFilter( filters[0], filters[1], items ) );
        }
        // multiple operator
        else if ( filters.length == 4 )
        {
            for ( int i = 0; i < filters.length; i += 2 )
            {
                queryItem.getFilters()
                    .add( parseSingleOperatorValueFilter( filters[i], filters[i + 1], items ) );
            }
        }
        else
        {
            throw new BadRequestException( "Query item or filter is invalid: " + items );
        }

        return queryItem;
    }

    /**
     * Creates a QueryFilter from the given query string. Query is on format
     * {operator}:{filter-value}. Only the filter-value is mandatory. The EQ
     * QueryOperator is used as operator if not specified. We split the query at
     * the first delimiter, so the filter value can be any sequence of
     * characters
     *
     * @throws BadRequestException given invalid query string
     */
    public static QueryFilter parseQueryFilter( String filter )
        throws BadRequestException
    {
        if ( StringUtils.isEmpty( filter ) )
        {
            return null;
        }

        if ( !filter.contains( DimensionalObject.DIMENSION_NAME_SEP ) )
        {
            return new QueryFilter( QueryOperator.EQ, filter );
        }

        return parseSingleOperatorValueFilter( filter.split( FILTER_ITEM_SPLIT ), filter );
    }

    private static QueryFilter parseSingleOperatorValueFilter( String[] operatorValue, String filter )
        throws BadRequestException
    {
        if ( null == operatorValue || operatorValue.length < 2 )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + filter );
        }

        return parseSingleOperatorValueFilter( operatorValue[0], operatorValue[1], filter );
    }

    private static QueryFilter parseSingleOperatorValueFilter( String operator, String value, String filter )
        throws BadRequestException
    {
        if ( StringUtils.isEmpty( operator ) || StringUtils.isEmpty( value ) )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + filter );
        }

        try
        {
            return new QueryFilter( queryOperator( operator ), escapedFilterValue( value ) );

        }
        catch ( Exception exception )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + filter );
        }
    }

    /**
     * Escapes colon in the input value and reconstruct the value
     *
     * @param value
     * @return
     */
    private static String escapedFilterValue( String value )
    {
        Stack<Character> stack = new Stack<>();

        for ( int i = 0; i < value.length(); i++ )
        {
            if ( i == value.length() - 1
                || !(value.charAt( i ) == '\\' && value.charAt( i + 1 ) == DIMENSION_NAME_SEP.charAt( 0 )) )
            {
                stack.add( value.charAt( i ) );
            }
        }

        return stack.stream().map( Object::toString ).collect( Collectors.joining( "" ) );
    }

    private static QueryOperator queryOperator(
        String itemOperator )
    {
        return QueryOperator.fromString( itemOperator );
    }
}
