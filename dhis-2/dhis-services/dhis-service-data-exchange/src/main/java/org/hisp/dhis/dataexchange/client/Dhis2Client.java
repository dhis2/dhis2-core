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
import java.text.SimpleDateFormat;
import java.util.Set;

import lombok.Getter;

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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Getter
public class Dhis2Client
{
    private final Set<HttpStatus> ERROR_STATUS_CODES = Set.of(
        UNAUTHORIZED, FORBIDDEN, NOT_FOUND );

    private final Dhis2Config config;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    public Dhis2Client( Dhis2Config config )
    {
        this.config = config;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        objectMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        objectMapper.setSerializationInclusion( Include.NON_NULL );
        objectMapper.setDateFormat( new SimpleDateFormat( "yyyy-MM-dd" ) );
    }

    /**
     * Handles error status codes.
     *
     * @param response the {@link ResponseEntity}.
     * @throws Dhis2ClientException
     */
    private void handleErrors( ResponseEntity<?> response )
    {
        final HttpStatus status = response.getStatusCode();

        if ( ERROR_STATUS_CODES.contains( status ) )
        {
            throw new Dhis2ClientException( status.getReasonPhrase(), status.value() );
        }
    }

    private <T> ResponseEntity<WebMessage> executeJsonPostRequest( URI uri, T body )
    {
        HttpEntity<T> requestEntity = new HttpEntity<>( body, getJsonAuthHeaders( config.getAccessToken() ) );

        ResponseEntity<WebMessage> response = restTemplate.exchange( uri, HttpMethod.POST, requestEntity,
            WebMessage.class );

        handleErrors( response );

        return response;
    }

    private MultiValueMap<String, String> getJsonAuthHeaders( String apiToken )
    {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE );
        headers.add( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE );
        headers.add( HttpHeaders.AUTHORIZATION, String.format( "ApiToken %s", apiToken ) );
        return headers;
    }

    public ImportSummary saveDataValueSet( DataValueSet dataValueSet )
    {
        URI uri = config.getResolvedUri( "/dataValueSets" );

        ResponseEntity<WebMessage> response = executeJsonPostRequest( uri, dataValueSet );

        return (ImportSummary) response.getBody().getResponse();
    }
}
