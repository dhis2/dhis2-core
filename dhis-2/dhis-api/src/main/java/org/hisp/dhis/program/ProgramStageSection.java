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
import java.util.Map;
import java.util.Objects;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.adapter.DeviceRenderTypeMapSerializer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.type.SectionRenderingObject;
import org.hisp.dhis.util.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "programStageSection", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramStageSection
    extends BaseNameableObject
    implements MetadataObject
{
    private String description;

    private ProgramStage programStage;

    private List<DataElement> dataElements = new ArrayList<>();

    private List<ProgramIndicator> programIndicators = new ArrayList<>();

    private Integer sortOrder;

    /**
     * The style represents how the ProgramStageSection should be presented on
     * the client
     */
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

    public ProgramStageSection()
    {
    }

    public ProgramStageSection( String name, List<DataElement> dataElements )
    {
        this.name = name;
        this.dataElements = dataElements;
    }

    public ProgramStageSection( String name, List<DataElement> dataElements, Integer sortOrder )
    {
        this( name, dataElements );
        this.sortOrder = sortOrder;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasProgramStage()
    {
        return programStage != null;
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
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "dataElements", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataElement", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( List<DataElement> dataElements )
    {
        this.dataElements = dataElements;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "programIndicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "programIndicator", namespace = DxfNamespaces.DXF_2_0 )
    public List<ProgramIndicator> getProgramIndicators()
    {
        return programIndicators;
    }

    public void setProgramIndicators( List<ProgramIndicator> programIndicators )
    {
        this.programIndicators = programIndicators;
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

    public void setRenderType( DeviceRenderTypeMap<SectionRenderingObject> renderType )
    {
        this.renderType = renderType;
    }

    public static ProgramStageSection copyOf( ProgramStageSection original, ProgramStage stage,
        Map<String, ProgramIndicator.ProgramIndicatorTuple> indicatorMappings, Map<String, String> copyOptions )
    {
        ProgramStageSection copy = new ProgramStageSection();
        copy.setProgramStage( stage );
        copy.setAutoFields();
        setShallowCopyValues( copy, original );
        setDeepCopyValues( copy, original, indicatorMappings, copyOptions );
        return copy;
    }

    private static void setDeepCopyValues( ProgramStageSection copy, ProgramStageSection original,
        Map<String, ProgramIndicator.ProgramIndicatorTuple> indicatorMappings, Map<String, String> copyOptions )
    {
        copyIndicators( copy, original, indicatorMappings, copyOptions );
    }

    /**
     * This method copies the List of {@link ProgramIndicator} in the
     * {@link ProgramStageSection}. It takes in a mapping of
     * {@link ProgramIndicator} as a param, so it can use an existing mapping if
     * found. If no mapping is found then a new {@link ProgramIndicator} is
     * created.
     *
     * @param copy The {@link ProgramStageSection} copy which will have its List
     *        of {@link ProgramIndicator} set.
     * @param original {@link ProgramStageSection} to copy
     * @param indicatorMappings Mapping of {@link ProgramIndicator} with the
     *        original {@link ProgramIndicator} UID as key and the
     *        {@link org.hisp.dhis.program.ProgramIndicator.ProgramIndicatorTuple}
     *        as the value.
     * @param copyOptions Map of copy options to apply to a
     *        {@link ProgramIndicator}
     */
    private static void copyIndicators( ProgramStageSection copy, ProgramStageSection original,
        Map<String, ProgramIndicator.ProgramIndicatorTuple> indicatorMappings, Map<String, String> copyOptions )
    {
        List<ProgramIndicator> copyIndicators = new ArrayList<>();
        List<ProgramIndicator> originalIndicators = original.getProgramIndicators();
        if ( Objects.nonNull( originalIndicators ) )
        {
            for ( ProgramIndicator pi : originalIndicators )
            {
                ProgramIndicator indicatorCopy;
                if ( indicatorMappings.containsKey( pi.getUid() ) )
                {
                    indicatorCopy = indicatorMappings.get( pi.getUid() ).copy();
                }
                else
                {
                    indicatorCopy = ProgramIndicator.copyOf( pi, getParentProgram( copy ), copyOptions );
                }
                copyIndicators.add( indicatorCopy );
            }
        }
        copy.setProgramIndicators( copyIndicators );
    }

    private static Program getParentProgram( ProgramStageSection copy )
    {
        return copy.getProgramStage().getProgram();
    }

    private static void setShallowCopyValues( ProgramStageSection copy, ProgramStageSection original )
    {
        copy.setDataElements( ObjectUtils.copyOf( original.getDataElements() ) );
        copy.setDescription( original.getDescription() );
        copy.setFormName( original.getFormName() );
        copy.setLastUpdatedBy( original.getLastUpdatedBy() );
        copy.setName( original.getName() );

        copy.setPublicAccess( original.getPublicAccess() );
        copy.setRenderType( original.getRenderType() );
        copy.setSharing( original.getSharing() );
        copy.setShortName( original.getShortName() );
        copy.setSortOrder( original.getSortOrder() );
        copy.setStyle( original.getStyle() );
    }
}
