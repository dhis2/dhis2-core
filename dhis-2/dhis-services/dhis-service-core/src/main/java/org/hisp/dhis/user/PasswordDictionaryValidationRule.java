package org.hisp.dhis.user;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by zubair on 16.03.17.
 */
public class PasswordDictionaryValidationRule
        implements PasswordValidationRule
{
    List<String> dictionary = Arrays.asList( "user", "admin", "system", "administrator", "username", "password", "login", "manager");

    @Override
    public boolean isRuleApplicable( CredentialsInfo credentialsInfo )
    {
        return true;
    }

    @Override
    public PasswordValidationResult validate( CredentialsInfo credentialsInfo )
    {
        for ( String reserved : dictionary )
        {
            if ( StringUtils.containsIgnoreCase( credentialsInfo.getPassword(), reserved ) )
            {
                return new PasswordValidationResult( "Password must not have any generic word", "password_dictionary_validation", false );
            }
        }

        return new PasswordValidationResult( true );
    }
}
