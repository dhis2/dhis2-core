package org.hisp.dhis.i18n;

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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hisp.dhis.i18n.locale.I18nLocale;

public interface I18nLocaleService
{
    /**
     * Returns available languages in a mapping between code and name.
     */
    Map<String, String> getAvailableLanguages();
    
    /**
     * Returns available countries in a mapping between code and name.
     */
    Map<String, String> getAvailableCountries();
    
    boolean addI18nLocale( String language, String country );
    
    void saveI18nLocale( I18nLocale locale );
    
    I18nLocale getI18nLocale( int id );
    
    I18nLocale getI18nLocaleByUid( String uid );
        
    I18nLocale getI18nLocale( Locale locale );
    
    void deleteI18nLocale( I18nLocale locale );
    
    int getI18nLocaleCount();
    
    int getI18nLocaleCountByName( String name );
    
    List<I18nLocale> getI18nLocalesBetween( int first, int max );
    
    List<I18nLocale> getI18nLocalesBetweenLikeName( String name, int first, int max );
    
    List<Locale> getAllLocales();

}
