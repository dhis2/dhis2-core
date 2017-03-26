package org.hisp.dhis.user;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zubair on 08.03.17.
 */
public class DigitPatternValidationRule implements PasswordValidationRule
{
    private static final Pattern DIGIT_PATTERN = Pattern.compile( ".*\\d.*" );

    @Override
    public PasswordValidationResult validate( CredentialsInfo credentialsInfo )
    {
        if ( !DIGIT_PATTERN.matcher( credentialsInfo.getPassword() ).matches() )
        {
            return new PasswordValidationResult( "Password must have at least one digit", "password_digit_validation", false );
        }

        return new PasswordValidationResult( true );
    }

    @Override
    public boolean isRuleApplicable( CredentialsInfo credentialsInfo )
    {
        return true;
    }
}
