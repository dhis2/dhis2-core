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
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.login.LoginResponse.STATUS;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.jboss.aerogear.security.otp.Totp;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

@Tag("logintests")
@Slf4j
public class LoginTest {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final RestTemplate restTemplate = new RestTemplate();

  public static String dhis2ServerApi = "http://localhost:8080/api";
  public static String dhis2Server = "http://localhost:8080/";

  public static final String DEFAULT_DASHBOARD_PATH = "/dhis-web-dashboard/";
  private static final String LOGIN_API_PATH = "/auth/login";
  public static final String SUPER_USER_ROLE_UID = "yrB6vc5Ip3r";

  // Change this to "localhost" if you want to run the tests locally
  public static final String SMTP_HOSTNAME = "test";

  private static int smtpPort;
  private static Wiser wiser;

  private static String orgUnitUID;

  @BeforeAll
  static void setup() throws JsonProcessingException {
    startSMTPServer();
    dhis2ServerApi = TestConfiguration.get().baseUrl();
    dhis2Server = TestConfiguration.get().baseUrl().replace("/api", "/");
    orgUnitUID = createOrgUnit();
  }

  @AfterEach
  void tearDown() {
    wiser.getMessages().clear();
    invalidateAllSession();
  }

  @Test
  void testDefaultLogin() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    createSuperuser(username, password, orgUnitUID);

    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(loginResponse, DEFAULT_DASHBOARD_PATH);
    String cookie = extractSessionCookie(loginResponse);

