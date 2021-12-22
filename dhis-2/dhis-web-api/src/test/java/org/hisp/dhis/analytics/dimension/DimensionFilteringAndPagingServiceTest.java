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
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hisp.dhis.common.DimensionsCriteria;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class DimensionFilteringAndPagingServiceTest
{

    private final FieldFilterService fieldFilterService = mock( FieldFilterService.class );

    private final DimensionMapperService dimensionMapperService = mock( DimensionMapperService.class );

    private DimensionFilteringAndPagingService service;

    @Before
    public void setup()
    {
        service = new DimensionFilteringAndPagingService( fieldFilterService, dimensionMapperService );

        List<DimensionResponse> dimensionResponses = IntStream.rangeClosed( 1, 10 )
            .mapToObj( this::buildDimensionResponse )
            .collect( Collectors.toList() );

        when( dimensionMapperService.toDimensionResponse( any() ) ).thenReturn( dimensionResponses );
        when( fieldFilterService.toObjectNodes( any() ) )
            .thenAnswer( invocationOnMock -> {
                FieldFilterParams<DimensionResponse> argument = invocationOnMock.getArgument( 0 );
                return argument.getObjects();
            } );
    }

    @Test
    public void testPaging()
    {
        DimensionsCriteria criteria = new DimensionsCriteria();
        criteria.setPageSize( 5 );

        PagingWrapper<ObjectNode> pagingWrapper = service.pageAndFilter(
            Collections.emptyList(),
            criteria,
            Collections.singletonList( "*" ) );

        assertThat( pagingWrapper.getElements().get( "dimensions" ).size(), is( 5 ) );
    }

    @Test
    public void testFiltering()
    {
        DimensionsCriteria criteria = new DimensionsCriteria();
        criteria.setFilter( "name:eq:test" );

        PagingWrapper<ObjectNode> pagingWrapper = service.pageAndFilter(
            Collections.emptyList(),
            criteria,
            Collections.singletonList( "*" ) );

        assertThat( pagingWrapper.getElements().get( "dimensions" ).size(), is( 5 ) );
    }

    private DimensionResponse buildDimensionResponse( int operand )
    {
        DimensionResponse.DimensionResponseBuilder builder = DimensionResponse.builder();
        return (operand % 2 == 0 ? builder.name( "test" ) : builder.name( "another" )).build();
    }
}
