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
package org.hisp.dhis.webapi.controller.dataitem;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hisp.dhis.query.Query.from;
import static org.hisp.dhis.webapi.controller.dataitem.DataItemServiceFacade.DATA_TYPE_ENTITY_MAP;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE_SIZE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.query.Junction.Type;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

public class DataItemServiceFacadeTest
{
    @Mock
    private QueryService queryService;

    @Rule
    public MockitoRule mockitoRule = rule();

    private DataItemServiceFacade dataItemServiceFacade;

    @Before
    public void setUp()
    {
        dataItemServiceFacade = new DataItemServiceFacade( queryService );
    }

    @Test
    public void testRetrieveDataItemEntities()
    {
        // Given
        final Set<Class<? extends BaseDimensionalItemObject>> anyTargetEntities = new HashSet<>(
            asList( Indicator.class ) );
        final List<BaseDimensionalItemObject> expectedItemsFound = asList( new Indicator(), new Indicator() );
        final List<String> anyFilters = asList( "anyFilter" );
        final WebOptions anyWebOptions = mockWebOptions( 10, 1 );
        final Set<String> anyOrdering = new HashSet<>( asList( "name:desc" ) );
        final OrderParams anyOrderParams = new OrderParams( anyOrdering );
        final Query anyQuery = from( new Schema( Indicator.class, "indicator", "indicators" ) );

        // When
        when( queryService.getQueryFromUrl( any(), anyList(), anyList(),
            any( Pagination.class ), any( Type.class ) ) ).thenReturn( anyQuery );
        when( (List<BaseDimensionalItemObject>) queryService.query( any( Query.class ) ) )
            .thenReturn( expectedItemsFound );
        final List<BaseDimensionalItemObject> actualDimensionalItems = dataItemServiceFacade
            .retrieveDataItemEntities( anyTargetEntities, anyFilters, anyWebOptions, anyOrderParams );

        // Then
        assertThat( actualDimensionalItems, containsInAnyOrder( expectedItemsFound.toArray() ) );
    }

    @Test
    public void testRetrieveDataItemEntitiesWhenTargetEntitiesIsEmpty()
    {
        // Given
        final Set<Class<? extends BaseDimensionalItemObject>> anyTargetEntities = emptySet();
        final List<BaseDimensionalItemObject> expectedItemsFound = asList( new Indicator(), new Indicator() );
        final List<String> anyFilters = asList( "anyFilter" );
        final WebOptions anyWebOptions = mockWebOptions( 10, 1 );
        final Set<String> anyOrdering = new HashSet<>( asList( "name:desc" ) );
        final OrderParams anyOrderParams = new OrderParams( anyOrdering );
        final Query anyQuery = from( new Schema( Indicator.class, "indicator", "indicators" ) );

        // When
        when( queryService.getQueryFromUrl( any(), anyList(), anyList(),
            any( Pagination.class ), any( Type.class ) ) ).thenReturn( anyQuery );
        when( (List<BaseDimensionalItemObject>) queryService.query( any( Query.class ) ) )
            .thenReturn( expectedItemsFound );
        final List<BaseDimensionalItemObject> actualDimensionalItems = dataItemServiceFacade
            .retrieveDataItemEntities( anyTargetEntities, anyFilters, anyWebOptions, anyOrderParams );

        // Then
        assertThat( actualDimensionalItems, is( empty() ) );
    }

    @Test
    public void testExtractTargetEntitiesUsingEqualsFilter()
    {
        // Given
        final Set<Class<? extends BaseDimensionalItemObject>> expectedTargetEntities = new HashSet<>(
            asList( Indicator.class ) );
        final List<String> theFilters = newArrayList( "dimensionItemType:eq:INDICATOR" );

        // When
        final Set<Class<? extends BaseDimensionalItemObject>> actualTargetEntities = dataItemServiceFacade
            .extractTargetEntities( theFilters );

        // Then
        assertThat( actualTargetEntities, containsInAnyOrder( expectedTargetEntities.toArray() ) );
    }

    @Test
    public void testExtractTargetEntitiesUsingInFilter()
    {
        // Given
        final Set<Class<? extends BaseDimensionalItemObject>> expectedTargetEntities = new HashSet<>(
            asList( Indicator.class, DataSet.class ) );
        final List<String> theFilters = newArrayList( "dimensionItemType:in:[INDICATOR, DATA_SET]" );

        // When
        final Set<Class<? extends BaseDimensionalItemObject>> actualTargetEntities = dataItemServiceFacade
            .extractTargetEntities( theFilters );

        // Then
        assertThat( actualTargetEntities, containsInAnyOrder( expectedTargetEntities.toArray() ) );
    }

    @Test
    public void testExtractTargetEntitiesWhenThereIsNoExplicitTargetSet()
    {
        // Given
        final List<String> noTargetEntitiesFilters = emptyList();

        // When
        final Set<Class<? extends BaseDimensionalItemObject>> actualTargetEntities = dataItemServiceFacade
            .extractTargetEntities( noTargetEntitiesFilters );

        // Then
        assertThat( actualTargetEntities, containsInAnyOrder( DATA_TYPE_ENTITY_MAP.values().toArray() ) );
    }

    private WebOptions mockWebOptions( final int pageSize, final int pageNumber )
    {
        final Map<String, String> options = new HashMap<>( 0 );
        options.put( PAGE_SIZE, valueOf( pageSize ) );
        options.put( PAGE, valueOf( pageNumber ) );
        options.put( PAGING, "true" );

        return new WebOptions( options );
    }
}
