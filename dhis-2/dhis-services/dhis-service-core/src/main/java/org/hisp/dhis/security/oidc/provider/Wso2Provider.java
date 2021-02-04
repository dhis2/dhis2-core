package org.hisp.dhis.security.oidc.provider;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.Objects;

import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_WSO2_CLIENT_ID;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_WSO2_CLIENT_SECRET;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_WSO2_DISPLAY_ALIAS;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_WSO2_ENABLE_LOGOUT;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_WSO2_MAPPING_CLAIM;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_WSO2_SERVER_URL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class Wso2Provider extends DhisOidcProvider
{
    public static final String REGISTRATION_ID = "wso2";

    private Wso2Provider()
    {
        throw new IllegalStateException( "Utility class" );
    }

    public static DhisOidcClientRegistration build( DhisConfigurationProvider config )
    {
        Objects.requireNonNull( config, "DhisConfigurationProvider is missing!" );

        String wso2ClientId = config.getProperty( OIDC_PROVIDER_WSO2_CLIENT_ID );

        if ( wso2ClientId.isEmpty() )
        {
            return null;
        }

        String wso2ClientSecret = config.getProperty( OIDC_PROVIDER_WSO2_CLIENT_SECRET );
        String wso2ClientMappingClaim = config.getProperty( OIDC_PROVIDER_WSO2_MAPPING_CLAIM );
        String providerBaseUrl = config.getProperty( OIDC_PROVIDER_WSO2_SERVER_URL );
        boolean supportLogout = Boolean.parseBoolean( config.getProperty( OIDC_PROVIDER_WSO2_ENABLE_LOGOUT ) );

        if ( wso2ClientSecret.isEmpty() )
        {
            throw new IllegalArgumentException( "WSO2 client secret is missing!" );
        }

        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId( Wso2Provider.REGISTRATION_ID );

        builder.clientAuthenticationMethod( ClientAuthenticationMethod.BASIC );
        builder.authorizationGrantType( AuthorizationGrantType.AUTHORIZATION_CODE );
        builder.scope( "openid", "profile", DEFAULT_MAPPING_CLAIM );
        builder.authorizationUri( providerBaseUrl + "/oauth2/authorize" );
        builder.tokenUri( providerBaseUrl + "/oauth2/token" );
        builder.jwkSetUri( providerBaseUrl + "/oauth2/jwks" );
        builder.userInfoUri( providerBaseUrl + "/oauth2/userinfo" );
        builder.clientName( wso2ClientId );
        builder.clientId( wso2ClientId );
        builder.clientSecret( wso2ClientSecret );
        builder.redirectUri( DEFAULT_REDIRECT_TEMPLATE_URL );
        builder.userInfoAuthenticationMethod( AuthenticationMethod.HEADER );
        builder.userNameAttributeName( IdTokenClaimNames.SUB );

        if ( supportLogout )
        {
            builder.providerConfigurationMetadata( ImmutableMap
                .of( "end_session_endpoint", providerBaseUrl + "/oidc/logout" ) );
        }

        ClientRegistration clientRegistration = builder.build();

        return DhisOidcClientRegistration.builder()
            .clientRegistration( clientRegistration )
            .mappingClaimKey( wso2ClientMappingClaim )
            .loginIcon( "../security/wso2-logo.svg" )
            .loginIconPadding( "0px 1px" )
            .loginText( config.getProperty( OIDC_PROVIDER_WSO2_DISPLAY_ALIAS ) )
            .build();
    }
}
