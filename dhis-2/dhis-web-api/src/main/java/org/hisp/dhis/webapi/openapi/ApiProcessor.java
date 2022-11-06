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

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.Value;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

class ApiProcessor
{
    public static void main( String[] args )
        throws Exception
    {
        Path path = Path
            .of( AbstractCrudController.class.getProtectionDomain().getCodeSource().getLocation().getPath() );
        List<Class<?>> classes;
        try ( Stream<Path> files = Files.walk( path ) )
        {
            classes = files
                .filter( f -> f.getFileName().toString().endsWith( "Controller.class" ) )
                .map( ApiProcessor::toClassName )
                .map( ApiProcessor::toClass )
                .filter( c -> !Modifier.isAbstract( c.getModifiers() ) )
                .collect( toList() );
        }
        Api api = describeApi( classes );
        System.out.println( OpenApiGenerator.generate( api ) );
        System.out.println(
            format( "%d controllers, %d schemas", api.getControllers().size(), api.getSchemas().size() ) );
    }

    private static String toClassName( Path f )
    {
        return f.toString().substring( f.toString().indexOf( "org/hisp/" ) ).replace( ".class", "" )
            .replace( '/', '.' );
    }

    private static Class<?> toClass( String className )
    {
        try
        {
            return Class.forName( className );
        }
        catch ( Exception ex )
        {
            throw new IllegalArgumentException( "failed loading: " + className, ex );
        }
    }

    public static Api describeApi( List<Class<?>> controllers )
    {
        Api api = new Api();
        api.getSchemas().put( String.class.getSimpleName(), Api.STRING );
        controllers.stream()
            .filter( ApiProcessor::isControllerType )
            .forEach( source -> api.getControllers().add( describeController( api, source ) ) );
        return api;
    }

    private static Api.Controller describeController( Api api, Class<?> source )
    {
        RequestMapping rm = source.getAnnotation( RequestMapping.class );
        String name = rm != null && !rm.name().isEmpty() ? rm.name()
            : source.getSimpleName().replace( "Controller", "" );
        Api.Controller controller = new Api.Controller( source, name );
        if ( rm != null )
            controller.getPaths().addAll( List.of( rm.value() ) );
        for ( Method m : source.getMethods() )
        {
            Mapping mapping = getMapping( m );
            if ( mapping != null )
            {
                controller.getEndpoints().add( describeEndpoint( api, controller, m, mapping ) );
            }
        }
        return controller;
    }

    private static Api.Endpoint describeEndpoint( Api api, Api.Controller controller, Method source, Mapping mapping )
    {
        String name = mapping.getName().isEmpty() ? source.getName() : mapping.getName();

        Api.Endpoint e = new Api.Endpoint( source, name );

        // request:
        e.getPaths().addAll( List.of( mapping.getValue() ) );
        e.getMethods().addAll( List.of( mapping.method ) );
        stream( mapping.getConsumes() ).map( MediaType::parseMediaType ).forEach( e.getConsumes()::add );
        // - declared parameters
        for ( Parameter p : source.getParameters() )
        {
            if ( isSimpleEndpointParameter( p ) )
            {
                e.getParameters().add( describeParameter( api, p ) );
            }
            else if ( isComplexEndpointParameter( p ) )
            {
                for ( Method m : p.getType().getMethods() )
                {
                    if ( isEndpointParameter( m, 1, List.of( "set" ) ) )
                    {
                        e.getParameters().add( describeParameter( api, m ) );
                    }
                }
            }
        }
        // - undeclared parameters (not found in signature)
        if ( source.isAnnotationPresent( UndeclaredParameters.class ) )
        {
            stream( source.getAnnotation( UndeclaredParameters.class ).value() )
                .forEach( p -> describeUndeclaredParameters( api, controller, e, p ) );
        }
        // response:
        stream( mapping.getProduces() ).map( MediaType::parseMediaType ).forEach( e.getProduces()::add );
        HttpStatus status = HttpStatus.OK;
        if ( source.isAnnotationPresent( ResponseStatus.class ) )
            status = source.getAnnotation( ResponseStatus.class ).value();
        Api.Schema body = describeBodySchema( api, source.getGenericReturnType() );
        e.getResponses().add( new Api.Response( status, body ) );
        return e;
    }

