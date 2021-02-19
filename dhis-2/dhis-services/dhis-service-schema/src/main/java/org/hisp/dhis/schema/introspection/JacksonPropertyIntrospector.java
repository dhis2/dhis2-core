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
package org.hisp.dhis.schema.introspection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.system.util.AnnotationUtils;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.system.util.SchemaUtils;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Primitives;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com> (original author)
 * @author Jan Bernitt (extraction to this class)
 */
@Slf4j
public class JacksonPropertyIntrospector implements PropertyIntrospector
{
    @Override
    public void introspect( Class<?> clazz, Map<String, Property> properties )
    {
        List<String> classFieldNames = ReflectionUtils.getAllFieldNames( clazz );

        // TODO this is quite nasty, should find a better way of exposing
        // properties at class-level
        if ( AnnotationUtils.isAnnotationPresent( clazz, JacksonXmlRootElement.class ) )
        {
            properties.put( "__self__", createSelfProperty( clazz ) );
        }

        Map<String, String> translatableFields = AnnotationUtils.getTranslatableAnnotatedFields( clazz );

        for ( Property property : collectProperties( clazz ) )
        {
            Method getterMethod = property.getGetterMethod();
            JsonProperty jsonProperty = AnnotationUtils.getAnnotation( getterMethod, JsonProperty.class );

            String fieldName = getFieldName( getterMethod );
            property.setName( !StringUtils.isEmpty( jsonProperty.value() ) ? jsonProperty.value() : fieldName );

            if ( property.getGetterMethod() != null )
            {
                property.setReadable( true );
            }

            if ( property.getSetterMethod() != null )
            {
                property.setWritable( true );
            }

            if ( classFieldNames.contains( fieldName ) )
            {
                property.setFieldName( fieldName );
            }

            if ( properties.containsKey( fieldName ) )
            {
                Property existing = properties.get( fieldName );
                property.setPersisted( true );
                property.setWritable( true );
                property.setUnique( existing.isUnique() );
                property.setRequired( existing.isRequired() );
                property.setLength( existing.getLength() );
                property.setMax( existing.getMax() );
                property.setMin( existing.getMin() );
                property.setCollection( existing.isCollection() );
                property.setCascade( existing.getCascade() );
                property.setOwner( existing.isOwner() );
                property.setManyToMany( existing.isManyToMany() );
                property.setOneToOne( existing.isOneToOne() );
                property.setManyToOne( existing.isManyToOne() );
                property.setOwningRole( existing.getOwningRole() );
                property.setInverseRole( existing.getInverseRole() );
                // the existing is replaced later on with the modified one
            }

            if ( AnnotationUtils.isAnnotationPresent( property.getGetterMethod(), Description.class ) )
            {
                Description description = AnnotationUtils.getAnnotation( property.getGetterMethod(),
                    Description.class );
                property.setDescription( description.value() );
            }

            if ( AnnotationUtils.isAnnotationPresent( property.getGetterMethod(), JacksonXmlProperty.class ) )
            {
                JacksonXmlProperty jacksonXmlProperty = AnnotationUtils.getAnnotation( getterMethod,
                    JacksonXmlProperty.class );

                if ( StringUtils.isEmpty( jacksonXmlProperty.localName() ) )
                {
                    property.setName( property.getName() );
                }
                else
                {
                    property.setName( jacksonXmlProperty.localName() );
                }

                if ( !StringUtils.isEmpty( jacksonXmlProperty.namespace() ) )
                {
                    property.setNamespace( jacksonXmlProperty.namespace() );
                }

                property.setAttribute( jacksonXmlProperty.isAttribute() );
            }

            Class<?> returnType = property.getGetterMethod().getReturnType();
            property.setKlass( Primitives.wrap( returnType ) );

            if ( Collection.class.isAssignableFrom( returnType ) )
            {
                property.setCollection( true );
                property.setCollectionName( property.getName() );
                property.setOrdered( List.class.isAssignableFrom( returnType ) );

                Type type = property.getGetterMethod().getGenericReturnType();

                if ( type instanceof ParameterizedType )
                {
                    Class<?> klass = (Class<?>) getInnerType( (ParameterizedType) type );
                    property.setItemKlass( Primitives.wrap( klass ) );

                    if ( collectProperties( klass ).isEmpty() )
                    {
                        property.setSimple( true );
                    }

                    property.setIdentifiableObject( IdentifiableObject.class.isAssignableFrom( klass ) );
                    property.setNameableObject( NameableObject.class.isAssignableFrom( klass ) );
                    property.setEmbeddedObject( EmbeddedObject.class.isAssignableFrom( klass ) );
                    property.setAnalyticalObject( AnalyticalObject.class.isAssignableFrom( klass ) );
                }
            }
            else
            {
                if ( collectProperties( returnType ).isEmpty() )
                {
                    property.setSimple( true );
                }
            }

            if ( translatableFields.containsKey( property.getFieldName() ) )
            {
                property.setTranslatable( true );
                property.setTranslationKey( translatableFields.get( property.getFieldName() ) );
            }

            if ( property.isCollection() )
            {
                if ( AnnotationUtils.isAnnotationPresent( property.getGetterMethod(), JacksonXmlElementWrapper.class ) )
                {
                    JacksonXmlElementWrapper jacksonXmlElementWrapper = AnnotationUtils.getAnnotation( getterMethod,
                        JacksonXmlElementWrapper.class );
                    property.setCollectionWrapping( jacksonXmlElementWrapper.useWrapping() );

                    // TODO what if element-wrapper have different namespace?
                    if ( !StringUtils.isEmpty( jacksonXmlElementWrapper.localName() ) )
                    {
                        property.setCollectionName( jacksonXmlElementWrapper.localName() );
                    }
                }

                properties.put( property.getCollectionName(), property );
            }
            else
            {
                properties.put( property.getName(), property );
            }

            if ( Enum.class.isAssignableFrom( property.getKlass() ) )
            {
                Object[] enumConstants = property.getKlass().getEnumConstants();
                List<String> enumValues = new ArrayList<>();

                for ( Object value : enumConstants )
                {
                    enumValues.add( value.toString() );
                }

                property.setConstants( enumValues );
            }

            SchemaUtils.updatePropertyTypes( property );
        }
    }

