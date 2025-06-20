/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockserver.model.HttpRequest.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.auth.ApiHeadersAuthScheme;
import org.hisp.dhis.common.auth.ApiQueryParamsAuthScheme;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpMethod;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.config.PostgresDhisConfigurationProvider;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.client.MockMvcHttpConnector;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

@Transactional
@ContextConfiguration(
    classes = {
      RouteControllerTest.ClientHttpConnectorTestConfig.class,
      RouteControllerTest.DhisConfigurationProviderTestConfig.class
    })
class RouteControllerTest extends PostgresControllerIntegrationTestBase {

  private static GenericContainer<?> tokenMockServerContainer;
  private static MockServerClient tokenMockServerClient;

  @Autowired private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

  @Autowired private ObjectMapper jsonMapper;

  public static class DhisConfigurationProviderTestConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      Properties override = new Properties();
      override.put(ConfigurationKey.ROUTE_REMOTE_SERVERS_ALLOWED.getKey(), "http://*,https://stub");

      PostgresDhisConfigurationProvider postgresDhisConfigurationProvider =
          new PostgresDhisConfigurationProvider(null);
      postgresDhisConfigurationProvider.addProperties(override);
      return postgresDhisConfigurationProvider;
    }
  }

  @Configuration
  public static class ClientHttpConnectorTestConfig {
    @Autowired private ObjectMapper jsonMapper;

    @Autowired private WebApplicationContext webApplicationContext;

    @Bean
    public ClientHttpConnector clientHttpConnector() {
      MockMvc mockMvc =
          MockMvcBuilders.webAppContextSetup(webApplicationContext)
              .addFilter(
                  (request, response, chain) -> {
                    Map<String, String> headers = new HashMap<>();
                    for (String headerName :
                        Collections.list(((MockHttpServletRequest) request).getHeaderNames())) {
                      headers.put(
                          headerName, ((MockHttpServletRequest) request).getHeader(headerName));
                    }

                    String queryString = ((MockHttpServletRequest) request).getQueryString();
                    response.setContentType("application/json");

                    response
                        .getWriter()
                        .write(
                            jsonMapper.writeValueAsString(
                                Map.of(
                                    "name",
                                    "John Doe",
                                    "headers",
                                    headers,
                                    "queryString",
                                    queryString != null ? queryString : "")));
                  })
              .build();
      return new MockMvcHttpConnector(mockMvc);
    }
  }

  @BeforeAll
  static void beforeAll() {
    tokenMockServerContainer =
        new GenericContainer<>("mockserver/mockserver")
            .waitingFor(new HttpWaitStrategy().forStatusCode(404))
            .withExposedPorts(1080);
    tokenMockServerContainer.start();
    tokenMockServerClient =
        new MockServerClient("localhost", tokenMockServerContainer.getFirstMappedPort());
  }

  @BeforeEach
  void beforeEach() {
    tokenMockServerClient.reset();
  }

  @AfterAll
  static void afterAll() {
    tokenMockServerContainer.stop();
  }

  @Transactional
  @Nested
  @ContextConfiguration(classes = {RouteControllerTest.DhisConfigurationProviderTestConfig.class})
  class IntegrationTest extends PostgresControllerIntegrationTestBase {

    private static GenericContainer<?> upstreamMockServerContainer;
    private MockServerClient upstreamMockServerClient;

    @BeforeAll
    public static void beforeAll() {
      upstreamMockServerContainer =
          new GenericContainer<>("mockserver/mockserver")
              .waitingFor(new HttpWaitStrategy().forStatusCode(404))
              .withExposedPorts(1080);
      upstreamMockServerContainer.start();
    }

    @BeforeEach
    public void beforeEach() {
      upstreamMockServerClient =
          new MockServerClient("localhost", upstreamMockServerContainer.getFirstMappedPort());
    }

    @AfterEach
    void afterEach() {
      upstreamMockServerClient.reset();
    }

    @AfterAll
    static void afterAll() {
      upstreamMockServerContainer.stop();
    }

    @Test
    void testRunRouteAsyncRequestTimeoutIsNotDefault() throws JsonProcessingException {
      upstreamMockServerClient
          .when(request().withPath("/"))
          .respond(org.mockserver.model.HttpResponse.response("{}"));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + upstreamMockServerContainer.getFirstMappedPort());

      HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

      long asyncRequestTimeout =
          webRequestWithMvcResult(
                  buildMockRequest(
                      HttpMethod.GET,
                      "/routes/"
                          + postHttpResponse
                              .content()
                              .get("response.uid")
                              .as(JsonString.class)
                              .string()
                          + "/run",
                      new ArrayList<>(),
                      "application/json",
                      null))
              .getRequest()
              .getAsyncContext()
              .getTimeout();

      assertEquals(Duration.ofMinutes(5).toMillis(), asyncRequestTimeout);
    }

    @Test
    void testRunRouteWhenResponseDurationExceedsRouteResponseTimeout()
        throws JsonProcessingException {
      upstreamMockServerClient
          .when(request().withPath("/"))
          .respond(
              org.mockserver.model.HttpResponse.response("{}").withDelay(TimeUnit.SECONDS, 10));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + upstreamMockServerContainer.getFirstMappedPort());
      route.put("responseTimeoutSeconds", 5);

      HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
      MvcResult mvcResult =
          webRequestWithAsyncMvcResult(
              buildMockRequest(
                  HttpMethod.GET,
                  "/routes/"
                      + postHttpResponse.content().get("response.uid").as(JsonString.class).string()
                      + "/run",
                  new ArrayList<>(),
                  "application/json",
                  null));

      assertEquals(504, mvcResult.getResponse().getStatus());
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
      MvcResult mvcResult =
          webRequestWithAsyncMvcResult(
              buildMockRequest(
                  HttpMethod.GET,
                  "/routes/"
                      + postHttpResponse.content().get("response.uid").as(JsonString.class).string()
                      + "/run",
                  new ArrayList<>(),
                  "application/json",
                  null));

      assertEquals(200, mvcResult.getResponse().getStatus());
    }

    @Test
    void testRunRouteWhenResponseIsHttpError()
        throws JsonProcessingException, UnsupportedEncodingException {
      upstreamMockServerClient
          .when(request().withPath("/"))
          .respond(
              org.mockserver.model.HttpResponse.response(
                      jsonMapper.writeValueAsString(Map.of("message", "not found")))
                  .withStatusCode(404));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + upstreamMockServerContainer.getFirstMappedPort());

      HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
      MvcResult mvcResult =
          webRequestWithAsyncMvcResult(
              buildMockRequest(
                  HttpMethod.GET,
                  "/routes/"
                      + postHttpResponse.content().get("response.uid").as(JsonString.class).string()
                      + "/run",
                  new ArrayList<>(),
                  "application/json",
                  null));

      assertEquals(404, mvcResult.getResponse().getStatus());
      JsonObject responseBody =
          JsonValue.of(mvcResult.getResponse().getContentAsString()).asObject();
      assertEquals("not found", responseBody.get("message").as(JsonString.class).string());
    }
  }

  @Test
  void testRunRouteGivenOAuth2ClientCredentialsAuthSchemeWhenTokenEndpointReturnsUnauthorizedError()
      throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put(
        "auth",
        Map.of(
            "type",
            "oauth2-client-credentials",
            "clientId",
            "alice",
            "clientSecret",
            "passw0rd",
            "tokenUri",
            "http://localhost:" + tokenMockServerContainer.getFirstMappedPort() + "/token"));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    MockServerClient routeTargetMockServerClient =
        new MockServerClient("localhost", tokenMockServerContainer.getFirstMappedPort());
    routeTargetMockServerClient
        .when(request().withPath("/token"))
        .respond(org.mockserver.model.HttpResponse.response().withStatusCode(401));

    MvcResult mvcResult =
        webRequestWithMvcResult(
            buildMockRequest(
                HttpMethod.GET,
                "/routes/"
                    + postHttpResponse.content().get("response.uid").as(JsonString.class).string()
                    + "/run",
                new ArrayList<>(),
                "application/json",
                null));

    assertEquals(502, mvcResult.getResponse().getStatus());
  }

  @Test
  void testRunRouteGivenOAuth2ClientCredentialsAuthScheme()
      throws JsonProcessingException, UnsupportedEncodingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    String tokenUri =
        "http://localhost:" + tokenMockServerContainer.getFirstMappedPort() + "/token";
    route.put(
        "auth",
        Map.of(
            "type",
            "oauth2-client-credentials",
            "clientId",
            "john",
            "clientSecret",
            "passw0rd",
            "tokenUri",
            tokenUri));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    tokenMockServerClient
        .when(request().withPath("/token"))
        .respond(
            org.mockserver.model.HttpResponse.response(
                    """
                        { "access_token":"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
                          "token_type":"Bearer",
                          "expires_in":3600,
                          "refresh_token":"IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk",
                          "scope":"create"}""")
                .withContentType(MediaType.APPLICATION_JSON)
                .withStatusCode(200));

    assertNull(oAuth2AuthorizedClientService.loadAuthorizedClient("john:" + tokenUri, "anonymous"));

    MvcResult mvcResult =
        webRequestWithAsyncMvcResult(
            buildMockRequest(
                HttpMethod.GET,
                "/routes/"
                    + postHttpResponse.content().get("response.uid").as(JsonString.class).string()
                    + "/run",
                new ArrayList<>(),
                "application/json",
                null));

    assertEquals(200, mvcResult.getResponse().getStatus());
    JsonObject responseBody = JsonValue.of(mvcResult.getResponse().getContentAsString()).asObject();

    assertEquals("John Doe", responseBody.get("name").as(JsonString.class).string());
    assertEquals(
        "Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
        responseBody.get("headers").asObject().get("Authorization").as(JsonString.class).string());

    OAuth2AuthorizedClient oAuth2AuthorizedClient =
        oAuth2AuthorizedClientService.loadAuthorizedClient("john:" + tokenUri, "anonymous");
    assertEquals(
        "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
        oAuth2AuthorizedClient.getAccessToken().getTokenValue());
  }

  @Test
  void testDeleteRouteRemovesOAuth2AuthorizedClientWhenAuthSchemeIsOAuth2ClientCredentials()
      throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    String tokenUri =
        "http://localhost:" + tokenMockServerContainer.getFirstMappedPort() + "/token";
    route.put(
        "auth",
        Map.of(
            "type",
            "oauth2-client-credentials",
            "clientId",
            "tom",
            "clientSecret",
            "passw0rd",
            "tokenUri",
            tokenUri));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    tokenMockServerClient
        .when(request().withPath("/token"))
        .respond(
            org.mockserver.model.HttpResponse.response(
                    """
                                        { "access_token":"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
                                          "token_type":"Bearer",
                                          "expires_in":3600,
                                          "refresh_token":"IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk",
                                          "scope":"create"}""")
                .withContentType(MediaType.APPLICATION_JSON)
                .withStatusCode(200));

    String routeId = postHttpResponse.content().get("response.uid").as(JsonString.class).string();

    assertNull(oAuth2AuthorizedClientService.loadAuthorizedClient("tom:" + tokenUri, "anonymous"));
    webRequestWithAsyncMvcResult(
        buildMockRequest(
            HttpMethod.GET,
            "/routes/" + routeId + "/run",
            new ArrayList<>(),
            "application/json",
            null));

    assertNotNull(
        oAuth2AuthorizedClientService.loadAuthorizedClient("tom:" + tokenUri, "anonymous"));
    DELETE("/routes/" + routeId);
    assertNull(oAuth2AuthorizedClientService.loadAuthorizedClient("tom:" + tokenUri, "anonymous"));
  }

  @Test
  void testUpdateRouteRemovesOAuth2AuthorizedClientWhenAuthSchemeIsOAuth2ClientCredentials()
      throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    String tokenUri =
        "http://localhost:" + tokenMockServerContainer.getFirstMappedPort() + "/token";
    route.put(
        "auth",
        Map.of(
            "type",
            "oauth2-client-credentials",
            "clientId",
            "mary",
            "clientSecret",
            "passw0rd",
            "tokenUri",
            tokenUri));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    tokenMockServerClient
        .when(request().withPath("/token"))
        .respond(
            org.mockserver.model.HttpResponse.response(
                    """
                                        { "access_token":"MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
                                          "token_type":"Bearer",
                                          "expires_in":3600,
                                          "refresh_token":"IwOGYzYTlmM2YxOTQ5MGE3YmNmMDFkNTVk",
                                          "scope":"create"}""")
                .withContentType(MediaType.APPLICATION_JSON)
                .withStatusCode(200));

    String routeId = postHttpResponse.content().get("response.uid").as(JsonString.class).string();

    assertNull(oAuth2AuthorizedClientService.loadAuthorizedClient("mary:" + tokenUri, "anonymous"));
    webRequestWithAsyncMvcResult(
        buildMockRequest(
            HttpMethod.GET,
            "/routes/" + routeId + "/run",
            new ArrayList<>(),
            "application/json",
            null));

    assertNotNull(
        oAuth2AuthorizedClientService.loadAuthorizedClient("mary:" + tokenUri, "anonymous"));
    PUT("/routes/" + routeId, jsonMapper.writeValueAsString(route));
    assertNull(oAuth2AuthorizedClientService.loadAuthorizedClient("mary:" + tokenUri, "anonymous"));
  }

  @Test
  void testRunRouteGivenApiQueryParamsAuthScheme()
      throws JsonProcessingException, UnsupportedEncodingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-query-params", "queryParams", Map.of("token", "foo")));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    MvcResult mvcResult =
        webRequestWithAsyncMvcResult(
            buildMockRequest(
                HttpMethod.GET,
                "/routes/"
                    + postHttpResponse.content().get("response.uid").as(JsonString.class).string()
                    + "/run",
                new ArrayList<>(),
                "application/json",
                null));

    assertEquals(200, mvcResult.getResponse().getStatus());
    JsonObject responseBody = JsonValue.of(mvcResult.getResponse().getContentAsString()).asObject();

    assertEquals("John Doe", responseBody.get("name").as(JsonString.class).string());
    assertEquals("token=foo", responseBody.get("queryString").as(JsonString.class).string());
  }

  @Test
  void testRunRouteGivenApiHeadersAuthScheme()
      throws JsonProcessingException, UnsupportedEncodingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-headers", "headers", Map.of("X-API-KEY", "foo")));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    MvcResult mvcResult =
        webRequestWithAsyncMvcResult(
            buildMockRequest(
                HttpMethod.GET,
                "/routes/"
                    + postHttpResponse.content().get("response.uid").as(JsonString.class).string()
                    + "/run",
                new ArrayList<>(),
                "application/json",
                null));

    assertEquals(200, mvcResult.getResponse().getStatus());
    JsonObject responseBody = JsonValue.of(mvcResult.getResponse().getContentAsString()).asObject();
    assertEquals("John Doe", responseBody.asObject().get("name").as(JsonString.class).string());
    assertEquals(
        "foo",
        responseBody.get("headers").asObject().get("X-API-KEY").as(JsonString.class).string());
  }

  @Test
  void testAddRouteGivenApiHeadersAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-headers", "headers", Map.of("X-API-KEY", "foo")));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CREATED, postHttpResponse);
  }

  @Test
  void testGetRouteGivenApiHeadersAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-headers", "headers", Map.of("X-API-KEY", "foo")));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    HttpResponse getHttpResponse =
        GET(
            "/routes/{id}",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());
    assertStatus(HttpStatus.OK, getHttpResponse);
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
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-query-params", "queryParams", Map.of("token", "foo")));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CREATED, postHttpResponse);
  }

  @Test
  void testGetRouteGivenApiQueryParamsAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-query-params", "queryParams", Map.of("token", "foo")));
    route.put("url", "https://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));

    HttpResponse getHttpResponse =
        GET(
            "/routes/{id}",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());
    assertStatus(HttpStatus.OK, getHttpResponse);
    assertEquals(
        ApiQueryParamsAuthScheme.API_QUERY_PARAMS_TYPE,
        getHttpResponse.content().get("auth.type").as(JsonString.class).string());
    assertFalse(getHttpResponse.content().get("auth").as(JsonObject.class).has("queryParams"));
  }

  @Test
  void testAddRouteGivenResponseTimeoutGreaterThanMax() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "https://stub");
    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(61, Integer.MAX_VALUE));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CONFLICT, postHttpResponse);
  }

  @Test
  void testAddRouteGivenResponseTimeoutLessThanMin() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "https://stub");
    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 1));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CONFLICT, postHttpResponse);
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

    assertStatus(HttpStatus.CONFLICT, updateHttpResponse);
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

    assertStatus(HttpStatus.CONFLICT, updateHttpResponse);
  }
}
