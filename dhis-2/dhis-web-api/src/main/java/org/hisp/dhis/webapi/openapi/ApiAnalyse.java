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

import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.webapi.openapi.Property.getProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * Given a set of controller {@link Class}es this creates a {@link Api} model
 * that describes all relevant {@link Api.Endpoint}s and {@link Api.Schema}s.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
final class ApiAnalyse
{
    /**
     * The included classes can be filtered based on REST API resource path or
     * {@link OpenApi.Tags} present on the controller class level. Method level
     * path and tags will not be considered for this filter.
     *
     * @param controllers controllers all potential controllers
     * @param paths filter based on resource path (empty includes all)
     * @param tags filter based on tags (empty includes all)
     */
    record Scope( Set<Class<?>> controllers, Set<String> paths, Set<String> tags )
    {
    }

    private static final Map<Class<?>, Api.SchemaGenerator> GENERATORS = new ConcurrentHashMap<>();

    public static void register( Class<?> type, Api.SchemaGenerator generator )
    {
        GENERATORS.put( type, generator );
    }

    static
    {
        register( UID.class, SchemaGenerators.UID );
        register( org.hisp.dhis.webapi.common.UID.class, SchemaGenerators.UID );
        register( Api.PropertyNames.class, SchemaGenerators.PROPERTY_NAMES );
    }

    /**
     * A mapping annotation might have an empty array for the paths which is
     * identical to just the root path o the controller. This array is used to
     * reflect the presence of this root path as the path mapped.
     */
    private static final String[] ROOT_PATH = { "" };

    /**
     * Create an {@link Api} model from controller {@link Class}.
     *
     * @return the {@link Api} for all controllers matching both of the filters
     */
    public static Api analyseApi( Scope scope )
    {
        Api api = new Api();
        Stream<Class<?>> inScope = scope.controllers.stream().filter( ApiAnalyse::isControllerType );
        Set<String> paths = scope.paths;
        if ( paths != null && !paths.isEmpty() )
        {
            inScope = inScope.filter( c -> isRootPath( c, paths ) );
        }
        Set<String> tags = scope.tags;
        if ( tags != null && !tags.isEmpty() )
        {
            inScope = inScope.filter( c -> c.isAnnotationPresent( OpenApi.Tags.class )
                && Stream.of( c.getAnnotation( OpenApi.Tags.class ).value() ).anyMatch( tags::contains ) );
        }
        inScope.forEach( source -> api.getControllers().add( analyseController( api, source ) ) );
        return api;
    }

    private static Api.Controller analyseController( Api api, Class<?> source )
    {
        String name = getAnnotated( source, RequestMapping.class, RequestMapping::name, n -> !n.isEmpty(),
            () -> source.getSimpleName().replace( "Controller", "" ) );
        Class<?> entityClass = source.isAnnotationPresent( OpenApi.EntityType.class )
            ? source.getAnnotation( OpenApi.EntityType.class ).value()
            : null;
        if ( entityClass == OpenApi.EntityType.class )
        {
            entityClass = (Class<?>) (((ParameterizedType) source.getGenericSuperclass()).getActualTypeArguments()[0]);
        }
        Api.Controller controller = new Api.Controller( api, source, entityClass, name );
        whenAnnotated( source, RequestMapping.class, a -> controller.getPaths().addAll( List.of( a.value() ) ) );
        whenAnnotated( source, OpenApi.Tags.class, a -> controller.getTags().addAll( List.of( a.value() ) ) );

        methodsIn( source )
            .map( ApiAnalyse::getMapping )
            .filter( Objects::nonNull )
            .map( mapping -> analyseEndpoint( controller, mapping ) )
            .forEach( endpoint -> controller.getEndpoints().add( endpoint ) );

        return controller;
    }

    private static Stream<Method> methodsIn( Class<?> source )
    {
        return source == null || source == Object.class
            ? Stream.empty()
            : Stream.concat( stream( source.getDeclaredMethods() ), methodsIn( source.getSuperclass() ) );
    }

