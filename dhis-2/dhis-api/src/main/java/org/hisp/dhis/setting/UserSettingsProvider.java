package org.hisp.dhis.setting;

@FunctionalInterface
public interface UserSettingsProvider {

  UserSettings getCurrentSettings();
}
