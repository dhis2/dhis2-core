package org.hisp.dhis.webapi.controller.dataitem;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.node.NodeUtils.createPager;
import static org.hisp.dhis.webapi.controller.dataitem.DataItemQueryController.API_RESOURCE_PATH;

import java.util.List;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for handling the pagination links. This component
 * is coupled to the controller class, where it's being used.
 * 
 * It also keeps and internal caching used to speed up the pagination process.
 * This caching should be removed once we have a new centralized caching
 * solution in place. At that stage, the new solution should be favoured.
 */
@Component
class PagingNodeHandler
{
    private final QueryService queryService;

    private final LinkService linkService;

    // @formatter:off
    private Cache<String,Integer> pageCountingCache = new Cache2kBuilder<String, Integer>() {}
            .expireAfterWrite( 1, MINUTES )
            .build();
    // @formatter:on

    PagingNodeHandler( final QueryService queryService, final LinkService linkService )
    {
        checkNotNull( queryService );
        checkNotNull( linkService );

        this.queryService = queryService;
        this.linkService = linkService;
    }

    /**
     * This method takes care of the pagination link and their respective
     * attributes. It will count the number of results available and base on the
     * WebOptions will calculate the pagination output.
     * 
     * @param rootNode the node where the the pagination will be attached to
     * @param entities the list of classes which requires pagination
     * @param currentUser the current logged user
     * @param options holds the pagination definitions
     * @param filters the query filters used in the count query
     */
    void addPagingLinkToNode( final RootNode rootNode,
        final List<Class<? extends BaseDimensionalItemObject>> entities, final User currentUser,
        final WebOptions options, final List<String> filters )
    {
        final WebMetadata metadata = new WebMetadata();
        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            if ( isNotEmpty( entities ) )
            {
                long count = 0;

                // Counting and summing up the results for each entity.
                for ( final Class<? extends BaseDimensionalItemObject> entity : entities )
                {
                    count += pageCountingCache.computeIfAbsent(
                        createPageCountingCacheKey( currentUser, entity, filters, options ),
                        () -> countEntityRowsTotal( entity, options, filters ) );
                }

                pager = new Pager( options.getPage(), count, options.getPageSize() );

                linkService.generatePagerLinks( pager, API_RESOURCE_PATH );

                if ( pager != null )
                {
                    rootNode.addChild( createPager( pager ) );
                }
            }
        }
    }

    private int countEntityRowsTotal( final Class<? extends BaseDimensionalItemObject> entity, final WebOptions options,
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
}
