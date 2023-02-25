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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final String OPERATOR_GROUP = EnumSet.allOf( QueryOperator.class ).stream().map( Enum::toString )
        .collect( Collectors.joining( "|" ) );

    private static final Pattern QUERY_FILTER_OPERATOR_PATTERN = Pattern
        .compile( DIMENSION_NAME_SEP + "(?i)("
            + OPERATOR_GROUP
            + ")" + DIMENSION_NAME_SEP );

    /**
     * RegEx to validate {operator}:{value} in a query filter
     */
    private static final String OPERATOR_VALUE_QUERY_FILTER_REG_EX = "(?i)(" + OPERATOR_GROUP + ")" +
        DIMENSION_NAME_SEP + "(.)+";

    private static final Pattern OPERATOR_VALUE_QUERY_FILTER_VALIDATION_PATTERN = Pattern
        .compile( OPERATOR_VALUE_QUERY_FILTER_REG_EX );

    /**
     * Apply func to given arg only if given arg is not empty otherwise return
     * null.
     *
     * @param func function to be called if arg is not empty
     * @param arg arg to be checked
     * @param <T> base identifiable object to be returned from func
     * @return result of func
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
     * @param items query item strings each composed of identifier, operator and
     *        value
     * @param attributes tracked entity attribute map from identifiers to
     *        attributes
     * @return query items each of a tracked entity attribute with attached
     *         query filters
     */
    public static List<QueryItem> parseAttributeQueryItems( Set<String> items,
        Map<String, TrackedEntityAttribute> attributes )
        throws BadRequestException
    {
        List<QueryItem> itemList = new ArrayList<>();
        for ( String item : items )
        {
            itemList.add( parseAttributeQueryItem( item, attributes ) );
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
     * is mandatory. Multiple operator:value pairs are allowed.
     * <p>
     * The identifier is passed to given map function which translates the
     * identifier to a QueryItem. A QueryFilter for each operator:value pair is
     * then added to this QueryItem.
     *
     * @throws BadRequestException given invalid query item
     */
    public static QueryItem parseQueryItem( String fullPath, CheckedFunction<String, QueryItem> map )
        throws BadRequestException
    {
        int identifierIndex = fullPath.indexOf( DIMENSION_NAME_SEP );

        if ( identifierIndex == -1 || fullPath.length() - 1 == identifierIndex )
        {
            return map.apply( fullPath.replace( DIMENSION_NAME_SEP, "" ) );
        }

        QueryItem queryItem = map.apply( fullPath.substring( 0, identifierIndex ) );

        String[] split = QUERY_FILTER_OPERATOR_PATTERN
            .split( fullPath );

        LinkedList<String> values = new LinkedList<>( Arrays.asList( split ).subList( 1, split.length ) );

        Matcher operatorMatcher = QUERY_FILTER_OPERATOR_PATTERN.matcher( fullPath );

        LinkedList<String> operators = new LinkedList<>();

        while ( operatorMatcher.find() )
        {
            operators.add( operatorMatcher.group().replace( DIMENSION_NAME_SEP, "" ) );
        }

        if ( values.isEmpty() || operators.isEmpty() || operators.size() != values.size() )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + fullPath );
        }

        for ( int i = 0; i < operators.size(); i++ )
        {
            queryItem.addFilter( new QueryFilter(
                QueryOperator.fromString( operators.get( i ) ), values.get( i ) ) );
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
    public static QueryFilter parseQueryFilter( String query )
        throws BadRequestException
    {
        if ( query == null || query.isEmpty() )
        {
            return null;
        }

        if ( !query.contains( DimensionalObject.DIMENSION_NAME_SEP ) )
        {
            return new QueryFilter( QueryOperator.EQ, query );
        }

        if ( !OPERATOR_VALUE_QUERY_FILTER_VALIDATION_PATTERN
            .matcher( query ).matches() )
        {
            throw new BadRequestException( "Query has invalid format: " + query );
        }

        String[] operatorValueSplit = query.split( DIMENSION_NAME_SEP, 2 );

        return new QueryFilter( QueryOperator.fromString( operatorValueSplit[0] ), operatorValueSplit[1] );
    }
}
