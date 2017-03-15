package org.hisp.dhis.user;

/**
 * Created by zubair on 08.03.17.
 */
public interface PasswordValidationRule
{
    PasswordValidationResult validate( String username, String password );
}
