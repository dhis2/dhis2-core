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
import java.util.function.Function;
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
import org.hisp.dhis.webapi.common.UID;

/**
 * RequestParamUtils are functions used to parse and transform tracker request
 * parameters. This class is intended to only house functions without any
 * dependencies on services or components.
 */
public class RequestParamUtils
{
    private RequestParamUtils()
    {
        throw new IllegalStateException( "Utility class" );
    }

    private static final char COMMA_SEPARATOR = ',';

    /**
     * Negative lookahead to avoid wrong split when filter value contains colon.
     * It skips colon escaped by slash
     */
    private static final Pattern FILTER_ITEM_SPLIT = Pattern.compile( "(?<!//)" + DIMENSION_NAME_SEP );

    /**
     * Negative lookahead to avoid wrong split of comma-separated list of
     * filters when one or more filter value contain comma. It skips comma
     * escaped by slash
     */
    private static final Pattern FILTER_LIST_SPLIT = Pattern.compile( "(?<!//)" + COMMA_SEPARATOR );

    /**
     * Apply func to given arg only if given arg is not empty otherwise return
     * null.
     *
     * @param func function to be called if arg is not empty
     * @param arg arg to be checked
     * @param <T> base identifiable object to be returned from func
     * @return result of func
     */
    public static <T extends BaseIdentifiableObject> T applyIfNonEmpty( Function<String, T> func, String arg )
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
    public static Set<String> parseAndFilterUids( String input )
    {
        return parseUidString( input )
            .filter( CodeGenerator::isValidUid )
            .collect( Collectors.toSet() );
    }

    /**
     * Helps us transition request parameters that contained semicolon separated
     * UIDs (deprecated) to comma separated UIDs in a backwards compatible way.
     *
     * @param deprecatedParamName request parameter name of deprecated
     *        semi-colon separated parameter
     * @param deprecatedParamUids semicolon separated uids
     * @param newParamName new request parameter replacing deprecated request
     *        parameter
     * @param newParamUids new request parameter uids
     * @return uids from the request parameter containing uids
     * @throws IllegalArgumentException when both deprecated and new request
     *         parameter contain uids
     */
    public static Set<UID> validateDeprecatedUidsParameter( String deprecatedParamName, String deprecatedParamUids,
        String newParamName, Set<UID> newParamUids )
    {
        Set<String> deprecatedParamParsedUids = parseUids( deprecatedParamUids );
        if ( !deprecatedParamParsedUids.isEmpty() && !newParamUids.isEmpty() )
        {
            throw new IllegalArgumentException(
                String.format(
                    "Only one parameter of '%s' (deprecated; semicolon separated UIDs) and '%s' (comma separated UIDs) must be specified. Prefer '%s' as '%s' will be removed.",
                    deprecatedParamName, newParamName, newParamName, deprecatedParamName ) );
        }

        return !deprecatedParamParsedUids.isEmpty()
            ? deprecatedParamParsedUids.stream().map( UID::of ).collect( Collectors.toSet() )
            : newParamUids;
    }

    /**
     * Helps us transition request parameters from a deprecated to a new one.
     *
     * @param deprecatedParamName request parameter name of deprecated parameter
     * @param deprecatedParam value of deprecated request parameter
     * @param newParamName new request parameter replacing deprecated request
     *        parameter
     * @param newParam value of the request parameter
     * @return value of the one request parameter that is non-empty
     * @throws IllegalArgumentException when both deprecated and new request
     *         parameter are non-empty
     */
    public static UID validateDeprecatedUidParameter( String deprecatedParamName, UID deprecatedParam,
        String newParamName, UID newParam )
    {
        if ( newParam != null && deprecatedParam != null )
        {
            throw new IllegalArgumentException(
                String.format(
                    "Only one parameter of '%s' and '%s' must be specified. Prefer '%s' as '%s' will be removed.",
                    deprecatedParamName, newParamName, newParamName, deprecatedParamName ) );
        }

        return newParam != null ? newParam : deprecatedParam;
    }

