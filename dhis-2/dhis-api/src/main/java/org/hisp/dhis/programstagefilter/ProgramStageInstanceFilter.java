package org.hisp.dhis.programstagefilter;

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

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
@JacksonXmlRootElement( localName = "programStageInstanceFilter", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramStageInstanceFilter extends BaseIdentifiableObject implements MetadataObject
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Property for filtering events by program
     */
    private Program program;

    /**
     * Property for filtering events by programstage
     */
    private ProgramStage programStage;

    /**
     * Property indicating description of programStageInstanceFilter
     */
    private String description;

    /**
     * Criteria object representing selected projections, filtering and sorting
     * criteria in events
     */
    private EventQueryCriteria eventQueryCriteria;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramStageInstanceFilter()
    {

    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public EventQueryCriteria getEventQueryCriteria()
    {
        return eventQueryCriteria;
    }

    public void setEventQueryCriteria( EventQueryCriteria eventQueryCriteria )
    {
        this.eventQueryCriteria = eventQueryCriteria;
    }

    public void copyValuesFrom( ProgramStageInstanceFilter psiFilter )
    {
        if ( psiFilter != null )
        {
            this.eventQueryCriteria = psiFilter.getEventQueryCriteria();
            this.program = psiFilter.getProgram();
            this.programStage = psiFilter.getProgramStage();
            this.userAccesses = psiFilter.getUserAccesses();
            this.userGroupAccesses = psiFilter.getUserGroupAccesses();
            this.code = psiFilter.getCode();
            this.name = psiFilter.getName();
            this.description = psiFilter.getDescription();
            this.publicAccess = psiFilter.getPublicAccess();
        }
    }

}
