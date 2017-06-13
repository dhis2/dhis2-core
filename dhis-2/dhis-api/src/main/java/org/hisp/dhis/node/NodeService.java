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

import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;
import org.hisp.dhis.node.types.RootNode;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface NodeService
{
    /**
     * Find a nodeSerializer that supports contentType or return null.
     *
     * @param contentType NodeSerializer contentType
     * @return NodeSerializer that support contentType, or null if not match was found
     * @see org.hisp.dhis.node.NodeSerializer
     */
    NodeSerializer getNodeSerializer( String contentType );

    /**
     * Write out rootNode to a nodeSerializer that matches the contentType.
     *
     * @param rootNode     RootNode to write
     * @param contentType  NodeSerializer contentType
     * @param outputStream Write to this outputStream
     */
    void serialize( RootNode rootNode, String contentType, OutputStream outputStream );

    /**
     * Find a nodeDeserializer that supports contentType or return null.
     *
     * @param contentType NodeDeserializer contentType
     * @return NodeDeserializer that support contentType, or null if not match was found
     * @see org.hisp.dhis.node.NodeDeserializer
     */
    NodeDeserializer getNodeDeserializer( String contentType );

    /**
     * @param contentType NodeDeserializer contentType
     * @param inputStream Read RootNode from this stream
     * @return RootNode deserialized from inputStream
     */
    RootNode deserialize( String contentType, InputStream inputStream );

    /**
     * Convert a single object to a complex node instance.
     *
     * @param object Object to convert
     * @return Instance of complex node, or null if any issues
     */
    ComplexNode toNode( Object object );

    /**
     * Convert a list of objects to a collection node of complex nodes.
     *
     * @param objects List of objects to convert
     * @return {@link CollectionNode} instance with converted objects
     */
    CollectionNode toNode( List<Object> objects );
}
