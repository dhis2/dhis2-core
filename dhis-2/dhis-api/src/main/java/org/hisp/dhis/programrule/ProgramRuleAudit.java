package org.hisp.dhis.programrule;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Author Zubair Asghar.
 */
public class ProgramRuleAudit
{
    private int id;

    private ProgramRule programRule;

    private List<ProgramRuleVariable> programRuleVariables = new ArrayList<>();

    private List<DataElement> dataElements = new ArrayList<>();

    private List<TrackedEntityAttribute> attributes = new ArrayList<>();

    private List<String> environmentVariables = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramRuleAudit( ProgramRule programRule )
    {
        this.programRule = programRule;
    }

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
    public ProgramRule getProgramRule()
    {
        return programRule;
    }

    public void setProgramRule( ProgramRule programRule )
    {
        this.programRule = programRule;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramRuleVariable> getProgramRuleVariables()
    {
        return programRuleVariables;
    }

    public void setProgramRuleVariables( List<ProgramRuleVariable> programRuleVariables )
    {
        this.programRuleVariables = programRuleVariables;
    }

    public void setDataElements( List<DataElement> dataElements )
    {
        this.dataElements = dataElements;
    }

    public void setAttributes( List<TrackedEntityAttribute> attributes )
    {
        this.attributes = attributes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityAttribute> getAttributes()
    {
        return attributes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getEnvironmentVariables()
    {
        return environmentVariables;
    }

    public void setEnvironmentVariables( List<String> environmentVariables )
    {
        this.environmentVariables = environmentVariables;
    }

    public void setAuditFields()
    {
        this.dataElements = programRuleVariables.stream()
            .filter( Objects::nonNull )
            .filter( v -> ProgramRuleVariableSourceType.getDataTypes().contains( v.getSourceType() ) )
            .map( ProgramRuleVariable::getDataElement )
            .collect( Collectors.toList() );

        this.attributes = programRuleVariables.stream()
            .filter( Objects::nonNull )
            .filter( v -> ProgramRuleVariableSourceType.getAttributeTypes().contains( v.getSourceType() ) )
            .map( ProgramRuleVariable::getAttribute )
            .collect( Collectors.toList() );
    }
}
