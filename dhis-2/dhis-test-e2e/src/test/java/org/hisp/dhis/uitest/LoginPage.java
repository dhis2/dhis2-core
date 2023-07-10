package org.hisp.dhis.uitest;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class LoginPage
{
    @FindBy( css = "input[id$='username']" )
    public WebElement inputUsername;

    @FindBy( css = "input[class='button']" )
    public WebElement inputSubmit;

    @FindBy( css = "input[id$='password']" )
    public WebElement inputPassword;

    public LoginPage( WebDriver driver )
    {
        PageFactory.initElements( driver, this );
    }
}