    // Verify session cookie works
    ResponseEntity<String> meResponse = getWithCookie("/me", cookie);
    assertEquals(HttpStatus.OK, meResponse.getStatusCode());
    assertNotNull(meResponse.getBody());
  }

  @Test
  void testDefaultLoginFailure() {
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("wrongpassword").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, jsonHeaders());
    try {
      restTemplate.postForEntity(
          dhis2ServerApi + LOGIN_API_PATH, requestEntity, LoginResponse.class);
      fail("Should have thrown an exception");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }
  }

  @Test
  void testLoginWithTOTP2FA() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    enrollAndLoginTOTP2FA(username, password);
  }

  @Test
  void testLoginWithEmail2FA() throws IOException, MessagingException {
    try {
      String username = CodeGenerator.generateCode(8).toLowerCase();
      String password = "Test123###...";
      enrollAndLoginEmail2FA(username, password);
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

  @Test
  void testDisableTOTP2FA() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    QrSecretAndCookie qrSecretAndCookie = enrollAndLoginTOTP2FA(username, password);

    // Test Login doesn't work without 2FA code
    ResponseEntity<LoginResponse> failedLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginStatus(failedLoginResp, STATUS.INCORRECT_TWO_FACTOR_CODE);

    // Disable TOTP 2FA
    disable2FAWithTOTP(qrSecretAndCookie);

    // Test Login works without 2FA code
    ResponseEntity<LoginResponse> successfulLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(successfulLoginResp, DEFAULT_DASHBOARD_PATH);
  }

  @Test
  void testDisableEmail2FA() throws IOException, MessagingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    String cookie = enrollAndLoginEmail2FA(username, password);

    // Test Login doesn't work without 2FA code
    ResponseEntity<LoginResponse> failedLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginStatus(failedLoginResp, STATUS.INCORRECT_TWO_FACTOR_CODE);

    // Disable Email 2FA
    disable2FAWithEmail(cookie);

    // Test Login works without 2FA code
    ResponseEntity<LoginResponse> successfulLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(successfulLoginResp, DEFAULT_DASHBOARD_PATH);
  }

  @Test
  void testShowQrCodeAfterEnabledFails() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    QrSecretAndCookie qrSecretAndCookie = enrollAndLoginTOTP2FA(username, password);

    // Should fail since 2FA is already enabled
    ResponseEntity<String> getQrJsonResp =
        getWithCookie("/2fa/qrCodeJson", qrSecretAndCookie.cookie());
    assertEquals(HttpStatus.CONFLICT, getQrJsonResp.getStatusCode());
    assertMessage(getQrJsonResp, "User is not in TOTP 2FA enrollment mode");

    ResponseEntity<String> getQrPngResp =
        getWithCookie("/2fa/qrCodePng", qrSecretAndCookie.cookie());
    assertEquals(HttpStatus.CONFLICT, getQrPngResp.getStatusCode());
    assertMessage(getQrPngResp, "User is not in TOTP 2FA enrollment mode");
  }

  @Test
  void testReEnrollFails() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    QrSecretAndCookie qrSecretAndCookie = enrollAndLoginTOTP2FA(username, password);

    ResponseEntity<String> enrollTOTPResp =
        postWithCookie("/2fa/enrollTOTP2FA", null, qrSecretAndCookie.cookie());
    assertEquals(HttpStatus.CONFLICT, enrollTOTPResp.getStatusCode());
    assertMessage(
        enrollTOTPResp, "User has 2FA enabled already, disable 2FA before you try to enroll again");

    ResponseEntity<String> enrollEmailResp =
        postWithCookie("/2fa/enrollEmail2FA", null, qrSecretAndCookie.cookie());
    assertEquals(HttpStatus.CONFLICT, enrollEmailResp.getStatusCode());
    assertMessage(
        enrollEmailResp,
        "User has 2FA enabled already, disable 2FA before you try to enroll again");
  }

  @Test
  void redirectAfterEmailVerificationFailure() {
    RestTemplate template = addAdminBasicAuthHeaders(getRestTemplateNoRedirects());
    ResponseEntity<String> response =
        template.exchange(
            dhis2ServerApi + "/account/verifyEmail?token=WRONGTOKEN",
            HttpMethod.GET,
            new HttpEntity<>(new HashMap(), jsonHeaders()),
            String.class);
    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    List<String> location = response.getHeaders().get("Location");
    assertEquals(dhis2Server + "dhis-web-login/#/email-verification-failure", location.get(0));
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
    assertRedirectUrl("/users/resource.js", DEFAULT_DASHBOARD_PATH);
  }

  @Test
  void testRedirectToHtmlResource() {
    assertRedirectToSameUrl("/users/resource.html");
  }

  @Test
  void testRedirectToSlashEnding() {
    assertRedirectToSameUrl("/users/");
  }

  @Test
  void testRedirectToResourceWorker() {
    assertRedirectUrl("/dhis-web-dashboard/service-worker.js", DEFAULT_DASHBOARD_PATH);
  }

  @Test
  void testRedirectToCssResourceWorker() {
    assertRedirectUrl("/dhis-web-dashboard/static/css/main.4536e618.css", DEFAULT_DASHBOARD_PATH);
  }

  @Test
  void testRedirectAccountWhenVerifiedEmailEnforced() {
    changeSystemSetting("enforceVerifiedEmail", "true");
    try {
      assertRedirectUrl("/dhis-web-dashboard/", "/dhis-web-user-profile/#/profile");
    } finally {
      changeSystemSetting("enforceVerifiedEmail", "false");
    }
  }

  @Test
  void testRedirectEndingSlash() {
    assertRedirectToSameUrl("/dhis-web-dashboard/");
  }

  @Test
  void testRedirectMissingEndingSlash() {
    testRedirectWhenLoggedIn("/dhis-web-dashboard", "/dhis-web-dashboard/");
  }

  // --------------------------------------------------------------------------------------------
  // Helper classes and records
  // --------------------------------------------------------------------------------------------

  public record QrSecretAndCookie(String secret, String cookie) {}

  // --------------------------------------------------------------------------------------------
  // Private helper methods for starting servers and setup
  // --------------------------------------------------------------------------------------------

  private static void startSMTPServer() {
    smtpPort = findAvailablePort();
    wiser = new Wiser();
    wiser.setHostname(SMTP_HOSTNAME);
    wiser.setPort(smtpPort);
    wiser.start();
  }

  private static void invalidateAllSession() {
    ResponseEntity<String> response = deleteWithAdminBasicAuth("/sessions", null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  // --------------------------------------------------------------------------------------------
  // 2FA enrollment / disabling methods
  // --------------------------------------------------------------------------------------------

  private QrSecretAndCookie enrollAndLoginTOTP2FA(String username, String password)
      throws JsonProcessingException {
    createSuperuser(username, password, orgUnitUID);

    String cookie = performInitialLogin(username, password);

    String base32Secret = enrollTOTP2FA(cookie);

    // Attempt login with a new TOTP code
    cookie = loginWith2FA(username, password, new Totp(base32Secret).now());

    return new QrSecretAndCookie(base32Secret, cookie);
  }

  private static String enrollAndLoginEmail2FA(String username, String password)
      throws IOException, MessagingException {
    createSuperuser(username, password, orgUnitUID);

    // First login
    String cookie = performInitialLogin(username, password);

    // Enable email 2FA in system settings
    configureEmail2FASettings(cookie);

    // Try enrolling email 2FA, should fail without verified email
    ResponseEntity<String> twoFAResp = postWithCookie("/2fa/enrollEmail2FA", null, cookie);
    assertEquals(HttpStatus.CONFLICT, twoFAResp.getStatusCode());
    assertMessage(
        twoFAResp,
        "User does not have a verified email, please verify your email before you try to enable 2FA");

    verifyEmail(cookie);

    enrollEmail2FA(cookie);

    // Attempt to login with email 2FA
    loginWith2FA(username, password, extract2FACodeFromLatestEmail());

    return cookie;
  }

  private static void enrollEmail2FA(String cookie) throws MessagingException, IOException {
    // Enroll in Email 2FA successfully
    ResponseEntity<String> twoFAEnableResp = postWithCookie("/2fa/enrollEmail2FA", null, cookie);
    assertEquals(HttpStatus.OK, twoFAEnableResp.getStatusCode());
    assertMessage(
        twoFAEnableResp,
        "The user has enrolled in email-based 2FA, a code was generated and sent successfully to the user's email");

    // Enable Email 2FA
    String enroll2FACode = extract2FACodeFromLatestEmail();
    ResponseEntity<String> enableResp =
        postWithCookie("/2fa/enable", Map.of("code", enroll2FACode), cookie);
    assertEquals(HttpStatus.OK, enableResp.getStatusCode());
    assertMessage(enableResp, "2FA was enabled successfully");
  }

  private static void verifyEmail(String cookie) throws MessagingException, IOException {
    sendVerificationEmail(cookie);
    String verifyToken = extractEmailVerifyToken();
    verifyEmailWithToken(cookie, verifyToken);
  }

  private void disable2FAWithTOTP(QrSecretAndCookie qrSecretAndCookie)
      throws JsonProcessingException {
    // Generate TOTP code
    String code = new Totp(qrSecretAndCookie.secret()).now();
    ResponseEntity<String> disableResp =
        postWithCookie("/2fa/disable", Map.of("code", code), qrSecretAndCookie.cookie());
    assertEquals(HttpStatus.OK, disableResp.getStatusCode());
    assertMessage(disableResp, "2FA was disabled successfully");
  }

  private void disable2FAWithEmail(String cookie) throws IOException, MessagingException {
    Map<String, String> emptyCode = Map.of("code", "");
    ResponseEntity<String> disableFailResp = postWithCookie("/2fa/disable", emptyCode, cookie);
    assertEquals(HttpStatus.CONFLICT, disableFailResp.getStatusCode());
    assertMessage(disableFailResp, "2FA code was sent to the users email");
    String disable2FACode = extract2FACodeFromLatestEmail();

    ResponseEntity<String> disableOkResp =
        postWithCookie("/2fa/disable", Map.of("code", disable2FACode), cookie);
    assertEquals(HttpStatus.OK, disableOkResp.getStatusCode());
    assertMessage(disableOkResp, "2FA was disabled successfully");
  }

  // --------------------------------------------------------------------------------------------
  // Private helper methods for login and assertions
  // --------------------------------------------------------------------------------------------

  private static String performInitialLogin(String username, String password) {
    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(loginResponse, DEFAULT_DASHBOARD_PATH);
    return extractSessionCookie(loginResponse);
  }

  private static String loginWith2FA(String username, String password, String twoFACode) {
    ResponseEntity<LoginResponse> login2FAResp =
        loginWithUsernameAndPassword(username, password, twoFACode);
    assertLoginSuccess(login2FAResp, DEFAULT_DASHBOARD_PATH);
    return extractSessionCookie(login2FAResp);
  }

  // --------------------------------------------------------------------------------------------
  // Private helper methods for TOTP enrollment steps
  // --------------------------------------------------------------------------------------------

  private static String enrollTOTP2FA(String cookie) throws JsonProcessingException {
    ResponseEntity<String> twoFAResp = postWithCookie("/2fa/enrollTOTP2FA", null, cookie);
    assertMessage(
        twoFAResp,
        "The user has enrolled in TOTP 2FA, call the QR code endpoint to continue the process");

    // Get base32 TOTP secret from QR code
    String base32Secret = getBase32SecretFromQR(cookie);

    // Enable TOTP 2FA
    enableTOTP2FA(cookie, base32Secret);

    return base32Secret;
  }

  private static String getBase32SecretFromQR(String cookie) throws JsonProcessingException {
    ResponseEntity<String> showQRResp = getWithCookie("/2fa/qrCodeJson", cookie);
    JsonNode showQrRespJson = objectMapper.readTree(showQRResp.getBody());
    String base32Secret = showQrRespJson.get("base32Secret").asText();
    assertNotNull(base32Secret);
    return base32Secret;
  }

  private static void enableTOTP2FA(String cookie, String base32Secret)
      throws JsonProcessingException {
    String enable2FACode = new Totp(base32Secret).now();
    Map<String, String> enable2FAReqBody = Map.of("code", enable2FACode);
    ResponseEntity<String> enable2FAResp = postWithCookie("/2fa/enable", enable2FAReqBody, cookie);
    assertMessage(enable2FAResp, "2FA was enabled successfully");
  }

  // --------------------------------------------------------------------------------------------
  // Private helper methods for email verification steps
  // --------------------------------------------------------------------------------------------

  private static void configureEmail2FASettings(String cookie) {
    setSystemPropertyWithCookie("email2FAEnabled", "true", cookie);
    setSystemPropertyWithCookie("keyEmailHostName", SMTP_HOSTNAME, cookie);
    setSystemPropertyWithCookie("keyEmailPort", String.valueOf(smtpPort), cookie);
    setSystemPropertyWithCookie("keyEmailUsername", "nils", cookie);
    setSystemPropertyWithCookie("keyEmailSender", "system@nils.no", cookie);
    setSystemPropertyWithCookie("keyEmailTls", "false", cookie);
  }

  private static void sendVerificationEmail(String cookie) {
    ResponseEntity<String> sendVerificationEmailResp =
        postWithCookie("/account/sendEmailVerification", null, cookie);
    assertEquals(HttpStatus.CREATED, sendVerificationEmailResp.getStatusCode());
  }

  private static void verifyEmailWithToken(String cookie, String verifyToken) {
    ResponseEntity<String> verifyEmailResp =
        getWithCookie(
            getRestTemplateNoRedirects(), "/account/verifyEmail?token=" + verifyToken, cookie);
    assertEquals(HttpStatus.FOUND, verifyEmailResp.getStatusCode());
    List<String> location = verifyEmailResp.getHeaders().get("Location");
    assertEquals(dhis2Server + "dhis-web-login/#/email-verification-success", location.get(0));
  }

  // --------------------------------------------------------------------------------------------
  // Private helper methods for assertions
  // --------------------------------------------------------------------------------------------

  private static void assertLoginSuccess(
      ResponseEntity<LoginResponse> response, String expectedRedirectUrl) {
    assertLoginStatus(response, STATUS.SUCCESS);
    assertNotNull(response.getBody());
    assertEquals(expectedRedirectUrl, response.getBody().getRedirectUrl());
  }

  private static void assertLoginStatus(ResponseEntity<LoginResponse> response, STATUS status) {
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(status, response.getBody().getLoginStatus());
  }

  private static void assertMessage(ResponseEntity<String> response, String expectedMessage)
      throws JsonProcessingException {
    assertNotNull(response);
    JsonNode jsonResponse = objectMapper.readTree(response.getBody());
    assertEquals(expectedMessage, jsonResponse.get("message").asText());
  }

  // --------------------------------------------------------------------------------------------
  // Private helper methods for redirect assertions
  // --------------------------------------------------------------------------------------------

  private static void assertRedirectToSameUrl(String url) {
    assertRedirectUrl(url, url);
  }

  private static void assertRedirectUrl(String url, String redirectUrl) {
    // Do an invalid login to store original URL request
    ResponseEntity<LoginResponse> firstResponse =
        restTemplate.postForEntity(dhis2Server + url, null, LoginResponse.class);
    String cookie = firstResponse.getHeaders().get(HttpHeaders.SET_COOKIE).get(0);

    // Do a valid login with the captured cookie
    HttpHeaders getHeaders = jsonHeaders();
    getHeaders.set("Cookie", cookie);
    LoginRequest loginRequest =
        LoginRequest.builder().username("admin").password("district").build();
    HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest, getHeaders);

    ResponseEntity<LoginResponse> loginResponse =
        restTemplate.postForEntity(
            dhis2ServerApi + LOGIN_API_PATH, requestEntity, LoginResponse.class);

    assertNotNull(loginResponse);
    assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    LoginResponse body = loginResponse.getBody();
    assertNotNull(body);
    assertEquals(STATUS.SUCCESS, body.getLoginStatus());
    assertEquals(redirectUrl, body.getRedirectUrl());
  }

  private static void testRedirectWhenLoggedIn(String url, String redirectUrl) {
    // Disable auto-redirects
    RestTemplate restTemplateNoRedirects = getRestTemplateNoRedirects();

    // Do an invalid login to capture URL request
    ResponseEntity<LoginResponse> firstResponse =
        restTemplateNoRedirects.postForEntity(
            dhis2Server + url,
            new HttpEntity<>(
                LoginRequest.builder().username("username").password("password").build(),
                new HttpHeaders()),
            LoginResponse.class);
    String cookie = firstResponse.getHeaders().get(HttpHeaders.SET_COOKIE).get(0);

    // Do a valid login
    HttpHeaders cookieHeaders = jsonHeaders();
    cookieHeaders.set("Cookie", cookie);
    ResponseEntity<LoginResponse> secondResponse =
        restTemplateNoRedirects.postForEntity(
            dhis2ServerApi + LOGIN_API_PATH,
            new HttpEntity<>(
                LoginRequest.builder().username("admin").password("district").build(),
                cookieHeaders),
            LoginResponse.class);
    cookie = extractSessionCookie(secondResponse);

    // Test the redirect
    HttpHeaders headers = jsonHeaders();
    headers.set("Cookie", cookie);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> redirResp =
        restTemplateNoRedirects.exchange(
            dhis2Server + "/dhis-web-dashboard", HttpMethod.GET, entity, String.class);
    List<String> location = redirResp.getHeaders().get("Location");
    assertNotNull(location);
    assertEquals(1, location.size());
    String actual = location.get(0);
    assertEquals(redirectUrl, actual.replaceAll(dhis2Server, ""));
  }

  // --------------------------------------------------------------------------------------------
  // Private helper methods for HTTP calls
  // --------------------------------------------------------------------------------------------

  private static HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private static ResponseEntity<LoginResponse> loginWithUsernameAndPassword(
      String username, String password, String twoFACode) {
    HttpHeaders headers = jsonHeaders();
    LoginRequest loginRequest =
        LoginRequest.builder()
            .username(username)
            .password(password)
            .twoFactorCode(twoFACode)
            .build();
    return restTemplate.postForEntity(
        dhis2ServerApi + LOGIN_API_PATH,
        new HttpEntity<>(loginRequest, headers),
        LoginResponse.class);
  }

  private static ResponseEntity<String> postWithCookie(String path, Object body, String cookie) {
    HttpHeaders headers = jsonHeaders();
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

  private static ResponseEntity<String> getWithCookie(
      RestTemplate template, String path, String cookie) {
    HttpHeaders headers = jsonHeaders();
    headers.set("Cookie", cookie);
    return exchangeWithHeaders(template, path, HttpMethod.GET, null, headers);
  }

  private static ResponseEntity<String> getWithCookie(String path, String cookie) {
    return getWithCookie(restTemplate, path, cookie);
  }

  private static ResponseEntity<String> getWithAdminBasicAuth(
      String path, Map<String, Object> map) {
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    return rt.exchange(
        dhis2ServerApi + path, HttpMethod.GET, new HttpEntity<>(map, jsonHeaders()), String.class);
  }

  private static ResponseEntity<String> postWithAdminBasicAuth(
      String path, Map<String, Object> map) {
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    return rt.exchange(
        dhis2ServerApi + path, HttpMethod.POST, new HttpEntity<>(map, jsonHeaders()), String.class);
  }

  private static ResponseEntity<String> deleteWithAdminBasicAuth(
      String path, Map<String, Object> map) {
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    return rt.exchange(
        dhis2ServerApi + path,
        HttpMethod.DELETE,
        new HttpEntity<>(map, jsonHeaders()),
        String.class);
  }

  private static ResponseEntity<String> exchangeWithHeaders(
      String path, HttpMethod method, Object body, HttpHeaders headers) {
    return exchangeWithHeaders(restTemplate, path, method, body, headers);
  }

  private static ResponseEntity<String> exchangeWithHeaders(
      RestTemplate template, String path, HttpMethod method, Object body, HttpHeaders headers) {
    try {
      return template.exchange(
          dhis2ServerApi + path, method, new HttpEntity<>(body, headers), String.class);
    } catch (HttpClientErrorException e) {
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    }
  }

  private static RestTemplate addAdminBasicAuthHeaders(RestTemplate template) {
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

  @NotNull
  private static RestTemplate getRestTemplateNoRedirects() {
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

  // --------------------------------------------------------------------------------------------
  // Private helper methods for parsing and extracting content from emails
  // --------------------------------------------------------------------------------------------

  private static @NotNull String extract2FACodeFromLatestEmail()
      throws MessagingException, IOException {
    List<WiserMessage> messages = wiser.getMessages();
    String text = getTextFromMessage(messages.get(messages.size() - 1).getMimeMessage());
    return text.substring(text.indexOf("code:") + 7, text.indexOf("code:") + 13);
  }

  private static String getTextFromMessage(Message message) throws MessagingException, IOException {
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

  private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart)
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

  private static @NotNull String extractEmailVerifyToken() throws MessagingException, IOException {
    assertFalse(wiser.getMessages().isEmpty());
    WiserMessage wiserMessage = wiser.getMessages().get(0);
    MimeMessage verificationMessage = wiserMessage.getMimeMessage();
    String verificationEmail = getTextFromMessage(verificationMessage);
    String verifyToken =
        verificationEmail.substring(
            verificationEmail.indexOf("?token=") + 7,
            verificationEmail.indexOf("You must respond"));
    return verifyToken.replaceAll("[\\n\\r]", "");
  }

  // --------------------------------------------------------------------------------------------
  // Private helper methods for server configuration and resource creation
  // --------------------------------------------------------------------------------------------

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

  private static String createOrgUnit() throws JsonProcessingException {
    ResponseEntity<String> jsonStringResponse =
        postWithAdminBasicAuth(
            "/organisationUnits",
            Map.of("name", "orgA", "shortName", "orgA", "openingDate", "2024-11-21T16:00:00.000Z"));

    JsonNode fullResponseNode = objectMapper.readTree(jsonStringResponse.getBody());
    String uid = fullResponseNode.get("response").get("uid").asText();
    assertNotNull(uid);
    return uid;
  }

  private static String extractSessionCookie(ResponseEntity<?> response) {
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertNotNull(cookies);
    assertEquals(1, cookies.size());
    return cookies.get(0);
  }

  private static void setSystemPropertyWithCookie(String property, String value, String cookie) {
    ResponseEntity<String> systemSettingsResp =
        postWithCookie("/systemSettings/" + property + "?value=" + value, null, cookie);
    assertEquals(HttpStatus.OK, systemSettingsResp.getStatusCode());
  }

  private static void setSystemProperty(String property, String value) {
    ResponseEntity<String> systemSettingsResp =
        postWithAdminBasicAuth("/systemSettings/" + property + "?value=" + value, null);
    assertEquals(HttpStatus.OK, systemSettingsResp.getStatusCode());
  }

  private static void changeSystemSetting(String key, String value) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    RestTemplate rt = addAdminBasicAuthHeaders(new RestTemplate());
    ResponseEntity<String> response =
        rt.exchange(
            dhis2ServerApi + "/systemSettings/" + key,
            HttpMethod.POST,
            new HttpEntity<>(value, headers),
            String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
