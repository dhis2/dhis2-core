package org.hisp.dhis.translation;

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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.hisp.dhis.system.util.LocaleUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Transactional
public class DefaultTranslationService
    implements TranslationService
{
    private static Cache<Class<?>, Boolean> IS_TRANSLATED_CACHE = Caffeine.newBuilder()
        .expireAfterAccess( 15, TimeUnit.MINUTES )
        .initialCapacity( 100 )
        .maximumSize( 200 )
        .build();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private TranslationStore translationStore;

    public void setTranslationStore( TranslationStore translationStore )
    {
        this.translationStore = translationStore;
    }

    // -------------------------------------------------------------------------
    // Translation
    // -------------------------------------------------------------------------

    @Override
    public void addTranslation( Translation translation )
    {
        translationStore.save( translation );
    }

    @Override
    public void createOrUpdate( Collection<Translation> translations )
    {
        for ( Translation translation : translations )
        {
            createOrUpdate( translation );
        }
    }

    @Override
    public void updateTranslation( Translation translation )
    {
        translationStore.update( translation );
    }

    @Override
    public Translation getTranslation( String className, Locale locale, String property, String objectUid )
    {
        return translationStore.getTranslation( className, locale, property, objectUid );
    }

    @Override
    public Translation getTranslationNoFallback( String className, Locale locale, String property, String objectUid )
    {
        return translationStore.getTranslationNoFallback( className, locale, property, objectUid );
    }

    @Override
    public List<Translation> getTranslations( String className, Locale locale, String objectUid )
    {
        return translationStore.getTranslations( className, locale, objectUid );
    }

    @Override
    public List<Translation> getTranslationsNoFallback( String className, Locale locale, String objectUid )
    {
        return translationStore.getTranslationsNoFallback( className, objectUid, locale );
    }

    @Override
    public List<Translation> getTranslations( String className, Locale locale )
    {
        return translationStore.getTranslations( className, locale );
    }

    @Override
    public List<Translation> getTranslations( Locale locale )
    {
        return translationStore.getTranslations( locale );
    }

    @Override
    public List<Translation> getAllTranslations()
    {
        return translationStore.getAll();
    }

    @Override
    public void deleteTranslation( Translation translation )
    {
        translationStore.delete( translation );
    }

    @Override
    public void deleteTranslations( String className, String objectUid )
    {
        translationStore.deleteTranslations( className, objectUid );
    }

    @Override
    public void createOrUpdate( Translation translation )
    {
        Translation translationNoFallback = getTranslationNoFallback( translation.getClassName(), LocaleUtils.getLocale( translation.getLocale() ), translation.getProperty(), translation.getObjectUid() );

        if ( translation.getValue() != null && !translation.getValue().trim().isEmpty() )
        {
            if ( translationNoFallback != null )
            {
                translationNoFallback.setValue( translation.getValue() );
                updateTranslation( translationNoFallback );
            }
            else
            {
                addTranslation( translation );
            }
        }
        else if ( translationNoFallback != null )
        {
            deleteTranslation( translationNoFallback );
        }
    }

    @Override
    public boolean hasTranslations( String className )
    {
        return translationStore.hasTranslations( className );
    }

    @Override
    public boolean isTranslated( final Class<?> klass )
    {
        return IS_TRANSLATED_CACHE.get( klass, c -> hasTranslations( klass.getSimpleName() ) );
    }
}
