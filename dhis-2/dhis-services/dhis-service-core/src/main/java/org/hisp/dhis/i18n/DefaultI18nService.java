package org.hisp.dhis.i18n;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.translation.TranslationService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hisp.dhis.system.util.ReflectionUtils.getClassName;
import static org.hisp.dhis.system.util.ReflectionUtils.getProperty;

/**
 * @author Oyvind Brucker
 */
public class DefaultI18nService
    implements I18nService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private TranslationService translationService;

    public void setTranslationService( TranslationService translationService )
    {
        this.translationService = translationService;
    }

    private I18nLocaleService localeService;

    public void setLocaleService( I18nLocaleService localeService )
    {
        this.localeService = localeService;
    }

    private UserSettingService userSettingService;

    public void setUserSettingService( UserSettingService userSettingService )
    {
        this.userSettingService = userSettingService;
    }

    // -------------------------------------------------------------------------
    // I18nService implementation
    // -------------------------------------------------------------------------

    @Override
    public Map<String, String> getObjectPropertyValues( Object object )
    {
        if ( object == null )
        {
            return null;
        }

        List<String> properties = getObjectPropertyNames( object );

        Map<String, String> translations = new HashMap<>();

        for ( String property : properties )
        {
            translations.put( property, getProperty( object, property ) );
        }

        return translations;
    }

    @Override
    public List<String> getObjectPropertyNames( Object object )
    {
        if ( object == null )
        {
            return null;
        }

        if ( !(object instanceof IdentifiableObject) )
        {
            throw new IllegalArgumentException( "I18n object must be identifiable: " + object );
        }

        if ( object instanceof DataElement )
        {
            return Arrays.asList( DataElement.I18N_PROPERTIES );
        }

        return (object instanceof NameableObject) ? Arrays.asList( NameableObject.I18N_PROPERTIES ) :
            Arrays.asList( IdentifiableObject.I18N_PROPERTIES );
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public void removeObject( Object object )
    {
        if ( object != null )
        {
            translationService.deleteTranslations( getClassName( object ), getProperty( object, "uid" ) );
        }
    }

    // -------------------------------------------------------------------------
    // Translation
    // -------------------------------------------------------------------------

    @Override
    public void updateTranslation( String className, Locale locale, Map<String, String> translations, String objectUid )
    {
        if ( locale != null && className != null )
        {
            for ( Map.Entry<String, String> translationEntry : translations.entrySet() )
            {
                String key = translationEntry.getKey();
                String value = translationEntry.getValue();
                Translation t = new Translation( className, locale.toString(), key, value, objectUid );
                translationService.createOrUpdate( t );
            }
        }


    }

    @Override
    public Map<String, String> getTranslations( String className, String objectUid )
    {
        return getTranslations( className, getCurrentLocale(), objectUid );
    }

    @Override
    public Map<String, String> getTranslations( String className, Locale locale, String objectUid )
    {
        if ( locale != null && className != null )
        {
            return convertTranslations( translationService.getTranslations( className, locale, objectUid ) );
        }

        return new HashMap<>();
    }

    @Override
    public Map<String, String> getTranslationsNoFallback( String className, String objectUid )
    {


        return getTranslationsNoFallback( className, objectUid, getCurrentLocale() );
    }

    @Override
    public Map<String, String> getTranslationsNoFallback( String className, String objectUid, Locale locale )
    {
        if ( locale != null && className != null )
        {
            return convertTranslations( translationService.getTranslationsNoFallback( className, locale, objectUid ) );
        }

        return new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Locale
    // -------------------------------------------------------------------------

    @Override
    public Locale getCurrentLocale()
    {
        return (Locale) userSettingService.getUserSetting( UserSettingKey.DB_LOCALE );
    }

    @Override
    public boolean currentLocaleIsBase()
    {
        return getCurrentLocale() == null;
    }

    @Override
    public List<Locale> getAvailableLocales()
    {
        return localeService.getAllLocales();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a map for a collection of Translations where the key is the
     * translation property and the value is the translation value.
     *
     * @param translations the Collection of translations.
     * @return Map containing translations.
     */
    private Map<String, String> convertTranslations( Collection<Translation> translations )
    {
        Map<String, String> translationMap = new Hashtable<>();

        for ( Translation translation : translations )
        {
            if ( translation.getProperty() != null && translation.getValue() != null )
            {
                translationMap.put( translation.getProperty(), translation.getValue() );
            }
        }

        return translationMap;
    }
}
