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
package org.hisp.dhis.webapi.controller.dimension;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.dimension.AnalyticsDimensionService;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = DimensionController.RESOURCE_PATH )
public class DimensionController
    extends AbstractCrudController<DimensionalObject>
{
    public static final String RESOURCE_PATH = "/dimensions";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private AnalyticsDimensionService analyticsDimensionService;

    @Autowired
    private IdentifiableObjectManager identifiableObjectManager;

    @Autowired
    private DimensionItemPageHandler dimensionItemPageHandler;

    // -------------------------------------------------------------------------
    // Controller
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings( "unchecked" )
    protected @ResponseBody List<DimensionalObject> getEntityList( WebMetadata metadata, WebOptions options,
        List<String> filters, List<Order> orders )
        throws QueryParserException
    {
        List<DimensionalObject> dimensionalObjects;
        Query query = queryService.getQueryFromUrl( DimensionalObject.class, filters, orders,
            getPaginationData( options ), options.getRootJunction() );
        query.setDefaultOrder();
        query.setDefaults( Defaults.valueOf( options.get( "defaults", DEFAULTS ) ) );
        query.setObjects( dimensionService.getAllDimensions() );
        dimensionalObjects = (List<DimensionalObject>) queryService.query( query );

        return dimensionalObjects;
    }

    @Override
    protected List<DimensionalObject> getEntity( String uid, WebOptions options )
    {
        if ( isNotBlank( uid ) && isValidUid( uid ) )
        {
            return newArrayList( dimensionService.getDimensionalObjectCopy( uid, true ) );
        }

        return emptyList();
    }

    @SuppressWarnings( "unchecked" )
    @GetMapping( "/{uid}/items" )
    public @ResponseBody RootNode getItems( @PathVariable String uid, @RequestParam Map<String, String> parameters,
        OrderParams orderParams )
        throws QueryParserException
    {
        List<String> fields = newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = newArrayList( contextService.getParameterValues( "filter" ) );
        List<Order> orders = orderParams.getOrders( getSchema( DimensionalItemObject.class ) );
        WebOptions options = new WebOptions( parameters );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.defaultPreset().getFields() );
        }

        // This is the base list used in this flow. It contains only items
        // allowed to the current user.
        List<DimensionalItemObject> readableItems = dimensionService.getCanReadDimensionItems( uid );

        // This is needed for two reasons:
        // 1) We are doing in-memory paging;
        // 2) We have to count all items respecting the filtering.
        Query queryForCount = queryService.getQueryFromUrl( DimensionalItemObject.class, filters, orders );
        queryForCount.setObjects( readableItems );

        List<DimensionalItemObject> forCountItems = (List<DimensionalItemObject>) queryService
            .query( queryForCount );

        Query query = queryService.getQueryFromUrl( DimensionalItemObject.class, filters, orders,
            getPaginationData( options ) );
        query.setObjects( readableItems );
        query.setDefaultOrder();

        List<DimensionalItemObject> paginatedItems = (List<DimensionalItemObject>) queryService.query( query );

        RootNode rootNode = NodeUtils.createMetadata();

        CollectionNode collectionNode = rootNode
            .addChild( fieldFilterService.toCollectionNode( DimensionalItemObject.class,
                new FieldFilterParams( paginatedItems, fields ) ) );
        collectionNode.setName( "items" );

        for ( Node node : collectionNode.getChildren() )
        {
            ((AbstractNode) node).setName( "item" );
        }

        // Adding pagination elements to the root node.
        final int totalOfItems = isNotEmpty( forCountItems ) ? forCountItems.size() : 0;
        dimensionItemPageHandler.addPaginationToNodeIfEnabled( rootNode, options, uid, totalOfItems );

        return rootNode;
    }

    @GetMapping( "/constraints" )
    public @ResponseBody RootNode getDimensionConstraints(
        @RequestParam( value = "links", defaultValue = "true", required = false ) Boolean links )
    {
        List<String> fields = newArrayList( contextService.getParameterValues( "fields" ) );
        List<DimensionalObject> dimensionConstraints = dimensionService.getDimensionConstraints();

        if ( links )
        {
            linkService.generateLinks( dimensionConstraints, false );
        }

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( fieldFilterService.toCollectionNode( getEntityClass(),
            new FieldFilterParams( dimensionConstraints, fields ) ) );

        return rootNode;
    }

    @GetMapping( "/recommendations" )
    public @ResponseBody RootNode getRecommendedDimensions( @RequestParam Set<String> dimension )
    {
        List<String> fields = newArrayList( contextService.getParameterValues( "fields" ) );
        DataQueryRequest request = DataQueryRequest.newBuilder().dimension( dimension ).build();

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.defaultPreset().getFields() );
        }

        List<DimensionalObject> dimensions = analyticsDimensionService.getRecommendedDimensions( request );

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild(
            fieldFilterService.toCollectionNode( getEntityClass(), new FieldFilterParams( dimensions, fields ) ) );

        return rootNode;
    }

    @GetMapping( "/dataSet/{uid}" )
    public @ResponseBody RootNode getDimensionsForDataSet( @PathVariable String uid,
        @RequestParam( value = "links", defaultValue = "true", required = false ) Boolean links,
        Model model, HttpServletResponse response )
        throws WebMessageException
    {
        WebMetadata metadata = new WebMetadata();
        List<String> fields = newArrayList( contextService.getParameterValues( "fields" ) );

        DataSet dataSet = identifiableObjectManager.get( DataSet.class, uid );

        if ( dataSet == null )
        {
            throw new WebMessageException( notFound( "Data set not found: " + uid ) );
        }

        List<DimensionalObject> dimensions = new ArrayList<>();
        dimensions.addAll( dataSet.getCategoryCombo().getCategories().stream()
            .filter( ca -> !ca.isDefault() )
            .collect( toList() ) );
        dimensions.addAll( dataSet.getCategoryOptionGroupSets() );

        dimensions = dimensionService.getCanReadObjects( dimensions );

        metadata.setDimensions( dimensions.stream()
            .map( dim -> dimensionService.getDimensionalObjectCopy( dim.getUid(), true ) )
            .collect( toList() ) );

        if ( links )
        {
            linkService.generateLinks( metadata, false );
        }

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( fieldFilterService.toCollectionNode( getEntityClass(),
            new FieldFilterParams( metadata.getDimensions(), fields ) ) );

        return rootNode;
    }
}
