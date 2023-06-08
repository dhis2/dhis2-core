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
package org.hisp.dhis.dxf2.deprecated.tracker.event;

import static org.hisp.dhis.common.OpenApi.Shared.Pattern.TRACKER;

import java.util.Objects;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.program.UserInfoSnapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Shared( pattern = TRACKER )
@JacksonXmlRootElement( localName = "dataValue", namespace = DxfNamespaces.DXF_2_0 )
public class DataValue
{
    private String created;

    private UserInfoSnapshot createdByUserInfo;

    private String lastUpdated;

    private UserInfoSnapshot lastUpdatedByUserInfo;

    private String value;

    private String dataElement = "";

    private Boolean providedElsewhere = false;

    private String storedBy;

    private boolean skipSynchronization;

    public DataValue()
    {
    }

    public DataValue( String dataElement, String value )
    {
        this.dataElement = dataElement;
        this.value = value;
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserInfoSnapshot getCreatedByUserInfo()
    {
        return createdByUserInfo;
    }

    public void setCreatedByUserInfo( UserInfoSnapshot createdByUserInfo )
    {
        this.createdByUserInfo = createdByUserInfo;
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
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserInfoSnapshot getLastUpdatedByUserInfo()
    {
        return lastUpdatedByUserInfo;
    }

    public void setLastUpdatedByUserInfo( UserInfoSnapshot lastUpdatedByUserInfo )
    {
        this.lastUpdatedByUserInfo = lastUpdatedByUserInfo;
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
    public Boolean getProvidedElsewhere()
    {
        return providedElsewhere;
    }

    public void setProvidedElsewhere( Boolean providedElsewhere )
    {
        this.providedElsewhere = providedElsewhere;
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

    @JsonIgnore
    public boolean isSkipSynchronization()
    {
        return skipSynchronization;
    }

    public void setSkipSynchronization( boolean skipSynchronization )
    {
        this.skipSynchronization = skipSynchronization;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }
        if ( object == null || getClass() != object.getClass() )
        {
            return false;
        }

        DataValue dataValue = (DataValue) object;

        return dataElement.equals( dataValue.dataElement );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( dataElement );
    }

    @Override
    public String toString()
    {
        return "DataValue{" +
            "value='" + value + '\'' +
            ", dataElement='" + dataElement + '\'' +
            ", providedElsewhere=" + providedElsewhere +
            ", storedBy='" + storedBy + '\'' +
            ", skipSynchronization='" + skipSynchronization + '\'' +
            '}';
    }
}
