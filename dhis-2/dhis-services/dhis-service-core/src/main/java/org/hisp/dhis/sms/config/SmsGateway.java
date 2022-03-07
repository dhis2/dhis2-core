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
package org.hisp.dhis.sms.config;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Slf4j
public abstract class SmsGateway
{
    protected static final String PROTOCOL_VERSION = "X-Version";

    protected static final String MAX_MESSAGE_PART = "?maxMessageParts=4";

    protected static final String BASIC = " Basic ";

    public static final String KEY_TEXT = "text";

    public static final String KEY_RECIPIENT = "recipients";

    public static final Set<HttpStatus> OK_CODES = ImmutableSet.of( HttpStatus.OK,
        HttpStatus.ACCEPTED, HttpStatus.CREATED );

    private static final ImmutableMap<HttpStatus, GatewayResponse> GATEWAY_RESPONSE_MAP = new ImmutableMap.Builder<HttpStatus, GatewayResponse>()
        .put( HttpStatus.OK, GatewayResponse.RESULT_CODE_200 )
        .put( HttpStatus.ACCEPTED, GatewayResponse.RESULT_CODE_202 )
        .put( HttpStatus.CREATED, GatewayResponse.RESULT_CODE_202 )
        .put( HttpStatus.MULTI_STATUS, GatewayResponse.RESULT_CODE_207 )
        .put( HttpStatus.BAD_REQUEST, GatewayResponse.RESULT_CODE_400 )
        .put( HttpStatus.UNAUTHORIZED, GatewayResponse.RESULT_CODE_401 )
        .put( HttpStatus.PAYMENT_REQUIRED, GatewayResponse.RESULT_CODE_402 )
        .put( HttpStatus.NOT_FOUND, GatewayResponse.RESULT_CODE_404 )
        .put( HttpStatus.METHOD_NOT_ALLOWED, GatewayResponse.RESULT_CODE_405 )
        .put( HttpStatus.GONE, GatewayResponse.RESULT_CODE_410 )
        .put( HttpStatus.SERVICE_UNAVAILABLE, GatewayResponse.RESULT_CODE_503 )
        .put( HttpStatus.FORBIDDEN, GatewayResponse.RESULT_CODE_403 )
        .put( HttpStatus.INTERNAL_SERVER_ERROR, GatewayResponse.RESULT_CODE_504 ).build();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier( "tripleDesStringEncryptor" )
    private PBEStringEncryptor pbeStringEncryptor;

    protected abstract List<OutboundMessageResponse> sendBatch( OutboundMessageBatch batch,
        SmsGatewayConfig gatewayConfig );

    protected abstract boolean accept( SmsGatewayConfig gatewayConfig );

    protected abstract OutboundMessageResponse send( String subject, String text, Set<String> recipients,
        SmsGatewayConfig gatewayConfig );

    public HttpStatus send( String urlTemplate, HttpEntity<?> request, HttpMethod httpMethod, Class<?> klass )
    {
        ResponseEntity<?> response;
        HttpStatus statusCode;

        try
        {
            response = restTemplate.exchange( urlTemplate, httpMethod, request, klass );

            statusCode = response.getStatusCode();
        }
        catch ( HttpClientErrorException ex )
        {
            log.error( "Sms request client error", ex );

            statusCode = ex.getStatusCode();
        }
        catch ( HttpServerErrorException ex )
        {
            log.error( "Sms request server error", ex );

            statusCode = ex.getStatusCode();
        }
        catch ( Exception ex )
        {
            log.error( "Sms request error", ex );

            statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        log.info( "Sms request status code: " + statusCode );

        return statusCode;
    }

    public OutboundMessageResponse wrapHttpStatus( HttpStatus httpStatus )
    {
        GatewayResponse gatewayResponse;

        OutboundMessageResponse status = new OutboundMessageResponse();

        if ( OK_CODES.contains( httpStatus ) )
        {
            gatewayResponse = GATEWAY_RESPONSE_MAP.get( httpStatus );

            status.setOk( true );
        }
        else
        {
            gatewayResponse = GATEWAY_RESPONSE_MAP.getOrDefault( httpStatus, GatewayResponse.FAILED );

            status.setOk( false );
        }

        status.setResponseObject( gatewayResponse );
        status.setDescription( gatewayResponse.getResponseMessage() );

        return status;
    }

    protected HttpHeaders getAuthenticationHeaderParameters( SmsGatewayConfig config )
    {
        String credentials = config.getUsername().trim() + ":" +
            pbeStringEncryptor.decrypt( config.getPassword().trim() );
        String encodedCredentials = Base64.getEncoder().encodeToString( credentials.getBytes() );

        HttpHeaders headers = new HttpHeaders();
        headers.set( HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE );
        headers.set( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE );
        headers.set( HttpHeaders.AUTHORIZATION, BASIC + encodedCredentials );

        return headers;
    }
}
