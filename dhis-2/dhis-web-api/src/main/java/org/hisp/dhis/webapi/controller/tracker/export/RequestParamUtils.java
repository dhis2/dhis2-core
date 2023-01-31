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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     */
    public static QueryItem parseQueryItem( String item, CheckedFunction<String, QueryItem> map )
        throws BadRequestException
    {
        String[] split = item.split( DimensionalObject.DIMENSION_NAME_SEP );

        if ( split.length % 2 != 1 )
        {
            throw new BadRequestException( "Query item or filter is invalid: " + item );
        }

        QueryItem queryItem = map.apply( split[0] );

        if ( split.length > 1 ) // Filters specified
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                queryItem.getFilters().add( new QueryFilter( operator, split[i + 1] ) );
            }
        }

        return queryItem;
    }
}
