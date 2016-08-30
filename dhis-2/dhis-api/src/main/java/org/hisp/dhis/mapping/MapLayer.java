package org.hisp.dhis.mapping;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

/**
 * @author Jan Henrik Overland
 */
@JacksonXmlRootElement( localName = "mapLayer", namespace = DxfNamespaces.DXF_2_0 )
public class MapLayer
    extends BaseIdentifiableObject
{
    private String type;

    private String url;

    private String layers;

    private String time;

    private String fillColor;

    private double fillOpacity;

    private String strokeColor;

    private int strokeWidth;

    public MapLayer()
    {
    }

    public MapLayer( String name, String type, String url, String layers, String time, String fillColor,
        double fillOpacity, String strokeColor, int strokeWidth )
    {
        this.name = name;
        this.type = type;
        this.url = url;
        this.layers = layers;
        this.time = time;
        this.fillColor = fillColor;
        this.fillOpacity = fillOpacity;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getType()
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.URL )
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
    public String getTime()
    {
        return time;
    }

    public void setTime( String time )
    {
        this.time = time;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.COLOR )
    public String getFillColor()
    {
        return fillColor;
    }

    public void setFillColor( String fillColor )
    {
        this.fillColor = fillColor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public double getFillOpacity()
    {
        return fillOpacity;
    }

    public void setFillOpacity( double fillOpacity )
    {
        this.fillOpacity = fillOpacity;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.COLOR )
    public String getStrokeColor()
    {
        return strokeColor;
    }

    public void setStrokeColor( String strokeColor )
    {
        this.strokeColor = strokeColor;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getStrokeWidth()
    {
        return strokeWidth;
    }

    public void setStrokeWidth( int strokeWidth )
    {
        this.strokeWidth = strokeWidth;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            MapLayer mapLayer = (MapLayer) other;

            strokeWidth = mapLayer.getStrokeWidth();
            fillOpacity = mapLayer.getFillOpacity();

            if ( mergeMode.isReplace() )
            {
                type = mapLayer.getType();
                url = mapLayer.getUrl();
                layers = mapLayer.getLayers();
                time = mapLayer.getTime();
                fillColor = mapLayer.getFillColor();
                strokeColor = mapLayer.getStrokeColor();
            }
            else if ( mergeMode.isMerge() )
            {
                type = mapLayer.getType() == null ? type : mapLayer.getType();
                url = mapLayer.getUrl() == null ? url : mapLayer.getUrl();
                layers = mapLayer.getLayers() == null ? layers : mapLayer.getLayers();
                time = mapLayer.getTime() == null ? time : mapLayer.getTime();
                fillColor = mapLayer.getFillColor() == null ? fillColor : mapLayer.getFillColor();
                strokeColor = mapLayer.getStrokeColor() == null ? strokeColor : mapLayer.getStrokeColor();
            }
        }
    }
}