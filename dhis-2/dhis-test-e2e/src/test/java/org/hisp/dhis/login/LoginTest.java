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
package org.hisp.dhis.login;

import static org.hisp.dhis.login.PortUtil.findAvailablePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.jboss.aerogear.security.otp.Totp;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

@Tag("logintests")
public class LoginTest {

  public static final String SUPER_USER_ROLE_UID = "yrB6vc5Ip3r";
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final RestTemplate restTemplate =
      new RestTemplate(new HttpComponentsClientHttpRequestFactory());

  public static String dhis2ServerApi = "http://localhost:8080/api";
  public static String dhis2Server = "http://localhost:8080/";
  private static int smtpPort;
  private static Wiser wiser;
  private static String orgUnitUID;

  private static void startSMTPServer() {
    smtpPort = findAvailablePort();
    wiser = new Wiser();
    wiser.setHostname("test");
    wiser.setPort(smtpPort);
    wiser.start();
  }

  @BeforeAll
  static void setup() throws JsonProcessingException {
    startSMTPServer();
    dhis2ServerApi = "http://localhost:51261/api";
    dhis2Server = "http://localhost:51261/";
    // Create a new org unit for the new users
    orgUnitUID = createOrgUnit(objectMapper);
  }

  @Test
  void testLoginAndGetCookie() {
    String username = TestConfiguration.get().defaultUserUsername();
    String password = TestConfiguration.get().defaultUSerPassword();
    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(username, password, null);

    // Verify response and extract cookie
    assertLoginSuccess(loginResponse, "/dhis-web-dashboard/");
    String cookie = extractSessionCookie(loginResponse);

    // Verify session cookie works
    ResponseEntity<String> meResponse = getWithCookie("/me", cookie);
    assertEquals(org.springframework.http.HttpStatus.OK, meResponse.getStatusCode());
    assertNotNull(meResponse.getBody());
  }

  @Test
  void testLoginWithTOTP2FA() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    // Create a new user with the superuser role
    createSuperuser(username, password, orgUnitUID);

    // First Login
    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(username, password, null);

    // Verify response and extract cookie
    assertLoginSuccess(loginResponse, "/dhis-web-dashboard/");
    String cookie = extractSessionCookie(loginResponse);

    // Verify session
    ResponseEntity<String> getResponse = getWithCookie("/me", cookie);
    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertNotNull(getResponse.getBody());

    // Enroll in TOTP 2FA
    ResponseEntity<String> twoFAResp = postWithCookie("/2fa/enrollTOTP2FA", null, cookie);
    assertMessage(
        twoFAResp,
        "The user has enrolled in TOTP 2FA, call the QR code endpoint to continue the process");

    // Get QR code and Base32 secret
    ResponseEntity<String> showQRResp = getWithCookie("/2fa/showQRCodeAsJson", cookie);
    JsonNode showQrRespJson = objectMapper.readTree(showQRResp.getBody());
    String base32Secret = showQrRespJson.get("base32Secret").asText();
    assertNotNull(base32Secret);

    // Generate TOTP code and enable 2FA
    String enable2FACode = new Totp(base32Secret).now();
    Map<String, String> enable2FAReqBody = Map.of("code", enable2FACode);
    ResponseEntity<String> enable2FAResp = postWithCookie("/2fa/enable", enable2FAReqBody, cookie);
    assertMessage(enable2FAResp, "Two factor authentication was enabled successfully");

    // Attempt to log in without 2FA code
    ResponseEntity<LoginResponse> failedLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertNotNull(failedLoginResp.getBody());
    assertEquals(
        LoginResponse.STATUS.INCORRECT_TWO_FACTOR_CODE, failedLoginResp.getBody().getLoginStatus());

    // Attempt to log in with correct 2FA code
    String login2FACode = new Totp(base32Secret).now();
    ResponseEntity<LoginResponse> login2FAResp =
        loginWithUsernameAndPassword(username, password, login2FACode);
    assertLoginSuccess(login2FAResp, "/dhis-web-dashboard/");
    String newSessionCookie = extractSessionCookie(login2FAResp);

