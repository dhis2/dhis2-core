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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockserver.model.HttpRequest.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.auth.ApiHeadersAuthScheme;
import org.hisp.dhis.common.auth.ApiQueryParamsAuthScheme;
import org.hisp.dhis.http.HttpMethod;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.route.RouteService;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.mock.web.MockHttpServletRequest;
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
@ContextConfiguration(classes = {RouteControllerTest.ClientHttpConnectorTestConfig.class})
class RouteControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private RouteService service;

  @Autowired private ObjectMapper jsonMapper;

  @Autowired private ClientHttpConnector clientHttpConnector;

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

  @Transactional
  @Nested
  public class IntegrationTest extends PostgresControllerIntegrationTestBase {

    private static GenericContainer<?> routeTargetMockServerContainer;
    private MockServerClient routeTargetMockServerClient;

    @BeforeAll
    public static void beforeAll() {
      routeTargetMockServerContainer =
          new GenericContainer<>("mockserver/mockserver")
              .waitingFor(new HttpWaitStrategy().forStatusCode(404))
              .withExposedPorts(1080);
      routeTargetMockServerContainer.start();
    }

    @BeforeEach
    public void beforeEach() {
      routeTargetMockServerClient =
          new MockServerClient("localhost", routeTargetMockServerContainer.getFirstMappedPort());
    }

    @AfterEach
    public void afterEach() {
      routeTargetMockServerClient.reset();
    }

    @Test
    void testRunRouteWhenResponseDurationExceedsRouteResponseTimeout()
        throws JsonProcessingException {
      routeTargetMockServerClient
          .when(request().withPath("/"))
          .respond(
              org.mockserver.model.HttpResponse.response("{}").withDelay(TimeUnit.SECONDS, 10));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + routeTargetMockServerContainer.getFirstMappedPort());
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
      routeTargetMockServerClient
          .when(request().withPath("/"))
          .respond(org.mockserver.model.HttpResponse.response("{}"));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + routeTargetMockServerContainer.getFirstMappedPort());
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
      routeTargetMockServerClient
          .when(request().withPath("/"))
          .respond(
              org.mockserver.model.HttpResponse.response(
                      jsonMapper.writeValueAsString(Map.of("message", "not found")))
                  .withStatusCode(404));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + routeTargetMockServerContainer.getFirstMappedPort());

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
  void testRunRouteGivenApiQueryParamsAuthScheme()
      throws JsonProcessingException, UnsupportedEncodingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-query-params", "queryParams", Map.of("token", "foo")));
    route.put("url", "http://stub");

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
    route.put("url", "http://stub");

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
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CREATED, postHttpResponse);
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
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CREATED, postHttpResponse);
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
    route.put("url", "http://stub");
    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(61, Integer.MAX_VALUE));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CONFLICT, postHttpResponse);
  }

  @Test
  void testAddRouteGivenResponseTimeoutLessThanMin() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "http://stub");
    route.put("responseTimeoutSeconds", ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 1));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CONFLICT, postHttpResponse);
  }

  @Test
  void testUpdateRouteGivenResponseTimeoutGreaterThanMax() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "http://stub");

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
    route.put("url", "http://stub");

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
