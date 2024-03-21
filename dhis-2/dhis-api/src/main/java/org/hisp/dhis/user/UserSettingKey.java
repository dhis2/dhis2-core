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
package org.hisp.dhis.user;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.common.DisplayProperty;

/**
 * @author Lars Helge Overland
 */
public enum UserSettingKey {
  STYLE("keyStyle"),
  MESSAGE_EMAIL_NOTIFICATION("keyMessageEmailNotification", true, Boolean.class),
  MESSAGE_SMS_NOTIFICATION("keyMessageSmsNotification", true, Boolean.class),
  UI_LOCALE("keyUiLocale", Locale.class),
  DB_LOCALE("keyDbLocale", Locale.class),
  ANALYSIS_DISPLAY_PROPERTY("keyAnalysisDisplayProperty", DisplayProperty.class),
  TRACKER_DASHBOARD_LAYOUT("keyTrackerDashboardLayout");

  private final String name;

  private final Serializable defaultValue;

  private final Class<?> clazz;

  private static Map<String, Serializable> DEFAULT_USER_SETTINGS_MAP =
      Stream.of(UserSettingKey.values())
          .filter(k -> k.getDefaultValue() != null)
          .collect(Collectors.toMap(UserSettingKey::getName, UserSettingKey::getDefaultValue));

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  UserSettingKey(String name) {
    this.name = name;
    this.defaultValue = null;
    this.clazz = String.class;
  }

  UserSettingKey(String name, Class<?> clazz) {
    this.name = name;
    this.defaultValue = null;
    this.clazz = clazz;
  }

  UserSettingKey(String name, Serializable defaultValue, Class<?> clazz) {
    this.name = name;
    this.defaultValue = defaultValue;
    this.clazz = clazz;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public Serializable getDefaultValue() {
    return defaultValue;
  }

  public boolean hasDefaultValue() {
    return defaultValue != null;
  }

  public static Optional<UserSettingKey> getByName(String name) {
    for (UserSettingKey setting : UserSettingKey.values()) {
      if (setting.getName().equals(name)) {
        return Optional.of(setting);
      }
    }

    return Optional.empty();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Serializable getAsRealClass(UserSettingKey key, String value) {

    Class<?> settingClazz = key.getClazz();

    if (Double.class.isAssignableFrom(settingClazz)) {
      return Double.valueOf(value);
    } else if (Integer.class.isAssignableFrom(settingClazz)) {
      return Integer.valueOf(value);
    } else if (Boolean.class.isAssignableFrom(settingClazz)) {
      return Boolean.valueOf(value);
    } else if (Locale.class.isAssignableFrom(settingClazz)) {
      return handleLocale(key, LocaleUtils.toLocale(value));
    } else if (Enum.class.isAssignableFrom(settingClazz)) {
      return Enum.valueOf((Class<? extends Enum>) settingClazz, value.toUpperCase());
    }

    // TODO handle Dates

    return value;
  }

  /**
   * Method to determine whether a locale should be transformed or not. We only want to transform UI
   * locales. We do not want to transform DB locales.
   *
   * @param key key to check whether the transformation should apply or not
   * @param locale locale to transform or return
   * @return the received locale if it's the DC_LOCALE or the possibly-transformed UI locale
   */
  private static Serializable handleLocale(UserSettingKey key, Locale locale) {
    if (DB_LOCALE == key) return locale;

    return handleObsoleteLocales(locale);
  }

  /**
   * By default, for backwards compatibility reasons, Java maps Indonesian locales to the old 'in'
   * ISO format, even if we pass 'id' into a {@link Locale} constructor. See {@link
   * Locale#convertOldISOCodes(String)}. This method sets the Indonesian codes to the codes we want
   * to use (which conform with the newer, universally-accepted ISO language formats for Indonesia
   * 'id'). <br>
   * <br>
   * JDK 17 does not have this issue and allows switching between both codes - see <a
   * href="https://bugs.openjdk.org/browse/JDK-8267069">JDK bug</a>. This is needed purely for DHIS2
   * versions running on a JDK below 17.
   *
   * @param locale locale
   * @return adjusted locale for Indonesian codes or as is for anything else
   */
  public static String handleObsoleteLocales(Locale locale) {

    switch (locale.toString()) {
      case "in":
        return "id";
      case "in_ID":
        return "id_ID";
      default:
        return locale.toString();
    }
  }

  // -------------------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------------------

  public String getName() {
    return name;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public static Map<String, Serializable> getDefaultUserSettingsMap() {
    return new HashMap<>(DEFAULT_USER_SETTINGS_MAP);
  }

  public static Set<UserSetting> getDefaultUserSettings(User user) {
    Set<UserSetting> defaultUserSettings = new HashSet<>();
    DEFAULT_USER_SETTINGS_MAP.forEach(
        (key, value) -> {
          UserSetting userSetting = new UserSetting();
          userSetting.setName(key);
          userSetting.setValue(value);
          userSetting.setUser(user);
          defaultUserSettings.add(userSetting);
        });
    return defaultUserSettings;
  }
}
