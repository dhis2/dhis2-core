package org.hisp.dhis.mapping;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.legend.LegendSet;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@JacksonXmlRootElement( localName = "externalMapLayer", namespace = DxfNamespaces.DXF_2_0 )
public class ExternalMapLayer
    extends BaseIdentifiableObject implements MetadataObject
{
    private MapService mapService;

    private String url;

    private String attribution;

    private String layers;

    private ImageFormat imageFormat;

    private MapLayerPosition mapLayerPosition;

    private LegendSet legendSet;

    private String legendSetUrl;

    //-----------------------------------------------------
    // Constructor
    //-----------------------------------------------------

    public ExternalMapLayer()
    {
    }

    public ExternalMapLayer( String name )
    {
        this.name = name;
    }

    //-----------------------------------------------------
    // Getters & Setters
    //-----------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MapService getMapService()
    {
        return mapService;
    }

    public void setMapService( MapService mapService )
    {
        this.mapService = mapService;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getAttribution()
    {
        return attribution;
    }

    public void setAttribution( String attribution )
    {
        this.attribution = attribution;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLayers()
    {
        return layers;
    }

    public void setLayers( String layers )
    {
        this.layers = layers;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImageFormat getImageFormat()
    {
        return imageFormat;
    }

    public void setImageFormat( ImageFormat imageFormat )
    {
        this.imageFormat = imageFormat;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MapLayerPosition getMapLayerPosition()
    {
        return mapLayerPosition;
    }

    public void setMapLayerPosition( MapLayerPosition mapLayerPosition )
    {
        this.mapLayerPosition = mapLayerPosition;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSet;
    }

    public void setLegendSet( LegendSet legendSet )
    {
        this.legendSet = legendSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLegendSetUrl()
    {
        return legendSetUrl;
    }

    public void setLegendSetUrl( String legendSetUrl )
    {
        this.legendSetUrl = legendSetUrl;
    }
}
