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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.auth.ApiHeadersAuthScheme;
import org.hisp.dhis.common.auth.ApiQueryParamsAuthScheme;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
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

    private static GenericContainer<?> mockServerContainer;
    private MockServerClient mockServerClient;

    @BeforeAll
    public static void beforeAll() {
      mockServerContainer =
          new GenericContainer<>("mockserver/mockserver")
              .waitingFor(new HttpWaitStrategy().forStatusCode(404))
              .withExposedPorts(1080);
      mockServerContainer.start();
    }

    @BeforeEach
    public void beforeEach() {
      mockServerClient =
          new MockServerClient("localhost", mockServerContainer.getFirstMappedPort());
    }

    @AfterEach
    public void afterEach() {
      mockServerClient.reset();
    }

    @Test
    public void testRunRouteWhenResponseDurationExceedsRouteResponseTimeout()
        throws JsonProcessingException {
      mockServerClient
          .when(request().withPath("/"))
          .respond(
              org.mockserver.model.HttpResponse.response("{}").withDelay(TimeUnit.SECONDS, 10));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + mockServerContainer.getFirstMappedPort());
      route.put("responseTimeout", 5);

      HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
      HttpResponse runHttpResponse =
          GET(
              "/routes/{id}/run",
              postHttpResponse.content().get("response.uid").as(JsonString.class).string());

      assertStatus(HttpStatus.GATEWAY_TIMEOUT, runHttpResponse);
    }

    @Test
    public void testRunRouteWhenResponseDurationDoesNotExceedRouteResponseTimeout()
        throws JsonProcessingException {
      mockServerClient
          .when(request().withPath("/"))
          .respond(org.mockserver.model.HttpResponse.response("{}"));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + mockServerContainer.getFirstMappedPort());
      route.put("responseTimeout", 5);

      HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
      HttpResponse runHttpResponse =
          GET(
              "/routes/{id}/run",
              postHttpResponse.content().get("response.uid").as(JsonString.class).string());

      assertStatus(HttpStatus.OK, runHttpResponse);
    }

    @Test
    public void testRunRouteWhenResponseIsHttpError() throws JsonProcessingException {
      mockServerClient
          .when(request().withPath("/"))
          .respond(
              org.mockserver.model.HttpResponse.response(
                      jsonMapper.writeValueAsString(Map.of("message", "not found")))
                  .withStatusCode(404));

      Map<String, Object> route = new HashMap<>();
      route.put("name", "route-under-test");
      route.put("url", "http://localhost:" + mockServerContainer.getFirstMappedPort());

      HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
      HttpResponse runHttpResponse =
          GET(
              "/routes/{id}/run",
              postHttpResponse.content().get("response.uid").as(JsonString.class).string());

      assertStatus(HttpStatus.NOT_FOUND, runHttpResponse);
      assertEquals("not found", runHttpResponse.error().getMessage());
    }
  }

  @Test
  void testRunRouteGivenApiQueryParamsAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-query-params", "queryParams", Map.of("token", "foo")));
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    HttpResponse runHttpResponse =
        GET(
            "/routes/{id}/run",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());

    assertStatus(HttpStatus.OK, runHttpResponse);
    assertEquals("John Doe", runHttpResponse.content().get("name").as(JsonString.class).string());
    assertEquals(
        "token=foo", runHttpResponse.content().get("queryString").as(JsonString.class).string());
  }

  @Test
  void testRunRouteGivenApiHeadersAuthScheme() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("auth", Map.of("type", "api-headers", "headers", Map.of("X-API-KEY", "foo")));
    route.put("url", "http://stub");

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    HttpResponse runHttpResponse =
        GET(
            "/routes/{id}/run",
            postHttpResponse.content().get("response.uid").as(JsonString.class).string());

    assertStatus(HttpStatus.OK, runHttpResponse);
    assertEquals("John Doe", runHttpResponse.content().get("name").as(JsonString.class).string());
    assertEquals(
        "foo",
        runHttpResponse
            .content()
            .get("headers")
            .asObject()
            .get("X-API-KEY")
            .as(JsonString.class)
            .string());
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
    route.put("responseTimeout", ThreadLocalRandom.current().nextInt(61, Integer.MAX_VALUE));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CONFLICT, postHttpResponse);
  }

  @Test
  void testAddRouteGivenResponseTimeoutLessThanMin() throws JsonProcessingException {
    Map<String, Object> route = new HashMap<>();
    route.put("name", "route-under-test");
    route.put("url", "http://stub");
    route.put("responseTimeout", ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, 1));

    HttpResponse postHttpResponse = POST("/routes", jsonMapper.writeValueAsString(route));
    assertStatus(HttpStatus.CONFLICT, postHttpResponse);
  }
}
