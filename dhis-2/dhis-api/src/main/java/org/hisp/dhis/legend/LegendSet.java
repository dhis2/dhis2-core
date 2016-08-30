package org.hisp.dhis.legend;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jan Henrik Overland
 */
@JacksonXmlRootElement( localName = "legendSet", namespace = DxfNamespaces.DXF_2_0 )
public class LegendSet
    extends BaseIdentifiableObject
{
    private String symbolizer;

    private Set<Legend> legends = new HashSet<>();

    public LegendSet()
    {
    }

    public LegendSet( String name, String symbolizer, Set<Legend> legends )
    {
        this.name = name;
        this.symbolizer = symbolizer;
        this.legends = legends;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void removeAllLegends()
    {
        legends.clear();
    }

    public Legend getLegendByUid( String uid )
    {
        for ( Legend legend : legends )
        {
            if ( legend != null && legend.getUid().equals( uid ) )
            {
                return legend;
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSymbolizer()
    {
        return symbolizer;
    }

    public void setSymbolizer( String symbolizer )
    {
        this.symbolizer = symbolizer;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "legends", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "legend", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Legend> getLegends()
    {
        return legends;
    }

    public void setLegends( Set<Legend> legends )
    {
        this.legends = legends;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            LegendSet legendSet = (LegendSet) other;

            if ( mergeMode.isReplace() )
            {
                symbolizer = legendSet.getSymbolizer();
            }
            else if ( mergeMode.isMerge() )
            {
                symbolizer = legendSet.getSymbolizer() == null ? symbolizer : legendSet.getSymbolizer();
            }

            removeAllLegends();
            legends.addAll( legendSet.getLegends() );
        }
    }
}