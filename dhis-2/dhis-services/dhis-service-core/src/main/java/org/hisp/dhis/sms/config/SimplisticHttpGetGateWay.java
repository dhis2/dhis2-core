package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Simplistic http gateway sending smses through a get to a url constructed from
 * the provided urlTemplate and map of static parameters.
 * <p>
 * This gateway is simplistic in that it can't evaluate the response from the
 * provider, being most suitable as an example gateway. For production use a
 * more robust gateway should be used implemented for the specific provider.
 *
 * <p>
 * The gateway adds the following keys to the parameters:
 * <ul>
 * <li>recipient</li>
 * <li>message</li>
 * <li>sender - if available in the message</li>
 * </ul>
 *
 * An example usage with bulksms.com would be this template:<br/>
 * http://bulksms.vsms.net:5567/eapi/submission/send_sms/2/2.0?username={
 * username
 * }&amp;password={password}&amp;message={message}&amp;msisdn={recipient}<br/>
 * With the following parameters provided:
 * <ul>
 * <li>username</li>
 * <li>password</li>
 * </ul>
 */
public class SimplisticHttpGetGateWay
    extends SmsGateway
{
    private static final Log log = LogFactory.getLog( SimplisticHttpGetGateWay.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private RestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public List<OutboundMessageResponse> sendBatch( OutboundMessageBatch batch, SmsGatewayConfig gatewayConfig )
    {
        return batch.getMessages()
            .parallelStream()
            .map( m -> send( m.getSubject(), m.getText(), m.getRecipients(), gatewayConfig ) )
            .collect( Collectors.toList() );
    }

    @Override
    protected SmsGatewayConfig getGatewayConfigType()
    {
        return new GenericHttpGatewayConfig();
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        GenericHttpGatewayConfig genericConfig = (GenericHttpGatewayConfig) config;

        UriComponentsBuilder uriBuilder = buildUrl( genericConfig );
        uriBuilder.queryParam( genericConfig.getMessageParameter(), text );
        uriBuilder.queryParam( genericConfig.getRecipientParameter(), StringUtils.join( recipients, "," ) );

        ResponseEntity<String> responseEntity = null;

        try
        {
            URI url = uriBuilder.build().encode().toUri();

            responseEntity = restTemplate.exchange( url, genericConfig.isUseGet() ? HttpMethod.GET : HttpMethod.POST, null, String.class );
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

    private UriComponentsBuilder buildUrl( GenericHttpGatewayConfig config )
    {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl( config.getUrlTemplate() );

        for ( Map.Entry<String, String> entry : getUrlParameters( config.getParameters() ).entrySet() )
        {
            uriBuilder.queryParam( entry.getKey().toString(), entry.getValue().toString() );
        }

        return uriBuilder;
    }

    private Map<String, String> getUrlParameters( List<GenericGatewayParameter> parameters )
    {
        return parameters.stream().filter( p -> !p.isHeader() )
                .collect( Collectors.toMap( GenericGatewayParameter::getKey, GenericGatewayParameter::getValueForKey ) ) ;
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

        return wrapHttpStatus( responseEntity.getStatusCode() );
    }
}
