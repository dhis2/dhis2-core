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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.common.DisplayProperty;

/**
 * @author Lars Helge Overland
 */
@Getter
public enum UserSettingKey {
  STYLE("keyStyle", UserSettings::getStyle),
  MESSAGE_EMAIL_NOTIFICATION(
      "keyMessageEmailNotification",
      UserSettings::getMessageEmailNotification,
      true,
      Boolean.class),
  MESSAGE_SMS_NOTIFICATION(
      "keyMessageSmsNotification", UserSettings::getMessageSmsNotification, true, Boolean.class),
  UI_LOCALE("keyUiLocale", UserSettings::getUiLocale, Locale.class),
  DB_LOCALE("keyDbLocale", UserSettings::getDbLocale, Locale.class),
  ANALYSIS_DISPLAY_PROPERTY(
      "keyAnalysisDisplayProperty",
      UserSettings::getAnalysisDisplayProperty,
      DisplayProperty.class),
  TRACKER_DASHBOARD_LAYOUT("keyTrackerDashboardLayout", UserSettings::getTrackerDashboardLayout);

  private final String name;

  private Function<UserSettings, ? extends Serializable> getter;

  private final Serializable defaultValue;

  private final Class<?> clazz;

  private static Map<String, Serializable> DEFAULT_USER_SETTINGS_MAP =
      Stream.of(UserSettingKey.values())
          .filter(k -> k.getDefaultValue() != null)
          .collect(Collectors.toMap(UserSettingKey::getName, UserSettingKey::getDefaultValue));

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  UserSettingKey(String name, Function<UserSettings, String> getter) {
    this(name, getter, null, String.class);
  }

  <T extends Serializable> UserSettingKey(
      String name, Function<UserSettings, T> getter, Class<T> clazz) {
    this(name, getter, null, clazz);
  }

  <T extends Serializable> UserSettingKey(
      String name, Function<UserSettings, T> getter, T defaultValue, Class<T> clazz) {
    this.name = name;
    this.getter = getter;
    this.defaultValue = defaultValue;
    this.clazz = clazz;
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

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
  public static Serializable getAsRealClass(String name, String value) {
    Optional<UserSettingKey> setting = getByName(name);

    if (setting.isPresent()) {
      Class<?> settingClazz = setting.get().getClazz();

      if (Double.class.isAssignableFrom(settingClazz)) {
        return Double.valueOf(value);
      } else if (Integer.class.isAssignableFrom(settingClazz)) {
        return Integer.valueOf(value);
      } else if (Boolean.class.isAssignableFrom(settingClazz)) {
        return Boolean.valueOf(value);
      } else if (Locale.class.isAssignableFrom(settingClazz)) {
        return LocaleUtils.toLocale(value);
      } else if (Enum.class.isAssignableFrom(settingClazz)) {
        return Enum.valueOf((Class<? extends Enum>) settingClazz, value.toUpperCase());
      }

      // TODO handle Dates
    }

    return value;
  }

  // -------------------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------------------

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
