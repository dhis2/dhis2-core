package org.hisp.dhis.user;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by zubair on 16.03.17.
 */
public class SpecialCharacterValidationRule
        implements PasswordValidationRule
{
    private static final Pattern SPECIAL_CHARACTER = Pattern.compile( ".*[^A-Za-z0-9].*" );

    @Override
    public PasswordValidationResult validate( Map<String, String> parameters )
    {
        if ( !SPECIAL_CHARACTER.matcher( parameters.get( "password" ) ).matches() )
        {
            return new PasswordValidationResult( "Password must have at least one special character", "password_specialcharacter_validation",false );
        }

        return new PasswordValidationResult( true );
    }

    @Override
    public boolean isRuleApplicable( Map<String, String> parameters, boolean newUser )
    {
        return true;
    }
}
