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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.system.database.DatabaseInfo;

/**
 * @author Lars Helge Overland
 */
@Getter
@Setter
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SystemInfo {
  // -------------------------------------------------------------------------
  // Transient properties
  // -------------------------------------------------------------------------

  @JsonProperty private final String contextPath;
  @JsonProperty private final String userAgent;

  // -------------------------------------------------------------------------
  // Volatile properties
  // -------------------------------------------------------------------------

  @JsonProperty private final String calendar;
  @JsonProperty private final String dateFormat;
  @JsonProperty private final Date serverDate;
  @JsonProperty private final String serverTimeZoneId;
  @JsonProperty private final String serverTimeZoneDisplayName;
  @JsonProperty private final Date lastAnalyticsTableSuccess;
  @JsonProperty private final String intervalSinceLastAnalyticsTableSuccess;
  @JsonProperty private final String lastAnalyticsTableRuntime;
  @JsonProperty private final Date lastSystemMonitoringSuccess;
  @JsonProperty private final Date lastAnalyticsTablePartitionSuccess;
  @JsonProperty private final String intervalSinceLastAnalyticsTablePartitionSuccess;
  @JsonProperty private final String lastAnalyticsTablePartitionRuntime;
  @JsonProperty private final DatabaseInfo databaseInfo;

  // -------------------------------------------------------------------------
  // Stable properties
  // -------------------------------------------------------------------------

  @JsonProperty private final String version;
  @JsonProperty private final String revision;
  @JsonProperty private final Date buildTime;
  @JsonProperty private final String jasperReportsVersion;
  @JsonProperty private final String environmentVariable;
  @JsonProperty private final String fileStoreProvider;
  @JsonProperty private final String readOnlyMode;
  @JsonProperty private final String nodeId;
  @JsonProperty private final String javaVersion;
  @JsonProperty private final String javaVendor;
  @JsonProperty private final String javaOpts;
  @JsonProperty private final String osName;
  @JsonProperty private final String osArchitecture;
  @JsonProperty private final String osVersion;
  @JsonProperty private final String externalDirectory;
  @JsonProperty private final Integer readReplicaCount;
  @JsonProperty private final String memoryInfo;
  @JsonProperty private final Integer cpuCores;
  @JsonProperty private final boolean encryption;
  @JsonProperty private final boolean emailConfigured;
  @JsonProperty private final boolean redisEnabled;
  @JsonProperty private final String redisHostname;
  @JsonProperty private final String systemId;
  @JsonProperty private final String systemName;
  @JsonProperty private final String systemMetadataVersion;
  @JsonProperty private final String instanceBaseUrl;
  @JsonProperty private final String systemMonitoringUrl;
  @JsonProperty private final String clusterHostname;
  @JsonProperty private final Boolean isMetadataVersionEnabled;
  @JsonProperty private final Date lastMetadataVersionSyncAttempt;
  @JsonProperty private final Boolean isMetadataSyncEnabled;

  /**
   * Clears sensitive system info properties.
   *
   * <p>Note that {@code systemId} must be present for {@link
   * org.hisp.dhis.dxf2.monitoring.MonitoringService} to function.
   */
  public SystemInfo withoutSensitiveInfo() {
    return toBuilder()
        .jasperReportsVersion(null)
        .environmentVariable(null)
        .fileStoreProvider(null)
        .readOnlyMode(null)
        .nodeId(null)
        .javaVersion(null)
        .javaVendor(null)
        .javaOpts(null)
        .osName(null)
        .osArchitecture(null)
        .osVersion(null)
        .externalDirectory(null)
        .readReplicaCount(null)
        .memoryInfo(null)
        .cpuCores(null)
        .systemMonitoringUrl(null)
        .encryption(false)
        .redisEnabled(false)
        .redisHostname(null)
        .clusterHostname(null)
        .databaseInfo(databaseInfo.withoutSensitiveInfo())
        .build();
  }
}