    // Verify new session cookie works
    ResponseEntity<String> apiMeResp = getWithCookie("/me", newSessionCookie);
    assertEquals(HttpStatus.OK, apiMeResp.getStatusCode());
    assertNotNull(apiMeResp.getBody());
  }

  @Test
  void testLoginWithEmail2FAWrapper() throws IOException, MessagingException {
    try {
      testLoginWithEmail2FA();
    } finally {
      // Reset system settings
      setSystemProperty("email2FAEnabled", "false");
      setSystemProperty("keyEmailHostName", "");
      setSystemProperty("keyEmailPort", "25");
      setSystemProperty("keyEmailUsername", null);
      setSystemProperty("keyEmailSender", "");
      setSystemProperty("keyEmailTls", "true");
    }
  }

  private static void testLoginWithEmail2FA() throws IOException, MessagingException {
    String username = CodeGenerator.generateCode(8).toLowerCase();
    String password = "Test123###...";
    // Create a new user with the superuser role
    String newUserUID = createSuperuser(username, password, orgUnitUID);

    // First Login
    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(username, password, null);

    // Verify response and extract cookie
    assertLoginSuccess(loginResponse, "/dhis-web-dashboard/");
    String cookie = extractSessionCookie(loginResponse);

    // Verify session
    ResponseEntity<String> meResponse = getWithCookie("/me", cookie);
    assertEquals(HttpStatus.OK, meResponse.getStatusCode());
    JsonNode jsonResponse = new ObjectMapper().readTree(meResponse.getBody());
    String userUID = jsonResponse.get("id").asText();

    assertEquals(newUserUID, userUID);

    // Enable Email 2FA in system settings, set up SMTP server
    setSystemPropertyWithCookie("email2FAEnabled", "true", cookie);
    setSystemPropertyWithCookie("keyEmailHostName", "localhost", cookie);
    setSystemPropertyWithCookie("keyEmailPort", String.valueOf(smtpPort), cookie);
    setSystemPropertyWithCookie("keyEmailUsername", "nils", cookie);
    setSystemPropertyWithCookie("keyEmailSender", "system@nils.no", cookie);
    setSystemPropertyWithCookie("keyEmailTls", "false", cookie);

    // Enroll in Email 2FA
    ResponseEntity<String> twoFAResp = postWithCookie("/2fa/enrollEmail2FA", null, cookie);
    assertEquals(HttpStatus.CONFLICT, twoFAResp.getStatusCode());
    assertMessage(
        twoFAResp,
        "User has not a verified email, please verify your email first before you enable 2FA");

    // Send verification email to user
    ResponseEntity<String> sendVerificationEmailResp =
        postWithCookie("/account/sendEmailVerification", null, cookie);
    assertEquals(HttpStatus.CREATED, sendVerificationEmailResp.getStatusCode());

    String verifyToken = extractEmailVerifyToken();

    // Verify email with token extracted from email
    ResponseEntity<String> verifyEmailResp =
        getWithCookie("/account/verifyEmail?token=" + verifyToken, cookie);
    assertEquals(HttpStatus.OK, verifyEmailResp.getStatusCode());

    // Enroll in Email 2FA
    ResponseEntity<String> twoFAEnableResp = postWithCookie("/2fa/enrollEmail2FA", null, cookie);
    assertEquals(HttpStatus.OK, twoFAEnableResp.getStatusCode());
    assertMessage(
        twoFAEnableResp,
        "The user has enrolled in email-based 2FA, a code was generated and sent successfully to the user's email");

    // Enable 2FA with enrollment 2FA code sent via email
    String enrollCode = extract2FACodeFromLatestEmail();
    Map<String, String> enable2FAReqBody = Map.of("code", enrollCode);
    ResponseEntity<String> enable2FAResp = postWithCookie("/2fa/enable", enable2FAReqBody, cookie);
    assertMessage(enable2FAResp, "Two factor authentication was enabled successfully");

    // Attempt to log in without 2FA code, should send email with a new code
    ResponseEntity<LoginResponse> failedLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertNotNull(failedLoginResp.getBody());
    assertEquals(
        LoginResponse.STATUS.INCORRECT_TWO_FACTOR_CODE, failedLoginResp.getBody().getLoginStatus());

    // Attempt to log in with correct 2FA code sent by email
    String login2FACode = extract2FACodeFromLatestEmail();
    ResponseEntity<LoginResponse> login2FAResp =
        loginWithUsernameAndPassword(username, password, login2FACode);
    assertLoginSuccess(login2FAResp, "/dhis-web-dashboard/");

    // Verify new session cookie works
    String newSessionCookie = extractSessionCookie(login2FAResp);
    ResponseEntity<String> apiMeResp = getWithCookie("/me", newSessionCookie);
    assertEquals(HttpStatus.OK, apiMeResp.getStatusCode());
    assertNotNull(apiMeResp.getBody());
  }

  @Test
  void testLoginFailure() {
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("wrongpassword").build();
    HttpEntity<LoginRequest> requestEntity =
        new HttpEntity<>(loginRequest, new HttpHeadersBuilder().withContentTypeJson().build());

    try {
      restTemplate.postForEntity(
          dhis2ServerApi + "/auth/login", requestEntity, LoginResponse.class);
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }
  }

  @Test
  void testRedirectWithQueryParam() {
    assertRedirectToSameUrl("/api/users?fields=id,name,displayName");
  }

  @Test
  void testRedirectWithoutQueryParam() {
    assertRedirectToSameUrl("/api/users");
  }

  @Test
  void testRedirectToResource() {
    assertRedirectUrl("/users/resource.js", "/dhis-web-dashboard/");
  }

  @Test
  void testRedirectToHtmlResource() {
    assertRedirectUrl("/users/resource.html", "/users/resource.html");
  }

  @Test
  void testRedirectToSlashEnding() {
    assertRedirectUrl("/users/", "/users/");
  }

  @Test
  void testRedirectToResourceWorker() {
    assertRedirectUrl("/dhis-web-dashboard/service-worker.js", "/dhis-web-dashboard/");
  }

  @Test
  void testRedirectToCssResourceWorker() {
    assertRedirectUrl("/dhis-web-dashboard/static/css/main.4536e618.css", "/dhis-web-dashboard/");
  }

  @Test
  void testRedirectAccountWhenVerifiedEmailEnforced() {
    changeSystemSetting("enforceVerifiedEmail", "true");
    try {
      assertRedirectUrl("/dhis-web-dashboard/", "/dhis-web-user-profile/#/account");
    } finally {
      changeSystemSetting("enforceVerifiedEmail", "false");
    }
  }

  private static void changeSystemSetting(String key, String value) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    RestTemplate restTemplate = createRestTemplateWithAdminBasicAuthHeader();
    HttpEntity<String> requestEntity = new HttpEntity<>(value, headers);
    ResponseEntity<String> response =
        restTemplate.exchange(
            dhis2ServerApi + "/systemSettings/" + key,
            HttpMethod.POST,
            requestEntity,
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testRedirectEndingSlash() {
    testRedirectWhenLoggedIn("/dhis-web-dashboard/", "/dhis-web-dashboard/");
  }

  @Test
  void testRedirectMissingEndingSlash() {
    testRedirectWhenLoggedIn("/dhis-web-dashboard", "/dhis-web-dashboard/");
  }

  private static void assertRedirectToSameUrl(String url) {
    assertRedirectUrl(url, url);
  }

  private static void assertRedirectUrl(String url, String redirectUrl) {
    RestTemplate restTemplate = new RestTemplate();
    // Do an invalid login and capture response cookie, we need to do this first
    // so that unauthorized URL is cached in the session
    ResponseEntity<LoginResponse> firstResponse =
        restTemplate.postForEntity(dhis2Server + url, null, LoginResponse.class);
    HttpHeaders headersFirstResponse = firstResponse.getHeaders();
    String firstCookie = headersFirstResponse.get(HttpHeaders.SET_COOKIE).get(0);

    // Do a valid login request with the first cookie, check that we get redirected to the first
    // cached URL
    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set("Cookie", firstCookie);
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, getHeaders);
    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            dhis2ServerApi + "/auth/login", requestEntity, LoginResponse.class);

    assertNotNull(loginResponse);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    LoginResponse body = loginResponse.getBody();
    assertNotNull(body);
    assertEquals(LoginResponse.STATUS.SUCCESS, body.getLoginStatus());
    assertEquals(redirectUrl, body.getRedirectUrl());
  }

  private static void testRedirectWhenLoggedIn(String url, String redirectUrl) {
    // Create a custom ClientHttpRequestFactory that disables redirects so we can capture the 302
    // and assert the Location header
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
        restTemplate.postForEntity(dhis2Server + url, null, LoginResponse.class);
    HttpHeaders headersFirstResponse = firstResponse.getHeaders();
    String firstCookie = headersFirstResponse.get(HttpHeaders.SET_COOKIE).get(0);

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.set("Cookie", firstCookie);
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, getHeaders);

    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            dhis2ServerApi + "/auth/login", requestEntity, LoginResponse.class);
    HttpHeaders loginHeaders = loginResponse.getHeaders();
    String loggedInCookie = loginHeaders.get(HttpHeaders.SET_COOKIE).get(0);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", loggedInCookie);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> redirResp =
        restTemplate.exchange(
            dhis2Server + "/dhis-web-dashboard", HttpMethod.GET, entity, String.class);

    HttpHeaders respHeaders = redirResp.getHeaders();
    List<String> location = respHeaders.get("Location");
    assertNotNull(location);
    assertEquals(1, location.size());
    assertEquals(redirectUrl, location.get(0));
  }

  private static void setSystemPropertyWithCookie(String property, String value, String cookie) {
    ResponseEntity<String> systemSettingsResp =
        postWithCookie("/systemSettings/" + property + "?value=" + value, null, cookie);
    assertEquals(HttpStatus.OK, systemSettingsResp.getStatusCode());
  }

  private static String createSuperuser(String username, String password, String orgUnitUID)
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
            List.of(Map.of("id", SUPER_USER_ROLE_UID)), // Superuser static role UID
            "firstName",
            "user",
            "surname",
            "userson",
            "organisationUnits",
            List.of(Map.of("id", orgUnitUID)));

    // Create user
    ResponseEntity<String> jsonStringResponse = postWithAdminBasicAuth("/users", userMap);
    JsonNode fullResponseNode = objectMapper.readTree(jsonStringResponse.getBody());
    JsonNode response = fullResponseNode.get("response");
    String uid = response.get("uid").asText();
    assertNotNull(uid);
    // Verify user was created
    ResponseEntity<String> userResp = getWithAdminBasicAuth("/users/" + uid, Map.of());
    assertEquals(HttpStatus.OK, userResp.getStatusCode());
    // Verify user has the same values as the ones sent in the request
    JsonNode userJson = objectMapper.readTree(userResp.getBody());
    assertEquals(username, userJson.get("username").asText());
    assertEquals(username + "@email.com", userJson.get("email").asText());
    assertEquals("user", userJson.get("firstName").asText());
    assertEquals("userson", userJson.get("surname").asText());
    assertEquals(orgUnitUID, userJson.get("organisationUnits").get(0).get("id").asText());
    assertEquals(SUPER_USER_ROLE_UID, userJson.get("userRoles").get(0).get("id").asText());
    return uid;
  }

  private static ResponseEntity<String> postWithCookie(String path, Object body, String cookie) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);
    if (body != null) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }
    HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
    try {
      return restTemplate.postForEntity(dhis2ServerApi + path, requestEntity, String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  private static ResponseEntity<String> getWithAdminBasicAuth(
      String path, Map<String, Object> map) {
    RestTemplate restTemplate = createRestTemplateWithAdminBasicAuthHeader();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(map, headers);
    return restTemplate.exchange(
        dhis2ServerApi + path, HttpMethod.GET, requestEntity, String.class);
  }

  private static ResponseEntity<LoginResponse> loginWithUsernameAndPassword(
      String username, String password, String twoFACode) {
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
        dhis2ServerApi + "/auth/login", requestEntity, LoginResponse.class);
  }

  private static ResponseEntity<String> getWithCookie(String path, String cookie) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Cookie", cookie);
    HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
    try {
      return restTemplate.exchange(
          dhis2ServerApi + path, HttpMethod.GET, requestEntity, String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  private static void assertLoginSuccess(
      ResponseEntity<LoginResponse> response, String expectedRedirectUrl) {
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    LoginResponse body = response.getBody();
    assertNotNull(body);
    assertEquals(LoginResponse.STATUS.SUCCESS, body.getLoginStatus());
    assertEquals(expectedRedirectUrl, body.getRedirectUrl());
  }

  private static String extractSessionCookie(ResponseEntity<?> response) {
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertNotNull(cookies);
    assertEquals(1, cookies.size());
    return cookies.get(0);
  }

  private static String createOrgUnit(ObjectMapper objectMapper) throws JsonProcessingException {
    ResponseEntity<String> jsonStringResponse =
        postWithAdminBasicAuth(
            "/organisationUnits",
            Map.of("name", "orgA", "shortName", "orgA", "openingDate", "2024-11-21T16:00:00.000Z"));

    JsonNode fullResponseNode = objectMapper.readTree(jsonStringResponse.getBody());
    JsonNode response = fullResponseNode.get("response");
    String uid = response.get("uid").asText();
    assertNotNull(uid);
    return uid;
  }

  private static ResponseEntity<String> postWithAdminBasicAuth(
      String path, Map<String, Object> map) {
    RestTemplate restTemplate = createRestTemplateWithAdminBasicAuthHeader();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(map, headers);
    return restTemplate.exchange(
        dhis2ServerApi + path, HttpMethod.POST, requestEntity, String.class);
  }

  private static RestTemplate createRestTemplateWithAdminBasicAuthHeader() {
    RestTemplate restTemplate = new RestTemplate();
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

  private static void assertMessage(ResponseEntity<String> response, String expectedMessage)
      throws JsonProcessingException {
    assertNotNull(response);
    JsonNode jsonResponse = new ObjectMapper().readTree(response.getBody());
    assertEquals(expectedMessage, jsonResponse.get("message").asText());
  }

  private static @NotNull String extract2FACodeFromLatestEmail()
      throws MessagingException, IOException {
    List<WiserMessage> messages = wiser.getMessages();
    String twoFAEmail =
        getTextFromMessage(wiser.getMessages().get(messages.size() - 1).getMimeMessage());
    return twoFAEmail.substring(twoFAEmail.indexOf("code:") + 7, twoFAEmail.indexOf("code:") + 13);
  }

  private static String getTextFromMessage(Message message) throws MessagingException, IOException {
    if (message.isMimeType("text/plain")) {
      // Simple text message
      return message.getContent().toString();
    } else if (message.isMimeType("multipart/*")) {
      // Multipart message
      MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
      return getTextFromMimeMultipart(mimeMultipart);
    } else if (message.isMimeType("message/rfc822")) {
      // Nested message (forwarded email)
      return getTextFromMessage((Message) message.getContent());
    } else {
      // Other content types (e.g., text/html)
      Object content = message.getContent();
      if (content instanceof String) {
        return (String) content;
      }
    }
    return "";
  }

  private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart)
      throws MessagingException, IOException {
    StringBuilder result = new StringBuilder();
    int count = mimeMultipart.getCount();

    for (int i = 0; i < count; i++) {
      BodyPart bodyPart = mimeMultipart.getBodyPart(i);
      if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
        // Skip attachments
        continue;
      }
      if (bodyPart.isMimeType("text/plain")) {
        // Plain text part
        result.append(bodyPart.getContent().toString());
      } else if (bodyPart.isMimeType("text/html")) {
        // HTML part (optional: strip HTML tags if needed)
        String html = (String) bodyPart.getContent();
        // Use a library like JSoup to convert HTML to plain text if necessary
        result.append(html);
      } else if (bodyPart.getContent() instanceof MimeMultipart) {
        // Nested multipart
        result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
      }
    }
    return result.toString();
  }

  private static @NotNull String extractEmailVerifyToken() throws MessagingException, IOException {
    assertFalse(wiser.getMessages().isEmpty());
    WiserMessage wiserMessage = wiser.getMessages().get(0);
    assertNotNull(wiserMessage);
    MimeMessage verificationMessage = wiserMessage.getMimeMessage();
    assertNotNull(verificationMessage);
    String verificationEmail = getTextFromMessage(verificationMessage);
    assertNotNull(verificationEmail);
    assertFalse(verificationEmail.isEmpty());
    String verifyToken =
        verificationEmail.substring(
            verificationEmail.indexOf("?token=") + 7,
            verificationEmail.indexOf("You must respond"));
    verifyToken = verifyToken.replaceAll("[\\n\\r]", "");
    return verifyToken;
  }

  private static void setSystemProperty(String property, String value) {
    ResponseEntity<String> systemSettingsResp =
        postWithAdminBasicAuth("/systemSettings/" + property + "?value=" + value, null);
    assertEquals(HttpStatus.OK, systemSettingsResp.getStatusCode());
  }
}
