package org.hisp.dhis.attribute;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.document.Document;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "attribute", namespace = DxfNamespaces.DXF_2_0 )
public class Attribute
    extends BaseIdentifiableObject
{
    private ValueType valueType;

    private boolean dataElementAttribute;

    private boolean dataElementGroupAttribute;

    private boolean indicatorAttribute;

    private boolean indicatorGroupAttribute;

    private boolean dataSetAttribute;

    private boolean organisationUnitAttribute;

    private boolean organisationUnitGroupAttribute;

    private boolean organisationUnitGroupSetAttribute;

    private boolean userAttribute;

    private boolean userGroupAttribute;

    private boolean programAttribute;

    private boolean programStageAttribute;

    private boolean trackedEntityAttribute;

    private boolean trackedEntityAttributeAttribute;

    private boolean categoryOptionAttribute;

    private boolean categoryOptionGroupAttribute;

    private boolean documentAttribute;

    private boolean optionAttribute;

    private boolean optionSetAttribute;

    private boolean constantAttribute;

    private boolean legendSetAttribute;

    private boolean programIndicatorAttribute;

    private boolean sqlViewAttribute;

    private boolean sectionAttribute;

    private boolean mandatory;

    private boolean unique;

    private Integer sortOrder;

    private OptionSet optionSet;

    public Attribute()
    {

    }

    public Attribute( String name, ValueType valueType )
    {
        this.name = name;
        this.valueType = valueType;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash( valueType, dataElementAttribute, dataElementGroupAttribute, indicatorAttribute, indicatorGroupAttribute,
            dataSetAttribute, organisationUnitAttribute, organisationUnitGroupAttribute, organisationUnitGroupSetAttribute, userAttribute, userGroupAttribute,
            programAttribute, programStageAttribute, trackedEntityAttribute, trackedEntityAttributeAttribute, categoryOptionAttribute, categoryOptionGroupAttribute,
            mandatory, unique, optionSet, optionAttribute, constantAttribute, legendSetAttribute, programIndicatorAttribute, sqlViewAttribute, sectionAttribute );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }

        final Attribute other = (Attribute) obj;

        return Objects.equals( this.valueType, other.valueType )
            && Objects.equals( this.dataElementAttribute, other.dataElementAttribute )
            && Objects.equals( this.dataElementGroupAttribute, other.dataElementGroupAttribute )
            && Objects.equals( this.indicatorAttribute, other.indicatorAttribute )
            && Objects.equals( this.indicatorGroupAttribute, other.indicatorGroupAttribute )
            && Objects.equals( this.dataSetAttribute, other.dataSetAttribute )
            && Objects.equals( this.organisationUnitAttribute, other.organisationUnitAttribute )
            && Objects.equals( this.organisationUnitGroupAttribute, other.organisationUnitGroupAttribute )
            && Objects.equals( this.organisationUnitGroupSetAttribute, other.organisationUnitGroupSetAttribute )
            && Objects.equals( this.userAttribute, other.userAttribute )
            && Objects.equals( this.userGroupAttribute, other.userGroupAttribute )
            && Objects.equals( this.programAttribute, other.programAttribute )
            && Objects.equals( this.programStageAttribute, other.programStageAttribute )
            && Objects.equals( this.trackedEntityAttribute, other.trackedEntityAttribute )
            && Objects.equals( this.trackedEntityAttributeAttribute, other.trackedEntityAttributeAttribute )
            && Objects.equals( this.categoryOptionAttribute, other.categoryOptionAttribute )
            && Objects.equals( this.categoryOptionGroupAttribute, other.categoryOptionGroupAttribute )
            && Objects.equals( this.optionAttribute, other.optionAttribute )
            && Objects.equals( this.constantAttribute, other.constantAttribute )
            && Objects.equals( this.legendSetAttribute, other.legendSetAttribute )
            && Objects.equals( this.programIndicatorAttribute, other.programIndicatorAttribute )
            && Objects.equals( this.sqlViewAttribute, other.sqlViewAttribute )
            && Objects.equals( this.sectionAttribute, other.sectionAttribute )

            && Objects.equals( this.mandatory, other.mandatory )
            && Objects.equals( this.unique, other.unique )
            && Objects.equals( this.optionSet, other.optionSet );
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

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isMandatory()
    {
        return mandatory;
    }

    public void setMandatory( boolean mandatory )
    {
        this.mandatory = mandatory;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUnique()
    {
        return unique;
    }

    public void setUnique( boolean unique )
    {
        this.unique = unique;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDataElementAttribute()
    {
        return dataElementAttribute;
    }

    public void setDataElementAttribute( boolean dataElementAttribute )
    {
        this.dataElementAttribute = dataElementAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDataElementGroupAttribute()
    {
        return dataElementGroupAttribute;
    }

    public void setDataElementGroupAttribute( Boolean dataElementGroupAttribute )
    {
        this.dataElementGroupAttribute = dataElementGroupAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isIndicatorAttribute()
    {
        return indicatorAttribute;
    }

    public void setIndicatorAttribute( boolean indicatorAttribute )
    {
        this.indicatorAttribute = indicatorAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isIndicatorGroupAttribute()
    {
        return indicatorGroupAttribute;
    }

    public void setIndicatorGroupAttribute( Boolean indicatorGroupAttribute )
    {
        this.indicatorGroupAttribute = indicatorGroupAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDataSetAttribute()
    {
        return dataSetAttribute;
    }

    public void setDataSetAttribute( Boolean dataSetAttribute )
    {
        this.dataSetAttribute = dataSetAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOrganisationUnitAttribute()
    {
        return organisationUnitAttribute;
    }

    public void setOrganisationUnitAttribute( boolean organisationUnitAttribute )
    {
        this.organisationUnitAttribute = organisationUnitAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOrganisationUnitGroupAttribute()
    {
        return organisationUnitGroupAttribute;
    }

    public void setOrganisationUnitGroupAttribute( Boolean organisationUnitGroupAttribute )
    {
        this.organisationUnitGroupAttribute = organisationUnitGroupAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOrganisationUnitGroupSetAttribute()
    {
        return organisationUnitGroupSetAttribute;
    }

    public void setOrganisationUnitGroupSetAttribute( Boolean organisationUnitGroupSetAttribute )
    {
        this.organisationUnitGroupSetAttribute = organisationUnitGroupSetAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUserAttribute()
    {
        return userAttribute;
    }

    public void setUserAttribute( boolean userAttribute )
    {
        this.userAttribute = userAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isUserGroupAttribute()
    {
        return userGroupAttribute;
    }

    public void setUserGroupAttribute( Boolean userGroupAttribute )
    {
        this.userGroupAttribute = userGroupAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isProgramAttribute()
    {
        return programAttribute;
    }

    public void setProgramAttribute( boolean programAttribute )
    {
        this.programAttribute = programAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isProgramStageAttribute()
    {
        return programStageAttribute;
    }

    public void setProgramStageAttribute( boolean programStageAttribute )
    {
        this.programStageAttribute = programStageAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isTrackedEntityAttribute()
    {
        return trackedEntityAttribute;
    }

    public void setTrackedEntityAttribute( boolean trackedEntityAttribute )
    {
        this.trackedEntityAttribute = trackedEntityAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isTrackedEntityAttributeAttribute()
    {
        return trackedEntityAttributeAttribute;
    }

    public void setTrackedEntityAttributeAttribute( boolean trackedEntityAttributeAttribute )
    {
        this.trackedEntityAttributeAttribute = trackedEntityAttributeAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCategoryOptionAttribute()
    {
        return categoryOptionAttribute;
    }

    public void setCategoryOptionAttribute( boolean categoryOptionAttribute )
    {
        this.categoryOptionAttribute = categoryOptionAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isCategoryOptionGroupAttribute()
    {
        return categoryOptionGroupAttribute;
    }

    public void setCategoryOptionGroupAttribute( boolean categoryOptionGroupAttribute )
    {
        this.categoryOptionGroupAttribute = categoryOptionGroupAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isDocumentAttribute()
    {
        return documentAttribute;
    }

    public void setDocumentAttribute( boolean documentAttribute )
    {
        this.documentAttribute = documentAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOptionAttribute()
    {
        return optionAttribute;
    }

    public void setOptionAttribute( boolean optionAttribute )
    {
        this.optionAttribute = optionAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isOptionSetAttribute()
    {
        return optionSetAttribute;
    }

    public void setOptionSetAttribute( boolean optionSetAttribute )
    {
        this.optionSetAttribute = optionSetAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isLegendSetAttribute()
    {
        return legendSetAttribute;
    }

    public void setLegendSetAttribute( boolean legendSetAttribute )
    {
        this.legendSetAttribute = legendSetAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isConstantAttribute()
    {
        return constantAttribute;
    }

    public void setConstantAttribute( boolean constantAttribute )
    {
        this.constantAttribute = constantAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isProgramIndicatorAttribute()
    {
        return programIndicatorAttribute;
    }

    public void setProgramIndicatorAttribute( boolean programIndicatorAttribute )
    {
        this.programIndicatorAttribute = programIndicatorAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSqlViewAttribute()
    {
        return sqlViewAttribute;
    }

    public void setSqlViewAttribute( boolean sqlViewAttribute )
    {
        this.sqlViewAttribute = sqlViewAttribute;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isSectionAttribute()
    {
        return sectionAttribute;
    }

    public void setSectionAttribute( boolean sectionAttribute )
    {
        this.sectionAttribute = sectionAttribute;
    }

    @JsonProperty
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder( Integer sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    public List<Class<? extends IdentifiableObject>> getSupportedClasses()
    {
        List<Class<? extends IdentifiableObject>> klasses = new ArrayList<>();

        if ( dataElementAttribute ) klasses.add( DataElement.class );
        if ( dataElementGroupAttribute ) klasses.add( DataElementGroup.class );
        if ( categoryOptionAttribute ) klasses.add( DataElementCategoryOption.class );
        if ( categoryOptionGroupAttribute ) klasses.add( CategoryOptionGroup.class );
        if ( indicatorAttribute ) klasses.add( Indicator.class );
        if ( indicatorGroupAttribute ) klasses.add( IndicatorGroup.class );
        if ( dataSetAttribute ) klasses.add( DataSet.class );
        if ( organisationUnitAttribute ) klasses.add( OrganisationUnit.class );
        if ( organisationUnitGroupAttribute ) klasses.add( OrganisationUnitGroup.class );
        if ( organisationUnitGroupSetAttribute ) klasses.add( OrganisationUnitGroupSet.class );
        if ( userAttribute ) klasses.add( User.class );
        if ( userGroupAttribute ) klasses.add( UserGroup.class );
        if ( programAttribute ) klasses.add( Program.class );
        if ( programStageAttribute ) klasses.add( ProgramStage.class );
        if ( trackedEntityAttribute ) klasses.add( TrackedEntity.class );
        if ( trackedEntityAttributeAttribute ) klasses.add( TrackedEntityAttribute.class );
        if ( documentAttribute ) klasses.add( Document.class );
        if ( optionAttribute ) klasses.add( Option.class );
        if ( optionSetAttribute ) klasses.add( OptionSet.class );
        if ( legendSetAttribute ) klasses.add( LegendSet.class );
        if ( constantAttribute ) klasses.add( Constant.class );
        if ( programIndicatorAttribute ) klasses.add( ProgramIndicator.class );
        if ( sqlViewAttribute ) klasses.add( SqlView.class );
        if ( sectionAttribute ) klasses.add( Section.class );

        return klasses;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            Attribute attribute = (Attribute) other;

            dataElementAttribute = attribute.isDataElementAttribute();
            dataElementGroupAttribute = attribute.isDataElementGroupAttribute();
            indicatorAttribute = attribute.isIndicatorAttribute();
            indicatorGroupAttribute = attribute.isIndicatorGroupAttribute();
            dataSetAttribute = attribute.isDataSetAttribute();
            organisationUnitAttribute = attribute.isOrganisationUnitAttribute();
            organisationUnitGroupAttribute = attribute.isOrganisationUnitGroupAttribute();
            organisationUnitGroupSetAttribute = attribute.isOrganisationUnitGroupSetAttribute();
            userAttribute = attribute.isUserAttribute();
            userGroupAttribute = attribute.isUserGroupAttribute();
            programAttribute = attribute.isProgramAttribute();
            programStageAttribute = attribute.isProgramStageAttribute();
            trackedEntityAttribute = attribute.isTrackedEntityAttribute();
            trackedEntityAttributeAttribute = attribute.isTrackedEntityAttributeAttribute();
            categoryOptionAttribute = attribute.isCategoryOptionAttribute();
            categoryOptionGroupAttribute = attribute.isCategoryOptionGroupAttribute();
            documentAttribute = attribute.isDocumentAttribute();
            optionAttribute = attribute.isOptionAttribute();
            optionSetAttribute = attribute.isOptionSetAttribute();
            constantAttribute = attribute.isConstantAttribute();
            legendSetAttribute = attribute.isLegendSetAttribute();
            programIndicatorAttribute = attribute.isProgramIndicatorAttribute();
            sqlViewAttribute = attribute.isSqlViewAttribute();
            sectionAttribute = attribute.isSectionAttribute();
            mandatory = attribute.isMandatory();
            unique = attribute.isUnique();

            if ( mergeMode.isReplace() )
            {
                valueType = attribute.getValueType();
                sortOrder = attribute.getSortOrder();
            }
            else if ( mergeMode.isMerge() )
            {
                valueType = attribute.getValueType() == null ? valueType : attribute.getValueType();
                sortOrder = attribute.getSortOrder() == null ? sortOrder : attribute.getSortOrder();
            }
        }
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "sortOrder", sortOrder )
            .add( "valueType", valueType )
            .add( "dataElementAttribute", dataElementAttribute )
            .add( "dataElementGroupAttribute", dataElementGroupAttribute )
            .add( "indicatorAttribute", indicatorAttribute )
            .add( "indicatorGroupAttribute", indicatorGroupAttribute )
            .add( "dataSetAttribute", dataSetAttribute )
            .add( "organisationUnitAttribute", organisationUnitAttribute )
            .add( "organisationUnitGroupAttribute", organisationUnitGroupAttribute )
            .add( "organisationUnitGroupSetAttribute", organisationUnitGroupSetAttribute )
            .add( "userAttribute", userAttribute )
            .add( "userGroupAttribute", userGroupAttribute )
            .add( "programAttribute", programAttribute )
            .add( "programStageAttribute", programStageAttribute )
            .add( "trackedEntityAttribute", trackedEntityAttribute )
            .add( "trackedEntityAttributeAttribute", trackedEntityAttributeAttribute )
            .add( "categoryOptionAttribute", categoryOptionAttribute )
            .add( "categoryOptionGroupAttribute", categoryOptionGroupAttribute )
            .add( "constantAttribute", constantAttribute )
            .add( "legendSetAttribute", legendSetAttribute )
            .add( "programIndicatorAttribute", programIndicatorAttribute )
            .add( "sqlViewAttribute", sqlViewAttribute )
            .add( "sectionAttribute", sectionAttribute )
            .add( "mandatory", mandatory )
            .toString();
    }
}
