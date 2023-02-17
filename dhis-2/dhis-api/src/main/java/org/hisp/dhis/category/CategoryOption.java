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
package org.hisp.dhis.category;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.ObjectStyle;
import org.hisp.dhis.common.SystemDefaultMetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.schema.annotation.PropertyRange;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
public class CategoryOption
    extends BaseDimensionalItemObject implements SystemDefaultMetadataObject
{
    public static final String DEFAULT_NAME = "default";

    private Date startDate;

    private Date endDate;

    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    private Set<Category> categories = new HashSet<>();

    private Set<CategoryOptionCombo> categoryOptionCombos = new HashSet<>();

    private Set<CategoryOptionGroup> groups = new HashSet<>();

    private ObjectStyle style;

    /**
     * The name to appear in forms.
     */
    private String formName;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CategoryOption()
    {

    }

    public CategoryOption( String name )
    {
        this.name = name;
        this.shortName = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @JsonProperty( "isDefault" )
    @Override
    public boolean isDefault()
    {
        return DEFAULT_NAME.equals( name );
    }

    /**
     * Returns a set of category option group sets which are associated with the
     * category option groups of this category option.
     */
    public Set<CategoryOptionGroupSet> getGroupSets()
    {
        Set<CategoryOptionGroupSet> groupSets = new HashSet<>();

        if ( groups != null )
        {
            for ( CategoryOptionGroup group : groups )
            {
                groupSets.addAll( group.getGroupSets() );
            }
        }

        return groupSets;
    }

    public void addCategoryOptionCombo( CategoryOptionCombo dataElementCategoryOptionCombo )
    {
        categoryOptionCombos.add( dataElementCategoryOptionCombo );
        dataElementCategoryOptionCombo.getCategoryOptions().add( this );
    }

    public void removeCategoryOptionCombo( CategoryOptionCombo dataElementCategoryOptionCombo )
    {
        categoryOptionCombos.remove( dataElementCategoryOptionCombo );
        dataElementCategoryOptionCombo.getCategoryOptions().remove( this );
    }

    public void addOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.add( organisationUnit );
        organisationUnit.getCategoryOptions().add( this );
    }

    public void addOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        organisationUnits.forEach( this::addOrganisationUnit );
    }

    public void removeOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.remove( organisationUnit );
        organisationUnit.getCategoryOptions().remove( this );
    }

    public void removeOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        organisationUnits.forEach( this::removeOrganisationUnit );
    }

    /**
     * Gets an adjusted end date, adjusted if this data set has open periods
     * after the end date.
     *
     * @param dataSet the data set to adjust for
     * @return the adjusted end date
     */
    public Date getAdjustedEndDate( DataSet dataSet )
    {
        if ( endDate == null || dataSet.getOpenPeriodsAfterCoEndDate() == 0 )
        {
            return endDate;
        }

        return dataSet.getPeriodType().getRewindedDate( endDate, -dataSet.getOpenPeriodsAfterCoEndDate() );
    }

    /**
     * Gets an adjusted end date, adjusted if a data element belongs to any data
     * sets that have open periods after the end date. If so, it chooses the
     * latest end date.
     *
     * @param dataElement the data element to adjust for
     * @return the adjusted end date
     */
    public Date getAdjustedEndDate( DataElement dataElement )
    {
        if ( endDate == null )
        {
            return null;
        }

        Date latestAdjustedDate = endDate;

        for ( DataSetElement element : dataElement.getDataSetElements() )
        {
            Date adjustedDate = getAdjustedEndDate( element.getDataSet() );

            if ( adjustedDate.after( latestAdjustedDate ) )
            {
                latestAdjustedDate = adjustedDate;
            }
        }

        return latestAdjustedDate;
    }

    /**
     * Gets an adjusted end date, adjusted if this program has open days after
     * the end date.
     *
     * @param program the program to adjust for
     * @return the adjusted end date
     */
    public Date getAdjustedEndDate( Program program )
    {
        if ( endDate == null || program.getOpenDaysAfterCoEndDate() == 0 )
        {
            return endDate;
        }

        return (new DailyPeriodType()).getRewindedDate( endDate, -program.getOpenDaysAfterCoEndDate() );
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.CATEGORY_OPTION;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Set<OrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( Set<OrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "categories", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "category", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Category> getCategories()
    {
        return categories;
    }

    public void setCategories( Set<Category> categories )
    {
        this.categories = categories;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "categoryOptionCombos", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0 )
    public Set<CategoryOptionCombo> getCategoryOptionCombos()
    {
        return categoryOptionCombos;
    }

    public void setCategoryOptionCombos( Set<CategoryOptionCombo> categoryOptionCombos )
    {
        this.categoryOptionCombos = categoryOptionCombos;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "categoryOptionGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionGroup", namespace = DxfNamespaces.DXF_2_0 )
    public Set<CategoryOptionGroup> getGroups()
    {
        return groups;
    }

    public void setGroups( Set<CategoryOptionGroup> groups )
    {
        this.groups = groups;
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

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getFormName()
    {
        return formName;
    }

    @Override
    public void setFormName( String formName )
    {
        this.formName = formName;
    }

    /**
     * Returns the form name, or the name if it does not exist.
     */
    @Override
    public String getFormNameFallback()
    {
        return formName != null && !formName.isEmpty() ? getFormName() : getDisplayName();
    }
}
