package org.hisp.dhis.user;

/**
 * Created by zubair on 06.03.17.
 */
public class PasswordValidationResult
{
    private String errorMessage;

    private String i18ErrorMessage;

    private boolean valid;

    public PasswordValidationResult( String errorMessage, String i18ErrorMessage, boolean valid )
    {
        this.errorMessage = errorMessage;
        this.i18ErrorMessage = i18ErrorMessage;
        this.valid = valid;
    }

    public PasswordValidationResult()
    {
    }

    public PasswordValidationResult( boolean valid )
    {
        this.valid = valid;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }

    public boolean isValid()
    {
        return valid;
    }

    public void setValid( boolean valid )
    {
        this.valid = valid;
    }

    public String getI18ErrorMessage()
    {
        return i18ErrorMessage;
    }

    public void setI18ErrorMessage( String i18ErrorMessage )
    {
        this.i18ErrorMessage = i18ErrorMessage;
    }
}
