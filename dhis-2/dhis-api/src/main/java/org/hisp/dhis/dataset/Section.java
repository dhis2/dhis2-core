package org.hisp.dhis.dataset;

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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JacksonXmlRootElement( localName = "section", namespace = DxfNamespaces.DXF_2_0 )
public class Section
    extends BaseIdentifiableObject
{
    private String description;

    private DataSet dataSet;

    private List<DataElement> dataElements = new ArrayList<>();

    private List<Indicator> indicators = new ArrayList<>();

    private Set<DataElementOperand> greyedFields = new HashSet<>();

    private int sortOrder;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Section()
    {
    }

    public Section( String name, DataSet dataSet, List<DataElement> dataElements, Set<DataElementOperand> greyedFields )
    {
        this.name = name;
        this.dataSet = dataSet;
        this.dataElements = dataElements;
        this.greyedFields = greyedFields;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addDataElement( DataElement dataElement )
    {
        dataElements.add( dataElement );
    }

    public void removeDataElement( DataElement dataElement )
    {
        dataElements.remove( dataElement );
    }

    public void addGreyedField( DataElementOperand greyedField )
    {
        greyedFields.add( greyedField );
    }

    public void removeGreyedField( DataElementOperand greyedField )
    {
        greyedFields.remove( greyedField );
    }

    private void addIndicator( Indicator indicator )
    {
        indicators.remove( indicator );
    }

    public void removeAllGreyedFields()
    {
        greyedFields.clear();
    }

    public void removeAllDataElements()
    {
        dataElements.clear();
    }

    public void removeAllIndicators()
    {
        indicators.clear();
    }

    public boolean hasCategoryCombo()
    {
        return getCategoryCombo() != null;
    }

    public DataElementCategoryCombo getCategoryCombo()
    {
        return dataElements != null && !dataElements.isEmpty() ? dataElements.get( 0 ).getCategoryCombo() : null;
    }

    public boolean hasMultiDimensionalDataElement()
    {
        for ( DataElement element : dataElements )
        {
            if ( element.isMultiDimensional() )
            {
                return true;
            }
        }

        return false;
    }

    public boolean categorComboIsInvalid()
    {
        if ( dataElements != null && dataElements.size() > 0 )
        {
            DataElementCategoryCombo categoryCombo = null;

            for ( DataElement element : dataElements )
            {
                if ( element != null )
                {
                    if ( categoryCombo != null && !categoryCombo.equals( element.getCategoryCombo() ) )
                    {
                        return true;
                    }

                    categoryCombo = element.getCategoryCombo();
                }
            }
        }

        return false;
    }

    public boolean hasDataElements()
    {
        return dataElements != null && !dataElements.isEmpty();
    }

    @Override
    public boolean haveUniqueNames()
    {
        return false;
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
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
    public DataSet getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( DataSet dataSet )
    {
        this.dataSet = dataSet;
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
    @JacksonXmlElementWrapper( localName = "indicators", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "indicator", namespace = DxfNamespaces.DXF_2_0 )
    public List<Indicator> getIndicators()
    {
        return indicators;
    }

    public void setIndicators( List<Indicator> indicators )
    {
        this.indicators = indicators;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder( int sortOrder )
    {
        this.sortOrder = sortOrder;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "greyedFields", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "greyedField", namespace = DxfNamespaces.DXF_2_0 )
    public Set<DataElementOperand> getGreyedFields()
    {
        return greyedFields;
    }

    public void setGreyedFields( Set<DataElementOperand> greyedFields )
    {
        this.greyedFields = greyedFields;
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            Section section = (Section) other;
            sortOrder = section.getSortOrder();

            if ( mergeMode.isReplace() )
            {
                dataSet = section.getDataSet();
                description = section.getDescription();
            }
            else if ( mergeMode.isMerge() )
            {
                dataSet = section.getDataSet() == null ? dataSet : section.getDataSet();
                description = section.getDescription() == null ? description : section.getDescription();
            }

            removeAllDataElements();
            section.getDataElements().forEach( this::addDataElement );

            removeAllGreyedFields();
            section.getGreyedFields().forEach( this::addGreyedField );

            removeAllIndicators();
            section.getIndicators().forEach( this::addIndicator );
        }
    }
}
