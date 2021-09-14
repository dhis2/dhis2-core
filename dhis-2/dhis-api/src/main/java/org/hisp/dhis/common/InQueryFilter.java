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
package org.hisp.dhis.common;

import static org.hisp.dhis.analytics.QueryKey.NV;
import static org.hisp.dhis.common.QueryOperator.IN;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A specialization of {@link QueryFilter} to properly render "in" condition
 *
 * @author Giuseppe Nespolino
 */
public class InQueryFilter extends QueryFilter
{
    private final String field;

    /**
     * Construct a InQueryFilter using field name and the original
     * {@link QueryFilter}
     *
     * @param field the field on which to construct the InQueryFilter
     * @param queryFilter The original {@link QueryFilter}
     */
    public InQueryFilter( String field, QueryFilter queryFilter )
    {
        super( IN, queryFilter.getFilter() );
        this.field = field;
    }

    /**
     * Renders this InQueryFilter into SQL
     *
     * @param encodedFilter actual "in" parameters
     * @return a SQL condition representing this InQueryFilter
     */
    public String getSqlFilter( String encodedFilter, boolean isText )
    {
        List<String> filterItems = getFilterItems( encodedFilter );
        String condition = "";
        if ( hasNonMissingValue( filterItems ) )
        {
            condition = field + " " + operator.getValue() + streamOfNonMissingValues( filterItems )
                .filter( Objects::nonNull )
                .map( item -> toLowerIfNecessary( item, isText ) )
                .map( item -> quoteIfNecessary( item, isText ) )
                .collect( Collectors.joining( ",", " (", ")" ) );
            if ( hasMissingValue( filterItems ) )
            {
                condition = "(" + condition + " or " + field + " is null )";
            }
        }
        else
        {
            if ( hasMissingValue( filterItems ) )
            {
                condition = field + " is null";
            }
        }

        return condition + " ";
    }

    private String quoteIfNecessary( String item, boolean isText )
    {
        return isText ? quote( item ) : item;
    }

    private String toLowerIfNecessary( String item, boolean isText )
    {
        return isText ? item.toLowerCase() : item;
    }

    public String renderSqlFilter( boolean isText, Function<String, String> encoder )
    {
        return this.getSqlFilter( encoder.apply( this.getFilter() ), isText );
    }

    private boolean hasMissingValue( List<String> filterItems )
    {
        return anyMatch( filterItems, this::isMissingItem );
    }

    private Stream<String> streamOfNonMissingValues( List<String> filterItems )
    {
        return filterItems.stream()
            .filter( this::isNotMissingItem );
    }

    private boolean hasNonMissingValue( List<String> filterItems )
    {
        return anyMatch( filterItems, this::isNotMissingItem );
    }

    private boolean anyMatch( List<String> filterItems, Predicate<String> predi )
    {
        return filterItems.stream().anyMatch( predi );
    }

    private boolean isNotMissingItem( String filterItem )
    {
        return !isMissingItem( filterItem );
    }

    private boolean isMissingItem( String filterItem )
    {
        return NV.equals( filterItem );
    }

}
