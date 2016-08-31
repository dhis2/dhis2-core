package org.hisp.dhis.node;

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
import com.google.common.collect.Maps;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultNodeService implements NodeService
{
    @Autowired( required = false )
    private List<NodeSerializer> nodeSerializers = Lists.newArrayList();

    @Autowired( required = false )
    private List<NodeDeserializer> nodeDeserializers = Lists.newArrayList();

    private Map<String, NodeSerializer> nodeSerializerMap = Maps.newHashMap();

    private Map<String, NodeDeserializer> nodeDeserializerMap = Maps.newHashMap();

    @Autowired
    private FieldFilterService fieldFilterService;

    @PostConstruct
    private void init()
    {
        for ( NodeSerializer nodeSerializer : nodeSerializers )
        {
            for ( String contentType : nodeSerializer.contentTypes() )
            {
                nodeSerializerMap.put( contentType, nodeSerializer );
            }
        }

        for ( NodeDeserializer nodeDeserializer : nodeDeserializers )
        {
            for ( String contentType : nodeDeserializer.contentTypes() )
            {
                nodeDeserializerMap.put( contentType, nodeDeserializer );
            }
        }
    }

    @Override
    public NodeSerializer getNodeSerializer( String contentType )
    {
        if ( nodeSerializerMap.containsKey( contentType ) )
        {
            return nodeSerializerMap.get( contentType );
        }

        return null;
    }

    @Override
    public void serialize( RootNode rootNode, String contentType, OutputStream outputStream )
    {
        NodeSerializer nodeSerializer = getNodeSerializer( contentType );

        if ( nodeSerializer == null )
        {
            return; // TODO throw exception?
        }

        try
        {
            nodeSerializer.serialize( rootNode, outputStream );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    @Override
    public NodeDeserializer getNodeDeserializer( String contentType )
    {
        if ( nodeDeserializerMap.containsKey( contentType ) )
        {
            return nodeDeserializerMap.get( contentType );
        }

        return null;
    }

    @Override
    public RootNode deserialize( String contentType, InputStream inputStream )
    {
        NodeDeserializer nodeDeserializer = getNodeDeserializer( contentType );

        if ( nodeDeserializer == null )
        {
            return null; // TODO throw exception?
        }

        try
        {
            return nodeDeserializer.deserialize( inputStream );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ComplexNode toNode( Object object )
    {
        Assert.notNull( object, "object can not be null" );
        return fieldFilterService.filter( object, new ArrayList<>() );
    }

    @Override
    public CollectionNode toNode( List<Object> objects )
    {
        Assert.notNull( objects, "objects can not be null" );
        Assert.isTrue( objects.size() > 0, "objects list must be larger than 0" );

        return fieldFilterService.filter( objects.get( 0 ).getClass(), objects, new ArrayList<>() );
    }
}
