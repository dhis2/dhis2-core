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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;
import static org.hisp.dhis.node.NodeUtils.createPager;
import static org.hisp.dhis.webapi.controller.dataitem.DataItemQueryController.API_RESOURCE_PATH;

import java.util.List;

import javax.annotation.PostConstruct;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for handling the result and pagination nodes. This
 * component is coupled to the controller class, where it's being used.
 *
 * It also keeps an internal cache which's used to speed up the pagination
 * process.
 *
 * IMPORTANT: This cache should be removed once we have a new centralized
 * caching solution in place. At that stage, the new solution should be
 * favoured.
 */
@Component
class ResponseHandler
{
    private final String CACHE_DATA_ITEMS_PAGINATION = "dataItemsPagination";

    private final QueryService queryService;

    private final LinkService linkService;

    private final FieldFilterService fieldFilterService;

    private final Environment environment;

    private final CacheProvider cacheProvider;

    private Cache<Long> PAGE_COUNTING_CACHE;

    ResponseHandler( final QueryService queryService, final LinkService linkService,
        final FieldFilterService fieldFilterService, final Environment environment, final CacheProvider cacheProvider )
    {
        checkNotNull( queryService );
        checkNotNull( linkService );
        checkNotNull( fieldFilterService );
        checkNotNull( environment );
        checkNotNull( cacheProvider );

        this.queryService = queryService;
        this.linkService = linkService;
        this.fieldFilterService = fieldFilterService;
        this.environment = environment;
        this.cacheProvider = cacheProvider;
    }

    /**
     * Appends the given dimensionalItemsFound (the collection of results) and
     * fields to the rootNode.
     *
     * @param rootNode the main response root node
     * @param dimensionalItemsFound the collection of results
     * @param fields the list of fields to be returned
     */
    void addResultsToNode( final RootNode rootNode,
        final List<BaseDimensionalItemObject> dimensionalItemsFound, final List<String> fields )
    {
        final CollectionNode collectionNode = fieldFilterService.toCollectionNode( BaseDimensionalItemObject.class,
            new FieldFilterParams( dimensionalItemsFound, fields ) );
        collectionNode.setName( "dataItems" );
        rootNode.addChild( collectionNode );
    }

    /**
     * This method takes care of the pagination link and their respective
     * attributes. It will count the number of results available and base on the
     * WebOptions will calculate the pagination output.
     *
     * @param rootNode the node where the the pagination will be attached to
     * @param targetEntities the list of classes which requires pagination
     * @param currentUser the current logged user
     * @param options holds the pagination definitions
     * @param filters the query filters used in the count query
     */
    void addPaginationToNode( final RootNode rootNode,
        final List<Class<? extends BaseDimensionalItemObject>> targetEntities, final User currentUser,
        final WebOptions options, final List<String> filters )
    {
        if ( options.hasPaging() )
        {
            if ( isNotEmpty( targetEntities ) )
            {
                long count = 0;

                // Counting and summing up the results for each entity.
                for ( final Class<? extends BaseDimensionalItemObject> entity : targetEntities )
                {
                    count += PAGE_COUNTING_CACHE.get(
                        createPageCountingCacheKey( currentUser, entity, filters, options ),
                        p -> countEntityRowsTotal( entity, options, filters ) ).orElse( Long.valueOf( 0 ) );
                }

                final Pager pager = new Pager( options.getPage(), count, options.getPageSize() );

                linkService.generatePagerLinks( pager, API_RESOURCE_PATH );

                rootNode.addChild( createPager( pager ) );
            }
        }
    }

    private long countEntityRowsTotal( final Class<? extends BaseDimensionalItemObject> entity,
        final WebOptions options,
        final List<String> filters )
    {
        final Query query = queryService.getQueryFromUrl( entity, filters, emptyList(), new Pagination(),
            options.getRootJunction() );

        return queryService.count( query );
    }

    private String createPageCountingCacheKey( final User currentUser,
        final Class<? extends BaseDimensionalItemObject> entity, final List<String> filters, final WebOptions options )
    {
        return currentUser.getUsername() + "." + entity + "." + join( "|", filters ) + "."
            + options.getRootJunction().name();
    }

    @PostConstruct
    void init()
    {
        // formatter:off
        PAGE_COUNTING_CACHE = cacheProvider.newCacheBuilder( Long.class )
            .forRegion( CACHE_DATA_ITEMS_PAGINATION )
            .expireAfterWrite( 5, MINUTES )
            .withInitialCapacity( 1000 )
            .forceInMemory()
            .withMaximumSize( isTestRun( environment.getActiveProfiles() ) ? 0 : 20000 )
            .build();
        // formatter:on
    }
}
