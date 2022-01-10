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
package org.hisp.dhis.visualization;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import java.io.Serializable;

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.schema.annotation.PropertyRange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Class representing a series item in a chart.
 *
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "seriesItem", namespace = DXF_2_0 )
public class Series
    implements Serializable
{
    /**
     * Refers to a {@link DimensionalItemObject#getDimensionItem()}.
     */
    private String dimensionItem;

    /**
     * The series axis. 0 represents the primary axis, 1 represents the
     * secondary axis.
     */
    private Integer axis;

    /**
     * Visualization type for the series. Will override the type specified by
     * {@link Visualization#getType()}.
     */
    private VisualizationType type;

    public Series()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getDimensionItem()
    {
        return dimensionItem;
    }

    public void setDimensionItem( String dimensionItem )
    {
        this.dimensionItem = dimensionItem;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    @PropertyRange( min = 0 )
    public Integer getAxis()
    {
        return axis;
    }

    public void setAxis( Integer axis )
    {
        this.axis = axis;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public VisualizationType getType()
    {
        return type;
    }

    public void setType( VisualizationType type )
    {
        this.type = type;
    }
}
