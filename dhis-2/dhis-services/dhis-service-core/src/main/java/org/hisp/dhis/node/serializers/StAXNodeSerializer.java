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

import com.google.common.collect.Lists;
import org.hisp.dhis.node.AbstractNodeSerializer;
import org.hisp.dhis.node.Node;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class StAXNodeSerializer extends AbstractNodeSerializer
{
    public static final String CONTENT_TYPE = "application/xml";

    private static final XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();

    static
    {
        xmlFactory.setProperty( "javax.xml.stream.isRepairingNamespaces", true );
    }

    private XMLStreamWriter writer;

    @Override
    public List<String> contentTypes()
    {
        return Lists.newArrayList( CONTENT_TYPE );
    }

    @Override
    protected void startSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        writer = xmlFactory.createXMLStreamWriter( outputStream );
        writer.setDefaultNamespace( rootNode.getDefaultNamespace() );
    }

    @Override
    protected void flushStream() throws Exception
    {
        writer.flush();
    }

    @Override
    protected void startWriteRootNode( RootNode rootNode ) throws Exception
    {
        writer.writeStartDocument( "UTF-8", "1.0" );

        if ( !StringUtils.isEmpty( rootNode.getComment() ) )
        {
            writer.writeComment( rootNode.getComment() );
        }

        writeStartElement( rootNode );
    }

    @Override
    protected void endWriteRootNode( RootNode rootNode ) throws Exception
    {
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    @Override
    protected void startWriteSimpleNode( SimpleNode simpleNode ) throws Exception
    {
        String value = null;

        if ( simpleNode.getValue() != null && Date.class.isAssignableFrom( simpleNode.getValue().getClass() ) )
        {
            value = DateUtils.getIso8601NoTz( (Date) simpleNode.getValue() );
        }
        else
        {
            value = String.valueOf( simpleNode.getValue() );
        }

        if ( simpleNode.isAttribute() )
        {
            if ( value == null )
            {
                return;
            }

            if ( !StringUtils.isEmpty( simpleNode.getNamespace() ) )
            {
                writer.writeAttribute( simpleNode.getNamespace(), simpleNode.getName(), value );
            }
            else
            {
                writer.writeAttribute( simpleNode.getName(), value );
            }
        }
        else
        {
            writeStartElement( simpleNode );

            if ( value != null )
            {
                writer.writeCharacters( value );
            }
        }
    }

    @Override
    protected void endWriteSimpleNode( SimpleNode simpleNode ) throws Exception
    {
        if ( !simpleNode.isAttribute() )
        {
            writer.writeEndElement();
        }
    }

    @Override
    protected void startWriteComplexNode( ComplexNode complexNode ) throws Exception
    {
        writeStartElement( complexNode );
    }

    @Override
    protected void endWriteComplexNode( ComplexNode complexNode ) throws Exception
    {
        writer.writeEndElement();
    }

    @Override
    protected void startWriteCollectionNode( CollectionNode collectionNode ) throws Exception
    {
        if ( collectionNode.isWrapping() && !collectionNode.getChildren().isEmpty() )
        {
            writeStartElement( collectionNode );
        }
    }

    @Override
    protected void endWriteCollectionNode( CollectionNode collectionNode ) throws Exception
    {
        if ( collectionNode.isWrapping() && !collectionNode.getChildren().isEmpty() )
        {
            writer.writeEndElement();
        }
    }

    private void writeStartElement( Node node ) throws XMLStreamException
    {
        if ( !StringUtils.isEmpty( node.getComment() ) )
        {
            writer.writeComment( node.getComment() );
        }

        if ( !StringUtils.isEmpty( node.getNamespace() ) )
        {
            writer.writeStartElement( node.getNamespace(), node.getName() );
        }
        else
        {
            writer.writeStartElement( node.getName() );
        }
    }
}
