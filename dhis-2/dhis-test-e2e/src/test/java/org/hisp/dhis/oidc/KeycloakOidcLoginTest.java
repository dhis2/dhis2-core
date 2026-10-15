/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * E2e coverage for the external-IdP OIDC login flow (browser redirect via {@code
 * /oauth2/authorization/keycloak} -> Keycloak login form -> {@code /oauth2/code/keycloak} -> DHIS2
 * session with a {@code DhisOidcUser} principal).
 *
 * <p>Pinned contract: the IdP {@code email} claim maps to {@code User.openId}; the resulting
 * session's authorities come from the DHIS2 user's roles (never from the IdP); unmapped IdP users
 * fail to {@code /login/?oidcFailure=true}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Tag("auth-idp")
@Slf4j
class KeycloakOidcLoginTest extends KeycloakBaseTest {

  private static String seleniumUrl;

  private WebDriver driver;

  @BeforeAll
  static void setup() throws JsonProcessingException {
    setupKeycloakBase();
    seleniumUrl = TestConfiguration.get().seleniumUrl();

    createDhisOidcUser("kcloginmapped", "kcloginmapped@dhis2.org", SUPER_USER_ROLE_UID);
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

  @Test
  void testLoginConfigListsKeycloakProvider() {
    ResponseEntity<String> response = getWithAdminBasicAuth("/loginConfig", null);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(
        response.getBody().contains("/oauth2/authorization/keycloak"),
        "loginConfig should advertise the keycloak provider, body: " + response.getBody());
  }

  @Test
  void testOidcLoginMappedUser() throws MalformedURLException, JsonProcessingException {
    driver = createDriver();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));

    // Trigger the OIDC login redirect
    driver.get(serverHostUrl + "/oauth2/authorization/keycloak");
    wait.until(ExpectedConditions.urlContains("/realms/dhis2/"));
    log.info("[oidcLogin] on Keycloak login page: {}", driver.getCurrentUrl());

    // Authenticate at Keycloak
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#username")));
    driver.findElement(By.cssSelector("input#username")).sendKeys("kcloginmapped");
    driver.findElement(By.cssSelector("input#password")).sendKeys("KcLogin123###");
    driver.findElement(By.cssSelector("#kc-login")).click();

    // Back at DHIS2 with an authenticated session
    wait.until(
        d -> {
          String url = d.getCurrentUrl();
          return url.startsWith(serverHostUrl) && !url.contains("/realms/") ? url : null;
        });
    log.info("[oidcLogin] back at DHIS2: {}", driver.getCurrentUrl());

    Cookie session = driver.manage().getCookieNamed("JSESSIONID");
    assertNotNull(session, "no JSESSIONID cookie after OIDC login");

    ResponseEntity<String> me = getWithCookie("/me", session.getName() + "=" + session.getValue());
    assertEquals(HttpStatus.OK, me.getStatusCode(), "body: " + me.getBody());
    JsonNode json = objectMapper.readTree(me.getBody());
    assertEquals("kcloginmapped", json.get("username").asText());
    // authorities come from the DHIS2 role (superuser), never from the IdP
    assertTrue(json.get("authorities").toString().contains("ALL"), "body: " + me.getBody());
  }

  @Test
  void testOidcLoginUnmappedUserFails() throws MalformedURLException {
    driver = createDriver();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));

    driver.get(serverHostUrl + "/oauth2/authorization/keycloak");
    wait.until(ExpectedConditions.urlContains("/realms/dhis2/"));

    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input#username")));
    driver.findElement(By.cssSelector("input#username")).sendKeys("kcunmapped");
    driver.findElement(By.cssSelector("input#password")).sendKeys("KcUnmapped123###");
    driver.findElement(By.cssSelector("#kc-login")).click();

    // No matching DHIS2 user (User.openId) -> oauth2Login failureUrl
    String failureUrl =
        wait.until(ExpectedConditions.urlContains("oidcFailure")) ? driver.getCurrentUrl() : null;
    assertNotNull(failureUrl);
    assertTrue(
        failureUrl.contains("oidcFailure=true"),
        "expected redirect to /login/?oidcFailure=true, got: " + failureUrl);
  }

  private WebDriver createDriver() throws MalformedURLException {
    ChromeOptions chromeOptions = new ChromeOptions();
    chromeOptions.addArguments("--remote-allow-origins=*");
    chromeOptions.addArguments("--no-sandbox");
    chromeOptions.addArguments("--disable-dev-shm-usage");
    chromeOptions.addArguments("--disable-gpu");
    chromeOptions.setPageLoadTimeout(Duration.ofSeconds(60));
    return new RemoteWebDriver(new URL(seleniumUrl), chromeOptions);
  }
}
