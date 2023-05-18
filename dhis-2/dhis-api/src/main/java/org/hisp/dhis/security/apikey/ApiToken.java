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
package org.hisp.dhis.security.apikey;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Getter
@Setter
@Builder( toBuilder = true )
@JacksonXmlRootElement( localName = "apiToken", namespace = DxfNamespaces.DXF_2_0 )
public class ApiToken extends BaseIdentifiableObject implements MetadataObject
{
    public ApiToken()
    {
    }

    public ApiToken( String key, Integer version, ApiTokenType type, Long expire,
        List<ApiTokenAttribute> attributes )
    {
        this.key = key;
        this.version = version;
        this.type = type;
        this.expire = expire;
        this.attributes = attributes;
    }

    @JsonIgnore
    private String key;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.INTEGER )
    private Integer version;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private ApiTokenType type;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.NUMBER )
    private Long expire;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    private List<ApiTokenAttribute> attributes = new ArrayList<>();

    private ApiTokenAttribute findApiTokenAttribute( Class<? extends ApiTokenAttribute> attributeClass )
    {
        for ( ApiTokenAttribute attribute : getAttributes() )
        {
            if ( attribute.getClass().equals( attributeClass ) )
            {
                return attribute;
            }
        }
        return null;
    }

    public void addIpToAllowedList( String ipAddress )
    {
        IpAllowedList allowedIps = getIpAllowedList();

        if ( allowedIps == null )
        {
            allowedIps = IpAllowedList.of( ipAddress );
            attributes.add( allowedIps );
        }
        else
        {
            allowedIps.getAllowedIps().add( ipAddress );
        }
    }

    public IpAllowedList getIpAllowedList()
    {
        return (IpAllowedList) findApiTokenAttribute( IpAllowedList.class );
    }

    public void addMethodToAllowedList( String methodName )
    {
        MethodAllowedList allowedMethods = getMethodAllowedList();

        if ( allowedMethods == null )
        {
            allowedMethods = MethodAllowedList.of( methodName );
            attributes.add( allowedMethods );
        }
        else
        {
            allowedMethods.getAllowedMethods().add( methodName );
        }
    }

    public MethodAllowedList getMethodAllowedList()
    {
        return (MethodAllowedList) findApiTokenAttribute( MethodAllowedList.class );
    }

    public void addReferrerToAllowedList( String referrer )
    {
        RefererAllowedList allowedReferrers = getRefererAllowedList();

        if ( allowedReferrers == null )
        {
            allowedReferrers = RefererAllowedList.of( referrer );
            attributes.add( allowedReferrers );
        }
        else
        {
            allowedReferrers.getAllowedReferrers().add( referrer );
        }
    }

    public RefererAllowedList getRefererAllowedList()
    {
        return (RefererAllowedList) findApiTokenAttribute( RefererAllowedList.class );
    }
}
