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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "trackedEntityAttributeValueAudit", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityAttributeValueAudit
    implements Serializable
{
    private int id;

    private TrackedEntityAttribute attribute;

    private TrackedEntityInstance entityInstance;

    private Date created;

    private String plainValue;

    private String encryptedValue;

    private String modifiedBy;

    private AuditType auditType;

    /**
     * This value is only used to store values from setValue when we don't know
     * if attribute is set or not.
     */
    private String value;

    public TrackedEntityAttributeValueAudit()
    {
    }

    public TrackedEntityAttributeValueAudit( TrackedEntityAttributeValue trackedEntityAttributeValue, String value,
        String modifiedBy, AuditType auditType )
    {
        this.attribute = trackedEntityAttributeValue.getAttribute();
        this.entityInstance = trackedEntityAttributeValue.getEntityInstance();

        this.created = new Date();
        this.value = value;
        this.modifiedBy = modifiedBy;
        this.auditType = auditType;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( attribute, entityInstance, created, getValue(), modifiedBy, auditType );
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

        final TrackedEntityAttributeValueAudit other = (TrackedEntityAttributeValueAudit) obj;

        return Objects.equals( this.attribute, other.attribute )
            && Objects.equals( this.entityInstance, other.entityInstance )
            && Objects.equals( this.created, other.created )
            && Objects.equals( this.getValue(), other.getValue() )
            && Objects.equals( this.modifiedBy, other.modifiedBy )
            && Objects.equals( this.auditType, other.auditType );
    }

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public String getPlainValue()
    {
        return (!getAttribute().getConfidential() && this.value != null ? this.value : this.plainValue);
    }

    public void setPlainValue( String plainValue )
    {
        this.plainValue = plainValue;
    }

    public String getEncryptedValue()
    {
        return (getAttribute().getConfidential() && this.value != null ? this.value : this.encryptedValue);
    }

    public void setEncryptedValue( String encryptedValue )
    {
        this.encryptedValue = encryptedValue;
    }

    @JsonProperty( "trackedEntityAttribute" )
    @JacksonXmlProperty( localName = "trackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttribute getAttribute()
    {
        return attribute;
    }

    public void setAttribute( TrackedEntityAttribute attribute )
    {
        this.attribute = attribute;
    }

    @JsonProperty( "trackedEntityInstance" )
    @JacksonXmlProperty( localName = "trackedEntityInstance", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityInstance getEntityInstance()
    {
        return entityInstance;
    }

    public void setEntityInstance( TrackedEntityInstance entityInstance )
    {
        this.entityInstance = entityInstance;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getCreated()
    {
        return created;
    }

    public void setCreated( Date created )
    {
        this.created = created;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getValue()
    {
        return (getAttribute().getConfidential() ? this.getEncryptedValue() : this.getPlainValue());
    }

    /**
     * Property which temporarily stores the attribute value. The
     * {@link getEncryptedValue} and {@link getPlainValue} methods handle the
     * value when requested.
     *
     * @param value the value to be stored.
     */
    public void setValue( String value )
    {
        this.value = value;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getModifiedBy()
    {
        return modifiedBy;
    }

    public void setModifiedBy( String modifiedBy )
    {
        this.modifiedBy = modifiedBy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AuditType getAuditType()
    {
        return auditType;
    }

    public void setAuditType( AuditType auditType )
    {
        this.auditType = auditType;
    }
}
