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
package org.hisp.dhis.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.CodeGenerator;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.hisp.dhis.uitest.ConsentPage;
import org.hisp.dhis.uitest.LoginPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

@Tag("oauth2tests")
@Slf4j
class OAuth2Test extends BaseE2ETest {

  private static final String REDIRECT_URI = "http://localhost:9090/oauth2/code/dhis2-client";

  private static final String AUTHORIZE_PARAMS =
      "/oauth2/authorize?response_type=code&client_id=dhis2-client"
          + "&redirect_uri="
          + REDIRECT_URI
          + "&scope=openid%20email";

  private WebDriver driver;

  // Selenium URL if running locally
  private static String seleniumUrl = "http://localhost:4444";

  private static URL getSeleniumUrl() {
    String seleniumUrl = TestConfiguration.get().seleniumUrl();
    try {
      return new URL(seleniumUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeAll
  static void setup() throws JsonProcessingException {
    seleniumUrl = getSeleniumUrl().toString();
    serverApiUrl = TestConfiguration.get().baseUrl();

    // When testing with docker, use this: http://host.docker.internal:8080
    serverHostUrl = TestConfiguration.get().baseUrl().replace("/api", "");

    log.info(
        "[setup] seleniumUrl={}, serverApiUrl={}, serverHostUrl={}",
        seleniumUrl,
        serverApiUrl,
        serverHostUrl);

    orgUnitUID = createOrgUnit();
    log.info("[setup] created orgUnit uid={}", orgUnitUID);

    setupOAuth2Client();
    log.info("[setup] OAuth2 client created");
  }

  static void setupOAuth2Client() throws JsonProcessingException {
    Map<String, Object> clientMap =
        Map.of(
            "clientId", "dhis2-client",
            "clientSecret", "$2a$12$FtWBAB.hWkR3SSul7.HWROr8/aEuUEjywnB86wrYz0HtHh4iam6/G",
            "clientAuthenticationMethods", "client_secret_post",
            "authorizationGrantTypes", "refresh_token,authorization_code",
            "redirectUris", REDIRECT_URI,
            "postLogoutRedirectUris", "http://127.0.0.1:8080/",
            "scopes", "email_verified,openid,profile,email");

    // Create client
    ResponseEntity<String> response = postAsAdmin(serverApiUrl + "/oAuth2Clients", clientMap);
    JsonNode fullResponseNode = objectMapper.readTree(response.getBody());

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    String uid = fullResponseNode.get("response").get("uid").asText();
    assertNotNull(uid);

    response = postAsAdmin(serverApiUrl + "/oAuth2Clients", clientMap);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @AfterEach
  void cleanupDriver() {
    if (driver != null) {
      try {
        driver.quit();
      } catch (Exception e) {
        log.warn("[cleanupDriver] failed to quit WebDriver", e);
      }
      driver = null;
    }
  }

  @AfterAll
  static void tearDown() {
    invalidateAllSession();
  }

  @RepeatedTest(value = 10, name = "Get Access Token Test {currentRepetition}/{totalRepetitions}")
  void testGetAccessToken(TestInfo testInfo) throws MalformedURLException, JsonProcessingException {
    String testName = testInfo.getDisplayName();
    log.info("[{}] === START ===", testName);

    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    log.info("[{}] creating superuser username={}", testName, username);
    createSuperuser(username, password, orgUnitUID);
    log.info("[{}] superuser created", testName);

    driver = createDriver();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

    // Call the authorize endpoint
    String authorizeUrl = serverHostUrl + AUTHORIZE_PARAMS;
    log.info("[{}] navigating to authorize URL: {}", testName, authorizeUrl);
    driver.get(authorizeUrl);

    wait.until(ExpectedConditions.urlContains(serverHostUrl + "/login/"));
    String currentUrl = driver.getCurrentUrl();
    log.info("[{}] redirected to login page: {}", testName, currentUrl);
    assertEquals(serverHostUrl + "/login/", currentUrl);

    // Wait for the login page to load
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#username")));
    log.info("[{}] login page loaded, entering credentials", testName);

    // Login
    LoginPage mainPage = new LoginPage(driver);
    mainPage.inputUsername.sendKeys(username);
    mainPage.inputPassword.sendKeys(password);
    mainPage.inputSubmit.click();
    log.info("[{}] login submitted, waiting for consent page", testName);

    // Wait for the consent page to load
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".form-check")));
    String consentPageUrl = driver.getCurrentUrl();
    log.info("[{}] consent page loaded, url={}", testName, consentPageUrl);
    assertEquals(serverHostUrl + AUTHORIZE_PARAMS, consentPageUrl);

    // Give consent
    ConsentPage consentPage = new ConsentPage(driver);
    consentPage.emailCheckbox.click();
    consentPage.submitButton.click();
    log.info("[{}] consent submitted, waiting for redirect to {}", testName, REDIRECT_URI);

    // Wait for redirect and capture the URL atomically inside the wait callback.
    // The redirect goes to localhost:9090 which doesn't exist, so Chrome will quickly
    // navigate to an error page — we must capture the URL in the same poll cycle that
    // detects it, not in a separate getCurrentUrl() call afterwards.
    String redirectUrl =
        wait.until(
            d -> {
              String url = d.getCurrentUrl();
              return url.contains(REDIRECT_URI) ? url : null;
            });
    log.info("[{}] captured redirect URL: {}", testName, redirectUrl);

    String code = extractAuthorizationCode(redirectUrl, testName);

    log.info("[{}] authorization code extracted, length={}", testName, code.length());
    assertEquals(
        128,
        code.length(),
        "code has wrong size: '"
            + code
            + "', code length: "
            + code.length()
            + ", full redirectUrl: "
            + redirectUrl);

    // Call the token endpoint with the authorization code and get access token
    log.info("[{}] exchanging code for access token", testName);
    String accessToken = getAccessToken(code);
    assertNotNull(accessToken);
    log.info("[{}] access token received, length={}", testName, accessToken.length());

    String actualIssuerUri;
    String expectedIssuerUri = "http://web:8080/";

    // Decode the access_token
    try {
      SignedJWT signedJWT = SignedJWT.parse(accessToken);
      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
      actualIssuerUri = claimsSet.getIssuer();
      log.info("[{}] JWT issuer: {}", testName, actualIssuerUri);
    } catch (ParseException e) {
      log.error("[{}] JWT parsing failed, accessToken={}", testName, accessToken, e);
      throw new RuntimeException(e);
    }
    assertEquals(
        expectedIssuerUri,
        actualIssuerUri,
        "The JWT issuer URI should match the configured SERVER_BASE_URL.");

    // Call the /me endpoint with the access token
    log.info("[{}] calling /me with bearer JWT", testName);
    ResponseEntity<String> withBearerJwt = getWithBearerJwt(serverApiUrl + "/me", accessToken);
    HttpStatusCode statusCode = withBearerJwt.getStatusCode();
    String body = withBearerJwt.getBody();
    log.info("[{}] /me response: status={}, body={}", testName, statusCode, body);

    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
    assertNotNull(body);
    assertTrue(
        body.contains(
            "Found no matching DHIS2 user for the mapping claim: 'email' with the value:"));

    log.info("[{}] === PASS ===", testName);
  }

  /**
   * Regression test for LazyInitializationException on User.userRoles when authenticating with a
   * JWT bearer token. Before the fix, the JWT auth code accessed user.getAuthorities() outside a
   * Hibernate session, causing a 500 error. The fix uses currentUserDetails.getAuthorities()
   * instead, which has authorities eagerly loaded.
   */
  @Test
  void testBearerTokenAuthWithMatchingUser(TestInfo testInfo)
      throws MalformedURLException, JsonProcessingException {
    String testName = testInfo.getDisplayName();
    log.info("[{}] === START ===", testName);

    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    String email = username + "@email.com";

    // Create user with openId set to email so JWT bearer auth can find the user.
    // The JWT auth resolver maps the "email" claim to User.openId via getUserByOpenId().
    log.info(
        "[{}] creating user with openId mapping, username={}, email={}", testName, username, email);
    Map<String, Object> userMap =
        Map.of(
            "username",
            username,
            "password",
            password,
            "email",
            email,
            "openId",
            email,
            "userRoles",
            List.of(Map.of("id", SUPER_USER_ROLE_UID)),
            "firstName",
            "user",
            "surname",
            "userson",
            "organisationUnits",
            List.of(Map.of("id", orgUnitUID)));

    ResponseEntity<String> createResp = postWithAdminBasicAuth("/users", userMap);
    log.info(
        "[{}] user creation response: status={}, body={}",
        testName,
        createResp.getStatusCode(),
        createResp.getBody());
    JsonNode createJson = objectMapper.readTree(createResp.getBody());
    String uid = createJson.get("response").get("uid").asText();
    assertNotNull(uid);
    log.info("[{}] user created, uid={}", testName, uid);

    driver = createDriver();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

    // Start OAuth2 authorization code flow
    String authorizeUrl = serverHostUrl + AUTHORIZE_PARAMS;
    log.info("[{}] navigating to authorize URL: {}", testName, authorizeUrl);
    driver.get(authorizeUrl);

    wait.until(ExpectedConditions.urlContains(serverHostUrl + "/login/"));
    log.info("[{}] redirected to login page: {}", testName, driver.getCurrentUrl());

    // Login
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#username")));
    log.info("[{}] login page loaded, entering credentials", testName);
    LoginPage mainPage = new LoginPage(driver);
    mainPage.inputUsername.sendKeys(username);
    mainPage.inputPassword.sendKeys(password);
    mainPage.inputSubmit.click();
    log.info("[{}] login submitted, waiting for consent page", testName);

    // Give consent
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".form-check")));
    log.info("[{}] consent page loaded, url={}", testName, driver.getCurrentUrl());
    ConsentPage consentPage = new ConsentPage(driver);
    consentPage.emailCheckbox.click();
    consentPage.submitButton.click();
    log.info("[{}] consent submitted, waiting for redirect", testName);

    // Capture redirect URL atomically (see comment in testGetAccessToken)
    String redirectUrl =
        wait.until(
            d -> {
              String url = d.getCurrentUrl();
              return url.contains(REDIRECT_URI) ? url : null;
            });
    log.info("[{}] captured redirect URL: {}", testName, redirectUrl);

    String code = extractAuthorizationCode(redirectUrl, testName);
    assertFalse(code.isBlank(), "code is empty");
    log.info("[{}] authorization code extracted, length={}", testName, code.length());

    // Exchange code for access token
    log.info("[{}] exchanging code for access token", testName);
    String accessToken = getAccessToken(code);
    assertNotNull(accessToken);
    log.info("[{}] access token received, length={}", testName, accessToken.length());

    // Call /me with the JWT bearer token.
    // Before the fix this returned 500 (LazyInitializationException on User.userRoles).
    // After the fix this should return 200 with the authenticated user's details.
    log.info("[{}] calling /me with bearer JWT", testName);
    ResponseEntity<String> response = getWithBearerJwt(serverApiUrl + "/me", accessToken);
    log.info(
        "[{}] /me response: status={}, body={}",
        testName,
        response.getStatusCode(),
        response.getBody());
    assertEquals(
        HttpStatus.OK,
        response.getStatusCode(),
        "Bearer JWT auth should succeed, not fail with LazyInitializationException. Body: "
            + response.getBody());

    JsonNode meJson = objectMapper.readTree(response.getBody());
    assertEquals(username, meJson.get("username").asText());

    log.info("[{}] === PASS ===", testName);
  }

  public String getAccessToken(String code) throws JsonProcessingException {
    String body = callTokenEndpoint(code);
    assertNotNull(body, "Token endpoint returned null body");
    log.info("[getAccessToken] token endpoint response body: {}", body);
    JsonNode jsonNode = objectMapper.readTree(body);
    JsonNode accessToken = jsonNode.get("access_token");
    assertNotNull(accessToken, "No 'access_token' field in token response: " + body);
    return accessToken.asText();
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private WebDriver createDriver() throws MalformedURLException {
    ChromeOptions chromeOptions = new ChromeOptions();
    chromeOptions.addArguments("--remote-allow-origins=*");
    chromeOptions.addArguments("--no-sandbox");
    chromeOptions.addArguments("--disable-dev-shm-usage");
    chromeOptions.addArguments("--disable-gpu");
    chromeOptions.setPageLoadTimeout(Duration.ofSeconds(60));
    WebDriver wd = new RemoteWebDriver(new URL(seleniumUrl), chromeOptions);
    log.info("[createDriver] new WebDriver session created");
    return wd;
  }

  /**
   * Extracts the authorization code from a redirect URL like {@code
   * http://localhost:9090/oauth2/code/dhis2-client?code=XXXXX}.
   *
   * <p>Parses the query string properly instead of using brittle indexOf, and logs diagnostics for
   * CI debugging.
   */
  private static String extractAuthorizationCode(String redirectUrl, String testName) {
    assertNotNull(redirectUrl, "redirectUrl is null");
    assertFalse(redirectUrl.isBlank(), "redirectUrl is blank");

    int queryStart = redirectUrl.indexOf('?');
    assertTrue(
        queryStart > 0, "[" + testName + "] redirect URL has no query string: " + redirectUrl);

    String query = redirectUrl.substring(queryStart + 1);
    log.info("[{}] redirect query string: {}", testName, query);

    // Parse query params — code might not be the only parameter
    String code = null;
    for (String param : query.split("&")) {
      if (param.startsWith("code=")) {
        code = param.substring(5);
        break;
      }
    }

    assertNotNull(code, "[" + testName + "] no 'code' parameter in redirect URL: " + redirectUrl);
    assertFalse(
        code.isBlank(),
        "[" + testName + "] 'code' parameter is blank in redirect URL: " + redirectUrl);

    return code;
  }
}
