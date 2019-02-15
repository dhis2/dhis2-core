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
    private static final String BULKSMS = "bulksms";
    private static final String CLICKATELL = "clickatell";

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
        bulkConfig.setName( BULKSMS );
        clickatellConfig = new ClickatellGatewayConfig();
        clickatellConfig.setName( CLICKATELL );

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
    public void testUpdateDefaultGatewaySuccess()
    {
        assertTrue( subject.addGateway( bulkConfig ) );
        assertEquals( bulkConfig, subject.getDefaultGateway() );
        assertEquals( BULKSMS, subject.getDefaultGateway().getName() );
        assertEquals( bulkConfig, subject.getDefaultGateway() );

        BulkSmsGatewayConfig updated = new BulkSmsGatewayConfig();
        updated.setName( "changedbulksms" );

        subject.updateGateway( bulkConfig, updated );

        assertEquals( 1, spyConfiguration.getGateways().size() );
        assertEquals( updated, subject.getDefaultGateway() );
        assertEquals( "changedbulksms", subject.getDefaultGateway().getName() );
    }

    @Test
    public void testUpdateGatewaySuccess()
    {
        assertTrue( subject.addGateway( bulkConfig ) );
        assertEquals( bulkConfig, subject.getDefaultGateway() );
        assertEquals( BULKSMS, subject.getDefaultGateway().getName() );
        assertEquals( bulkConfig, subject.getDefaultGateway() );

        subject.addGateway( clickatellConfig );

        assertEquals( 2, spyConfiguration.getGateways().size() );

        ClickatellGatewayConfig updated = new ClickatellGatewayConfig();
        updated.setName( "changedclickatell" );
        updated.setUid( "tempUId" );

        subject.updateGateway( clickatellConfig, updated );

        assertEquals( 2, subject.getGatewayConfigurationMap().size() );
        assertTrue( subject.getGatewayConfigurationMap().get( BULKSMS ).isDefault() );
        assertFalse( subject.getGatewayConfigurationMap().get( CLICKATELL ).isDefault() );
        assertNotEquals( "tempUId", subject.getGatewayConfigurationMap().get( CLICKATELL ).getUid() );
    }

    @Test
    public void testNothingShouldBeUpdatedIfNullIsPassed()
    {
        bulkConfig.setName( BULKSMS );
        assertTrue( subject.addGateway( bulkConfig ) );

        subject.updateGateway( bulkConfig, null );
    }

    @Test
    public void testReturnFalseIfConfigIsNull()
    {
        assertFalse( subject.addGateway( null ) );
    }

    @Test
    public void testWhenNoDefaultGateway()
    {
        assertNull( subject.getDefaultGateway() );
    }

    @Test
    public void testSecondGatewayIsSetToFalse()
    {
        when( smsConfigurationManager.getSmsConfiguration() ).thenReturn( spyConfiguration );

        subject.addGateway( bulkConfig );

        clickatellConfig.setDefault( true );
        boolean isAdded = subject.addGateway( clickatellConfig );

        assertTrue( isAdded );
        assertEquals( 2, spyConfiguration.getGateways().size() );
        assertTrue( spyConfiguration.getGateways().contains( clickatellConfig ) );

        assertNotNull( subject.getDefaultGateway() );
        assertEquals( subject.getDefaultGateway(), bulkConfig );
    }

    @Test
    public void testRemoveDefaultGateway()
    {
        subject.addGateway( bulkConfig );
        subject.addGateway( clickatellConfig );

        String bulkId = subject.getGatewayConfigurationMap().get( BULKSMS ).getUid();
        String clickatelId = subject.getGatewayConfigurationMap().get( CLICKATELL ).getUid();

        assertNotNull( bulkId );
        assertNotNull( clickatelId );
        assertEquals( bulkConfig, subject.getDefaultGateway() );

        assertTrue( subject.removeGatewayByUid( bulkId ) );
        assertEquals( 1, subject.getGatewayConfigurationMap().size() );
        assertNull( subject.getGatewayConfigurationMap().get( BULKSMS ) );
        assertEquals( clickatellConfig, subject.getDefaultGateway() );
    }

    @Test
    public void  testRemoveGateway()
    {
        subject.addGateway( bulkConfig );
        subject.addGateway( clickatellConfig );

        String bulkId = subject.getGatewayConfigurationMap().get( BULKSMS ).getUid();
        String clickatelId = subject.getGatewayConfigurationMap().get( CLICKATELL ).getUid();

        assertEquals( bulkConfig, subject.getDefaultGateway() );
        assertEquals( 2, subject.getGatewayConfigurationMap().size() );

        assertTrue( subject.removeGatewayByUid( clickatelId ) );
        assertEquals( 1, subject.getGatewayConfigurationMap().size() );
        assertNull( subject.getGatewayConfigurationMap().get( CLICKATELL ) );
        assertEquals( bulkConfig, subject.getDefaultGateway() );
    }

    @Test
    public void testRemoveSingleGateway()
    {
        subject.addGateway( bulkConfig );

        String bulkId = subject.getGatewayConfigurationMap().get( BULKSMS ).getUid();

        System.out.println( subject.getGatewayConfigurationMap() );
        subject.removeGatewayByUid( bulkId );

        assertTrue( subject.getGatewayConfigurationMap().isEmpty() );
        assertNull( subject.getDefaultGateway() );
    }
}
