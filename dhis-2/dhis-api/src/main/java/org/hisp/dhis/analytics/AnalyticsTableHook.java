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
package org.hisp.dhis.analytics;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.resourcetable.ResourceTableType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "analyticsTableHook", namespace = DxfNamespaces.DXF_2_0 )
public class AnalyticsTableHook
    extends BaseIdentifiableObject
    implements MetadataObject
{
    private AnalyticsTablePhase phase;

    private ResourceTableType resourceTableType;

    private AnalyticsTableType analyticsTableType;

    private String sql;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AnalyticsTableHook()
    {
    }

    public AnalyticsTableHook( String name, AnalyticsTablePhase phase, ResourceTableType resourceTableType, String sql )
    {
        this.name = name;
        this.phase = phase;
        this.resourceTableType = resourceTableType;
        this.sql = sql;
    }

    public AnalyticsTableHook( String name, AnalyticsTablePhase phase, AnalyticsTableType analyticsTableType,
        String sql )
    {
        this.name = name;
        this.phase = phase;
        this.analyticsTableType = analyticsTableType;
        this.sql = sql;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AnalyticsTablePhase getPhase()
    {
        return phase;
    }

    public void setPhase( AnalyticsTablePhase phase )
    {
        this.phase = phase;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ResourceTableType getResourceTableType()
    {
        return resourceTableType;
    }

    public void setResourceTableType( ResourceTableType resourceTableType )
    {
        this.resourceTableType = resourceTableType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AnalyticsTableType getAnalyticsTableType()
    {
        return analyticsTableType;
    }

    public void setAnalyticsTableType( AnalyticsTableType analyticsTableType )
    {
        this.analyticsTableType = analyticsTableType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSql()
    {
        return sql;
    }

    public void setSql( String sql )
    {
        this.sql = sql;
    }
}
