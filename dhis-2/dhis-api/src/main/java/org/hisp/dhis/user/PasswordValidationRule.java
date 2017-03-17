package org.hisp.dhis.user;

import java.util.Map;

/**
 * Created by zubair on 08.03.17.
 */
public interface PasswordValidationRule
{
    PasswordValidationResult validate( Map<String, String> parameters );

    boolean isRuleApplicable( Map<String, String> parameters, boolean newUser );
}
