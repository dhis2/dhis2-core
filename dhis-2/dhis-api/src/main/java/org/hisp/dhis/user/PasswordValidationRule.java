package org.hisp.dhis.user;

import java.util.Map;

/**
 * Created by zubair on 08.03.17.
 */
public interface PasswordValidationRule
{
    PasswordValidationResult validate( CredentialsInfo credentialsInfo );

    boolean isRuleApplicable( CredentialsInfo credentialsInfo );
}
