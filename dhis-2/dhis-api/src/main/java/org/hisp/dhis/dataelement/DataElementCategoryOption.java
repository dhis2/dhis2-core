package org.hisp.dhis.dataelement;

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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementCategoryOption
    extends BaseDimensionalItemObject
{
    public static final String DEFAULT_NAME = "default";

    private Date startDate;

    private Date endDate;

    private Set<OrganisationUnit> organisationUnits = new HashSet<>();

    private Set<DataElementCategory> categories = new HashSet<>();

    private Set<DataElementCategoryOptionCombo> categoryOptionCombos = new HashSet<>();

    private Set<CategoryOptionGroup> groups = new HashSet<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementCategoryOption()
    {

    }

    public DataElementCategoryOption( String name )
    {
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

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
                if ( group.getGroupSet() != null )
                {
                    groupSets.add( group.getGroupSet() );
                }
            }
        }

        return groupSets;
    }

    public void addCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo )
    {
        categoryOptionCombos.add( dataElementCategoryOptionCombo );
        dataElementCategoryOptionCombo.getCategoryOptions().add( this );
    }

    public void removeCategoryOptionCombo( DataElementCategoryOptionCombo dataElementCategoryOptionCombo )
    {
        categoryOptionCombos.remove( dataElementCategoryOptionCombo );
        dataElementCategoryOptionCombo.getCategoryOptions().remove( this );
    }

    public void addOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.add( organisationUnit );
        organisationUnit.getCategoryOptions().add( this );
    }

    public void removeOrganisationUnit( OrganisationUnit organisationUnit )
    {
        organisationUnits.remove( organisationUnit );
        organisationUnit.getCategoryOptions().remove( this );
    }

    public boolean includes( Period period )
    {
        return (startDate == null || !startDate.after( period.getEndDate() ))
            && (endDate == null || !endDate.before( period.getStartDate() ));
    }

    public boolean includes( OrganisationUnit ou )
    {
        return organisationUnits == null || organisationUnits.isEmpty() || ou.isDescendant( organisationUnits );
    }

    public boolean includesAny( Set<OrganisationUnit> orgUnits )
    {
        for ( OrganisationUnit ou : orgUnits )
        {
            if ( includes( ou ) )
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
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.CATEGORY_OPTION;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Override
    public boolean isAutoGenerated()
    {
        return name != null && name.equals( DEFAULT_NAME );
    }

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
    public Set<DataElementCategory> getCategories()
    {
        return categories;
    }

    public void setCategories( Set<DataElementCategory> categories )
    {
        this.categories = categories;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "categoryOptionCombos", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOptionCombo", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementCategoryOptionCombo> getCategoryOptionCombos()
    {
        return categoryOptionCombos;
    }

    public void setCategoryOptionCombos( Set<DataElementCategoryOptionCombo> categoryOptionCombos )
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

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            DataElementCategoryOption categoryOption = (DataElementCategoryOption) other;

            if ( mergeMode.isReplace() )
            {
                startDate = categoryOption.getStartDate();
                endDate = categoryOption.getEndDate();
            }
            else if ( mergeMode.isMerge() )
            {
                startDate = categoryOption.getStartDate() == null ? startDate : categoryOption.getStartDate();
                endDate = categoryOption.getEndDate() == null ? endDate : categoryOption.getEndDate();
            }

            organisationUnits.clear();
            categories.clear();
            groups.clear();
            categoryOptionCombos.clear();

            organisationUnits.addAll( categoryOption.getOrganisationUnits() );
            categories.addAll( categoryOption.getCategories() );
            groups.addAll( categoryOption.getGroups() );
            categoryOptionCombos.addAll( categoryOption.getCategoryOptionCombos() );
        }
    }
}
