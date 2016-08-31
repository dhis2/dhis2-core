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

import org.hisp.dhis.node.config.Config;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.node.types.SimpleNode;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.OutputStream;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class AbstractNodeSerializer implements NodeSerializer
{
    protected static final DateTimeFormatter DT_FORMATTER = DateTimeFormat.forPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
        .withZoneUTC();

    protected void startSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
    }

    protected void endSerialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
    }

    protected abstract void flushStream() throws Exception;

    protected Config config;

    @Override
    public void serialize( RootNode rootNode, OutputStream outputStream ) throws Exception
    {
        this.config = rootNode.getConfig();
        startSerialize( rootNode, outputStream );
        writeRootNode( rootNode );
        endSerialize( rootNode, outputStream );
        this.config = null;
    }

    protected abstract void startWriteRootNode( RootNode rootNode ) throws Exception;

    protected void writeRootNode( RootNode rootNode ) throws Exception
    {
        startWriteRootNode( rootNode );

        for ( Node node : rootNode.getChildren() )
        {
            dispatcher( node );
            flushStream();
        }

        endWriteRootNode( rootNode );
        flushStream();
    }

    protected abstract void endWriteRootNode( RootNode rootNode ) throws Exception;

    protected abstract void startWriteSimpleNode( SimpleNode simpleNode ) throws Exception;

    protected void writeSimpleNode( SimpleNode simpleNode ) throws Exception
    {
        if ( !config.getInclusionStrategy().include( simpleNode.getValue() ) )
        {
            return;
        }

        startWriteSimpleNode( simpleNode );
        endWriteSimpleNode( simpleNode );
    }

    protected abstract void endWriteSimpleNode( SimpleNode simpleNode ) throws Exception;

    protected abstract void startWriteComplexNode( ComplexNode complexNode ) throws Exception;

    protected void writeComplexNode( ComplexNode complexNode ) throws Exception
    {
        if ( !config.getInclusionStrategy().include( complexNode.getChildren() ) )
        {
            return;
        }

        startWriteComplexNode( complexNode );

        for ( Node node : complexNode.getChildren() )
        {
            dispatcher( node );
            flushStream();
        }

        endWriteComplexNode( complexNode );
    }

    protected abstract void endWriteComplexNode( ComplexNode complexNode ) throws Exception;

    protected abstract void startWriteCollectionNode( CollectionNode collectionNode ) throws Exception;

    protected void writeCollectionNode( CollectionNode collectionNode ) throws Exception
    {
        if ( !config.getInclusionStrategy().include( collectionNode.getChildren() ) )
        {
            return;
        }

        startWriteCollectionNode( collectionNode );

        for ( Node node : collectionNode.getChildren() )
        {
            dispatcher( node );
            flushStream();
        }

        endWriteCollectionNode( collectionNode );
    }

    protected abstract void endWriteCollectionNode( CollectionNode collectionNode ) throws Exception;

    protected void dispatcher( Node node ) throws Exception
    {
        switch ( node.getType() )
        {
            case SIMPLE:
                writeSimpleNode( (SimpleNode) node );
                break;
            case COMPLEX:
                writeComplexNode( (ComplexNode) node );
                break;
            case COLLECTION:
                writeCollectionNode( (CollectionNode) node );
                break;
        }
    }
}
