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
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zubair on 08.03.17.
 */
public class PasswordLengthValidationRule implements PasswordValidationRule
{
    public static final String ERROR = "Password must have at least %d, and at most %d characters";
    private static final String I18_ERROR = "password_length_validation";

    private final SystemSettingManager systemSettingManager;

    @Autowired
    public PasswordLengthValidationRule( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    @Override
    public PasswordValidationResult validate( CredentialsInfo credentialsInfo )
    {
        if ( StringUtils.isBlank( credentialsInfo.getPassword() ) )
        {
            return new PasswordValidationResult( MANDATORY_PARAMETER_MISSING, I18_MANDATORY_PARAMETER_MISSING, false );
        }

        int minCharLimit = (Integer) systemSettingManager.getSystemSetting( SettingKey.MIN_PASSWORD_LENGTH );

        int maxCharLimit = (Integer) systemSettingManager.getSystemSetting( SettingKey.MAX_PASSWORD_LENGTH );

        String password = credentialsInfo.getPassword();

        if ( password.trim().length() < minCharLimit || password.trim().length() > maxCharLimit )
        {
            return new PasswordValidationResult( String.format( ERROR, minCharLimit, maxCharLimit ), I18_ERROR, false );
        }

        return new PasswordValidationResult( true );
    }
    
    @Override
    public boolean isRuleApplicable( CredentialsInfo credentialsInfo )
    {
        return true;
    }
}
