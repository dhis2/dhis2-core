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
package org.hisp.dhis.dxf2.datavalue;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.datavalueset.DataValueEntry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@JacksonXmlRootElement( localName = "dataValue", namespace = DxfNamespaces.DXF_2_0 )
public final class DataValue implements DataValueEntry
{
    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String dataElement;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String period;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String orgUnit;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String categoryOptionCombo;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String attributeOptionCombo;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String value;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String storedBy;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String created;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String lastUpdated;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private String comment;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private boolean followup;

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    private Boolean deleted;

    @Override
    public boolean getFollowup()
    {
        return followup;
    }
}
