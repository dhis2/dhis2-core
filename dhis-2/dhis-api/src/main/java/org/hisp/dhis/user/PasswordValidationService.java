package org.hisp.dhis.user;

/**
 * Created by zubair on 06.03.17.
 */
public interface PasswordValidationService
{
    PasswordValidationResult validate( String username, String password, boolean newUser );
}
