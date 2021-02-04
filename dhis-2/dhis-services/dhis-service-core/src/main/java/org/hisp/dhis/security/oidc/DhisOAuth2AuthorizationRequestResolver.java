package org.hisp.dhis.security.oidc;

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

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.function.Consumer;

import static org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component( "dhisOAuth2AuthorizationRequestResolver" )
public class DhisOAuth2AuthorizationRequestResolver
    implements OAuth2AuthorizationRequestResolver
{

    private DefaultOAuth2AuthorizationRequestResolver resolver;

    @Autowired
    private DhisClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public DhisConfigurationProvider dhisConfigurationProvider;

    @PostConstruct
    public void init()
    {
        this.resolver = new DefaultOAuth2AuthorizationRequestResolver( clientRegistrationRepository,
            DEFAULT_AUTHORIZATION_REQUEST_BASE_URI );
        this.resolver.setAuthorizationRequestCustomizer( authorizationRequestCustomizer() );
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer()
    {
        Consumer<OAuth2AuthorizationRequest.Builder> builderConsumer = customizer -> {
        };
        return builderConsumer;
    }

    @Override
    public OAuth2AuthorizationRequest resolve( HttpServletRequest request )
    {
        return this.resolver.resolve( request );
    }

    @Override
    public OAuth2AuthorizationRequest resolve( HttpServletRequest request, String clientRegistrationId )
    {
        return this.resolver.resolve( request, clientRegistrationId );
    }
}
