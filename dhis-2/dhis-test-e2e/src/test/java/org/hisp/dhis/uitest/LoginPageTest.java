package org.hisp.dhis.uitest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import org.hisp.dhis.helpers.config.TestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

public class LoginPageTest {
  private WebDriver driver;

  private LoginPage mainPage;

  private static URL getUrl() {
    URL remoteAddress = null;
    String seleniumUrl = TestConfiguration.get().seleniumUrl();
    try {
      remoteAddress = new URL(seleniumUrl);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

    return remoteAddress;
  }

  @BeforeEach
  public void setUp() {
    ChromeOptions chromeOptions = new ChromeOptions();
    chromeOptions.addArguments("--remote-allow-origins=*");
    driver = new RemoteWebDriver(getUrl(), chromeOptions);
    driver.get("http://web:8080");

    mainPage = new LoginPage(driver);
  }

  @AfterEach
  public void tearDown() {
    driver.quit();
  }

  @Test
  public void login() {
    mainPage.inputUsername.sendKeys("admin");
    mainPage.inputPassword.sendKeys("district");
    mainPage.inputSubmit.click();

    String currentUrl = driver.getCurrentUrl();
    assertEquals("http://web:8080/dhis-web-dashboard/", currentUrl);
  }
}
