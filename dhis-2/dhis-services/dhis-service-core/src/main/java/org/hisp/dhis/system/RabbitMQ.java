package org.hisp.dhis.system;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "rabbitmq", namespace = DxfNamespaces.DXF_2_0 )
public class RabbitMQ
{
    private String addresses;

    private String host;

    private String virtualHost;

    private Integer port;

    private String exchange;

    private String username;

    private String password;

    private int connectionTimeout;

    public RabbitMQ()
    {
    }

    public RabbitMQ( String host, Integer port, String username, String password )
    {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getAddresses()
    {
        return addresses;
    }

    public RabbitMQ setAddresses( String addresses )
    {
        this.addresses = addresses;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getHost()
    {
        return host;
    }

    public RabbitMQ setHost( String host )
    {
        this.host = host;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getVirtualHost()
    {
        return virtualHost;
    }

    public RabbitMQ setVirtualHost( String virtualHost )
    {
        this.virtualHost = virtualHost;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getPort()
    {
        return port;
    }

    public RabbitMQ setPort( Integer port )
    {
        this.port = port;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getExchange()
    {
        return exchange;
    }

    public RabbitMQ setExchange( String exchange )
    {
        this.exchange = exchange;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUsername()
    {
        return username;
    }

    public RabbitMQ setUsername( String username )
    {
        this.username = username;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getPassword()
    {
        return password;
    }

    public RabbitMQ setPassword( String password )
    {
        this.password = password;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public RabbitMQ setConnectionTimeout( int connectionTimeout )
    {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public boolean isValid()
    {
        return host != null;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "addresses", addresses )
            .add( "host", host )
            .add( "virtualHost", virtualHost )
            .add( "port", port )
            .add( "username", username )
            .add( "password", password )
            .add( "connectionTimeout", connectionTimeout )
            .toString();
    }
}