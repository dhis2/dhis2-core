/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static java.util.stream.Collectors.toSet;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

import org.hisp.dhis.common.OpenApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Captures the result of the controller analysis process in a data model.
 *
 * Simple fields in the model are "immutable" while collections are used
 * "mutable" to aggregate the results during the analysis process.
 *
 * Descriptions that are added later use a {@link Maybe} box, so they can be
 * used "mutable" too.
 *
 * @author Jan Bernitt
 */
@Value
public class Api
{
    /**
     * Can be used in {@link OpenApi.Param#value()} to point not to the type to
     * use but the generator to use.
     *
     * All generators must provide an accessible no args constructor and be
     * stateless.
     */
    @FunctionalInterface
    interface SchemaGenerator
    {
        Schema generate( Endpoint endpoint, Type source, Class<?>... args );
    }

    /**
     * A "virtual" property name enumeration type. It creates an OpenAPI
     * {@code enum} string schema containing all valid property names for the
     * target type. The target type is either the actual type substitute for the
     * {@link OpenApi.EntityType} or the first argument type.
     */
    @NoArgsConstructor
    public static final class PropertyNames
    {
    }

    List<Controller> controllers = new ArrayList<>();

    /**
     * Note that this needs to use the {@link ConcurrentSkipListMap} as most
     * other maps do not allow to be modified from within a callback that itself
     * is adding an entry like {@link Map#computeIfAbsent(Object, Function)}.
     * Here, while one {@link Schema} is resolved more {@link Schema} might be
     * added.
     */
    Map<Class<?>, Schema> schemas = new ConcurrentSkipListMap<>( Comparator.comparing( Class::getName ) );

    /**
     * "Global" or shared parameters originating from parameter object classes.
     * These are reused purely for sake of removing duplication from the
     * resulting OpenAPI document.
     */
    Map<String, Parameter> parameters = new TreeMap<>();

    /**
     * "Global" tag descriptions
     */
    Map<String, Tag> tags = new TreeMap<>();

    /**
     * @return all tags used in the {@link Api}
     */
    Set<String> getUsedTags()
    {
        Set<String> used = new TreeSet<>();
        controllers.forEach( controller -> {
            used.addAll( controller.getTags() );
            controller.endpoints.forEach( endpoint -> used.addAll( endpoint.getTags() ) );
        } );
        used.add( "synthetic" );
        return used;
    }

    @Data
    static final class Maybe<T>
    {
        T value;

        boolean isPresent()
        {
            return value != null;
        }

        T init( Supplier<T> ifNotPresent )
        {
            if ( !isPresent() )
            {
                setValue( ifNotPresent.get() );
            }
            return getValue();
        }