    /**
     * Parse semicolon separated string of UIDs.
     *
     * @param input string to parse
     * @return set of uids
     */
    public static Set<String> parseUids( String input )
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
     * Parse request parameter to filter tracked entity attributes using UID,
     * operator and values. Refer to
     * {@link #parseQueryItem(String, CheckedFunction)} for details on the
     * expected item format.
     *
     * @param filterItem query item strings each composed of UID, operator and
     *        value
     * @param attributes tracked entity attribute map from UIDs to attributes
     * @return query items each of a tracked entity attribute with attached
     *         query filters
     */
    public static List<QueryItem> parseAttributeQueryItems( String filterItem,
        Map<String, TrackedEntityAttribute> attributes )
        throws BadRequestException
    {
        if ( StringUtils.isEmpty( filterItem ) )
        {
            return List.of();
        }

        String[] uidOperatorValues = FILTER_LIST_SPLIT.split( filterItem );

        List<QueryItem> itemList = new ArrayList<>();
        for ( String uidOperatorValue : uidOperatorValues )
        {
            itemList.add( parseQueryItem( uidOperatorValue, id -> attributeToQueryItem( id, attributes ) ) );
        }

        return itemList;
    }

    /**
     * Parse request parameter to filter data elements using UID, operator and
     * values. Refer to {@link #parseQueryItem(String, CheckedFunction)} for
     * details on the expected item format.
     *
     * @param filterItem query item strings each composed of UID, operator and
     *        value
     * @param uidToQueryItem function to translate the data element UID to a
     *        QueryItem
     * @return query items each of a data element with attached query filters
     */
    public static List<QueryItem> parseDataElementQueryItems( String filterItem,
        CheckedFunction<String, QueryItem> uidToQueryItem )
        throws BadRequestException
    {
        if ( StringUtils.isEmpty( filterItem ) )
        {
            return List.of();
        }

        String[] uidOperatorValues = FILTER_LIST_SPLIT.split( filterItem );

        List<QueryItem> itemList = new ArrayList<>();
        for ( String uidOperatorValue : uidOperatorValues )
        {
            itemList.add( parseQueryItem( uidOperatorValue, uidToQueryItem ) );
        }

        return itemList;
    }

    private static QueryItem attributeToQueryItem( String uid, Map<String, TrackedEntityAttribute> attributes )
        throws BadRequestException
    {
        if ( attributes.isEmpty() )
        {
            throw new BadRequestException( "Attribute does not exist: " + uid );
        }

        TrackedEntityAttribute at = attributes.get( uid );
        if ( at == null )
        {
            throw new BadRequestException( "Attribute does not exist: " + uid );
        }

        return new QueryItem( at, null, at.getValueType(), at.getAggregationType(), at.getOptionSet(), at.isUnique() );
    }

    /**
     * Creates a QueryItem with QueryFilters from the given item string.
     * Expected item format is {uid}:{operator}:{value}[:{operator}:{value}].
     * Only the UID is mandatory. Multiple operator:value pairs are allowed.
     * <p>
     * The UID is passed to given map function which translates UID to a
     * QueryItem. A QueryFilter for each operator:value pair is then added to
     * this QueryItem.
     *
     * @throws BadRequestException filter is neither multiple nor single
     *         operator:value format
     */
    public static QueryItem parseQueryItem( String items, CheckedFunction<String, QueryItem> uidToQueryItem )
        throws BadRequestException
    {
        int uidIndex = items.indexOf( DIMENSION_NAME_SEP ) + 1;

        if ( uidIndex == 0 || items.length() == uidIndex )
        {
            return uidToQueryItem.apply( items.replace( DIMENSION_NAME_SEP, "" ) );
        }

        QueryItem queryItem = uidToQueryItem.apply( items.substring( 0, uidIndex - 1 ) );

        String[] filters = FILTER_ITEM_SPLIT.split( items.substring( uidIndex ) );

        // single operator
        if ( filters.length == 2 )
        {
            queryItem.getFilters()
                .add( operatorValueQueryFilter( filters[0], filters[1], items ) );
        }
        // multiple operator
        else if ( filters.length == 4 )
        {
            for ( int i = 0; i < filters.length; i += 2 )
            {
                queryItem.getFilters()
                    .add( operatorValueQueryFilter( filters[i], filters[i + 1], items ) );
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

        return operatorValueQueryFilter( FILTER_ITEM_SPLIT.split( filter ), filter );
    }

    private static QueryFilter operatorValueQueryFilter( String[] operatorValue, String filter )
        throws BadRequestException
    {
        if ( null == operatorValue || operatorValue.length < 2 )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + filter );
        }

        return operatorValueQueryFilter( operatorValue[0], operatorValue[1], filter );
    }

    private static QueryFilter operatorValueQueryFilter( String operator, String value, String filter )
        throws BadRequestException
    {
        if ( StringUtils.isEmpty( operator ) || StringUtils.isEmpty( value ) )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + filter );
        }

        try
        {
            return new QueryFilter( QueryOperator.fromString( operator ), escapedFilterValue( value ) );

        }
        catch ( IllegalArgumentException exception )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + filter );
        }
    }

    /**
     * Replace escaped comma or colon
     *
     * @param value
     * @return
     */
    private static String escapedFilterValue( String value )
    {
        return value.replace( "//,", "," )
            .replace( "//:", ":" );
    }
}
