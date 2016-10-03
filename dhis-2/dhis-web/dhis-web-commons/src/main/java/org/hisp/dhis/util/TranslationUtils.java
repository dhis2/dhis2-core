package org.hisp.dhis.util;

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

        return (object instanceof NameableObject) ? Arrays.asList( NameableObject.I18N_PROPERTIES ) :
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

        if ( translations == null || locale == null )
        {
            return null;
        }

        Map<String, String> translationMap = new Hashtable<>();

        for ( ObjectTranslation translation : translations )
        {
            if ( translation.getValue() != null && translation.getLocale().equals( locale.toString() ) )
            {
                translationMap.put( translation.getProperty().name().replace( "_","" ).toLowerCase(), translation.getValue() );
            }
        }

        return translationMap;
    }
}
