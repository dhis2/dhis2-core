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

import static org.hisp.dhis.util.DateUtils.getPrettyInterval;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DefaultSystemService implements SystemService, InitializingBean {

  private final LocationManager locationManager;
  private final DatabaseInfoProvider databaseInfoProvider;
  private final ConfigurationService configurationService;
  private final DhisConfigurationProvider dhisConfig;
  private final CalendarService calendarService;
  private final SystemSettingManager settings;

  /** Variable holding fixed system info state. */
  private SystemInfo systemInfo = null;

  @Override
  public void afterPropertiesSet() {
    systemInfo = getStableSystemInfo();

    List<String> info =
        List.of(
            "DHIS 2 Version: " + systemInfo.getVersion(),
            "Revision: " + systemInfo.getRevision(),
            "Build date: " + systemInfo.getBuildTime(),
            "Database name: " + systemInfo.getDatabaseInfo().getName(),
            "Java version: " + systemInfo.getJavaVersion());

    log.info(String.join(", ", info));
  }

  // -------------------------------------------------------------------------
  // SystemService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public SystemInfo getSystemInfo() {
    if (systemInfo == null) return null;

    TimeZone tz = Calendar.getInstance().getTimeZone();
    Date lastAnalyticsTableSuccess =
        settings.getDateSetting(SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_UPDATE);
    Date lastAnalyticsTablePartitionSuccess =
        settings.getDateSetting(SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_UPDATE);

    Date now = new Date();

    return systemInfo.toBuilder()
        .calendar(calendarService.getSystemCalendar().name())
        .dateFormat(calendarService.getSystemDateFormat().getJs())
        .serverDate(now)
        .serverTimeZoneId(tz.getID())
        .serverTimeZoneDisplayName(tz.getDisplayName())
        .lastAnalyticsTableSuccess(lastAnalyticsTableSuccess)
        .intervalSinceLastAnalyticsTableSuccess(getPrettyInterval(lastAnalyticsTableSuccess, now))
        .lastAnalyticsTableRuntime(
            settings.getStringSetting(SettingKey.LAST_SUCCESSFUL_ANALYTICS_TABLES_RUNTIME))
        .lastAnalyticsTablePartitionSuccess(lastAnalyticsTablePartitionSuccess)
        .intervalSinceLastAnalyticsTablePartitionSuccess(
            getPrettyInterval(lastAnalyticsTablePartitionSuccess, now))
        .lastAnalyticsTablePartitionRuntime(
            settings.getStringSetting(
                SettingKey.LAST_SUCCESSFUL_LATEST_ANALYTICS_PARTITION_RUNTIME))
        .lastSystemMonitoringSuccess(
            settings.getDateSetting(SettingKey.LAST_SUCCESSFUL_SYSTEM_MONITORING_PUSH))
        .systemName(settings.getStringSetting(SettingKey.APPLICATION_TITLE))
        .instanceBaseUrl(dhisConfig.getServerBaseUrl())
        .emailConfigured(settings.emailConfigured())
        .isMetadataVersionEnabled(settings.getBooleanSetting(SettingKey.METADATAVERSION_ENABLED))
        .systemMetadataVersion(settings.getStringSetting(SettingKey.SYSTEM_METADATA_VERSION))
        .lastMetadataVersionSyncAttempt(
            getLastMetadataVersionSyncAttempt(
                settings.getDateSetting(SettingKey.LAST_SUCCESSFUL_METADATA_SYNC),
                settings.getDateSetting(SettingKey.METADATA_LAST_FAILED_TIME)))
        .build();
  }

  /**
   * @return A {@link SystemInfo} with all properties set that are stable (immutable) after start
   */
  private SystemInfo getStableSystemInfo() {
    Configuration config = configurationService.getConfiguration();
    Properties props = System.getProperties();
    boolean redisEnabled = dhisConfig.isEnabled(ConfigurationKey.REDIS_ENABLED);

    return loadBuildProperties().toBuilder()
        .environmentVariable(locationManager.getEnvironmentVariable())
        .externalDirectory(getExternalDirectory())
        .fileStoreProvider(dhisConfig.getProperty(ConfigurationKey.FILESTORE_PROVIDER))
        .readOnlyMode(dhisConfig.getProperty(ConfigurationKey.SYSTEM_READ_ONLY_MODE))
        .nodeId(dhisConfig.getProperty(ConfigurationKey.NODE_ID))
        .systemMonitoringUrl(dhisConfig.getProperty(ConfigurationKey.SYSTEM_MONITORING_URL))
        .systemId(config.getSystemId())
        .clusterHostname(dhisConfig.getProperty(ConfigurationKey.CLUSTER_HOSTNAME))
        .redisEnabled(redisEnabled)
        .redisHostname(redisEnabled ? dhisConfig.getProperty(ConfigurationKey.REDIS_HOST) : null)
        // Database
        .databaseInfo(databaseInfoProvider.getDatabaseInfo())
        .readReplicaCount(
            Integer.valueOf(dhisConfig.getProperty(ConfigurationKey.ACTIVE_READ_REPLICAS)))
        // System env variables and properties
        .javaOpts(getJavaOpts())
        .javaVersion(props.getProperty("java.version"))
        .javaVendor(props.getProperty("java.vendor"))
        .osName(props.getProperty("os.name"))
        .osArchitecture(props.getProperty("os.arch"))
        .osVersion(props.getProperty("os.version"))
        .memoryInfo(SystemUtils.getMemoryString())
        .cpuCores(SystemUtils.getCpuCores())
        .encryption(dhisConfig.getEncryptionStatus().isOk())
        .build();
  }

  public static SystemInfo loadBuildProperties() {
    ClassPathResource resource = new ClassPathResource("build.properties");

    if (resource.isReadable()) {
      try (InputStream in = resource.getInputStream()) {
        Properties properties = new Properties();
        properties.load(in);
        String buildTime = properties.getProperty("build.time");
        DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

        return SystemInfo.builder()
            .version(properties.getProperty("build.version"))
            .revision(properties.getProperty("build.revision"))
            .jasperReportsVersion(properties.getProperty("jasperreports.version"))
            .buildTime(new DateTime(dateFormat.parseDateTime(buildTime)).toDate())
            .build();
      } catch (IOException ex) {
        // Do nothing
      }
    } else {
      log.error(
          "build.properties is not available in the classpath. "
              + "Make sure you build the project with Maven before you start the embedded Jetty server.");
    }
    return SystemInfo.builder().build();
  }

  private static String getJavaOpts() {
    try {
      return System.getenv("JAVA_OPTS");
    } catch (SecurityException ex) {
      return "Unknown";
    }
  }

  @Nonnull
  private String getExternalDirectory() {
    try {
      File directory = locationManager.getExternalDirectory();
      return directory.getAbsolutePath();
    } catch (LocationManagerException ex) {
      return "Not set";
    }
  }

  private Date getLastMetadataVersionSyncAttempt(
      Date lastSuccessfulMetadataSyncTime, Date lastFailedMetadataSyncTime) {
    if (lastSuccessfulMetadataSyncTime == null && lastFailedMetadataSyncTime == null) {
      return null;
    }
    if (lastSuccessfulMetadataSyncTime == null || lastFailedMetadataSyncTime == null) {
      return (lastFailedMetadataSyncTime != null
          ? lastFailedMetadataSyncTime
          : lastSuccessfulMetadataSyncTime);
    }
    return (lastSuccessfulMetadataSyncTime.compareTo(lastFailedMetadataSyncTime) < 0)
        ? lastFailedMetadataSyncTime
        : lastSuccessfulMetadataSyncTime;
  }
}
