/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.springframework.stereotype.Service;

/**
 * Provide the endpoints for api calls in metadata versioning
 *
 * @author anilkumk
 */
@RequiredArgsConstructor
@Service
public class DefaultMetadataSystemSettingService implements MetadataSystemSettingService {

  private final SystemSettingsService settingsService;

  @Override
  public String getRemoteInstanceUserName() {
    return getSettings().getRemoteInstanceUsername();
  }

  @Override
  public String getRemoteInstancePassword() {
    return getSettings().getRemoteInstancePassword();
  }

  @Override
  public String getVersionDetailsUrl(String versionName) {
    return getBaseUrl() + "?versionName=" + versionName;
  }

  @Override
  public String getDownloadVersionSnapshotURL(String versionName) {
    return getBaseUrl() + "/" + versionName + "/data.gz";
  }

  @Override
  public String getMetaDataDifferenceURL(String versionName) {
    return getBaseUrl() + "/history?baseline=" + versionName;
  }

  @Override
  public String getEntireVersionHistory() {
    return getBaseUrl() + "/history";
  }

  @Nonnull
  private String getBaseUrl() {
    return getSettings().getRemoteInstanceUrl() + "/api/metadata/version";
  }

  @Override
  public void setSystemMetadataVersion(String versionName) {
    settingsService.put("keySystemMetadataVersion", versionName);
  }

  @Override
  public String getSystemMetadataVersion() {
    return getSettings().getSystemMetadataVersion();
  }

  @Override
  public boolean getStopMetadataSyncSetting() {
    return getSettings().getStopMetadataSync();
  }

  private SystemSettings getSettings() {
    return settingsService.getCurrentSettings();
  }
}