    private static Api.Endpoint analyseEndpoint( Api.Controller controller, EndpointMapping mapping )
    {
        Method source = mapping.source();
        String name = mapping.name().isEmpty() ? source.getName() : mapping.name();
        Class<?> entityClass = getAnnotated( source, OpenApi.EntityType.class, OpenApi.EntityType::value,
            c -> c != OpenApi.EntityType.class,
            controller::getEntityClass );

        // request media types
        Set<MediaType> consumes = stream( mapping.consumes() ).map( MediaType::parseMediaType ).collect( toSet() );
        if ( consumes.isEmpty() )
        {
            // assume JSON if nothing is set explicitly
            consumes.add( MediaType.APPLICATION_JSON );
        }

        Api.Endpoint endpoint = new Api.Endpoint( controller, source, entityClass, name,
            ConsistentAnnotatedElement.of( source ).isAnnotationPresent( Deprecated.class ) ? Boolean.TRUE : null );

        whenAnnotated( source, OpenApi.Tags.class, a -> endpoint.getTags().addAll( List.of( a.value() ) ) );
        Stream.of( mapping.path() )
            .map( path -> path.endsWith( "/" ) ? path.substring( 0, path.length() - 1 ) : path )
            .forEach( path -> endpoint.getPaths().add( path ) );
        endpoint.getMethods().addAll( Set.of( mapping.method ) );

        // request:
        analyseParameters( endpoint, consumes );

        // response:
        endpoint.getResponses().putAll( analyseResponses( endpoint, mapping, consumes ) );

        return endpoint;
    }

    private static Map<HttpStatus, Api.Response> analyseResponses( Api.Endpoint endpoint, EndpointMapping mapping,
        Set<MediaType> consumes )
    {
        Method source = mapping.source();
        Set<MediaType> produces = stream( mapping.produces() )
            .map( MediaType::parseMediaType )
            .collect( toSet() );
        if ( produces.isEmpty() )
        {
            // either make symmetric or assume JSON as standard
            if ( consumes.contains( MediaType.APPLICATION_JSON ) || consumes.contains( MediaType.APPLICATION_XML ) )
            {
                produces.addAll( consumes ); // make symmetric
            }
            else
            {
                produces.add( MediaType.APPLICATION_JSON );
            }
        }

        HttpStatus signatureStatus = getAnnotated( source, ResponseStatus.class,
            a -> firstNonEqual( HttpStatus.INTERNAL_SERVER_ERROR, a.value(), a.code() ),
            s -> s != HttpStatus.INTERNAL_SERVER_ERROR, () -> HttpStatus.OK );

        Map<HttpStatus, Api.Response> res = new LinkedHashMap<>();
        // response(s) declared via annotation(s)
        getAnnotations( source, OpenApi.Response.class ).forEach( a -> res.putAll(
            analyseResponses( endpoint, a, produces, List.of( signatureStatus ), source.getGenericReturnType() ) ) );
        // response from method signature
        Class<?> type = source.getReturnType();
        res.computeIfAbsent( signatureStatus, status -> {
            Api.Response response = new Api.Response( status );
            if ( type != void.class && type != Void.class && type != ModelAndView.class )
            {
                response.add( produces, analyseResponseSchema( endpoint, source.getGenericReturnType() ) );
            }
            return response;
        } );
        // error response(s) from annotated exception types in method signature and
        // error response(s) from annotations on exceptions in method signature
        Stream.concat( stream( source.getExceptionTypes() ), stream( source.getAnnotatedExceptionTypes() ) )
            .map( ex -> ex.getAnnotationsByType( OpenApi.Response.class ) )
            .flatMap( Stream::of )
            .forEach( a -> res.putAll( analyseResponses( endpoint, a, produces, List.of(), null ) ) );

        return res;
    }

