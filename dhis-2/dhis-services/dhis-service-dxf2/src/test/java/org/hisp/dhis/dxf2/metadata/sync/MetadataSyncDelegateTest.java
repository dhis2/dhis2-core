package org.hisp.dhis.dxf2.metadata.sync;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.system.util.HttpUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author aamerm
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( HttpUtils.class )
@Category( IntegrationTest.class )
public class MetadataSyncDelegateTest
    extends DhisSpringTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    @InjectMocks
    private MetadataSyncDelegate metadataSyncDelegate;

    @Autowired
    @Mock
    private DefaultMetadataSystemSettingService metadataSystemSettingService;

    @Autowired
    @Mock
    private SystemService systemService;

    @Autowired
    @Mock
    private RenderService renderService;

    private String username = "username";
    private String password = "password";

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks( this );

        PowerMockito.mockStatic( HttpUtils.class );

        when( metadataSystemSettingService.getRemoteInstanceUserName() ).thenReturn( username );
        when( metadataSystemSettingService.getRemoteInstancePassword() ).thenReturn( password );
    }

    @Test
    public void testShouldVerifyIfStopSyncReturnFalseIfNoSystemVersionInLocal()
    {
        String versionSnapshot = "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        SystemInfo systemInfo = new SystemInfo();
        when ( systemService.getSystemInfo() ).thenReturn( systemInfo );
        boolean shouldStopSync = metadataSyncDelegate.shouldStopSync( versionSnapshot );
        assertFalse(shouldStopSync);
    }

    @Test
    public void testShouldVerifyIfStopSyncReturnFalseIfNoSystemVersionInRemote() throws IOException
    {
        String versionSnapshot = "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setVersion( "2.26" );
        when ( systemService.getSystemInfo() ).thenReturn( systemInfo );
        when ( renderService.getSystemObject(any( ByteArrayInputStream.class), eq( RenderFormat.JSON ) ) ).thenReturn( null );
        boolean shouldStopSync = metadataSyncDelegate.shouldStopSync( versionSnapshot );
        assertFalse(shouldStopSync);
    }

    @Test
    public void testShouldVerifyIfStopSyncReturnTrueIfDHISVersionMismatch() throws IOException
    {
        String versionSnapshot = "{\"system:\": {\"date\":\"2016-06-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\"," +
            "\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        String systemNodeString = "{\"date\":\"2016-06-24T05:27:25.128+0000\", \"version\": \"2.26\"}";
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setVersion( "2.25" );
        when( systemService.getSystemInfo() ).thenReturn( systemInfo );
        when( metadataSystemSettingService.getStopMetadataSyncSetting() ).thenReturn( true );
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree( systemNodeString );
        when( renderService.getSystemObject( any( ByteArrayInputStream.class), eq( RenderFormat.JSON ) ) ).thenReturn( jsonNode);

        boolean shouldStopSync = metadataSyncDelegate.shouldStopSync( versionSnapshot );
        assertTrue(shouldStopSync);
    }

    @Test
    public void testShouldVerifyIfStopSyncReturnFalseIfDHISVersionSame() throws IOException
    {
        String versionSnapshot = "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        String systemNodeString = "{\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}";
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setVersion( "2.26" );
        when( systemService.getSystemInfo() ).thenReturn( systemInfo );
        when( metadataSystemSettingService.getStopMetadataSyncSetting() ).thenReturn( true );
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree( systemNodeString );
        when( renderService.getSystemObject(any( ByteArrayInputStream.class), eq( RenderFormat.JSON ) ) ).thenReturn( jsonNode);

        boolean shouldStopSync = metadataSyncDelegate.shouldStopSync( versionSnapshot );
        assertFalse(shouldStopSync);
    }

    @Test
    public void testShouldVerifyIfStopSyncReturnFalseIfStopSyncIsNotSet() throws IOException
    {
        String versionSnapshot = "{\"system:\": {\"date\":\"2016-05-24T05:27:25.128+0000\", \"version\": \"2.26\"}, \"name\":\"testVersion\",\"created\":\"2016-05-26T11:43:59.787+0000\",\"type\":\"BEST_EFFORT\",\"id\":\"ktwh8PHNwtB\",\"hashCode\":\"12wa32d4f2et3tyt5yu6i\"}";
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setVersion( "2.26" );

        when( systemService.getSystemInfo() ).thenReturn( systemInfo );
        when( metadataSystemSettingService.getStopMetadataSyncSetting() ).thenReturn( false );
        boolean shouldStopSync = metadataSyncDelegate.shouldStopSync( versionSnapshot );
        assertFalse(shouldStopSync);
    }
}
