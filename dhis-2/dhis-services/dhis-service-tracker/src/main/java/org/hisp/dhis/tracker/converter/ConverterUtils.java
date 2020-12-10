package org.hisp.dhis.tracker.converter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.tracker.domain.TrackerDto;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.SneakyThrows;

/**
 * @author Luciano Fiandesio
 */
public class ConverterUtils
{

    @SneakyThrows
    public static List<Field> getPatchFields( Class<? extends TrackerDto> clazz, Object o )
    {
        final Set<Field> fieldsAnnotatedWith = new Reflections( clazz, new FieldAnnotationsScanner() )
            .getFieldsAnnotatedWith( JsonProperty.class );

        List<Field> patchableFields = new ArrayList<>();
        for ( Field field : fieldsAnnotatedWith )
        {
            if ( field.getDeclaringClass().equals( clazz ) )
            {
                if ( !Collection.class.isAssignableFrom( field.getType() ) )
                {
                    ReflectionUtils.makeAccessible( field );

                    if ( ReflectionUtils.getField( field, o ) != null
                        || (String.class.isAssignableFrom( field.getType() )
                            && !StringUtils.isEmpty( field.get( o ) )) )
                    {
                        // TODO how to deal with boolean and default values?
                        patchableFields.add( field );
                    }
                }
            }
        }
        return patchableFields;
    }
}
