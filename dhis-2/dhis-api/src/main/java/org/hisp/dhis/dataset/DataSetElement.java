package org.hisp.dhis.dataset;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;

import java.util.Objects;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "dataSetElement", namespace = DxfNamespaces.DXF_2_0 )
public class DataSetElement implements EmbeddedObject
{
    /**
     * The database internal identifier for this Object.
     */
    private int id;

    /**
     * Data set, never null.
     */
    private DataSet dataSet;

    /**
     * Data element, never null.
     */
    private DataElement dataElement;

    /**
     * Category combination, potentially null.
     */
    private DataElementCategoryCombo categoryCombo;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DataSetElement()
    {

    }

    public DataSetElement( DataSet dataSet, DataElement dataElement )
    {
        this.dataSet = dataSet;
        this.dataElement = dataElement;
    }

    public DataSetElement( DataSet dataSet, DataElement dataElement, DataElementCategoryCombo categoryCombo )
    {
        this.dataSet = dataSet;
        this.dataElement = dataElement;
        this.categoryCombo = categoryCombo;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Returns the category combination of this data set element, if null,
     * then returns the category combination of the data element of this data
     * set element.
     */
    public DataElementCategoryCombo getResolvedCategoryCombo()
    {
        return hasCategoryCombo() ? getCategoryCombo() : dataElement.getDataElementCategoryCombo();
    }

    public boolean hasCategoryCombo()
    {
        return categoryCombo != null;
    }

    // -------------------------------------------------------------------------
    // Hash code and equals
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        return Objects.hash( super.hashCode(), dataSet, dataElement );
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( object.getClass() ) )
        {
            return false;
        }

        DataSetElement other = (DataSetElement) object;

        return objectEquals( other );
    }

    public boolean objectEquals( DataSetElement other )
    {
        return dataSet.equals( other.getDataSet() ) && dataElement.equals( other.getDataElement() );
    }

    @Override
    public String toString()
    {
        return "{" +
            "\"class\":\"" + getClass() + "\", " +
            "\"dataSet\":\"" + dataSet + "\", " +
            "\"dataElement\":\"" + dataElement + "\" " +
            "\"categoryCombo\":\"" + categoryCombo + "\" " +
            "}";
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    @JsonIgnore
    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
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
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    /**
     * Category combination of this data set element. Can be null, use
     * {@link #getResolvedCategoryCombo} to get fall back to category
     * combination of data element.
     */
    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementCategoryCombo getCategoryCombo()
    {
        return categoryCombo;
    }

    public void setCategoryCombo( DataElementCategoryCombo categoryCombo )
    {
        this.categoryCombo = categoryCombo;
    }
}
