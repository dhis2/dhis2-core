package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by zubair on 08.03.17.
 */
@Component( "org.hisp.dhis.user.PasswordHistoryValidationRule" )
public class PasswordHistoryValidationRule implements PasswordValidationRule
{
    private static final int HISTORY_LIMIT = 24;

    public static final String ERROR = "Password must not be one of the previous %d passwords";
    public static final String I18_ERROR = "password_history_validation";

    private final PasswordEncoder passwordEncoder;

    private final UserService userService;

    private final CurrentUserService currentUserService;

    @Autowired
    public PasswordHistoryValidationRule( PasswordEncoder passwordEncoder, UserService userService, CurrentUserService currentUserService )
    {
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @Override
    public PasswordValidationResult validate( CredentialsInfo credentialsInfo )
    {
        if ( StringUtils.isBlank( credentialsInfo.getPassword() ) )
        {
            return new PasswordValidationResult( MANDATORY_PARAMETER_MISSING, I18_MANDATORY_PARAMETER_MISSING, false );
        }

        UserCredentials userCredentials = userService.getUserCredentialsByUsername( credentialsInfo.getUsername() );

        List<String> previousPasswords = userCredentials.getPreviousPasswords();

        for ( String encodedPassword : previousPasswords )
        {
            final boolean match = passwordEncoder.matches( credentialsInfo.getPassword(), encodedPassword );

            if ( match )
            {
                return new PasswordValidationResult( String.format( ERROR , HISTORY_LIMIT ), I18_ERROR, false );
            }
        }

        // remove one item from password history if size exceeds HISTORY_LIMIT
        if ( previousPasswords.size() == HISTORY_LIMIT )
        {
            userCredentials.getPreviousPasswords().remove( 0 );

            userService.updateUserCredentials( userCredentials );
        }

        return new PasswordValidationResult( true );
    }

    @Override
    public boolean isRuleApplicable( CredentialsInfo credentialsInfo )
    {
        UserCredentials userCredentials = userService.getUserCredentialsByUsername( credentialsInfo.getUsername() );

        if ( !userService.credentialsNonExpired( userCredentials ) )
        {
            return true;
        }

        // no need to check password history in case of new user
        return !credentialsInfo.isNewUser() &&
            currentUserService.getCurrentUsername().equals( credentialsInfo.getUsername() );
    }
}