    private static Map<HttpStatus, Api.Response> analyseResponses( Api.Endpoint endpoint, OpenApi.Response response,
        Set<MediaType> defaultProduces, List<HttpStatus> defaultStatuses, Type defaultResponseType )
    {
        List<HttpStatus> statuses = response.status().length == 0
            ? defaultStatuses
            : stream( response.status() ).map( s -> HttpStatus.resolve( s.getCode() ) ).toList();
        Set<Api.Header> headers = stream( response.headers() )
            .map( header -> new Api.Header( header.name(), header.description(),
                analyseParamSchema( endpoint, null, response.value() ) ) )
            .collect( toSet() );
        Set<MediaType> produces = response.mediaTypes().length == 0
            ? defaultProduces
            : stream( response.mediaTypes() ).map( MediaType::valueOf ).collect( toUnmodifiableSet() );
        return statuses.stream().collect( toMap( Function.identity(), status -> new Api.Response( status )
            .add( produces, analyseResponseSchema( endpoint, defaultResponseType, response.value() ) )
            .add( headers ) ) );
    }

    private static void analyseParameters( Api.Endpoint endpoint, Set<MediaType> consumes )
    {
        Method source = endpoint.getSource();
        // request parameter(s) declared via annotation(s)
        getAnnotations( source, OpenApi.Param.class ).forEach( p -> analyseParam( endpoint, p, consumes ) );
        getAnnotations( source, OpenApi.Params.class ).forEach( p -> analyseParams( endpoint, p ) );

        // request parameters from method signature
        for ( Parameter p : source.getParameters() )
        {
            if ( p.isAnnotationPresent( OpenApi.Ignore.class ) )
            {
                continue;
            }
            if ( p.isAnnotationPresent( OpenApi.Param.class ) )
            {
                OpenApi.Param a = p.getAnnotation( OpenApi.Param.class );
                EndpointParam param = getParam( p );
                String name = firstNonEmpty( a.name(), param == null ? "" : param.name(), p.getName() );
                Api.Parameter.In in = param == null ? Api.Parameter.In.QUERY : param.in();
                boolean required = param == null ? a.required() : param.required();
                Api.Schema type = analyseParamSchema( endpoint, p.getParameterizedType(), a.value() );
                if ( in != Api.Parameter.In.BODY )
                {
                    endpoint.getParameters().computeIfAbsent( name,
                        key -> new Api.Parameter( p, key, in, required, type ) );
                }
                else
                {
                    Api.RequestBody requestBody = endpoint.getRequestBody()
                        .init( () -> new Api.RequestBody( p, required ) );
                    consumes.forEach( mediaType -> requestBody.getConsumes().putIfAbsent( mediaType, type ) );
                }
            }
            else if ( p.isAnnotationPresent( PathVariable.class ) )
            {
                PathVariable a = p.getAnnotation( PathVariable.class );
                String name = firstNonEmpty( a.name(), a.value(), p.getName() );
                endpoint.getParameters().computeIfAbsent( name,
                    key -> new Api.Parameter( p, key, Api.Parameter.In.PATH, a.required(),
                        analyseInputSchema( endpoint, p.getParameterizedType() ) ) );
            }
            else if ( p.isAnnotationPresent( RequestParam.class ) && p.getType() != Map.class )
            {
                RequestParam a = p.getAnnotation( RequestParam.class );
                String name = firstNonEmpty( a.name(), a.value(), p.getName() );
                endpoint.getParameters().computeIfAbsent( name,
                    key -> new Api.Parameter( p, key, Api.Parameter.In.QUERY, a.required(),
                        analyseInputSchema( endpoint, p.getParameterizedType() ) ) );
            }
            else if ( p.isAnnotationPresent( RequestBody.class ) )
            {
                RequestBody a = p.getAnnotation( RequestBody.class );
                Api.RequestBody requestBody = endpoint.getRequestBody()
                    .init( () -> new Api.RequestBody( p, a.required() ) );
                Api.Schema type = analyseParamSchema( endpoint, p.getParameterizedType() );
                consumes.forEach( mediaType -> requestBody.getConsumes().putIfAbsent( mediaType, type ) );
            }
            else if ( isParams( p ) )
            {
                analyseParams( endpoint, p.getType() );
            }
        }
    }

