package org.hisp.dhis.setting;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Stian Strandli
 * @author Lars Helge Overland
 */
public interface SystemSettingManager
{
    /**
     * Saves the given system setting key and value.
     *
     * @param key the system setting key.
     * @param value the system setting value.
     */
    void saveSystemSetting( SettingKey key, Serializable value );

    /**
     * Saves the translation for given setting key and locale if given setting key is translatable.
     * If the translation string contains an empty string, the translation for given locale and key is removed.
     *
     * @param key SettingKey
     * @param locale locale of the translation
     * @param translation Actual translation
     */
    void saveSystemSettingTranslation( SettingKey key, String locale, String translation );

    /**
     * Deletes the system setting with the given key.
     *
     * @param key the system setting key.
     */
    void deleteSystemSetting( SettingKey key );

    /**
     * Returns the system setting value for the given key. If no value exists, returns
     * the default value held by {@link SettingKey#getDefaultValue()}. If not,
     * returns null.
     *
     * @param key the system setting key.
     * @return the setting value.
     */
    Serializable getSystemSetting( SettingKey key );

    /**
     * Returns the system setting value for the given key. If no value exists, returns
     * the default value as defined by the given default value.
     *
     * @param key the system setting key.
     * @return the setting value.
     */
    Serializable getSystemSetting( SettingKey key, Serializable defaultValue );

    /**
     * Returns the translation for given setting key and locale or empty Optional if no translation is
     * available or setting key is not translatable.
     * @param key SettingKey
     * @param locale Locale of required translation
     * @return The Optional with the actual translation or empty Optional
     */
    Optional<String> getSystemSettingTranslation( SettingKey key, String locale );

    /**
     * Returns all system settings.
     *
     * @return a list of all system settings.
     */
    List<SystemSetting> getAllSystemSettings();

    /**
     * Returns all system settings as a mapping between the setting name and the
     * value. Includes system settings which have a default value but no explicitly
     * set value.
     */
    Map<String, Serializable> getSystemSettingsAsMap();

    /**
     * Returns system settings for the given collection of setting keys as a map,
     * where the key is string representation of the {@link SettingKey}, and the
     * value is the setting value.
     *
     * @param keys the collection of setting keys.
     * @return a map of system settings.
     */
    Map<String, Serializable> getSystemSettings( Collection<SettingKey> keys );

    /**
     * Invalidates the currently cached system settings.
     */
    void invalidateCache();

    // -------------------------------------------------------------------------
    // Specific methods
    // -------------------------------------------------------------------------

    List<String> getFlags();

    String getFlagImage();

    String getEmailHostName();

    int getEmailPort();

    String getEmailUsername();

    boolean getEmailTls();

    String getEmailSender();

    boolean accountRecoveryEnabled();

    boolean selfRegistrationNoRecaptcha();

    boolean emailConfigured();

    boolean systemNotificationEmailValid();

    boolean hideUnapprovedDataInAnalytics();

    String googleAnalyticsUA();

    Integer credentialsExpires();

    boolean isConfidential( String name );

    boolean isTranslatable( String name );
}
