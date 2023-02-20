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
package org.hisp.dhis.scheduling.parameters;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.scheduling.JobParameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "jobParameters", namespace = DxfNamespaces.DXF_2_0 )
public class AnalyticsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 4613054056442242637L;

    private Integer lastYears;

    private Set<AnalyticsTableType> skipTableTypes = new HashSet<>();

    private Set<String> skipPrograms = new HashSet<>();

    private boolean skipResourceTables = false;

    public AnalyticsJobParameters()
    {
    }

    public AnalyticsJobParameters( Integer lastYears, Set<AnalyticsTableType> skipTableTypes,
        Set<String> skipPrograms, boolean skipResourceTables )
    {
        this.lastYears = lastYears;
        this.skipTableTypes = skipTableTypes;
        this.skipPrograms = skipPrograms;
        this.skipResourceTables = skipResourceTables;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getLastYears()
    {
        return lastYears;
    }

    public void setLastYears( Integer lastYears )
    {
        this.lastYears = lastYears;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "skipTableTypes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "skipTableType", namespace = DxfNamespaces.DXF_2_0 )
    public Set<AnalyticsTableType> getSkipTableTypes()
    {
        return skipTableTypes;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "skipPrograms", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "skipProgram", namespace = DxfNamespaces.DXF_2_0 )
    @OpenApi.Property( { UID[].class, Program.class } )
    public Set<String> getSkipPrograms()
    {
        return skipPrograms;
    }

    public void setSkipTableTypes( Set<AnalyticsTableType> skipTableTypes )
    {
        this.skipTableTypes = skipTableTypes;
    }

    public void setSkipPrograms( Set<String> skipPrograms )
    {
        this.skipPrograms = skipPrograms;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSkipResourceTables()
    {
        return skipResourceTables;
    }

    public void setSkipResourceTables( boolean skipResourceTables )
    {
        this.skipResourceTables = skipResourceTables;
    }

    @Override
    public Optional<ErrorReport> validate()
    {
        return Optional.empty();
    }
}
