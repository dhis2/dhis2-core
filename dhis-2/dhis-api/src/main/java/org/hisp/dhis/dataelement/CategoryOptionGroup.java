package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.annotation.Scanned;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.ExportView;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "categoryOptionGroup", namespace = DxfNamespaces.DXF_2_0 )
public class CategoryOptionGroup
    extends BaseNameableObject
{
    @Scanned
    private Set<DataElementCategoryOption> members = new HashSet<>();

    private CategoryOptionGroupSet groupSet;

    private DataDimensionType dataDimensionType;

    /**
     * Set of the dynamic attributes values that belong to this data element.
     */
    private Set<AttributeValue> attributeValues = new HashSet<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public CategoryOptionGroup()
    {

    }

    public CategoryOptionGroup( String name )
    {
        this();
        this.name = name;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addCategoryOption( DataElementCategoryOption categoryOption )
    {
        members.add( categoryOption );
        categoryOption.getGroups().add( this );
    }

    public void removeCategoryOption( DataElementCategoryOption categoryOption )
    {
        members.remove( categoryOption );
        categoryOption.getGroups().remove( this );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty( "categoryOptions" )
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementCategoryOption> getMembers()
    {
        return members;
    }

    public void setMembers( Set<DataElementCategoryOption> members )
    {
        this.members = members;
    }

    @JsonProperty( "categoryOptionGroupSet" )
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class } )
    @JacksonXmlProperty( localName = "categoryOptionGroupSet", namespace = DxfNamespaces.DXF_2_0 )
    public CategoryOptionGroupSet getGroupSet()
    {
        return groupSet;
    }

    public void setGroupSet( CategoryOptionGroupSet groupSet )
    {
        this.groupSet = groupSet;
    }

    @JsonProperty
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataDimensionType getDataDimensionType()
    {
        return dataDimensionType;
    }

    public void setDataDimensionType( DataDimensionType dataDimensionType )
    {
        this.dataDimensionType = dataDimensionType;
    }

    @JsonProperty( "attributeValues" )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "attributeValues", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "attributeValue", namespace = DxfNamespaces.DXF_2_0 )
    public Set<AttributeValue> getAttributeValues()
    {
        return attributeValues;
    }

    public void setAttributeValues( Set<AttributeValue> attributeValues )
    {
        this.attributeValues = attributeValues;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            CategoryOptionGroup categoryOptionGroup = (CategoryOptionGroup) other;

            if ( strategy.isReplace() )
            {
                groupSet = categoryOptionGroup.getGroupSet();
                dataDimensionType = categoryOptionGroup.getDataDimensionType();
            }
            else if ( strategy.isMerge() )
            {
                groupSet = categoryOptionGroup.getGroupSet() == null ? groupSet : categoryOptionGroup.getGroupSet();
                dataDimensionType = categoryOptionGroup.getDataDimensionType() == null ? dataDimensionType : categoryOptionGroup.getDataDimensionType();
            }

            members.clear();

            for ( DataElementCategoryOption categoryOption : categoryOptionGroup.getMembers() )
            {
                addCategoryOption( categoryOption );
            }

            attributeValues.clear();
            attributeValues.addAll( categoryOptionGroup.getAttributeValues() );
        }
    }
}
