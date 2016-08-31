package org.hisp.dhis.schema;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.annotation.Description;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Default PropertyIntrospectorService implementation that uses Reflection and Jackson annotations
 * for reading in properties.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Jackson2PropertyIntrospectorService
    extends AbstractPropertyIntrospectorService
{
    @Override
    protected Map<String, Property> scanClass( Class<?> clazz )
    {
        Map<String, Property> propertyMap = Maps.newHashMap();
        Map<String, Property> hibernatePropertyMap = getPropertiesFromHibernate( clazz );
        List<String> classFieldNames = ReflectionUtils.getAllFieldNames( clazz );

        // TODO this is quite nasty, should find a better way of exposing properties at class-level
        if ( clazz.isAnnotationPresent( JacksonXmlRootElement.class ) )
        {
            Property property = new Property();

            JacksonXmlRootElement jacksonXmlRootElement = clazz.getAnnotation( JacksonXmlRootElement.class );

            if ( !StringUtils.isEmpty( jacksonXmlRootElement.localName() ) )
            {
                property.setName( jacksonXmlRootElement.localName() );
            }

            if ( !StringUtils.isEmpty( jacksonXmlRootElement.namespace() ) )
            {
                property.setNamespace( jacksonXmlRootElement.namespace() );
            }

            propertyMap.put( "__self__", property );
        }

        List<Property> properties = collectProperties( clazz );

        for ( Property property : properties )
        {
            Method getterMethod = property.getGetterMethod();
            JsonProperty jsonProperty = getterMethod.getAnnotation( JsonProperty.class );

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

            if ( hibernatePropertyMap.containsKey( fieldName ) )
            {
                Property hibernateProperty = hibernatePropertyMap.get( fieldName );
                property.setPersisted( true );
                property.setWritable( true );
                property.setUnique( hibernateProperty.isUnique() );
                property.setRequired( hibernateProperty.isRequired() );
                property.setLength( hibernateProperty.getLength() );
                property.setMax( hibernateProperty.getMax() );
                property.setMin( hibernateProperty.getMin() );
                property.setCollection( hibernateProperty.isCollection() );
                property.setCascade( hibernateProperty.getCascade() );
                property.setOwner( hibernateProperty.isOwner() );
                property.setManyToMany( hibernateProperty.isManyToMany() );
                property.setOneToOne( hibernateProperty.isOneToOne() );
                property.setManyToOne( hibernateProperty.isManyToOne() );
                property.setOwningRole( hibernateProperty.getOwningRole() );
                property.setInverseRole( hibernateProperty.getInverseRole() );

                property.setGetterMethod( hibernateProperty.getGetterMethod() );
                property.setSetterMethod( hibernateProperty.getSetterMethod() );
            }

            if ( property.getGetterMethod().isAnnotationPresent( Description.class ) )
            {
                Description description = property.getGetterMethod().getAnnotation( Description.class );
                property.setDescription( description.value() );
            }

            if ( property.getGetterMethod().isAnnotationPresent( JacksonXmlProperty.class ) )
            {
                JacksonXmlProperty jacksonXmlProperty = getterMethod.getAnnotation( JacksonXmlProperty.class );

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

                Type type = property.getGetterMethod().getGenericReturnType();

                if ( ParameterizedType.class.isInstance( type ) )
                {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Class<?> klass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                    property.setItemKlass( Primitives.wrap( klass ) );

                    if ( collectProperties( klass ).isEmpty() )
                    {
                        property.setSimple( true );
                    }

                    if ( IdentifiableObject.class.isAssignableFrom( klass ) )
                    {
                        property.setIdentifiableObject( true );

                        if ( NameableObject.class.isAssignableFrom( klass ) )
                        {
                            property.setNameableObject( true );
                        }
                    }
                }
            }
            else
            {
                if ( collectProperties( returnType ).isEmpty() )
                {
                    property.setSimple( true );
                }
            }

            if ( property.isCollection() )
            {
                if ( property.getGetterMethod().isAnnotationPresent( JacksonXmlElementWrapper.class ) )
                {
                    JacksonXmlElementWrapper jacksonXmlElementWrapper = getterMethod.getAnnotation( JacksonXmlElementWrapper.class );
                    property.setCollectionWrapping( jacksonXmlElementWrapper.useWrapping() );

                    // TODO what if element-wrapper have different namespace?
                    if ( !StringUtils.isEmpty( jacksonXmlElementWrapper.localName() ) )
                    {
                        property.setCollectionName( jacksonXmlElementWrapper.localName() );
                    }
                }

                propertyMap.put( property.getCollectionName(), property );
            }
            else
            {
                propertyMap.put( property.getName(), property );
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

        return propertyMap;
    }

    private String getFieldName( Method method )
    {
        String name;

        String[] getters = new String[]{
            "is", "has", "get"
        };

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

    private List<Property> collectProperties( Class<?> klass )
    {
        Map<String, Method> methodMap = ReflectionUtils.getMethodMap( klass );
        List<Property> properties = Lists.newArrayList();

        methodMap.values().stream()
            .filter( method -> method.isAnnotationPresent( JsonProperty.class ) && method.getGenericParameterTypes().length == 0 )
            .forEach( method -> {
                String fieldName = getFieldName( method );
                String setterName = "set" + StringUtils.capitalize( fieldName );

                Property property = new Property( klass, method, null );
                property.setFieldName( fieldName );
                property.setSetterMethod( methodMap.get( setterName ) );

                properties.add( property );
            } );

        return properties;
    }
}
