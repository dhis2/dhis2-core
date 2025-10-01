/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.awaitility.Awaitility;
import org.hisp.dhis.common.auth.ApiHeadersAuthScheme;
import org.hisp.dhis.common.auth.ApiQueryParamsAuthScheme;
import org.hisp.dhis.config.IntegrationTestConfig;
import org.hisp.dhis.config.PostgresDhisConfigurationProvider;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.route.Route;
import org.hisp.dhis.route.RouteService;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockserver.client.MockServerClient;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

@Transactional
@ContextConfiguration(classes = {RouteControllerTest.DhisConfigurationProviderTestConfig.class})
class RouteControllerTest extends DhisControllerIntegrationTest {

  private static GenericContainer<?> upstreamMockServerContainer;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private RouteService routeService;

  @Autowired private ObjectMapper jsonMapper;

  private MockServerClient upstreamMockServerClient;

  public static class DhisConfigurationProviderTestConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      Properties override = new Properties();
      override.put(ConfigurationKey.AUDIT_DATABASE.getKey(), "on");

      IntegrationTestConfig integrationTestConfig = new IntegrationTestConfig();
      PostgresDhisConfigurationProvider postgresDhisConfigurationProvider =
          (PostgresDhisConfigurationProvider) integrationTestConfig.dhisConfigurationProvider();
      postgresDhisConfigurationProvider.addProperties(override);

