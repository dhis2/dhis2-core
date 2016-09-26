package org.hisp.dhis.dxf2.datavalue;

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

import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "dataValue", namespace = DxfNamespaces.DXF_2_0 )
public class DataValue
{
    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    protected String dataElement;

    protected String period;

    protected String orgUnit;

    protected String categoryOptionCombo;

    protected String attributeOptionCombo;

    protected String value;

    protected String storedBy;

    protected String created;

    protected String lastUpdated;

    protected String comment;

    protected Boolean followup;
    
    protected Boolean deleted;

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public DataValue()
    {
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    public boolean hasLastUpdated()
    {
        String updated = getLastUpdated();
        return updated != null && !updated.isEmpty();
    }

    public boolean hasCreated()
    {
        String creat = getCreated();
        return creat != null && !creat.isEmpty();
    }
    
    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "dataElement", dataElement )
            .add( "period", period )
            .add( "orgUnit", orgUnit )
            .add( "categoryOptionCombo", categoryOptionCombo )
            .add( "attributeOptionCombo", attributeOptionCombo )
            .add( "value", value )
            .add( "comment", comment )
            .add( "deleted", deleted )
            .toString();
    }

    //--------------------------------------------------------------------------
    // Getters and setters
    //--------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( String dataElement )
    {
        this.dataElement = dataElement;
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
    public String getOrgUnit()
    {
        return orgUnit;
    }

    public void setOrgUnit( String orgUnit )
    {
        this.orgUnit = orgUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getCategoryOptionCombo()
    {
        return categoryOptionCombo;
    }

    public void setCategoryOptionCombo( String categoryOptionCombo )
    {
        this.categoryOptionCombo = categoryOptionCombo;
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
    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
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

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getCreated()
    {
        return created;
    }

    public void setCreated( String created )
    {
        this.created = created;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( String lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Boolean getFollowup()
    {
        return followup;
    }

    public void setFollowup( Boolean followup )
    {
        this.followup = followup;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Boolean getDeleted()
    {
        return deleted;
    }

    public void setDeleted( Boolean deleted )
    {
        this.deleted = deleted;
    }

    public void close()
    {
    }
}
