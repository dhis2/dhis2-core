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
import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.common.DhisApiVersion.DEFAULT;
import static org.hisp.dhis.common.PagerUtils.pageCollection;
import static org.hisp.dhis.node.NodeUtils.createMetadata;
import static org.hisp.dhis.node.NodeUtils.createPager;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for providing methods responsible for retrieving
 * dimensional items based on the provided URL params and filters.
 *
 * It should expose only query methods.
 */
@Slf4j
@ApiVersion( { DEFAULT, DhisApiVersion.ALL } )
@RestController
public class DataItemQueryController
{
    private final String RESOURCE_PATH = "/dataItems";

    private final DataItemServiceFacade dataItemServiceFacade;

    private final FieldFilterService fieldFilterService;

    private final ContextService contextService;

    public DataItemQueryController(final DataItemServiceFacade dataItemServiceFacade,
                                   final FieldFilterService fieldFilterService, final ContextService contextService )
    {
        checkNotNull( dataItemServiceFacade );
        checkNotNull( fieldFilterService );
        checkNotNull( contextService );

        this.dataItemServiceFacade = dataItemServiceFacade;
        this.fieldFilterService = fieldFilterService;
        this.contextService = contextService;
    }

    /**
     * Retrieve all data items based in the URL filters and parameters.
     *
     * @return the list of items found in JSON format
     */
    @GetMapping( value = RESOURCE_PATH, produces = APPLICATION_JSON_VALUE )
    public RootNode getJson( @RequestParam
    final Map<String, String> urlParameters, final OrderParams orderParams )
        throws QueryParserException
    {
        log.debug( "Looking for data items (JSON response)" );

        // TODO: Should we cache it?
        // FIXME: Fix pagination
        return getDimensionalItems( urlParameters, orderParams );
    }

    /**
     * Retrieve all data items based in the URL filters and parameters.
     *
     * @return the list of items found in XML format
     */
    @GetMapping( value = RESOURCE_PATH + ".xml", produces = APPLICATION_XML_VALUE )
    public RootNode getXml( @RequestParam
    final Map<String, String> urlParameters, final OrderParams orderParams )
    {
        log.debug( "Looking for data items (XML response)" );

        // TODO: Should we cache it?
        // FIXME: Fix pagination
        return getDimensionalItems( urlParameters, orderParams );
    }

    private RootNode getDimensionalItems( final Map<String, String> urlParameters, final OrderParams orderParams )
    {
        // Defining the input params.
        final List<String> fields = newArrayList( contextService.getParameterValues( "fields" ) );
        final List<String> filters = newArrayList( contextService.getParameterValues( "filter" ) );
        final WebOptions options = new WebOptions( urlParameters );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        // Retrieving the data items based on the input params.
        List<BaseDimensionalItemObject> dimensionalItemsFound = dataItemServiceFacade.retrieveDataItems( filters,
            options, orderParams );

        // Building the response.
        final RootNode rootNode = createMetadata();

        dimensionalItemsFound = appendPagerObjectToNode( rootNode, options, dimensionalItemsFound );

        rootNode.addChild( fieldFilterService.toCollectionNode( BaseDimensionalItemObject.class,
            new FieldFilterParams( dimensionalItemsFound, fields ) ) );

        return rootNode;
    }

    private List<BaseDimensionalItemObject> appendPagerObjectToNode( final RootNode rootNode,
        final WebOptions options, List<BaseDimensionalItemObject> dimensionalItems )
    {
        final WebMetadata metadata = new WebMetadata();
        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            pager = new Pager( options.getPage(), dimensionalItems.size(), options.getPageSize() );
            dimensionalItems = pageCollection( dimensionalItems, pager );
        }

        if ( pager != null )
        {
            rootNode.addChild( createPager( pager ) );
        }

        return dimensionalItems;
    }
}
