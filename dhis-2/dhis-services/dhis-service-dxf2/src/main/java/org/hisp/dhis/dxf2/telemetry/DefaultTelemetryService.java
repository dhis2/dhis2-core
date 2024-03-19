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
package org.hisp.dhis.dxf2.telemetry;

import java.util.Date;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.statistics.StatisticsProvider;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.dxf2.telemetry.TelemetryService")
public class DefaultTelemetryService implements TelemetryService {
  private final SystemService systemService;

  private final SystemSettingManager systemSettingManager;

  private final RestTemplate restTemplate;

  private final TaskScheduler scheduler;

  private final StatisticsProvider statisticsProvider;

  private final AppManager appManager;

  @PostConstruct
  public void init() {
    /* Schedule telemetry reports once per month on the first of the month */
    CronTrigger cronTrigger = new CronTrigger("* * * 1 * ?");
    scheduler.schedule(this::pushTelemetryReport, cronTrigger);

    log.info("Scheduled telemetry service");
  }

  @Override
  public TelemetryData getTelemetryData() {
    SystemInfo systemInfo = systemService.getSystemInfo().withoutSensitiveInfo();
    return TelemetryData.builder()
        .systemId(systemInfo.getSystemId())
        .version(systemInfo.getVersion())
        .revision(systemInfo.getRevision())
        .buildTime(systemInfo.getBuildTime())

        // .javaVersion(systemInfo.getJavaVersion())
        // .javaVendor(systemInfo.getJavaVendor())
        // .osName(systemInfo.getOsName())

        .readOnlyMode(systemInfo.getReadOnlyMode())
        .readReplicaCount(systemInfo.getReadReplicaCount())
        .encryption(systemInfo.isEncryption())
        .emailConfigured(systemInfo.isEmailConfigured())
        .redisEnabled(systemInfo.isRedisEnabled())
        .isMetadataVersionEnabled(systemInfo.getIsMetadataVersionEnabled())
        .isMetadataSyncEnabled(systemInfo.getIsMetadataSyncEnabled())
        .calendar(systemInfo.getCalendar())
        .dateFormat(systemInfo.getDateFormat())
        .lastAnalyticsTableSuccess(systemInfo.getLastAnalyticsTableSuccess())
        .lastAnalyticsTableRuntime(systemInfo.getLastAnalyticsTableRuntime())
        .lastAnalyticsTablePartitionSuccess(systemInfo.getLastAnalyticsTablePartitionSuccess())
        .lastAnalyticsTablePartitionRuntime(systemInfo.getLastAnalyticsTablePartitionRuntime())
        .lastMetadataVersionSyncAttempt(systemInfo.getLastMetadataVersionSyncAttempt())
        .objectCounts(statisticsProvider.getObjectCounts())
        .apps(
            appManager.getApps("").stream()
                .map(
                    app ->
                        new TelemetryData.AppInfo(
                            app.getName(),
                            app.getVersion(),
                            app.getAppHubId(),
                            app.hasAppEntrypoint(),
                            app.hasPluginEntrypoint(),
                            app.getAppType(),
                            app.getPluginType()))
                .toList())
        .build();
  }

  @Override
  public void pushTelemetryReport() {
    Boolean enabled = systemSettingManager.getBooleanSetting(SettingKey.TELEMETRY_REPORTS_ENABLED);
    if (!BooleanUtils.toBoolean(enabled)) {
      log.debug("Telemetry reports are disabled, skipping telemetry report push");
      return;
    }

    String url = systemSettingManager.getStringSetting(SettingKey.TELEMETRY_URL);
    if (StringUtils.isBlank(url)) {
      log.debug("Telemetry service URL not configured, aborting telemetry report");
      return;
    }

    TelemetryData telemetryData = getTelemetryData();

    if (StringUtils.isBlank(telemetryData.getSystemId())) {
      log.warn("System ID not available, aborting telemetry report");
      return;
    }

    pushTelemetryReport(telemetryData, url);
  }

  /**
   * Pushes system info to the monitoring target.
   *
   * @param telemetryData the {@link TelemetryData}.
   */
  private void pushTelemetryReport(TelemetryData telemetryData, String url) {
    Date startTime = new Date();

    HttpEntity<TelemetryData> requestEntity = new HttpEntity<>(telemetryData);

    ResponseEntity<String> response = null;
    HttpStatus sc = null;

    try {
      response = restTemplate.postForEntity(url, requestEntity, String.class);
      sc = response.getStatusCode();
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      log.warn(String.format("Monitoring request failed, status code: %s", sc), ex);
      return;
    } catch (ResourceAccessException ex) {
      log.info("Monitoring request failed, network is unreachable");
      return;
    }

    if (response != null && sc != null && sc.is2xxSuccessful()) {
      systemSettingManager.saveSystemSetting(
          SettingKey.LAST_SUCCESSFUL_SYSTEM_MONITORING_PUSH, startTime);

      log.debug(String.format("Telemetry report successfully sent, URL: %s", url));
    } else {
      log.warn(String.format("Telemetry report failed with status code: %s", sc));
    }
  }
}
