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
package org.hisp.dhis.common;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.schema.annotation.Gist;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserSettingKey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Base class for translatable object.
 * <p>
 * Contains necessary methods for storing and retrieving translation values.
 * <p>
 * Beside extending this object, translatable object needs to have a column
 * translations with type jblTranslations defined in hibernate mapping file.
 */
public class TranslatableObject
{
    /**
     * Set of available object translation, normally filtered by locale.
     */
    protected Set<Translation> translations = new HashSet<>();

    /**
     * Cache for object translations, where the cache key is a combination of
     * locale and translation property, and value is the translated value.
     */
    protected final Map<String, String> translationCache = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @Gist( included = Gist.Include.FALSE )
    @JsonProperty
    @JacksonXmlElementWrapper( localName = "translations", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "translation", namespace = DxfNamespaces.DXF_2_0 )
    public Set<Translation> getTranslations()
    {
        if ( translations == null )
        {
            translations = new HashSet<>();
        }

        return translations;
    }

    /**
     * Clears out cache when setting translations.
     */
    public void setTranslations( Set<Translation> translations )
    {
        this.translationCache.clear();
        this.translations = translations;
    }

    // -------------------------------------------------------------------------
    // Util methods
    // -------------------------------------------------------------------------

    /**
     * Returns a translated value for this object for the given property. The
     * current locale is read from the user context.
     *
     * @param translationKey the translation key.
     * @param defaultValue the value to use if there are no translations.
     * @return a translated value.
     */
    protected String getTranslation( String translationKey, String defaultValue )
    {
        Locale locale = CurrentUserUtil.getUserSetting( UserSettingKey.DB_LOCALE );

        final String defaultTranslation = defaultValue != null ? defaultValue.trim() : null;

        if ( locale == null || translationKey == null || CollectionUtils.isEmpty( translations ) )
        {
            return defaultValue;
        }

        return translationCache.computeIfAbsent( Translation.getCacheKey( locale.toString(), translationKey ),
            key -> getTranslationValue( locale.toString(), translationKey, defaultTranslation ) );

    }

    /**
     * Get Translation value from {@code Set<Translation>} by given locale and
     * translationKey
     *
     * @return Translation value if exists, otherwise return default value.
     */
    private String getTranslationValue( String locale, String translationKey, String defaultValue )
    {
        for ( Translation translation : translations )
        {
            if ( locale.equals( translation.getLocale() ) && translationKey.equals( translation.getProperty() ) &&
                !StringUtils.isEmpty( translation.getValue() ) )
            {
                return translation.getValue();
            }
        }

        return defaultValue;
    }
}
