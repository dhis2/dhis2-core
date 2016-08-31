package org.hisp.dhis.node.serializers;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;
import org.hisp.dhis.node.AbstractNodeSerializer;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class Jackson2JsonNodeSerializer extends AbstractNodeSerializer
{
    public static final String CONTENT_TYPE = "application/json";

    public static final String JSONP_CALLBACK = "org.hisp.dhis.node.serializers.Jackson2JsonNodeSerializer.callback";

    private final static ObjectMapper objectMapper = new ObjectMapper();

    static
    {
        objectMapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
        objectMapper.configure( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true );
        objectMapper.configure( SerializationFeature.WRAP_EXCEPTIONS, true );
        objectMapper.getFactory().enable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
    }

    private JsonGenerator generator = null;

    @Override
    public List<String> contentTypes()
    {
        return Lists.newArrayList( CONTENT_TYPE );
    }

    @Override
    protected void flushStream() throws Exception
    {
        generator.flush();
    }

    @Override
    protected void startSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        generator = objectMapper.getFactory().createGenerator( outputStream );
    }

    @Override
    protected void startWriteRootNode( RootNode rootNode ) throws Exception
    {
        if ( config.getProperties().containsKey( JSONP_CALLBACK ) )
        {
            generator.writeRaw( config.getProperties().get( JSONP_CALLBACK ) + "(" );
        }

        generator.writeStartObject();
    }

    @Override
    protected void endWriteRootNode( RootNode rootNode ) throws Exception
    {
        generator.writeEndObject();

        if ( config.getProperties().containsKey( JSONP_CALLBACK ) )
        {
            generator.writeRaw( ")" );
        }
    }

    @Override
    protected void startWriteSimpleNode( SimpleNode simpleNode ) throws Exception
    {
        Object value = simpleNode.getValue();

        if ( Date.class.isAssignableFrom( simpleNode.getValue().getClass() ) )
        {
            value = DateUtils.getIso8601NoTz( (Date) simpleNode.getValue() );
        }

        if ( simpleNode.getParent().isCollection() )
        {
            generator.writeObject( value );
        }
        else
        {
            generator.writeObjectField( simpleNode.getName(), value );
        }
    }

    @Override
    protected void endWriteSimpleNode( SimpleNode simpleNode ) throws Exception
    {
    }

    @Override
    protected void startWriteComplexNode( ComplexNode complexNode ) throws Exception
    {
        if ( complexNode.getParent().isCollection() )
        {
            generator.writeStartObject();
        }
        else
        {
            generator.writeObjectFieldStart( complexNode.getName() );
        }
    }

    @Override
    protected void endWriteComplexNode( ComplexNode complexNode ) throws Exception
    {
        generator.writeEndObject();
    }

    @Override
    protected void startWriteCollectionNode( CollectionNode collectionNode ) throws Exception
    {
        if ( collectionNode.getParent().isCollection() )
        {
            generator.writeStartArray();
        }
        else
        {
            generator.writeArrayFieldStart( collectionNode.getName() );
        }
    }

    @Override
    protected void endWriteCollectionNode( CollectionNode collectionNode ) throws Exception
    {
        generator.writeEndArray();
    }
}
