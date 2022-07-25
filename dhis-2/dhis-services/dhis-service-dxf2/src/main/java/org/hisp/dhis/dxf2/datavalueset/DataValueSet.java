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
package org.hisp.dhis.dxf2.datavalueset;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.ArrayList;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.datavalue.DataValue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@Setter
@NoArgsConstructor
@JacksonXmlRootElement( localName = "dataValueSet", namespace = DxfNamespaces.DXF_2_0 )
public class DataValueSet
{
    // --------------------------------------------------------------------------
    // Options
    // --------------------------------------------------------------------------

    private String idScheme;

    private String dataElementIdScheme;

    private String orgUnitIdScheme;

    private String categoryOptionComboIdScheme;

    private String dataSetIdScheme;

    private Boolean dryRun;

    private String strategy;

    // --------------------------------------------------------------------------
    // Properties
    // --------------------------------------------------------------------------

    private String dataSet;

    private String completeDate;

    private String period;

    private String orgUnit;

    private String attributeOptionCombo;

    private List<DataValue> dataValues = new ArrayList<>();

    private List<String> attributeCategoryOptions;

    // --------------------------------------------------------------------------
    // Getters and setters
    // --------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getIdScheme()
    {
        return idScheme;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDataElementIdScheme()
    {
        return dataElementIdScheme;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getOrgUnitIdScheme()
    {
        return orgUnitIdScheme;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getCategoryOptionComboIdScheme()
    {
        return categoryOptionComboIdScheme;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDataSetIdScheme()
    {
        return dataSetIdScheme;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Boolean getDryRun()
    {
        return dryRun;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getStrategy()
    {
        return strategy;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDataSet()
    {
        return dataSet;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getCompleteDate()
    {
        return completeDate;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getPeriod()
    {
        return period;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getOrgUnit()
    {
        return orgUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    @JsonProperty( value = "dataValues" )
    @JacksonXmlElementWrapper( localName = "dataValues", useWrapping = false, namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "dataValue", namespace = DxfNamespaces.DXF_2_0 )
    public List<DataValue> getDataValues()
    {
        return dataValues;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getAttributeCategoryOptions()
    {
        return attributeCategoryOptions;
    }

    /**
     * Returns the general identifier scheme. IdScheme.NULL is returned if
     * scheme has not been set.
     */
    public IdScheme getIdSchemeProperty()
    {
        String scheme = getIdScheme();
        return IdScheme.from( scheme );
    }

    /**
     * Returns the data element identifier scheme. Falls back to the general
     * identifier scheme if not set. IdScheme.NULL is returned if no scheme has
     * been set.
     */
    public IdScheme getDataElementIdSchemeProperty()
    {
        return getIdScheme( getDataElementIdScheme() );
    }

    /**
     * Returns the organisation unit identifier scheme. Falls back to the
     * general identifier scheme if not set. IdScheme.NULL is returned if no
     * scheme has been set.
     */
    public IdScheme getOrgUnitIdSchemeProperty()
    {
        return getIdScheme( getOrgUnitIdScheme() );
    }

    /**
     * Returns the category option combo identifier scheme. Falls back to the
     * general identifier scheme if not set. IdScheme.NULL is returned if no
     * scheme has been set.
     */
    public IdScheme getCategoryOptionComboIdSchemeProperty()
    {
        return getIdScheme( getCategoryOptionComboIdScheme() );
    }

    /**
     * Returns the data set identifier scheme. Falls back to the general
     * identifier scheme if not set. IdScheme.NULL is returned if no scheme has
     * been set.
     */
    public IdScheme getDataSetIdSchemeProperty()
    {
        return getIdScheme( getDataSetIdScheme() );
    }

    private IdScheme getIdScheme( String objectIdScheme )
    {
        String scheme = getIdScheme();
        scheme = defaultIfEmpty( objectIdScheme, scheme );
        return IdScheme.from( scheme );
    }

    // --------------------------------------------------------------------------
    // toString
    // --------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "[" + dataSet + ", " + completeDate + ", " + period + ", " + orgUnit + ", " + dataValues.size() + "]";
    }
}
