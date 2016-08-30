package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
public class IndexController
{
    @Autowired
    private SchemaService schemaService;

    @Autowired
    private ContextService contextService;

    //--------------------------------------------------------------------------
    // GET
    //--------------------------------------------------------------------------

    @RequestMapping( value = "/api", method = RequestMethod.GET )
    public void getIndex( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        String location = response.encodeRedirectURL( "/resources" );
        response.sendRedirect( ContextUtils.getRootPath( request ) + location );
    }

    @RequestMapping( value = "/", method = RequestMethod.GET )
    public void getIndexWithSlash( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        String location = response.encodeRedirectURL( "/resources" );
        response.sendRedirect( ContextUtils.getRootPath( request ) + location );
    }

    @RequestMapping( value = "/resources", method = RequestMethod.GET )
    public @ResponseBody RootNode getResources()
    {
        return createRootNode();
    }

    private RootNode createRootNode()
    {
        RootNode rootNode = NodeUtils.createMetadata();
        CollectionNode collectionNode = rootNode.addChild( new CollectionNode( "resources" ) );

        for ( Schema schema : schemaService.getSchemas() )
        {
            if ( schema.haveApiEndpoint() )
            {
                ComplexNode complexNode = collectionNode.addChild( new ComplexNode( "resource" ) );

                // TODO add i18n to this
                complexNode.addChild( new SimpleNode( "displayName", beautify( schema.getPlural() ) ) );
                complexNode.addChild( new SimpleNode( "singular", schema.getSingular() ) );
                complexNode.addChild( new SimpleNode( "plural", schema.getPlural() ) );
                complexNode.addChild( new SimpleNode( "href", contextService.getApiPath() + schema.getRelativeApiEndpoint() ) );
            }
        }

        return rootNode;
    }

    private String beautify( String name )
    {
        String[] camelCaseWords = StringUtils.capitalize( name ).split( "(?=[A-Z])" );
        return StringUtils.join( camelCaseWords, " " ).trim();
    }
}
