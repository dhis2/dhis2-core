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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StrSubstitutor;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

public class SimplisticHttpGetGateWay
    extends SmsGateway
{
    private static final Set<ContentType> TEMPLATE_SUPPORTED_TYPES = Sets.newHashSet( ContentType.APPLICATION_JSON, ContentType.APPLICATION_XML );
    private static final ContentType URL_SUPPORTED_TYPE = ContentType.FORM_URL_ENCODED;

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
    protected SmsGatewayConfig getGatewayConfigType()
    {
        return new GenericHttpGatewayConfig();
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        GenericHttpGatewayConfig genericConfig = (GenericHttpGatewayConfig) config;

        ContentType contentType = genericConfig.getContentType();
        String data = null;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl( config.getUrlTemplate() );

        HttpEntity<String> requestEntity = null;
        HttpHeaders headers;


        if ( contentType.equals( URL_SUPPORTED_TYPE ) )
        {
            data = encodeUrlParameters( genericConfig, text, recipients );
        }
        else if ( TEMPLATE_SUPPORTED_TYPES.contains( contentType ) )
        {
            data = draftTemplate( genericConfig, text, recipients );
        }

        headers = createHttpHeaders( genericConfig );

        requestEntity = new HttpEntity<>( data, headers );


        URI url = uriBuilder.build().encode().toUri();

        HttpStatus status = send( url.toString(), requestEntity, genericConfig.isUseGet() ? HttpMethod.GET : HttpMethod.POST, String.class );

        return  wrapHttpStatus( status );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private HttpHeaders createHttpHeaders( GenericHttpGatewayConfig  config )
    {
        HttpHeaders httpHeaders = new HttpHeaders();

        Map<String, String> headers = config.getParameters().stream()
            .filter( GenericGatewayParameter::isHeader )
            .collect( Collectors.toMap( GenericGatewayParameter::getKey, p -> p.isEncode() ? Base64.getEncoder().encodeToString( p.getValue().getBytes() ) : p.getValue() ) );

        if ( headers.containsKey( HttpHeaders.AUTHORIZATION ) )
        {
            headers.put( HttpHeaders.AUTHORIZATION, SmsGateway.BASIC + headers.get( HttpHeaders.AUTHORIZATION ) );
        }

        headers.forEach( httpHeaders::set );

        return httpHeaders;
    }

    private String draftTemplate( GenericHttpGatewayConfig config, String text, Set<String> recipients )
    {
        List<GenericGatewayParameter> parameters = config.getParameters();

        Map<String, String> substitutes = parameters.stream().collect( Collectors.toMap( GenericGatewayParameter::getKey, GenericGatewayParameter::getValueForKey ) );
        substitutes.put( config.getMessageParameter(), text );
        substitutes.put( config.getRecipientParameter(), StringUtils.join( recipients, "," ) );

        StrSubstitutor strSubstitutor = new StrSubstitutor( substitutes );

        return strSubstitutor.replace( config.getDataTemplate() );
    }

    private String encodeUrlParameters( GenericHttpGatewayConfig config, String text, Set<String> recipients )
    {
        Map<String, String> requestParams = getUrlParameters( config.getParameters() );
        requestParams.put( config.getMessageParameter(), text );
        requestParams.put( config.getRecipientParameter(), StringUtils.join( recipients, "," ) );

        return requestParams.entrySet().stream()
            .map( v -> v.getKey() + "=" + encodeValue( v.getValue() ) )
            .collect( Collectors.joining( "&" ) );
    }

    private String encodeValue( String value )
    {
        try
        {
            if ( !value.isEmpty() )
            {
                value = URLEncoder.encode( value, StandardCharsets.UTF_8.toString() );
            }
        }
        catch( UnsupportedEncodingException e )
        {
            DebugUtils.getStackTrace( e );
        }

        return value;
    }

    private Map<String, String> getUrlParameters( List<GenericGatewayParameter> parameters )
    {
        return parameters.stream().filter( p -> !p.isHeader() )
            .collect( Collectors.toMap( GenericGatewayParameter::getKey, GenericGatewayParameter::getValueForKey ) ) ;
    }
}