      return postgresDhisConfigurationProvider;
    }
  }

  @BeforeAll
  public static void beforeAll() {
    upstreamMockServerContainer =
        new GenericContainer<>("mockserver/mockserver")
            .waitingFor(new HttpWaitStrategy().forStatusCode(404))
            .withExposedPorts(1080);
    upstreamMockServerContainer.start();
  }

  @AfterAll
  public static void afterAll() {
    upstreamMockServerContainer.stop();
  }

  @Override
  public void integrationTestBefore() {
    routeService.postConstruct();
    upstreamMockServerClient =
        new MockServerClient("localhost", upstreamMockServerContainer.getFirstMappedPort());
    upstreamMockServerClient.reset();
  }

  @Test
  void testRunRouteIsAudited() throws JsonProcessingException {
    upstreamMockServerClient
        .when(request().withPath("/testRunRouteIsAudited"))
        .respond(org.mockserver.model.HttpResponse.response("{}"));

    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put(
        "url", "http://localhost:" + upstreamMockServerContainer.getFirstMappedPort() + "/**");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    HttpResponse runHttpResponse =
        GET(
            "/routes/{id}/run/testRunRouteIsAudited",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());
    assertStatus(org.hisp.dhis.web.HttpStatus.OK, runHttpResponse);

    Awaitility.await()
        .untilAsserted(
            () -> {
              List<Map<String, Object>> auditEntries =
                  jdbcTemplate.queryForList("SELECT * FROM audit ORDER BY createdAt DESC");
              assertFalse(auditEntries.isEmpty());
              assertEquals("API", auditEntries.get(0).get("auditscope"));
              Map<String, String> auditEntry =
                  jsonMapper.readValue(
                      ((PGobject) auditEntries.get(0).get("attributes")).getValue(), Map.class);
              assertEquals("Route Run", auditEntry.get("source"));
              assertEquals(
                  "http://localhost:"
                      + upstreamMockServerContainer.getFirstMappedPort()
                      + "/testRunRouteIsAudited",
                  auditEntry.get("upstreamUrl"));
            });
  }

  @Test
  void testRunRouteGivenApiQueryParamsAuthScheme() throws IOException {
    CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse mockHttpResponse = mock(CloseableHttpResponse.class);

    when(mockHttpResponse.getAllHeaders()).thenReturn(new org.apache.http.Header[] {});
    when(mockHttpResponse.getStatusLine())
        .thenReturn(
            new BasicStatusLine(
                new ProtocolVersion("http", 1, 1), org.apache.http.HttpStatus.SC_OK, "ok"));

    ArgumentCaptor<HttpUriRequest> httpUriRequestArgumentCaptor =
        ArgumentCaptor.forClass(HttpUriRequest.class);
    when(mockHttpClient.execute(httpUriRequestArgumentCaptor.capture(), any(HttpContext.class)))
        .thenReturn(mockHttpResponse);

    routeService.setHttpClient(mockHttpClient);

    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-query-params", "queryParams", Map.of("token", "foo")));
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    HttpResponse runHttpResponse =
        GET(
            "/routes/{id}/run",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());

    assertStatus(org.hisp.dhis.web.HttpStatus.OK, runHttpResponse);
    assertEquals("token=foo", httpUriRequestArgumentCaptor.getValue().getURI().getQuery());
  }

  @Test
  void testRunRouteGivenApiHeadersAuthScheme() throws IOException {
    CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse mockHttpResponse = mock(CloseableHttpResponse.class);

    ArgumentCaptor<HttpUriRequest> httpUriRequestArgumentCaptor =
        ArgumentCaptor.forClass(HttpUriRequest.class);
    when(mockHttpResponse.getAllHeaders()).thenReturn(new org.apache.http.Header[] {});
    when(mockHttpResponse.getStatusLine())
        .thenReturn(
            new BasicStatusLine(
                new ProtocolVersion("http", 1, 1), org.apache.http.HttpStatus.SC_OK, "ok"));
    when(mockHttpClient.execute(httpUriRequestArgumentCaptor.capture(), any(HttpContext.class)))
        .thenReturn(mockHttpResponse);

    routeService.setHttpClient(mockHttpClient);

    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-headers", "headers", Map.of("X-API-KEY", "foo")));
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    HttpResponse runHttpResponse =
        GET(
            "/routes/{id}/run",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());

    assertStatus(org.hisp.dhis.web.HttpStatus.OK, runHttpResponse);
    assertEquals(
        "foo", httpUriRequestArgumentCaptor.getValue().getHeaders("X-API-KEY")[0].getValue());
  }

  @Test
  void testAddRouteGivenApiHeadersAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-headers", "headers", Map.of("X-API-KEY", "foo")));
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(org.hisp.dhis.web.HttpStatus.CREATED, postHttpResponse);

    HttpResponse getHttpResponse =
        GET(
            "/routes/{id}",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());
    assertStatus(org.hisp.dhis.web.HttpStatus.OK, getHttpResponse);
    assertNotEquals(
        "foo",
        getHttpResponse.content().get("auth.headers.X-API-KEY").as(JsonString.class).string());
    assertEquals(
        ApiHeadersAuthScheme.API_HEADERS_TYPE,
        getHttpResponse.content().get("auth.type").as(JsonString.class).string());
  }

  @Test
  void testGetRouteGivenApiHeadersAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-headers", "headers", Map.of("X-API-KEY", "foo")));
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    HttpResponse getHttpResponse =
        GET(
            "/routes/{id}",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());
    assertStatus(org.hisp.dhis.web.HttpStatus.OK, getHttpResponse);
    assertNotEquals(
        "foo",
        getHttpResponse.content().get("auth.headers.X-API-KEY").as(JsonString.class).string());
    assertEquals(
        ApiHeadersAuthScheme.API_HEADERS_TYPE,
        getHttpResponse.content().get("auth.type").as(JsonString.class).string());
    assertFalse(getHttpResponse.content().get("auth").as(JsonObject.class).has("headers"));
  }

  @Test
  void testAddRouteGivenApiQueryParamsAuthScheme() throws JsonProcessingException {
    ApiQueryParamsAuthScheme queryParamsAuthScheme = new ApiQueryParamsAuthScheme();
    queryParamsAuthScheme.setQueryParams(Map.of("token", "foo"));

    Route route = new Route();
    route.setName("route-under-test");
    route.setAuth(queryParamsAuthScheme);
    route.setUrl("http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(org.hisp.dhis.web.HttpStatus.CREATED, postHttpResponse);

    HttpResponse getHttpResponse =
        GET(
            "/routes/{id}",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());
    assertStatus(org.hisp.dhis.web.HttpStatus.OK, getHttpResponse);
    assertNotEquals(
        "foo", getHttpResponse.content().get("auth.headers.token").as(JsonString.class).string());
    assertEquals(
        ApiQueryParamsAuthScheme.API_QUERY_PARAMS_TYPE,
        getHttpResponse.content().get("auth.type").as(JsonString.class).string());
  }

  @Test
  void testGetRouteGivenApiQueryParamsAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-query-params", "queryParams", Map.of("token", "foo")));
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    HttpResponse getHttpResponse =
        GET(
            "/routes/{id}",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());
    assertStatus(org.hisp.dhis.web.HttpStatus.OK, getHttpResponse);
    assertEquals(
        ApiQueryParamsAuthScheme.API_QUERY_PARAMS_TYPE,
        getHttpResponse.content().get("auth.type").as(JsonString.class).string());
    assertFalse(getHttpResponse.content().get("auth").as(JsonObject.class).has("queryParams"));
  }

  @Test
  void testRunRouteWhenResponseDurationExceedsRouteResponseTimeout()
      throws JsonProcessingException {
    upstreamMockServerClient
        .when(request().withPath("/"))
        .respond(org.mockserver.model.HttpResponse.response("{}").withDelay(TimeUnit.SECONDS, 20));

    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "http://localhost:" + upstreamMockServerContainer.getFirstMappedPort());
    route.put("responseTimeoutSeconds", 5);

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    HttpResponse runHttpResponse =
        GET(
            "/routes/{id}/run",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());

    assertStatus(org.hisp.dhis.web.HttpStatus.SERVICE_UNAVAILABLE, runHttpResponse);
  }

  @Test
  void testRunRouteWhenResponseDurationDoesNotExceedRouteResponseTimeout()
      throws JsonProcessingException {
    upstreamMockServerClient
        .when(request().withPath("/"))
        .respond(org.mockserver.model.HttpResponse.response("{}"));

    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "http://localhost:" + upstreamMockServerContainer.getFirstMappedPort());
    route.put("responseTimeoutSeconds", 5);

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    HttpResponse runHttpResponse =
        GET(
            "/routes/{id}/run",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());

    assertStatus(org.hisp.dhis.web.HttpStatus.OK, runHttpResponse);
  }

  @Test
  void testAddRouteGivenResponseTimeoutGreaterThanMax() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "https://stub");
    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(61, Integer.MAX_VALUE));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(org.hisp.dhis.web.HttpStatus.CONFLICT, postHttpResponse);
  }

  @Test
  void testAddRouteGivenResponseTimeoutLessThanMin() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "https://stub");
    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 1));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(org.hisp.dhis.web.HttpStatus.CONFLICT, postHttpResponse);
  }

  @Test
  void testUpdateRouteGivenResponseTimeoutGreaterThanMax() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(61, Integer.MAX_VALUE));
    HttpResponse updateHttpResponse =
        PUT(
            "/routes/"
                + postHttpResponse.content().get("response.uid").as(JsonString.class).string(),
            jsonMapper.writeValueAsString(route));

    assertStatus(org.hisp.dhis.web.HttpStatus.CONFLICT, updateHttpResponse);
  }

  @Test
  void testUpdateRouteGivenResponseTimeoutLessThanMin() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 1));
    HttpResponse updateHttpResponse =
        PUT(
            "/routes/"
                + postHttpResponse.content().get("response.uid").as(JsonString.class).string(),
            jsonMapper.writeValueAsString(route));

    assertStatus(org.hisp.dhis.web.HttpStatus.CONFLICT, updateHttpResponse);
  }
}
