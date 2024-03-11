package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

/**
 * Created by zubair on 16.03.17.
 */
public class UserParameterValidationRule
    implements PasswordValidationRule
{
    public static final String ERROR = "Username/Email must not be a part of password";
    private static final String I18_ERROR = "password_username_validation";

    @Override
    public boolean isRuleApplicable( CredentialsInfo credentialsInfo )
    {
        return true;
    }

    @Override
    public PasswordValidationResult validate( CredentialsInfo credentialsInfo )
    {
        String email = credentialsInfo.getEmail();
        String password = credentialsInfo.getPassword();
        String username = credentialsInfo.getUsername();

        // other parameters will be skipped in case of new user
        if ( credentialsInfo.isNewUser() )
        {
            if ( StringUtils.isBlank( password ) )
            {
                return new PasswordValidationResult( MANDATORY_PARAMETER_MISSING, I18_MANDATORY_PARAMETER_MISSING, false );
            }
        }
        else if ( StringUtils.isBlank( password ) || StringUtils.isBlank( username ) )
        {
            return new PasswordValidationResult( MANDATORY_PARAMETER_MISSING, I18_MANDATORY_PARAMETER_MISSING, false );
        }

        // Password should not contain part of either username or email
        if ( StringUtils.containsIgnoreCase( password, StringUtils.defaultIfEmpty( username, null ) ) ||
                StringUtils.containsIgnoreCase( password, StringUtils.defaultIfEmpty( email, null ) ) )
        {
            return new PasswordValidationResult( ERROR, I18_ERROR, false );
        }

        return new PasswordValidationResult( true );
    }
}
