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

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.net.URI;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.Validate;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dataexchange.client.response.Dhis2Response;
import org.hisp.dhis.dataexchange.client.response.ImportSummaryResponse;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for DHIS 2.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Getter
public class Dhis2Client
{
    private static final Set<HttpStatus> ERROR_STATUS_CODES = Set.of( UNAUTHORIZED, FORBIDDEN, NOT_FOUND );

    private final Dhis2Config config;

    private final RestTemplate restTemplate;

    /**
     * Main constructor.
     *
     * @param config the {@link Dhis2Config}.
     */
    public Dhis2Client( Dhis2Config config )
    {
        this.config = config;
        this.restTemplate = new RestTemplate();
        Validate.notNull( config );
    }

    /**
     * Handles response errors.
     *
     * @param response the {@link ResponseEntity}.
     * @throws Dhis2ClientException
     */
    private void handleErrors( ResponseEntity<?> response )
    {
        log.info( "Response status code: {}", response.getStatusCode() );

        if ( ERROR_STATUS_CODES.contains( response.getStatusCode() ) || response.getStatusCode().is5xxServerError() )
        {
            log.warn( "Errror status code: {}, reason phrase: {}",
                response.getStatusCode().value(), response.getStatusCode().getReasonPhrase() );

            throw new Dhis2ClientException( response.getStatusCode() );
        }
    }

    /**
     * Returns a map of HTTP headers indicating JSON format and DHIS 2 access
     * token-based authorization.
     *
     * @return a {@link HttpHeaders}.
     */
    private HttpHeaders getJsonAuthHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept( List.of( MediaType.APPLICATION_JSON ) );
        headers.setContentType( MediaType.APPLICATION_JSON );
        headers.setBasicAuth( config.getUsername(), config.getPassword() );
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
        ResponseEntity<U> response = restTemplate.exchange( uri, HttpMethod.POST, requestEntity, type );
        handleErrors( response );
        return response;
    }

    /**
     * Returns the base URL to the target DHIS 2 instance.
     *
     * @return the base URL.
     */
    public String getUrl()
    {
        return config.getUrl();
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
        ImportSummaryResponse response = executeJsonPostRequest(
            uri, dataValueSet, ImportSummaryResponse.class ).getBody();
        return response != null ? response.getResponse() : null;
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
        UriComponentsBuilder builder = config.getResolvedUriBUilder( "dataValueSets" );

        IdSchemes idSchemes = options.getIdSchemes();

        addIfNotDefault( builder, "dataElementIdScheme", idSchemes.getDataElementIdScheme() );
        addIfNotDefault( builder, "orgUnitIdScheme", idSchemes.getOrgUnitIdScheme() );
        addIfNotDefault( builder, "categoryOptionComboIdScheme", idSchemes.getCategoryOptionComboIdScheme() );
        addIfNotDefault( builder, "idScheme", idSchemes.getIdScheme() );

        return builder.build().toUri();
    }

    /**
     * Adds the given identifier scheme to the URI builder unless it equal the
     * default identifier scheme.
     *
     * @param builder the {@link UriComponentsBuilder}.
     * @param queryParam the query parameter name.
     * @param idScheme the {@link IdScheme}.
     */
    void addIfNotDefault( UriComponentsBuilder builder, String queryParam, IdScheme idScheme )
    {
        if ( idScheme != IdSchemes.DEFAULT_ID_SCHEME && idScheme != null )
        {
            builder.queryParam( queryParam, idScheme.name().toLowerCase() );
        }
    }
}
