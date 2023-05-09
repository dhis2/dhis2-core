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
package org.hisp.dhis.program;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.trackedentity.TrackedEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
@JacksonXmlRootElement( localName = "programTempOwnershipAudit", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramTempOwnershipAudit implements Serializable
{
    private static final long serialVersionUID = 6713155272099925278L;

    private int id;

    private Program program;

    private String reason;

    private Date created;

    private String accessedBy;

    private TrackedEntity entityInstance;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramTempOwnershipAudit()
    {
    }

    public ProgramTempOwnershipAudit( Program program, TrackedEntity entityInstance, String reason,
        String accessedBy )
    {
        this.program = program;
        this.reason = reason;
        this.accessedBy = accessedBy;
        this.created = new Date();
        this.entityInstance = entityInstance;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( program, entityInstance, reason, created, accessedBy );
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

        final ProgramTempOwnershipAudit other = (ProgramTempOwnershipAudit) obj;

        return Objects.equals( this.program, other.program )
            && Objects.equals( this.reason, other.reason ) && Objects.equals( this.created, other.created )
            && Objects.equals( this.accessedBy, other.accessedBy )
            && Objects.equals( this.entityInstance, other.entityInstance );
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

    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    public TrackedEntity getEntityInstance()
    {
        return entityInstance;
    }

    public void setEntityInstance( TrackedEntity entityInstance )
    {
        this.entityInstance = entityInstance;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getReason()
    {
        return reason;
    }

    public void setReason( String reason )
    {
        this.reason = reason;
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
}
