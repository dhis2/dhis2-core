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
package org.hisp.dhis.config;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.common.internal.OtelVersion;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.usagemetrics.UsageMetricsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OtelSdkConfig {

  private final DhisConfigurationProvider dhisConfig;
  private final UsageMetricsService usageMetricsService;

  @Bean
  public OpenTelemetrySdk otelSdk(SystemService systemService) {
    SystemInfo systemInfo = systemService.getSystemInfo();
    MetricExporter metricExporter =
        OtlpHttpMetricExporter.builder()
            .setEndpoint(dhisConfig.getProperty(ConfigurationKey.USAGE_METRICS_ENDPIONT))
            .build();
    SdkMeterProvider sdkMeterProvider =
        SdkMeterProvider.builder()
            .setResource(
                Resource.builder()
                    .put("service.namespace", "DHIS2")
                    .put("service.name", "dhis2-core")
                    .put("service.version", systemInfo.getVersion())
                    .put(
                        "service.instance.id",
                        DigestUtils.md5Hex(usageMetricsService.getImplementationId()))
                    .put("telemetry.sdk.language", "java")
                    .put("telemetry.sdk.name", "opentelemetry")
                    .put("telemetry.sdk.version", OtelVersion.VERSION)
                    .build())
            .registerMetricReader(
                new MetricReader() {
                  private CollectionRegistration collectionRegistration =
                      CollectionRegistration.noop();

                  @Override
                  public void register(CollectionRegistration registration) {
                    this.collectionRegistration = registration;
                  }

                  @Override
                  public CompletableResultCode forceFlush() {
                    Collection<MetricData> metricData = collectionRegistration.collectAllMetrics();
                    if (metricData.isEmpty()) {
                      return new CompletableResultCode().succeed();
                    } else {
                      return metricExporter.export(metricData);
                    }
                  }

                  @Override
                  public CompletableResultCode shutdown() {
                    return metricExporter.shutdown();
                  }

                  @Override
                  public AggregationTemporality getAggregationTemporality(
                      InstrumentType instrumentType) {
                    return metricExporter.getAggregationTemporality(instrumentType);
                  }

                  @Override
                  public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
                    return metricExporter.getDefaultAggregation(instrumentType);
                  }

                  @Override
                  public MemoryMode getMemoryMode() {
                    return metricExporter.getMemoryMode();
                  }
                })
            .build();

    OpenTelemetrySdk otelSdk =
        OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProvider).build();
    Runtime.getRuntime().addShutdownHook(new Thread(sdkMeterProvider::shutdown));

    return otelSdk;
  }
}
