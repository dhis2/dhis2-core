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
package org.hisp.dhis.security.oidc;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
public class DhisOidcUserService
    extends OidcUserService
{
    @Autowired
    public UserService userService;

    @Autowired
    private DhisOidcProviderRepository clientRegistrationRepository;

    @Override
    public OidcUser loadUser( OidcUserRequest userRequest )
        throws OAuth2AuthenticationException
    {
        OidcUser oidcUser = super.loadUser( userRequest );

        ClientRegistration clientRegistration = userRequest.getClientRegistration();

        DhisOidcClientRegistration oidcClientRegistration = clientRegistrationRepository
            .getDhisOidcClientRegistration( clientRegistration.getRegistrationId() );

        String mappingClaimKey = oidcClientRegistration.getMappingClaimKey();
        Map<String, Object> attributes = oidcUser.getAttributes();
        Object claimValue = attributes.get( mappingClaimKey );
        OidcUserInfo userInfo = oidcUser.getUserInfo();
        if ( claimValue == null && userInfo != null )
        {
            claimValue = userInfo.getClaim( mappingClaimKey );
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( String
                .format( "Trying to look up DHIS2 user with OidcUser mapping mappingClaimKey='%s', claim value='%s'",
                    mappingClaimKey, claimValue ) );
        }

        if ( claimValue != null )
        {
            User user = userService.getUserByOpenId( (String) claimValue );
            if ( user != null && user.isExternalAuth() )
            {
                if ( user.isDisabled() || !user.isAccountNonExpired() )
                {
                    throw new OAuth2AuthenticationException( new OAuth2Error( "user_disabled" ),
                        "User is disabled" );
                }

                CurrentUserDetails userDetails = userService.createUserDetails( user );
                return new DhisOidcUser( userDetails, attributes, IdTokenClaimNames.SUB, oidcUser.getIdToken() );
            }
        }

        String errorMessage = String
            .format(
                "Failed to look up DHIS2 user with OidcUser mapping mapping; mappingClaimKey='%s', claimValue='%s'",
                mappingClaimKey, claimValue );

        if ( log.isDebugEnabled() )
        {
            log.debug( errorMessage );
        }

        OAuth2Error oauth2Error = new OAuth2Error(
            "could_not_map_oidc_user_to_dhis2_user",
            errorMessage,
            null );

        throw new OAuth2AuthenticationException( oauth2Error, oauth2Error.toString() );
    }
}