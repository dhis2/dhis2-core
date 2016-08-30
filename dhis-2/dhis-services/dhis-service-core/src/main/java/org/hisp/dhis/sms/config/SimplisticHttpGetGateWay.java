package org.hisp.dhis.sms.config;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.util.IOUtils;
import org.hisp.dhis.sms.MessageResponseStatus;
import org.hisp.dhis.sms.OutBoundMessage;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.outbound.MessageBatch;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

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
 * <li>recipient
 * <li>message
 * <li>sender - if available in the message
 * </ul>
 * 
 * An example usage with bulksms.com would be this template:<br/>
 * http://bulksms.vsms.net:5567/eapi/submission/send_sms/2/2.0?username={
 * username
 * }&amp;password={password}&amp;message={message}&amp;msisdn={recipient}<br/>
 * With the following parameters provided:
 * <ul>
 * <li>username
 * <li>password
 * </ul>
 */
public class SimplisticHttpGetGateWay
    implements SmsGateway
{
    private static final Log log = LogFactory.getLog( SimplisticHttpGetGateWay.class );

    public static final ImmutableMap<Integer, GatewayResponse> SIMPLISTIC_GATEWAY_RESPONSE_MAP = new ImmutableMap.Builder<Integer, GatewayResponse>()
        .put( HttpURLConnection.HTTP_OK, GatewayResponse.RESULT_CODE_0 )
        .put( HttpURLConnection.HTTP_CREATED, GatewayResponse.RESULT_CODE_0 )
        .put( HttpURLConnection.HTTP_ACCEPTED, GatewayResponse.RESULT_CODE_0 )
        .put( HttpURLConnection.HTTP_CONFLICT, GatewayResponse.FAILED ).build();

    private static final Set<Integer> OK_CODES = ImmutableSet.of( HttpURLConnection.HTTP_OK,
        HttpURLConnection.HTTP_ACCEPTED, HttpURLConnection.HTTP_CREATED );

    @Override
    public List<MessageResponseStatus<GatewayResponse>> sendBatch( MessageBatch batch, SmsGatewayConfig gatewayConfig )
    {
        List<MessageResponseStatus<GatewayResponse>> statuses = new ArrayList<>();

        for ( OutBoundMessage message : batch.getBatch() )
        {
            statuses.add( send( message.getSubject(), message.getText(), message.getRecipients(), gatewayConfig ) );
        }

        return statuses;
    }

    @Override
    public boolean accept( SmsGatewayConfig gatewayConfig )
    {
        return gatewayConfig != null && gatewayConfig instanceof GenericHttpGatewayConfig;
    }

    @Override
    public MessageResponseStatus<GatewayResponse> send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        GenericHttpGatewayConfig genericHttpConfiguraiton = (GenericHttpGatewayConfig) config;

        MessageResponseStatus<GatewayResponse> status = new MessageResponseStatus<GatewayResponse>();

        UriComponentsBuilder uri = buildUrl( genericHttpConfiguraiton, text, recipients );

        BufferedReader reader = null;

        try
        {
            URL requestURL = new URL( uri.build().encode( "ISO-8859-1" ).toUriString() );

            log.info( "Requesting URL: " + uri.build().toString() );

            URLConnection conn = requestURL.openConnection();

            reader = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );

            HttpURLConnection httpConnection = (HttpURLConnection) conn;

            reader.close();

            if ( OK_CODES.contains( httpConnection.getResponseCode() ) )
            {

                GatewayResponse gatewayResponse = SIMPLISTIC_GATEWAY_RESPONSE_MAP
                    .get( httpConnection.getResponseCode() );

                status.setResponseObject( gatewayResponse );
                status.setResponseMessage( gatewayResponse.getResponseMessage() );
                status.setOk( true );

                return status;

            }
            else
            {
                status.setResponseObject( GatewayResponse.FAILED );
                status.setResponseMessage( GatewayResponse.FAILED.getResponseMessage() );
                status.setOk( false );

                return status;
            }
        }
        catch ( IOException e )
        {
            log.error( "Message failed: " + e.getMessage() );

            IOUtils.closeSilently( reader );

            status.setResponseObject( GatewayResponse.FAILED );
            status.setResponseMessage( GatewayResponse.FAILED.getResponseMessage() );
            status.setOk( false );

            return status;
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private UriComponentsBuilder buildUrl( GenericHttpGatewayConfig config, String text, Set<String> recipients )
    {
        UriComponentsBuilder uriBuilder = null;

        uriBuilder = UriComponentsBuilder.fromHttpUrl( config.getUrlTemplate() );

        uriBuilder = getUrlParameters( config.getParameters(), uriBuilder );

        uriBuilder.queryParam( config.getMessageParameter(), text );

        uriBuilder.queryParam( config.getRecipientParameter(),
            !recipients.isEmpty() ? recipients.iterator().next() : "" );

        return uriBuilder;
    }

    private UriComponentsBuilder getUrlParameters( List<GenericGatewayParameter> parameters,
        UriComponentsBuilder uriBuilder )
    {
        for ( GenericGatewayParameter parameter : parameters )
        {
            uriBuilder.queryParam( parameter.getKey(), parameter.getValue() );
        }

        return uriBuilder;
    }
}
