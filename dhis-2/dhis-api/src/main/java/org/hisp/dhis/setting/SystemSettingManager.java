package org.hisp.dhis.setting;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import java.util.Set;

/**
 * @author Stian Strandli
 */
public interface SystemSettingManager
{
    void saveSystemSetting( String name, Serializable value );

    void saveSystemSetting( SettingKey setting, Serializable value );

    void deleteSystemSetting( String name );

    void deleteSystemSetting( SettingKey setting );

    Serializable getSystemSetting( String name );

    Serializable getSystemSetting( SettingKey setting );

    Serializable getSystemSetting( SettingKey setting, Serializable defaultValue );

    List<SystemSetting> getAllSystemSettings();

    /**
     * Returns all system settings as a mapping between the setting name and the
     * value. Includes system settings which have a default value but no explicitly
     * set value.
     */
    Map<String, Serializable> getSystemSettingsAsMap();

    Map<String, Serializable> getSystemSettingsAsMap( Set<String> names );

    Map<String, Serializable> getSystemSettings( Collection<SettingKey> settings );

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

    String getInstanceBaseUrl();

    boolean accountRecoveryEnabled();

    boolean selfRegistrationNoRecaptcha();

    boolean emailEnabled();

    boolean systemNotificationEmailValid();

    boolean hideUnapprovedDataInAnalytics();

    boolean isOpenIdConfigured();

    String googleAnalyticsUA();

    Integer credentialsExpires();

    boolean isConfidential( String name );
}