    private static void describeUndeclaredParameters( Api api, Api.Controller controller, Api.Endpoint endpoint,
        UndeclaredParam param )
    {
        String name = param.name();
        Class<?> value = param.value();
        if ( value == IdentifiableObject.class )
        {
            value = (Class<?>) ((ParameterizedType) controller.getSource().getGenericSuperclass())
                .getActualTypeArguments()[0];
        }
        List<Api.Parameter> params = endpoint.getParameters();
        if ( name.isEmpty() || "{}".equals( name ) )
        {
            params.add(
                new Api.Parameter( endpoint.getSource(), "", Api.Parameter.Location.BODY, true,
                    describeSchema( api, value ) ) );
        }
        else if ( name.startsWith( "{" ) && name.endsWith( "}" ) )
        {
            stream( value.getMethods() )
                .filter( m -> isEndpointParameter( m, 0, List.of( "has", "is", "get" ) ) )
                .map( m -> describeParameter( api, m ) )
                .forEach( params::add );
        }
        else
        {
            params.add(
                new Api.Parameter( endpoint.getSource(), name, Api.Parameter.Location.QUERY, false,
                    describeSchema( api, value ) ) );
        }
    }

    private static Api.Parameter describeParameter( Api api, Parameter source )
    {
        Api.Schema schema = describeSchema( api, source.getType() );
        if ( source.isAnnotationPresent( PathVariable.class ) )
        {
            PathVariable a = source.getAnnotation( PathVariable.class );
            return new Api.Parameter( source, a.name().isEmpty() ? source.getName() : a.name(),
                Api.Parameter.Location.PATH, a.required(), schema );
        }
        if ( source.isAnnotationPresent( RequestParam.class ) )
        {
            RequestParam a = source.getAnnotation( RequestParam.class );
            boolean required = a.required()
                && a.defaultValue().equals( "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n" );
            return new Api.Parameter( source, a.name().isEmpty() ? source.getName() : a.name(),
                Api.Parameter.Location.QUERY,
                required, schema );
        }
        if ( source.isAnnotationPresent( RequestBody.class ) )
        {
            RequestBody a = source.getAnnotation( RequestBody.class );
            return new Api.Parameter( source, "", Api.Parameter.Location.BODY, a.required(), schema );
        }
        throw new UnsupportedOperationException( "not yet" );
    }

    private static Api.Parameter describeParameter( Api api, Method source )
    {
        String name = getProperty( source.getName() );
        Api.Schema type = describeSchema( api, source.getParameterCount() == 0
            ? source.getReturnType()
            : source.getParameterTypes()[0] );
        return new Api.Parameter( source, name, Api.Parameter.Location.QUERY, false, type );
    }

    private static Api.Schema describeSchema( Api api, Class<?> source )
    {
        return describeSchema( api, source, new IdentityHashMap<>() );
    }

    private static Api.Schema describeSchema( Api api, Class<?> source, Map<Class<?>, Api.Schema> currentlyResolved )
    {
        Api.Schema s = currentlyResolved.get( source );
        if ( s != null )
            return s;
        return api.getSchemas().computeIfAbsent( source.getName(), key -> {
            Api.Schema schema = new Api.Schema( source, null );
            currentlyResolved.put( source, schema );
            if ( source.isEnum() || source.isInterface() )
                return schema;
            if ( source.isArray() )
            {
                describeSchema( api, source.getComponentType(), currentlyResolved );
                return schema;
            }
            for ( Method m : source.getMethods() )
            {
                if ( isSchemaField( m ) )
                {
                    Type type = m.getParameterCount() == 1 ? m.getGenericParameterTypes()[0] : m.getGenericReturnType();
                    Api.Schema ms = describeCustomSchema( api, type, currentlyResolved );
                    if ( ms != null )
                        schema.getFields().add( new Api.Field( getProperty( m ), ms, getFieldRequired( m ) ) );
                }
            }
            for ( Field f : source.getDeclaredFields() )
            {
                if ( isSchemaField( f )
                    && schema.getFields().stream().noneMatch( field -> field.getName().equals( f.getName() ) ) )
                {
                    Api.Schema fs = describeCustomSchema( api, f.getGenericType(), currentlyResolved );
                    if ( fs != null )
                        schema.getFields().add( new Api.Field( getProperty( f ), fs, getFieldRequired( f ) ) );
                }
            }
            return schema;
        } );
    }

