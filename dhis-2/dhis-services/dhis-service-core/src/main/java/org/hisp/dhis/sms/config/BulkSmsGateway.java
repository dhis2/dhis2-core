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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.sms.outbound.SubmissionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

public class BulkSmsGateway
    extends SmsGateway
{
    private static final Log log = LogFactory.getLog( BulkSmsGateway.class );

    private static final int MIN = 1;

    private static final int MAX = 2147483647;

    public static final ImmutableMap<String, GatewayResponse> BULKSMS_GATEWAY_RESPONSE_MAP = new ImmutableMap.Builder<String, GatewayResponse>()
        .put( "0", GatewayResponse.RESULT_CODE_0 ).put( "1", GatewayResponse.RESULT_CODE_1 )
        .put( "22", GatewayResponse.RESULT_CODE_22 ).put( "23", GatewayResponse.RESULT_CODE_23 )
        .put( "24", GatewayResponse.RESULT_CODE_24 ).put( "25", GatewayResponse.RESULT_CODE_25 )
        .put( "26", GatewayResponse.RESULT_CODE_26 ).put( "27", GatewayResponse.RESULT_CODE_27 )
        .put( "40", GatewayResponse.RESULT_CODE_40 ).build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private RestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public List<OutboundMessageResponse> sendBatch( OutboundMessageBatch smsBatch, SmsGatewayConfig config )
    {
        BulkSmsGatewayConfig bulkSmsConfig = (BulkSmsGatewayConfig) config;

        UriComponentsBuilder uriBuilder = buildBaseUrl( bulkSmsConfig, SubmissionType.BATCH );
        uriBuilder.queryParam( "batch_data", buildCsvUrl( smsBatch.getMessages() ) );

        return Lists.newArrayList( send( uriBuilder ) );
    }

    @Override
    public boolean accept( SmsGatewayConfig gatewayConfig )
    {
        return gatewayConfig != null && gatewayConfig instanceof BulkSmsGatewayConfig;
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        UriComponentsBuilder uriBuilder = createUri( (BulkSmsGatewayConfig) config, recipients, SubmissionType.SINGLE );
        uriBuilder.queryParam( "message", text );

        return send( uriBuilder );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private UriComponentsBuilder createUri( BulkSmsGatewayConfig bulkSmsConfig, Set<String> recipients,
        SubmissionType type )
    {
        UriComponentsBuilder uriBuilder = buildBaseUrl( bulkSmsConfig, type );
        uriBuilder.queryParam( "msisdn", getRecipients( recipients ) );

        return uriBuilder;
    }

    private OutboundMessageResponse send( UriComponentsBuilder uriBuilder )
    {
        ResponseEntity<String> responseEntity = null;

        try
        {
            URI url = uriBuilder.build().encode( "ISO-8859-1" ).toUri();

            responseEntity = restTemplate.exchange( url, HttpMethod.POST, null, String.class );
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

    private String buildCsvUrl( List<OutboundMessage> smsBatch )
    {
        String csvData = "msisdn,message\n";

        for ( OutboundMessage sms : smsBatch )
        {
            csvData += getRecipients( sms.getRecipients() );
            csvData += "," + sms.getText() + "\n";
        }

        return csvData;
    }

    private UriComponentsBuilder buildBaseUrl( BulkSmsGatewayConfig bulkSmsConfiguration, SubmissionType type )
    {
        Random r = new Random();

        int stopDuplicationID = r.nextInt( (MAX - MIN) + 1 ) + MIN;

        UriComponentsBuilder uriBuilder = null;

        if ( type.equals( SubmissionType.SINGLE ) )
        {
            uriBuilder = UriComponentsBuilder.fromHttpUrl( bulkSmsConfiguration.getUrlTemplate() );
        }
        else // SubmissionType.BATCH
        {
            uriBuilder = UriComponentsBuilder.fromHttpUrl( bulkSmsConfiguration.getUrlTemplateForBatchSms() );
        }

        uriBuilder.queryParam( "username", bulkSmsConfiguration.getUsername() )
            .queryParam( "password", bulkSmsConfiguration.getPassword() ).queryParam( "allow_concat_text_sms", true )
            .queryParam( "concat_text_sms_max_parts", 4 ).queryParam( "stop_dup_id", stopDuplicationID );

        return uriBuilder;
    }

    private OutboundMessageResponse getResponse( ResponseEntity<String> responseEntity )
    {
        OutboundMessageResponse status = new OutboundMessageResponse();

        if ( responseEntity == null )
        {
            status.setResponseObject( GatewayResponse.FAILED );
            status.setOk( false );

            return status;
        }

        String response = responseEntity.getBody();

        GatewayResponse gatewayResponse = BULKSMS_GATEWAY_RESPONSE_MAP.get( StringUtils.split( response, "|" )[0] );

        gatewayResponse.setBatchId( StringUtils.split( response, "|" )[2] );

        status.setResponseObject( gatewayResponse );
        status.setDescription( gatewayResponse.getResponseMessage() );

        return status;
    }

    private String getRecipients( Set<String> recipients )
    {
        return StringUtils.join( recipients, "," );
    }
}
