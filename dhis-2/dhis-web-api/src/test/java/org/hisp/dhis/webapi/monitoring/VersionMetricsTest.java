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
package org.hisp.dhis.webapi.monitoring;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.Date;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link VersionMetrics}. */
class VersionMetricsTest {

  @Test
  @DisplayName("Registration is a no-op without a meter registry")
  void nullRegistry_noOp() {
    SystemService systemService = mock(SystemService.class);

    assertDoesNotThrow(() -> new VersionMetrics(systemService, (MeterRegistry) null));
  }

  @Test
  @DisplayName("Registration is a no-op when system info is not yet available")
  void nullSystemInfo_noOp() {
    SystemService systemService = mock(SystemService.class);
    when(systemService.getSystemInfo()).thenReturn(null);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    assertDoesNotThrow(() -> new VersionMetrics(systemService, registry));

    assertNull(registry.find(VersionMetrics.VERSION_INFO).gauge());
  }

  @Test
  @DisplayName("Version info gauge exposes version, revision and build time as labels")
  void versionInfoGauge_exposesLabels() {
    SystemService systemService = mock(SystemService.class);
    Date buildTime = new Date(0);
    when(systemService.getSystemInfo())
        .thenReturn(
            SystemInfo.builder()
                .version("2.42.0")
                .revision("abcdef1")
                .buildTime(buildTime)
                .build());
    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    new VersionMetrics(systemService, registry);

    assertEquals(
        1.0,
        registry
            .get(VersionMetrics.VERSION_INFO)
            .tags(
                "version",
                "2.42.0",
                "revision",
                "abcdef1",
                "build_time",
                buildTime.toInstant().toString())
            .gauge()
            .value());
  }

  @Test
  @DisplayName("Metric name survives Prometheus's naming sanitization unchanged")
  void meterName_survivesScrape() {
    SystemService systemService = mock(SystemService.class);
    when(systemService.getSystemInfo())
        .thenReturn(
            SystemInfo.builder()
                .version("2.42.0")
                .revision("abcdef1")
                .buildTime(new Date(0))
                .build());
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    new VersionMetrics(systemService, registry);
    String scrape = registry.scrape();

    assertTrue(scrape.contains("dhis2_version_info"), scrape);
    assertTrue(scrape.contains("2.42.0"), scrape);
    assertTrue(scrape.contains("abcdef1"), scrape);
  }
}
