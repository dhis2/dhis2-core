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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.h2.util.IOUtils;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
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
    private static final String HTTP_POST = "POST";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";

    private static final ImmutableMap<Integer, GatewayResponse> SIMPLISTIC_GATEWAY_RESPONSE_MAP = new ImmutableMap.Builder<Integer, GatewayResponse>()
        .put( HttpURLConnection.HTTP_OK, GatewayResponse.RESULT_CODE_0 )
        .put( HttpURLConnection.HTTP_CREATED, GatewayResponse.RESULT_CODE_0 )
        .put( HttpURLConnection.HTTP_ACCEPTED, GatewayResponse.RESULT_CODE_0 )
        .put( HttpURLConnection.HTTP_CONFLICT, GatewayResponse.FAILED ).build();

    private static final ImmutableSet<Integer> OK_CODES = ImmutableSet.of( HttpURLConnection.HTTP_OK,
        HttpURLConnection.HTTP_ACCEPTED, HttpURLConnection.HTTP_CREATED );

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
    protected SmsGatewayConfig getGatewayType()
    {
        return new GenericHttpGatewayConfig();
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        GenericHttpGatewayConfig genericHttpConfiguration = (GenericHttpGatewayConfig) config;

        OutboundMessageResponse status = new OutboundMessageResponse();

        UriComponentsBuilder uri = buildUrl( genericHttpConfiguration );

        BufferedReader reader = null;
        OutputStreamWriter writer = null;

        try
        {
            URL requestURL = new URL( uri.build().encode().toUriString() );

            HttpURLConnection httpConnection = (HttpURLConnection) requestURL.openConnection();
            httpConnection .setRequestProperty( "Content-Type", CONTENT_TYPE );
            httpConnection.setRequestMethod( HTTP_POST );
            httpConnection.setDoOutput( true );

            httpConnection = getRequestHeaderParameters( httpConnection, genericHttpConfiguration.getParameters() );

            String data = getPostData( text, recipients, genericHttpConfiguration );

            writer = new OutputStreamWriter( httpConnection.getOutputStream() );

            writer.write( data );
            writer.flush();

            reader = new BufferedReader( new InputStreamReader( httpConnection.getInputStream() ) );

            Integer responseCode = httpConnection.getResponseCode();

            if ( OK_CODES.contains( responseCode ) )
            {
                status.setOk( true );
            }
            else
            {
                status.setOk( false );
            }

            GatewayResponse response = SIMPLISTIC_GATEWAY_RESPONSE_MAP.getOrDefault( responseCode, GatewayResponse.FAILED );

            status.setResponseObject( response );
            status.setDescription( response.getResponseMessage() );
        }
        catch ( IOException e )
        {
            DebugUtils.getStackTrace( e );

            setErrorStatus( status );
        }
        finally
        {
            IOUtils.closeSilently( reader );
            IOUtils.closeSilently( writer );
        }

        return status;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setErrorStatus( OutboundMessageResponse status )
    {
        status.setResponseObject( GatewayResponse.FAILED );
        status.setDescription( GatewayResponse.FAILED.getResponseMessage() );
        status.setOk( false );
    }

    private UriComponentsBuilder buildUrl( GenericHttpGatewayConfig config )
    {
        return UriComponentsBuilder.fromHttpUrl( config.getUrlTemplate() );
    }

    private Map<String, String> getUrlParameters( List<GenericGatewayParameter> parameters )
    {
        return parameters.stream().filter( p -> !p.isHeader() )
            .collect( Collectors.toMap( GenericGatewayParameter::getKey, GenericGatewayParameter::getValue ) ) ;
    }

    private HttpURLConnection getRequestHeaderParameters( HttpURLConnection urlConnection, List<GenericGatewayParameter> parameters )
    {
        parameters.stream().filter( GenericGatewayParameter::isHeader ).forEach( p -> urlConnection.setRequestProperty( p.getKey(), p.getValue() ) );

        return urlConnection;
    }

    private String getPostData( String text, Set<String> recipients, GenericHttpGatewayConfig config ) throws UnsupportedEncodingException
    {
        Map<String, String> parameters = getUrlParameters( config.getParameters() );
        parameters.put( config.getMessageParameter(), text );
        parameters.put( config.getRecipientParameter(), StringUtils.join( recipients, "," ) );

        StringBuilder sb = new StringBuilder();

        for( Map.Entry<String,String> entry : parameters.entrySet() )
        {
            sb.append( URLEncoder.encode( entry.getKey(), "UTF-8" ) )
                .append( "=" )
                .append( URLEncoder.encode( entry.getValue(), "UTF-8" ) ).append( "&" );
        }

        return sb.toString();
    }
}
