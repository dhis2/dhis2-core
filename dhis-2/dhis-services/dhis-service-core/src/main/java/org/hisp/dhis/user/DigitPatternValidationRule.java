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
    public PasswordValidationResult validate( Map<String, String> parameters )
    {
        if ( !DIGIT_PATTERN.matcher( parameters.get( "password" ) ).matches() )
        {
            return new PasswordValidationResult( "Password must have at least one digit", "password_digit_validation", false );
        }

        return new PasswordValidationResult( true );
    }

    @Override
    public boolean isRuleApplicable( Map<String, String> parameters, boolean newUser )
    {
        return true;
    }
}
