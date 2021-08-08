/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.system.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.hisp.dhis.translation.Translatable;

/**
 * @author Lars Helge Overland
 */
public class AnnotationUtils
{
    /**
     * Returns methods on the given target object which are annotated with the
     * annotation of the given class.
     *
     * @param target the target object.
     * @param annotationType the annotation class type.
     * @return a list of methods annotated with the given annotation.
     */
    public static List<Method> getAnnotatedMethods( Object target, Class<? extends Annotation> annotationType )
    {
        final List<Method> methods = new ArrayList<>();

        if ( target == null || annotationType == null )
        {
            return methods;
        }

        for ( Method method : target.getClass().getMethods() )
        {
            Annotation annotation = org.springframework.core.annotation.AnnotationUtils.findAnnotation( method,
                annotationType );

            if ( annotation != null )
            {
                methods.add( method );
            }
        }

        return methods;
    }

    /**
     * Returns Map of fields and their getter methods on the given class and its
     * parents (if any) which are annotated with the annotation of the given
     * annotationType. The annotation can be applied to either field or getter
     * method.
     *
     * @param klass
     * @param annotationType
     * @return Map<Field, Method>
     */
    public static Map<Field, Method> getAnnotatedFields( Class<?> klass, Class<? extends Annotation> annotationType )
    {
        final Map<Field, Method> mapFields = new HashMap<>();

        if ( klass == null || annotationType == null )
        {
            return mapFields;
        }

        FieldUtils.getAllFieldsList( klass ).forEach( field -> {

            Method getter = ReflectionUtils.findGetterMethod( field.getName(), klass );

            if ( getter == null )
            {
                return;
            }

            if ( field.isAnnotationPresent( annotationType ) || getter.isAnnotationPresent( annotationType ) )
            {
                mapFields.put( field, getter );
            }
        } );

        return mapFields;
    }

    /**
     * Scan through all fields and getters of given class and return those with
     * {@link Translatable} marked. Return Map<String,String> with key is
     * fieldName and value is translation key. This translation key is used for
     * storing translation values in JsonB format.
     */
    public static Map<String, String> getTranslatableAnnotatedFields( Class<?> klass )
    {
        final Map<String, String> mapFields = new HashMap<>();

        if ( klass == null )
        {
            return mapFields;
        }

        List<Method> methods = MethodUtils.getMethodsListWithAnnotation( klass, Translatable.class, true, false );

        methods.forEach( method -> {
            Translatable translatableAnnotation = method.getAnnotation( Translatable.class );

            if ( translatableAnnotation != null )
            {
                mapFields.put( translatableAnnotation.propertyName(),
                    !StringUtils.isEmpty( translatableAnnotation.key() ) ? translatableAnnotation.key()
                        : translatableAnnotation.propertyName() );
            }
        } );

        return mapFields;
    }

    /**
     * Check to see if annotation is present on a given Class, take into account
     * class hierarchy.
     *
     * @param klass Class
     * @param annotationType Annotation
     * @return true/false depending on if annotation is present
     */
    public static boolean isAnnotationPresent( Class<?> klass, Class<? extends Annotation> annotationType )
    {
        return org.springframework.core.annotation.AnnotationUtils.findAnnotation( klass, annotationType ) != null;
    }

    /**
     * Check to see if annotation is present on a given Method, take into
     * account class hierarchy.
     *
     * @param method Method
     * @param annotationType Annotation
     * @return true/false depending on if annotation is present
     */
    public static boolean isAnnotationPresent( Method method, Class<? extends Annotation> annotationType )
    {
        return org.springframework.core.annotation.AnnotationUtils.findAnnotation( method, annotationType ) != null;
    }

    /**
     * Gets annotation on a given Class, takes into account class hierarchy.
     *
     * @param klass Class
     * @param annotationType Annotation
     * @return Annotation instance on Class
     */
    public static <A extends Annotation> A getAnnotation( Class<?> klass, Class<A> annotationType )
    {
        return org.springframework.core.annotation.AnnotationUtils.findAnnotation( klass, annotationType );
    }

    /**
     * Gets annotation on a given Method, takes into account class hierarchy.
     *
     * @param method Method
     * @param annotationType Annotation
     * @return Annotation instance on Method
     */
    public static <A extends Annotation> A getAnnotation( Method method, Class<A> annotationType )
    {
        return org.springframework.core.annotation.AnnotationUtils.findAnnotation( method, annotationType );
    }
}
