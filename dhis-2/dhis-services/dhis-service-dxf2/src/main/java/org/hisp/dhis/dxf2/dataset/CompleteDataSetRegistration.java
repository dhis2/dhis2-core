package org.hisp.dhis.dxf2.dataset;

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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Halvdan Hoem Grelland
 */
@JacksonXmlRootElement( localName = "completeDataSetRegistration", namespace = DxfNamespaces.DXF_2_0 )
public class CompleteDataSetRegistration
{
    //--------------------------------------------------------------------------
    // Field names
    //--------------------------------------------------------------------------

    protected static final String FIELD_COMPLETE_DATA_SET_REGISTRATION = "completeDataSetRegistration";

    protected static final String FIELD_DATASET = "dataSet";

    protected static final String FIELD_PERIOD = "period";

    protected static final String FIELD_ORGUNIT = "organisationUnit";

    protected static final String FIELD_ATTR_OPTION_COMBO = "attributeOptionCombo";

    protected static final String FIELD_DATE = "date";

    protected static final String FIELD_STORED_BY = "storedBy";

    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    protected String dataSet;

    protected String period;

    protected String organisationUnit; // 'source'

    protected String attributeOptionCombo;

    protected String date;

    protected String storedBy;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public CompleteDataSetRegistration()
    {
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    public boolean hasDate()
    {
        return StringUtils.isNotBlank( getDate() );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( FIELD_DATASET, dataSet )
            .add( FIELD_PERIOD, period )
            .add( FIELD_ORGUNIT, organisationUnit )
            .add( FIELD_ATTR_OPTION_COMBO, attributeOptionCombo )
            .add( FIELD_DATE, date )
            .add( FIELD_STORED_BY, storedBy )
            .toString();
    }

    //--------------------------------------------------------------------------
    // Streaming logic stubs
    //--------------------------------------------------------------------------

    protected void open()
    {
    }

    protected void close()
    {
    }

    protected void writeField( String fieldName, String value )
    {
    }

    //--------------------------------------------------------------------------
    // Getters and setters
    //--------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( String dataSet )
    {
        this.dataSet = dataSet;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getPeriod()
    {
        return period;
    }

    public void setPeriod( String period )
    {
        this.period = period;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getOrganisationUnit()
    {
        return organisationUnit;
    }

    public void setOrganisationUnit( String organisationUnit )
    {
        this.organisationUnit = organisationUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getAttributeOptionCombo()
    {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo( String attributeOptionCombo )
    {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDate()
    {
        return date;
    }

    public void setDate( String date )
    {
        this.date = date;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }
}