    private static void analyseParam( Api.Endpoint endpoint, OpenApi.Param param, Set<MediaType> consumes )
    {
        String name = param.name();
        Api.Schema type = analyseParamSchema( endpoint, null, param.value() );
        Api.Schema wrapped = param.asProperty().isEmpty()
            ? type
            : new Api.Schema( Api.Schema.Type.OBJECT, false, Object.class, Object.class )
                .add( new Api.Property( param.asProperty(), true, type ) );
        boolean required = param.required();
        if ( name.isEmpty() )
        {
            Api.RequestBody requestBody = new Api.RequestBody( endpoint.getSource(), required );
            consumes.forEach( mediaType -> requestBody.getConsumes().put( mediaType, wrapped ) );
            endpoint.getRequestBody().setValue( requestBody );
            return;
        }
        endpoint.getParameters().put( name,
            new Api.Parameter( endpoint.getSource(), name, Api.Parameter.In.QUERY, required, wrapped ) );
    }

    private static void analyseParams( Api.Endpoint endpoint, OpenApi.Params params )
    {
        analyseParams( endpoint, params.value() );
    }

    private static void analyseParams( Api.Endpoint endpoint, Class<?> paramsObject )
    {
        Collection<Property> properties = getProperties( paramsObject );
        if ( isSharable( paramsObject, false ) )
        {
            OpenApi.Shared shared = paramsObject.getAnnotation( OpenApi.Shared.class );
            String sharedName = shared.name().isEmpty() ? null : shared.name();
            Api api = endpoint.getIn().getIn();
            Map<Class<?>, List<Api.Parameter>> sharedParameters = api.getComponents().getParameters();
            properties.forEach( property -> {
                Api.Parameter parameter = analyseParameter( endpoint, property );
                parameter.getSharedName().setValue( sharedName );
                sharedParameters.computeIfAbsent( paramsObject, e -> new ArrayList<>() ).add( parameter );
                endpoint.getParameters().put( parameter.getName(), parameter );
            } );
        }
        else
        {
            properties.forEach( property -> endpoint.getParameters()
                .computeIfAbsent( property.getName(), name -> analyseParameter( endpoint, property ) ) );
        }
    }

    private static Api.Parameter analyseParameter( Api.Endpoint endpoint, Property property )
    {
        AnnotatedElement member = (AnnotatedElement) property.getSource();
        Type type = property.getType();
        Api.Schema schema = type instanceof Class && isGeneratorType( (Class<?>) type )
            ? analyseGeneratorSchema( endpoint, type, member.getAnnotation( OpenApi.Property.class ).value() )
            : analyseInputSchema( endpoint, getSubstitutedType( endpoint, property, member ) );
        return new Api.Parameter( member, property.getName(), Api.Parameter.In.QUERY, false, schema );
    }

    private static Api.Schema analyseParamSchema( Api.Endpoint endpoint, Type source, Class<?>... oneOf )
    {
        if ( oneOf.length == 0 && source != null )
        {
            return analyseInputSchema( endpoint, source );
        }
        if ( isGeneratorType( oneOf[0] ) )
        {
            return analyseGeneratorSchema( endpoint, source, oneOf );
        }
        return Api.Schema.oneOf( List.of( oneOf ),
            type -> analyseInputSchema( endpoint, getSubstitutedType( endpoint, type ) ) );
    }

    private static Api.Schema analyseResponseSchema( Api.Endpoint endpoint, Type source, Class<?>... oneOf )
    {
        if ( oneOf.length == 0 && source != null )
        {
            return analyseOutputSchema( endpoint, source );
        }
        if ( isGeneratorType( oneOf[0] ) )
        {
            return analyseGeneratorSchema( endpoint, source, oneOf );
        }
        return Api.Schema.oneOf( List.of( oneOf ),
            type -> analyseOutputSchema( endpoint, getSubstitutedType( endpoint, type ) ) );
    }

    private static boolean isGeneratorType( Class<?> type )
    {
        return Api.SchemaGenerator.class.isAssignableFrom( type )
            || Api.SchemaGenerator[].class.isAssignableFrom( type )
            || GENERATORS.containsKey( type );
    }

    private static Api.Schema analyseGeneratorSchema( Api.Endpoint endpoint, Type source, Class<?>... oneOf )
    {
        Class<?> type = oneOf[0];
        Class<?> elementType = Object[].class.isAssignableFrom( type ) ? type.getComponentType() : type;
        Api.Schema schema = newGenerator( elementType ).generate( endpoint, source,
            copyOfRange( oneOf, 1, oneOf.length ) );
        return type == elementType
            ? schema
            : new Api.Schema( Api.Schema.Type.ARRAY, false, type, type ).withElements( schema );
    }

