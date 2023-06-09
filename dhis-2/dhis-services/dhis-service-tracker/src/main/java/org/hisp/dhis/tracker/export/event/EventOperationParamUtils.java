/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.CheckedFunction;

public class EventOperationParamUtils
{

    private EventOperationParamUtils()
    {
        throw new IllegalStateException( "Utility class" );
    }

    private static final String COMPARISON_OPERATOR = EnumSet.allOf( QueryOperator.class ).stream()
        .filter( QueryOperator::isComparison ).map( Enum::toString )
        .collect( Collectors.joining( "|" ) );

    /**
     * For multi operand, we support digits and dates
     * {@link org.hisp.dhis.util.DateUtils}.
     */
    private static final String DIGITS_DATES_VALUES_REG_EX = "[\\s\\d\\-+.:T]+";

    private static final Pattern MULTIPLE_OPERAND_VALUE_REG_EX_PATTERN = Pattern
        .compile( "(?i)(" + COMPARISON_OPERATOR + ")" + DIMENSION_NAME_SEP
            + DIGITS_DATES_VALUES_REG_EX + "(?!" + "(?i)(" + COMPARISON_OPERATOR + ")"
            + ")" );

    private static final String MULTI_OPERAND_VALUE_REG_EX = "(?i)(" + COMPARISON_OPERATOR + ")"
        + DIMENSION_NAME_SEP
        + "(" + DIGITS_DATES_VALUES_REG_EX + ")";

    /**
     * RegEx to search and validate multiple operand
     * {operator}:{value}[:{operator}:{value}], We allow comparison for digits
     * and dates,
     */
    private static final Pattern MULTI_OPERAND_VALUE_PATTERN = Pattern
        .compile(
            MULTI_OPERAND_VALUE_REG_EX
                + DIMENSION_NAME_SEP
                + MULTI_OPERAND_VALUE_REG_EX );

    /**
     * RegEx to validate and match {operator}:{value} in a filter
     */
    private static final String SINGLE_OPERAND_REG_EX = "(?i)("
        + EnumSet.allOf( QueryOperator.class ).stream().map( Enum::toString )
            .collect( Collectors.joining( "|" ) )
        + ")" +
        DIMENSION_NAME_SEP + "(.)+";

    private static final Pattern SINGLE_OPERAND_VALIDATION_PATTERN = Pattern
        .compile( SINGLE_OPERAND_REG_EX );

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

    /**
     * Creates a QueryItem with QueryFilters from the given item string.
     * Expected item format is
     * {identifier}:{operator}:{value}[:{operator}:{value}]. Only the identifier
     * is mandatory. Multiple operator:value pairs are allowed, If is not a
     * multiple operand, a single operator:value filter will be created.
     * Otherwise, the query item is not valid.
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
        int identifierIndex = fullPath.indexOf( DIMENSION_NAME_SEP ) + 1;

        if ( identifierIndex == 0 || fullPath.length() == identifierIndex )
        {
            return map.apply( fullPath.replace( DIMENSION_NAME_SEP, "" ) );
        }

        QueryItem queryItem = map.apply( fullPath.substring( 0, identifierIndex - 1 ) );

        String filter = fullPath.substring( identifierIndex );

        if ( MULTI_OPERAND_VALUE_PATTERN
            .matcher( filter ).matches() )
        {
            Matcher matcher = MULTIPLE_OPERAND_VALUE_REG_EX_PATTERN.matcher( filter );

            while ( matcher.find() )
            {
                queryItem.getFilters().add( singleOperatorValueFilter( matcher.group() ) );
            }

            return queryItem;
        }

        if ( SINGLE_OPERAND_VALIDATION_PATTERN
            .matcher( filter ).matches() )
        {
            queryItem.getFilters().add( singleOperatorValueFilter( filter ) );
        }
        else
        {
            throw new BadRequestException( "Query item or filter is invalid: " + fullPath );
        }

        return queryItem;
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

    private static QueryFilter singleOperatorValueFilter( String operatorValue )
    {
        String[] operatorValueSplit = operatorValue.split( DIMENSION_NAME_SEP, 2 );

        return new QueryFilter( QueryOperator.fromString( operatorValueSplit[0] ), operatorValueSplit[1] );
    }
}
