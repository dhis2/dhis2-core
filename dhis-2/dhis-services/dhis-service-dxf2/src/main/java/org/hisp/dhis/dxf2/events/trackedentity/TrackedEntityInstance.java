package org.hisp.dhis.dxf2.events.trackedentity;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityInstance
{
    private String trackedEntity;

    private String trackedEntityInstance;

    private String orgUnit;
    
    private String created;

    private String lastUpdated;

    private List<Relationship> relationships = new ArrayList<>();

    private List<Attribute> attributes = new ArrayList<>();
    
    private Boolean inactive;

    public TrackedEntityInstance()
    {
    }

    /**
     * Trims the value property of attribute values to null.
     */
    public void trimValuesToNull()
    {
        if ( attributes != null )
        {
            for ( Attribute attribute : attributes )
            {
                attribute.setValue( StringUtils.trimToNull( attribute.getValue() ) );
            }
        }
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntity()
    {
        return trackedEntity;
    }

    public void setTrackedEntity( String trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstance()
    {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance( String trackedEntityInstance )
    {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getOrgUnit()
    {
        return orgUnit;
    }

    public void setOrgUnit( String orgUnit )
    {
        this.orgUnit = orgUnit;
    }
    
    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getCreated()
    {
        return created;
    }

    public void setCreated( String created )
    {
        this.created = created;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( isAttribute = true )
    public String getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( String lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<Relationship> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( List<Relationship> relationships )
    {
        this.relationships = relationships;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "attributes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attribute", namespace = DxfNamespaces.DXF_2_0 )
    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( List<Attribute> attributes )
    {
        this.attributes = attributes;
    }
    
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "inactive", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "inactive", namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isInactive() {
		return inactive;
	}

	public void setInactive(Boolean inactive) {
		this.inactive = inactive;
	}

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        TrackedEntityInstance that = (TrackedEntityInstance) o;

        if ( attributes != null ? !attributes.equals( that.attributes ) : that.attributes != null ) return false;
        if ( orgUnit != null ? !orgUnit.equals( that.orgUnit ) : that.orgUnit != null ) return false;
        if ( created != null ? !created.equals( that.created ) : that.created != null ) return false;
        if ( relationships != null ? !relationships.equals( that.relationships ) : that.relationships != null ) return false;
        if ( trackedEntity != null ? !trackedEntity.equals( that.trackedEntity ) : that.trackedEntity != null ) return false;
        if ( trackedEntityInstance != null ? !trackedEntityInstance.equals( that.trackedEntityInstance ) : that.trackedEntityInstance != null )
            return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = trackedEntity != null ? trackedEntity.hashCode() : 0;
        result = 31 * result + (trackedEntityInstance != null ? trackedEntityInstance.hashCode() : 0);
        result = 31 * result + (orgUnit != null ? orgUnit.hashCode() : 0);
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (relationships != null ? relationships.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "TrackedEntityInstance{" +
            "trackedEntity='" + trackedEntity + '\'' +
            ", trackedEntityInstance='" + trackedEntityInstance + '\'' +
            ", orgUnit='" + orgUnit + '\'' +
            ", created='" + created + '\'' +
            ", relationships=" + relationships +
            ", attributes=" + attributes +
            ", inactive=" + inactive +
            '}';
    }	
}
