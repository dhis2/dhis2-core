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
package org.hisp.dhis.programstagefilter;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.translation.Translatable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
@JacksonXmlRootElement( localName = "programStageInstanceFilter", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramStageInstanceFilter extends BaseIdentifiableObject
    implements MetadataObject
{

    private static final long serialVersionUID = 1L;

    /**
     * Property for filtering events by program
     */
    private String program;

    /**
     * Property for filtering events by programstage
     */
    private String programStage;

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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.IDENTIFIER, required = Property.Value.TRUE )
    @PropertyRange( min = 11, max = 11 )
    public String getProgram()
    {
        return program;
    }

    public void setProgram( String program )
    {
        this.program = program;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.IDENTIFIER, required = Property.Value.FALSE )
    @PropertyRange( min = 11, max = 11 )
    public String getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( String programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDescription()
    {
        return description;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Translatable( propertyName = "description", key = "DESCRIPTION" )
    public String getDisplayDescription()
    {
        return getTranslation( "DESCRIPTION", getDescription() );
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

            this.sharing = psiFilter.getSharing().copy();

            this.code = psiFilter.getCode();
            this.name = psiFilter.getName();
            this.description = psiFilter.getDescription();
        }
    }

}
