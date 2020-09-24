package org.hisp.dhis.security.oidc.provider;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.Objects;

import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_ID;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_SECRET;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_MAPPING_CLAIM;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_REDIR_BASE_URL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class GoogleProvider extends DhisOidcProvider
{
    public static final String REGISTRATION_ID = "google";

    private GoogleProvider()
    {
        throw new IllegalStateException( "Utility class" );
    }

    public static DhisOidcClientRegistration build( DhisConfigurationProvider config )
    {
        Objects.requireNonNull( config, "DhisConfigurationProvider is missing!" );

        String googleClientId = config.getProperty( OIDC_PROVIDER_GOOGLE_CLIENT_ID );
        String googleClientSecret = config.getProperty( OIDC_PROVIDER_GOOGLE_CLIENT_SECRET );
        String googleClientRedirectBaseUri = config.getProperty( OIDC_PROVIDER_GOOGLE_REDIR_BASE_URL );
        String googleClientMappingClaim = config.getProperty( OIDC_PROVIDER_GOOGLE_MAPPING_CLAIM );

        if ( googleClientId.isEmpty() )
        {
            return null;
        }
        if ( googleClientSecret.isEmpty() )
        {
            throw new IllegalArgumentException( "Google client secret is missing!" );
        }

        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder( REGISTRATION_ID )
            .clientId( googleClientId )
            .clientSecret( googleClientSecret )
            .redirectUriTemplate( googleClientRedirectBaseUri + DEFAULT_CODE_URL_TEMPLATE )
            .build();

        return DhisOidcClientRegistration.builder()
            .clientRegistration( google )
            .mappingClaimKey( googleClientMappingClaim )
            .registrationId( REGISTRATION_ID )
            .build();
    }

}
