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
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.DhisApiVersion.DEFAULT;
import static org.hisp.dhis.node.NodeUtils.createMetadata;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
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
class DataItemQueryController
{
    static final String API_RESOURCE_PATH = "/dataItems";

    private final DataItemServiceFacade dataItemServiceFacade;

    private final ContextService contextService;

    private final ResponseHandler responseHandler;

    private final AclService aclService;

    public DataItemQueryController(final DataItemServiceFacade dataItemServiceFacade,
                                   final ContextService contextService, final ResponseHandler responseHandler, final AclService aclService )
    {
        checkNotNull( dataItemServiceFacade );
        checkNotNull( contextService );
        checkNotNull(responseHandler);
        checkNotNull( aclService );

        this.dataItemServiceFacade = dataItemServiceFacade;
        this.contextService = contextService;
        this.responseHandler = responseHandler;
        this.aclService = aclService;
    }

    /**
     * Retrieve all data items based in the URL filters and parameters.
     *
     * @return the list of items found in JSON format
     */
    @GetMapping( value = API_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE )
    public RootNode getJson( @RequestParam
    final Map<String, String> urlParameters, final OrderParams orderParams, final User currentUser )
        throws QueryParserException
    {
        log.debug( "Looking for data items (JSON response)" );

        return getDimensionalItems( currentUser, urlParameters, orderParams );
    }

    /**
     * Retrieve all data items based in the URL filters and parameters.
     *
     * @return the list of items found in XML format
     */
    @GetMapping( value = API_RESOURCE_PATH + ".xml", produces = APPLICATION_XML_VALUE )
    public RootNode getXml( @RequestParam
    final Map<String, String> urlParameters, final OrderParams orderParams, final User currentUser )
    {
        log.debug( "Looking for data items (XML response)" );

        return getDimensionalItems( currentUser, urlParameters, orderParams );
    }

    /**
     * Based on the informed arguments, this method will read the URL and based on
     * the give params will retrieve the respective data items.
     * 
     * @param currentUser the logged user
     * @param urlParameters the request url params
     * @param orderParams the request order params
     * @return the complete root node
     */
    private RootNode getDimensionalItems( final User currentUser, final Map<String, String> urlParameters,
        final OrderParams orderParams )
    {
        // Defining the input params.
        final List<String> fields = newArrayList( contextService.getParameterValues( "fields" ) );
        final List<String> filters = newArrayList( contextService.getParameterValues( "filter" ) );
        final WebOptions options = new WebOptions( urlParameters );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        // Extracting the target entities to be queried.
        final List<Class<? extends BaseDimensionalItemObject>> targetEntities = dataItemServiceFacade
            .extractTargetEntities( filters );

        // Checking if the user can read all the target entities.
        checkAuthorization( currentUser, targetEntities );

        // Retrieving the data items based on the input params.
        final List<BaseDimensionalItemObject> dimensionalItemsFound = dataItemServiceFacade.retrieveDataItemEntities(
            targetEntities, filters, options, orderParams );

        // Creating the response node.
        final RootNode rootNode = createMetadata();
        responseHandler.addResultsToNode( rootNode, dimensionalItemsFound, fields );
        responseHandler.addPaginationToNode( rootNode, targetEntities, currentUser, options, filters );

        return rootNode;
    }

    private void checkAuthorization( final User currentUser,
        final List<Class<? extends BaseDimensionalItemObject>> entities )
    {
        if ( isNotEmpty( entities ) )
        {
            for ( final Class<? extends BaseDimensionalItemObject> entity : entities )
            {
                if ( !aclService.canRead( currentUser, entity ) )
                {
                    throw new ReadAccessDeniedException(
                        "You don't have the proper permissions to read objects of type " + entity.getSimpleName() );
                }
            }
        }
    }
}
