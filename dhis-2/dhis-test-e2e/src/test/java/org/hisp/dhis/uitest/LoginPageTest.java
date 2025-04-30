/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.uitest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

@Tag("uitests")
@Disabled("Deprecated with Struts")
class LoginPageTest {
  private WebDriver driver;

  private LoginPage mainPage;

  private static URL getSeleniumUrl() {
    String seleniumUrl = TestConfiguration.get().seleniumUrl();
    try {
      return new URL(seleniumUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  public void setUp() {
    ChromeOptions chromeOptions = new ChromeOptions();
    chromeOptions.addArguments("--remote-allow-origins=*");
    driver = new RemoteWebDriver(getSeleniumUrl(), chromeOptions);
    driver.get("http://web:8080/dhis-web-commons/security/login.action");

    mainPage = new LoginPage(driver);
  }

  @AfterEach
  public void tearDown() {
    driver.quit();
  }

  @Test
  void login() {
    mainPage.inputUsername.sendKeys("admin");
    mainPage.inputPassword.sendKeys("district");
    mainPage.inputSubmit.click();

    String currentUrl = driver.getCurrentUrl();
    assertEquals("http://web:8080/dhis-web-dashboard/", currentUrl);
  }
}
