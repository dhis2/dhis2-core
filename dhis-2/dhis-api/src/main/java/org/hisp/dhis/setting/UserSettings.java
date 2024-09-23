package org.hisp.dhis.setting;

import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.i18n.locale.LocaleManager;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@linkplain UserSettings} are bound to the user session.
 *
 * <p>Once initialized on login they do remain unchanged for the duration of the session. This
 * includes potential fallbacks from {@link SystemSettings}. Like {@link SystemSettings} that only
 * hold data of settings defined in the DB a {@linkplain UserSettings} instance only holds
 * key-value-pairs for settings either defined as user setting in the DB or as system setting
 * fallback. Keys that are not defined in DB use the default provided through their access method.
 *
 * <p>The only exception are individual request that make an override using {@link
 * #overrideCurrentUserSettings(Map)}.
 *
 * <p>For that reason user has to log out and re-login to make changes to system or user settings
 * visible in the session. This choice is made to balance complexity, predictability and resource
 * costs while providing a clear flow for users.
 *
 * @author Jan Bernitt
 * @since 2.42
 */
public non-sealed interface UserSettings extends Settings {

  /**
   * An immutable per thread instance for the current user's settings.
   * <p>
   * Any reading usage of user settings should be made starting from the instance returned by this
   * method. Do not access the {@link UserSettings} from {@link org.hisp.dhis.user.UserDetails}
   * directly or any service that might provide them from DB.
   *
   * <p>The settings are initialized from the current {@link org.hisp.dhis.user.UserDetails} but can
   * be overridden per request using request parameters. In such a case the object returned will
   * reflect the changes (which are applied using {@link #overrideCurrentUserSettings(Map)}) for the
   * scope of the request only. The settings in {@link org.hisp.dhis.user.UserDetails} still reflect
   * the state that was last stored for the user.
   *
   * @return an immutable instance of the current user's settings. It explicitly defines settings
   *     stored for the user as well as settings defined for the system in case they are not defined
   *     for the user.
   */
  static UserSettings getCurrentSettings() {
    return CurrentUserSettings.getCurrentSettings();
  }

  /**
   * Removes the use {@link UserSettings} instance from the current thread.
   * This happens at the end of each request.
   */
  static void clearCurrentUserSettings() {
    CurrentUserSettings.clearCurrentSettings();
  }

  /**
   * Allows to overlay the current user's settings with overrides for the scope of the current request.
   * This does not change the user's setting in the session nor the settings stored in the DB.
   *
   * @param settings the overrides to apply on top of the user's session held settings
   */
  static void overrideCurrentUserSettings(Map<String, String> settings) {
    CurrentUserSettings.overrideCurrentSettings(settings);
  }

  static UserSettings of(Map<String, String> settings) {
    return LazySettings.of(UserSettings.class, settings);
  }

  /**
   * Union.
   *
   * @param settings entries for the union
   * @return a new {@link UserSettings} instance with all entries of this instance and the provided settings map
   */
  default UserSettings withOverlay(Map<String, String> settings) {
      Map<String, String> merged = new HashMap<>(toMap());
      merged.putAll(settings);
      return UserSettings.of(merged);
  }

  /**
   * @param settings fallback entries
   * @return a new {@link UserSettings} instance with values using fallback if they were not defined
   */
  default UserSettings withFallback(Map<String, String> settings) {
    Map<String, String> original = toMap();
    Map<String, String> merged = new HashMap<>(original);
    //FIXME the set of keys iterated here must be all keys of UserSettings
    for (String key : original.keySet()) {
      String value = settings.get(key);
      if (value != null)
        merged.put(key, value);
    }
    return UserSettings.of(merged);
  }

  default String getUserStyle() {
    return asString("keyStyle", "");
  }

  default boolean getUserMessageEmailNotification() {
    return asBoolean("keyMessageEmailNotification", false);
  }

  default boolean getUserMessageSmsNotification() {
    return asBoolean("keyMessageSmsNotification", false);
  }

  default Locale getUserUiLocale() {
    return asLocale("keyUiLocale", LocaleManager.DEFAULT_LOCALE);
  }

  default Locale getUserDbLocale() {
    return asLocale("keyDbLocale", LocaleManager.DEFAULT_LOCALE);
  }

  default Locale getUserLocale() {
    return keys().contains("keyDbLocale") ? getUserDbLocale() : getUserUiLocale();
  }

  default DisplayProperty getUserAnalysisDisplayProperty() {
    return asEnum("keyAnalysisDisplayProperty", DisplayProperty.NAME);
  }

  default String getUserTrackerDashboardLayout() {
    return asString("keyTrackerDashboardLayout", "");
  }

}
