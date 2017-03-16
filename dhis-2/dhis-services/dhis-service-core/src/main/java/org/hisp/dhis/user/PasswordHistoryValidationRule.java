package org.hisp.dhis.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

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

    @Override
    public PasswordValidationResult validate( String username, String password )
    {
        boolean match;

        UserCredentials userCredentials = userService.getUserCredentialsByUsername( username );

        List<String> previousPasswords = userCredentials.getPreviousPasswords();

        for ( String encodedPassword : previousPasswords )
        {
            match = passwordEncoder.matches( password, encodedPassword );

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
}
