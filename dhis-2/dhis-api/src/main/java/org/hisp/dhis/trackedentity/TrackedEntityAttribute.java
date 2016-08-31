package org.hisp.dhis.trackedentity;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.annotation.PropertyRange;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "trackedEntityAttribute", namespace = DxfNamespaces.DXF_2_0 )
public class TrackedEntityAttribute
    extends BaseDimensionalItemObject
{
    private String description;

    private ValueType valueType;

    private Boolean inherit = false;

    private TrackedEntityAttributeGroup attributeGroup;

    private OptionSet optionSet;

    private TrackedEntity trackedEntity;

    private String expression;

    private Boolean displayOnVisitSchedule = false;

    private Integer sortOrderInVisitSchedule;

    private Boolean displayInListNoProgram = false;

    private Integer sortOrderInListNoProgram;

    private Boolean confidential = false;

    private Boolean unique = false;

    // For Local ID type

    private Boolean orgunitScope = false;

    private Boolean programScope = false;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityAttribute()
    {
    }

    public TrackedEntityAttribute( String name, String description, ValueType valueType, Boolean inherit, Boolean displayOnVisitSchedule )
    {
        this.name = name;
        this.description = description;
        this.valueType = valueType;
        this.inherit = inherit;
        this.displayOnVisitSchedule = displayOnVisitSchedule;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Indicates whether the value type of this attribute is numeric.
     */
    public boolean isNumericType()
    {
        return valueType.isNumeric();
    }

    /**
     * Indicates whether the value type of this attribute is date.
     */
    public boolean isDateType()
    {
        return valueType.isDate();
    }
    
    /**
     * Indicates whether this attribute has confidential information.
     */
    @JsonIgnore
    public boolean isConfidential()
    {
        return confidential != null && confidential;
    }

    /**
     * Indicates whether this attribute has an option set.
     */
    public boolean hasOptionSet()
    {
        return optionSet != null;
    }

    @Override
    public boolean hasLegendSet()
    {
        return legendSet != null;
    }

    /**
     * Checks whether the given value is present among the options in the option
     * set of this attribute, matching on code.
     */
    public Boolean isValidOptionValue( String value )
    {
        if ( !hasOptionSet() || value == null )
        {
            return false;
        }

        for ( Option option : getOptionSet().getOptions() )
        {
            if ( value.equals( option.getCode() ) )
            {
                return true;
            }
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public DimensionType getDimensionType()
    {
        return DimensionType.PROGRAM_ATTRIBUTE;
    }

    // -------------------------------------------------------------------------
    // Helper getters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonView( { DetailedView.class } )
    public boolean isOptionSetValue()
    {
        return optionSet != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getInherit()
    {
        return inherit;
    }

    public void setInherit( Boolean inherit )
    {
        this.inherit = inherit;
    }

    @Override
    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getDescription()
    {
        return description;
    }

    @Override
    public void setDescription( String description )
    {
        this.description = description;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty( "trackedEntityAttributeGroup" )
    @JsonView( { DetailedView.class } )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "trackedEntityAttributeGroup", namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttributeGroup getAttributeGroup()
    {
        return attributeGroup;
    }

    public void setAttributeGroup( TrackedEntityAttributeGroup attributeGroup )
    {
        this.attributeGroup = attributeGroup;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getExpression()
    {
        return expression;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayOnVisitSchedule()
    {
        return displayOnVisitSchedule;
    }

    public void setDisplayOnVisitSchedule( Boolean displayOnVisitSchedule )
    {
        this.displayOnVisitSchedule = displayOnVisitSchedule;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSortOrderInVisitSchedule()
    {
        return sortOrderInVisitSchedule;
    }

    public void setSortOrderInVisitSchedule( Integer sortOrderInVisitSchedule )
    {
        this.sortOrderInVisitSchedule = sortOrderInVisitSchedule;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getDisplayInListNoProgram()
    {
        return displayInListNoProgram;
    }

    public void setDisplayInListNoProgram( Boolean displayInListNoProgram )
    {
        this.displayInListNoProgram = displayInListNoProgram;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSortOrderInListNoProgram()
    {
        return sortOrderInListNoProgram;
    }

    public void setSortOrderInListNoProgram( Integer sortOrderInListNoProgram )
    {
        this.sortOrderInListNoProgram = sortOrderInListNoProgram;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isUnique()
    {
        return unique != null ? unique : false;
    }

    public void setUnique( Boolean unique )
    {
        this.unique = unique;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getOrgunitScope()
    {
        return orgunitScope != null ? orgunitScope : false;
    }

    public void setOrgunitScope( Boolean orgunitScope )
    {
        this.orgunitScope = orgunitScope;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getProgramScope()
    {
        return programScope != null ? programScope : false;
    }

    public void setProgramScope( Boolean programScope )
    {
        this.programScope = programScope;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OptionSet getOptionSet()
    {
        return optionSet;
    }

    public void setOptionSet( OptionSet optionSet )
    {
        this.optionSet = optionSet;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntity getTrackedEntity()
    {
        return trackedEntity;
    }

    public void setTrackedEntity( TrackedEntity trackedEntity )
    {
        this.trackedEntity = trackedEntity;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getConfidential()
    {
        return confidential;
    }

    public void setConfidential( Boolean confidential )
    {
        this.confidential = confidential;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            TrackedEntityAttribute trackedEntityAttribute = (TrackedEntityAttribute) other;

            if ( strategy.isReplace() )
            {
                description = trackedEntityAttribute.getDescription();
                valueType = trackedEntityAttribute.getValueType();
                inherit = trackedEntityAttribute.getInherit();
                attributeGroup = trackedEntityAttribute.getAttributeGroup();
                expression = trackedEntityAttribute.getExpression();
                displayOnVisitSchedule = trackedEntityAttribute.getDisplayOnVisitSchedule();
                sortOrderInVisitSchedule = trackedEntityAttribute.getSortOrderInVisitSchedule();
                displayInListNoProgram = trackedEntityAttribute.getDisplayInListNoProgram();
                sortOrderInListNoProgram = trackedEntityAttribute.getSortOrderInListNoProgram();
                unique = trackedEntityAttribute.isUnique();
                orgunitScope = trackedEntityAttribute.getOrgunitScope();
                programScope = trackedEntityAttribute.getProgramScope();
                optionSet = trackedEntityAttribute.getOptionSet();
            }
            else if ( strategy.isMerge() )
            {
                description = trackedEntityAttribute.getDescription() == null ? description : trackedEntityAttribute.getDescription();
                valueType = trackedEntityAttribute.getValueType() == null ? valueType : trackedEntityAttribute.getValueType();
                inherit = trackedEntityAttribute.getInherit() == null ? inherit : trackedEntityAttribute.getInherit();
                attributeGroup = trackedEntityAttribute.getAttributeGroup() == null ? attributeGroup : trackedEntityAttribute.getAttributeGroup();
                expression = trackedEntityAttribute.getExpression() == null ? expression : trackedEntityAttribute.getExpression();
                displayOnVisitSchedule = trackedEntityAttribute.getDisplayOnVisitSchedule() == null ? displayOnVisitSchedule : trackedEntityAttribute.getDisplayOnVisitSchedule();
                sortOrderInVisitSchedule = trackedEntityAttribute.getSortOrderInVisitSchedule() == null ? sortOrderInVisitSchedule : trackedEntityAttribute.getSortOrderInVisitSchedule();
                displayInListNoProgram = trackedEntityAttribute.getDisplayInListNoProgram() == null ? displayInListNoProgram : trackedEntityAttribute.getDisplayInListNoProgram();
                sortOrderInListNoProgram = trackedEntityAttribute.getSortOrderInListNoProgram() == null ? sortOrderInListNoProgram : trackedEntityAttribute.getSortOrderInListNoProgram();
                unique = trackedEntityAttribute.isUnique() == null ? unique : trackedEntityAttribute.isUnique();
                orgunitScope = trackedEntityAttribute.getOrgunitScope() == null ? orgunitScope : trackedEntityAttribute.getOrgunitScope();
                programScope = trackedEntityAttribute.getProgramScope() == null ? programScope : trackedEntityAttribute.getProgramScope();
                optionSet = trackedEntityAttribute.getOptionSet() == null ? optionSet : trackedEntityAttribute.getOptionSet();
            }
        }
    }
}
