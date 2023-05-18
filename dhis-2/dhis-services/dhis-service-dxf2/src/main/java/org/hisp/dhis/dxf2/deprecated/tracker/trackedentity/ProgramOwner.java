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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity;

import java.util.Objects;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Ameen Mohamed <ameen@dhis2.com>
 *
 * @deprecated this is a class related to "old" (deprecated) tracker which will
 *             be removed with "old" tracker. Make sure to plan migrating to new
 *             tracker.
 */
@Deprecated( since = "2.41" )
@JacksonXmlRootElement( localName = "programOwner", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramOwner
{
    private String ownerOrgUnit;

    private String trackedEntityInstance;

    private String program;

    public ProgramOwner()
    {
    }

    public ProgramOwner( TrackedEntityProgramOwner programOwner )
    {
        this.ownerOrgUnit = programOwner.getOrganisationUnit().getUid();
        this.program = programOwner.getProgram().getUid();
        this.trackedEntityInstance = programOwner.getTrackedEntity().getUid();
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getOwnerOrgUnit()
    {
        return ownerOrgUnit;
    }

    public void setOwnerOrgUnit( String ownerOrgUnit )
    {
        this.ownerOrgUnit = ownerOrgUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getTrackedEntityInstance()
    {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance( String trackedEntityInstance )
    {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getProgram()
    {
        return program;
    }

    public void setProgram( String program )
    {
        this.program = program;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( trackedEntityInstance, program, ownerOrgUnit );
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
        final ProgramOwner other = (ProgramOwner) obj;
        return Objects.equals( this.trackedEntityInstance, other.trackedEntityInstance )
            && Objects.equals( this.program, other.program )
            && Objects.equals( this.ownerOrgUnit, other.ownerOrgUnit );
    }

    @Override
    public String toString()
    {
        return "ProgramOwner{" +
            "ownerOrgUnit='" + ownerOrgUnit + '\'' +
            ", trackedEntityInstance='" + trackedEntityInstance + '\'' +
            ", program='" + program + '\'' +
            '}';
    }
}
