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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hisp.dhis.common.auth.ApiTokenAuth;
import org.hisp.dhis.common.auth.Auth;
import org.hisp.dhis.common.auth.HttpBasicAuth;
import org.hisp.dhis.user.User;
import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
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

    /*
     * Sensitive headers to be removed from forwarded requests. TODO: make this
     * configurable
     */
    private static final List<String> sensitiveHeaderNames = List.of( "cookie", "authorization" );

    /*
     * Non-sensitive headers to be removed from forwarded requests. From IETF
     * (https://datatracker.ietf.org/doc/html/draft-ietf-httpbis-p1-messaging-
     * 14) See also Spring Cloud Gateway
     * (https://cloud.spring.io/spring-cloud-gateway/reference/html/#
     * removehopbyhop-headers-filter)
     */
    private static final List<String> removeHopByHopHeaderNames = List.of( "connection", "keep-alive",
        "proxy-authenticate", "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade" );

    static
    {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectionRequestTimeout( 1_000 );
        requestFactory.setConnectTimeout( 5_000 );
        requestFactory.setReadTimeout( 10_000 );
        requestFactory.setBufferRequestBody( true );

        HttpClient httpClient = HttpClientBuilder.create()
            .disableCookieManagement()
            .useSystemProperties()
            .build();
        requestFactory.setHttpClient( httpClient );

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
            route = routeStore.getByCodeNoAcl( id );
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
            log.error( "Unable to create clone of Route with ID " + route.getUid() + ". Please check its data." );
            return null;
        }

        decrypt( route );

        return route;
    }

    public ResponseEntity<String> exec( Route route, User user, Optional<String> subPath, HttpServletRequest request )
        throws IOException,
        IllegalArgumentException
    {
        HttpHeaders headers = forwardHeaders( request );

        route.getHeaders().forEach( headers::add );

        if ( user != null && StringUtils.hasText( user.getUsername() ) )
        {
            headers.add( "X-Forwarded-User", user.getUsername() );
        }

        if ( route.getAuth() != null )
        {
            route.getAuth().apply( headers );
        }

        HttpHeaders queryParameters = new HttpHeaders();
        request.getParameterMap().forEach( ( key, value ) -> queryParameters.addAll( key, List.of( value ) ) );

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl( route.getBaseUrl() )
            .queryParams( queryParameters );

        if ( subPath.isPresent() )
        {
            if ( !route.allowsSubpaths() )
            {
                throw new IllegalArgumentException(
                    String.format( "Route %s does not allow subpaths", route.getId() ) );
            }
            uriComponentsBuilder.path( subPath.get() );
        }

        String body = StreamUtils.copyToString( request.getInputStream(), StandardCharsets.UTF_8 );
        HttpEntity<String> entity = new HttpEntity<>( body, headers );
        HttpMethod httpMethod = Objects.requireNonNullElse( HttpMethod.resolve( request.getMethod() ), HttpMethod.GET );
        String targetUri = uriComponentsBuilder.toUriString();

        log.info( String.format( "Sending %s %s via route %s (%s)", httpMethod, targetUri, route.getName(),
            route.getUid() ) );

        ResponseEntity<String> response = restTemplate.exchange( targetUri, httpMethod,
            entity, String.class );

        log.info( String.format( "Request %s %s responded with HTTP status %s via route %s (%s)", httpMethod, targetUri,
            response.getStatusCode().toString(), route.getName(), route.getUid() ) );

        return response;
    }

    private HttpHeaders forwardHeaders( HttpServletRequest request )
    {
        HttpHeaders headers = new HttpHeaders();
        Collections.list( request.getHeaderNames() ).forEach( name -> {
            String lowercaseName = name.toLowerCase();
            if ( sensitiveHeaderNames.contains( lowercaseName ) || removeHopByHopHeaderNames.contains( lowercaseName )
                || lowercaseName.equals( "host" ) )
            {
                log.info( String.format( "Blocked header %s", name,
                    Collections.list( request.getHeaders( name ) ).toString() ) );
                return;
            }
            headers.addAll( name, Collections.list( request.getHeaders( name ) ) );
        } );

        headers.forEach( ( String name, List<String> values ) -> log
            .info( String.format( "Forwarded header %s=%s", name, values.toString() ) ) );

        return headers;
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
