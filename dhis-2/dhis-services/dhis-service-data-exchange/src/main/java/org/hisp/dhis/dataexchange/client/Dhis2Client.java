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
import java.util.Set;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Getter
public class Dhis2Client
{
    private static final Set<HttpStatus> ERROR_STATUS_CODES = Set.of( UNAUTHORIZED, FORBIDDEN, NOT_FOUND );

    private final Dhis2Config config;

    private final RestTemplate restTemplate;

    public Dhis2Client( Dhis2Config config )
    {
        this.config = config;
        this.restTemplate = new RestTemplate();
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
     * @return a {@link MultiValueMap}.
     */
    private MultiValueMap<String, String> getJsonAuthHeaders()
    {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE );
        headers.add( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE );
        headers.add( HttpHeaders.AUTHORIZATION, config.getAuthHeaderValue() );

        log.info( "{}: {}", HttpHeaders.AUTHORIZATION,
            config.getAuthHeaderValue() ); // TODO remove

        return headers;
    }

    /**
     * Executes a HTTP POST request with the given body in JSON format.
     *
     * @param <T>
     * @param uri the request URI.
     * @param body the request body.
     * @return a {@link ResponseEntity}.
     */
    private <T> ResponseEntity<WebMessage> executeJsonPostRequest( URI uri, T body )
    {
        log.info( "URI: {}" + uri.toString() ); // TODO Remove

        HttpEntity<T> requestEntity = new HttpEntity<>( body, getJsonAuthHeaders() );
        ResponseEntity<WebMessage> response = restTemplate.exchange(
            uri, HttpMethod.POST, requestEntity, WebMessage.class );
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
     * Saves the given data value set.
     *
     * @param dataValueSet the {@link DataValueSet}.
     * @return an {@link ImportSummary}.
     */
    public ImportSummary saveDataValueSet( DataValueSet dataValueSet )
    {
        URI uri = config.getResolvedUri( "/dataValueSets" );
        WebMessage response = executeJsonPostRequest( uri, dataValueSet ).getBody();
        return response != null ? (ImportSummary) response.getResponse() : null;
    }
}
