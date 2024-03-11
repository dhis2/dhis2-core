package org.hisp.dhis.attribute;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MergeableObject;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "attributeValue", namespace = DxfNamespaces.DXF_2_0 )
public class AttributeValue
    implements MergeableObject, Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -6625127769248931066L;

    private int id;

    /**
     * The date this object was created.
     */
    private Date created;

    /**
     * The date this object was last updated.
     */
    private Date lastUpdated;

    private Attribute attribute;

    private String value;

    public AttributeValue()
    {
        setAutoFields();
    }

    public AttributeValue( String value )
    {
        this();
        this.value = value;
    }

    public AttributeValue( String value, Attribute attribute )
    {
        this( value );
        this.attribute = attribute;
    }

    public void setAutoFields()
    {
        if ( created == null )
        {
            created = new Date();
        }

        lastUpdated = new Date();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        AttributeValue that = (AttributeValue) o;

        if ( id != that.id ) return false;
        if ( attribute != null ? !attribute.equals( that.attribute ) : that.attribute != null ) return false;
        if ( value != null ? !value.equals( that.value ) : that.value != null ) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override public String toString()
    {
        return "AttributeValue{" +
            "class=" + getClass() +
            ", id=" + id +
            ", created=" + created +
            ", lastUpdated=" + lastUpdated +
            ", attribute=" + attribute +
            ", value='" + value + '\'' +
            '}';
    }


    @JsonIgnore
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Attribute getAttribute()
    {
        return attribute;
    }

    public void setAttribute( Attribute attribute )
    {
        this.attribute = attribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public boolean isUnique()
    {
        return attribute != null && attribute.isUnique();
    }

    public boolean isMandatory()
    {
        return attribute != null && attribute.isMandatory();
    }

    @Override
    public boolean mergeableEquals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        AttributeValue that = (AttributeValue) o;

        if ( !Objects.equals( attribute, that.attribute ) )
        {
            return false;
        }

        return true;
    }
}
