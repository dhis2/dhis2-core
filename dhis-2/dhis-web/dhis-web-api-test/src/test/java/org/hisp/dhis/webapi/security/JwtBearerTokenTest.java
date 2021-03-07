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

package org.hisp.dhis.webapi.security;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.hisp.dhis.webapi.utils.WebClientUtils.failOnException;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.security.jwt.DhisJwtAuthenticationManagerResolver;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.oidc.GenericOidcProviderConfigParser;
import org.hisp.dhis.security.oidc.provider.GoogleProvider;
import org.hisp.dhis.webapi.DhisControllerConvenienceWithAuthTest;
import org.hisp.dhis.webapi.WebClient;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.utils.JoseHeader;
import org.hisp.dhis.webapi.utils.JoseHeaderNames;
import org.hisp.dhis.webapi.utils.JwtClaimsSet;
import org.hisp.dhis.webapi.utils.JwtUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class JwtBearerTokenTest extends DhisControllerConvenienceWithAuthTest
{
    public static final String EXPIRED_GOOGLE_JWT_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImU4NzMyZGIwNjI4NzUxNTU1NjIxM2I4MGFjYmNmZDA4Y2ZiMzAyYTkiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiIxMDE5NDE3MDAyNTQ0LW1xYTdmbGs0bWpvaHJnc2JnOWJ0YTlidmx1b2o4NW8wLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiYXVkIjoiMTAxOTQxNzAwMjU0NC1tcWE3ZmxrNG1qb2hyZ3NiZzlidGE5YnZsdW9qODVvMC5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInN1YiI6IjExMDk3ODA1MDEyNzk2NzA2NTUwNiIsImVtYWlsIjoiZGhpczJvaWRjdXNlckBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6IkhXbTNXcXphM2p5TEFUZjNlU1pBNVEiLCJuYW1lIjoiZGhpczJvaWRjdXNlciBUZXN0ZXIiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tLy1oRmptUnhOQkJTWS9BQUFBQUFBQUFBSS9BQUFBQUFBQUFBQS9BTVp1dWNuQkdYVTF5X05fV25qSXJndHBpSXFWMl9ndll3L3M5Ni1jL3Bob3RvLmpwZyIsImdpdmVuX25hbWUiOiJkaGlzMm9pZGN1c2VyIiwiZmFtaWx5X25hbWUiOiJUZXN0ZXIiLCJsb2NhbGUiOiJlbiIsImlhdCI6MTYxNDk1NzU5MCwiZXhwIjoxNjE0OTYxMTkwfQ.OCm7hj4H-UqRpM_Xrfq58U3ZGI3k7-S3c4AslVAaMxKsNitsPDZ7oxs-FJT-E7uDqnp1wW5LyBLj8jfJZ4JnvuiNGZrvCCpR3m70_4mSgP8VTjFFEijgfW1IIy_BWI8gDY6iCK7qgOATdYnCyJteWBMKRPr5wVSN05TT3xxLzsE7C5ViOzHAm2v6XrrsEhfcjNmwKmlljjpImTwtUSTBS3DWoWsHaNqXfE3rO0M7231FWl2X0vk5oO-KycNoS1vDZLAvdf6QRJVnPMkQ6Cx5XSMSYEmUmFqM3Sj2ip0Q48hAe4ydzIgRWdGbzGnMH3euqGWr4_G_EBvVqfVPnBF0YA";

    public static final String CORRECT_AUDIENCE_AKA_CLIENTID = "1019417002544-mqa7flk4mjohrgsbg9bta9bvluoj85o0.apps.googleusercontent.com";

    @Autowired
    private DhisOidcProviderRepository dhisOidcProviderRepository;

    @Autowired
    private DhisJwtAuthenticationManagerResolver dhisJwtAuthenticationManagerResolver;

    private static List<JWK> jwkList;

    private static JWKSource<SecurityContext> jwkSource;

    private static JwtUtils jwsEncoder;

    private static final RSAKey defaultRSA = TestJwks.DEFAULT_RSA_JWK;

    private static NimbusJwtDecoder jwtDecoder;

    @BeforeClass
    public static void setUpClass()
        throws JOSEException
    {
        jwkList = new ArrayList<>();
        jwkList.add( defaultRSA );

        jwkSource = ( jwkSelector, securityContext ) -> jwkSelector.select( new JWKSet( jwkList ) );
        jwsEncoder = new JwtUtils( jwkSource );

        jwtDecoder = NimbusJwtDecoder.withPublicKey( defaultRSA.toRSAPublicKey() ).build();
    }

    @Before
    public void setUp()
    {
        dhisJwtAuthenticationManagerResolver.setJwtDecoder( jwtDecoder );

        dhisOidcProviderRepository.clear();
    }

    private Jwt createJwt( String provider )
    {
        JoseHeader joseHeader = TestJoseHeaders.joseHeader( provider ).build();
        JwtClaimsSet jwtClaimsSet = TestJwtClaimsSets.jwtClaimsSet( provider ).build();

        return jwsEncoder.encode( joseHeader, jwtClaimsSet );
    }

    @Test
    public void testJwkEncodeEndDecode()
        throws JOSEException
    {
        Jwt encodedJws = createJwt("testproviderone.com");

        // Assert headers/claims were added
        assertEquals( "JWT", encodedJws.getHeaders().get( JoseHeaderNames.TYP ) );
        assertEquals( defaultRSA.getKeyID(), encodedJws.getHeaders().get( JoseHeaderNames.KID ) );
        assertNotNull( encodedJws.getId() );

        String tokenValue = encodedJws.getTokenValue();

        Jwt decode = jwtDecoder.decode( tokenValue );
        log.info( "decode:" + decode );
    }

    @Test
    public void testMalformedToken()
    {
        JsonError error = GET( "NOT_A_JWT_TOKEN", "/api/me" ).error();

        assertEquals( HttpStatus.UNAUTHORIZED.value(), error.getHttpStatusCode() );
        assertEquals( "invalid_token", error.getMessage() );
        assertEquals( "Invalid JWT serialization: Missing dot delimiter(s)",
            error.getDevMessage() );

        log.info( "s:" + error );
    }

    @Test
    public void testExpiredToken()
    {
        dhisJwtAuthenticationManagerResolver.setJwtDecoder( null );

        setupGoogleProvider( CORRECT_AUDIENCE_AKA_CLIENTID );

        JsonError error = GET( EXPIRED_GOOGLE_JWT_TOKEN, "/api/me" ).error();

        assertEquals( HttpStatus.UNAUTHORIZED.value(), error.getHttpStatusCode() );
        assertEquals( "invalid_token", error.getMessage() );
        assertEquals( "An error occurred while attempting to decode the Jwt: Jwt expired at 2021-03-05T16:19:50Z",
            error.getDevMessage() );

        log.info( "s:" + error );
    }

    @Test
    public void testMissingUser()
    {
        String providerURI = "testproviderone.com";
        setupTestingProvider( "client-1", "testproviderone", providerURI );
        String tokenValue = createJwt( providerURI ).getTokenValue();

        JsonError error = GET( tokenValue, "/api/me" ).error();
        assertEquals( HttpStatus.UNAUTHORIZED.value(), error.getHttpStatusCode() );
        assertEquals( "invalid_token", error.getMessage() );
        assertEquals( "Found no matching DHIS2 user for the mapping claim:'email' with the value:'null'",
            error.getDevMessage() );

        log.info( "s:" + error );
    }

    @Test
    public void testNoClientMatch()
    {
        String providerURI = "testprovidertwo.com";
        setupTestingProvider( "client-2", "testprovidertwo", providerURI );
        String tokenValue = createJwt( providerURI ).getTokenValue();

        JsonError error = GET( tokenValue, "/api/me" ).error();
        assertEquals( HttpStatus.UNAUTHORIZED.value(), error.getHttpStatusCode() );
        assertEquals( "invalid_token", error.getMessage() );
        assertEquals( "Invalid audience", error.getDevMessage() );

        log.info( "s:" + error );
    }

    private void setupGoogleProvider( String clientId )
    {
        Properties config = new Properties();
        config.put( ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_ID.getKey(), clientId );
        config.put( ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_SECRET.getKey(), "secret" );
        DhisOidcClientRegistration parse = GoogleProvider.parse( config );
        dhisOidcProviderRepository.addRegistration( parse );
    }

    private void setupTestingProvider( String clientId, String providerName, final String providerURI )
    {
        Properties config = new Properties();

        config.put( "oidc.provider." + providerName + ".client_id", clientId );
        config.put( "oidc.provider." + providerName + ".client_secret", "secret" );
        config.put( "oidc.provider." + providerName + ".issuer_uri", "https://" + providerURI );
        config.put( "oidc.provider." + providerName + ".authorization_uri", "https://" + providerURI + "/authorize" );
        config.put( "oidc.provider." + providerName + ".token_uri", "https://" + providerURI + "/token" );
        config.put( "oidc.provider." + providerName + ".user_info_uri", "https://" + providerURI + "/userinfo" );
        config.put( "oidc.provider." + providerName + ".jwk_uri", "https://" + providerURI + "/jwk" );

        GenericOidcProviderConfigParser.parse( config ).forEach( dhisOidcProviderRepository::addRegistration );

        Set<String> allRegistrationId = dhisOidcProviderRepository.getAllRegistrationId();

        log.info( " all ->" + allRegistrationId.size() );
    }

    @Override
    public WebClient.HttpResponse authWebRequest( String token, MockHttpServletRequestBuilder request )
    {
        return failOnException(
            () -> new WebClient.HttpResponse(
                mvc.perform( request.header( "Authorization", "Bearer " + token ) ).andReturn().getResponse() ) );
    }
}
