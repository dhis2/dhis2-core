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
package org.hisp.dhis.trackedentity;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.ValueType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
@JacksonXmlRootElement( localName = "trackedEntityTypeAttribute", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityTypeAttribute
    extends BaseIdentifiableObject
    implements EmbeddedObject
{
    private TrackedEntityType trackedEntityType;

    private TrackedEntityAttribute trackedEntityAttribute;

    private boolean displayInList;

    private Boolean mandatory;

    private Boolean searchable = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityTypeAttribute()
    {
        setAutoFields();
    }

    public TrackedEntityTypeAttribute( TrackedEntityType trackedEntityType,
        TrackedEntityAttribute trackedEntityAttribute )
    {
        this();
        this.trackedEntityType = trackedEntityType;
        this.trackedEntityAttribute = trackedEntityAttribute;
    }

    public TrackedEntityTypeAttribute( TrackedEntityType trackedEntityType, TrackedEntityAttribute attribute,
        boolean displayInList,
        Boolean mandatory )
    {
        this( trackedEntityType, attribute );
        this.displayInList = displayInList;
        this.mandatory = mandatory;
    }

    @Override
    public String getName()
    {
        return (trackedEntityType != null ? trackedEntityType.getDisplayName() + " " : "")
            + (trackedEntityAttribute != null ? trackedEntityAttribute.getDisplayName() : "");
    }

    @JsonProperty
    public String getDisplayShortName()
    {
        return (trackedEntityType != null ? trackedEntityType.getDisplayShortName() + " " : "")
            + (trackedEntityAttribute != null ? trackedEntityAttribute.getDisplayShortName() : "");
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        return trackedEntityAttribute != null ? trackedEntityAttribute.getValueType() : null;
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"id\":\"" + id + "\", " +
            "\"uid\":\"" + uid + "\", " +
            "\"trackedEntityType\":" + trackedEntityType + ", " +
            "\"trackedEntityAttribute\":" + trackedEntityAttribute + ", " +
            "\"created\":\"" + created + "\", " +
            "\"lastUpdated\":\"" + lastUpdated + "\" " +
            "}";
    }

    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityType getTrackedEntityType()
    {
        return trackedEntityType;
    }

    public void setTrackedEntityType( TrackedEntityType trackedEntityType )
    {
        this.trackedEntityType = trackedEntityType;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttribute getTrackedEntityAttribute()
    {
        return trackedEntityAttribute;
    }

    public void setTrackedEntityAttribute( TrackedEntityAttribute trackedEntityAttribute )
    {
        this.trackedEntityAttribute = trackedEntityAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isMandatory()
    {
        return mandatory;
    }

    public void setMandatory( Boolean mandatory )
    {
        this.mandatory = mandatory;
    }

    @JsonProperty
    @JacksonXmlProperty( localName = "displayInList", namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDisplayInList()
    {
        return displayInList;
    }

    public void setDisplayInList( boolean displayInList )
    {
        this.displayInList = displayInList;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isSearchable()
    {
        return searchable;
    }

    public void setSearchable( Boolean searchable )
    {
        this.searchable = searchable;
    }
}