    private static Api.Schema describeBodySchema( Api api, Type source )
    {
        if ( source instanceof Class<?> )
        {
            Class<?> cls = (Class<?>) source;
            if ( JsonNode.class.isAssignableFrom( cls ) )
            {
                return new Api.Schema( JsonNode.class, "JSON", source );
            }
            return describeSchema( api, cls );
        }
        if ( source instanceof ParameterizedType )
        {
            ParameterizedType pt = (ParameterizedType) source;
            Type rawType = pt.getRawType();
            if ( rawType == ResponseEntity.class )
            {
                return describeBodySchema( api, pt.getActualTypeArguments()[0] );
            }
            if ( rawType == StreamingJsonRoot.class )
            {
                return new Api.Schema( StreamingJsonRoot.class, "", pt.getActualTypeArguments()[0] );
            }
            return describeCustomSchema( api, source, new IdentityHashMap<>() );
        }
        if ( source instanceof WildcardType )
        {
            WildcardType wt = (WildcardType) source;
            return describeBodySchema( api, wt.getUpperBounds()[0] );
        }
        return null;
    }

    private static Api.Schema describeCustomSchema( Api api, Type source, Map<Class<?>, Api.Schema> currentlyResolved )
    {
        if ( source instanceof ParameterizedType )
        {
            ParameterizedType pt = (ParameterizedType) source;
            Class<?> rawType = (Class<?>) pt.getRawType();
            if ( rawType == Class.class )
                return new Api.Schema( String.class, "", source );
            if ( !rawType.isInterface() )
                return null; // give up
            if ( Collection.class.isAssignableFrom( rawType ) || rawType == Iterable.class )
            {
                Type itemType = pt.getActualTypeArguments()[0];
                if ( itemType instanceof Class<?> )
                    return describeCustomSchema( api, Array.newInstance( (Class<?>) itemType, 0 ).getClass(),
                        currentlyResolved );
                Api.Schema colSchema = new Api.Schema( Collection.class, source );
                colSchema.getFields()
                    .add( new Api.Field( "", describeCustomSchema( api, itemType, currentlyResolved ), true ) );
                return colSchema;
            }
            if ( Map.class.isAssignableFrom( rawType ) )
            {
                Api.Schema mapSchema = new Api.Schema( Map.class, source );
                mapSchema.getFields().add( new Api.Field( "key",
                    describeCustomSchema( api, pt.getActualTypeArguments()[0], currentlyResolved ), true ) );
                mapSchema.getFields().add( new Api.Field( "value",
                    describeCustomSchema( api, pt.getActualTypeArguments()[1], currentlyResolved ), true ) );
                return mapSchema;
            }
        }
        else if ( source instanceof Class<?> )
        {
            Class<?> type = (Class<?>) source;
            if ( IdentifiableObject.class.isAssignableFrom( type ) )
                return Api.ref( type );
            if ( IdentifiableObject[].class.isAssignableFrom( type ) )
                return Api.refs( type.getComponentType() );
            if ( Object[].class.isAssignableFrom( type ) )
            {
                Class<?> comp = type.getComponentType();
                while ( Object[].class.isAssignableFrom( comp ) )
                    comp = comp.getComponentType();
                // if the type is an array type make sure the element type is
                // also added
                describeSchema( api, comp, currentlyResolved );
            }
            return describeSchema( api, type, currentlyResolved );
        }
        return null;
    }

