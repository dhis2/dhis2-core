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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;

import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.test.config.PostgresDhisConfigurationProvider;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.BinaryBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

@Transactional
@ContextConfiguration(
    classes = {SendUsageMetricsCheckJobTest.DhisConfigurationProviderTestConfig.class})
class SendUsageMetricsCheckJobTest extends PostgresIntegrationTestBase {

  private static GenericContainer<?> otelCollectorMockServerContainer;
  private static MockServerClient otelCollectorMockServerClient;

  @Autowired private SendUsageMetricsCheckJob sendUsageMetricsCheckJob;

  @Autowired private UsageMetricsConsentStore usageMetricsConsentStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  public static class DhisConfigurationProviderTestConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      Properties override = new Properties();
      override.put(
          ConfigurationKey.USAGE_METRICS_ENDPIONT.getKey(),
          "http://localhost:" + otelCollectorMockServerContainer.getFirstMappedPort());

      PostgresDhisConfigurationProvider postgresDhisConfigurationProvider =
          new PostgresDhisConfigurationProvider(null);
      postgresDhisConfigurationProvider.addProperties(override);
      return postgresDhisConfigurationProvider;
    }
  }

  @BeforeAll
  static void beforeAll() {
    otelCollectorMockServerContainer =
        new GenericContainer<>("mockserver/mockserver")
            .waitingFor(new HttpWaitStrategy().forStatusCode(404))
            .withExposedPorts(1080);
    otelCollectorMockServerContainer.start();
    otelCollectorMockServerClient =
        new MockServerClient("localhost", otelCollectorMockServerContainer.getFirstMappedPort());
  }

  @BeforeEach
  void beforeEach() {
    sendUsageMetricsCheckJob.closeMetricsExporter();
    otelCollectorMockServerClient.reset();
    otelCollectorMockServerClient
        .when(request().withPath("/v1/metrics"))
        .respond(org.mockserver.model.HttpResponse.response().withStatusCode(200));
  }

  @Test
  void testExecuteSchedulesRegularMetricsExportWhenSendUsageMetricsConsentIsTrue()
      throws InvalidProtocolBufferException, ParseException {
    UsageMetricsConsent usageMetricsConsent = new UsageMetricsConsent();
    usageMetricsConsent.setDbSystemIdentifier(
        jdbcTemplate
            .queryForList("SELECT system_identifier FROM pg_control_system()")
            .get(0)
            .get("system_identifier")
            .toString());
    usageMetricsConsent.setConsent(true);
    usageMetricsConsentStore.save(usageMetricsConsent);

    sendUsageMetricsCheckJob.setExportIntervalSeconds(1);
    sendUsageMetricsCheckJob.execute(null, null);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> otelCollectorMockServerClient.verify(request().withPath("/v1/metrics")));

    byte[] value =
        ((BinaryBody)
                otelCollectorMockServerClient
                    .retrieveRecordedRequests(request().withPath("/v1/metrics"))[0].getBody())
            .getValue();
    ExportMetricsServiceRequest exportMetricsServiceRequest =
        ExportMetricsServiceRequest.parseFrom(value);
    List<ResourceMetrics> resourceMetricsList =
        exportMetricsServiceRequest.getResourceMetricsList();

    ScopeMetrics scopeMetrics = resourceMetricsList.get(0).getScopeMetrics(0);
    assertEquals(12, scopeMetrics.getMetricsCount());

    assertBuildInfoMetric(scopeMetrics.getMetrics(0));
    assertCoreAppsMetric(scopeMetrics.getMetrics(1));
    assertDataSummaryMetrics(scopeMetrics);
    assertEnvInfoMetric(scopeMetrics.getMetrics(5));
  }

  @Test
  void testExecuteDoesNotScheduleRegularMetricsExportWhenSendUsageMetricsConsentIsMissing() {
    sendUsageMetricsCheckJob.setExportIntervalSeconds(1);
    sendUsageMetricsCheckJob.execute(null, null);
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThrows(
                    AssertionError.class,
                    () -> otelCollectorMockServerClient.verify(request().withPath("/v1/metrics"))));
  }

  @Test
  void testExecuteDoesNotScheduleRegularMetricsExportWhenSendUsageMetricsConsentIsFalse() {
    UsageMetricsConsent usageMetricsConsent = new UsageMetricsConsent();
    usageMetricsConsent.setDbSystemIdentifier(
        jdbcTemplate
            .queryForList("SELECT system_identifier FROM pg_control_system()")
            .get(0)
            .get("system_identifier")
            .toString());
    usageMetricsConsent.setConsent(false);
    usageMetricsConsentStore.save(usageMetricsConsent);

    sendUsageMetricsCheckJob.setExportIntervalSeconds(1);
    sendUsageMetricsCheckJob.execute(null, null);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThrows(
                    AssertionError.class,
                    () -> otelCollectorMockServerClient.verify(request().withPath("/v1/metrics"))));
  }

  @Test
  void
      testExecuteRemovesConsentWhenDbSystemIdentifierAtTimeOfConsentIsDifferentThanCurrentDbSystemIdentifier() {
    UsageMetricsConsent usageMetricsConsent = new UsageMetricsConsent();
    usageMetricsConsent.setDbSystemIdentifier("abc");
    usageMetricsConsent.setConsent(true);
    usageMetricsConsentStore.save(usageMetricsConsent);

    sendUsageMetricsCheckJob.setExportIntervalSeconds(1);
    sendUsageMetricsCheckJob.execute(null, null);
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThrows(
                    AssertionError.class,
                    () -> otelCollectorMockServerClient.verify(request().withPath("/v1/metrics"))));

    assertTrue(usageMetricsConsentStore.getAll().isEmpty());
  }

  private void assertBuildInfoMetric(Metric buildInfoMetrics) throws ParseException {
    assertEquals("build", buildInfoMetrics.getName());
    assertEquals("Build Info", buildInfoMetrics.getDescription());
    NumberDataPoint dataPoints = buildInfoMetrics.getSum().getDataPoints(0);

    assertEquals(2, dataPoints.getAttributesList().size());

    assertEquals("build_time", dataPoints.getAttributes(0).getKey());
    DateUtils.parseDate(
        dataPoints.getAttributes(0).getValue().getStringValue(), "EEE MMM d HH:mm:ss zzz yyyy");

    assertEquals("revision", dataPoints.getAttributes(1).getKey());
    assertEquals("abc1234", dataPoints.getAttributes(1).getValue().getStringValue());
  }

  private void assertCoreAppsMetric(Metric coreAppsMetric) {
    assertEquals("core_apps", coreAppsMetric.getName());
    assertEquals("Core Apps", coreAppsMetric.getDescription());
  }

  private void assertDataSummaryMetrics(ScopeMetrics scopeMetrics) {
    assertEquals("dashboards", scopeMetrics.getMetrics(2).getName());
    assertEquals("Dashboards", scopeMetrics.getMetrics(2).getDescription());

    assertEquals("data_sets", scopeMetrics.getMetrics(3).getName());
    assertEquals("Data Sets", scopeMetrics.getMetrics(3).getDescription());

    assertEquals("dhis2_users", scopeMetrics.getMetrics(4).getName());
    assertEquals("DHIS2 Users", scopeMetrics.getMetrics(4).getDescription());
    Gauge dhis2UsersGauge = scopeMetrics.getMetrics(4).getGauge();

    assertEquals(
        "active_users_last_2_days",
        dhis2UsersGauge.getDataPoints(0).getAttributes(1).getValue().getStringValue());
    assertEquals(
        "active_users_last_30_days",
        dhis2UsersGauge.getDataPoints(1).getAttributes(1).getValue().getStringValue());
    assertEquals(
        "active_users_last_7_days",
        dhis2UsersGauge.getDataPoints(2).getAttributes(1).getValue().getStringValue());
    assertEquals(
        "active_users_last_hour",
        dhis2UsersGauge.getDataPoints(3).getAttributes(1).getValue().getStringValue());
    assertEquals(
        "active_users_today",
        dhis2UsersGauge.getDataPoints(4).getAttributes(1).getValue().getStringValue());
    assertEquals(
        "users", dhis2UsersGauge.getDataPoints(5).getAttributes(1).getValue().getStringValue());

    assertEquals("maps", scopeMetrics.getMetrics(6).getName());
    assertEquals("Maps", scopeMetrics.getMetrics(6).getDescription());

    assertEquals("organisation_units", scopeMetrics.getMetrics(7).getName());
    assertEquals("Organisation Units", scopeMetrics.getMetrics(7).getDescription());

    assertEquals("tracked_entities", scopeMetrics.getMetrics(8).getName());
    assertEquals("Tracked Entities", scopeMetrics.getMetrics(8).getDescription());

    assertEquals("tracked_entity_types", scopeMetrics.getMetrics(9).getName());
    assertEquals("Tracked Entity Types", scopeMetrics.getMetrics(9).getDescription());

    assertEquals("tracker_programs", scopeMetrics.getMetrics(10).getName());
    assertEquals("Tracker Programs", scopeMetrics.getMetrics(10).getDescription());

    assertEquals("visualizations", scopeMetrics.getMetrics(11).getName());
    assertEquals("Visualizations", scopeMetrics.getMetrics(11).getDescription());
  }

  private void assertEnvInfoMetric(Metric envMetric) {
    assertEquals("environment", envMetric.getName());
    assertEquals("Environment", envMetric.getDescription());

    NumberDataPoint dataPoints = envMetric.getSum().getDataPoints(0);
    assertEquals(6, dataPoints.getAttributesList().size());

    assertEquals("cpu_cores", dataPoints.getAttributes(0).getKey());
    assertTrue(Integer.parseInt(dataPoints.getAttributes(0).getValue().getStringValue()) > 0);

    assertEquals("java_vendor", dataPoints.getAttributes(1).getKey());
    assertFalse(dataPoints.getAttributes(1).getValue().getStringValue().isEmpty());

    assertEquals("java_version", dataPoints.getAttributes(2).getKey());
    assertFalse(dataPoints.getAttributes(2).getValue().getStringValue().isEmpty());

    assertEquals("jvm_mem_mb_total", dataPoints.getAttributes(3).getKey());
    assertTrue(Integer.parseInt(dataPoints.getAttributes(3).getValue().getStringValue()) > 0);

    assertEquals("os", dataPoints.getAttributes(4).getKey());
    assertFalse(dataPoints.getAttributes(4).getValue().getStringValue().isEmpty());

    assertEquals("postgres_version", dataPoints.getAttributes(5).getKey());
    assertFalse(dataPoints.getAttributes(5).getValue().getStringValue().isEmpty());
  }
}
