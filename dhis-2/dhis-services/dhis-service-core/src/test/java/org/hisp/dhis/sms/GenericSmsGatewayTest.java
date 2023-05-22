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
package org.hisp.dhis.sms;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.hisp.dhis.sms.config.ContentType;
import org.hisp.dhis.sms.config.GenericGatewayParameter;
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
import org.hisp.dhis.sms.config.SimplisticHttpGetGateWay;
import org.hisp.dhis.sms.config.SmsGateway;
import org.hisp.dhis.system.util.SmsUtils;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Sets;

/**
 * @author Zubair Asghar.
 */
@ExtendWith( MockitoExtension.class )
class GenericSmsGatewayTest
{

    private static final String GATEWAY_URL = "http://gateway.com/messages";

    private static final String UID = "UID-123";

    private static final String CONFIG_TEMPLATE_JSON = "{\"to\": \"${recipients}\",\"body\": \"${text}\"}";

    private static final String CONFIG_TEMPLATE_URL_ENCODED = "to=${recipients}&message=${text}&user=${user}&pass=${password}";

    private static final String TEXT = "HI DHIS2";

    private static final String SUBJECT = "Greeting";

    private static final Set<String> RECIPIENTS = Sets.newHashSet( "4033XXYY, 404YYXXX" );

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private PBEStringEncryptor pbeStringEncryptor;

    @Captor
    private ArgumentCaptor<HttpEntity<String>> httpEntityArgumentCaptor;

    @Captor
    private ArgumentCaptor<HttpMethod> httpMethodArgumentCaptor;

    private SimplisticHttpGetGateWay subject;

    private GenericHttpGatewayConfig gatewayConfig;

    private GenericGatewayParameter username;

    private GenericGatewayParameter password;

    private StringSubstitutor strSubstitutor;

    private String body;

    private Map<String, String> valueStore = new HashMap<>();

    @BeforeEach
    public void setUp()
    {
        subject = new SimplisticHttpGetGateWay( restTemplate, pbeStringEncryptor );

        gatewayConfig = new GenericHttpGatewayConfig();
        gatewayConfig.setUseGet( false );
        gatewayConfig.setName( "generic" );
        gatewayConfig.setUrlTemplate( GATEWAY_URL );
        gatewayConfig.setDefault( true );
        gatewayConfig.setUid( UID );

        username = new GenericGatewayParameter();
        username.setKey( "user" );
        username.setValue( "user_uio" );
        username.setEncode( false );
        username.setHeader( true );
        username.setConfidential( false );

        password = new GenericGatewayParameter();
        password.setKey( "password" );
        password.setValue( "abc123" );
        password.setEncode( false );
        password.setHeader( true );
        password.setConfidential( true );

        valueStore.put( SmsGateway.KEY_TEXT, SmsUtils.encode( TEXT ) );
        valueStore.put( SmsGateway.KEY_RECIPIENT, StringUtils.join( RECIPIENTS, "," ) );
    }

    @Test
    void testSendSms_Json()
    {
        strSubstitutor = new StringSubstitutor( valueStore );
        body = strSubstitutor.replace( CONFIG_TEMPLATE_JSON );

        gatewayConfig.getParameters().clear();
        gatewayConfig.setParameters( Arrays.asList( username, password ) );
        gatewayConfig.setContentType( ContentType.APPLICATION_JSON );
        gatewayConfig.setConfigurationTemplate( CONFIG_TEMPLATE_JSON );

        ResponseEntity<String> responseEntity = new ResponseEntity<>( "success", HttpStatus.OK );

        when( restTemplate.exchange( any( URI.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) )
            .thenReturn( responseEntity );

        assertThat( subject.send( SUBJECT, TEXT, RECIPIENTS, gatewayConfig ).isOk(), is( true ) );

        verify( restTemplate ).exchange( any( URI.class ), httpMethodArgumentCaptor.capture(),
            httpEntityArgumentCaptor.capture(), eq( String.class ) );

        assertNotNull( httpEntityArgumentCaptor.getValue() );
        assertNotNull( httpMethodArgumentCaptor.getValue() );

        HttpMethod httpMethod = httpMethodArgumentCaptor.getValue();
        assertEquals( HttpMethod.POST, httpMethod );

        HttpEntity<String> requestEntity = httpEntityArgumentCaptor.getValue();

        assertEquals( body, requestEntity.getBody() );

        HttpHeaders httpHeaders = requestEntity.getHeaders();
        assertTrue( httpHeaders.get( "Content-type" ).contains( gatewayConfig.getContentType().getValue() ) );

        List<GenericGatewayParameter> parameters = gatewayConfig.getParameters();

        parameters.stream().filter( p -> p.isEncode() && p.isConfidential() && p.isHeader() ).forEach( p -> {
            assertTrue( httpHeaders.containsKey( p.getKey() ) );
            assertEquals( " Basic ZGVjcnlwdGVkVGV4dA==", httpHeaders.get( p.getKey() ).get( 0 ) );
        } );
    }

    @Test
    void testSendSms_Url()
    {
        username.setHeader( false );
        password.setHeader( false );
        password.setConfidential( true );

        valueStore.put( username.getKey(), username.getValue() );
        valueStore.put( password.getKey(), password.getValue() );

        strSubstitutor = new StringSubstitutor( valueStore );

        gatewayConfig.getParameters().clear();
        gatewayConfig.setParameters( Arrays.asList( username, password ) );
        gatewayConfig.setContentType( ContentType.FORM_URL_ENCODED );
        gatewayConfig.setConfigurationTemplate( CONFIG_TEMPLATE_URL_ENCODED );

        body = strSubstitutor.replace( CONFIG_TEMPLATE_URL_ENCODED );

        ResponseEntity<String> responseEntity = new ResponseEntity<>( "success", HttpStatus.OK );

        when( pbeStringEncryptor.decrypt( anyString() ) ).thenReturn( password.getValue() );
        when( restTemplate.exchange( any( URI.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) )
            .thenReturn( responseEntity );

        assertThat( subject.send( SUBJECT, TEXT, RECIPIENTS, gatewayConfig ).isOk(), is( true ) );

        verify( restTemplate ).exchange( any( URI.class ), httpMethodArgumentCaptor.capture(),
            httpEntityArgumentCaptor.capture(), eq( String.class ) );

        assertNotNull( httpEntityArgumentCaptor.getValue() );
        assertNotNull( httpMethodArgumentCaptor.getValue() );

        HttpMethod httpMethod = httpMethodArgumentCaptor.getValue();
        assertEquals( HttpMethod.POST, httpMethod );

        HttpEntity<String> requestEntity = httpEntityArgumentCaptor.getValue();

        assertEquals( body, requestEntity.getBody() );
    }
}
