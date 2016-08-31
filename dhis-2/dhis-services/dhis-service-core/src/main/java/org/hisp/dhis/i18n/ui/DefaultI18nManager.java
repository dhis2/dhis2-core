package org.hisp.dhis.i18n.ui;

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

import java.util.Locale;
import java.util.ResourceBundle;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManagerException;

/**
 * @author Pham Thi Thuy
 * @author Nguyen Dang Quang
 */
public class DefaultI18nManager
    implements I18nManager
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ResourceBundleManager resourceBundleManager;

    public void setResourceBundleManager( ResourceBundleManager resourceBundleManager )
    {
        this.resourceBundleManager = resourceBundleManager;
    }

    private LocaleManager localeManager;

    public void setLocaleManager( LocaleManager localeManager )
    {
        this.localeManager = localeManager;
    }

    // -------------------------------------------------------------------------
    // I18nManager implementation
    // -------------------------------------------------------------------------

    @Override
    public I18n getI18n()
    {
        Locale locale = getCurrentLocale();
        
        return new I18n( getGlobalResourceBundle( locale ), null );
    }

    @Override
    public I18n getI18n( Locale locale )
    {
        return new I18n( getGlobalResourceBundle( locale ), null );
    }

    @Override
    public I18n getI18n( Class<?> clazz )
    {
        return new I18n( getGlobalResourceBundle(), getSpecificResourceBundle( clazz.getName() ) );
    }

    @Override
    public I18n getI18n( Class<?> clazz, Locale locale )
    {
        return new I18n( getGlobalResourceBundle( locale ), getSpecificResourceBundle( clazz.getName(), locale ) );
    }

    @Override
    public I18n getI18n( String clazzName )
    {
        return new I18n( getGlobalResourceBundle(), getSpecificResourceBundle( clazzName ) );
    }
    
    @Override
    public I18nFormat getI18nFormat()
    {
        I18nFormat formatter = new I18nFormat( getGlobalResourceBundle() );

        formatter.init();

        return formatter;
    }

    // -------------------------------------------------------------------------
    // Support methods
    // -------------------------------------------------------------------------

    private ResourceBundle getGlobalResourceBundle()
    {
        return getGlobalResourceBundle( getCurrentLocale() );
    }
    
    private ResourceBundle getGlobalResourceBundle( Locale locale )
    {
        try
        {
            return resourceBundleManager.getGlobalResourceBundle( locale );
        }
        catch ( ResourceBundleManagerException e )
        {
            throw new RuntimeException( "Failed to get global resource bundle", e );
        }
    }

    private ResourceBundle getSpecificResourceBundle( String clazzName )
    {
        return resourceBundleManager.getSpecificResourceBundle( clazzName, getCurrentLocale() );
    }
    
    private ResourceBundle getSpecificResourceBundle( String clazzName, Locale locale )
    {
        return resourceBundleManager.getSpecificResourceBundle( clazzName, locale );
    }

    private Locale getCurrentLocale()
    {
        return localeManager.getCurrentLocale();
    }
}
