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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.join;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_UID;
import static org.hisp.dhis.node.NodeUtils.createPager;
import static org.hisp.dhis.webapi.controller.dataitem.DataItemQueryController.API_RESOURCE_PATH;
import static org.hisp.dhis.webapi.controller.dataitem.helper.FilteringHelper.setFilteringParams;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dataitem.query.QueryExecutor;
import org.hisp.dhis.dataitem.query.shared.QueryParam;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
 *
 * @author maikel arabori
 */
@Component
class ResponseHandler
{
    private final QueryExecutor queryExecutor;

    private final LinkService linkService;

    private final FieldFilterService fieldFilterService;

    private final Cache<Long> pageCountingCache;

    ResponseHandler( QueryExecutor queryExecutor, LinkService linkService, FieldFilterService fieldFilterService,
        CacheProvider cacheProvider )
    {
        checkNotNull( queryExecutor );
        checkNotNull( linkService );
        checkNotNull( fieldFilterService );
        checkNotNull( cacheProvider );

        this.queryExecutor = queryExecutor;
        this.linkService = linkService;
        this.fieldFilterService = fieldFilterService;
        this.pageCountingCache = cacheProvider.createDataItemsPaginationCache();
    }

    /**
     * Appends the given dimensionalItemsFound (the collection of results) and
     * fields to the rootNode.
     *
     * @param rootNode the main response root node
     * @param dimensionalItemsFound the collection of results
     * @param fields the list of fields to be returned
     */
    void addResultsToNode( RootNode rootNode, List<DataItem> dimensionalItemsFound, Set<String> fields )
    {
        CollectionNode collectionNode = fieldFilterService.toConcreteClassCollectionNode( DataItem.class,
            new FieldFilterParams( dimensionalItemsFound, newArrayList( fields ) ), "dataItems", DXF_2_0 );

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
    void addPaginationToNode( RootNode rootNode, Set<Class<? extends BaseIdentifiableObject>> targetEntities,
        User currentUser, WebOptions options, Set<String> filters )
    {
        if ( options.hasPaging() && isNotEmpty( targetEntities ) )
        {
            // Defining query params map and setting common params.
            MapSqlParameterSource paramsMap = new MapSqlParameterSource().addValue( USER_UID, currentUser.getUid() );

            setFilteringParams( filters, options, paramsMap, currentUser );

            AtomicLong count = new AtomicLong();

            // Counting and summing up the results for each entity.
            count.addAndGet( pageCountingCache.get(
                createPageCountingCacheKey( currentUser, targetEntities, filters, options ),
                p -> countEntityRowsTotal( targetEntities, options, paramsMap ) ) );

            Pager pager = new Pager( options.getPage(), count.get(), options.getPageSize() );

            linkService.generatePagerLinks( pager, API_RESOURCE_PATH );

            rootNode.addChild( createPager( pager ) );
        }
    }

    private long countEntityRowsTotal( Set<Class<? extends BaseIdentifiableObject>> targetEntities,
        WebOptions options, MapSqlParameterSource paramsMap )
    {
        // Calculate pagination.
        if ( options.hasPaging() )
        {
            int maxLimit = options.getPage() * options.getPageSize();
            paramsMap.addValue( QueryParam.MAX_LIMIT, maxLimit );
        }

        return new Long( queryExecutor.count( targetEntities, paramsMap ) );
    }

    private String createPageCountingCacheKey( User currentUser,
        Set<Class<? extends BaseIdentifiableObject>> targetEntities, Set<String> filters, WebOptions options )
    {
        return currentUser.getUsername() + "." + targetEntities + "." + join( "|", filters ) + "."
            + options.getRootJunction().name();
    }
}
