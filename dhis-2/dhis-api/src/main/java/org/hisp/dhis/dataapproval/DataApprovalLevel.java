package org.hisp.dhis.dataapproval;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

/**
 * Records the approval of DataSet values for a given OrganisationUnit and
 * Period.
 *
 * @author Jim Grace
 */
@JacksonXmlRootElement( localName = "dataApprovalLevel", namespace = DxfNamespaces.DXF_2_0 )
public class DataApprovalLevel
    extends BaseIdentifiableObject implements MetadataObject
{
    /**
     * The data approval level, 1=highest level, max=lowest level.
     */
    private int level;

    /**
     * The organisation unit level for this data approval level.
     */
    private int orgUnitLevel;

    /**
     * The category option group set (optional) for this data approval level.
     */
    private CategoryOptionGroupSet categoryOptionGroupSet;

    /**
     * The name of the organisation unit level (derived through the service.)
     */
    private transient String orgUnitLevelName;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataApprovalLevel()
    {
    }

    public DataApprovalLevel( String name, int orgUnitLevel, CategoryOptionGroupSet categoryOptionGroupSet )
    {
        this.name = name;
        this.orgUnitLevel = orgUnitLevel;
        this.categoryOptionGroupSet = categoryOptionGroupSet;
    }

    public DataApprovalLevel( DataApprovalLevel level )
    {
        this.name = level.name;
        this.level = level.level;
        this.orgUnitLevel = level.orgUnitLevel;
        this.categoryOptionGroupSet = level.categoryOptionGroupSet;
        this.created = level.created;
        this.lastUpdated = level.lastUpdated;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Returns the name of the category option group set for this data approval
     * level, or an empty string if there is no category option group set.
     *
     * @return name of this approval level's category option group set.
     */
    public String getCategoryOptionGroupSetName()
    {
        return categoryOptionGroupSet == null ? "" : categoryOptionGroupSet.getName();
    }

    /**
     * Indicates whether this approval level specified a category option group set.
     */
    public boolean hasCategoryOptionGroupSet()
    {
        return categoryOptionGroupSet != null;
    }

    /**
     * Indicates whether the given approval level represents the same level as this.
     */
    public boolean levelEquals( DataApprovalLevel other )
    {
        if ( other == null )
        {
            return false;
        }

        if ( level != other.getLevel() )
        {
            return false;
        }

        if ( categoryOptionGroupSet != null ?
            !categoryOptionGroupSet.equals( other.getCategoryOptionGroupSet() ) :
            other.getCategoryOptionGroupSet() != null )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "DataApprovalLevel{" +
            "name=" + name +
            ", level=" + level +
            ", orgUnitLevel=" + orgUnitLevel +
            ", categoryOptionGroupSet='" + (categoryOptionGroupSet == null ? "(null)" : categoryOptionGroupSet.getName()) + "'" +
            ", created=" + created +
            ", lastUpdated=" + lastUpdated +
            '}';
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getLevel()
    {
        return level;
    }

    public void setLevel( int level )
    {
        this.level = level;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getOrgUnitLevel()
    {
        return orgUnitLevel;
    }

    public void setOrgUnitLevel( int orgUnitLevel )
    {
        this.orgUnitLevel = orgUnitLevel;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( value = PropertyType.REFERENCE, required = Property.Value.DEFAULT, persisted = Property.Value.TRUE, owner = Property.Value.TRUE )
    public CategoryOptionGroupSet getCategoryOptionGroupSet()
    {
        return categoryOptionGroupSet;
    }

    public void setCategoryOptionGroupSet( CategoryOptionGroupSet categoryOptionGroupSet )
    {
        this.categoryOptionGroupSet = categoryOptionGroupSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getOrgUnitLevelName()
    {
        return orgUnitLevelName;
    }

    public void setOrgUnitLevelName( String orgUnitLevelName )
    {
        this.orgUnitLevelName = orgUnitLevelName;
    }
}