    private static Api.Schema analyseInputSchema( Api.Endpoint endpoint, Type source )
    {
        return analyseTypeSchema( endpoint, source, false, new IdentityHashMap<>() );
    }

    private static Api.Schema analyseOutputSchema( Api.Endpoint endpoint, Type source )
    {
        return analyseTypeSchema( endpoint, source, false, new IdentityHashMap<>() );
    }

    /**
     * The centerpiece of the type analysis.
     * <p>
     * Some important aspects to understand:
     * <ul>
     * <li>Only {@link Class} types (named types in Java) that never transform
     * to different schemas depending on their context may end up in
     * {@link Api#getSchemas()}. Otherwise one (the first) transformation would
     * wrongly be used for all possible transformations.</li>
     * <li>While resolving the schema of a {@link Class} type the resulting
     * {@link Api.Schema} is added to the resolving context map before any
     * properties of that schema are recursively resolved. This is necessary so
     * that recursive type structures do not end up in endless loops or stack
     * overflows. Instead the context already knows the {@link Api.Schema}
     * instance for the type (even if some of its properties might still be
     * missing) and the instance can be returned from the resolving
     * context.</li>
     * </ul>
     *
     * @return a schema describing a complex "record-like" or "bean" object
     */
    private static Api.Schema analyseClassSchema( Api.Endpoint endpoint, Class<?> rawType,
        Map<Class<?>, Api.Schema> resolving )
    {
        Api.Schema s = resolving.get( rawType );
        if ( s != null )
        {
            return s;
        }
        boolean sharable = isSharable( rawType, true );
        UnaryOperator<Api.Schema> resolvedTo = schema -> {
            if ( schema.isShared() )
            {
                OpenApi.Shared shared = rawType.getAnnotation( OpenApi.Shared.class );
                schema.getSharedName().setValue( shared == null || shared.name().isEmpty() ? null : shared.name() );
            }
            resolving.put( rawType, schema );
            return schema;
        };
        Function<Class<?>, Api.Schema> createSchema = type -> {
            if ( type.isArray() )
            {
                Api.Schema schema = resolvedTo.apply( new Api.Schema( Api.Schema.Type.ARRAY, false, type, type ) );
                // eventually this will resolve the simple element type
                schema.withElements( analyseClassSchema( endpoint, type.getComponentType(), resolving ) );
                return schema;
            }
            if ( type.isAnnotationPresent( JsonSubTypes.class ) )
            {
                return analyseSubTypeSchema( endpoint, type, resolving );
            }
            boolean alwaysSimple = isSimpleType( type );
            Collection<Property> properties = alwaysSimple ? List.of() : getProperties( type );
            boolean named = sharable && isShared( type );
            if ( alwaysSimple || properties.isEmpty() )
            {
                return resolvedTo.apply( new Api.Schema( Api.Schema.Type.SIMPLE, named, type, type ) );
            }
            Api.Schema schema = resolvedTo.apply( new Api.Schema( Api.Schema.Type.OBJECT, named, type, type ) );
            properties.forEach( property -> schema.getProperties().add(
                new Api.Property( getPropertyName( endpoint, property ), property.getRequired(),
                    analysePropertySchema( endpoint, property, resolving ) ) ) );
            return schema;
        };
        return !sharable
            ? createSchema.apply( rawType )
            : endpoint.getIn().getIn().getSchemas().computeIfAbsent( rawType, createSchema );
    }

    private static Api.Schema analysePropertySchema( Api.Endpoint endpoint, Property property,
        Map<Class<?>, Api.Schema> resolving )
    {
        AnnotatedElement member = (AnnotatedElement) property.getSource();
        if ( member.isAnnotationPresent( JsonSubTypes.class ) )
        {
            return analyseSubTypeSchema( endpoint, member, resolving );
        }
        Type type = getSubstitutedType( endpoint, property, member );
        if ( type instanceof Class && isGeneratorType( (Class<?>) type ) )
        {
            return analyseGeneratorSchema( endpoint, type, member.getAnnotation( OpenApi.Property.class ).value() );
        }
        return analyseTypeSchema( endpoint, type, type == property.getType(), resolving );
    }

