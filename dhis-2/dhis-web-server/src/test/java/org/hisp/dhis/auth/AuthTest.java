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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.jboss.aerogear.security.otp.Totp;
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
import org.subethamail.wiser.Wiser;
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

  // Added for the fake SMTP server
  private static int smtpPort;
  private static Wiser wiser;

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

    // Start the fake SMTP server
    smtpPort = findAvailablePort();
    wiser = new Wiser();
    wiser.setHostname("localhost");
    wiser.setPort(smtpPort);
    wiser.start();

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
  void testLogin2FA() throws JsonProcessingException {
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

    HttpHeaders validCookieHeader = new HttpHeaders();
    validCookieHeader.set("Cookie", cookie);
    HttpEntity<String> getEntity = new HttpEntity<>("", validCookieHeader);

    ResponseEntity<JsonNode> getResponse =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/me", HttpMethod.GET, getEntity, JsonNode.class);

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertNotNull(getResponse);
    assertNotNull(getResponse.getBody());

    HttpEntity<Void> twoFAReqEntity = new HttpEntity<>(validCookieHeader);
    ResponseEntity<String> twoFAResp =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/2fa/enrollTOTP2FA", twoFAReqEntity, String.class);

    assertNotNull(twoFAResp);
    assertEquals(HttpStatus.OK, twoFAResp.getStatusCode());
    assertNotNull(twoFAResp.getBody());

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonResponse = objectMapper.readTree(twoFAResp.getBody());
    String message = jsonResponse.get("message").asText();
    assertEquals(
        "The user has enrolled in TOTP 2FA, call the QR code endpoint to continue the process",
        message);

    HttpEntity<String> getQREntity = new HttpEntity<>("", validCookieHeader);
    ResponseEntity<String> showQRResp =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/2fa/showQRCodeAsJson",
            HttpMethod.GET,
            getQREntity,
            String.class);
    assertNotNull(showQRResp);
    assertEquals(HttpStatus.OK, showQRResp.getStatusCode());
    assertNotNull(showQRResp.getBody());
    JsonNode showQrRespJson = objectMapper.readTree(showQRResp.getBody());

    String base32Secret = showQrRespJson.get("base32Secret").asText();
    String base64QRImage = showQrRespJson.get("base64QRImage").asText();

    assertNotNull(base32Secret);
    assertNotNull(base64QRImage);

    /// Generate TOTP code
    String code = new Totp(base32Secret).now();

    /// Enable 2FA
    Map<String, String> enable2FAReqBody = Map.of("code", code);
    HttpEntity<Map<String, String>> enable2FAEntity =
        new HttpEntity<>(enable2FAReqBody, validCookieHeader);
    ResponseEntity<String> enable2FAResp =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/2fa/enable", enable2FAEntity, String.class);

    assertNotNull(enable2FAResp);
    assertEquals(HttpStatus.OK, enable2FAResp.getStatusCode());
    assertNotNull(enable2FAResp.getBody());

    JsonNode enable2FAJSONResp = objectMapper.readTree(enable2FAResp.getBody());
    assertEquals(
        "Two factor authentication was enabled successfully",
        enable2FAJSONResp.get("message").asText());

    /// Start new login
    HttpHeadersBuilder headersBuilder2 = new HttpHeadersBuilder().withContentTypeJson();
    LoginRequest loginRequest2 =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity2 =
        new HttpEntity<>(loginRequest2, headersBuilder2.build());
    ResponseEntity<LoginResponse> loginResponse2 =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/auth/login", requestEntity2, LoginResponse.class);
    assertNotNull(loginResponse2);
    assertEquals(HttpStatus.OK, loginResponse2.getStatusCode());
    LoginResponse body2 = loginResponse2.getBody();
    assertNotNull(body2);
    assertEquals(LoginResponse.STATUS.INCORRECT_TWO_FACTOR_CODE, body2.getLoginStatus());
  }

  @Test
  void testLogin2FAS() throws JsonProcessingException {
    String port = Integer.toString(availablePort);
    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();

    // First Login
    ResponseEntity<LoginResponse> loginResponse =
        login(restTemplate, port, "admin", "district", null);

    // Verify response and extract cookie
    assertLoginSuccess(loginResponse, "/dhis-web-dashboard/");
    String cookie = extractSessionCookie(loginResponse);

    // Verify session
    ResponseEntity<String> getResponse = getWithCookie(restTemplate, port, "/api/me", cookie);
    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertNotNull(getResponse.getBody());

    // Enroll in TOTP 2FA
    ResponseEntity<String> twoFAResp =
        postWithCookie(restTemplate, port, "/api/2fa/enrollTOTP2FA", null, cookie);
    assertMessage(
        twoFAResp,
        "The user has enrolled in TOTP 2FA, call the QR code endpoint to continue the process");

    // Get QR code and base32 secret
    ResponseEntity<String> showQRResp =
        getWithCookie(restTemplate, port, "/api/2fa/showQRCodeAsJson", cookie);
    JsonNode showQrRespJson = objectMapper.readTree(showQRResp.getBody());
    String base32Secret = showQrRespJson.get("base32Secret").asText();
    assertNotNull(base32Secret);

    // Generate TOTP code and enable 2FA
    String enable2FACode = new Totp(base32Secret).now();
    Map<String, String> enable2FAReqBody = Map.of("code", enable2FACode);
    ResponseEntity<String> enable2FAResp =
        postWithCookie(restTemplate, port, "/api/2fa/enable", enable2FAReqBody, cookie);
    assertMessage(enable2FAResp, "Two factor authentication was enabled successfully");

    // Attempt to log in without 2FA code
    ResponseEntity<LoginResponse> failedLoginResp =
        login(restTemplate, port, "admin", "district", null);
    assertEquals(
        LoginResponse.STATUS.INCORRECT_TWO_FACTOR_CODE, failedLoginResp.getBody().getLoginStatus());

    String login2FACode = new Totp(base32Secret).now();
    ResponseEntity<LoginResponse> login2FAResp =
        login(restTemplate, port, "admin", "district", login2FACode);
    assertLoginSuccess(login2FAResp, "/dhis-web-dashboard/");
  }

  private ResponseEntity<LoginResponse> login(
      RestTemplate restTemplate, String port, String username, String password, String twoFACode) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    LoginRequest loginRequest =
        LoginRequest.builder()
            .username(username)
            .password(password)
            .twoFactorCode(twoFACode)
            .build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, headers);
    return restTemplate.postForEntity(
        "http://localhost:" + port + "/api/auth/login", requestEntity, LoginResponse.class);
  }

  private void assertLoginSuccess(
      ResponseEntity<LoginResponse> response, String expectedRedirectUrl) {
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    LoginResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(LoginResponse.STATUS.SUCCESS, body.getLoginStatus());
    assertEquals(expectedRedirectUrl, body.getRedirectUrl());
  }

  private String extractSessionCookie(ResponseEntity<?> response) {
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertNotNull(cookies);
    assertEquals(1, cookies.size());
    return cookies.get(0);
  }

  private ResponseEntity<String> postWithCookie(
      RestTemplate restTemplate, String port, String path, Object body, String cookie) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);
    if (body != null) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }
    HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
    return restTemplate.postForEntity(
        "http://localhost:" + port + path, requestEntity, String.class);
  }

  private ResponseEntity<String> getWithCookie(
      RestTemplate restTemplate, String port, String path, String cookie) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);
    HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
    return restTemplate.exchange(
        "http://localhost:" + port + path, HttpMethod.GET, requestEntity, String.class);
  }

  private void assertMessage(ResponseEntity<String> response, String expectedMessage)
      throws JsonProcessingException {
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    JsonNode jsonResponse = new ObjectMapper().readTree(response.getBody());
    assertEquals(expectedMessage, jsonResponse.get("message").asText());
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
