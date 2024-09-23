package org.hisp.dhis.setting;

/**
 * Read-only access to {@link SystemSettings}
 *
 * @author Jan Bernitt
 * @since 2.42
 * @implNote This is marked {@link FunctionalInterface} to increase the chance nobody adds a method
 *     to this as the sole purpose is to give access to the current {@link SystemSettings}. Anything
 *     that goes beyond should be added to the {@link SystemSettingsService}.
 */
@FunctionalInterface
public interface SystemSettingsProvider {

  /**
   * During a request the methods always returns the same instance unless {@link
   * SystemSettingsService#clearCurrentSettings()} is called manually.
   *
   * @return an immutable view of all settings (including confidential ones). For internal use only!
   */
  SystemSettings getCurrentSettings();
}