    private static Api.Schema analyseSubTypeSchema( Api.Endpoint endpoint, AnnotatedElement baseType,
        Map<Class<?>, Api.Schema> resolving )
    {
        List<Class<?>> types = Stream.of( baseType.getAnnotation( JsonSubTypes.class ).value() )
            .map( JsonSubTypes.Type::value )
            .collect( toList() );
        return Api.Schema.oneOf( types, subType -> analyseClassSchema( endpoint, subType, resolving ) );
    }

    private static Api.Schema analyseTypeSchema( Api.Endpoint endpoint, Type source, boolean useRefs,
        Map<Class<?>, Api.Schema> resolving )
    {
        if ( source instanceof Class<?> type )
        {
            if ( useRefs && isReferencableType( type ) )
            {
                return Api.Schema.ref( type );
            }
            if ( useRefs && isReferencableArrayType( type ) )
            {
                return new Api.Schema( Api.Schema.Type.ARRAY, false, type, type )
                    .withElements( Api.Schema.ref( type.getComponentType() ) );
            }
            return analyseClassSchema( endpoint, type, resolving );
        }
        if ( source instanceof ParameterizedType pt )
        {
            Class<?> rawType = (Class<?>) pt.getRawType();
            if ( rawType == Class.class )
            {
                return new Api.Schema( Api.Schema.Type.SIMPLE, false, source, rawType );
            }
            Type typeArg0 = pt.getActualTypeArguments()[0];
            if ( Collection.class.isAssignableFrom( rawType ) && rawType.isInterface() || rawType == Iterable.class )
            {
                if ( typeArg0 instanceof Class<?> )
                    return analyseTypeSchema( endpoint, Array.newInstance( (Class<?>) typeArg0, 0 ).getClass(),
                        useRefs, resolving );
                return new Api.Schema( Api.Schema.Type.ARRAY, false, source, rawType )
                    .withElements( analyseTypeSchema( endpoint, typeArg0, useRefs, resolving ) );
            }
            if ( Map.class.isAssignableFrom( rawType ) && rawType.isInterface() )
            {
                return new Api.Schema( Api.Schema.Type.OBJECT, isShared( rawType ), source, rawType ).withEntries(
                    analyseTypeSchema( endpoint, typeArg0, false, resolving ),
                    analyseTypeSchema( endpoint, pt.getActualTypeArguments()[1], useRefs, resolving ) );
            }
            if ( rawType == ResponseEntity.class )
            {
                // just unpack, present of ResponseEntity is hidden
                return analyseTypeSchema( endpoint, typeArg0, false, resolving );
            }
            return Api.Schema.unknown( source );
        }
        if ( source instanceof WildcardType wt )
        {
            if ( wt.getLowerBounds().length == 0
                && Arrays.equals( wt.getUpperBounds(), new Type[] { Object.class } ) )
                return Api.Schema.unknown( wt );
            // simplification: <? extends X> => <X>
            return analyseTypeSchema( endpoint, wt.getUpperBounds()[0], useRefs, resolving );
        }
        return Api.Schema.unknown( source );
    }

    private static boolean isShared( Class<?> source )
    {
        String name = source.getName();
        return !source.isPrimitive()
            && !source.isEnum()
            && !source.isArray()
            && !name.startsWith( "java.lang" )
            && !name.startsWith( "java.util" )
            && !OpenApiGenerator.isSimpleType( source );
    }

    private static boolean isReferencableType( Class<?> type )
    {
        return IdentifiableObject.class.isAssignableFrom( type )
            && type != Period.class
            && !EmbeddedObject.class.isAssignableFrom( type );
    }

    private static boolean isReferencableArrayType( Class<?> type )
    {
        return IdentifiableObject[].class.isAssignableFrom( type )
            && type != Period[].class
            && !EmbeddedObject[].class.isAssignableFrom( type );
    }

    /*
     * OpenAPI "business" helper methods
     */

