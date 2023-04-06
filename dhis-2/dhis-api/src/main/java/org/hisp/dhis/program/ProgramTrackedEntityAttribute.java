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
package org.hisp.dhis.program;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.adapter.DeviceRenderTypeMapSerializer;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.type.ValueTypeRenderingObject;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "programTrackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramTrackedEntityAttribute
    extends BaseIdentifiableObject
    implements EmbeddedObject
{
    private Program program;

    private TrackedEntityAttribute attribute;

    private boolean displayInList;

    private Integer sortOrder;

    private Boolean mandatory;

    private Boolean allowFutureDate;

    // TODO: Remove, replaced by renderType
    private Boolean renderOptionsAsRadio = false;

    private DeviceRenderTypeMap<ValueTypeRenderingObject> renderType;

    private Boolean searchable = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramTrackedEntityAttribute()
    {
        setAutoFields();
    }

    public ProgramTrackedEntityAttribute( Program program, TrackedEntityAttribute attribute )
    {
        this();
        this.program = program;
        this.attribute = attribute;
    }

    public ProgramTrackedEntityAttribute( Program program, TrackedEntityAttribute attribute, boolean displayInList,
        Boolean mandatory )
    {
        this( program, attribute );
        this.displayInList = displayInList;
        this.mandatory = mandatory;
    }

    public ProgramTrackedEntityAttribute( Program program, TrackedEntityAttribute attribute, boolean displayInList,
        Boolean mandatory, Integer sortOrder )
    {
        this( program, attribute );
        this.displayInList = displayInList;
        this.mandatory = mandatory;
        this.sortOrder = sortOrder;
    }

    public ProgramTrackedEntityAttribute( Program program, TrackedEntityAttribute attribute, boolean displayInList,
        Boolean mandatory, Boolean allowFutureDate )
    {
        this( program, attribute, displayInList, mandatory );
        this.allowFutureDate = allowFutureDate;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        return (program != null ? program.getDisplayName() + " " : "")
            + (attribute != null ? attribute.getDisplayName() : "");
    }

    @JsonProperty
    public String getDisplayShortName()
    {
        return (program != null ? program.getDisplayShortName() + " " : "")
            + (attribute != null ? attribute.getDisplayShortName() : "");
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        return attribute != null ? attribute.getValueType() : null;
    }

    @Override
    public String toString()
    {
        return "ProgramTrackedEntityAttribute{" +
            "class=" + getClass() +
            ", program=" + program +
            ", attribute=" + attribute +
            ", displayInList=" + displayInList +
            ", sortOrder=" + sortOrder +
            ", mandatory=" + mandatory +
            ", allowFutureDate=" + allowFutureDate +
            ", renderOptionsAsRadio=" + renderOptionsAsRadio +
            ", renderType=" + renderType +
            ", searchable=" + searchable +
            ", id=" + id +
            ", uid='" + uid + '\'' +
            ", created=" + created +
            ", lastUpdated=" + lastUpdated +
            '}';
    }
    // -------------------------------------------------------------------------
    // Getters && Setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty( "trackedEntityAttribute" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "trackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttribute getAttribute()
    {
        return attribute;
    }

    public void setAttribute( TrackedEntityAttribute attribute )
    {
        this.attribute = attribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isMandatory()
    {
        if ( mandatory != null )
        {
            return mandatory;
        }

        return false;
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
    public Boolean getAllowFutureDate()
    {
        return allowFutureDate;
    }

    public void setAllowFutureDate( Boolean allowFutureDate )
    {
        this.allowFutureDate = allowFutureDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder( Integer sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getRenderOptionsAsRadio()
    {
        return renderOptionsAsRadio;
    }

    public void setRenderOptionsAsRadio( Boolean renderOptionsAsRadio )
    {
        this.renderOptionsAsRadio = renderOptionsAsRadio;
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @JsonSerialize( using = DeviceRenderTypeMapSerializer.class )
    public DeviceRenderTypeMap<ValueTypeRenderingObject> getRenderType()
    {
        return renderType;
    }

    public void setRenderType(
        DeviceRenderTypeMap<ValueTypeRenderingObject> renderType )
    {
        this.renderType = renderType;
    }
}
