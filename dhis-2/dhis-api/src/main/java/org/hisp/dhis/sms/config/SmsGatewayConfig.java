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
package org.hisp.dhis.sms.config;

import java.io.Serializable;

import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Super class for gateway configurations
 *
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@JacksonXmlRootElement( localName = "smsgatewayconfig", namespace = DxfNamespaces.DXF_2_0 )
@JsonInclude( JsonInclude.Include.NON_NULL )
@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, property = "type" )
@JsonSubTypes( { @JsonSubTypes.Type( value = BulkSmsGatewayConfig.class, name = "bulksms" ),
    @JsonSubTypes.Type( value = GenericHttpGatewayConfig.class, name = "http" ),
    @JsonSubTypes.Type( value = ClickatellGatewayConfig.class, name = "clickatell" ),
    @JsonSubTypes.Type( value = SMPPGatewayConfig.class, name = "smpp" ) } )
public abstract class SmsGatewayConfig
    implements Serializable
{
    private static final long serialVersionUID = -4288220735161151632L;

    @JsonProperty
    private String uid;

    @JsonProperty
    private String name;

    @JsonProperty
    private String username;

    @JsonProperty
    private String password;

    private boolean isDefault;

    @JsonProperty
    private boolean sendUrlParameters;

    @JsonProperty
    private String urlTemplate;

    @JsonProperty
    private String maxSmsLength;

    public String getUrlTemplate()
    {
        return urlTemplate;
    }

    public void setUrlTemplate( String urlTemplate )
    {
        this.urlTemplate = urlTemplate;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty( "isDefault" )
    public boolean isDefault()
    {
        return isDefault;
    }

    public void setDefault( boolean isDefault )
    {
        this.isDefault = isDefault;
    }

    public String getUid()
    {
        return uid;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }

    public boolean isSendUrlParameters()
    {
        return sendUrlParameters;
    }

    public void setSendUrlParameters( boolean sendUrlParameters )
    {
        this.sendUrlParameters = sendUrlParameters;
    }

    public String getMaxSmsLength()
    {
        return maxSmsLength;
    }

    public void setMaxSmsLength( String maxSmsLength )
    {
        this.maxSmsLength = maxSmsLength;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !(o instanceof SmsGatewayConfig) )
        {
            return false;
        }

        final SmsGatewayConfig other = (SmsGatewayConfig) o;

        return uid.equals( other.getUid() );
    }

    @Override
    public int hashCode()
    {
        return uid.hashCode();
    }

    @Override
    public String toString()
    {
        return "SmsGatewayConfig{" +
            "uid='" + uid + '\'' +
            ", name='" + name + '\'' +
            ", username='" + username + '\'' +
            ", isDefault=" + isDefault +
            ", urlTemplate='" + urlTemplate + '\'' +
            '}';
    }
}
