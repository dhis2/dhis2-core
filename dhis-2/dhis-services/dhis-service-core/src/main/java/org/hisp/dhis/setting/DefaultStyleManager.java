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

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.ObjectUtils;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class DefaultStyleManager
    implements StyleManager
{
    private static final String SEPARATOR = "/";

    private static final String SYSTEM_SEPARATOR = File.separator;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final SystemSettingManager systemSettingManager;

    private final UserSettingService userSettingService;

    /**
     * Map for styles. The key refers to the user setting key and the value
     * refers to the path to the CSS file of the style relative to
     * /dhis-web-commons/.
     */
    private final SortedMap<String, String> styles;

    private final I18nManager i18nManager;

    // -------------------------------------------------------------------------
    // StyleManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void setSystemStyle( String style )
    {
        systemSettingManager.saveSystemSetting( SettingKey.STYLE, style );
    }

    @Override
    public void setUserStyle( String style )
    {
        userSettingService.saveUserSetting( UserSettingKey.STYLE, style );
    }

    @Override
    public String getCurrentStyle()
    {
        String style = (String) userSettingService.getUserSetting( UserSettingKey.STYLE );

        return ObjectUtils.firstNonNull( style, getSystemStyle() );
    }

    @Override
    public String getSystemStyle()
    {
        return systemSettingManager.getStringSetting( SettingKey.STYLE );
    }

    @Override
    public String getCurrentStyleDirectory()
    {
        String currentStyle = getCurrentStyle();

        if ( currentStyle.lastIndexOf( SEPARATOR ) != -1 )
        {
            return currentStyle.substring( 0, currentStyle.lastIndexOf( SEPARATOR ) );
        }

        if ( currentStyle.lastIndexOf( SYSTEM_SEPARATOR ) != -1 )
        {
            return currentStyle.substring( 0, currentStyle.lastIndexOf( SYSTEM_SEPARATOR ) );
        }

        return currentStyle;
    }

    @Override
    public List<StyleObject> getStyles()
    {
        I18n i18n = i18nManager.getI18n();

        List<StyleObject> list = Lists.newArrayList();

        for ( Entry<String, String> entry : styles.entrySet() )
        {
            String name = i18n.getString( entry.getKey() );

            list.add( new StyleObject( name, entry.getKey(), entry.getValue() ) );
        }

        return list;
    }
}
