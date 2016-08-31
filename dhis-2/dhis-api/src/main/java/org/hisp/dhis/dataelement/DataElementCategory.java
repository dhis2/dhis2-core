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

import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.annotation.Scanned;
import org.hisp.dhis.common.view.DetailedView;
import org.hisp.dhis.common.view.DimensionalView;
import org.hisp.dhis.common.view.ExportView;

import java.util.ArrayList;
import java.util.List;

/**
 * A Category is a dimension of a data element. DataElements can have sets of
 * dimensions (known as CategoryCombos). An Example of a Category might be
 * "Sex". The Category could have two (or more) CategoryOptions such as "Male"
 * and "Female".
 *
 * @author Abyot Asalefew
 */
@JacksonXmlRootElement( localName = "category", namespace = DxfNamespaces.DXF_2_0 )
public class DataElementCategory
    extends BaseDimensionalObject
{
    public static final String DEFAULT_NAME = "default";

    private DataDimensionType dataDimensionType;

    @Scanned
    private List<DataElementCategoryOption> categoryOptions = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataElementCategory()
    {

    }

    public DataElementCategory( String name )
    {
        this.name = name;
    }

    public DataElementCategory( String name, List<DataElementCategoryOption> categoryOptions )
    {
        this( name );
        this.categoryOptions = categoryOptions;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptions.add( dataElementCategoryOption );
        dataElementCategoryOption.getCategories().add( this );
    }

    public void removeDataElementCategoryOption( DataElementCategoryOption dataElementCategoryOption )
    {
        categoryOptions.remove( dataElementCategoryOption );
        dataElementCategoryOption.getCategories().remove( this );
    }

    public void removeAllCategoryOptions()
    {
        for ( DataElementCategoryOption categoryOption : categoryOptions )
        {
            categoryOption.getCategories().remove( this );
        }

        categoryOptions.clear();
    }

    public DataElementCategoryOption getCategoryOption( DataElementCategoryOptionCombo categoryOptionCombo )
    {
        for ( DataElementCategoryOption categoryOption : categoryOptions )
        {
            if ( categoryOption.getCategoryOptionCombos().contains( categoryOptionCombo ) )
            {
                return categoryOption;
            }
        }

        return null;
    }

    public boolean isDefault()
    {
        return DEFAULT_NAME.equals( name );
    }

    // -------------------------------------------------------------------------
    // Dimensional object
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, DimensionalView.class } )
    @JacksonXmlElementWrapper( localName = "items", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "item", namespace = DxfNamespaces.DXF_2_0 )
    public List<NameableObject> getItems()
    {
        return new ArrayList<>( categoryOptions );
    }

    @Override
    public DimensionType getDimensionType()
    {
        return DimensionType.CATEGORY;
    }

    // ------------------------------------------------------------------------
    // Logic
    // ------------------------------------------------------------------------

    @Override
    public boolean isAutoGenerated()
    {
        return name != null && name.equals( DEFAULT_NAME );
    }

    // ------------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------------

    @Override
    public String getShortName()
    {
        if ( getName() == null || getName().length() <= 50 )
        {
            return getName();
        }
        else
        {
            return getName().substring( 0, 49 );
        }
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

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JsonView( { DetailedView.class, ExportView.class } )
    @JacksonXmlElementWrapper( localName = "categoryOptions", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "categoryOption", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataElementCategoryOption> getCategoryOptions()
    {
        return categoryOptions;
    }

    public void setCategoryOptions( List<DataElementCategoryOption> categoryOptions )
    {
        this.categoryOptions = categoryOptions;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            DataElementCategory category = (DataElementCategory) other;

            if ( strategy.isReplace() )
            {
                dataDimensionType = category.getDataDimensionType();
            }
            else if ( strategy.isMerge() )
            {
                dataDimensionType = category.getDataDimensionType() == null ? dataDimensionType : category.getDataDimensionType();
            }

            removeAllCategoryOptions();

            for ( DataElementCategoryOption dataElementCategoryOption : category.getCategoryOptions() )
            {
                addDataElementCategoryOption( dataElementCategoryOption );
            }
        }
    }
}
