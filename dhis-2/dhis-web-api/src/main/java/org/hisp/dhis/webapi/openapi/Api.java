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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;

import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.http.HttpStatus;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Captures the result of the controller analysis process in a data model.
 *
 * Simple fields in the model are "immutable" while collections are used
 * "mutable" to aggregate the results during the analysis process.
 *
 * @author Jan Bernitt
 */
@Value
class Api
{
    List<Controller> controllers = new ArrayList<>();

    /**
     * Note that this needs to use the {@link ConcurrentSkipListMap} as most
     * other maps do not allow to be modified from within a callback that itself
     * is adding an entry like {@link Map#computeIfAbsent(Object, Function)}.
     * Here, while one {@link Schema} is resolved more {@link Schema} might be
     * added.
     */
    Map<String, Schema> schemas = new ConcurrentSkipListMap<>();

    /**
     * A {@link Ref} is used for all {@link IdentifiableObject} fields and
     * collections within other objects. This reflects the pattern used in DHIS2
     * that UID references to {@link IdentifiableObject}s are expected in this
     * form:
     *
     * <pre>
     *     {"id": "[uid]"}
     * </pre>
     *
     * while in the database model the full object will occur.
     */
    @Value
    static class Ref
    {
        String id;
    }

    @Value
    static class Field
    {

        String name;

        Schema type;

        Boolean required;
    }

    @Value
    static class Controller
    {
        @ToString.Exclude
        Class<?> source;

        String name;

        List<String> paths = new ArrayList<>();

        List<Endpoint> endpoints = new ArrayList<>();
    }

    @Value
    static class Endpoint
    {

        @ToString.Exclude
        Method source;

        String name;

        List<RequestMethod> methods = new ArrayList<>();

        List<String> paths = new ArrayList<>();

        List<Parameter> parameters = new ArrayList<>();

        List<MimeType> consumes = new ArrayList<>();

        List<MimeType> produces = new ArrayList<>();

        List<Response> responses = new ArrayList<>();
    }

    @Value
    static class Parameter
    {

        public enum Location
        {
            PATH,
            QUERY,
            BODY
        }

        @ToString.Exclude
        AnnotatedElement source;

        String name;

        Location location;

        boolean required;

        Schema type;
    }

    @Value
    static class Response
    {
        HttpStatus status;

        Schema body;
        // TODO List headers
    }

    @Value
    @AllArgsConstructor
    static class Schema
    {

        @ToString.Exclude
        Class<?> source;

        /**
         * Empty unless this is a named "record" type that should be referenced
         * as a named schema in the generated OpenAPI document.
         */
        String name;

        @ToString.Exclude
        Type hint;

        /**
         * Is empty for primitive types
         */
        List<Field> fields = new ArrayList<>();

        public Schema( Class<?> source, Type hint )
        {
            this( source, Api.schemaName( source ), hint );
        }
    }

    public static final Schema STRING = new Schema( String.class, null );

    public static Schema ref( Class<?> to )
    {
        Schema ref = new Schema( Ref.class, to );
        ref.getFields().add( new Field( "id", STRING, true ) );
        return ref;
    }

    public static Schema refs( Class<?> to )
    {
        return new Schema( Ref[].class, to );
    }

    static String schemaName( Class<?> source )
    {
        String name = source.getName();
        if ( source.isPrimitive() || source.isEnum() || source.isArray() || name.startsWith( "java.lang" )
            || name.startsWith( "java.util" ) )
            return "";
        if ( !name.startsWith( "org.hisp.dhis." ) || name.contains( ".openapi." ) )
            return source.getSimpleName();
        return name.substring( name.lastIndexOf( '.', name.lastIndexOf( '.' ) - 1 ) + 1 ).replace( '$', '.' );
    }
}
