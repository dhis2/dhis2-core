package org.hisp.dhis.dxf2.telemetry;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.appmanager.AppType;
import org.hisp.dhis.common.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
