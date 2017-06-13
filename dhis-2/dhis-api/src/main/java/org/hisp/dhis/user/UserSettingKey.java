package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.lang3.LocaleUtils;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Lars Helge Overland
 */
public enum UserSettingKey
{
    STYLE( "keyStyle" ),
    MESSAGE_EMAIL_NOTIFICATION( "keyMessageEmailNotification", Boolean.class ),
    MESSAGE_SMS_NOTIFICATION( "keyMessageSmsNotification", Boolean.class ),
    UI_LOCALE( "keyUiLocale", Locale.class ),
    DB_LOCALE( "keyDbLocale", Locale.class ),
    ANALYSIS_DISPLAY_PROPERTY( "keyAnalysisDisplayProperty", String.class ),
    TRACKER_DASHBOARD_LAYOUT( "keyTrackerDashboardLayout" ),
    AUTO_SAVE_CASE_ENTRY_FORM( "keyAutoSaveCaseEntryForm", Boolean.class ),
    AUTO_SAVE_TRACKED_ENTITY_REGISTRATION_ENTRY_FORM( "keyAutoSavetTrackedEntityForm", Boolean.class ),
    AUTO_SAVE_DATA_ENTRY_FORM( "keyAutoSaveDataEntryForm", Boolean.class );

    private final String name;

    private final Class<?> clazz;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    UserSettingKey( String name )
    {
        this.name = name;
        this.clazz = String.class;
    }

    UserSettingKey( String name, Class<?> clazz )
    {
        this.name = name;
        this.clazz = clazz;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public static Optional<UserSettingKey> getByName( String name )
    {
        for ( UserSettingKey setting : UserSettingKey.values() )
        {
            if ( setting.getName().equals( name ) )
            {
                return Optional.of( setting );
            }
        }

        return Optional.empty();
    }

    public static Serializable getAsRealClass( String name, String value )
    {
        Optional<UserSettingKey> setting = getByName( name );

        if ( setting.isPresent() )
        {
            Class<?> settingClazz = setting.get().getClazz();

            if ( Double.class.isAssignableFrom( settingClazz ) )
            {
                return Double.valueOf( value );
            }
            else if ( Integer.class.isAssignableFrom( settingClazz ) )
            {
                return Integer.valueOf( value );
            }
            else if ( Boolean.class.isAssignableFrom( settingClazz ) )
            {
                return Boolean.valueOf( value );
            }
            else if ( Locale.class.isAssignableFrom( settingClazz ) )
            {
                return LocaleUtils.toLocale( value );
            }

            //TODO handle Dates
        }

        return value;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }

    public Class<?> getClazz()
    {
        return clazz;
    }
}
