package org.hisp.dhis.i18n;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.comparator.LocaleNameComparator;
import org.hisp.dhis.i18n.locale.I18nLocale;
import org.hisp.dhis.system.util.LocaleUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Transactional
public class DefaultI18nLocaleService
    implements I18nLocaleService
{    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private I18nLocaleStore localeStore;

    public void setLocaleStore( I18nLocaleStore localeStore )
    {
        this.localeStore = localeStore;
    }
    
    private Map<String, String> languages = new LinkedHashMap<>();

    private Map<String, String> countries = new LinkedHashMap<>();
    
    /**
     * Load all ISO languages and countries into mappings.
     */
    @PostConstruct
    public void init()
    {   
        List<IdentifiableObject> langs = new ArrayList<>();
        List<IdentifiableObject> countrs = new ArrayList<>();
        
        for ( String lang : Locale.getISOLanguages() )
        {
            langs.add( new BaseIdentifiableObject( lang, lang, new Locale( lang ).getDisplayLanguage() ) );
        }

        for ( String country : Locale.getISOCountries() )
        {
            countrs.add( new BaseIdentifiableObject( country, country, new Locale( "en", country ).getDisplayCountry() ) );
        }
        
        Collections.sort( langs );
        Collections.sort( countrs );
        
        for ( IdentifiableObject lang : langs )
        {
            languages.put( lang.getCode(), lang.getName() );
        }
        
        for ( IdentifiableObject countr : countrs )
        {
            countries.put( countr.getCode(), countr.getName() );
        }
    }

    // -------------------------------------------------------------------------
    // I18nLocaleService implementation
    // -------------------------------------------------------------------------

    @Override
    public Map<String, String> getAvailableLanguages()
    {
        return languages;
    }
    
    @Override
    public Map<String, String> getAvailableCountries()
    {
        return countries;
    }
    
    @Override
    public I18nLocale addI18nLocale( String language, String country )
    {
        String languageName = languages.get( language );
        String countryName = countries.get( country );
        
        if ( language == null || languageName == null )
        {
            throw new IllegalArgumentException( "Invalid Language." );
        }
        
        if ( country != null && countryName == null )
        {
            throw new IllegalArgumentException( "Invalid country." );
        }

        String localeStr = LocaleUtils.getLocaleString( language, country, null );
        Locale locale = LocaleUtils.getLocale( localeStr );
        
        I18nLocale i18nLocale = new I18nLocale( locale );
        
        saveI18nLocale( i18nLocale );
        
        return i18nLocale;
    }
        
    @Override
    public void saveI18nLocale( I18nLocale locale )
    {
        localeStore.save( locale );
    }
    
    @Override
    public I18nLocale getI18nLocale( int id )
    {
        return localeStore.get( id );
    }
    
    @Override
    public I18nLocale getI18nLocaleByUid( String uid )
    {
        return localeStore.getByUid( uid );
    }
        
    @Override
    public I18nLocale getI18nLocale( Locale locale )
    {
        return localeStore.getI18nLocaleByLocale( locale );
    }
    
    @Override
    public void deleteI18nLocale( I18nLocale locale )
    {
        localeStore.delete( locale );
    }
    
    @Override
    public int getI18nLocaleCount()
    {
        return localeStore.getCount();
    }
    
    @Override
    public List<Locale> getAllLocales()
    {
        List<Locale> locales = new ArrayList<>();
        
        for ( I18nLocale locale : localeStore.getAll() )
        {
            locales.add( LocaleUtils.getLocale( locale.getLocale() ) );
        }
        
        Collections.sort( locales, LocaleNameComparator.INSTANCE );
        
        return locales;
    }

    @Override
    public List<I18nLocale> getAllI18nLocales()
    {
        return localeStore.getAll();
    }

    
}
