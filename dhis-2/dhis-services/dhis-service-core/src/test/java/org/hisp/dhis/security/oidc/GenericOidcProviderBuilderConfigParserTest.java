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
package org.hisp.dhis.security.oidc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class GenericOidcProviderBuilderConfigParserTest
{
    @Test
    public void parseConfigAllValidParameters()
    {
        Properties properties = new Properties();

        properties.put( "oidc.provider.idporten.client_id", "testClientId" );
        properties.put( "oidc.provider.idporten.client_secret", "testClientSecret!#!?" );
        properties.put( "oidc.provider.idporten.authorization_uri", "https://oidc-ver2.difi.no/authorize" );
        properties.put( "oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token" );
        properties.put( "oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo" );
        properties.put( "oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk" );
        properties.put( "oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession" );
        properties.put( "oidc.provider.idporten.scopes", "pid" );
        properties.put( "oidc.provider.idporten.mapping_claim", "helseid://claims/identity/pid" );
        properties.put( "oidc.provider.idporten.display_alias", "IdPorten" );
        properties.put( "oidc.provider.idporten.enable_logout", "true" );
        properties.put( "oidc.provider.idporten.logo_image", "../security/idporten-logo.svg" );
        properties.put( "oidc.provider.idporten.logo_image_padding", "0px 0px" );
        properties.put( "oidc.provider.idporten.extra_request_parameters", "acr_value 4,test_param five" );
        properties.put( "oidc.provider.idporten.enable_pkce", "false" );

        List<Map<String, String>> parse = GenericOidcProviderConfigParser.parse( properties );

        assertThat( parse, hasSize( 1 ) );
    }

    @Test
    public void parseValidMinimumConfig()
    {
        Properties properties = new Properties();

        properties.put( "oidc.provider.idporten.client_id", "testClientId" );
        properties.put( "oidc.provider.idporten.client_secret", "testClientSecret!#!?" );
        properties.put( "oidc.provider.idporten.authorization_uri", "https://oidc-ver2.difi.no/authorize" );
        properties.put( "oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token" );
        properties.put( "oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo" );
        properties.put( "oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk" );
        properties.put( "oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession" );

        List<Map<String, String>> parse = GenericOidcProviderConfigParser.parse( properties );

        assertThat( parse, hasSize( 1 ) );
    }

    @Test
    public void parseConfigMissingRequiredParameter()
    {
        Properties properties = new Properties();

        properties.put( "oidc.provider.idporten.client_id", "testClientId" );
        properties.put( "oidc.provider.idporten.client_secret", "testClientSecret!#!?" );
        properties.put( "oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token" );
        properties.put( "oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo" );
        properties.put( "oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk" );
        properties.put( "oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession" );

        List<Map<String, String>> parse = GenericOidcProviderConfigParser.parse( properties );

        assertThat( parse, hasSize( 0 ) );
    }

    @Test
    public void parseConfigMalformedKeyNameParameter()
    {
        Properties properties = new Properties();

        properties.put( "oidc.provider.idporten.client_id", "testClientId" );
        properties.put( "oidc.provider.idporten.client_secret", "testClientSecret!#!?" );
        properties.put( "oidc.provider.idporten.INVALID_PROPERTY_NAME", "https://oidc-ver2.difi.no/authorize" );
        properties.put( "oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token" );
        properties.put( "oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo" );
        properties.put( "oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk" );
        properties.put( "oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession" );

        List<Map<String, String>> parse = GenericOidcProviderConfigParser.parse( properties );

        assertThat( parse, hasSize( 0 ) );
    }

    @Test
    public void parseConfigInvalidURIParameter()
    {
        Properties properties = new Properties();

        properties.put( "oidc.provider.idporten.client_id", "testClientId" );
        properties.put( "oidc.provider.idporten.client_secret", "testClientSecret!#!?" );
        properties
            .put( "oidc.provider.idporten.authorization_uri", "INVALID_URI_SCHEME://oidc-ver2.difi.no/authorize" );
        properties.put( "oidc.provider.idporten.token_uri", "https://oidc-ver2.difi.no/token" );
        properties.put( "oidc.provider.idporten.user_info_uri", "https://oidc-ver2.difi.no/userinfo" );
        properties.put( "oidc.provider.idporten.jwk_uri", "https://oidc-ver2.difi.no/jwk" );
        properties.put( "oidc.provider.idporten.end_session_endpoint", "https://oidc-ver2.difi.no/endsession" );

        List<Map<String, String>> parse = GenericOidcProviderConfigParser.parse( properties );

        assertThat( parse, hasSize( 0 ) );
    }
}