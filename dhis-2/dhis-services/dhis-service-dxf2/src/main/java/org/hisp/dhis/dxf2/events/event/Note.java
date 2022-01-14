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
package org.hisp.dhis.dxf2.events.event;

import java.util.Date;
import java.util.Objects;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.program.UserInfoSnapshot;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "note", namespace = DxfNamespaces.DXF_2_0 )
public class Note
{
    private String note;

    private String value;

    private String storedBy;

    private String storedDate;

    private UserInfoSnapshot lastUpdatedBy;

    private Date lastUpdated;

    public Note()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getNote()
    {
        return note;
    }

    public void setNote( String note )
    {
        this.note = note;
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
    public String getStoredDate()
    {
        return storedDate;
    }

    public void setStoredDate( String storedDate )
    {
        this.storedDate = storedDate;
    }

    @JsonProperty
    @JacksonXmlProperty
    public UserInfoSnapshot getLastUpdatedBy()
    {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy( UserInfoSnapshot lastUpdatedBy )
    {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    public void setLastUpdated( Date lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Note that = (Note) o;
        return Objects.equals( note, that.note ) &&
            Objects.equals( storedDate, that.storedDate ) &&
            Objects.equals( storedBy, that.storedBy ) &&
            Objects.equals( value, that.value );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( note, storedDate, storedBy, value );
    }

    @Override
    public String toString()
    {
        return "Note{" +
            "note='" + note + '\'' +
            ", value='" + value + '\'' +
            ", storedBy='" + storedBy + '\'' +
            ", storedDate='" + storedDate + '\'' +
            '}';
    }
}
