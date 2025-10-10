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
package org.hisp.dhis;

import static org.hisp.dhis.login.PortUtil.findAvailablePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;
import org.hisp.dhis.login.LoginRequest;
import org.hisp.dhis.login.LoginResponse;
import org.hisp.dhis.login.LoginResponse.STATUS;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.subethamail.wiser.Wiser;

public class BaseE2ETest {

  // Change this to "localhost" if you want to run the tests locally
  public static final String SMTP_HOSTNAME = "test";

  public static int smtpPort;
  public static Wiser wiser;

  public static final ObjectMapper objectMapper = new ObjectMapper();
  public static final RestTemplate restTemplate = new RestTemplate();

  public static String serverApiUrl = "http://localhost:8080/api";
  public static String serverHostUrl = "http://localhost:8080/";

  public static final String DEFAULT_LOGIN_REDIRECT = "/";
  public static final String LOGIN_API_PATH = "/auth/login";
  public static final String SUPER_USER_ROLE_UID = "yrB6vc5Ip3r";

  public static String orgUnitUID;

  // --------------------------------------------------------------------------------------------
  // Private helper methods for starting servers and setup
  // --------------------------------------------------------------------------------------------

  public static void startSMTPServer() {
    smtpPort = findAvailablePort();
    wiser = new Wiser();
    wiser.setHostname(SMTP_HOSTNAME);
    wiser.setPort(smtpPort);
    wiser.start();
  }

  public static void configureEmail2FASettings(String cookie) {
    setSystemPropertyWithCookie("keyEmailHostName", SMTP_HOSTNAME, cookie);
    setSystemPropertyWithCookie("keyEmailPort", String.valueOf(smtpPort), cookie);
    setSystemPropertyWithCookie("keyEmailUsername", "nils", cookie);
    setSystemPropertyWithCookie("keyEmailPassword", "nils", cookie);
    setSystemPropertyWithCookie("keyEmailSender", "system@nils.no", cookie);
    setSystemPropertyWithCookie("keyEmailTls", "false", cookie);
    setSystemPropertyWithCookie("keySelfRegistrationNoRecaptcha", "true", cookie);
  }

  public static void invalidateAllSession() {
    ResponseEntity<String> response = deleteWithAdminBasicAuth("/sessions", null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  // --------------------------------------------------------------------------------------------
  // public helper methods for login and assertions
  // --------------------------------------------------------------------------------------------

  public static String performInitialLogin(String username, String password) {
    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(loginResponse, DEFAULT_LOGIN_REDIRECT);
    return extractSessionCookie(loginResponse);
  }

  public static String loginWith2FA(String username, String password, String twoFACode) {
    ResponseEntity<LoginResponse> login2FAResp =
        loginWithUsernameAndPassword(username, password, twoFACode);
    assertLoginSuccess(login2FAResp, DEFAULT_LOGIN_REDIRECT);
    return extractSessionCookie(login2FAResp);
  }

  // --------------------------------------------------------------------------------------------
  // public helper methods for assertions
  // --------------------------------------------------------------------------------------------

  public static void assertLoginSuccess(
      ResponseEntity<LoginResponse> response, String expectedRedirectUrl) {
    assertLoginStatus(response, STATUS.SUCCESS);
    assertNotNull(response.getBody());
    assertEquals(expectedRedirectUrl, response.getBody().getRedirectUrl());
  }

  public static void assertLoginStatus(ResponseEntity<LoginResponse> response, STATUS status) {
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(status, response.getBody().getLoginStatus());
  }

  public static void assertMessage(ResponseEntity<String> response, String expectedMessage)
      throws JsonProcessingException {
    assertNotNull(response);
    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
    assertEquals(expectedMessage, jsonResponse.get("message").asText());
  }

  // --------------------------------------------------------------------------------------------
  // public helper methods for redirect assertions
  // --------------------------------------------------------------------------------------------

  public static void assertRedirectToSameUrl(String url) {
    assertRedirectUrl(url, url, true);
  }

  public static void assertRedirectUrl(String url, String redirectUrl, boolean shouldSaveRequest) {
    // Do an invalid login to store original URL request
    ResponseEntity<LoginResponse> firstResponse =
        restTemplate.postForEntity(serverHostUrl + url, null, LoginResponse.class);

    HttpHeaders getHeaders = jsonHeaders();
    List<String> cookies = firstResponse.getHeaders().get(HttpHeaders.SET_COOKIE);

    if (shouldSaveRequest) {
      String cookie = cookies.get(0);
      getHeaders.set("Cookie", cookie);
    } else {
      assertNull(cookies);
    }

    // Do a valid login with the captured cookie
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, getHeaders);

    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            serverApiUrl + LOGIN_API_PATH, requestEntity, LoginResponse.class);

    assertNotNull(loginResponse);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    LoginResponse body = loginResponse.getBody();
    assertNotNull(body);
    assertEquals(STATUS.SUCCESS, body.getLoginStatus());
    assertEquals(redirectUrl, body.getRedirectUrl());
  }

