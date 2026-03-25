/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.usagemetrics;

import static org.hisp.dhis.scheduling.JobType.SEND_USAGE_METRICS_CHECK;

import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendUsageMetricsCheckJob implements Job {

  @Qualifier("sendUsageMetricsRegistry")
  private final PrometheusRegistry prometheusRegistry;

  private final SystemService systemService;
  private final JdbcTemplate jdbcTemplate;
  private final UsageMetricsConsentStore usageMetricsConsentStore;
  private final DhisConfigurationProvider dhisConfig;

  private SystemInfo systemInfo;
  private OpenTelemetryExporter openTelemetryExporter;
  private String otelEndpoint;
  @Setter private int exportIntervalSeconds = 604800; // weekly interval

  @Override
  public JobType getJobType() {
    return SEND_USAGE_METRICS_CHECK;
  }

  @PostConstruct
  public void postConstruct() {
    systemInfo = systemService.getSystemInfo();
    otelEndpoint = dhisConfig.getProperty(ConfigurationKey.USAGE_METRICS_ENDPIONT);
  }

  @Override
  public void execute(JobEntry config, JobProgress progress) {
    List<UsageMetricsConsent> usageMetricsConsents = usageMetricsConsentStore.getAll();
    if (!usageMetricsConsents.isEmpty()
        && usageMetricsConsents.get(0).getConsent()
        && openTelemetryExporter == null) {
      String currentDbSystemIdentifier = getCurrentDbSystemIdentifier();

      if (isEnvMatch(
          currentDbSystemIdentifier, usageMetricsConsents.get(0).getDbSystemIdentifier())) {
        openTelemetryExporter =
            OpenTelemetryExporter.builder()
                .endpoint(otelEndpoint)
                .protocol("http/protobuf")
                .serviceInstanceId(
                    DigestUtils.md5Hex(currentDbSystemIdentifier + systemInfo.getSystemId()))
                .serviceName("dhis2-core")
                .serviceNamespace("DHIS2")
                .serviceVersion(systemInfo.getVersion())
                .intervalSeconds(exportIntervalSeconds)
                .registry(prometheusRegistry)
                .buildAndStart();
      } else {
        usageMetricsConsentStore.delete(usageMetricsConsents.get(0));
        closeMetricsExporter();
        log.warn(
            "Opted-in to sending usage metrics but detected environment mismatch due to different DB system identifiers. Expected DB system identifier was [{}] but actual DB system identifier is [{}]. Opt-in again to send DHIS2 usage metrics",
            usageMetricsConsents.get(0).getDbSystemIdentifier(),
            currentDbSystemIdentifier);
      }
    } else if (usageMetricsConsents.isEmpty() || !usageMetricsConsents.get(0).getConsent()) {
      closeMetricsExporter();
    }
  }

  protected boolean isEnvMatch(
      String currentDbSystemIdentifier, String expectedDbSystemIdentifier) {
    return currentDbSystemIdentifier.equals(expectedDbSystemIdentifier);
  }

  protected String getCurrentDbSystemIdentifier() {
    return jdbcTemplate
        .queryForList("SELECT system_identifier FROM pg_control_system()")
        .get(0)
        .get("system_identifier")
        .toString();
  }

  protected void closeMetricsExporter() {
    if (openTelemetryExporter != null) {
      openTelemetryExporter.close();
      openTelemetryExporter = null;
    }
  }
}
