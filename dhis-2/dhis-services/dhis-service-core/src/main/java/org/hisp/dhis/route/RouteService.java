/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.route;

import static org.hisp.dhis.config.HibernateEncryptionConfig.AES_128_STRING_ENCRYPTOR;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.auth.ApiTokenAuth;
import org.hisp.dhis.common.auth.Auth;
import org.hisp.dhis.common.auth.HttpBasicAuth;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RouteService
{
    private final RouteStore routeStore;

    private final ObjectMapper objectMapper;

    @Qualifier( AES_128_STRING_ENCRYPTOR )
    private final PBEStringCleanablePasswordEncryptor encryptor;

    private static final RestTemplate restTemplate = new RestTemplate();

    static
    {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectionRequestTimeout( 1_000 );
        requestFactory.setConnectTimeout( 5_000 );
        requestFactory.setReadTimeout( 10_000 );
        requestFactory.setBufferRequestBody( true );

        restTemplate.setRequestFactory( requestFactory );
    }

    /**
     * Get {@see Route} by uid/code, decrypts its password/token and returns it.
     *
     * @param id uid/code
     * @return {@see Route}
     */
    public Route getDecryptedRoute( @Nonnull String id )
    {
        Route route = routeStore.getByUidNoAcl( id );

        if ( route == null )
        {
            route = routeStore.getByUidNoAcl( id );
        }

        if ( route == null || route.isDisabled() )
        {
            return null;
        }

        try
        {
            route = objectMapper.readValue( objectMapper.writeValueAsString( route ), Route.class );
        }
        catch ( JsonProcessingException ex )
        {
            log.error( "Unable to create clone of Route with ID " + route.getUid() + ". Please check it's data." );
            return null;
        }

        decrypt( route );

        return route;
    }

    public ResponseEntity<String> exec( Route route, HttpServletRequest request )
        throws IOException
    {
        HttpHeaders headers = new HttpHeaders();
        route.getHeaders().forEach( headers::add );

        if ( route.getAuth() != null )
        {
            route.getAuth().apply( headers );
        }

        HttpHeaders queryParameters = new HttpHeaders();
        request.getParameterMap().forEach( ( key, value ) -> queryParameters.addAll( key, List.of( value ) ) );

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl( route.getUrl() )
            .queryParams( queryParameters );

        String body = StreamUtils.copyToString( request.getInputStream(), StandardCharsets.UTF_8 );

        HttpEntity<String> entity = new HttpEntity<>( body, headers );

        HttpMethod httpMethod = Objects.requireNonNullElse( HttpMethod.resolve( request.getMethod() ), HttpMethod.GET );

        return restTemplate.exchange( uriComponentsBuilder.toUriString(), httpMethod,
            entity, String.class );
    }

    private void decrypt( Route route )
    {
        Auth auth = route.getAuth();

        if ( auth == null )
        {
            return;
        }

        if ( auth.getType().equals( ApiTokenAuth.TYPE ) )
        {
            ApiTokenAuth apiTokenAuth = (ApiTokenAuth) auth;
            apiTokenAuth.setToken( encryptor.decrypt( apiTokenAuth.getToken() ) );
        }
        else if ( auth.getType().equals( HttpBasicAuth.TYPE ) )
        {
            HttpBasicAuth httpBasicAuth = (HttpBasicAuth) auth;
            httpBasicAuth.setPassword( encryptor.decrypt( httpBasicAuth.getPassword() ) );
        }
    }
}