    private static boolean isSharable( Class<?> rawType, boolean defaultValue )
    {
        return !rawType.isAnnotationPresent( OpenApi.Shared.class )
            ? defaultValue
            : rawType.getAnnotation( OpenApi.Shared.class ).value();
    }

    private static String getPropertyName( Api.Endpoint endpoint, Property property )
    {
        return "path$".equals( property.getName() )
            ? endpoint.getIn().getPaths().get( 0 ).replace( "/", "" )
            : property.getName();
    }

    private static boolean isSimpleType( Class<?> type )
    {
        if ( type.isEnum()
            || type == MultipartFile.class
            || type == Geometry.class )
        {
            return true;
        }
        String moduleName = type.getModule().getName();
        if ( moduleName == null )
        {
            return false;
        }
        return moduleName.startsWith( "java." ) || moduleName.startsWith( "jdk." );
    }

    private static Type getSubstitutedType( Api.Endpoint endpoint, Property property, AnnotatedElement member )
    {
        Type type = property.getType();
        if ( member.isAnnotationPresent( OpenApi.EntityType.class ) )
        {
            return getSubstitutedType( endpoint, member.getAnnotation( OpenApi.EntityType.class ).value() );
        }
        if ( type instanceof Class<?> )
        {
            return getSubstitutedType( endpoint, (Class<?>) type );
        }
        return type;
    }

    /**
     * @return the type referred to by the type found in an annotation.
     */
    private static Class<?> getSubstitutedType( Api.Endpoint endpoint, Class<?> type )
    {
        if ( type == OpenApi.EntityType.class && endpoint.getEntityType() != null )
        {
            return endpoint.getEntityType();
        }
        if ( type == OpenApi.EntityType[].class && endpoint.getEntityType() != null )
        {
            return Array.newInstance( endpoint.getEntityType(), 0 ).getClass();
        }
        if ( type == WebMessageResponse.class )
        {
            return WebMessage.class;
        }
        return type;
    }

    private static boolean isControllerType( Class<?> source )
    {
        return (source.isAnnotationPresent( RestController.class )
            || source.isAnnotationPresent( Controller.class ))
            && !source.isAnnotationPresent( OpenApi.Ignore.class );
    }

    /**
     * @return is this a parameter objects with properties which are parameters?
     */
    private static boolean isParams( Parameter source )
    {
        Class<?> type = source.getType();
        if ( type.isAnnotationPresent( OpenApi.Params.class ) )
        {
            return true;
        }
        if ( type.isInterface()
            || type.isEnum()
            || IdentifiableObject.class.isAssignableFrom( type )
            || source.getAnnotations().length > 0
            || source.isAnnotationPresent( OpenApi.Ignore.class )
            || !(source.getParameterizedType() instanceof Class) )
            return false;
        return stream( type.getDeclaredConstructors() ).anyMatch( c -> c.getParameterCount() == 0 );
    }

    private static boolean isRootPath( Class<?> controller, Set<String> included )
    {
        RequestMapping a = controller.getAnnotation( RequestMapping.class );
        return a != null && stream( firstNonEmpty( a.value(), a.path() ) ).anyMatch( included::contains );
    }

