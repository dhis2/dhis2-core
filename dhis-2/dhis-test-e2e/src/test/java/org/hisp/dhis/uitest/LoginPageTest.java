package org.hisp.dhis.uitest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoginPageTest
{
    private WebDriver driver;

    private LoginPage mainPage;

    @BeforeEach
    public void setUp()
    {
        ChromeOptions chromeOptions = new ChromeOptions();
        //        chromeOptions.setCapability( "browserVersion", "100" );
        //        chromeOptions.setCapability( "platformName", "Windows" );
        // Showing a test name instead of the session id in the Grid UI
        chromeOptions.addArguments( "--remote-allow-origins=*" );
        //        chromeOptions.setCapability( "se:name", "My simple test" );
        // Other type of metadata can be seen in the Grid UI by clicking on the
        // session info or via GraphQL
        //        chromeOptions.setCapability( "se:sampleMetadata", "Sample metadata value" );
        driver = new RemoteWebDriver( getUrl(), chromeOptions );
        driver.get( "http://host.docker.internal:8080" );

        //        ChromeOptions options = new ChromeOptions();
        //        // Fix the issue https://github.com/SeleniumHQ/selenium/issues/11750
        //        options.addArguments( "--remote-allow-origins=*" );
        //        driver = new ChromeDriver( options );
        //        driver.manage().window().maximize();
        //        driver.manage().timeouts().implicitlyWait( Duration.ofSeconds( 10 ) );
        //        driver.get( "http://localhost:8080" );

        mainPage = new LoginPage( driver );
    }

    private static URL getUrl()
    {
        URL remoteAddress = null;
        try
        {
            remoteAddress = new URL( "http://localhost:4444" );
        }
        catch ( MalformedURLException e )
        {
            throw new RuntimeException( e );
        }
        return remoteAddress;
    }

    @AfterEach
    public void tearDown()
    {
        driver.quit();
    }

    @Test
    public void login()
    {
        mainPage.inputUsername.sendKeys( "admin" );
        mainPage.inputPassword.sendKeys( "district" );
        mainPage.inputSubmit.click();

        String currentUrl = driver.getCurrentUrl();
        assertEquals( "http://host.docker.internal:8080/dhis-web-dashboard/", currentUrl );
    }

}
