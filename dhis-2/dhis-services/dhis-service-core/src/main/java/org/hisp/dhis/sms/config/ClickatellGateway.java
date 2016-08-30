package org.hisp.dhis.sms.config;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.sms.MessageResponseStatus;
import org.hisp.dhis.sms.outbound.ClickatellRequestEntity;
import org.hisp.dhis.sms.outbound.ClickatellResponseEntity;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.outbound.MessageBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public class ClickatellGateway
    implements SmsGateway
{
    private static final Log log = LogFactory.getLog( ClickatellGateway.class );

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String ACCEPT = "Accept";

    private static final String AUTHORIZATION = "Authorization";

    private static final String PROTOCOL_VERSION = "X-Version";

    private static final String maxMessageParts = "?maxMessageParts=4";

    private static final ImmutableMap<HttpStatus, GatewayResponse> CLICKATELL_GATEWAY_RESPONSE_MAP = new ImmutableMap.Builder<HttpStatus, GatewayResponse>()
        .put( HttpStatus.OK, GatewayResponse.RESULT_CODE_200 )
        .put( HttpStatus.ACCEPTED, GatewayResponse.RESULT_CODE_202 )
        .put( HttpStatus.MULTI_STATUS, GatewayResponse.RESULT_CODE_207 )
        .put( HttpStatus.BAD_REQUEST, GatewayResponse.RESULT_CODE_400 )
        .put( HttpStatus.UNAUTHORIZED, GatewayResponse.RESULT_CODE_401 )
        .put( HttpStatus.PAYMENT_REQUIRED, GatewayResponse.RESULT_CODE_402 )
        .put( HttpStatus.NOT_FOUND, GatewayResponse.RESULT_CODE_404 )
        .put( HttpStatus.METHOD_NOT_ALLOWED, GatewayResponse.RESULT_CODE_405 )
        .put( HttpStatus.GONE, GatewayResponse.RESULT_CODE_410 )
        .put( HttpStatus.SERVICE_UNAVAILABLE, GatewayResponse.RESULT_CODE_503 ).build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private RestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean accept( SmsGatewayConfig gatewayConfig )
    {
        return gatewayConfig != null && gatewayConfig instanceof ClickatellGatewayConfig;
    }

    public List<MessageResponseStatus<GatewayResponse>> sendBatch( MessageBatch batch, SmsGatewayConfig config )
    {
        return null;
    }

    @Override
    public MessageResponseStatus<GatewayResponse> send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        ClickatellGatewayConfig clickatellConfiguration = (ClickatellGatewayConfig) config;
        HttpEntity<ClickatellRequestEntity> request =
            new HttpEntity<>( getRequestBody( text, recipients ), getRequestHeaderParameters( clickatellConfiguration ) );

        return handleResponse( send( clickatellConfiguration.getUrlTemplate(), request ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private HttpStatus send( String urlTemplate, HttpEntity<?> request )
    {
        ResponseEntity<ClickatellResponseEntity> response;
        HttpStatus statusCode = null;

        try
        {
            response = restTemplate.exchange( urlTemplate + maxMessageParts, HttpMethod.POST, request,
                ClickatellResponseEntity.class );

            statusCode = response.getStatusCode();
        }
        catch ( HttpClientErrorException ex )
        {
            log.error( "Client error", ex );

            statusCode = ex.getStatusCode();
        }
        catch ( HttpServerErrorException ex )
        {
            log.error( "Server error", ex );

            statusCode = ex.getStatusCode();
        }
        catch ( Exception ex )
        {
            log.error( "Error", ex );
        }

        log.info( "Response status code: " + statusCode );

        return statusCode;
    }

    private MessageResponseStatus<GatewayResponse> handleResponse( HttpStatus httpStatus )
    {
        MessageResponseStatus<GatewayResponse> status = new MessageResponseStatus<>();
        status.setResponseObject( CLICKATELL_GATEWAY_RESPONSE_MAP.get( httpStatus ) );

        return status;
    }

    private ClickatellRequestEntity getRequestBody( String text, Set<String> recipients )
    {
        ClickatellRequestEntity requestBody = new ClickatellRequestEntity();
        requestBody.setText( text );
        requestBody.setTo( recipients );

        return requestBody;
    }

    private HttpHeaders getRequestHeaderParameters( ClickatellGatewayConfig clickatellConfiguration )
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set( CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE );
        headers.set( ACCEPT, MediaType.APPLICATION_JSON_VALUE );
        headers.set( PROTOCOL_VERSION, "1" );
        headers.set( AUTHORIZATION, clickatellConfiguration.getAuthToken() );

        return headers;
    }
}