    private static EndpointMapping getMapping( Method source )
    {
        if ( ConsistentAnnotatedElement.of( source ).isAnnotationPresent( OpenApi.Ignore.class ) )
        {
            return null;// ignore this
        }
        if ( source.isAnnotationPresent( RequestMapping.class ) )
        {
            RequestMapping a = source.getAnnotation( RequestMapping.class );
            return new EndpointMapping( source, a.name(), firstNonEmpty( a.value(), a.path(), ROOT_PATH ),
                a.method(), a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( GetMapping.class ) )
        {
            GetMapping a = source.getAnnotation( GetMapping.class );
            return new EndpointMapping( source, a.name(), firstNonEmpty( a.value(), a.path(), ROOT_PATH ),
                new RequestMethod[] { RequestMethod.GET }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( PutMapping.class ) )
        {
            PutMapping a = source.getAnnotation( PutMapping.class );
            return new EndpointMapping( source, a.name(), firstNonEmpty( a.value(), a.path(), ROOT_PATH ),
                new RequestMethod[] { RequestMethod.PUT }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( PostMapping.class ) )
        {
            PostMapping a = source.getAnnotation( PostMapping.class );
            return new EndpointMapping( source, a.name(), firstNonEmpty( a.value(), a.path(), ROOT_PATH ),
                new RequestMethod[] { RequestMethod.POST }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( PatchMapping.class ) )
        {
            PatchMapping a = source.getAnnotation( PatchMapping.class );
            return new EndpointMapping( source, a.name(), firstNonEmpty( a.value(), a.path(), ROOT_PATH ),
                new RequestMethod[] { RequestMethod.PATCH }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        if ( source.isAnnotationPresent( DeleteMapping.class ) )
        {
            DeleteMapping a = source.getAnnotation( DeleteMapping.class );
            return new EndpointMapping( source, a.name(), firstNonEmpty( a.value(), a.path(), ROOT_PATH ),
                new RequestMethod[] { RequestMethod.DELETE }, a.params(), a.headers(), a.consumes(), a.produces() );
        }
        return null;
    }

    private static EndpointParam getParam( Parameter source )
    {
        if ( source.isAnnotationPresent( PathVariable.class ) )
        {
            PathVariable a = source.getAnnotation( PathVariable.class );
            return new EndpointParam( Api.Parameter.In.PATH, firstNonEmpty( a.name(), a.value() ), a.required() );
        }
        if ( source.isAnnotationPresent( RequestParam.class ) )
        {
            RequestParam a = source.getAnnotation( RequestParam.class );
            boolean required = a.required()
                && a.defaultValue().equals( "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n" );
            return new EndpointParam( Api.Parameter.In.QUERY, firstNonEmpty( a.name(), a.value() ), required );
        }
        if ( source.isAnnotationPresent( RequestBody.class ) )
        {
            RequestBody a = source.getAnnotation( RequestBody.class );
            return new EndpointParam( Api.Parameter.In.BODY, "", a.required() );
        }
        return null;
    }

    /*
     * Basic helper methods
     */

    private static Api.SchemaGenerator newGenerator( Class<?> type )
    {
        Api.SchemaGenerator generator = GENERATORS.get( type );
        if ( generator == null )
        {
            throw new IllegalStateException( "No generator for type: " + type );
        }
        return generator;
    }

    private static String[] firstNonEmpty( String[] a, String[] b, String[] c )
    {
        return firstNonEmpty( firstNonEmpty( a, b ), c );
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

    @SafeVarargs
    private static <E extends Enum<E>> E firstNonEqual( E to, E... samples )
    {
        return stream( samples ).filter( e -> e != to ).findFirst().orElse( samples[0] );
    }

    record EndpointMapping( Method source, String name, String[] path, RequestMethod[] method, String[] params,
        String[] headers, String[] consumes, String[] produces )
    {
    }

    record EndpointParam( Api.Parameter.In in, String name, boolean required )
    {
    }

    /*
     * Helpers for working with annotations
     */

    private static <A extends Annotation> Stream<A> getAnnotations( Method on, Class<A> type )
    {
        return stream( ConsistentAnnotatedElement.of( on ).getAnnotationsByType( type ) );
    }

    private static <A extends Annotation, T extends AnnotatedElement> void whenAnnotated( T on, Class<A> type,
        Consumer<A> whenPresent )
    {
        AnnotatedElement target = ConsistentAnnotatedElement.of( on );
        if ( target.isAnnotationPresent( type ) )
        {
            whenPresent.accept( target.getAnnotation( type ) );
        }
    }

    private static <A extends Annotation, B, T extends AnnotatedElement> B getAnnotated( T on, Class<A> type,
        Function<A, B> whenPresent, Predicate<B> test, Supplier<B> otherwise )
    {
        AnnotatedElement target = ConsistentAnnotatedElement.of( on );
        if ( !target.isAnnotationPresent( type ) )
        {
            return otherwise.get();
        }
        B value = whenPresent.apply( target.getAnnotation( type ) );
        return test.test( value ) ? value : otherwise.get();
    }
}
