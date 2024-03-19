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
package org.hisp.dhis.dxf2.metadata.systemsettings;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Provide the endpoints for api calls in metadata versioning
 *
 * @author anilkumk
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.dxf2.metadata.sync.MetadataSystemSettingService")
@Scope("prototype")
public class DefaultMetadataSystemSettingService implements MetadataSystemSettingService {
  private final SystemSettingManager systemSettingManager;

  private final String API_URL = "/api/metadata/version";

  private final String BASELINE_URL = API_URL + "/history?baseline=";

  @Override
  public String getRemoteInstanceUserName() {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_USERNAME);
  }

  @Override
  public String getRemoteInstancePassword() {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_PASSWORD);
  }

  @Override
  public String getVersionDetailsUrl(String versionName) {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_URL)
        + API_URL
        + "?versionName="
        + versionName;
  }

  @Override
  public String getDownloadVersionSnapshotURL(String versionName) {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_URL)
        + API_URL
        + "/"
        + versionName
        + "/data.gz";
  }

  @Override
  public String getMetaDataDifferenceURL(String versionName) {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_URL)
        + BASELINE_URL
        + versionName;
  }

  @Override
  public String getEntireVersionHistory() {
    return systemSettingManager.getStringSetting(SettingKey.REMOTE_INSTANCE_URL)
        + API_URL
        + "/history";
  }

  @Override
  public void setSystemMetadataVersion(String versionName) {
    systemSettingManager.saveSystemSetting(SettingKey.SYSTEM_METADATA_VERSION, versionName);
  }

  @Override
  public String getSystemMetadataVersion() {
    return systemSettingManager.getStringSetting(SettingKey.SYSTEM_METADATA_VERSION);
  }

  @Override
  public Boolean getStopMetadataSyncSetting() {
    Boolean stopSyncSetting = systemSettingManager.getBooleanSetting(SettingKey.STOP_METADATA_SYNC);
    return stopSyncSetting == null ? false : stopSyncSetting;
  }
}
