package org.hisp.dhis.trackedentity;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.program.ProgramIndicator;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "programIndicatorDimension", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityProgramIndicatorDimension
{
    private int id;

    /**
     * Program indicator.
     */
    private ProgramIndicator programIndicator;

    /**
     * Legend set.
     */
    private LegendSet legendSet;

    /**
     * Operator and filter on this format:
     * <operator>:<filter>;<operator>:<filter>
     * Operator and filter pairs can be repeated any number of times.
     */
    private String filter;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityProgramIndicatorDimension()
    {
    }

    public TrackedEntityProgramIndicatorDimension( ProgramIndicator programIndicator, LegendSet legendSet, String filter )
    {
        this.programIndicator = programIndicator;
        this.legendSet = legendSet;
        this.filter = filter;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public String getUid()
    {
        return programIndicator != null ? programIndicator.getUid() : null;
    }

    public String getDisplayName()
    {
        return programIndicator != null ? programIndicator.getDisplayName() : null;
    }

    @Override
    public String toString()
    {
        return "[Id: " + id + ", program indicator: " + programIndicator + ", legend set: " + legendSet + ", filter: " + filter + "]";
    }

    @Override
    public int hashCode()
    {
        int result = id;
        result = 31 * result + (programIndicator != null ? programIndicator.hashCode() : 0);
        result = 31 * result + (legendSet != null ? legendSet.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);

        return result;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( o.getClass() ) )
        {
            return false;
        }

        final TrackedEntityProgramIndicatorDimension other = (TrackedEntityProgramIndicatorDimension) o;

        if ( programIndicator != null ? !programIndicator.equals( other.programIndicator ) : other.programIndicator != null )
        {
            return false;
        }

        if ( legendSet != null ? !legendSet.equals( other.legendSet ) : other.legendSet != null )
        {
            return false;
        }

        if ( filter != null ? !filter.equals( other.filter ) : other.filter != null )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramIndicator getProgramIndicator()
    {
        return programIndicator;
    }

    public void setProgramIndicator( ProgramIndicator programIndicator )
    {
        this.programIndicator = programIndicator;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
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
    public String getFilter()
    {
        return filter;
    }

    public void setFilter( String filter )
    {
        this.filter = filter;
    }
}
