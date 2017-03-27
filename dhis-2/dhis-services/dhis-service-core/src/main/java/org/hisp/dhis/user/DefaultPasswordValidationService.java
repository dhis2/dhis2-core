package org.hisp.dhis.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zubair.
 */
public class DefaultPasswordValidationService
        implements PasswordValidationService
{
    @Autowired
    private List<PasswordValidationRule> rules;

    @Override
    public PasswordValidationResult validate( CredentialsInfo credentialsInfo )
    {
        PasswordValidationResult result;

        for ( PasswordValidationRule rule : rules )
        {
            if ( rule.isRuleApplicable( credentialsInfo ) )
            {
                result = rule.validate( credentialsInfo );

                if ( !result.isValid() )
                {
                    return result;
                }
            }
        }

        return new PasswordValidationResult( true );
    }
}
