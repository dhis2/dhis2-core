package org.hisp.dhis.setting;

import org.hisp.dhis.user.CurrentUserUtil;

import java.util.Map;

/**
 * Manages the {@link ThreadLocal} state for {@link UserSettings} that cannot be included in a
 * non-public way in the interface.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
final class CurrentUserSettings {

  static final ThreadLocal<UserSettings> CURRENT_USER_SETTINGS = new ThreadLocal<>();

  static UserSettings getCurrentSettings() {
    UserSettings settings = CURRENT_USER_SETTINGS.get();
    if (settings == null) {
      settings = CurrentUserUtil.getCurrentUserDetails().getUserSettings();
      CURRENT_USER_SETTINGS.set(settings);
    }
    return settings;
  }

  static void clearCurrentSettings() {
    CURRENT_USER_SETTINGS.remove();
  }

  static void overrideCurrentSettings(Map<String, String> settings) {
    CURRENT_USER_SETTINGS.set(UserSettings.of(getCurrentSettings(), settings));
  }
}
