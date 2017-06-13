package org.hisp.dhis.sms.config;

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

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public class GenericGatewayParameter
    implements Serializable
{
    private static final long serialVersionUID = -863990758156009672L;

    private boolean header;

    private String key;

    private String value;

    private boolean classified;

    public GenericGatewayParameter()
    {
    }

    public GenericGatewayParameter( String key, String value, boolean classified )
    {
        super();
        this.key = key;
        this.value = value;
        this.classified = classified;
    }

    @JsonProperty( value = "key" )
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getValue()
    {
        return value;
    }

    @JsonProperty( value = "value" )
    public String getValueFilterIfClassified()
    {
        return classified ? "" : value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    @JsonProperty( value = "classified" )
    public boolean isClassified()
    {
        return classified;
    }

    public void setClassified( boolean classified )
    {
        this.classified = classified;
    }

    @JsonProperty
    public boolean isHeader()
    {
        return header;
    }

    public void setHeader( boolean header )
    {
        this.header = header;
    }
}
