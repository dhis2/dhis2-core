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

package org.hisp.dhis.dxf2.metadata.sync;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dxf2.metadata.systemsettings.DefaultMetadataSystemSettingService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * @author anilkumk
 */

public class MetadataSystemSettingServiceTest
    extends DhisSpringTest
{
    @Autowired
    SystemSettingManager systemSettingManager;

    @Autowired
    DefaultMetadataSystemSettingService metadataSystemSettingService;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks( this );

        systemSettingManager.saveSystemSetting( SettingKey.REMOTE_INSTANCE_URL, "http://localhost:9080" );
        systemSettingManager.saveSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME, "username" );
        systemSettingManager.saveSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD, "password" );
    }

    @Test
    public void testShouldGetRemoteUserName()
    {
        String remoteInstanceUserName = metadataSystemSettingService.getRemoteInstanceUserName();

        assertEquals( "username", remoteInstanceUserName );
    }

    @Test
    public void testShouldGetRemotePassword()
    {
        String remoteInstancePassword = metadataSystemSettingService.getRemoteInstancePassword();

        assertEquals( "password", remoteInstancePassword );
    }

    @Test
    public void testShouldDownloadMetadataVersionForGivenVersionName()
    {
        String downloadVersionUrl = metadataSystemSettingService.getVersionDetailsUrl( "Version_Name" );

        assertEquals( "http://localhost:9080/api/metadata/version?versionName=Version_Name", downloadVersionUrl );
    }

    @Test
    public void testShouldDownloadMetadataVersionSnapshotForGivenVersionName()
    {
        String downloadVersionUrl = metadataSystemSettingService.getDownloadVersionSnapshotURL( "Version_Name" );

        assertEquals( "http://localhost:9080/api/metadata/version/Version_Name/data.gz", downloadVersionUrl );
    }

    @Test
    public void testShouldGetAllVersionsCreatedAfterTheGivenVersionName()
    {
        String metadataDifferenceUrl = metadataSystemSettingService.getMetaDataDifferenceURL("Version_Name");

        assertEquals("http://localhost:9080/api/metadata/version/history?baseline=Version_Name", metadataDifferenceUrl);
    }

    @Test
    public void testShouldGetEntireVersionHistoryWhenNoVersionNameIsGiven()
    {
        String versionHistoryUrl = metadataSystemSettingService.getEntireVersionHistory();

        assertEquals("http://localhost:9080/api/metadata/version/history", versionHistoryUrl );
    }
}