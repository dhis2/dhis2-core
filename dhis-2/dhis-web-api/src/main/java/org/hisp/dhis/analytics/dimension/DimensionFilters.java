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
package org.hisp.dhis.analytics.dimension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hisp.dhis.analytics.event.DimensionWrapper;

import com.google.common.collect.ImmutableMap;

@Getter
@NoArgsConstructor( access = AccessLevel.PRIVATE )
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public class DimensionFilters implements Predicate<DimensionWrapper>
{

    public static final DimensionFilters EMPTY_DATA_DIMENSION_FILTER = new DimensionFilters()
    {
        @Override
        public boolean test( DimensionWrapper dimemsion)
        {
            return true;
        }
    };

    private Collection<SingleFilter> filters;

    public static DimensionFilters of(String filterString )
    {
        if ( Objects.isNull( filterString ) || filterString.trim().equals( "" ) )
        {
            return EMPTY_DATA_DIMENSION_FILTER;
        }
        List<SingleFilter> filters = Arrays.stream( filterString.split( ";" ) )
            .map( String::trim )
            .map( SingleFilter::of )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );

        if ( filters.isEmpty() )
        {
            return EMPTY_DATA_DIMENSION_FILTER;
        }
        return new DimensionFilters( filters );
    }

    @Override
    public boolean test( DimensionWrapper dimemsion )
    {
        return filters.stream().allMatch( filter -> filter.test( dimemsion ) );
    }

    @Getter
    @AllArgsConstructor( access = AccessLevel.PRIVATE )
    private static class SingleFilter implements Predicate<DimensionWrapper>
    {
        private static final Map<String, Function<DimensionWrapper, ?>> FIELD_EXTRACTORS = ImmutableMap.of(
            "name", DimensionWrapper::getName,
            "dimensionType", DimensionWrapper::getDimensionType,
            "displayName", DimensionWrapper::getDisplayName,
            "displayShortName", DimensionWrapper::getDisplayShortName );

        private static final Map<String, BiFunction<String, String, Boolean>> OPERATOR_MAP = ImmutableMap.of(
            "eq", String::equalsIgnoreCase,
            "like", String::contains,
            "ilike", ( fieldValue, value ) -> fieldValue.toLowerCase().contains( value.toLowerCase() ) );

        private String field;

        private String operator;

        private String value;

        private static SingleFilter of( String filter )
        {
            StringTokenizer filterTokenizer = new StringTokenizer( filter, ":" );
            if ( filterTokenizer.countTokens() == 3 )
            {
                String field = filterTokenizer.nextToken();
                String operator = filterTokenizer.nextToken();
                String value = filterTokenizer.nextToken();
                return new SingleFilter( field.trim(), operator.trim(), value.trim() );
            }
            return null;
        }

        @Override
        public boolean test( DimensionWrapper dimension )
        {
            return Optional.ofNullable( FIELD_EXTRACTORS.get( field ) )
                .map( baseDimensionalItemObjectFunction -> baseDimensionalItemObjectFunction.apply( dimension ) )
                .map( Object::toString )
                .map( this::applyOperator )
                .orElse( false );
        }

        private boolean applyOperator( String fieldValue )
        {
            return Optional.ofNullable( OPERATOR_MAP.get( operator ) )
                .map( operation -> operation.apply( fieldValue, value ) )
                .orElse( false );
        }
    }
}
