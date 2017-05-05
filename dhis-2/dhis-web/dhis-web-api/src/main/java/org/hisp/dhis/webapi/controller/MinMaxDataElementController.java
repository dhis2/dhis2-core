package org.hisp.dhis.webapi.controller;

/*
 *
 *  Copyright (c) 2004-2017, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import com.google.common.collect.Lists;
import org.apache.poi.ss.formula.functions.T;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.hibernate.exception.ReadAccessDeniedException;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.descriptors.MinMaxDataElementSchemaDescriptor;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */

@Controller
@RequestMapping( value = MinMaxDataElementSchemaDescriptor.API_ENDPOINT )
@ApiVersion ( DhisApiVersion.DEFAULT, DhisApiVer )
public class MinMaxDataElementController
{

    @Autowired
    private ContextService contextService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private MinMaxDataElementService minMaxDataElementService;



    //--------------------------------------------------------------------------
    // GET
    //--------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.GET )
    public @ResponseBody RootNode getObjectList(
        @RequestParam Map<String, String> rpParameters, OrderParams orderParams,
        HttpServletResponse response, HttpServletRequest request, User currentUser ) throws QueryParserException
    {
        Schema schema = schemaService.getDynamicSchema( MinMaxDataElement.class );

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );
        List<Order> orders = orderParams.getOrders( schema );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.defaultPreset().getFields() );
        }

        WebOptions options = new WebOptions( rpParameters );
        WebMetadata metadata = new WebMetadata();

        List<T> entities = getEntityList( metadata, options, filters, orders );
        Pager pager = metadata.getPager();

        if ( options.hasPaging() && pager == null )
        {
            pager = new Pager( options.getPage(), entities.size(), options.getPageSize() );
            entities = PagerUtils.pageCollection( entities, pager );
        }

        postProcessEntities( entities );
        postProcessEntities( entities, options, rpParameters );

        handleLinksAndAccess( entities, fields, false, currentUser );

        linkService.generatePagerLinks( pager, getEntityClass() );

        RootNode rootNode = NodeUtils.createMetadata();
        rootNode.getConfig().setInclusionStrategy( getInclusionStrategy( rpParameters.get( "inclusionStrategy" ) ) );

        if ( pager != null )
        {
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.filter( getEntityClass(), entities, fields ) );

        return rootNode;
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.GET )
    public @ResponseBody RootNode getObject(
        @PathVariable( "uid" ) String pvUid,
        @RequestParam Map<String, String> rpParameters,
        HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        User user = currentUserService.getCurrentUser();

        if ( !aclService.canRead( user, getEntityClass() ) )
        {
            throw new ReadAccessDeniedException( "You don't have the proper permissions to read objects of this type." );
        }

        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );
        List<String> filters = Lists.newArrayList( contextService.getParameterValues( "filter" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        return getObjectInternal( pvUid, rpParameters, filters, fields, user );
    }

    @Override
    protected List<MinMaxDataElement> getEntityList( WebMetadata metadata, WebOptions options, List<String> filters, List<Order> orders )
        throws QueryParserException
    {
        List<MapView> entityList;

        if ( options.getOptions().containsKey( "query" ) )
        {
            entityList = Lists.newArrayList( manager.filter( getEntityClass(), options.getOptions().get( "query" ) ) );
        }
        else
        {
            entityList = new ArrayList<>( manager.getAll( getEntityClass() ) );
        }

        return entityList;
    }
}