        T orElse( T defaultValue )
        {
            return value != null ? value : defaultValue;
        }
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Tag
    {
        @EqualsAndHashCode.Include
        String name;

        Maybe<String> description = new Maybe<>();

        Maybe<String> externalDocsUrl = new Maybe<>();

        Maybe<String> externalDocsDescription = new Maybe<>();
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Controller
    {
        @ToString.Exclude
        Api in;

        @ToString.Exclude
        @EqualsAndHashCode.Include
        Class<?> source;

        @ToString.Exclude
        @EqualsAndHashCode.Include
        Class<?> entityClass;

        String name;

        List<String> paths = new ArrayList<>();

        List<Endpoint> endpoints = new ArrayList<>();

        Set<String> tags = new LinkedHashSet<>();
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Endpoint
    {
        @ToString.Exclude
        Controller in;

        @ToString.Exclude
        Method source;

        @ToString.Exclude
        Class<?> entityType;

        @EqualsAndHashCode.Include
        String name;

        Maybe<String> description = new Maybe<>();

        Set<String> tags = new LinkedHashSet<>();

        Boolean deprecated;

        @EqualsAndHashCode.Include
        Set<RequestMethod> methods = EnumSet.noneOf( RequestMethod.class );

        @EqualsAndHashCode.Include
        Set<String> paths = new LinkedHashSet<>();

        Maybe<RequestBody> requestBody = new Maybe<>();

        Map<String, Parameter> parameters = new TreeMap<>();

        Map<HttpStatus, Response> responses = new EnumMap<>( HttpStatus.class );

        boolean isSynthetic()
        {
            return source == null;
        }

        boolean isDeprecated()
        {
            return Boolean.TRUE == deprecated;
        }

        String getEntityTypeName()
        {
            return entityType == null ? "?" : entityType.getSimpleName();
        }
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class RequestBody
    {
        @ToString.Exclude
        AnnotatedElement source;

        boolean required;

        Maybe<String> description = new Maybe<>();

        @EqualsAndHashCode.Include
        Map<MediaType, Schema> consumes = new TreeMap<>();
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Parameter
    {
        public enum In
        {
            PATH,
            QUERY,
            BODY
        }

        @ToString.Exclude
        AnnotatedElement source;

        String globalName;

        @EqualsAndHashCode.Include
        String name;

        @EqualsAndHashCode.Include
        In in;

        boolean required;

        Maybe<String> description = new Maybe<>();

        Schema type;

        /**
         * @return true, if this parameter is one or many in a complex parameter
         *         object, false, if this parameter directly occurred
         *         individually in the endpoint method signature.
         */
        boolean isShared()
        {
            return globalName != null;
        }
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Response
    {
        @EqualsAndHashCode.Include
        HttpStatus status;

        Map<String, Header> headers = new TreeMap<>();

        Maybe<String> description = new Maybe<>();

        @EqualsAndHashCode.Include
        Map<MediaType, Schema> content = new TreeMap<>();

        Response add( Set<MediaType> produces, Schema body )
        {
            produces.forEach( mediaType -> content.put( mediaType, body ) );
            return this;
        }

        Response add( Set<Header> headers )
        {
            headers.forEach( header -> this.headers.put( header.getName(), header ) );
            return this;
        }
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Header
    {
        @EqualsAndHashCode.Include
        String name;

        String description;

        Schema type;
    }

    @Value
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Property
    {
        @EqualsAndHashCode.Include
        String name;

        Boolean required;

        /**
         * OBS! This cannot be included in {@link #toString()} because it might
         * be a circular with the {@link Schema} containing the
         * {@link Property}.
         */
        @ToString.Exclude
        Schema type;
    }

    @Value
    @AllArgsConstructor
    @EqualsAndHashCode( onlyExplicitlyIncluded = true )
    static class Schema
    {
        public static Schema ref( Class<?> to )
        {
            return new Schema( Type.REF, false, to, to );
        }

        public static Schema uid( Class<?> of )
        {
            return new Schema( Type.UID, false, of, of );
        }

        public static Schema unknown( java.lang.reflect.Type source )
        {
            return new Schema( Type.UNKNOWN, false, source, Object.class );
        }

        public static Schema oneOf( List<Class<?>> types, Function<Class<?>, Schema> toSchema )
        {
            if ( types.size() == 1 )
            {
                return toSchema.apply( types.get( 0 ) );
            }
            Schema oneOf = new Schema( Type.ONE_OF, false, Object.class, Object.class );
            types.forEach( type -> oneOf.add(
                new Property( oneOf.properties.size() + "", null, toSchema.apply( type ) ) ) );
            return oneOf;
        }

        public static Schema enumeration( Class<?> source, Class<?> of, List<String> values )
        {
            Schema schema = new Schema( Type.ENUM, false, source, of );
            schema.getValues().addAll( values );
            return schema;
        }

        static boolean isNamed( Class<?> source )
        {
            String name = source.getName();
            return !source.isPrimitive()
                && !source.isEnum()
                && !source.isArray()
                && !name.startsWith( "java.lang" )
                && !name.startsWith( "java.util" );
        }

        public enum Type
        {
            SIMPLE,
            ARRAY,
            OBJECT,
            UID,
            REF,
            UNKNOWN,
            ONE_OF,
            ENUM
        }

        @EqualsAndHashCode.Include
        Type type;

        /**
         * False, unless this is a named "record" type that should be referenced
         * as a named schema in the generated OpenAPI document.
         */
        @EqualsAndHashCode.Include
        boolean named;

        @ToString.Exclude
        java.lang.reflect.Type source;

        @ToString.Exclude
        @EqualsAndHashCode.Include
        Class<?> rawType;

        /**
         * Is empty for primitive types
         */
        @EqualsAndHashCode.Include
        List<Property> properties = new ArrayList<>();

        /**
         * Enum values in case this is an enum schema.
         */
        List<String> values = new ArrayList<>();

        public Schema( Type type, java.lang.reflect.Type source, Class<?> rawType )
        {
            this( type, isNamed( rawType ), source, rawType );
        }

        Set<String> getRequiredProperties()
        {
            return getProperties().stream()
                .filter( property -> Boolean.TRUE.equals( property.getRequired() ) )
                .map( Property::getName )
                .collect( toSet() );
        }

        Api.Schema add( Property property )
        {
            properties.add( property );
            return this;
        }

        Api.Schema withElements( Schema componentType )
        {
            return add( new Property( "components", true, componentType ) );
        }

        Api.Schema withEntries( Schema keyType, Schema valueType )
        {
            return add( new Api.Property( "keys", true, keyType ) )
                .add( new Api.Property( "values", true, valueType ) );
        }
    }

}
