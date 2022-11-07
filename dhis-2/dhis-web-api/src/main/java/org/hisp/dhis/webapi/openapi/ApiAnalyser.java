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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableSet;

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
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/**
 * Given a set of controller {@link Class}es this creates a {@link Api} model
 * that describes all relevant {@link Api.Endpoint}s and {@link Api.Schema}s.
 *
 * @author Jan Bernitt
 */
class ApiAnalyser
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
                .map( ApiAnalyser::toClassName )
                .map( ApiAnalyser::toClass )
                .filter( c -> !Modifier.isAbstract( c.getModifiers() ) )
                .collect( toList() );
        }
        Set<String> roots = args.length < 2 ? Set.of() : Stream.of( args ).skip( 1 ).collect( toUnmodifiableSet() );
        Api api = describeApi( classes, roots );
        String doc = OpenApiGenerator.generate( api );
        if ( args.length >= 1 )
        {
            Files.writeString( Path.of( args[0] ), doc, StandardOpenOption.TRUNCATE_EXISTING );
        }
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

    public static Api describeApi( List<Class<?>> controllers, Set<String> roots )
    {
        Api api = new Api();
        api.getSchemas().put( String.class, Api.STRING );
        api.getSchemas().put( Api.Ref.class, Api.ref( null ) );
        Stream<Class<?>> scope = controllers.stream()
            .filter( ApiAnalyser::isControllerType );
        if ( !roots.isEmpty() )
        {
            Set<String> normalizedRoots = roots.stream()
                .map( path -> path.startsWith( "/" ) ? path : "/" + path )
                .collect( toUnmodifiableSet() );
            scope = scope.filter( c -> isRoot( c, normalizedRoots ) );
        }
        scope
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
        e.getPaths().addAll( List.of( mapping.getPath() ) );
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
                boolean requireJsonProperty = stream( p.getType().getMethods() )
                    .anyMatch( m -> m.isAnnotationPresent( JsonProperty.class ) );
                for ( Method m : p.getType().getMethods() )
                {
                    if ( isEndpointParameter( m, 1, List.of( "set" ), requireJsonProperty ) )
                    {
                        e.getParameters().add( describeParameter( api, m ) );
                    }
                }
            }
        }
        // - undeclared parameters (not found in signature)
        if ( source.isAnnotationPresent( OpenApi.ParamRepeat.class ) )
        {
            stream( source.getAnnotation( OpenApi.ParamRepeat.class ).value() )
                .forEach( p -> describeOpenApiParam( api, controller, e, p ) );
        }
        else if ( source.isAnnotationPresent( OpenApi.Param.class ) )
        {
            describeOpenApiParam( api, controller, e, source.getAnnotation( OpenApi.Param.class ) );
        }
        if ( source.isAnnotationPresent( OpenApi.ParamsRepeat.class ) )
        {
            stream( source.getAnnotation( OpenApi.ParamsRepeat.class ).value() )
                .forEach( p -> describeOpenApiParams( api, e, p ) );
        }
        else if ( source.isAnnotationPresent( OpenApi.Params.class ) )
        {
            describeOpenApiParams( api, e, source.getAnnotation( OpenApi.Params.class ) );
        }
        // response:
        stream( mapping.getProduces() ).map( MediaType::parseMediaType ).forEach( e.getProduces()::add );
        HttpStatus status = HttpStatus.OK;
        if ( source.isAnnotationPresent( ResponseStatus.class ) )
        {
            ResponseStatus a = source.getAnnotation( ResponseStatus.class );
            status = firstNonEqual( HttpStatus.INTERNAL_SERVER_ERROR, a.value(), a.code(), status );
        }
        Api.Schema body = describeBodySchema( api, source.getGenericReturnType() );
        e.getResponses().add( new Api.Response( status, body ) );
        return e;
    }

    private static void describeOpenApiParam( Api api, Api.Controller controller, Api.Endpoint endpoint,
        OpenApi.Param param )
    {
        String name = param.name();
        Class<?> value = param.value();
        if ( value == IdentifiableObject.class )
        {
            value = (Class<?>) ((ParameterizedType) controller.getSource().getGenericSuperclass())
                .getActualTypeArguments()[0];
        }
        List<Api.Parameter> params = endpoint.getParameters();
        if ( name.isEmpty() )
        {
            params.add(
                new Api.Parameter( endpoint.getSource(), "", Api.Parameter.In.BODY, true,
                    describeSchema( api, value ) ) );
        }
        else
        {
            params.add(
                new Api.Parameter( endpoint.getSource(), name, Api.Parameter.In.QUERY, false,
                    describeSchema( api, value ) ) );
        }
    }

    private static void describeOpenApiParams( Api api, Api.Endpoint endpoint, OpenApi.Params params )
    {
        Class<?> value = params.value();
        boolean requireJsonProperty = stream( value.getMethods() )
            .anyMatch( m -> m.isAnnotationPresent( JsonProperty.class ) );
        stream( value.getMethods() )
            .filter( m -> isEndpointParameter( m, 0, List.of( "has", "is", "get" ), requireJsonProperty ) )
            .map( m -> describeParameter( api, m ) )
            .forEach( endpoint.getParameters()::add );
    }

    private static Api.Parameter describeParameter( Api api, Parameter source )
    {
        Api.Schema schema = describeSchema( api, source.getType() );
        if ( source.isAnnotationPresent( PathVariable.class ) )
        {
            PathVariable a = source.getAnnotation( PathVariable.class );
            String name = firstNonEmpty( a.name(), a.value(), source.getName() );
            return new Api.Parameter( source, name, Api.Parameter.In.PATH, a.required(), schema );
        }
        if ( source.isAnnotationPresent( RequestParam.class ) )
        {
            RequestParam a = source.getAnnotation( RequestParam.class );
            boolean required = a.required()
                && a.defaultValue().equals( "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n" );
            String name = firstNonEmpty( a.name(), a.value(), source.getName() );
            return new Api.Parameter( source, name, Api.Parameter.In.QUERY, required, schema );
        }
        if ( source.isAnnotationPresent( RequestBody.class ) )
        {
            RequestBody a = source.getAnnotation( RequestBody.class );
            return new Api.Parameter( source, "", Api.Parameter.In.BODY, a.required(), schema );
        }
        throw new UnsupportedOperationException( "not yet" );
    }

    private static Api.Parameter describeParameter( Api api, Method source )
    {
        String name = getName( source );
        Api.Schema type = describeSchema( api, source.getParameterCount() == 0
            ? source.getReturnType()
            : source.getParameterTypes()[0] );
        return new Api.Parameter( source, name, Api.Parameter.In.QUERY, false, type );
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
        return api.getSchemas().computeIfAbsent( source, key -> {
            Api.Schema schema = new Api.Schema( source, null );
            currentlyResolved.put( source, schema );
            if ( source.isEnum() || source.isInterface() )
                return schema;
            if ( source.isArray() )
            {
                describeSchema( api, source.getComponentType(), currentlyResolved );
                return schema;
            }
            Set<String> fieldsAdded = new HashSet<>();
            for ( Method m : source.getMethods() )
            {
                if ( isSchemaField( m ) && !fieldsAdded.contains( getName( m ) ) )
                {
                    Type type = m.getParameterCount() == 1 ? m.getGenericParameterTypes()[0] : m.getGenericReturnType();
                    Api.Schema ms = describeCustomSchema( api, type, currentlyResolved );
                    if ( ms != null )
                    {
                        String name = getName( m );
                        fieldsAdded.add( name );
                        schema.getFields().add( new Api.Field( name, ms, getFieldRequired( m ) ) );
                    }
                }
            }
            for ( Field f : source.getDeclaredFields() )
            {
                if ( isSchemaField( f ) && !fieldsAdded.contains( getName( f ) ) )
                {
                    Api.Schema fs = describeCustomSchema( api, f.getGenericType(), currentlyResolved );
                    if ( fs != null )
                    {
                        String name = getName( f );
                        fieldsAdded.add( name );
                        schema.getFields().add( new Api.Field( name, fs, getFieldRequired( f ) ) );
                    }
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
                return new Api.Schema( JsonNode.class, "", source );
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

    private static String getPropertyName( Method method )
    {
        String name = method.getName();
        String prop = name.substring( name.startsWith( "is" ) ? 2 : 3 );
        return Character.toLowerCase( prop.charAt( 0 ) ) + prop.substring( 1 );
    }

    private static <T extends Member & AnnotatedElement> String getName( T member )
    {
        String name = member instanceof Field ? member.getName() : getPropertyName( (Method) member );
        JsonProperty property = member.getAnnotation( JsonProperty.class );
        String customName = property == null ? "" : property.value();
        return customName.isEmpty() ? name : customName;
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

    private static boolean isEndpointParameter( Method source, int parameterCount, List<String> prefixes,
        boolean requireJsonProperty )
    {
        return prefixes.stream().anyMatch( prefix -> source.getName().startsWith( prefix ) )
            && source.getParameterCount() == parameterCount
            && Modifier.isPublic( source.getModifiers() )
            && source.getDeclaringClass() != Object.class
            && (!requireJsonProperty || source.isAnnotationPresent( JsonProperty.class ));
    }

    private static boolean isRoot( Class<?> controller, Set<String> expected )
    {
        RequestMapping a = controller.getAnnotation( RequestMapping.class );
        return a != null && stream( firstNonEmpty( a.value(), a.path() ) ).anyMatch( expected::contains );
    }

    private static Mapping getMapping( Method source )
    {
        if ( source.isAnnotationPresent( RequestMapping.class ) )
        {
            RequestMapping a = source.getAnnotation( RequestMapping.class );
            return new Mapping( a.name(), firstNonEmpty( a.value(), a.path() ), a.method(), a.params(), a.headers(),
                a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( GetMapping.class ) )
        {
            GetMapping a = source.getAnnotation( GetMapping.class );
            return new Mapping( a.name(), firstNonEmpty( a.value(), a.path() ),
                new RequestMethod[] { RequestMethod.GET }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( PutMapping.class ) )
        {
            PutMapping a = source.getAnnotation( PutMapping.class );
            return new Mapping( a.name(), firstNonEmpty( a.value(), a.path() ),
                new RequestMethod[] { RequestMethod.PUT }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( PostMapping.class ) )
        {
            PostMapping a = source.getAnnotation( PostMapping.class );
            return new Mapping( a.name(), firstNonEmpty( a.value(), a.path() ),
                new RequestMethod[] { RequestMethod.POST }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( PatchMapping.class ) )
        {
            PatchMapping a = source.getAnnotation( PatchMapping.class );
            return new Mapping( a.name(), firstNonEmpty( a.value(), a.path() ),
                new RequestMethod[] { RequestMethod.PATCH }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        return null;
    }

    private static String[] firstNonEmpty( String[] a, String[] b )
    {
        return a.length == 0 ? b : a;
    }

    private static String firstNonEmpty( String a, String b )
    {
        return a.length() == 0 ? b : a;
    }

    private static String firstNonEmpty( String a, String b, String c )
    {
        String ab = firstNonEmpty( a, b );
        return ab.length() > 0 ? ab : c;
    }

    private static <E extends Enum<E>> E firstNonEqual( E to, E... samples )
    {
        return stream( samples ).filter( e -> e != to ).findFirst().orElse( samples[0] );
    }

    @Value
    static class Mapping
    {
        String name;

        String[] path;

        RequestMethod[] method;

        String[] params;

        String[] headers;

        String[] consumes;

        String[] produces;
    }
}
