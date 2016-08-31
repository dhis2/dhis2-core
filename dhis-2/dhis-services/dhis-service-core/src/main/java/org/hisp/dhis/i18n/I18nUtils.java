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

import java.util.List;
import java.util.Locale;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.system.paging.Paging;

/**
 * @author Lars Helge Overland
 */
public class I18nUtils
{
    public static <T> T i18n( I18nService i18nService, T object )
    {
        i18nService.internationalise( object );        
        return object;
    }
    
    public static <T> List<T> i18n( I18nService i18nService, List<T> objects )
    {
        i18nService.internationalise( objects );        
        return objects;
    }
    
    public static <T> T i18n( I18nService i18nService, Locale locale, T object )
    {
        i18nService.internationalise( object, locale );        
        return object;
    }
    
    public static <T> List<T> i18n( I18nService i18nService, Locale locale, List<T> objects )
    {
        i18nService.internationalise( objects, locale );        
        return objects;
    }
    
    public static <T extends IdentifiableObject> int getCountByName( 
        I18nService i18nService, GenericIdentifiableObjectStore<T> store, String name )
    {
        return i18nService.currentLocaleIsBase() ?
            store.getCountLikeName( name ) :
            Paging.getCountByName( i18n( i18nService, store.getAll() ), name );
    }
    
    public static <T extends IdentifiableObject> List<T> getObjectsBetween( 
        I18nService i18nService, GenericIdentifiableObjectStore<T> store, int first, int max )
    {
        return i18nService.currentLocaleIsBase() ?
            i18n( i18nService, store.getAllOrderedName( first, max ) ) :
            Paging.getObjectsBetween( i18n( i18nService, store.getAll() ), first, max );
    }
    
    public static <T extends IdentifiableObject> List<T> getObjectsBetweenByName(
        I18nService i18nService, GenericIdentifiableObjectStore<T> store, String name, int first, int max )
    {
        return i18nService.currentLocaleIsBase() ?
            i18n( i18nService, store.getAllLikeName( name, first, max ) ) :
            Paging.getObjectsBetweenByName( i18n( i18nService, store.getAll() ), name, first, max );
    }
    
    public static <T extends IdentifiableObject> List<T> getObjectsByName(
        I18nService i18nService, GenericIdentifiableObjectStore<T> store, String name )
    {
        return i18nService.currentLocaleIsBase() ?
            i18n( i18nService, store.getAllLikeName( name ) ) :
            Paging.getObjectsByName( i18n( i18nService, store.getAll() ), name );
    }
}
