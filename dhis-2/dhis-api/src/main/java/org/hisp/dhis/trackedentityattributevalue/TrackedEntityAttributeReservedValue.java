package org.hisp.dhis.trackedentityattributevalue;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Markus Bekken
 */
@JacksonXmlRootElement( localName = "trackedEntityAttributeReservedValue", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityAttributeReservedValue
    implements Serializable
{
    /**
     * Explicit serialization version UID
     */
    private static final long serialVersionUID = -1439881198734016116L;

    private int id;

    private TrackedEntityAttribute trackedEntityAttribute;

    private Date created;

    private Date expiryDate;

    private String value;

    private TrackedEntityInstance valueUtilizedByTEI;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    public TrackedEntityAttributeReservedValue()
    {
        setAutoFields();
    }

    public TrackedEntityAttributeReservedValue( TrackedEntityAttribute attribute )
    {
        setTrackedEntityAttribute( attribute );
    }

    public TrackedEntityAttributeReservedValue( TrackedEntityAttribute attribute, String value )
    {
        setTrackedEntityAttribute( attribute );
        setValue( value );
    }

    public void setAutoFields()
    {
        Calendar c = Calendar.getInstance();

        if ( getCreated() == null )
        {
            setCreated( c.getTime() );
        }

        if ( getExpiryDate() == null )
        {
            c.add( Calendar.YEAR, 1 );
            setExpiryDate( c.getTime() );
        }
    }

    // -------------------------------------------------------------------------
    // hashCode and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getTrackedEntityAttribute() == null) ? 0 : getTrackedEntityAttribute().hashCode());
        result = prime * result + ((getValue() == null) ? 0 : getValue().hashCode());
        result = prime * result + ((getCreated() == null) ? 0 : getCreated().hashCode());
        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( object.getClass() ) )
        {
            return false;
        }

        final TrackedEntityAttributeReservedValue other = (TrackedEntityAttributeReservedValue) object;

        if ( trackedEntityAttribute == null )
        {
            if ( other.trackedEntityAttribute != null )
            {
                return false;
            }
        }
        else if ( !trackedEntityAttribute.equals( other.trackedEntityAttribute ) )
        {
            return false;
        }

        if ( getValue() == null )
        {
            if ( other.getValue() != null )
            {
                return false;
            }
        }
        else if ( !getValue().equals( other.getValue() ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "[Tracked attribute=" + trackedEntityAttribute.getUid() + ", value='" + getValue() +
            "']";
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

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
    public Date getExpiryDate()
    {
        return expiryDate;
    }

    public void setExpiryDate( Date expiryDate )
    {
        this.expiryDate = expiryDate;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getValue()
    {
        return this.value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    @JsonProperty( "valueUtilizedByTEI" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "ValueUtilizedByTEI", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityInstance getValueUtilizedByTEI()
    {
        return valueUtilizedByTEI;
    }

    public void setValueUtilizedByTEI( TrackedEntityInstance valueUtilizedByTEI )
    {
        this.valueUtilizedByTEI = valueUtilizedByTEI;
    }

    @JsonProperty( "trackedEntityAttribute" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "trackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttribute getTrackedEntityAttribute()
    {
        return trackedEntityAttribute;
    }

    public void setTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        this.trackedEntityAttribute = attribute;
    }
}