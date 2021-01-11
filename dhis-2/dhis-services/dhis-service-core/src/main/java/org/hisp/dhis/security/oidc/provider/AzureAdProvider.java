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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * <p>
 * Well known url for reference. In a perfect world we would dynamically parse this.
 * https://login.microsoftonline.com/"+tenant+"/v2.0/.well-known/openid-configuration
 */
public class AzureAdProvider extends DhisOidcProvider
{
    public static final int MAX_AZURE_TENANTS = 10;

    public static final String PROVIDER_PREFIX = "oidc.provider.azure.";

    public static final String AZURE_TENANT = ".tenant";

    public static final String AZURE_DISPLAY_ALIAS = ".display_alias";

    public static final String AZURE_CLIENT_ID = ".client_id";

    public static final String AZURE_CLIENT_SECRET = ".client_secret";

    public static final String AZURE_REDIRECT_BASE_URL = ".redirect_baseurl";

    public static final String AZURE_MAPPING_CLAIM = ".mapping_claim";

    public static final String AZURE_SUPPORT_LOGOUT = ".support_logout";

    public static final String PROVIDER_STATIC_BASE_URL = "https://login.microsoftonline.com/";

    private AzureAdProvider()
    {
        throw new IllegalStateException( "Utility class" );
    }

    public static List<DhisOidcClientRegistration> buildList( DhisConfigurationProvider config )
    {
        Objects.requireNonNull( config, "DhisConfigurationProvider is missing!" );

        final Properties properties = config.getProperties();

        final ImmutableList.Builder<DhisOidcClientRegistration> clients = ImmutableList.builder();

        for ( int i = 0; i < MAX_AZURE_TENANTS; i++ )
        {
            String tenant = properties.getProperty( PROVIDER_PREFIX + i + AZURE_TENANT, "" );

            if ( tenant.isEmpty() )
            {
                continue;
            }

            String clientId = properties.getProperty( PROVIDER_PREFIX + i + AZURE_CLIENT_ID, "" );
            String clientSecret = config.getProperties().getProperty( PROVIDER_PREFIX + i + AZURE_CLIENT_SECRET );
            boolean supportLogout = Boolean.parseBoolean( MoreObjects.firstNonNull( config.getProperties()
                .getProperty( PROVIDER_PREFIX + i + AZURE_SUPPORT_LOGOUT ), "TRUE" ) );
            String mappingClaims = MoreObjects
                .firstNonNull( config.getProperties().getProperty( PROVIDER_PREFIX + i + AZURE_MAPPING_CLAIM ),
                    DEFAULT_MAPPING_CLAIM );

            if ( clientId.isEmpty() )
            {
                throw new IllegalArgumentException( "Azure client id is missing! tenant=" + tenant );
            }

            if ( clientSecret.isEmpty() )
            {
                throw new IllegalArgumentException( "Azure client secret is missing! tenant=" + tenant );
            }

            ClientRegistration.Builder builder = ClientRegistration.withRegistrationId( tenant );

            String providerBaseUrl = PROVIDER_STATIC_BASE_URL + tenant;

            builder.clientAuthenticationMethod( ClientAuthenticationMethod.BASIC );
            builder.authorizationGrantType( AuthorizationGrantType.AUTHORIZATION_CODE );
            builder.scope( "openid", "profile", DEFAULT_MAPPING_CLAIM );
            builder.authorizationUri( providerBaseUrl + "/oauth2/v2.0/authorize" );
            builder.tokenUri( providerBaseUrl + "/oauth2/v2.0/token" );
            builder.jwkSetUri( providerBaseUrl + "/discovery/v2.0/keys" );
            builder.userInfoUri( "https://graph.microsoft.com/oidc/userinfo" );
            builder.clientName( tenant );
            builder.clientId( clientId );
            builder.clientSecret( clientSecret );
            builder.redirectUri( DEFAULT_REDIRECT_TEMPLATE_URL );
            builder.userInfoAuthenticationMethod( AuthenticationMethod.HEADER );
            builder.userNameAttributeName( IdTokenClaimNames.SUB );

            if ( supportLogout )
            {
                builder.providerConfigurationMetadata(
                    ImmutableMap.of( "end_session_endpoint", providerBaseUrl + "/oauth2/v2.0/logout" ) );
            }

            ClientRegistration clientRegistration = builder.build();

            DhisOidcClientRegistration dhisOidcClientRegistration = DhisOidcClientRegistration.builder()
                .clientRegistration( clientRegistration )
                .mappingClaimKey( mappingClaims )
                .loginIcon( "../security/btn_azure_login.svg" )
                .loginIconPadding( "13px 13px" )
                .loginText( properties.getProperty( PROVIDER_PREFIX + i + AZURE_DISPLAY_ALIAS, "login_with_azure" ) )
                .build();

            clients.add( dhisOidcClientRegistration );
        }

        return clients.build();
    }
}
