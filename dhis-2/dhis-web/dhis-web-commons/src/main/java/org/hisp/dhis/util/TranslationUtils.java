package org.hisp.dhis.util;

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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.translation.ObjectTranslation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.hisp.dhis.system.util.ReflectionUtils.getProperty;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class TranslationUtils
{
    public static List<String> getObjectPropertyNames( Object object )
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

        return ( object instanceof NameableObject ) ? Arrays.asList( NameableObject.I18N_PROPERTIES ) :
            Arrays.asList( IdentifiableObject.I18N_PROPERTIES );
    }

    public static Map<String, String> getObjectPropertyValues( Object object )
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

    public static Map<String, String> convertTranslations( Set<ObjectTranslation> translations, Locale locale )
    {

        if ( !ObjectUtils.allNonNull( translations, locale ) )
        {
            return null;
        }

        Map<String, String> translationMap = new Hashtable<>();

        for ( ObjectTranslation translation : translations )
        {
            if ( StringUtils.isNotEmpty( translation.getValue() ) && translation.getLocale().equalsIgnoreCase( locale.toString() ) )
            {
                translationMap.put( translation.getProperty().getName(), translation.getValue() );
            }
        }

        return translationMap;
    }
}
