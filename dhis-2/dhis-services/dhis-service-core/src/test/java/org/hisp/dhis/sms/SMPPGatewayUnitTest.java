package org.hisp.dhis.sms;

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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.config.SMPPClient;
import org.hisp.dhis.sms.config.SMPPGateway;
import org.hisp.dhis.sms.config.SMPPGatewayConfig;
import org.jsmpp.bean.Address;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.SubmitMultiResult;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.bean.UnsuccessDelivery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @Author Zubair Asghar.
 */
public class SMPPGatewayUnitTest extends DhisConvenienceTest
{
    private static final String SYSTEM_ID = "smppclient1";
    private static final String SYSTEM_TYPE = "cp";
    private static final String HOST = "localhost";
    private static final String PASSWORD = "password";
    private static final String MESSAGE_ID = "messageId";
    private static final String RECIPIENT = "4740332255";
    private static final String TEXT = "text through smpp";
    private static final String SUBJECT = "subject";

    private static final int PORT = 2775;
    private static final int REJECTED = 7;

    private SMPPGatewayConfig smppGatewayConfig;
    private SubmitMultiResult multiResult;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SMPPClient smppClient;

    private SMPPGateway subject;

    @Before
    public void init()
    {
        subject = new SMPPGateway( smppClient );

        smppGatewayConfig = new SMPPGatewayConfig();
        smppGatewayConfig.setUrlTemplate( HOST );
        smppGatewayConfig.setPassword( PASSWORD );
        smppGatewayConfig.setPort( PORT );
        smppGatewayConfig.setSystemType( SYSTEM_TYPE );
        smppGatewayConfig.setUsername( SYSTEM_ID );
    }

    @Test
    public void testSuccessMessage()
    {
        UnsuccessDelivery[] unsuccessDeliveries = null;

        multiResult = new SubmitMultiResult( MESSAGE_ID, unsuccessDeliveries );

        when( smppClient.send( anyString(), anySet(), any() ) ).thenReturn( multiResult );

        OutboundMessageResponse response = subject.send( SUBJECT, TEXT, Sets.newHashSet( RECIPIENT ), smppGatewayConfig );

        assertTrue( response.isOk() );
        assertEquals( MESSAGE_ID, response.getDescription() );
    }

    @Test
    public void testFailedMessage()
    {
        UnsuccessDelivery failedDelivery = new UnsuccessDelivery( new Address( TypeOfNumber.NATIONAL, NumberingPlanIndicator.UNKNOWN, "XXXXXX" ), REJECTED );

        UnsuccessDelivery[] unsuccessDeliveries =new UnsuccessDelivery[1];

        unsuccessDeliveries[0] = failedDelivery;

        multiResult = new SubmitMultiResult( MESSAGE_ID, unsuccessDeliveries );

        when( smppClient.send( anyString(), anySet(), any() ) ).thenReturn( multiResult );

        OutboundMessageResponse response = subject.send( SUBJECT, TEXT, Sets.newHashSet( RECIPIENT ), smppGatewayConfig );

        assertFalse( response.isOk() );
        assertEquals( "REJECTD - messageId", response.getDescription() );
    }
}
