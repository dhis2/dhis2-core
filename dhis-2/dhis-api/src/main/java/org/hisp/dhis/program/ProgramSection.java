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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.adapter.DeviceRenderTypeMapSerializer;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.type.SectionRenderingObject;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "programSection", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramSection
    extends BaseNameableObject
    implements MetadataObject
{
    private String description;

    private Program program;

    private List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<TrackedEntityAttribute>();

    private Integer sortOrder;

    private ObjectStyle style;

    private String formName;

    /**
     * The renderType defines how the ProgramStageSection should be rendered on
     * the client
     */
    private DeviceRenderTypeMap<SectionRenderingObject> renderType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramSection()
    {
    }

    public ProgramSection( String name, List<TrackedEntityAttribute> trackedEntityAttributes )
    {
        this.name = name;
        this.trackedEntityAttributes = trackedEntityAttributes;
    }

    public ProgramSection( String name, List<TrackedEntityAttribute> trackedEntityAttributes, Integer sortOrder )
    {
        this( name, trackedEntityAttributes );
        this.sortOrder = sortOrder;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

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

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "trackedEntityAttributes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "trackedEntityAttributes", namespace = DxfNamespaces.DXF_2_0 )
    public List<TrackedEntityAttribute> getTrackedEntityAttributes()
    {
        return trackedEntityAttributes;
    }

    public void setTrackedEntityAttributes( List<TrackedEntityAttribute> trackedEntityAttributes )
    {
        this.trackedEntityAttributes = trackedEntityAttributes;
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @JsonSerialize( using = DeviceRenderTypeMapSerializer.class )
    public DeviceRenderTypeMap<SectionRenderingObject> getRenderType()
    {
        return renderType;
    }

    public void setRenderType(
        DeviceRenderTypeMap<SectionRenderingObject> renderType )
    {
        this.renderType = renderType;
    }

    public static final BiFunction<ProgramSection, Program, ProgramSection> copyOf = ( section, prog ) -> {
        ProgramSection copy = new ProgramSection();
        copy.setProgram( prog );
        copy.setAutoFields();
        setShallowCopyValues( copy, section );
        return copy;
    };

    private static void setShallowCopyValues( ProgramSection copy, ProgramSection original )
    {
        copy.setAccess( original.getAccess() );
        copy.setDescription( original.getDescription() );
        copy.setFormName( original.getFormName() );
        copy.setName( original.getName() );
        copy.setPublicAccess( original.getPublicAccess() );
        copy.setRenderType( original.getRenderType() );
        copy.setSharing( original.getSharing() );
        copy.setShortName( original.getShortName() );
        copy.setSortOrder( original.getSortOrder() );
        copy.setStyle( original.getStyle() );
        copy.setTrackedEntityAttributes( ObjectUtils.copyOf( original.getTrackedEntityAttributes() ) );
        copy.setTranslations( original.getTranslations() );
    }
}
