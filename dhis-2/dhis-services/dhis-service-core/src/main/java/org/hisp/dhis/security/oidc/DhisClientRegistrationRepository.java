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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_CLIENT_ID;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_CLIENT_SECRET;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_PROVIDER_REDIR_BASE_URL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component( "dhisClientRegistrationRepository" )
public class DhisClientRegistrationRepository
    implements ClientRegistrationRepository
{
    private InMemoryClientRegistrationRepository repository;

    @Autowired
    public DhisConfigurationProvider dhisConfigurationProvider;

    @PostConstruct
    public void init()
    {
        String id = dhisConfigurationProvider.getProperty( OIDC_PROVIDER_CLIENT_ID );
        String secret = dhisConfigurationProvider.getProperty( OIDC_PROVIDER_CLIENT_SECRET );
        String redirBaseUri = dhisConfigurationProvider.getProperty( OIDC_PROVIDER_REDIR_BASE_URL );

        HashMap<String, Object> metaDataMap = new HashMap<>();
        metaDataMap.put( "end_session_endpoint", "https://oidc-ver2.difi.no/idporten-oidc-provider/endsession" );

        ClientRegistration idporten = ClientRegistration.withRegistrationId( "idporten" )
            .clientId( id )
            .clientSecret( secret )
            .clientAuthenticationMethod( ClientAuthenticationMethod.BASIC )
            .authorizationGrantType( AuthorizationGrantType.AUTHORIZATION_CODE )
            .redirectUriTemplate( redirBaseUri + "/login/oauth2/code/{registrationId}" )
            .scope( "openid", "profile" )
            .authorizationUri( "https://oidc-ver2.difi.no/idporten-oidc-provider/authorize" )
            .tokenUri( "https://oidc-ver2.difi.no/idporten-oidc-provider/token" )
            .userInfoUri( "https://oidc-ver2.difi.no/idporten-oidc-provider/userinfo" )
            .userNameAttributeName( IdTokenClaimNames.SUB )
            .userInfoAuthenticationMethod( AuthenticationMethod.HEADER )
            .jwkSetUri( "https://oidc-ver2.difi.no/idporten-oidc-provider/jwk" )
            .providerConfigurationMetadata( metaDataMap )
            .clientName( "idporten" )
            .build();

        repository = new InMemoryClientRegistrationRepository( idporten );
    }

    @Override
    public ClientRegistration findByRegistrationId( String registrationId )
    {
        ClientRegistration byRegistrationId = repository.findByRegistrationId( registrationId );
        return byRegistrationId;
    }
}
