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
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.login.CodeGenerator;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.hisp.dhis.uitest.ConsentPage;
import org.hisp.dhis.uitest.LoginPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
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

  private static WebDriver driver;

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

    orgUnitUID = createOrgUnit();
    setupOAuth2Client();
  }

  static void setupOAuth2Client() throws JsonProcessingException {
    Map<String, Object> clientMap =
        Map.of(
            "clientId", "dhis2-client",
            "clientSecret", "$2a$12$FtWBAB.hWkR3SSul7.HWROr8/aEuUEjywnB86wrYz0HtHh4iam6/G",
            "clientAuthenticationMethods", "client_secret_post",
            "authorizationGrantTypes", "refresh_token,authorization_code",
            "redirectUris", "http://localhost:9090/oauth2/code/dhis2-client",
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

  @AfterAll
  static void tearDown() {
    if (driver != null) driver.quit();
    invalidateAllSession();
  }

  @RepeatedTest(value = 10, name = "Get Access Token Test {currentRepetition}/{totalRepetitions}")
  void testGetAccessToken()
      throws MalformedURLException, JsonProcessingException, InterruptedException {
    String username = CodeGenerator.generateCode(8);
    String password = "Test123###...";
    createSuperuser(username, password, orgUnitUID);

    ChromeOptions chromeOptions = new ChromeOptions();
    chromeOptions.addArguments("--remote-allow-origins=*");
    chromeOptions.addArguments("--no-sandbox");
    chromeOptions.addArguments("--disable-dev-shm-usage");
    chromeOptions.addArguments("--disable-gpu");
    chromeOptions.setPageLoadTimeout(Duration.ofSeconds(60));

    driver = new RemoteWebDriver(new URL(seleniumUrl), chromeOptions);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));

    // Call the authorize endpoint
    driver.get(
        serverHostUrl
            + "/oauth2/authorize?response_type=code&client_id=dhis2-client&redirect_uri=http://localhost:9090/oauth2/code/dhis2-client&scope=openid%20email");
    wait.until(ExpectedConditions.urlContains(serverHostUrl + "/dhis-web-login/"));
    String currentUrl = driver.getCurrentUrl();
    assertEquals(serverHostUrl + "/dhis-web-login/", currentUrl);

    // Wait for the login page to load
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#username")));

    // Login
    LoginPage mainPage = new LoginPage(driver);
    mainPage.inputUsername.sendKeys(username);
    mainPage.inputPassword.sendKeys(password);
    mainPage.inputSubmit.click();

    // Wait for the consent page to load
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".form-check")));
    String consentPageUrl = driver.getCurrentUrl();
    assertEquals(
        serverHostUrl
            + "/oauth2/authorize?response_type=code&client_id=dhis2-client&redirect_uri=http://localhost:9090/oauth2/code/dhis2-client&scope=openid%20email",
        consentPageUrl);

    // Give consent
    ConsentPage consentPage = new ConsentPage(driver);
    consentPage.emailCheckbox.click();
    consentPage.submitButton.click();

    // Wait for the redirect to happen
    wait.until(ExpectedConditions.urlContains("http://localhost:9090/oauth2/code/dhis2-client"));
    int codeStartIndex = driver.getCurrentUrl().indexOf("code=") + 5;

    // Workaround for weird behavior in some cases, where the getCurrentUrl() returns an empty
    // string.
    if (codeStartIndex < 6) {
      log.error("Failed to fetch current url, sleep and retry: " + driver.getCurrentUrl());
      Thread.sleep(1000);
      int tries = 0;
      while (codeStartIndex < 6 && tries < 5) {
        codeStartIndex = driver.getCurrentUrl().indexOf("code=") + 5;
        tries++;
        Thread.sleep(333);
      }
    }
    assertTrue(codeStartIndex > 5, "codeStartIndex is not valid codeStartIndex: " + codeStartIndex);

    String code = driver.getCurrentUrl().substring(codeStartIndex);
    assertNotNull(code);
    assertNotNull(code, "code is null");
    assertFalse(code.isBlank(), "code is empty");
    log.info("Authorization code: " + code);

    assertTrue(
        code.length() == 128, "code has wrong size: '" + code + "', code length: " + code.length());

    // Call the token endpoint with the authorization code and get access token
    String accessToken = getAccessToken(code);

    String actualIssuerUri = null;
    String expectedIssuerUri = "http://web:8080";

    // 6. Decode the access_token
    try {
      SignedJWT signedJWT = SignedJWT.parse(accessToken);
      JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
      // 3. Extract the actual issuer URI from the token's "iss" claim
      actualIssuerUri = claimsSet.getIssuer();
    } catch (ParseException e) {
      log.info("Parsing failed", e);
      throw new RuntimeException(e);
    }
    // 5. Assert that the actual issuer URI matches the configured SERVER_BASE_URL
    assertEquals(
        expectedIssuerUri,
        actualIssuerUri,
        "The JWT issuer URI should match the configured SERVER_BASE_URL.");

    assertNotNull(accessToken);
    log.info("Access token: " + accessToken);

    // Call the /me endpoint with the access token
    ResponseEntity<String> withBearerJwt = getWithBearerJwt(serverApiUrl + "/me", accessToken);
    HttpStatusCode statusCode = withBearerJwt.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
    String body = withBearerJwt.getBody();
    assertNotNull(body);

    log.info("Body: " + body);
    assertTrue(
        body.contains(
            "Found no matching DHIS2 user for the mapping claim: 'email' with the value:"));

    driver.quit();
  }

  public String getAccessToken(String code) throws JsonProcessingException {
    String body = callTokenEndpoint(code);
    assertNotNull(body);
    JsonNode jsonNode = objectMapper.readTree(body);
    JsonNode accessToken = jsonNode.get("access_token");
    assertNotNull(accessToken);
    return accessToken.asText();
  }
}
