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
package org.hisp.dhis.webapi.controller.dataitem;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.valueOf;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGE_SIZE;
import static org.hisp.dhis.webapi.webdomain.WebOptions.PAGING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.NoOpCache;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dataitem.query.QueryExecutor;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Unit tests for ResponseHandler.
 *
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class ResponseHandlerTest
{

    @Mock
    private QueryExecutor queryExecutor;

    @Mock
    private LinkService linkService;

    @Mock
    private FieldFilterService fieldFilterService;

    @Mock
    private CacheProvider cacheProvider;

    private ResponseHandler responseHandler;

    @BeforeEach
    public void setUp()
    {
        when( cacheProvider.createDataItemsPaginationCache() ).thenReturn( new NoOpCache<>() );
        responseHandler = new ResponseHandler( queryExecutor, linkService, fieldFilterService, cacheProvider );
    }

    @Test
    void testAddResultsToNodeWithSuccess()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final DataItem anyDataItem = DataItem.builder().name( "any" ).build();
        final List<DataItem> anyDimensionalItems = singletonList( anyDataItem );
        final Set<String> anyFields = newHashSet( "name" );
        final CollectionNode anyCollectionNode = new CollectionNode( "any" );

        // When
        when( fieldFilterService.toConcreteClassCollectionNode( any(), any(), any(), any() ) )
            .thenReturn( anyCollectionNode );
        responseHandler.addResultsToNode( anyRootNode, anyDimensionalItems, anyFields );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), hasSize( 1 ) );
        assertThat( anyRootNode.getChildren().get( 0 ).isCollection(), is( true ) );
    }

    @Test
    void testAddPaginationToNodeWithSuccess()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final Set<Class<? extends BaseIdentifiableObject>> anyTargetEntities = Set.of( Indicator.class,
            DataSet.class );
        final Set<String> anyFilters = newHashSet( "any" );
        final User anyUser = new User();
        final WebOptions anyWebOptions = mockWebOptions( 10, 1 );

        // When
        responseHandler.addPaginationToNode( anyRootNode, anyTargetEntities, anyUser, anyWebOptions, anyFilters );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), hasSize( 1 ) );
        assertThat( anyRootNode.getChildren().get( 0 ).isMetadata(), is( true ) );
        assertThat( anyRootNode.getChildren().get( 0 ).isComplex(), is( true ) );
        verify( linkService, times( 1 ) ).generatePagerLinks( any( Pager.class ), anyString() );
    }

    @Test
    void testAddPaginationToNodeWhenPagingIsFalse()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final Set<Class<? extends BaseIdentifiableObject>> anyTargetEntities = Set.of( Indicator.class,
            DataSet.class );
        final Set<String> anyFilters = newHashSet( "any" );
        final User anyUser = new User();
        final WebOptions webOptionsNoPaging = mockWebOptionsNoPaging();

        // When
        responseHandler.addPaginationToNode( anyRootNode, anyTargetEntities, anyUser, webOptionsNoPaging, anyFilters );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), is( empty() ) );
        verify( linkService, never() ).generatePagerLinks( any( Pager.class ), anyString() );
    }

    @Test
    void testAddPaginationToNodeWhenTargetEntitiesIsEmpty()
    {
        // Given
        final RootNode anyRootNode = new RootNode( "any" );
        final Set<Class<? extends BaseIdentifiableObject>> emptyTargetEntities = emptySet();
        final Set<String> anyFilters = newHashSet( "any" );
        final User anyUser = new User();
        final WebOptions anyWebOptions = mockWebOptions( 10, 1 );

        // When
        responseHandler.addPaginationToNode( anyRootNode, emptyTargetEntities, anyUser, anyWebOptions, anyFilters );

        // Then
        assertThat( anyRootNode, is( notNullValue() ) );
        assertThat( anyRootNode.getName(), is( equalTo( "any" ) ) );
        assertThat( anyRootNode.getChildren(), is( empty() ) );
        verify( linkService, never() ).generatePagerLinks( any( Pager.class ), anyString() );
        verify( queryExecutor, never() ).count( any( Set.class ), any( MapSqlParameterSource.class ) );
    }

    private WebOptions mockWebOptions( final int pageSize, final int pageNumber )
    {
        final Map<String, String> options = new HashMap<>( 0 );
        options.put( PAGE_SIZE, valueOf( pageSize ) );
        options.put( PAGE, valueOf( pageNumber ) );
        options.put( PAGING, "true" );

        return new WebOptions( options );
    }

    private WebOptions mockWebOptionsNoPaging()
    {
        final Map<String, String> options = new HashMap<>( 0 );
        options.put( PAGING, "false" );

        return new WebOptions( options );
    }
}
