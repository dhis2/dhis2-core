package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.TranslateParams;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebMetaData;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private IdentifiableObjectManager identifiableObjectManager;

    // -------------------------------------------------------------------------
    // Controller
    // -------------------------------------------------------------------------

    @Override
    protected List<DimensionalObject> getEntityList( WebMetaData metaData, WebOptions options, List<String> filters, List<Order> orders )
    {
        return dimensionService.getAllDimensions();
    }

    @Override
    protected List<DimensionalObject> getEntity( String uid, WebOptions options )
    {
        return Lists.newArrayList( dimensionService.getDimensionalObjectCopy( uid, true ) );
    }

    @RequestMapping( value = "/{uid}/items", method = RequestMethod.GET )
    public @ResponseBody RootNode getItems( @PathVariable String uid, @RequestParam Map<String, String> parameters,
        TranslateParams translateParams, Model model, HttpServletRequest request, HttpServletResponse response )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":identifiable" );
        }

        List<NameableObject> items = dimensionService.getCanReadDimensionItems( uid );
        items = objectFilterService.filter( items, filters );
        Collections.sort( items, IdentifiableObjectNameComparator.INSTANCE );

        translate( items, translateParams );

        RootNode rootNode = NodeUtils.createMetadata();

        CollectionNode collectionNode = rootNode.addChild( fieldFilterService.filter( getEntityClass(), items, fields ) );
        collectionNode.setName( "items" );

        for ( Node node : collectionNode.getChildren() )
        {
            ((AbstractNode) node).setName( "item" );
        }

        return rootNode;
    }

    @RequestMapping( value = "/constraints", method = RequestMethod.GET )
    public @ResponseBody RootNode getDimensionConstraints( @RequestParam( value = "links", defaultValue = "true", required = false ) Boolean links )
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<DimensionalObject> dimensionConstraints = dimensionService.getDimensionConstraints();

        if ( links )
        {
            linkService.generateLinks( dimensionConstraints, false );
        }

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.addChild( fieldFilterService.filter( getEntityClass(), dimensionConstraints, fields ) );

        return rootNode;
    }

    @RequestMapping( value = "/dataSet/{uid}", method = RequestMethod.GET )
    public String getDimensionsForDataSet( @PathVariable String uid,
        @RequestParam( value = "links", defaultValue = "true", required = false ) Boolean links,
        Model model, HttpServletResponse response ) throws WebMessageException
    {
        WebMetaData metaData = new WebMetaData();

        DataSet dataSet = identifiableObjectManager.get( DataSet.class, uid );

        if ( dataSet == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataSet not found for uid: " + uid ) );
        }

        if ( !dataSet.hasCategoryCombo() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data set does not have a category combination: " + uid ) );
        }

        List<DimensionalObject> dimensions = new ArrayList<>();
        dimensions.addAll( dataSet.getCategoryCombo().getCategories() );
        dimensions.addAll( dataSet.getCategoryOptionGroupSets() );

        dimensions = dimensionService.getCanReadObjects( dimensions );

        for ( DimensionalObject dim : dimensions )
        {
            metaData.getDimensions().add( dimensionService.getDimensionalObjectCopy( dim.getUid(), true ) );
        }

        model.addAttribute( "model", metaData );

        if ( links )
        {
            linkService.generateLinks( metaData, false );
        }

        return "dimensions";
    }
}
