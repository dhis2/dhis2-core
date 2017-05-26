package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public List<OutboundMessageResponse> sendBatch( OutboundMessageBatch batch, SmsGatewayConfig gatewayConfig )
    {
        return batch.getMessages()
          .stream()
          .map( m -> send( m.getSubject(), m.getText(), m.getRecipients(), gatewayConfig ) )
          .collect( Collectors.toList() );
     }

    @Override
    public boolean accept( SmsGatewayConfig gatewayConfig )
    {
        return gatewayConfig != null && gatewayConfig instanceof GenericHttpGatewayConfig;
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        GenericHttpGatewayConfig genericHttpConfiguration = (GenericHttpGatewayConfig) config;

        UriComponentsBuilder uri = buildUrl( genericHttpConfiguration, text, recipients );

        OutboundMessageResponse status = new OutboundMessageResponse();

        try
        {
            HttpEntity<?> request = new HttpEntity<>( getRequestHeaderParameters( genericHttpConfiguration.getParameters() ) );

            HttpStatus httpStatus = send( uri.build().encode( "ISO-8859-1" ).toUriString(), request, String.class );

            return wrapHttpStatus( httpStatus );
        }
        catch ( IOException e )
        {
            log.error( "Message failed: " + e.getMessage() );

            status.setResponseObject( GatewayResponse.RESULT_CODE_504 );
            status.setDescription( GatewayResponse.RESULT_CODE_504.getResponseMessage() );
            status.setOk( false );

            return status;
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private UriComponentsBuilder buildUrl( GenericHttpGatewayConfig config, String text, Set<String> recipients )
    {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl( config.getUrlTemplate() );
        uriBuilder = getUrlParameters( config.getParameters(), uriBuilder );
        uriBuilder.queryParam( config.getMessageParameter(), text );
        uriBuilder.queryParam( config.getRecipientParameter(),
            !recipients.isEmpty() ? recipients.iterator().next() : "" );

        return uriBuilder;
    }

    private UriComponentsBuilder getUrlParameters( List<GenericGatewayParameter> parameters,
        UriComponentsBuilder uriBuilder )
    {
        parameters.stream().filter( p -> !p.isHeader() ).forEach( p -> uriBuilder.queryParam( p.getKey(), p.getValue() ) );

        return uriBuilder;
    }

    private HttpHeaders getRequestHeaderParameters( List<GenericGatewayParameter> parameters )
    {
        HttpHeaders headers = new HttpHeaders();

        parameters.stream().filter( p -> p.isHeader() ).forEach( p -> headers.set( p.getKey(), p.getValue() ) );

        return headers;
    }
}
