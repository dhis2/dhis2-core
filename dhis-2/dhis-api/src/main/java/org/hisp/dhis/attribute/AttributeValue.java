/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.attribute;

import java.io.Serializable;
import java.util.Objects;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "attributeValues", namespace = DxfNamespaces.DXF_2_0 )
public class AttributeValue
    implements Serializable, EmbeddedObject
{
    private Attribute attribute;

    private String value;

    public AttributeValue()
    {
    }

    public AttributeValue( String value )
    {
        this();
        this.value = value;
    }

    public AttributeValue( String value, Attribute attribute )
    {
        this.value = value;
        this.attribute = attribute;
    }

    public AttributeValue( Attribute attribute, String value )
    {
        this.value = value;
        this.attribute = attribute;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null || getClass() != object.getClass() )
        {
            return false;
        }

        AttributeValue that = (AttributeValue) object;

        if ( !Objects.equals( attribute, that.attribute ) )
        {
            return false;
        }

        if ( !Objects.equals( value, that.value ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 7;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "AttributeValue{" +
            "class=" + getClass() +
            ", value='" + value + '\'' +
            ", attribute='" + attribute + '\'' +
            '}';
    }

    @JsonProperty
    @JacksonXmlProperty
    @Property( required = Property.Value.TRUE )
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    @JsonProperty
    @Property( value = PropertyType.REFERENCE, required = Property.Value.TRUE )
    public Attribute getAttribute()
    {
        return attribute;
    }

    public void setAttribute( Attribute attribute )
    {
        this.attribute = attribute;
    }
}
