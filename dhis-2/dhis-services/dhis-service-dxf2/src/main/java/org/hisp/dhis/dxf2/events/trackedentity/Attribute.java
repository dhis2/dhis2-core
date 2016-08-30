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
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.ValueType;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "attribute", namespace = DxfNamespaces.DXF_2_0 )
public class Attribute
{
    private String displayName;

    private String attribute;

    private String created;

    private String lastUpdated;

    private ValueType valueType;

    private String code;

    private String value;

    public Attribute()
    {
    }

    public Attribute( String value )
    {
        this.value = value;
    }

    public Attribute( String attribute, ValueType valueType, String value )
    {
        this.attribute = attribute;
        this.valueType = valueType;
        this.value = value;
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
    public String getAttribute()
    {
        return attribute;
    }

    public void setAttribute( String attribute )
    {
        this.attribute = attribute;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getCreated()
    {
        return created;
    }

    public void setCreated( String created )
    {
        this.created = created;
    }

    @JsonProperty
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
    @JacksonXmlProperty( isAttribute = true )
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;

        Attribute attribute1 = (Attribute) o;

        if ( attribute != null ? !attribute.equals( attribute1.attribute ) : attribute1.attribute != null )
        {
            return false;
        }
        
        if ( displayName != null ? !displayName.equals( attribute1.displayName ) : attribute1.displayName != null )
        {
            return false;
        }
        
        if ( valueType != null ? !valueType.equals( attribute1.valueType ) : attribute1.valueType != null )
        {
            return false;
        }
        
        if ( code != null ? !code.equals( attribute1.code ) : attribute1.code != null )
        {
            return false;
        }
        
        if ( value != null ? !value.equals( attribute1.value ) : attribute1.value != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        result = 31 * result + (valueType != null ? valueType.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "Attribute{" + "displayName='" + displayName + '\'' + ", attribute='" + attribute + '\'' + ", type='"
            + valueType + '\'' + ", code='" + code + '\'' + ", value='" + value + '\'' + '}';
    }
}