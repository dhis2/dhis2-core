/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.auth;

import static org.hisp.dhis.common.network.PortUtil.findAvailablePort;
import static org.hisp.dhis.system.StartupEventPublisher.SERVER_STARTED_LATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.system.util.HttpHeadersBuilder;
import org.hisp.dhis.test.IntegrationTest;
import org.hisp.dhis.webapi.controller.security.LoginRequest;
import org.hisp.dhis.webapi.controller.security.LoginResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Disabled(
    "fails to build in CI with Connections could not be acquired from the underlying database!")
@Slf4j
@IntegrationTest
@ActiveProfiles(profiles = {"test-postgres"})
class AuthTest {
  private static final String POSTGRES_POSTGIS_VERSION = "13-3.4-alpine";
  private static final DockerImageName POSTGIS_IMAGE_NAME =
      DockerImageName.parse("postgis/postgis").asCompatibleSubstituteFor("postgres");
  private static final String POSTGRES_DATABASE_NAME = "dhis";
  private static final String POSTGRES_USERNAME = "dhis";
  private static final String POSTGRES_PASSWORD = "dhis";
  private static PostgreSQLContainer<?> POSTGRES_CONTAINER;
  private static int availablePort;

  @BeforeAll
  static void setup() throws Exception {
    availablePort = findAvailablePort();

    POSTGRES_CONTAINER =
        new PostgreSQLContainer<>(POSTGIS_IMAGE_NAME.withTag(POSTGRES_POSTGIS_VERSION))
            .withDatabaseName(POSTGRES_DATABASE_NAME)
            .withUsername(POSTGRES_USERNAME)
            .withPassword(POSTGRES_PASSWORD)
            .withInitScript("db/extensions.sql")
            .withTmpFs(Map.of("/testtmpfs", "rw"))
            .withEnv("LC_COLLATE", "C");

    POSTGRES_CONTAINER.start();

    createTmpDhisConf();

    System.setProperty("dhis2.home", System.getProperty("java.io.tmpdir"));

    Thread printingHook =
        new Thread(
            () -> {
              log.info("In the middle of a shutdown");
            });
    Runtime.getRuntime().addShutdownHook(printingHook);

    Thread longRunningHook =
        new Thread(
            () -> {
              try {
                System.setProperty("server.port", Integer.toString(availablePort));
                org.hisp.dhis.web.tomcat.Main.main(null);
              } catch (InterruptedException ignored) {
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    longRunningHook.start();

    SERVER_STARTED_LATCH.await();

    log.info("Server started");
  }

  private static void createTmpDhisConf() {
    String jdbcUrl = POSTGRES_CONTAINER.getJdbcUrl();
    log.info("JDBC URL: " + jdbcUrl);
    String multiLineString =
        """
        connection.dialect = org.hibernate.dialect.PostgreSQLDialect
        connection.driver_class = org.postgresql.Driver
        connection.url = %s
        connection.username = dhis
        connection.password = dhis
        # Database schema behavior, can be validate, update, create, create-drop
        connection.schema = update
        system.audit.enabled = false
        """
            .formatted(jdbcUrl);
    try {
      String tmpDir = System.getProperty("java.io.tmpdir");
      Path tmpFilePath = Path.of(tmpDir, "dhis.conf");
      Files.writeString(tmpFilePath, multiLineString, StandardOpenOption.CREATE);
      log.info("File written successfully to " + tmpFilePath);
    } catch (Exception e) {
      log.error("Error creating file", e);
    }
  }

  @Test
  void testLogin() {
    String port = Integer.toString(availablePort);

    RestTemplate restTemplate = new RestTemplate();

    HttpHeadersBuilder headersBuilder = new HttpHeadersBuilder().withContentTypeJson();

    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, headersBuilder.build());

    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login", requestEntity, LoginResponse.class);

    assertNotNull(loginResponse);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    LoginResponse body = loginResponse.getBody();
    assertNotNull(body);
    assertEquals(LoginResponse.STATUS.SUCCESS, body.getLoginStatus());
    HttpHeaders headers = loginResponse.getHeaders();

    assertEquals("/dhis-web-dashboard/", body.getRedirectUrl());

    assertNotNull(headers);
    List<String> cookieHeader = headers.get(HttpHeaders.SET_COOKIE);
    assertNotNull(cookieHeader);
    assertEquals(1, cookieHeader.size());
    String cookie = cookieHeader.get(0);

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set("Cookie", cookie);
    HttpEntity<String> getEntity = new HttpEntity<>("", getHeaders);

    ResponseEntity<JsonNode> getResponse =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/me", HttpMethod.GET, getEntity, JsonNode.class);

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());

    assertNotNull(getResponse);
    assertNotNull(getResponse.getBody());
  }

  @Test
  void testLoginFailure() {
    String port = Integer.toString(availablePort);

    RestTemplate restTemplate = new RestTemplate();

    HttpHeadersBuilder headersBuilder = new HttpHeadersBuilder().withContentTypeJson();

    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("wrongpassword").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, headersBuilder.build());

    try {
      restTemplate.postForEntity(
          "http://localhost:" + port + "/api/auth/login", requestEntity, LoginResponse.class);
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }
  }

