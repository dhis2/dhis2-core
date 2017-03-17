package org.hisp.dhis.user;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zubair on 08.03.17.
 */
public class UpperCasePatternValidationRule implements PasswordValidationRule
{
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile( ".*[A-Z].*" );

    @Override
    public PasswordValidationResult validate( Map<String, String> parameters )
    {
        if ( !UPPERCASE_PATTERN.matcher( parameters.get( "password" ) ).matches() )
        {
            return new PasswordValidationResult( "Password must have at least one upper case", "password_uppercase_validation",false );
        }

        return new PasswordValidationResult( true );
    }

    @Override
    public boolean isRuleApplicable( Map<String, String> parameters, boolean newUser )
    {
        return true;
    }
}
