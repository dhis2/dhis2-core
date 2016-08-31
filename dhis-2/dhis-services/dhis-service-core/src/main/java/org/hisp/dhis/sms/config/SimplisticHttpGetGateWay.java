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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.SmsGateway;

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

    private static final String RECIPIENT = "recipient";

    private static final String MESSAGE = "message";

    public SimplisticHttpGetGateWay()
    {
    }

    @Override
    public boolean accept( SmsGatewayConfig gatewayConfig )
    {
        return gatewayConfig != null && gatewayConfig instanceof GenericHttpGatewayConfig;
    }

    @Override
    public GatewayResponse send( OutboundSms msg, SmsGatewayConfig gatewayConfig )
    {
        GenericHttpGatewayConfig genericGatewayConfig = (GenericHttpGatewayConfig) gatewayConfig;

        Map<String, String> requestParameters = new HashMap<>( genericGatewayConfig.getParameters() );
        requestParameters.put( RECIPIENT, msg.getRecipients().iterator().next() );
        requestParameters.put( MESSAGE, msg.getMessage() );

        String urlString = genericGatewayConfig.getUrlTemplate();

        for ( String key : requestParameters.keySet() )
        {
            if ( requestParameters.get( key ) != null )
            {
                try
                {
                    urlString = StringUtils.replace( urlString, "{" + key + "}",
                        URLEncoder.encode( requestParameters.get( key ), "UTF-8" ) );
                }
                catch ( UnsupportedEncodingException e )
                {
                    e.printStackTrace();
                }
            }
        }

        log.info( "RequestURL: " + urlString + " " + genericGatewayConfig.getName() );

        String line, response = "";
        BufferedReader reader = null;

        try
        {
            URL requestURL = new URL( urlString );
            URLConnection conn = requestURL.openConnection();
            reader = new BufferedReader( new InputStreamReader( conn.getInputStream() ) );

            while ( (line = reader.readLine()) != null )
            {
                response += line;
            }

            HttpURLConnection httpConnection = (HttpURLConnection) conn;

            if ( httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK
                && httpConnection.getResponseCode() != HttpURLConnection.HTTP_ACCEPTED
                && httpConnection.getResponseCode() != HttpURLConnection.HTTP_CREATED )
            {
                log.warn( "Couldn't send message, got response " + response + " " + genericGatewayConfig.getName() );

                return GatewayResponse.FAILED;
            }

            if ( reader != null )
            {
                reader.close();
            }

            return GatewayResponse.RESULT_CODE_0;
        }
        catch ( IOException e )
        {
            log.warn( "Couldn't send message " + genericGatewayConfig.getName() );
            
            return GatewayResponse.FAILED;
        }
    }

    @Override
    public GatewayResponse send( List<OutboundSms> sms, SmsGatewayConfig gatewayConfig )
    {
        return null;
    }
}
