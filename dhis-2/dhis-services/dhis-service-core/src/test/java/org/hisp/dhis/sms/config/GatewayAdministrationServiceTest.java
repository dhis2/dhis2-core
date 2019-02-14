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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;


/**
 * @Author Zubair Asghar.
 */
public class GatewayAdministrationServiceTest
{
    private BulkSmsGatewayConfig bulkConfig;
    private ClickatellGatewayConfig clickatellConfig;
    private SmsConfiguration spyConfiguration;

    // -------------------------------------------------------------------------
    // Mocking Dependencies
    // -------------------------------------------------------------------------

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SmsConfigurationManager smsConfigurationManager;

    private DefaultGatewayAdministrationService subject;

    @Before
    public void setUp()
    {
        subject = new DefaultGatewayAdministrationService( smsConfigurationManager );

        spyConfiguration = new SmsConfiguration();
        bulkConfig = new BulkSmsGatewayConfig();
        clickatellConfig = new ClickatellGatewayConfig();

        when( smsConfigurationManager.getSmsConfiguration() ).thenReturn( spyConfiguration );

        doAnswer( invocationOnMock ->
        {
            spyConfiguration = (SmsConfiguration) invocationOnMock.getArguments()[0];
            return spyConfiguration;
        }).when( smsConfigurationManager ).updateSmsConfiguration( any( SmsConfiguration.class ) );

    }

    @Test
    public void testAddGateway()
    {
        boolean isAdded = subject.addGateway( bulkConfig );

        assertTrue( isAdded );
        assertEquals( bulkConfig, spyConfiguration.getGateways().get( 0 ) );
        assertTrue( spyConfiguration.getGateways().get( 0 ).isDefault() );
    }

    @Test
    public void testReturnFalseIfConfigIsNull()
    {
        assertFalse( subject.addGateway( null ) );
    }

    @Test
    public void testSecondGatewayIsSetToFalse()
    {
        spyConfiguration.getGateways().add( bulkConfig );

        when( smsConfigurationManager.getSmsConfiguration() ).thenReturn( spyConfiguration );

        boolean isAdded = subject.addGateway( clickatellConfig );

        assertTrue( isAdded );
        assertEquals( 2, spyConfiguration.getGateways().size() );
        assertTrue( spyConfiguration.getGateways().contains( clickatellConfig ) );
        assertFalse( spyConfiguration.getGateways().get( 0 ).isDefault() );
    }
}
