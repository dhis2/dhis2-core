package org.hisp.dhis.user;

import java.util.Map;

/**
 * Created by zubair on 06.03.17.
 */
public interface PasswordValidationService
{
    PasswordValidationResult validate( Map<String, String> parameters, boolean newUser );
}
