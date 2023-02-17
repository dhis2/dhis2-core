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
package org.hisp.dhis.webapi.controller.dataelement;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.DataElementOperandSchemaDescriptor;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.utils.PaginationUtils;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Tags( "metadata" )
@Controller
@RequestMapping( value = DataElementOperandSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class DataElementOperandController
{
    private final IdentifiableObjectManager manager;

    private final QueryService queryService;

    private final FieldFilterService fieldFilterService;

    private final LinkService linkService;

    private final ContextService contextService;

    private final SchemaService schemaService;

    private final CategoryService dataElementCategoryService;

    private final Cache<String, Long> paginationCountCache = new Cache2kBuilder<String, Long>()
    {
    }
        .expireAfterWrite( 1, TimeUnit.MINUTES )
        .build();

    @GetMapping
    @SuppressWarnings( "unchecked" )
    public @ResponseBody RootNode getObjectList( @RequestParam Map<String, String> rpParameters,
        OrderParams orderParams, @CurrentUser User currentUser )
        throws QueryParserException
    {
        Schema schema = schemaService.getDynamicSchema( DataElementOperand.class );

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<Order> orders = orderParams.getOrders( schema );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        WebOptions options = new WebOptions( rpParameters );
        WebMetadata metadata = new WebMetadata();
        List<DataElementOperand> dataElementOperands = List.of();

        if ( options.isTrue( "persisted" ) )
        {
            dataElementOperands = Lists.newArrayList( manager.getAll( DataElementOperand.class ) );
        }
        else
        {
            boolean totals = options.isTrue( "totals" );

            String deg = CollectionUtils.popStartsWith( filters, "dataElement.dataElementGroups.id:eq:" );
            deg = deg != null ? deg.substring( "dataElement.dataElementGroups.id:eq:".length() ) : null;

            String ds = options.get( "dataSet" );

            if ( deg != null )
            {
                DataElementGroup dataElementGroup = manager.get( DataElementGroup.class, deg );
                if ( dataElementGroup != null )
                {
                    dataElementOperands = dataElementCategoryService.getOperands( dataElementGroup.getMembers(),
                        totals );
                }
            }
            else if ( ds != null )
            {
                DataSet dataSet = manager.get( DataSet.class, ds );
                dataElementOperands = dataElementCategoryService.getOperands( dataSet, totals );
            }
            else
            {
                List<DataElement> dataElements = new ArrayList<>( manager.getAllSorted( DataElement.class ) );
                dataElementOperands = dataElementCategoryService.getOperands( dataElements, totals );
            }
        }

        // This is needed for two reasons:
        // 1) We are doing in-memory paging;
        // 2) We have to count all items respecting the filtering and the
        // initial universe of elements. In this case, the variable
        // "dataElementOperands".
        Query queryForCount = queryService.getQueryFromUrl( DataElementOperand.class, filters, orders );
        queryForCount.setObjects( dataElementOperands );

        List<DataElementOperand> totalOfItems = (List<DataElementOperand>) queryService
            .query( queryForCount );

        Query query = queryService.getQueryFromUrl( DataElementOperand.class, filters, orders,
            PaginationUtils.getPaginationData( options ), options.getRootJunction() );
        query.setDefaultOrder();
        query.setObjects( dataElementOperands );

        dataElementOperands = (List<DataElementOperand>) queryService.query( query );
        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            final long countTotal = isNotEmpty( totalOfItems ) ? totalOfItems.size() : 0;

            // fetch the count for the current query from a short-lived cache

            // Line "String deg = CollectionUtils.popStartsWith( filters, "dataElement.dataElementGroups.id:eq:" );"
            // removes all related dataElementGroups from collection used later by calling calculatePaginationCountKey (param filters).
            // The key must not be unique and pager does not work properly.
            long cachedCountTotal = !filters.isEmpty() ? paginationCountCache.computeIfAbsent(
                calculatePaginationCountKey( currentUser, filters, options ),
                () -> countTotal ) : countTotal;

            pager = new Pager( options.getPage(), cachedCountTotal, options.getPageSize() );

            linkService.generatePagerLinks( pager, DataElementOperand.class );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( DataElementOperand.class,
            new FieldFilterParams( dataElementOperands, fields ) ) );

        return rootNode;
    }

    private String calculatePaginationCountKey( User currentUser, List<String> filters, WebOptions options )
    {
        return currentUser.getUsername() + "." + "DataElementOperand" + "." + String.join( "|", filters ) + "."
            + options.getRootJunction().name();
    }
}
