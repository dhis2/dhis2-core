package org.hisp.dhis.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by zubair on 06.03.17.
 */
public class DefaultPasswordValidationService
        implements PasswordValidationService
{
    @Autowired
    private List<PasswordValidationRule> rules;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public PasswordValidationResult validate( String username, String password, boolean newUser )
    {
        PasswordValidationResult result;

        for( PasswordValidationRule rule : rules )
        {
            if ( !isRuleApplicable( username, newUser ) )
            {
                continue;
            }

            result = rule.validate( username, password );

            if ( !result.isValid() )
            {
                return result;
            }
        }

        return new PasswordValidationResult( true );
    }

    private boolean isRuleApplicable( String username, boolean newUser)
    {
        // check for new user or if super user is changing other user's password
        return ( newUser || !currentUserService.getCurrentUsername().equals( username ) ) ? false : true;
    }
}
