package org.hisp.dhis.user;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Created by zubair on 16.03.17.
 */
public class UserParameterValidationRule
    implements PasswordValidationRule
{
    @Override
    public boolean isRuleApplicable( Map<String, String> parameters, boolean newUser )
    {
        return true;
    }

    @Override
    public PasswordValidationResult validate( Map<String, String> parameters )
    {
        String email = parameters.get( "email" );
        String password = parameters.get( "password" );
        String username = parameters.get( "username" );

        if ( StringUtils.containsIgnoreCase( password, username ) || StringUtils.containsIgnoreCase( password, email ) )
        {
            return new PasswordValidationResult( "Username/Email must not be a part of password", "password_username_validation", false );
        }

        return new PasswordValidationResult( true );
    }
}
