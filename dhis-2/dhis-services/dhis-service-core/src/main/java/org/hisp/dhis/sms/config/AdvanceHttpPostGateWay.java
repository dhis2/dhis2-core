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
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.smslib.AGateway;
import org.smslib.GatewayException;
import org.smslib.OutboundMessage;
import org.smslib.TimeoutException;

public class AdvanceHttpPostGateWay
    extends AGateway
{
    private static final Log log = LogFactory.getLog( AdvanceHttpPostGateWay.class );

    private static final String SENDER = "sender";

    private static final String RECIPIENT = "recipient";

    private static final String MESSAGE = "message";

    private Map<String, String> parameters;

    private String urlTemplate;

    public AdvanceHttpPostGateWay( String id, String urlTemplate, Map<String, String> parameters )
    {
        super( id );
        this.urlTemplate = urlTemplate;
        this.parameters = parameters;
    }

    @Override
    public int sendMessages( Collection<OutboundMessage> outboundMessages )
        throws TimeoutException, GatewayException, IOException, InterruptedException
    {
        Map<String, String> requestParameters = new HashMap<>( parameters );

        for ( OutboundMessage outboundMessage : outboundMessages )
        {
            requestParameters.put( RECIPIENT, outboundMessage.getRecipient() );
            requestParameters.put( MESSAGE, outboundMessage.getText() );

            String sender = outboundMessage.getFrom();

            if ( sender != null )
            {
                log.debug( "Adding sender " + sender + " " + getGatewayId() );
                requestParameters.put( SENDER, sender );
            }

            String urlString = urlTemplate;

            for ( String key : requestParameters.keySet() )
            {
                if ( requestParameters.get( key ) != null )
                {
                    urlString = StringUtils.replace( urlString, "{" + key + "}",
                        URLEncoder.encode( requestParameters.get( key ), "UTF-8" ) );
                }
            }

            log.info( "RequestURL: " + urlString + " " + getGatewayId() );

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
                    log.warn( "Couldn't send message, got response " + response + " " + getGatewayId() );
                    return 0;
                }
            }
            catch ( IOException ex )
            {
                log.warn( "Couldn't send message " + outboundMessage + " " + getGatewayId() );
                return 0;
            }
            finally
            {
                reader.close();
            }

            return 1;
        }

        return super.sendMessages( outboundMessages );
    }

    @Override
    public int getQueueSchedulingInterval()
    {
        return 0;
    }
}
