package org.hisp.dhis.dxf2.events.trackedentity;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "relationship", namespace = DxfNamespaces.DXF_2_0 )
public class Relationship
{
    private String displayName;

    private String trackedEntityInstanceA;

    private String trackedEntityInstanceB;

    private String relationship;

    private TrackedEntityInstance relative;

    public Relationship()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName( String name )
    {
        this.displayName = name;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstanceA()
    {
        return trackedEntityInstanceA;
    }

    public void setTrackedEntityInstanceA( String trackedEntityInstanceA )
    {
        this.trackedEntityInstanceA = trackedEntityInstanceA;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstanceB()
    {
        return trackedEntityInstanceB;
    }

    public void setTrackedEntityInstanceB( String trackedEntityInstanceB )
    {
        this.trackedEntityInstanceB = trackedEntityInstanceB;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getRelationship()
    {
        return relationship;
    }

    public void setRelationship( String relationship )
    {
        this.relationship = relationship;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityInstance getRelative()
    {
        return relative;
    }

    public void setRelative( TrackedEntityInstance relative )
    {
        this.relative = relative;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( displayName, trackedEntityInstanceA, trackedEntityInstanceB, relationship, relative );
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
        final Relationship other = (Relationship) obj;
        return Objects.equals( this.displayName, other.displayName )
            && Objects.equals( this.trackedEntityInstanceA, other.trackedEntityInstanceA )
            && Objects.equals( this.trackedEntityInstanceB, other.trackedEntityInstanceB )
            && Objects.equals( this.relationship, other.relationship )
            && Objects.equals( this.relative, other.relative );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "displayName", displayName )
            .add( "trackedEntityInstanceA", trackedEntityInstanceA )
            .add( "trackedEntityInstanceB", trackedEntityInstanceB )
            .add( "relationship", relationship )
            .toString();
    }
}
