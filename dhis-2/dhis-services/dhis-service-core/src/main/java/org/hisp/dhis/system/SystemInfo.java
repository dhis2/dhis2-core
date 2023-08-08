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
package org.hisp.dhis.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.springframework.beans.BeanUtils;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@NoArgsConstructor
public class SystemInfo {
  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  @JsonProperty private String contextPath;

  @JsonProperty private String userAgent;

  // -------------------------------------------------------------------------
  // Volatile properties
  // -------------------------------------------------------------------------

  @JsonProperty private String calendar;

  @JsonProperty private String dateFormat;

  @JsonProperty private Date serverDate;

  @JsonProperty private String serverTimeZoneId;

  @JsonProperty private String serverTimeZoneDisplayName;

  @JsonProperty private Date lastAnalyticsTableSuccess;

  @JsonProperty private String intervalSinceLastAnalyticsTableSuccess;

  @JsonProperty private String lastAnalyticsTableRuntime;

  @JsonProperty private Date lastSystemMonitoringSuccess;

  @JsonProperty private Date lastAnalyticsTablePartitionSuccess;

  @JsonProperty private String intervalSinceLastAnalyticsTablePartitionSuccess;

  @JsonProperty private String lastAnalyticsTablePartitionRuntime;

  // -------------------------------------------------------------------------
  // Stable properties
  // -------------------------------------------------------------------------

  @JsonProperty private String version;

  @JsonProperty private String revision;

  @JsonProperty private Date buildTime;

  @JsonProperty private String jasperReportsVersion;

  @JsonProperty private String environmentVariable;

  @JsonProperty private String fileStoreProvider;

  @JsonProperty private String readOnlyMode;

  @JsonProperty private String nodeId;

  @JsonProperty private String javaVersion;

  @JsonProperty private String javaVendor;

  @JsonProperty private String javaOpts;

  @JsonProperty private String osName;

  @JsonProperty private String osArchitecture;

  @JsonProperty private String osVersion;

  @JsonProperty private String externalDirectory;

  @JsonProperty private DatabaseInfo databaseInfo;

  @JsonProperty private Integer readReplicaCount;

  @JsonProperty private String memoryInfo;

  @JsonProperty private Integer cpuCores;

  @JsonProperty private boolean encryption;

  @JsonProperty private boolean emailConfigured;

  @JsonProperty private boolean redisEnabled;

  @JsonProperty private String redisHostname;

  @JsonProperty private String systemId;

  @JsonProperty private String systemName;

  @JsonProperty private String systemMetadataVersion;

  @JsonProperty private String instanceBaseUrl;

  @JsonProperty private String systemMonitoringUrl;

  @JsonProperty private String clusterHostname;

  @JsonProperty private Boolean isMetadataVersionEnabled;

  @JsonProperty private Date lastMetadataVersionSyncAttempt;

  @JsonProperty private Boolean isMetadataSyncEnabled;

  public SystemInfo instance() {
    SystemInfo info = new SystemInfo();
    BeanUtils.copyProperties(this, info);
    // clear sensitive info may reset the data
    info.setDatabaseInfo(databaseInfo == null ? null : databaseInfo.instance());
    return info;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public void clearSensitiveInfo() {
    this.jasperReportsVersion = null;
    this.environmentVariable = null;
    this.fileStoreProvider = null;
    this.readOnlyMode = null;
    this.nodeId = null;
    this.javaVersion = null;
    this.javaVendor = null;
    this.javaOpts = null;
    this.osName = null;
    this.osArchitecture = null;
    this.osVersion = null;
    this.externalDirectory = null;
    this.readReplicaCount = null;
    this.memoryInfo = null;
    this.cpuCores = null;
    this.systemMonitoringUrl = null;
    this.encryption = false;
    this.redisEnabled = false;
    this.redisHostname = null;
    this.systemId = null;
    this.clusterHostname = null;

    if (this.databaseInfo != null) {
      this.databaseInfo.clearSensitiveInfo();
    }
  }
}