  // --------------------------------------------------------------------------------------------
  // public helper methods for HTTP calls
  // --------------------------------------------------------------------------------------------

  public static HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  public static ResponseEntity<LoginResponse> loginWithUsernameAndPassword(
      String username, String password, String twoFACode) {
    HttpHeaders headers = jsonHeaders();
    LoginRequest loginRequest =
        LoginRequest.builder()
            .username(username)
            .password(password)
            .twoFactorCode(twoFACode)
            .build();
    return restTemplate.postForEntity(
        serverApiUrl + LOGIN_API_PATH,
        new HttpEntity<>(loginRequest, headers),
        LoginResponse.class);
  }

  public static ResponseEntity<String> postWithCookie(String path, Object body, String cookie) {
    HttpHeaders headers = jsonHeaders();
    headers.set("Cookie", cookie);
    if (body != null) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }
    HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
    try {
      return restTemplate.postForEntity(serverApiUrl + path, requestEntity, String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  public static ResponseEntity<String> postAsAdmin(String fullUrl, Map<String, Object> map) {
    HttpHeaders headers = jsonHeaders();
    String authHeader =
        Base64.getUrlEncoder().encodeToString("admin:district".getBytes(StandardCharsets.UTF_8));
    headers.add(HttpHeaders.AUTHORIZATION, "Basic " + authHeader);
    HttpEntity<?> requestEntity = new HttpEntity<>(map, headers);
    try {
      return restTemplate.postForEntity(fullUrl, requestEntity, String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  public String callTokenEndpoint(String code) {
    RestTemplate restTemplate = getRestTemplateNoRedirects();
    String apiUrl = serverHostUrl + "/oauth2/token";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("code", code);
    formData.add("client_id", "dhis2-client");
    formData.add("client_secret", "secret");
    formData.add("grant_type", "authorization_code");
    formData.add("redirect_uri", "http://localhost:9090/oauth2/code/dhis2-client");

    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
    ResponseEntity<String> response =
        restTemplate.postForEntity(apiUrl, requestEntity, String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    return response.getBody();
  }

  public static ResponseEntity<String> getWithCookie(
      RestTemplate template, String path, String cookie) {
    HttpHeaders headers = jsonHeaders();
    headers.set("Cookie", cookie);
    return exchangeWithHeaders(template, path, HttpMethod.GET, null, headers);
  }

  public static ResponseEntity<String> getWithCookie(String path, String cookie) {
    return getWithCookie(restTemplate, path, cookie);
  }

  public static ResponseEntity<String> getWithBearerJwt(String fullUrl, String token) {
    RestTemplate rt = addBearerTokenAuthHeaders(new RestTemplate(), token);
    try {
      return rt.exchange(
          fullUrl, HttpMethod.GET, new HttpEntity<>("", jsonHeaders()), String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  public static ResponseEntity<String> getWithAdminBasicAuth(String path, Map<String, Object> map) {
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    return rt.exchange(
        serverApiUrl + path, HttpMethod.GET, new HttpEntity<>(map, jsonHeaders()), String.class);
  }

  public static ResponseEntity<String> getWithWrongAuth(String path, Map<String, Object> map) {
    RestTemplate rt = new RestTemplate();

    String authHeader =
        Base64.getUrlEncoder().encodeToString("admin:wrong".getBytes(StandardCharsets.UTF_8));
    rt.getInterceptors()
        .add(
            (request, body, execution) -> {
              request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Basic " + authHeader);
              return execution.execute(request, body);
            });

    return rt.exchange(
        serverApiUrl + path, HttpMethod.GET, new HttpEntity<>(map, jsonHeaders()), String.class);
  }

  public static ResponseEntity<String> postWithAdminBasicAuth(
      String path, Map<String, Object> map) {
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    return rt.exchange(
        serverApiUrl + path, HttpMethod.POST, new HttpEntity<>(map, jsonHeaders()), String.class);
  }

  public static ResponseEntity<String> deleteWithAdminBasicAuth(
      String path, Map<String, Object> map) {
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    return rt.exchange(
        serverApiUrl + path, HttpMethod.DELETE, new HttpEntity<>(map, jsonHeaders()), String.class);
  }

  public static ResponseEntity<String> exchangeWithHeaders(
      RestTemplate template, String path, HttpMethod method, Object body, HttpHeaders headers) {
    try {
      return template.exchange(
          serverApiUrl + path, method, new HttpEntity<>(body, headers), String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  public static RestTemplate addAdminBasicAuthHeaders(RestTemplate template) {
    String authHeader =
        Base64.getUrlEncoder().encodeToString("admin:district".getBytes(StandardCharsets.UTF_8));
    template
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Basic " + authHeader);
              return execution.execute(request, body);
            });
    return template;
  }

  public static RestTemplate addBearerTokenAuthHeaders(RestTemplate template, String token) {
    template
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
              return execution.execute(request, body);
            });
    return template;
  }

  @Nonnull
  public static RestTemplate getRestTemplateNoRedirects() {
    // Disable auto-redirects
    ClientHttpRequestFactory requestFactory =
        new SimpleClientHttpRequestFactory() {
          @Override
          protected void prepareConnection(HttpURLConnection connection, String httpMethod)
              throws IOException {
            super.prepareConnection(connection, httpMethod);
            connection.setInstanceFollowRedirects(false);
          }
        };
    return new RestTemplate(requestFactory);
  }

  public static String getTextFromMessage(Message message) throws MessagingException, IOException {
    if (message.isMimeType("text/plain")) {
      return message.getContent().toString();
    } else if (message.isMimeType("multipart/*")) {
      return getTextFromMimeMultipart((MimeMultipart) message.getContent());
    } else if (message.isMimeType("message/rfc822")) {
      return getTextFromMessage((Message) message.getContent());
    } else {
      Object content = message.getContent();
      if (content instanceof String) {
        return (String) content;
      }
    }
    return "";
  }

  public static String getTextFromMimeMultipart(MimeMultipart mimeMultipart)
      throws MessagingException, IOException {
    StringBuilder result = new StringBuilder();
    int count = mimeMultipart.getCount();
    for (int i = 0; i < count; i++) {
      BodyPart bodyPart = mimeMultipart.getBodyPart(i);
      if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
        continue;
      }
      if (bodyPart.isMimeType("text/plain")) {
        result.append(bodyPart.getContent().toString());
      } else if (bodyPart.isMimeType("text/html")) {
        result.append(bodyPart.getContent().toString());
      } else if (bodyPart.getContent() instanceof MimeMultipart) {
        result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
      }
    }
    return result.toString();
  }

  // --------------------------------------------------------------------------------------------
  // public helper methods for server configuration and resource creation
  // --------------------------------------------------------------------------------------------

  public static String createSuperuser(String username, String password, String orgUnitUID)
      throws JsonProcessingException {
    Map<String, Object> userMap =
        Map.of(
            "username",
            username,
            "password",
            password,
            "email",
            username + "@email.com",
            "userRoles",
            List.of(Map.of("id", SUPER_USER_ROLE_UID)),
            "firstName",
            "user",
            "surname",
            "userson",
            "organisationUnits",
            List.of(Map.of("id", orgUnitUID)));

    // Create user
    ResponseEntity<String> response = postWithAdminBasicAuth("/users", userMap);
    JsonNode fullResponseNode = objectMapper.readTree(response.getBody());
    String uid = fullResponseNode.get("response").get("uid").asText();
    assertNotNull(uid);

    // Verify user
    ResponseEntity<String> userResp = getWithAdminBasicAuth("/users/" + uid, Map.of());
    assertEquals(HttpStatus.OK, userResp.getStatusCode());
    JsonNode userJson = objectMapper.readTree(userResp.getBody());
    assertEquals(username, userJson.get("username").asText());
    assertEquals(username + "@email.com", userJson.get("email").asText());
    assertEquals("user", userJson.get("firstName").asText());
    assertEquals("userson", userJson.get("surname").asText());
    assertEquals(orgUnitUID, userJson.get("organisationUnits").get(0).get("id").asText());
    assertEquals(SUPER_USER_ROLE_UID, userJson.get("userRoles").get(0).get("id").asText());
    return uid;
  }

  public static String createOrgUnit() throws JsonProcessingException {
    ResponseEntity<String> jsonStringResponse =
        postWithAdminBasicAuth(
            "/organisationUnits",
            Map.of("name", "orgA", "shortName", "orgA", "openingDate", "2024-11-21T16:00:00.000Z"));

    JsonNode fullResponseNode = objectMapper.readTree(jsonStringResponse.getBody());
    String uid = fullResponseNode.get("response").get("uid").asText();
    assertNotNull(uid);
    return uid;
  }

  public static String extractSessionCookie(ResponseEntity<?> response) {
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertNotNull(cookies);
    assertEquals(1, cookies.size());
    return cookies.get(0);
  }

  public static void setSystemPropertyWithCookie(String property, String value, String cookie) {
    ResponseEntity<String> systemSettingsResp =
        postWithCookie("/systemSettings/" + property + "?value=" + value, null, cookie);
    assertEquals(HttpStatus.OK, systemSettingsResp.getStatusCode());
  }

  public static void setSystemProperty(String property, String value) {
    ResponseEntity<String> systemSettingsResp =
        postWithAdminBasicAuth("/systemSettings/" + property + "?value=" + value, null);
    assertEquals(HttpStatus.OK, systemSettingsResp.getStatusCode());
  }

  public static void changeSystemSetting(String key, String value) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    ResponseEntity<String> response =
        rt.exchange(
            serverApiUrl + "/systemSettings/" + key,
            HttpMethod.POST,
            new HttpEntity<>(value, headers),
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
