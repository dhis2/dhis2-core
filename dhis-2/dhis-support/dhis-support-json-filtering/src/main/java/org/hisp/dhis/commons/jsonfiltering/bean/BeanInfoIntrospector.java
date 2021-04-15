/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.bean;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.SneakyThrows;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hisp.dhis.commons.jsonfiltering.config.JsonFilteringConfig;
import org.hisp.dhis.commons.jsonfiltering.view.PropertyView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Introspects bean classes, looking for @{@link PropertyView} annotations on
 * fields.
 */
public class BeanInfoIntrospector
{

    /**
     * Caches bean class to a map of views to property views.
     */
    private static final LoadingCache<Class<?>, BeanInfo> CACHE;

    static
    {
        CACHE = CacheBuilder.from( JsonFilteringConfig.getPROPERTY_DESCRIPTOR_CACHE_SPEC() )
            .build( new CacheLoader<Class<?>, BeanInfo>()
            {
                @Override
                public BeanInfo load( @NonNull Class<?> key )
                {
                    return introspectClass( key );
                }
            } );
    }

    private static BeanInfo introspectClass( Class<?> beanClass )
    {

        Map<String, Set<String>> viewToPropertyNames = Maps.newHashMap();
        Set<String> unwrapped = Sets.newHashSet();

        for ( PropertyDescriptor propertyDescriptor : getPropertyDescriptors( beanClass ) )
        {

            if ( propertyDescriptor.getReadMethod() == null )
            {
                continue;
            }

            Field field = FieldUtils.getField( propertyDescriptor.getReadMethod().getDeclaringClass(),
                propertyDescriptor.getName(), true );
            String propertyName = getPropertyName( propertyDescriptor, field );

            if ( isUnwrapped( propertyDescriptor, field ) )
            {
                unwrapped.add( propertyName );
            }

            Set<String> views = introspectPropertyViews( propertyDescriptor, field );

            for ( String view : views )
            {
                viewToPropertyNames.computeIfAbsent( view, k -> Sets.newHashSet() )
                    .add( propertyName );
            }
        }

        viewToPropertyNames = makeUnmodifiable( expand( viewToPropertyNames ) );
        unwrapped = Collections.unmodifiableSet( unwrapped );

        return new BeanInfo( viewToPropertyNames, unwrapped );
    }

    private static String getPropertyName( PropertyDescriptor propertyDescriptor, Field field )
    {
        String propertyName = null;

        if ( propertyDescriptor.getReadMethod() != null )
        {
            // noinspection ConstantConditions
            propertyName = getPropertyName( propertyName, propertyDescriptor.getReadMethod().getAnnotations() );
        }

        if ( propertyDescriptor.getWriteMethod() != null )
        {
            propertyName = getPropertyName( propertyName, propertyDescriptor.getWriteMethod().getAnnotations() );
        }

        if ( field != null )
        {
            propertyName = getPropertyName( propertyName, field.getAnnotations() );
        }

        if ( propertyName == null )
        {
            propertyName = propertyDescriptor.getName();
        }

        return propertyName;
    }

    private static String getPropertyName( String propertyName, Annotation[] annotations )
    {
        if ( propertyName != null )
        {
            return propertyName;
        }

        for ( Annotation ann : annotations )
        {
            if ( ann instanceof JsonProperty )
            {
                propertyName = getPropertyName( (JsonProperty) ann );

                if ( propertyName != null )
                {
                    return propertyName;
                }

            }

            for ( Annotation classAnn : ann.annotationType().getAnnotations() )
            {
                if ( classAnn instanceof JsonProperty )
                {
                    propertyName = getPropertyName( (JsonProperty) classAnn );

                    if ( propertyName != null )
                    {
                        return propertyName;
                    }
                }
            }
        }

        return null;
    }

    private static String getPropertyName( JsonProperty ann )
    {
        return StringUtils.defaultIfEmpty( ann.value(), null );
    }

    private static boolean isUnwrapped( PropertyDescriptor propertyDescriptor, Field field )
    {
        if ( field != null && field.isAnnotationPresent( JsonUnwrapped.class ) )
        {
            return true;
        }

        Method readMethod = propertyDescriptor.getReadMethod();

        if ( readMethod != null && readMethod.isAnnotationPresent( JsonUnwrapped.class ) )
        {
            return true;
        }

        Method writeMethod = propertyDescriptor.getWriteMethod();

        return writeMethod != null && writeMethod.isAnnotationPresent( JsonUnwrapped.class );
    }

    private static Map<String, Set<String>> makeUnmodifiable( Map<String, Set<String>> map )
    {
        map.replaceAll( ( k, v ) -> Collections.unmodifiableSet( map.get( k ) ) );

        return Collections.unmodifiableMap( map );
    }

    @SneakyThrows
    private static PropertyDescriptor[] getPropertyDescriptors( Class<?> beanClass )
    {
        return Introspector.getBeanInfo( beanClass ).getPropertyDescriptors();
    }

    // apply the base fields to other views if configured to do so.
    private static Map<String, Set<String>> expand( Map<String, Set<String>> viewToPropNames )
    {

        Set<String> baseProps = viewToPropNames.get( PropertyView.BASE_VIEW );

        if ( baseProps == null )
        {
            baseProps = ImmutableSet.of();
        }

        if ( !JsonFilteringConfig.isFILTER_IMPLICITLY_INCLUDE_BASE_FIELDS_IN_VIEW() )
        {

            // make an exception for full view
            Set<String> fullView = viewToPropNames.get( PropertyView.FULL_VIEW );

            if ( fullView != null )
            {
                fullView.addAll( baseProps );
            }

            return viewToPropNames;
        }

        for ( Map.Entry<String, Set<String>> entry : viewToPropNames.entrySet() )
        {
            String viewName = entry.getKey();
            Set<String> propNames = entry.getValue();

            if ( !PropertyView.BASE_VIEW.equals( viewName ) )
            {
                propNames.addAll( baseProps );
            }
        }

        return viewToPropNames;
    }

    // grab all the PropertyView (or derived) annotations and return their view
    // names.
    private static Set<String> introspectPropertyViews( PropertyDescriptor propertyDescriptor, Field field )
    {

        Set<String> views = Sets.newHashSet();

        if ( propertyDescriptor.getReadMethod() != null )
        {
            applyPropertyViews( views, propertyDescriptor.getReadMethod().getAnnotations() );
        }

        if ( propertyDescriptor.getWriteMethod() != null )
        {
            applyPropertyViews( views, propertyDescriptor.getWriteMethod().getAnnotations() );
        }

        if ( field != null )
        {
            applyPropertyViews( views, field.getAnnotations() );
        }

        if ( views.isEmpty() && JsonFilteringConfig.isPROPERTY_ADD_NON_ANNOTATED_FIELDS_TO_BASE_VIEW() )
        {
            return Collections.singleton( PropertyView.BASE_VIEW );
        }

        return views;
    }

    private static void applyPropertyViews( Set<String> views, Annotation[] annotations )
    {
        for ( Annotation ann : annotations )
        {
            if ( ann instanceof PropertyView )
            {
                views.addAll( Lists.newArrayList( ((PropertyView) ann).value() ) );
            }

            for ( Annotation classAnn : ann.annotationType().getAnnotations() )
            {
                if ( classAnn instanceof PropertyView )
                {
                    views.addAll( Lists.newArrayList( ((PropertyView) classAnn).value() ) );
                }
            }
        }
    }

    public BeanInfo introspect( Class<?> beanClass )
    {
        return CACHE.getUnchecked( beanClass );
    }
}
