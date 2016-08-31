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

import org.hisp.dhis.schema.Property;
import org.springframework.core.Ordered;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface Node extends Ordered
{
    /**
     * Name of this node.
     *
     * @return current name of node
     */
    String getName();

    /**
     * Type specifier for this node.
     *
     * @return Node type
     * @see org.hisp.dhis.node.NodeType
     */
    NodeType getType();

    /**
     * Get parent node, or null if this is a top-level node.
     *
     * @return parent or null if node does not have parent
     */
    Node getParent();

    /**
     * @param type Type to check for
     * @return True if node is of this type
     */
    boolean is( NodeType type );

    /**
     * Helper that checks if node is of simple type, useful to checking if
     * you are allowed to add children to this node.
     *
     * @return true if type is simple
     * @see org.hisp.dhis.node.NodeType
     */
    boolean isSimple();

    /**
     * Helper that checks if node is of complex type.
     *
     * @return true if type is complex
     * @see org.hisp.dhis.node.NodeType
     */
    boolean isComplex();

    /**
     * Helper that checks if node is of collection type.
     *
     * @return true if type is collection
     * @see org.hisp.dhis.node.NodeType
     */
    boolean isCollection();

    /**
     * Should this be considered data or metadata.
     *
     * @return True if metadata (like a pager)
     */
    boolean isMetadata();

    /**
     * Namespace for this node. Not all serializers support this, and its up to the
     * NodeSerializer implementation to decide what to do with this.
     *
     * @return namespace
     * @see org.hisp.dhis.node.NodeSerializer
     */
    String getNamespace();

    /**
     * Comment for this node. Not all serializers support this, and its up to the
     * NodeSerializer implementation to decide what to do with this.
     *
     * @return namespace
     * @see org.hisp.dhis.node.NodeSerializer
     */
    String getComment();

    /**
     * The associated property for this node (if any).
     */
    Property getProperty();

    /**
     * Is there a property associated with this Node.
     */
    boolean haveProperty();

    /**
     * Adds a child to this node.
     *
     * @param child Child node to add
     * @return Child node that was added
     */
    <T extends Node> T addChild( T child );

    /**
     * Remove a child from this node.
     *
     * @param child Child node to add
     */
    <T extends Node> void removeChild( T child );

    /**
     * Adds a collection of children to this node.
     *
     * @param children Child nodes to add
     */
    <T extends Node> void addChildren( Iterable<T> children );

    /**
     * Get all child notes associated with this node. Please note that the returned list is a copy
     * of the internal list, and changes to the list will not be reflected in the node.
     *
     * @return List of child nodes associated with this node
     */
    List<Node> getChildren();
}
