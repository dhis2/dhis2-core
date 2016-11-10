package org.hisp.dhis.trackedentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.schema.annotation.PropertyRange;

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

    private Boolean generated = false;

    private String pattern;

    // For Local ID type

    private Boolean orgunitScope = false;

    private Boolean programScope = false;

    private TrackedEntityAttributeSearchScope searchScope;

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
    public boolean isConfidentialBool()
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

    //TODO dimension, not item

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.PROGRAM_ATTRIBUTE;
    }

    // -------------------------------------------------------------------------
    // Helper getters
    // -------------------------------------------------------------------------

    @JsonProperty
    public boolean isOptionSetValue()
    {
        return optionSet != null;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean isGenerated()
    {
        return generated != null ? generated : false;
    }

    public void setGenerated( Boolean generated )
    {
        this.generated = generated;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getPattern()
    {
        return pattern != null ? pattern : "";
    }

    public void setPattern( String pattern )
    {
        this.pattern = pattern;
    }

    @JsonProperty
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Boolean getConfidential()
    {
        return confidential;
    }

    public void setConfidential( Boolean confidential )
    {
        this.confidential = confidential;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TrackedEntityAttributeSearchScope getSearchScope()
    {
        return searchScope;
    }

    public void setSearchScope( TrackedEntityAttributeSearchScope searchScope )
    {
        this.searchScope = searchScope;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            TrackedEntityAttribute trackedEntityAttribute = (TrackedEntityAttribute) other;

            if ( mergeMode.isReplace() )
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
                generated = trackedEntityAttribute.isGenerated();
                pattern = trackedEntityAttribute.getPattern();
                orgunitScope = trackedEntityAttribute.getOrgunitScope();
                programScope = trackedEntityAttribute.getProgramScope();
                searchScope = trackedEntityAttribute.getSearchScope();
                optionSet = trackedEntityAttribute.getOptionSet();
            }
            else if ( mergeMode.isMerge() )
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
                generated = trackedEntityAttribute.isGenerated() == null ? generated : trackedEntityAttribute.isGenerated();
                pattern = trackedEntityAttribute.getPattern() == null ? pattern : trackedEntityAttribute.getPattern();
                orgunitScope = trackedEntityAttribute.getOrgunitScope() == null ? orgunitScope : trackedEntityAttribute.getOrgunitScope();
                programScope = trackedEntityAttribute.getProgramScope() == null ? programScope : trackedEntityAttribute.getProgramScope();
                searchScope = trackedEntityAttribute.getSearchScope() == null ? searchScope : trackedEntityAttribute.getSearchScope();
                optionSet = trackedEntityAttribute.getOptionSet() == null ? optionSet : trackedEntityAttribute.getOptionSet();
            }
        }
    }
}
