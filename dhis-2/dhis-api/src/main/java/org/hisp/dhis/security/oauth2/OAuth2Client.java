package org.hisp.dhis.security.oauth2;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeStrategy;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.schema.annotation.PropertyRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "oAuth2Client", namespace = DxfNamespaces.DXF_2_0 )
public class OAuth2Client extends BaseIdentifiableObject
{
    /**
     * client_id
     */
    private String cid;

    /**
     * client_secret
     */
    private String secret = UUID.randomUUID().toString();

    /**
     * List of allowed redirect URI targets for this client.
     */
    private List<String> redirectUris = new ArrayList<>();

    /**
     * List of allowed grant types for this client.
     */
    private List<String> grantTypes = new ArrayList<>();

    public OAuth2Client()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.IDENTIFIER )
    public String getCid()
    {
        return cid;
    }

    public void setCid( String cid )
    {
        this.cid = cid;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 36, max = 36 )
    public String getSecret()
    {
        return secret;
    }

    public void setSecret( String secret )
    {
        this.secret = secret;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "redirectUris", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "redirectUri", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getRedirectUris()
    {
        return redirectUris;
    }

    public void setRedirectUris( List<String> redirectUris )
    {
        this.redirectUris = redirectUris;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "grantTypes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "grantType", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getGrantTypes()
    {
        return grantTypes;
    }

    public void setGrantTypes( List<String> grantTypes )
    {
        this.grantTypes = grantTypes;
    }

    @Override
    public int hashCode()
    {
        return 31 * super.hashCode() + Objects.hash( cid, secret, redirectUris, grantTypes );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() )
        {
            return false;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }

        final OAuth2Client other = (OAuth2Client) obj;

        return Objects.equals( this.cid, other.cid )
            && Objects.equals( this.secret, other.secret )
            && Objects.equals( this.redirectUris, other.redirectUris )
            && Objects.equals( this.grantTypes, other.grantTypes );
    }

    @Override
    public void mergeWith( IdentifiableObject other, MergeStrategy strategy )
    {
        super.mergeWith( other, strategy );

        if ( other.getClass().isInstance( this ) )
        {
            OAuth2Client oAuth2Client = (OAuth2Client) other;

            if ( strategy.isReplace() )
            {
                cid = oAuth2Client.getCid();
                secret = oAuth2Client.getSecret();
            }
            else if ( strategy.isMerge() )
            {
                cid = oAuth2Client.getCid() == null ? cid : oAuth2Client.getCid();
                secret = oAuth2Client.getSecret() == null ? secret : oAuth2Client.getSecret();
            }

            redirectUris.clear();
            grantTypes.clear();

            redirectUris.addAll( oAuth2Client.getRedirectUris() );
            grantTypes.addAll( oAuth2Client.getGrantTypes() );
        }
    }
}