  @Test
  void testRedirectWithQueryParam() {
    testRedirectUrl("/api/users?fields=id,name,displayName");
  }

  @Test
  void testRedirectWithoutQueryParam() {
    testRedirectUrl("/api/users");
  }

  @Test
  void testRedirectToResource() {
    testRedirectUrl("/api/users/resource.js", "/dhis-web-dashboard/");
  }

  @Test
  void testRedirectToHtmlResource() {
    testRedirectUrl("/api/users/resource.html", "/api/users/resource.html");
  }

  @Test
  void testRedirectToSlashEnding() {
    testRedirectUrl("/api/users/", "/api/users/");
  }

  @Test
  void testRedirectToResourceWorker() {
    testRedirectUrl("/dhis-web-dashboard/service-worker.js", "/dhis-web-dashboard/");
  }

  @Test
  void testRedirectToCssResourceWorker() {
    testRedirectUrl("/dhis-web-dashboard/static/css/main.4536e618.css", "/dhis-web-dashboard/");
  }

  @Test
  void testRedirectAccountWhenVerifiedEmailEnforced() {
    changeSystemSetting("enforceVerifiedEmail", "true");
    testRedirectUrl("/dhis-web-dashboard/", "/dhis-web-user-profile/#/account");
    changeSystemSetting("enforceVerifiedEmail", "false");
  }

  @Test
  void testRedirectMissingEndingSlash() {
    testRedirectWhenLoggedIn("/dhis-web-dashboard/", "/dhis-web-dashboard/");
  }

  private static void testRedirectUrl(String url) {
    testRedirectUrl(url, url);
  }

  private static RestTemplate createRestTemplateWithBasicAuthHeader() {
    RestTemplate restTemplate = new RestTemplate();

    // Create the authentication header
    String authHeader =
        Base64.getUrlEncoder().encodeToString("admin:district".getBytes(StandardCharsets.UTF_8));

    // Add header to every request
    restTemplate
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Basic " + authHeader);
              return execution.execute(request, body);
            });

    return restTemplate;
  }

  private static void changeSystemSetting(String key, String value) {
    String port = Integer.toString(availablePort);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);

    RestTemplate restTemplate = createRestTemplateWithBasicAuthHeader();
    HttpEntity<String> requestEntity = new HttpEntity<>(value, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/systemSettings/" + key,
            HttpMethod.POST,
            requestEntity,
            String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  private static void testRedirectUrl(String url, String redirectUrl) {
    String port = Integer.toString(availablePort);

    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<LoginResponse> firstResponse =
        restTemplate.postForEntity("http://localhost:" + port + url, null, LoginResponse.class);
    HttpHeaders headersFirstResponse = firstResponse.getHeaders();
    String firstCookie = headersFirstResponse.get(HttpHeaders.SET_COOKIE).get(0);

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set("Cookie", firstCookie);
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, getHeaders);

    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login", requestEntity, LoginResponse.class);

    assertNotNull(loginResponse);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    LoginResponse body = loginResponse.getBody();
    assertNotNull(body);
    assertEquals(LoginResponse.STATUS.SUCCESS, body.getLoginStatus());

    assertEquals(redirectUrl, body.getRedirectUrl());
  }

  private static void testRedirectWhenLoggedIn(String url, String redirectUrl) {
    String port = Integer.toString(availablePort);

    // Create a custom ClientHttpRequestFactory that disables redirects
    ClientHttpRequestFactory requestFactory =
        new SimpleClientHttpRequestFactory() {
          @Override
          protected void prepareConnection(HttpURLConnection connection, String httpMethod)
              throws IOException {
            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(false); // Disable redirects
          }
        };

    RestTemplate restTemplate = new RestTemplate(requestFactory);
    restTemplate
        .getMessageConverters()
        .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

    ResponseEntity<LoginResponse> firstResponse =
        restTemplate.postForEntity("http://localhost:" + port + url, null, LoginResponse.class);
    HttpHeaders headersFirstResponse = firstResponse.getHeaders();
    String firstCookie = headersFirstResponse.get(HttpHeaders.SET_COOKIE).get(0);

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set("Cookie", firstCookie);
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, getHeaders);

    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login", requestEntity, LoginResponse.class);
    HttpHeaders loginHeaders = loginResponse.getHeaders();
    String loggedInCookie = loginHeaders.get(HttpHeaders.SET_COOKIE).get(0);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", loggedInCookie);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> redirResp =
        restTemplate.exchange(
            "http://localhost:" + port + "/dhis-web-dashboard",
            HttpMethod.GET,
            entity,
            String.class);

    HttpHeaders respHeaders = redirResp.getHeaders();
    List<String> location = respHeaders.get("Location");
    assertNotNull(location);
    assertEquals(1, location.size());
    assertEquals(redirectUrl, location.get(0));
  }
}
