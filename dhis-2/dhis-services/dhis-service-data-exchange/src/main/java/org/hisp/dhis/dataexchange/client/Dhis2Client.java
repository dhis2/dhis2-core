/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dataexchange.client;

import static java.lang.String.format;

import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.Validate;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dataexchange.client.auth.AccessTokenAuthentication;
import org.hisp.dhis.dataexchange.client.auth.Authentication;
import org.hisp.dhis.dataexchange.client.auth.BasicAuthentication;
import org.hisp.dhis.dataexchange.client.auth.CookieAuthentication;
import org.hisp.dhis.dataexchange.client.response.Dhis2Response;
import org.hisp.dhis.dataexchange.client.response.InternalImportSummaryResponse;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * API client for DHIS 2.
 *
 * @author Lars Helge Overland
 */
@Slf4j
public class Dhis2Client
{
    private final String url;

    private final Authentication authentication;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    /**
     * Main constructor.
     *
     * @param url the URL for the DHIS 2 instance, excluding the
     *        <code>/api</code> path.
     * @param authentication the authentication for the DHIS 2 instance.
     */
    private Dhis2Client( String url, Authentication authentication )
    {
        this.url = url;
        this.authentication = authentication;
        this.restTemplate = new RestTemplate();
        this.objectMapper = JacksonObjectMapperConfig.jsonMapper;
        Validate.notNull( url );
        Validate.notNull( authentication );
    }

    /**
     * Creates a new {@link Dhis2Client} configured with basic authentication.
     *
     * @param url url the URL for the DHIS 2 instance, excluding the
     *        <code>/api</code> path.
     * @param username the username for the DHIS 2 instance.
     * @param password the password for the DHIS 2 instance.
     * @return
     */
    public static Dhis2Client withBasicAuth( String url, String username, String password )
    {
        return new Dhis2Client( url, new BasicAuthentication( username, password ) );
    }

    /**
     * Creates a new {@link Dhis2Client} configured with personal access token
     * authentication.
     *
     * @param url url the URL for the DHIS 2 instance, excluding the
     *        <code>/api</code> path.
     * @param accessToken the personal access token for the DHIS 2 instance.
     * @return
     */
    public static Dhis2Client withAccessTokenAuth( String url, String accessToken )
    {
        return new Dhis2Client( url, new AccessTokenAuthentication( accessToken ) );
    }

    /**
     * Creates a new {@link Dhis2Client} configured with cookie authentication.
     *
     * @param url url the URL for the DHIS 2 instance, excluding the
     *        <code>/api</code> path.
     * @param accessToken the cookie session identifier for the DHIS 2 instance.
     * @return
     */
    public static Dhis2Client withCookieAuth( String url, String sessionId )
    {
        return new Dhis2Client( url, new CookieAuthentication( sessionId ) );
    }

    /**
     * Returns a {@link UriComponentsBuilder} which is resolved to the base API
     * URL of the DHIS 2 instance.
     *
     * @return a resolved {@link UriComponentsBuilder}.
     */
    UriComponentsBuilder getResolvedUriBuilder( String path )
    {
        return UriComponentsBuilder.fromHttpUrl( url )
            .pathSegment( "api" )
            .path( path );
    }

    /**
     * Returns a {@link HttpHeaders} for basic authentication indicating JSON
     * accept and content-type format.
     *
     * @return a {@link HttpHeaders}.
     */
    HttpHeaders getJsonAuthHeaders()
    {
        HttpHeaders headers = authentication.withAuthentication( new HttpHeaders() );
        headers.setAccept( List.of( MediaType.APPLICATION_JSON ) );
        headers.setContentType( MediaType.APPLICATION_JSON );
        return headers;
    }

