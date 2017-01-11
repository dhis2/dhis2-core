package org.hisp.dhis.setting;

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

import org.hisp.dhis.util.ObjectUtils;

import java.util.Hashtable;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * @author James Chang
 */
public class DefaultTranslateSystemSettingManager
    implements TranslateSystemSettingManager
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    // -------------------------------------------------------------------------
    // Method implementation
    // -------------------------------------------------------------------------

    @Override
    public Map<String, String> getTranslationSystemAppearanceSettings( String locale )
    {
        Map<String, String> translations = new Hashtable<>();

        translations.put( SettingKey.APPLICATION_TITLE.getName(), getSystemSettingWithFallbacks( SettingKey.APPLICATION_TITLE.getName(), locale, SettingKey.APPLICATION_TITLE.getDefaultValue().toString() ) );
        translations.put( SettingKey.APPLICATION_INTRO.getName(), getSystemSettingWithFallbacks( SettingKey.APPLICATION_INTRO.getName(), locale, EMPTY ) );
        translations.put( SettingKey.APPLICATION_NOTIFICATION.getName(), getSystemSettingWithFallbacks( SettingKey.APPLICATION_NOTIFICATION.getName(), locale, EMPTY ) );
        translations.put( SettingKey.APPLICATION_FOOTER.getName(), getSystemSettingWithFallbacks( SettingKey.APPLICATION_FOOTER.getName(), locale, EMPTY ) );
        translations.put( SettingKey.APPLICATION_RIGHT_FOOTER.getName(), getSystemSettingWithFallbacks( SettingKey.APPLICATION_RIGHT_FOOTER.getName(), locale, EMPTY ) );

        return translations;
    }

    // -------------------------------------------------------------------------
    // Support Method implementation
    // -------------------------------------------------------------------------

    private String getSystemSettingWithFallbacks( String keyName, String localeStr, String defaultValue )
    {
        String settingValue = EMPTY;

        String keyWithLocale = (String) ObjectUtils.firstNonNull( systemSettingManager.getSystemSetting( keyName + localeStr ), "" );

        if ( keyWithLocale.isEmpty() )
        {
            settingValue = (String) ObjectUtils.firstNonNull( systemSettingManager.getSystemSetting( keyName ), defaultValue );
        }
        else
        {
            settingValue = keyWithLocale;
        }

        return settingValue;
    }
}
