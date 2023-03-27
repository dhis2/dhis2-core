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
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.DhisApiVersion.ALL;
import static org.hisp.dhis.common.DhisApiVersion.DEFAULT;
import static org.hisp.dhis.feedback.ErrorCode.E3012;
import static org.hisp.dhis.node.NodeUtils.createMetadata;
import static org.hisp.dhis.webapi.controller.dataitem.validator.FilterValidator.checkNamesAndOperators;
import static org.hisp.dhis.webapi.controller.dataitem.validator.OrderValidator.checkOrderParams;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class is responsible for providing methods responsible for retrieving
 * dimensional items based on the provided URL params and filters.
 *
 * It should expose only query methods.
 *
 * @author maikel arabori
 */
@OpenApi.Tags( "metadata" )
@Slf4j
@ApiVersion( { DEFAULT, ALL } )
@RequiredArgsConstructor
@RestController
public class DataItemQueryController
{
    static final String API_RESOURCE_PATH = "/dataItems";

    private static final String FIELDS = "fields";

    private static final String FILTER = "filter";

    private final DataItemServiceFacade dataItemServiceFacade;

    private final ContextService contextService;

    private final ResponseHandler responseHandler;

    private final AclService aclService;

    /**
     * Retrieve all data items based in the URL filters and parameters.
     *
     * @return the list of items found in JSON format
     */
    @GetMapping( value = API_RESOURCE_PATH, produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<RootNode> getJson( @RequestParam final Map<String, String> urlParameters,
        OrderParams orderParams, @CurrentUser User currentUser )
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
    public ResponseEntity<RootNode> getXml( @RequestParam final Map<String, String> urlParameters,
        OrderParams orderParams, @CurrentUser User currentUser )
    {
        log.debug( "Looking for data items (XML response)" );

        return getDimensionalItems( currentUser, urlParameters, orderParams );
    }

    /**
     * Based on the informed arguments, this method will read the URL and based
     * on the give params will retrieve the respective data items.
     *
     * @param currentUser the logged user
     * @param urlParameters the request url params
     * @param orderParams the request order params
     * @return the complete root node
     */
    private ResponseEntity<RootNode> getDimensionalItems( final User currentUser,
        final Map<String, String> urlParameters,
        final OrderParams orderParams )
    {
        // Defining the input params.
        Set<String> fields = newHashSet( contextService.getParameterValues( FIELDS ) );
        Set<String> filters = newHashSet( contextService.getParameterValues( FILTER ) );
        WebOptions options = new WebOptions( urlParameters );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        checkNamesAndOperators( filters );
        checkOrderParams( orderParams.getOrders() );

        // Extracting the target entities to be queried.
        Set<Class<? extends BaseIdentifiableObject>> targetEntities = dataItemServiceFacade
            .extractTargetEntities( filters );

        // Checking if the user can read all the target entities.
        checkAuthorization( currentUser, targetEntities );

        // Retrieving the data items based on the input params.
        List<DataItem> dimensionalItems = dataItemServiceFacade.retrieveDataItemEntities(
            targetEntities, filters, options, orderParams );

        // Creating the response node.
        RootNode rootNode = createMetadata();

        responseHandler.addResultsToNode( rootNode, dimensionalItems, fields );
        responseHandler.addPaginationToNode( rootNode, targetEntities, currentUser, options,
            filters );

        return new ResponseEntity<>( rootNode, OK );
    }

    private void checkAuthorization( final User currentUser,
        final Set<Class<? extends BaseIdentifiableObject>> entities )
    {
        if ( isNotEmpty( entities ) )
        {
            for ( final Class<? extends BaseIdentifiableObject> entity : entities )
            {
                if ( !aclService.canRead( currentUser, entity ) )
                {
                    throw new IllegalQueryException(
                        new ErrorMessage( E3012, currentUser.getUsername(), entity.getSimpleName() ) );
                }
            }
        }
    }
}
