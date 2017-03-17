package org.hisp.dhis.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import java.util.Map;

import java.util.regex.Pattern;

/**
 * Created by zubair on 06.03.17.
 */
public class DefaultPasswordValidationService
        implements PasswordValidationService
{
    @Autowired
    private List<PasswordValidationRule> rules;

    @Override
    public PasswordValidationResult validate( Map<String, String> parameters, boolean newUser )
    {
        PasswordValidationResult result;

        for ( PasswordValidationRule rule : rules )
        {
            if ( rule.isRuleApplicable( parameters, newUser ) )
            {
                result = rule.validate( parameters );

                if ( !result.isValid() )
                {
                    return result;
                }
            }
        }

        return new PasswordValidationResult( true );
    }
}
