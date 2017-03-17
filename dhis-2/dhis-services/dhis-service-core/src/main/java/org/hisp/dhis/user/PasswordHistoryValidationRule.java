package org.hisp.dhis.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

/**
 * Created by zubair on 08.03.17.
 */
public class PasswordHistoryValidationRule implements PasswordValidationRule
{
    private static final int HISTORY_LIMIT = 24;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public PasswordValidationResult validate( Map<String, String> parameters )
    {
        boolean match;

        UserCredentials userCredentials = userService.getUserCredentialsByUsername( parameters.get( "username" ) );

        List<String> previousPasswords = userCredentials.getPreviousPasswords();

        for ( String encodedPassword : previousPasswords )
        {
            match = passwordEncoder.matches( parameters.get( "password" ), encodedPassword );

            if ( match )
            {
                return new PasswordValidationResult( String.format(
                        "Password must not be one of the previous %d passwords", HISTORY_LIMIT ), "password_history_validation", false );
            }
        }

        if ( previousPasswords.size() == HISTORY_LIMIT )
        {
            userCredentials.getPreviousPasswords().remove( 0 );

            userService.updateUserCredentials( userCredentials );
        }

        return new PasswordValidationResult( true );
    }

    @Override
    public boolean isRuleApplicable( Map<String, String> parameters, boolean newUser )
    {
        return ( newUser || !currentUserService.getCurrentUsername().equals( parameters.get( "username" ) ) ) ? false : true;
    }
}
