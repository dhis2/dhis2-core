/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.webdomain;

import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "scheduling", namespace = DxfNamespaces.DXF_2_0 )
public class SchedulingStrategy
{
    private String resourceTableStrategy;

    private String analyticsStrategy;

    private String dataMartStrategy;

    private String monitoringStrategy;

    private String dataSynchStrategy;

    private String dataStatisticsStrategy;

    public SchedulingStrategy()
    {
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getResourceTableStrategy()
    {
        return resourceTableStrategy;
    }

    public void setResourceTableStrategy( String resourceTableStrategy )
    {
        this.resourceTableStrategy = resourceTableStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getAnalyticsStrategy()
    {
        return analyticsStrategy;
    }

    public void setAnalyticsStrategy( String analyticsStrategy )
    {
        this.analyticsStrategy = analyticsStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDataMartStrategy()
    {
        return dataMartStrategy;
    }

    public void setDataMartStrategy( String dataMartStrategy )
    {
        this.dataMartStrategy = dataMartStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getMonitoringStrategy()
    {
        return monitoringStrategy;
    }

    public void setMonitoringStrategy( String monitoringStrategy )
    {
        this.monitoringStrategy = monitoringStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDataSynchStrategy()
    {
        return dataSynchStrategy;
    }

    public void setDataSynchStrategy( String dataSynchStrategy )
    {
        this.dataSynchStrategy = dataSynchStrategy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDataStatisticsStrategy()
    {
        return dataStatisticsStrategy;
    }

    public void setDataStatisticsStrategy( String dataStatisticsStrategy )
    {
        this.dataStatisticsStrategy = dataStatisticsStrategy;
    }
}
