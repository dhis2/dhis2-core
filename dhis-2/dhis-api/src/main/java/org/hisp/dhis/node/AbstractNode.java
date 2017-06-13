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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hisp.dhis.node.exception.InvalidTypeException;
import org.hisp.dhis.node.types.SimpleNode;
import org.hisp.dhis.schema.Property;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public abstract class AbstractNode implements Node
{
    protected String name;

    protected final NodeType nodeType;

    protected boolean metadata;

    protected Node parent;

    protected String namespace;

    protected String comment;

    protected List<Node> children = Lists.newArrayList();

    protected ImmutableList<Node> sortedChildren;

    protected Property property;

    protected AbstractNode( String name, NodeType nodeType )
    {
        this.name = name;
        this.nodeType = nodeType;
    }

    protected AbstractNode( String name, NodeType nodeType, Property property )
    {
        this.name = name;
        this.nodeType = nodeType;
        this.property = property;
    }

    @Override
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    public NodeType getType()
    {
        return nodeType;
    }

    @Override
    public boolean isMetadata()
    {
        return metadata;
    }

    public void setMetadata( boolean metadata )
    {
        this.metadata = metadata;
    }

    @Override
    public Node getParent()
    {
        return parent;
    }

    protected void setParent( Node parent )
    {
        this.parent = parent;
    }

    @Override
    public boolean is( NodeType type )
    {
        return type.equals( nodeType );
    }

    @Override
    public boolean isSimple()
    {
        return is( NodeType.SIMPLE );
    }

    @Override
    public boolean isComplex()
    {
        return is( NodeType.COMPLEX );
    }

    @Override
    public boolean isCollection()
    {
        return is( NodeType.COLLECTION );
    }

    @Override
    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    @Override
    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    @Override
    public Property getProperty()
    {
        return property;
    }

    public void setProperty( Property property )
    {
        this.property = property;
    }

    @Override
    public boolean haveProperty()
    {
        return property != null;
    }

    @Override
    public <T extends Node> T addChild( T child ) throws InvalidTypeException
    {
        if ( child == null || child.getName() == null )
        {
            return null;
        }

        children.add( child );
        ((AbstractNode) child).setParent( this );

        sortedChildren = null;

        return child;
    }

    @Override
    public <T extends Node> void removeChild( T child )
    {
        if ( children.contains( child ) )
        {
            children.remove( child );
        }

        sortedChildren = null;
    }

    @Override
    public <T extends Node> void addChildren( Iterable<T> children )
    {
        for ( Node child : children )
        {
            addChild( child );
        }
    }

    @Override
    public List<Node> getChildren()
    {
        if ( sortedChildren == null )
        {
            List<Node> clone = Lists.newArrayList( children );
            Collections.sort( clone, OrderComparator.INSTANCE );
            sortedChildren = ImmutableList.copyOf( clone );
        }

        return sortedChildren;
    }

    public void setChildren( List<Node> children )
    {
        this.children = children;
        this.sortedChildren = null;
    }

    @Override
    public int getOrder()
    {
        if ( isSimple() )
        {
            if ( ((SimpleNode) this).isAttribute() )
            {
                return 10;
            }

            return 20;
        }
        else if ( isComplex() )
        {
            return 30;
        }
        else if ( isCollection() )
        {
            return 40;
        }

        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( name, nodeType, parent, namespace, comment, children );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }

        final AbstractNode other = (AbstractNode) obj;

        return Objects.equals( this.name, other.name ) &&
            Objects.equals( this.nodeType, other.nodeType ) &&
            Objects.equals( this.namespace, other.namespace ) &&
            Objects.equals( this.comment, other.comment ) &&
            Objects.equals( this.children, other.children );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "name", name )
            .add( "nodeType", nodeType )
            .add( "parent", (parent != null ? parent.getName() : null) )
            .add( "namespace", namespace )
            .add( "comment", comment )
            .add( "children", children )
            .toString();
    }
}
