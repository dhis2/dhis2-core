package org.hisp.dhis.common;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.legend.LegendSet;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class BaseDimensionalItemObject
    extends BaseNameableObject
    implements DimensionalItemObject
{
    /**
     * The dimension type.
     */
    private DimensionItemType dimensionItemType;

    /**
     * The legend sets for this dimension.
     */
    protected List<LegendSet> legendSets = new ArrayList<>();

    /**
     * The aggregation type for this dimension.
     */
    protected AggregationType aggregationType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public BaseDimensionalItemObject()
    {
    }

    public BaseDimensionalItemObject( String dimensionItem )
    {
        this.uid = dimensionItem;
        this.code = dimensionItem;
        this.name = dimensionItem;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public boolean hasLegendSet()
    {
        return legendSets != null && !legendSets.isEmpty();
    }

    @Override
    public boolean hasAggregationType()
    {
        return getAggregationType() != null;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDimensionItem()
    {
        return getUid();
    }

    @Override
    public String getDimensionItem( IdScheme idScheme )
    {
        return getPropertyValue( idScheme );
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DimensionItemType getDimensionItemType()
    {
        return dimensionItemType;
    }

    public void setDimensionItemType( DimensionItemType dimensionItemType )
    {
        this.dimensionItemType = dimensionItemType;
    }

    @Override
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "legendSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "legendSets", namespace = DxfNamespaces.DXF_2_0 )
    public List<LegendSet> getLegendSets()
    {
        return this.legendSets;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LegendSet getLegendSet()
    {
        return legendSets.isEmpty() ? null : legendSets.get( 0 );
    }

    public void setLegendSets( List<LegendSet> legendSets )
    {
        this.legendSets = legendSets;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public void setAggregationType( AggregationType aggregationType )
    {
        this.aggregationType = aggregationType;
    }
}
