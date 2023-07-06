/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.setting;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
public interface SystemSettingManager {
  /**
   * Saves the given system setting key and value.
   *
   * @param key the system setting key.
   * @param value the system setting value.
   */
  void saveSystemSetting(SettingKey key, Serializable value);

  /**
   * Saves the translation for given setting key and locale if given setting key is translatable. If
   * the translation string contains an empty string, the translation for given locale and key is
   * removed.
   *
   * @param key SettingKey
   * @param locale locale of the translation
   * @param translation Actual translation
   */
  void saveSystemSettingTranslation(SettingKey key, String locale, String translation);

  /**
   * Deletes the system setting with the given key.
   *
   * @param key the system setting key.
   */
  void deleteSystemSetting(SettingKey key);

  /**
   * Returns the system setting value for the given key. If no value exists, returns the default
   * value held by {@link SettingKey#getDefaultValue()}. If not, returns null.
   *
   * @param key the system setting key.
   * @return the setting value.
   */
  @Transactional(readOnly = true)
  default <T extends Serializable> T getSystemSetting(SettingKey key, Class<T> type) {
    if (type != key.getClazz()) {
      throw new IllegalArgumentException(
          String.format(
              "Key %s is a %s but was requested as %s",
              key.getName(), key.getClazz().getSimpleName(), type.getName()));
    }
    return type.cast(getSystemSetting(key, key.getDefaultValue()));
  }

  /**
   * Returns the system setting value for the given key. If no value exists, returns the default
   * value as defined by the given default value.
   *
   * @param key the system setting key.
   * @return the setting value.
   */
  <T extends Serializable> T getSystemSetting(SettingKey key, T defaultValue);

  /**
   * Returns the translation for given setting key and locale or empty Optional if no translation is
   * available or setting key is not translatable.
   *
   * @param key SettingKey
   * @param locale Locale of required translation
   * @return The Optional with the actual translation or empty Optional
   */
  Optional<String> getSystemSettingTranslation(SettingKey key, String locale);

  /**
   * Returns all system settings.
   *
   * @return a list of all system settings.
   */
  List<SystemSetting> getAllSystemSettings();

  /**
   * Returns all system settings as a mapping between the setting name and the value. Includes
   * system settings which have a default value but no explicitly set value.
   */
  Map<String, Serializable> getSystemSettingsAsMap();

  /**
   * Returns system settings for the given collection of setting keys as a map, where the key is
   * string representation of the {@link SettingKey}, and the value is the setting value.
   *
   * @param keys the collection of setting keys.
   * @return a map of system settings.
   */
  Map<String, Serializable> getSystemSettings(Collection<SettingKey> keys);

  /** Invalidates the currently cached system settings. */
  void invalidateCache();

  List<String> getFlags();

  boolean isConfidential(String name);

  boolean isTranslatable(String name);

  // -------------------------------------------------------------------------
  // Typed methods
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  default String getStringSetting(SettingKey key) {
    return getSystemSetting(key, String.class);
  }

  @Transactional(readOnly = true)
  default Integer getIntegerSetting(SettingKey key) {
    return getSystemSetting(key, Integer.class);
  }

  @Transactional(readOnly = true)
  default int getIntSetting(SettingKey key) {
    return getSystemSetting(key, Integer.class);
  }

  @Transactional(readOnly = true)
  default Boolean getBooleanSetting(SettingKey key) {
    return getSystemSetting(key, Boolean.class);
  }

  @Transactional(readOnly = true)
  default boolean getBoolSetting(SettingKey key) {
    return Boolean.TRUE.equals(getSystemSetting(key, Boolean.class));
  }

  @Transactional(readOnly = true)
  default Date getDateSetting(SettingKey key) {
    return getSystemSetting(key, Date.class);
  }

  // -------------------------------------------------------------------------
  // Specific methods
  // -------------------------------------------------------------------------

  @Transactional(readOnly = true)
  default String getFlagImage() {
    String flag = getStringSetting(SettingKey.FLAG);
    return flag != null ? flag + ".png" : null;
  }

  @Transactional(readOnly = true)
  default String getEmailHostName() {
    return StringUtils.trimToNull(getStringSetting(SettingKey.EMAIL_HOST_NAME));
  }

  @Transactional(readOnly = true)
  default int getEmailPort() {
    return getIntSetting(SettingKey.EMAIL_PORT);
  }

  @Transactional(readOnly = true)
  default String getEmailUsername() {
    return StringUtils.trimToNull(getStringSetting(SettingKey.EMAIL_USERNAME));
  }

  @Transactional(readOnly = true)
  default boolean getEmailTls() {
    return getBoolSetting(SettingKey.EMAIL_TLS);
  }

  @Transactional(readOnly = true)
  default String getEmailSender() {
    return StringUtils.trimToNull(getStringSetting(SettingKey.EMAIL_SENDER));
  }

  @Transactional(readOnly = true)
  default boolean accountRecoveryEnabled() {
    return getBoolSetting(SettingKey.ACCOUNT_RECOVERY);
  }

  @Transactional(readOnly = true)
  default boolean selfRegistrationNoRecaptcha() {
    return getBoolSetting(SettingKey.SELF_REGISTRATION_NO_RECAPTCHA);
  }

  @Transactional(readOnly = true)
  default boolean emailConfigured() {
    return StringUtils.isNotBlank(getEmailHostName()) && StringUtils.isNotBlank(getEmailUsername());
  }

  @Transactional(readOnly = true)
  default boolean systemNotificationEmailValid() {
    String address = getStringSetting(SettingKey.SYSTEM_NOTIFICATIONS_EMAIL);

    return address != null && ValidationUtils.emailIsValid(address);
  }

  @Transactional(readOnly = true)
  default boolean hideUnapprovedDataInAnalytics() {
    // -1 means approval is disabled
    return getIntSetting(SettingKey.IGNORE_ANALYTICS_APPROVAL_YEAR_THRESHOLD) >= 0;
  }

  @Transactional(readOnly = true)
  default String googleAnalyticsUA() {
    return StringUtils.trimToNull(getStringSetting(SettingKey.GOOGLE_ANALYTICS_UA));
  }

  @Transactional(readOnly = true)
  default Integer credentialsExpires() {
    return getIntegerSetting(SettingKey.CREDENTIALS_EXPIRES);
  }
}
