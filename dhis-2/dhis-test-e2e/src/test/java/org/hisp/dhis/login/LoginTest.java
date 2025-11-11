/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.LoginResponse.STATUS;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.jboss.aerogear.security.otp.Totp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.subethamail.wiser.WiserMessage;

@Tag("logintests")
@Slf4j
public class LoginTest extends BaseE2ETest {

  @BeforeAll
  static void setup() throws JsonProcessingException {
    startSMTPServer();
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = TestConfiguration.get().baseUrl().replace("/api", "/");
    orgUnitUID = createOrgUnit();
  }

  @AfterEach
  void tearDown() {
    wiser.getMessages().clear();
    invalidateAllSession();
  }

  @Test
  void testPreLoginCreatesNoCookie() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_HTML);
    ResponseEntity<String> response =
        exchangeWithHeaders(restTemplate, "/", HttpMethod.GET, null, headers);
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertNull(cookies, "Cookies should be null, we don't want session creation on the login page");
  }

  @Test
  void testDefaultLogin() throws JsonProcessingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    createSuperuser(username, password, orgUnitUID);

    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(loginResponse, DEFAULT_LOGIN_REDIRECT);
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
      restTemplate.postForEntity(serverApiUrl + LOGIN_API_PATH, requestEntity, LoginResponse.class);
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
    assertLoginStatus(failedLoginResp, STATUS.INCORRECT_TWO_FACTOR_CODE_TOTP);

    // Disable TOTP 2FA
    disable2FAWithTOTP(qrSecretAndCookie);

    // Test Login works without 2FA code
    ResponseEntity<LoginResponse> successfulLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(successfulLoginResp, DEFAULT_LOGIN_REDIRECT);
  }

  @Test
  void testDisableEmail2FA() throws IOException, MessagingException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    String cookie = enrollAndLoginEmail2FA(username, password);

    // Test Login doesn't work without 2FA code
    ResponseEntity<LoginResponse> failedLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginStatus(failedLoginResp, STATUS.EMAIL_TWO_FACTOR_CODE_SENT);

    // Disable Email 2FA
    disable2FAWithEmail(cookie);

    // Test Login works without 2FA code
    ResponseEntity<LoginResponse> successfulLoginResp =
        loginWithUsernameAndPassword(username, password, null);
    assertLoginSuccess(successfulLoginResp, DEFAULT_LOGIN_REDIRECT);
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
            serverApiUrl + "/account/verifyEmail?token=WRONGTOKEN",
            HttpMethod.GET,
            new HttpEntity<>(new HashMap<>(), jsonHeaders()),
            String.class);
    assertEquals(HttpStatus.FOUND, response.getStatusCode());
    List<String> location = response.getHeaders().get("Location");
    assertEquals(serverHostUrl + "login/#/email-verification-failure", location.get(0));
  }

  @Test
  void testRedirectWithQueryParam() {
    assertRedirectUrl("/api/users?fields=id,name,displayName", DEFAULT_LOGIN_REDIRECT, false);
  }

  @Test
  void testRedirectWithoutQueryParam() {
    assertRedirectUrl("/api/users", DEFAULT_LOGIN_REDIRECT, false);
  }

  @Test
  void testRedirectToResource() {
    assertRedirectUrl("/users/resource.js", DEFAULT_LOGIN_REDIRECT, false);
  }

  @Test
  void testRedirectToHtmlResource() {
    assertRedirectUrl("/users/resource.html", DEFAULT_LOGIN_REDIRECT, false);
  }

  @Test
  void testRedirectToSlashEnding() {
    assertRedirectUrl("/users/", DEFAULT_LOGIN_REDIRECT, false);
  }

  @Test
  void testRedirectToResourceWorker() {
    assertRedirectUrl("/dhis-web-dashboard/service-worker.js", DEFAULT_LOGIN_REDIRECT, true);
  }

  @Test
  void testRedirectToCssResourceWorker() {
    assertRedirectUrl(
        "/dhis-web-dashboard/static/css/main.4536e618.css", DEFAULT_LOGIN_REDIRECT, true);
  }

  @Test
  void testRedirectAccountWhenVerifiedEmailEnforced() {
    changeSystemSetting("enforceVerifiedEmail", "true");
    try {
      assertRedirectUrl("/dhis-web-dashboard/", "/dhis-web-user-profile/#/profile", true);
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
    testRedirectWhenLoggedIn("dhis-web-dashboard", "dhis-web-dashboard/");
  }

  @Test
  void testRedirectIcon() {
    testRedirectAsUser(
        "/api/icons/medicines_positive/icon.svg", "api/icons/medicines_positive/icon");
  }

  @Test
  void testJsonResponseForFailedLogin() {
    try {
      getWithWrongAuth("/me", Map.of());
      fail("Should have thrown an exception");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
      // Verify response is JSON format
      String responseBody = e.getResponseBodyAsString();
      assertTrue(responseBody.startsWith("{"), "Response should be in JSON format");
      try {
        // Parse response and verify it contains expected fields
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertTrue(jsonNode.has("message"), "JSON response should contain 'message' field");
        assertTrue(jsonNode.has("httpStatus"), "JSON response should contain 'httpStatus' field");
        assertTrue(
            jsonNode.has("httpStatusCode"), "JSON response should contain 'httpStatusCode' field");
        assertEquals("Unauthorized", jsonNode.get("httpStatus").asText());
        assertEquals(401, jsonNode.get("httpStatusCode").asInt());
      } catch (JsonProcessingException ex) {
        fail("Response is not valid JSON: " + responseBody);
      }
    }
  }

  @Test
  void testLoginFallback() {
    HttpHeaders headers = jsonHeaders();
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response =
        getRestTemplateNoRedirects()
            .exchange(serverHostUrl + "login.html", HttpMethod.GET, entity, String.class);
    HttpStatusCode statusCode = response.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);
  }

  // --------------------------------------------------------------------------------------------
  // Helper classes and records
  // --------------------------------------------------------------------------------------------

  public record QrSecretAndCookie(String secret, String cookie) {}

  // --------------------------------------------------------------------------------------------
  // 2FA enrollment / disabling methods
  // --------------------------------------------------------------------------------------------

  public QrSecretAndCookie enrollAndLoginTOTP2FA(String username, String password)
      throws JsonProcessingException {
    createSuperuser(username, password, orgUnitUID);

    String cookie = performInitialLogin(username, password);

    String base32Secret = enrollTOTP2FA(cookie);

    // Attempt login with a new TOTP code
    cookie = loginWith2FA(username, password, new Totp(base32Secret).now());

    return new QrSecretAndCookie(base32Secret, cookie);
  }

  public static String enrollAndLoginEmail2FA(String username, String password)
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

  public static void enrollEmail2FA(String cookie) throws MessagingException, IOException {
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

  public static void verifyEmail(String cookie) throws MessagingException, IOException {
    sendVerificationEmail(cookie);
    String verifyToken = extractEmailVerifyToken();
    verifyEmailWithToken(cookie, verifyToken);
  }

  public void disable2FAWithTOTP(QrSecretAndCookie qrSecretAndCookie)
      throws JsonProcessingException {
    // Generate TOTP code
    String code = new Totp(qrSecretAndCookie.secret()).now();
    ResponseEntity<String> disableResp =
        postWithCookie("/2fa/disable", Map.of("code", code), qrSecretAndCookie.cookie());
    assertEquals(HttpStatus.OK, disableResp.getStatusCode());
    assertMessage(disableResp, "2FA was disabled successfully");
  }

  public void disable2FAWithEmail(String cookie) throws IOException, MessagingException {
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
  // public helper methods for TOTP enrollment steps
  // --------------------------------------------------------------------------------------------

  public static String enrollTOTP2FA(String cookie) throws JsonProcessingException {
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

  public static String getBase32SecretFromQR(String cookie) throws JsonProcessingException {
    ResponseEntity<String> showQRResp = getWithCookie("/2fa/qrCodeJson", cookie);
    JsonNode showQrRespJson = objectMapper.readTree(showQRResp.getBody());
    String base32Secret = showQrRespJson.get("base32Secret").asText();
    assertNotNull(base32Secret);
    return base32Secret;
  }

  public static void enableTOTP2FA(String cookie, String base32Secret)
      throws JsonProcessingException {
    String enable2FACode = new Totp(base32Secret).now();
    Map<String, String> enable2FAReqBody = Map.of("code", enable2FACode);
    ResponseEntity<String> enable2FAResp = postWithCookie("/2fa/enable", enable2FAReqBody, cookie);
    assertMessage(enable2FAResp, "2FA was enabled successfully");
  }

  // --------------------------------------------------------------------------------------------
  // public helper methods for email verification steps
  // --------------------------------------------------------------------------------------------

  public static void sendVerificationEmail(String cookie) {
    ResponseEntity<String> sendVerificationEmailResp =
        postWithCookie("/account/sendEmailVerification", null, cookie);
    assertEquals(HttpStatus.CREATED, sendVerificationEmailResp.getStatusCode());
  }

  public static void verifyEmailWithToken(String cookie, String verifyToken) {
    ResponseEntity<String> verifyEmailResp =
        getWithCookie(
            getRestTemplateNoRedirects(), "/account/verifyEmail?token=" + verifyToken, cookie);
    assertEquals(HttpStatus.FOUND, verifyEmailResp.getStatusCode());
    List<String> location = verifyEmailResp.getHeaders().get("Location");
    assertEquals(serverHostUrl + "login/#/email-verification-success", location.get(0));
  }

  public static void testRedirectWhenLoggedIn(String url, String redirectUrl) {
    // Disable auto-redirects
    RestTemplate restTemplateNoRedirects = getRestTemplateNoRedirects();

    // Do an invalid login to capture URL request, if valid for save request
    ResponseEntity<LoginResponse> firstResponse =
        restTemplateNoRedirects.postForEntity(
            serverHostUrl + url,
            new HttpEntity<>(
                LoginRequest.builder().username("username").password("password").build(),
                new HttpHeaders()),
            LoginResponse.class);

    HttpHeaders cookieHeaders = jsonHeaders();
    List<String> cookies = firstResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
    if (cookies != null) {
      String cookie = cookies.get(0);
      cookieHeaders.set("Cookie", cookie);
    }

    // Do a valid login
    ResponseEntity<LoginResponse> secondResponse =
        restTemplateNoRedirects.postForEntity(
            serverApiUrl + LOGIN_API_PATH,
            new HttpEntity<>(
                LoginRequest.builder().username("admin").password("district").build(),
                cookieHeaders),
            LoginResponse.class);

    String cookie = extractSessionCookie(secondResponse);

    // Test the redirect
    HttpHeaders headers = jsonHeaders();
    headers.set("Cookie", cookie);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> redirResp =
        restTemplateNoRedirects.exchange(
            serverHostUrl + "dhis-web-dashboard", HttpMethod.GET, entity, String.class);
    List<String> location = redirResp.getHeaders().get("Location");
    assertNotNull(location);
    assertEquals(1, location.size());
    String actual = location.get(0);
    assertEquals(redirectUrl, actual.replaceAll(serverHostUrl, ""));
  }

  public static void testRedirectAsUser(String url, String redirectUrl) {
    // Disable auto-redirects
    RestTemplate restTemplateNoRedirects = getRestTemplateNoRedirects();

    // Do a valid login
    HttpHeaders cookieHeaders = jsonHeaders();
    ResponseEntity<LoginResponse> secondResponse =
        restTemplateNoRedirects.postForEntity(
            serverApiUrl + LOGIN_API_PATH,
            new HttpEntity<>(
                LoginRequest.builder().username("admin").password("district").build(),
                cookieHeaders),
            LoginResponse.class);
    String cookie = extractSessionCookie(secondResponse);

    // Test the redirect
    HttpHeaders headers = jsonHeaders();
    headers.set("Cookie", cookie);
    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<String> redirResp =
        restTemplateNoRedirects.exchange(serverHostUrl + url, HttpMethod.GET, entity, String.class);

    List<String> location = redirResp.getHeaders().get("Location");
    assertNotNull(location);
    assertEquals(1, location.size());
    String actual = location.get(0);
    assertEquals(redirectUrl, actual.replaceAll(serverHostUrl, ""));
  }

  // --------------------------------------------------------------------------------------------
  // public helper methods for parsing and extracting content from emails
  // --------------------------------------------------------------------------------------------

  public static @Nonnull String extract2FACodeFromLatestEmail()
      throws MessagingException, IOException {
    List<WiserMessage> messages = wiser.getMessages();
    String text = getTextFromMessage(messages.get(messages.size() - 1).getMimeMessage());
    return text.substring(text.indexOf("code:") + 7, text.indexOf("code:") + 13);
  }

  public static @Nonnull String extractEmailVerifyToken() throws MessagingException, IOException {
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
}