    private static Boolean getFieldRequired( AnnotatedElement source )
    {
        JsonProperty a = source.getAnnotation( JsonProperty.class );
        if ( a.required() )
            return true;
        if ( !a.defaultValue().isEmpty() )
            return false;
        return null;
    }

    private static <T extends Member & AnnotatedElement> String getProperty( T member )
    {
        String name = member instanceof Field ? member.getName() : getProperty( member.getName() );
        String customName = member.getAnnotation( JsonProperty.class ).value();
        return customName.isEmpty() ? name : customName;
    }

    private static String getProperty( String name )
    {
        String prop = name.substring( name.startsWith( "is" ) ? 2 : 3 );
        return Character.toLowerCase( prop.charAt( 0 ) ) + prop.substring( 1 );
    }

    private static <T extends AnnotatedElement & Member> boolean isSchemaField( T member )
    {
        return member.isAnnotationPresent( JsonProperty.class ) && !Modifier.isStatic( member.getModifiers() );
    }

    private static boolean isControllerType( Class<?> source )
    {
        return source.isAnnotationPresent( RestController.class )
            || source.isAnnotationPresent( Controller.class );
    }

    private static boolean isComplexEndpointParameter( Parameter source )
    {
        Class<?> type = source.getType();
        if ( type.isInterface()
            || type.isEnum()
            || IdentifiableObject.class.isAssignableFrom( type )
            || source.getAnnotations().length > 0 )
            return false;
        return stream( type.getDeclaredConstructors() ).anyMatch( c -> c.getParameterCount() == 0 );
    }

    private static boolean isSimpleEndpointParameter( Parameter source )
    {
        if ( source.getType() == Map.class )
            return false;
        return source.isAnnotationPresent( PathVariable.class )
            || source.isAnnotationPresent( RequestParam.class )
            || source.isAnnotationPresent( RequestBody.class );
    }

    private static boolean isEndpointParameter( Method source, int parameterCount, List<String> prefixes )
    {
        return prefixes.stream().anyMatch( prefix -> source.getName().substring( 0, prefix.length() ).equals( prefix ) )
            && source.getParameterCount() == parameterCount
            && Modifier.isPublic( source.getModifiers() )
            && source.getDeclaringClass() != Object.class;
    }

    private static Mapping getMapping( Method source )
    {
        if ( source.isAnnotationPresent( RequestMapping.class ) )
        {
            RequestMapping rm = source.getAnnotation( RequestMapping.class );
            return new Mapping( rm.name(), rm.value(), rm.method(), rm.params(), rm.headers(), rm.consumes(),
                rm.produces() );
        }
        if ( source.isAnnotationPresent( GetMapping.class ) )
        {
            GetMapping gm = source.getAnnotation( GetMapping.class );
            return new Mapping( gm.name(), gm.value(), new RequestMethod[] { RequestMethod.GET }, gm.params(),
                gm.headers(), gm.consumes(), gm.produces() );
        }
        if ( source.isAnnotationPresent( PutMapping.class ) )
        {
            PutMapping pm = source.getAnnotation( PutMapping.class );
            return new Mapping( pm.name(), pm.value(), new RequestMethod[] { RequestMethod.PUT }, pm.params(),
                pm.headers(), pm.consumes(), pm.produces() );
        }
        if ( source.isAnnotationPresent( PostMapping.class ) )
        {
            PostMapping pm = source.getAnnotation( PostMapping.class );
            return new Mapping( pm.name(), pm.value(), new RequestMethod[] { RequestMethod.POST }, pm.params(),
                pm.headers(), pm.consumes(), pm.produces() );
        }
        if ( source.isAnnotationPresent( PatchMapping.class ) )
        {
            PatchMapping pm = source.getAnnotation( PatchMapping.class );
            return new Mapping( pm.name(), pm.value(), new RequestMethod[] { RequestMethod.PATCH }, pm.params(),
                pm.headers(), pm.consumes(), pm.produces() );
        }
        return null;
    }

    @Value
    static class Mapping
    {
        String name;

        String[] value;

        RequestMethod[] method;

        String[] params;

        String[] headers;

        String[] consumes;

        String[] produces;
    }
}
