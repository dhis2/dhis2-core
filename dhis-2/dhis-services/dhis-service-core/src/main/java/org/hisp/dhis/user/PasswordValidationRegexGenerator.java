package org.hisp.dhis.user;

import org.hisp.dhis.setting.SystemSettings;
import org.springframework.stereotype.Component;

/**
 * Utility class to generate password validation regex based on system settings
 */
@Component
public class PasswordValidationRegexGenerator {

    /**
     * Generates a regex pattern that matches the current password validation rules
     * based on system settings.
     *
     * @param settings The current system settings
     * @return A regex pattern string that enforces the password rules
     */
    public String generatePasswordValidationPattern(SystemSettings settings) {
        int minLength = settings.getMinPasswordLength();
        int maxLength = settings.getMaxPasswordLength();
        
        // Build the regex pattern with the following rules:
        // 1. At least one uppercase letter
        // 2. At least one digit
        // 3. At least one special character
        // 4. Length between min and max from settings
        return String.format(
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_])[A-Za-z\\d\\W_]{%d,%d}$",
            minLength,
            maxLength
        );
    }
} 