/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.dxf2.telemetry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.appmanager.AppType;
import org.hisp.dhis.common.Objects;

@Getter
@Setter
@Builder(toBuilder = true)
public final class TelemetryData {
  @JsonProperty String systemId;

  // -------------------------------------------------------------------------
  // System Version
  // -------------------------------------------------------------------------

  @JsonProperty private final String version;
  @JsonProperty private final String revision;
  @JsonProperty private final Date buildTime;

  // -------------------------------------------------------------------------
  // Environment
  // -------------------------------------------------------------------------

  // @JsonProperty private final String javaVersion;
  // @JsonProperty private final String javaVendor;
  // @JsonProperty private final String osName;
  // @JsonProperty private final String memoryInfo;
  // @JsonProperty private final Integer cpuCores;

  // -------------------------------------------------------------------------
  // System configuration
  // -------------------------------------------------------------------------
  @JsonProperty private final String readOnlyMode;
  @JsonProperty private final Integer readReplicaCount;
  @JsonProperty private final boolean encryption;
  @JsonProperty private final boolean emailConfigured;
  @JsonProperty private final boolean redisEnabled;
  @JsonProperty private final Boolean isMetadataVersionEnabled;
  @JsonProperty private final Boolean isMetadataSyncEnabled;
  @JsonProperty private final String calendar;
  @JsonProperty private final String dateFormat;

  // -------------------------------------------------------------------------
  // System statistics
  // -------------------------------------------------------------------------

  @JsonProperty private final Date lastAnalyticsTableSuccess;
  @JsonProperty private final String lastAnalyticsTableRuntime;
  @JsonProperty private final Date lastAnalyticsTablePartitionSuccess;
  @JsonProperty private final String lastAnalyticsTablePartitionRuntime;
  @JsonProperty private final Date lastMetadataVersionSyncAttempt;

  // -------------------------------------------------------------------------
  // Database statistics
  // -------------------------------------------------------------------------

  @JsonProperty private final Map<Objects, Long> objectCounts;

  // -------------------------------------------------------------------------
  // Installed applications
  // -------------------------------------------------------------------------

  @JsonProperty private final List<AppInfo> apps;

  @RequiredArgsConstructor
  public static class AppInfo {
    @JsonProperty final String name;
    @JsonProperty final String version;
    @JsonProperty final String id;

    @JsonProperty final boolean hasAppEntrypoint;
    @JsonProperty final boolean hasPluginEntrypoint;

    @JsonProperty final AppType appType;
    @JsonProperty final String pluginType;
  }
}
