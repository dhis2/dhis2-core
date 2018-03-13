package org.hisp.dhis.trackedentity;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "trackedEntityType", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityType
    extends BaseNameableObject implements MetadataObject
{
    private List<TrackedEntityTypeAttribute> trackedEntityTypeAttributes = new ArrayList<>();

    private ObjectStyle style;

    private String formName;
    
    /**
     * Property indicating minimum number of attributes required to fill
     * before search is triggered
     */
    private int minAttributesRequiredToSearch = 1;
    
    /**
     * Property indicating maximum number of TEI to return after search
     */
    private int maxTeiCountToReturn = 0;
    
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
    
    public TrackedEntityType()
    {

    }

    public TrackedEntityType( String name, String description )
    {
        this.name = name;
        this.description = description;
    }
    
    // -------------------------------------------------------------------------
    // Logic methods
    // -------------------------------------------------------------------------
    
    /**
     * Returns TrackedEntityAttributes from TrackedEntityTypeAttributes.
     */
    public List<TrackedEntityAttribute> getTrackedEntityAttributes()
    {
        return trackedEntityTypeAttributes.stream().map( TrackedEntityTypeAttribute::getTrackedEntityAttribute ).collect( Collectors.toList() );
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------    
    
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityTypeAttribute> getTrackedEntityTypeAttributes()
    {
        return trackedEntityTypeAttributes;
    }

    public void setTrackedEntityTypeAttributes( List<TrackedEntityTypeAttribute> trackedEntityTypeAttributes )
    {
        this.trackedEntityTypeAttributes = trackedEntityTypeAttributes;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getMinAttributesRequiredToSearch()
    {
        return minAttributesRequiredToSearch;
    }

    public void setMinAttributesRequiredToSearch( int minAttributesRequiredToSearch )
    {
        this.minAttributesRequiredToSearch = minAttributesRequiredToSearch;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getMaxTeiCountToReturn()
    {
        return maxTeiCountToReturn;
    }

    public void setMaxTeiCountToReturn( int maxTeiCountToReturn )
    {
        this.maxTeiCountToReturn = maxTeiCountToReturn;
    }
    
    // -------------------------------------------------------------------------
    // Logic methods
    // -------------------------------------------------------------------------    

    /**
     * Returns IDs of searchable TrackedEntityAttributes.
     */
    public List<String> getSearchableAttributeIds()
    {
        List<String> searchableAttributes = new ArrayList<>();
        
        for ( TrackedEntityTypeAttribute trackedEntityTypeAttribute : trackedEntityTypeAttributes )
        {
            if ( trackedEntityTypeAttribute.isSearchable() || trackedEntityTypeAttribute.getTrackedEntityAttribute().isSystemWideUnique()  )
            {
                searchableAttributes.add( trackedEntityTypeAttribute.getTrackedEntityAttribute().getUid() );
            }
        }

        return searchableAttributes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ObjectStyle getStyle()
    {
        return style;
    }

    public void setStyle( ObjectStyle style )
    {
        this.style = style;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getFormName()
    {
        return formName;
    }

    public void setFormName( String formName )
    {
        this.formName = formName;
    }
}
