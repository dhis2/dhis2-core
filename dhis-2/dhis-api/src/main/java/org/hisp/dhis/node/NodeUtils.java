package org.hisp.dhis.node;

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

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class NodeUtils
{
    public static RootNode createRootNode( String name )
    {
        RootNode rootNode = new RootNode( name );
        rootNode.setDefaultNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );

        return rootNode;
    }

    public static RootNode createRootNode( Node node )
    {
        RootNode rootNode = new RootNode( node );
        rootNode.setDefaultNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );

        return rootNode;
    }

    public static RootNode createMetadata()
    {
        RootNode rootNode = new RootNode( "metadata" );
        rootNode.setDefaultNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );

        return rootNode;
    }

    public static RootNode createMetadata( Node child )
    {
        RootNode rootNode = createMetadata();
        rootNode.addChild( child );

        return rootNode;
    }

    public static Node createPager( Pager pager )
    {
        ComplexNode pagerNode = new ComplexNode( "pager" );
        pagerNode.setMetadata( true );

        pagerNode.addChild( new SimpleNode( "page", pager.getPage() ) );
        pagerNode.addChild( new SimpleNode( "pageCount", pager.getPageCount() ) );
        pagerNode.addChild( new SimpleNode( "total", pager.getTotal() ) );
        pagerNode.addChild( new SimpleNode( "pageSize", pager.getPageSize() ) );
        pagerNode.addChild( new SimpleNode( "nextPage", pager.getNextPage() ) );
        pagerNode.addChild( new SimpleNode( "prevPage", pager.getPrevPage() ) );

        return pagerNode;
    }

    public static Iterable<? extends Node> createSimples( Collection<?> collection )
    {
        return collection.stream().map( o -> new SimpleNode( "", o ) ).collect( Collectors.toList() );
    }

    public static Iterable<? extends Node> createSimples( Map<String, ?> map )
    {
        return map.keySet().stream().map( k -> new SimpleNode( k, map.get( k ) ) ).collect( Collectors.toList() );
    }

    private NodeUtils()
    {
    }
}
