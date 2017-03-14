package org.hisp.dhis.user;

import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zubair on 08.03.17.
 */
public class PasswordLengthValidationRule implements PasswordValidationRule
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    @Override
    public PasswordValidationResult validate( String username, String password )
    {
        int minCharLimit = (Integer) systemSettingManager.getSystemSetting( SettingKey.MIN_PASSWORD_LENGTH );

        int maxCharLimit = (Integer) systemSettingManager.getSystemSetting( SettingKey.MAX_PASSWORD_LENGTH );

        if ( password == null || password.trim().length() < minCharLimit || password.trim().length() > maxCharLimit )
        {
            return new PasswordValidationResult( String.format(
                    "Password must have at least %d, and at most %d characters", minCharLimit, maxCharLimit ), "password_length_validation", false );
        }

        return new PasswordValidationResult( true );
    }
}
