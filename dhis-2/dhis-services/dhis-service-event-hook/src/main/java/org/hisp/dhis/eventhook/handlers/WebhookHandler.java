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
package org.hisp.dhis.eventhook.handlers;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hisp.dhis.eventhook.Event;
import org.hisp.dhis.eventhook.EventHook;
import org.hisp.dhis.eventhook.Handler;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Morten Olav Hansen
 */
@Slf4j
public class WebhookHandler implements Handler
{
    private final WebhookTarget webhookTarget;

    private final RestTemplate restTemplate;

    public WebhookHandler( WebhookTarget target )
    {
        this.webhookTarget = target;
        this.restTemplate = new RestTemplate();
        configure( this.restTemplate );
    }

    @Override
    public void run( EventHook eventHook, Event event, String payload )
    {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType( MediaType.parseMediaType( webhookTarget.getContentType() ) );
        httpHeaders.setAll( webhookTarget.getHeaders() );

        if ( webhookTarget.getAuth() != null )
        {
            webhookTarget.getAuth().apply( httpHeaders );
        }

        HttpEntity<String> httpEntity = new HttpEntity<>( payload, httpHeaders );

        try
        {
            ResponseEntity<String> response = restTemplate.postForEntity(
                webhookTarget.getUrl(), httpEntity, String.class );

            log.info( "EventHook '{}' response status '{}' and body: {}",
                eventHook.getUid(), response.getStatusCode().name(), response.getBody() );
        }
        catch ( RestClientException ex )
        {
            log.error( ex.getMessage() );
        }
    }

    private void configure( RestTemplate template )
    {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

        requestFactory.setConnectionRequestTimeout( 1_000 );
        requestFactory.setConnectTimeout( 5_000 );
        requestFactory.setReadTimeout( 10_000 );
        requestFactory.setBufferRequestBody( true );

        HttpClient httpClient = HttpClientBuilder.create()
            .disableCookieManagement()
            .build();

        requestFactory.setHttpClient( httpClient );

        template.setRequestFactory( requestFactory );
    }
}