    private static Property createSelfProperty( Class<?> clazz )
    {
        Property self = new Property();

        JacksonXmlRootElement jacksonXmlRootElement = AnnotationUtils.getAnnotation( clazz,
            JacksonXmlRootElement.class );

        if ( !StringUtils.isEmpty( jacksonXmlRootElement.localName() ) )
        {
            self.setName( jacksonXmlRootElement.localName() );
        }

        if ( !StringUtils.isEmpty( jacksonXmlRootElement.namespace() ) )
        {
            self.setNamespace( jacksonXmlRootElement.namespace() );
        }
        return self;
    }

    private static String getFieldName( Method method )
    {
        String name;

        String[] getters = new String[] { "is", "has", "get" };

        name = method.getName();

        for ( String getter : getters )
        {
            if ( name.startsWith( getter ) )
            {
                name = name.substring( getter.length() );
            }
        }

        return StringUtils.uncapitalize( name );
    }

    private static List<Property> collectProperties( Class<?> klass )
    {
        boolean isPrimitiveOrWrapped = ClassUtils.isPrimitiveOrWrapper( klass );

        if ( isPrimitiveOrWrapped )
        {
            return Collections.emptyList();
        }

        Multimap<String, Method> multimap = ReflectionUtils.getMethodsMultimap( klass );
        List<String> fieldNames = ReflectionUtils.getAllFields( klass ).stream().map( Field::getName )
            .collect( Collectors.toList() );
        List<Property> properties = new ArrayList<>();

        Map<String, Method> methodMap = multimap.keySet().stream().filter( key -> {
            List<Method> methods = multimap.get( key ).stream()
                .filter( method -> AnnotationUtils.isAnnotationPresent( method, JsonProperty.class )
                    && method.getParameterTypes().length == 0 )
                .collect( Collectors.toList() );

            if ( methods.size() > 1 )
            {
                log.error( "More than one web-api exposed method with name '" + key + "' found on class '"
                    + klass.getName() + "' please fix as this is known to cause issues with Schema / Query services." );

                log.debug( "Methods found: " + methods );
            }

            return methods.size() == 1;
        } ).collect( Collectors.toMap( Function.identity(), key -> {
            List<Method> collect = multimap.get( key ).stream()
                .filter( method -> AnnotationUtils.isAnnotationPresent( method, JsonProperty.class )
                    && method.getParameterTypes().length == 0 )
                .collect( Collectors.toList() );

            return collect.get( 0 );
        } ) );

        methodMap.keySet().forEach( key -> {
            String fieldName = getFieldName( methodMap.get( key ) );
            String setterName = "set" + StringUtils.capitalize( fieldName );

            Property property = new Property( klass, methodMap.get( key ), null );

            if ( fieldNames.contains( fieldName ) )
            {
                property.setFieldName( fieldName );
            }

            Iterator<Method> methodIterator = multimap.get( setterName ).iterator();

            if ( methodIterator.hasNext() )
            {
                property.setSetterMethod( methodIterator.next() );
            }

            properties.add( property );
        } );

        return properties;
    }

    private static Type getInnerType( ParameterizedType parameterizedType )
    {
        ParameterizedType innerType = parameterizedType;

        while ( innerType.getActualTypeArguments()[0] instanceof ParameterizedType )
        {
            innerType = (ParameterizedType) parameterizedType.getActualTypeArguments()[0];
        }

        return innerType.getActualTypeArguments()[0];
    }
}
