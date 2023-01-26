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

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.system.util.SmsUtils;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Component( "org.hisp.dhis.sms.config.SimplisticHttpGetGateWay" )
public class SimplisticHttpGetGateWay
    extends SmsGateway
{
    private final RestTemplate restTemplate;

    @Qualifier( "tripleDesStringEncryptor" )
    private final PBEStringEncryptor pbeStringEncryptor;

    // -------------------------------------------------------------------------
    // SmsGateway implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean accept( SmsGatewayConfig gatewayConfig )
    {
        return gatewayConfig instanceof GenericHttpGatewayConfig;
    }

    @Override
    public List<OutboundMessageResponse> sendBatch( OutboundMessageBatch batch, SmsGatewayConfig gatewayConfig )
    {
        return batch.getMessages()
            .parallelStream()
            .map( m -> send( m.getSubject(), m.getText(), m.getRecipients(), gatewayConfig ) )
            .collect( Collectors.toList() );
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        GenericHttpGatewayConfig genericConfig = (GenericHttpGatewayConfig) config;

        UriComponentsBuilder uriBuilder;

        ResponseEntity<String> responseEntity = null;

        HttpEntity<String> requestEntity;

        URI uri;

        try
        {
            requestEntity = getRequestEntity( genericConfig, text, recipients );

            if ( genericConfig.isSendUrlParameters() )
            {
                uriBuilder = UriComponentsBuilder
                    .fromHttpUrl( config.getUrlTemplate() + "?" + requestEntity.getBody() );
            }
            else
            {
                uriBuilder = UriComponentsBuilder.fromHttpUrl( config.getUrlTemplate() );
            }

            uri = uriBuilder.build().encode().toUri();

            responseEntity = restTemplate.exchange( uri, genericConfig.isUseGet() ? HttpMethod.GET : HttpMethod.POST,
                requestEntity, String.class );
        }
        catch ( HttpClientErrorException ex )
        {
            log.error( "Client error " + ex.getMessage() );
        }
        catch ( HttpServerErrorException ex )
        {
            log.error( "Server error " + ex.getMessage() );
        }
        catch ( Exception ex )
        {
            log.error( "Error " + ex.getMessage() );
        }

        return getResponse( responseEntity );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private HttpEntity<String> getRequestEntity( GenericHttpGatewayConfig config, String text, Set<String> recipients )
    {
        final StringSubstitutor substitutor = new StringSubstitutor( getRequestData( config, text, recipients ) ); // Matches
                                                                                                                  // on
                                                                                                                  // ${...}

        String data = substitutor.replace( config.getConfigurationTemplate() );

        return new HttpEntity<>( data, getRequestHeaderParameters( config ) );
    }

    private Map<String, String> getRequestData( GenericHttpGatewayConfig config, String text, Set<String> recipients )
    {
        List<GenericGatewayParameter> parameters = config.getParameters();

        Map<String, String> valueStore = new HashMap<>();

        for ( GenericGatewayParameter parameter : parameters )
        {
            if ( !parameter.isHeader() )
            {
                valueStore.put( parameter.getKey(), encodeAndDecryptParameter( parameter ) );
            }
        }

        valueStore.put( KEY_TEXT, SmsUtils.encode( text ) );
        valueStore.put( KEY_RECIPIENT, StringUtils.join( recipients, "," ) );

        return valueStore;
    }

    private HttpHeaders getRequestHeaderParameters( GenericHttpGatewayConfig config )
    {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put( "Content-type", Collections.singletonList( config.getContentType().getValue() ) );

        for ( GenericGatewayParameter parameter : config.getParameters() )
        {
            if ( parameter.isHeader() )
            {
                if ( parameter.getKey().equals( HttpHeaders.AUTHORIZATION ) )
                {
                    httpHeaders.add( parameter.getKey(), BASIC + encodeAndDecryptParameter( parameter ) );
                }
                else
                {
                    httpHeaders.add( parameter.getKey(), encodeAndDecryptParameter( parameter ) );
                }
            }
        }

        return httpHeaders;
    }

    private String encodeAndDecryptParameter( GenericGatewayParameter parameter )
    {
        String value = parameter.isConfidential() ? pbeStringEncryptor.decrypt( parameter.getValue() )
            : parameter.getValue();

        return parameter.isEncode() ? Base64.getEncoder().encodeToString( value.getBytes() ) : value;
    }

    private OutboundMessageResponse getResponse( ResponseEntity<String> responseEntity )
    {
        OutboundMessageResponse status = new OutboundMessageResponse();

        if ( responseEntity == null || !OK_CODES.contains( responseEntity.getStatusCode() ) )
        {
            status.setResponseObject( GatewayResponse.FAILED );
            status.setOk( false );

            return status;
        }

        log.info( responseEntity.getBody() );
        return wrapHttpStatus( responseEntity.getStatusCode() );
    }
}