    /**
     * Executes a HTTP POST request with the given body in JSON format.
     *
     * @param <T> the request body type.
     * @param <U> the response type.
     * @param uri the request URI.
     * @param body the request body.
     * @param type the response type.
     * @return a {@link ResponseEntity}.
     */
    private <T, U extends Dhis2Response> ResponseEntity<U> executeJsonPostRequest( URI uri, T body, Class<U> type )
    {
        HttpEntity<T> requestEntity = new HttpEntity<>( body, getJsonAuthHeaders() );

        try
        {
            return restTemplate.exchange( uri, HttpMethod.POST, requestEntity, type );
        }
        catch ( HttpClientErrorException ex )
        {
            if ( HttpStatus.CONFLICT == ex.getStatusCode() )
            {
                return getResponseEntity( deserialize( ex.getResponseBodyAsString(), type ), ex );
            }

            throw ex;
        }
    }

    /**
     * Returns the base URL to the target DHIS 2 instance.
     *
     * @return the base URL.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Saves the given data value set using the given import options.
     *
     * @param dataValueSet the {@link DataValueSet}.
     * @param options the {@link ImportOptions}.
     * @return an {@link ImportSummary}.
     */
    public ImportSummary saveDataValueSet( DataValueSet dataValueSet, ImportOptions options )
    {
        URI uri = getDataValueSetUri( options );
        InternalImportSummaryResponse response = executeJsonPostRequest(
            uri, dataValueSet, InternalImportSummaryResponse.class ).getBody();
        return response != null ? response.getImportSummary() : null;
    }

    /**
     * Returns a {@link URI} for the <code>dataValueSets</code> API based on the
     * given {@link ImportOptions}. Specified identifier schemes which equals
     * the default identifier scheme are omitted.
     *
     * @param options the {@link ImportOptions}.
     * @return a {@link URI}.
     */
    URI getDataValueSetUri( ImportOptions options )
    {
        UriComponentsBuilder builder = getResolvedUriBuilder( "dataValueSets" );

        IdSchemes idSchemes = options.getIdSchemes();

        addIfNotDefault( builder, "dataElementIdScheme", idSchemes.getDataElementIdScheme() );
        addIfNotDefault( builder, "orgUnitIdScheme", idSchemes.getOrgUnitIdScheme() );
        addIfNotDefault( builder, "categoryOptionComboIdScheme", idSchemes.getCategoryOptionComboIdScheme() );
        addIfNotDefault( builder, "idScheme", idSchemes.getIdScheme() );

        return builder.build().toUri();
    }

    /**
     * Adds the given identifier scheme to the URI builder unless it equals the
     * default identifier scheme.
     *
     * @param builder the {@link UriComponentsBuilder}.
     * @param queryParam the query parameter name.
     * @param idScheme the {@link IdScheme}.
     */
    void addIfNotDefault( UriComponentsBuilder builder, String queryParam, IdScheme idScheme )
    {
        if ( idScheme != null && idScheme != IdSchemes.DEFAULT_ID_SCHEME )
        {
            builder.queryParam( queryParam, idScheme.name() );
        }
    }

    /**
     * Returns a {@link ResponseEntity} for the given body and
     * {@link HttpClientErrorException}.
     *
     * @param <T> the type.
     * @param body the body.
     * @param ex the {@link HttpClientErrorException}.
     * @return the {@link ResponseEntity}.
     */
    private <T extends Dhis2Response> ResponseEntity<T> getResponseEntity( T body, HttpClientErrorException ex )
    {
        return new ResponseEntity<>( body, ex.getResponseHeaders(), ex.getStatusCode() );
    }

    /**
     * Deserializes the given JSON value to the given Java type.
     *
     * @param <T> the type.
     * @param content the JSON value.
     * @param type the type.
     * @return the deserialized object.
     */
    <T> T deserialize( String content, Class<T> type )
    {
        try
        {
            return objectMapper.readValue( content, type );
        }
        catch ( JsonProcessingException ex )
        {
            String message = format( "Failed to read JSON value: %s", ex.getMessage() );

            log.error( message, ex );

            throw new UncheckedIOException( message, ex );
        }
    }
}
