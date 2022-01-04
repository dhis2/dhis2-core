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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.dimension.DimensionFilters.EMPTY_DATA_DIMENSION_FILTER;
import static org.junit.Assert.assertNull;

import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class DimensionFiltersTest
{

    @Test
    public void testDimensionFilterConstructor()
    {

        DimensionFilters dimensionFilters = DimensionFilters
            .of( "unsupportedField:eq:1;name:unsupportedOperator:3;failingToParseFilter;name:eq:test" );

        assertThat( dimensionFilters.getFilters(), hasSize( 1 ) );

        DimensionFilters anotherDimensionFilters = DimensionFilters
            .of( "unsupportedField:eq:1;name:unsupportedOperator:3;failingToParseFilter;" );

        assertNull( anotherDimensionFilters.getFilters() );
        assertThat( anotherDimensionFilters, is( EMPTY_DATA_DIMENSION_FILTER ) );
    }

    @Test
    public void testFields()
    {
        DimensionFilters dimensionFilters = DimensionFilters
            .of( "name:eq:1;dimensionType:eq:1;displayName:eq:1;displayShortName:eq:1" );

        assertThat( dimensionFilters.getFilters(), hasSize( 4 ) );
    }

    @Test
    public void testNameWithAllOperators()
    {
        Stream.<Pair<String, Function<DimensionResponse.DimensionResponseBuilder, DimensionResponse.DimensionResponseBuilder>>> of(
            Pair.of( "name", dimensionResponseBuilder -> dimensionResponseBuilder.name( "TeSt" ) ),
            Pair.of( "dimensionType", dimensionResponseBuilder -> dimensionResponseBuilder.dimensionType( "TeSt" ) ),
            Pair.of( "displayName", dimensionResponseBuilder -> dimensionResponseBuilder.displayName( "TeSt" ) ),
            Pair.of( "displayShortName",
                dimensionResponseBuilder -> dimensionResponseBuilder.displayShortName( "TeSt" ) ) )
            .forEach( fieldBuilder -> testFieldWithAllOperators( fieldBuilder.getKey(), fieldBuilder.getValue() ) );
    }

    private void testFieldWithAllOperators( String field,
        Function<DimensionResponse.DimensionResponseBuilder, DimensionResponse.DimensionResponseBuilder> dimensionBuilder )
    {

        assertFilter(
            DimensionFilters.of( field + ":eq:TeSt" ),
            dimensionBuilder.apply( DimensionResponse.builder() ).build(),
            Boolean.TRUE );

        assertFilter(
            DimensionFilters.of( field + ":eq:tEsT" ),
            dimensionBuilder.apply( DimensionResponse.builder() ).build(),
            Boolean.TRUE );

        assertFilter(
            DimensionFilters.of( field + ":like:eSt" ),
            dimensionBuilder.apply( DimensionResponse.builder() ).build(),
            Boolean.TRUE );

        assertFilter(
            DimensionFilters.of( field + ":like:eST" ),
            dimensionBuilder.apply( DimensionResponse.builder() ).build(),
            Boolean.FALSE );

        assertFilter(
            DimensionFilters.of( field + ":ilike:eST" ),
            dimensionBuilder.apply( DimensionResponse.builder() ).build(),
            Boolean.TRUE );
    }

    private void assertFilter( DimensionFilters filters, DimensionResponse dimensionResponse, Boolean expectedResponse )
    {
        assertThat( filters.test( dimensionResponse ), is( expectedResponse ) );
    }
}
