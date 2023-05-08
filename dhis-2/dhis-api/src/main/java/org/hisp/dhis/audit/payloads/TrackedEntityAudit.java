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
package org.hisp.dhis.audit.payloads;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This class is deprecated.
 *
 * @author Abyot Asalefew Gizaw abyota@gmail.com
 */
@JacksonXmlRootElement( localName = "trackedEntityInstanceAudit", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityAudit
    implements Serializable
{
    private static final long serialVersionUID = 4260110537887403524L;

    private long id;

    private String trackedEntity;

    private String comment;

    private Date created;

    private String accessedBy;

    private AuditType auditType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityAudit()
    {
    }

    public TrackedEntityAudit( String trackedEntity, String accessedBy, AuditType auditType )
    {
        this.trackedEntity = trackedEntity;
        this.accessedBy = accessedBy;
        this.created = new Date();
        this.auditType = auditType;
    }

    public TrackedEntityAudit( String trackedEntity, String comment, Date created, String accessedBy,
        AuditType auditType )
    {
        this( trackedEntity, accessedBy, auditType );
        this.comment = comment;
        this.created = created;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( trackedEntity, comment, created, accessedBy, auditType );
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

        final TrackedEntityAudit other = (TrackedEntityAudit) obj;

        return Objects.equals( this.trackedEntity, other.trackedEntity )
            && Objects.equals( this.comment, other.comment )
            && Objects.equals( this.created, other.created )
            && Objects.equals( this.accessedBy, other.accessedBy )
            && Objects.equals( this.auditType, other.auditType );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public long getId()
    {
        return id;
    }

    public void setId( long id )
    {
        this.id = id;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getTrackedEntity()
    {
        return trackedEntity;
    }

    public void setTrackedEntity( String trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
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
    public String getAccessedBy()
    {
        return accessedBy;
    }

    public void setAccessedBy( String accessedBy )
    {
        this.accessedBy = accessedBy;
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
