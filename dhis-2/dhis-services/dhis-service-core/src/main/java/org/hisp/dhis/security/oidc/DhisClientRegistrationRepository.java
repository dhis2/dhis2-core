package org.hisp.dhis.security.oidc;

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
 *
 */

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Set;

import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_ID;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_SECRET;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_MAPPING_CLAIM;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_GOOGLE_REDIR_BASE_URL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */

@Component( "dhisClientRegistrationRepository" )
public class DhisClientRegistrationRepository
    implements ClientRegistrationRepository
{
    private InMemoryClientRegistrationRepository repository;

    @Autowired
    private DhisConfigurationProvider dhisConfigurationProvider;

    private static final HashMap<String, DhisOidcClientRegistration> registrationHashMap = new HashMap<>();

    @PostConstruct
    public void init()
    {
        String googleClientId = dhisConfigurationProvider.getProperty( OIDC_PROVIDER_GOOGLE_CLIENT_ID );
        String googleClientSecret = dhisConfigurationProvider.getProperty( OIDC_PROVIDER_GOOGLE_CLIENT_SECRET );
        String googleClientRedirBaseUri = dhisConfigurationProvider.getProperty( OIDC_PROVIDER_GOOGLE_REDIR_BASE_URL );
        String googleClientMappingClaim = dhisConfigurationProvider.getProperty( OIDC_PROVIDER_GOOGLE_MAPPING_CLAIM );

        String registrationId = "google";
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder( registrationId )
            .clientId( googleClientId )
            .clientSecret( googleClientSecret )
            .redirectUriTemplate( googleClientRedirBaseUri + "/oauth2/code/{registrationId}" )
            .build();

        DhisOidcClientRegistration registration = DhisOidcClientRegistration.builder()
            .clientRegistration( google )
            .mappingClaimKey( googleClientMappingClaim )
            .registrationId( registrationId )
            .build();

        registrationHashMap.put( registrationId, registration );

        repository = new InMemoryClientRegistrationRepository( google );
    }

    @Override
    public ClientRegistration findByRegistrationId( String registrationId )
    {
        return repository.findByRegistrationId( registrationId );
    }

    public DhisOidcClientRegistration getDhisOidcClientRegistration( String registrationId )
    {
        return registrationHashMap.get( registrationId );
    }

    public Set<String> getAllRegistrationId()
    {
        return registrationHashMap.keySet();
    }
}
