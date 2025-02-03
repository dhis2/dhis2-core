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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.common.auth.ApiHeadersAuthScheme;
import org.hisp.dhis.common.auth.ApiQueryParamsAuthScheme;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.route.Route;
import org.hisp.dhis.route.RouteService;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Transactional
class RouteControllerTest extends DhisControllerIntegrationTest {

  @Autowired private RouteService service;

  @Autowired private ObjectMapper jsonMapper;

  @Test
  void testRunRouteGivenApiQueryParamsAuthScheme()
      throws JsonProcessingException, MalformedURLException {
    ArgumentCaptor<String> urlArgumentCaptor = ArgumentCaptor.forClass(String.class);

    RestTemplate mockRestTemplate = mock(RestTemplate.class);
    when(mockRestTemplate.exchange(
            urlArgumentCaptor.capture(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            any(Class.class)))
        .thenReturn(
            new ResponseEntity<>(
                jsonMapper.writeValueAsString(Map.of("name", "John Doe")), HttpStatus.OK));
    service.setRestTemplate(mockRestTemplate);

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
    assertEquals("John Doe", runHttpResponse.content().get("name").as(JsonString.class).string());

    assertEquals("token=foo", new URL(urlArgumentCaptor.getValue()).getQuery());
  }

  @Test
  void testRunRouteGivenApiHeadersAuthScheme() throws JsonProcessingException {
    ArgumentCaptor<HttpEntity<?>> httpEntityArgumentCaptor =
        ArgumentCaptor.forClass(HttpEntity.class);

    RestTemplate mockRestTemplate = mock(RestTemplate.class);
    when(mockRestTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            httpEntityArgumentCaptor.capture(),
            any(Class.class)))
        .thenReturn(
            new ResponseEntity<>(
                jsonMapper.writeValueAsString(Map.of("name", "John Doe")), HttpStatus.OK));
    service.setRestTemplate(mockRestTemplate);

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
    assertEquals("John Doe", runHttpResponse.content().get("name").as(JsonString.class).string());

    HttpEntity<?> capturedHttpEntity = httpEntityArgumentCaptor.getValue();
    HttpHeaders headers = capturedHttpEntity.getHeaders();
    assertEquals("foo", headers.get("X-API-KEY").get(0));
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
}
