package org.hisp.dhis.node.types;

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

import org.hisp.dhis.node.AbstractNode;
import org.hisp.dhis.node.NodeType;

import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class CollectionNode extends AbstractNode
{
    /**
     * Should this collection act as a wrapper around its children.
     */
    boolean wrapping = true;

    public CollectionNode( String name )
    {
        super( name, NodeType.COLLECTION );
    }

    public CollectionNode( String name, boolean wrapping )
    {
        super( name, NodeType.COLLECTION );
        this.wrapping = wrapping;
    }

    public boolean isWrapping()
    {
        return wrapping;
    }

    public void setWrapping( boolean wrapping )
    {
        this.wrapping = wrapping;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash( wrapping );
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

        if ( !super.equals( obj ) )
        {
            return false;
        }

        final CollectionNode other = (CollectionNode) obj;
        return Objects.equals( this.wrapping, other.wrapping );
    }
}
