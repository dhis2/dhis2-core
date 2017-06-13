package org.hisp.dhis.dxf2.metadata.systemsettings;

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

import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provide the endpoints for api calls in metadata versioning
 *
 * @author anilkumk.
 */

public class DefaultMetadataSystemSettingService
    implements MetadataSystemSettingService
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    private final String API_URL = "/api/metadata/version";
    private final String BASELINE_URL = API_URL + "/history?baseline=";

    public String getRemoteInstanceUserName()
    {
        return (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
    }

    public String getRemoteInstancePassword()
    {
        return (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );
    }

    public String getVersionDetailsUrl( String versionName )
    {
        return systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + API_URL + "?versionName=" + versionName;
    }

    public String getDownloadVersionSnapshotURL( String versionName )
    {
        return systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + API_URL + "/" + versionName + "/data.gz";
    }

    public String getMetaDataDifferenceURL( String versionName )
    {
        return systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + BASELINE_URL + versionName;
    }

    public String getEntireVersionHistory()
    {
        return systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + API_URL + "/history";
    }

    public void setSystemMetadataVersion( String versionName )
    {
        systemSettingManager.saveSystemSetting( SettingKey.SYSTEM_METADATA_VERSION, versionName );
    }

    public String getSystemMetadataVersion()
    {
        return (String) systemSettingManager.getSystemSetting( SettingKey.SYSTEM_METADATA_VERSION );
    }

    public Boolean getStopMetadataSyncSetting()
    {
        Boolean stopSyncSetting = (Boolean) systemSettingManager.getSystemSetting( SettingKey.STOP_METADATA_SYNC );
        return stopSyncSetting == null ? false : stopSyncSetting;
    }
}
