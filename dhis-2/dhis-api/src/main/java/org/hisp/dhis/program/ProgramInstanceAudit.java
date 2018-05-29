package org.hisp.dhis.program;

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

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
@JacksonXmlRootElement( localName = "programInstanceAudit", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramInstanceAudit implements Serializable
{
    private static final long serialVersionUID = 6713155272099925278L;
    
    private int id;

    private ProgramInstance programInstance;

    private String comment;
    
    private Date created;

    private String accessedBy;   

    private AuditType auditType;
    
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------
    
    public ProgramInstanceAudit()
    {
    }
    
    public ProgramInstanceAudit( ProgramInstance programInstance, String accessedBy, AuditType auditType )
    {
        this.programInstance = programInstance;
        this.accessedBy = accessedBy;
        this.created = new Date();
        this.auditType = auditType;
    }
    
    public ProgramInstanceAudit( ProgramInstance programInstance, String comment, String accessedBy, AuditType auditType )
    {
        this.programInstance = programInstance;
        this.comment = comment;
        this.accessedBy = accessedBy;
        this.created = new Date();
        this.auditType = auditType;
    }
    
    @Override
    public int hashCode()
    {
        return Objects.hash( programInstance, comment, created, accessedBy, auditType );
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
        
        final ProgramInstanceAudit other = (ProgramInstanceAudit) obj;
        
        return Objects.equals( this.programInstance,  other.programInstance )
            && Objects.equals( this.comment, other.comment )
            && Objects.equals( this.created, other.created )
            && Objects.equals( this.accessedBy, other.accessedBy )
            && Objects.equals( this.auditType, other.auditType );
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramInstance getProgramInstance()
    {
        return programInstance;
    }

    public void setProgramInstance( ProgramInstance programInstance )
    {
        this.programInstance = programInstance;
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
