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
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.option.OptionSet;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@JacksonXmlRootElement( localName = "attributeValue", namespace = DxfNamespaces.DXF_2_0 )
public class JsonAttributeValue
        implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = -3337622176393842811L;

    /**
     * The date this object was created.
     */
    private Date created;

    /**
     * The date this object was last updated.
     */
    private Date lastUpdated;

    private String attribute;

    private String value;

    private String valueType;

    public JsonAttributeValue()
    {
        setAutoFields();
    }

    public JsonAttributeValue( String value )
    {
        this();
        this.value = value;
    }

    public JsonAttributeValue( String value, String attribute )
    {
        this( value );
        this.attribute = attribute;
    }

    public JsonAttributeValue( String value, Attribute attribute )
    {
        this( value );
        this.attribute = attribute.getUid();
    }

    public JsonAttributeValue( AttributeValue attributeValue )
    {
        this.value = attributeValue.getValue();
        this.attribute = attributeValue.getAttribute().getUid();
        this.created = attributeValue.getCreated();
        this.lastUpdated = attributeValue.getLastUpdated();
        this.valueType = attributeValue.getAttribute().getValueType().name();
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

        JsonAttributeValue that = (JsonAttributeValue) o;

        return Objects.equals( this.attribute, that.attribute ) && Objects.equals( this.value, that.value );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( attribute, value );
    }

    @Override public String toString()
    {
        return "AttributeValue{" +
                "class=" + getClass() +
                ", created=" + created +
                ", lastUpdated=" + lastUpdated +
                ", attribute=" + attribute +
                ", value='" + value + '\'' +
                '}';
    }

    @JsonProperty
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @JsonProperty
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty
    public String getAttribute()
    {
        return attribute;
    }

    public void setAttribute( String attribute )
    {
        this.attribute = attribute;
    }

    @JsonProperty
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
}
