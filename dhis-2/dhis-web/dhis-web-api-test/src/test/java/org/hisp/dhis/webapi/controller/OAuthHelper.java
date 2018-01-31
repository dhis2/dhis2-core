package org.hisp.dhis.webapi.controller;

/*
 *
 *  Copyright (c) 2004-2017, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class OAuthHelper
{
    private AuthorizationServerTokenServices tokenServices;

    public OAuthHelper( AuthorizationServerTokenServices tokenServices )
    {
        this.tokenServices = tokenServices;
    }

    public RequestPostProcessor bearerToken( ClientDetails client, UserDetails userPrincipal, final String token )
    {
        return mockRequest ->
        {
            OAuth2AccessToken auth2AccessToken = createAccessToken( client, userPrincipal );

            mockRequest.addHeader( "Authorization", "Bearer " + ( token == null ? auth2AccessToken.getValue() : token ) );

            return mockRequest;
        };
    }

    public OAuth2AccessToken createAccessToken( ClientDetails client, UserDetails userPrincipal )
    {
        System.out.println( "OAuthHelper.createAccessToken" );
        System.out.println( "client = " + client );
        System.out.println( "userPrincipal = " + userPrincipal );
        if ( client == null  || userPrincipal == null )
        {
            return null;
        }

        // Default values for other parameters
        Map<String, String> requestParameters = Collections.emptyMap();
        boolean approved = true;
        String redirectUrl = null;
        Set<String> responseTypes = Collections.emptySet();
        Map<String, Serializable> extensionProperties = Collections.emptyMap();

        OAuth2Request oAuth2Request = new OAuth2Request( requestParameters, client.getClientId(), client.getAuthorities(), approved, client
            .getScope(),
            client.getResourceIds(), redirectUrl, responseTypes, extensionProperties );

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            userPrincipal,
            userPrincipal.getPassword(),
            userPrincipal.getAuthorities() );

        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication( oAuth2Request, authenticationToken );

        return tokenServices.createAccessToken( oAuth2Authentication );
    }
}