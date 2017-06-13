package org.hisp.dhis.webapi.controller.dataelement;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.common.OrderParams;
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
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = DataElementOperandSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataElementOperandController
{
    private final IdentifiableObjectManager manager;
    private final QueryService queryService;
    private final FieldFilterService fieldFilterService;
    private final LinkService linkService;
    private final ContextService contextService;
    private final SchemaService schemaService;
    private final DataElementCategoryService dataElementCategoryService;

    public DataElementOperandController( IdentifiableObjectManager manager, QueryService queryService,
        FieldFilterService fieldFilterService, LinkService linkService, ContextService contextService, SchemaService schemaService,
        DataElementCategoryService dataElementCategoryService )
    {
        this.manager = manager;
        this.queryService = queryService;
        this.fieldFilterService = fieldFilterService;
        this.linkService = linkService;
        this.contextService = contextService;
        this.schemaService = schemaService;
        this.dataElementCategoryService = dataElementCategoryService;
    }

    @GetMapping
    @SuppressWarnings( "unchecked" )
    public @ResponseBody RootNode getObjectList( @RequestParam Map<String, String> rpParameters, OrderParams orderParams )
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
        List<DataElementOperand> dataElementOperands;

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
                dataElementOperands = dataElementCategoryService.getOperands( dataElementGroup.getMembers(), totals );
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

        Query query = queryService.getQueryFromUrl( DataElementOperand.class, filters, orders, options.getRootJunction() );
        query.setDefaultOrder();
        query.setObjects( dataElementOperands );

        dataElementOperands = (List<DataElementOperand>) queryService.query( query );
        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            pager = new Pager( options.getPage(), dataElementOperands.size(), options.getPageSize() );
            linkService.generatePagerLinks( pager, DataElementOperand.class );

            dataElementOperands = PagerUtils.pageCollection( dataElementOperands, pager );
        }

        RootNode rootNode = NodeUtils.createMetadata();

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.filter( DataElementOperand.class, dataElementOperands, fields ) );

        return rootNode;
    }
}
