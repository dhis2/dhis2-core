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
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generates a <a href=
 * "https://github.com/OAI/OpenAPI-Specification/blob/main/versions/">OpenAPI
 * 3.x</a> version JSON document from an {@link Api} model.
 *
 * The generation offers a dozen {@link Config} options which concern both the
 * {@link org.hisp.dhis.webapi.openapi.JsonGenerator.Format} of the generated
 * JSON as well as the semantic {@link Config.Document} content.
 *
 * Alongside the input {@link Api} model there is a pool of known
 * {@link SimpleType}s. This is the core translation of primitives, wrapper,
 * {@link String}s but also used as a "correction" for seemingly complex types
 * which in their serialized form become simple ones, like a period that uses
 * its ISO string form.
 *
 * @author Jan Bernit
 */
@Slf4j
public class OpenApiGenerator extends JsonGenerator
{
    @Value
    static class Config
    {
        Format format;

        Document document;

        @Value
        @Builder
        static class Document
        {
            String version;

            String serverUrl;

            String licenseName;

            String licenseUrl;

            String contactName;

            String contactUrl;

            String contactEmail;

            boolean syntheticSummary;

            boolean syntheticDescription;
        }
    }

    @Value
    @Builder
    private static class SimpleType
    {
        Class<?> source;

        /*
         * OpenAPI properties below:
         */

        String type;

        String format;

        Boolean nullable;

        Integer minLength;

        Integer maxLength;

        String[] enums;
    }

    private static final Map<Class<?>, List<SimpleType>> SIMPLE_TYPES = new IdentityHashMap<>();

    private static void addSimpleType( Class<?> source, Consumer<SimpleType.SimpleTypeBuilder> schema )
    {
        SimpleType.SimpleTypeBuilder b = new SimpleType.SimpleTypeBuilder();
        b.source( source );
        schema.accept( b );
        SimpleType s = b.build();
        SIMPLE_TYPES.computeIfAbsent( s.source, key -> new ArrayList<>() ).add( s );
    }

    static
    {
        addSimpleType( int.class, schema -> schema.type( "integer" ).format( "int32" ).nullable( false ) );
        addSimpleType( long.class, schema -> schema.type( "integer" ).format( "int64" ).nullable( false ) );
        addSimpleType( float.class, schema -> schema.type( "number" ).format( "float" ).nullable( false ) );
        addSimpleType( double.class, schema -> schema.type( "number" ).format( "double" ).nullable( false ) );
        addSimpleType( boolean.class, schema -> schema.type( "boolean" ).nullable( false ) );
        addSimpleType( char.class, schema -> schema.type( "string" ).nullable( false ).minLength( 1 ).maxLength( 1 ) );
        addSimpleType( Integer.class, schema -> schema.type( "integer" ).format( "int32" ).nullable( true ) );
        addSimpleType( Long.class, schema -> schema.type( "integer" ).format( "int64" ).nullable( true ) );
        addSimpleType( Float.class, schema -> schema.type( "number" ).format( "float" ).nullable( true ) );
        addSimpleType( Double.class, schema -> schema.type( "number" ).format( "double" ).nullable( true ) );
        addSimpleType( Boolean.class, schema -> schema.type( "boolean" ).nullable( true ) );
        addSimpleType( Character.class,
            schema -> schema.type( "string" ).nullable( true ).minLength( 1 ).maxLength( 1 ) );
        addSimpleType( String.class, schema -> schema.type( "string" ).nullable( true ) );
        addSimpleType( Date.class, schema -> schema.type( "string" ).format( "date-time" ).nullable( true ) );
        addSimpleType( Locale.class, schema -> schema.type( "string" ).nullable( true ) );
        addSimpleType( JsonNode.class, schema -> schema.type( "object" ) );
        addSimpleType( ObjectNode.class, schema -> schema.type( "object" ) );
        addSimpleType( ArrayNode.class, schema -> schema.type( "array" ) );
        addSimpleType( RootNode.class, schema -> schema.type( "object" ) );
        addSimpleType( JsonPointer.class, schema -> schema.type( "string" ) );
        addSimpleType( Period.class, schema -> schema.type( "string" ) );
        addSimpleType( PeriodType.class, schema -> schema.type( "string" )
            .enums( stream( PeriodTypeEnum.values() ).map( PeriodTypeEnum::getName ).toArray( String[]::new ) ) );
        addSimpleType( Instant.class, schema -> schema.type( "string" ).format( "date-time" ) );
        addSimpleType( Instant.class, schema -> schema.type( "integer" ).format( "int64" ) );
        // not quite
        addSimpleType( Serializable.class, schema -> schema.type( "string" ) );
        addSimpleType( Geometry.class, schema -> schema.type( "object" ) );
    }

    public static String generate( Api api )
    {
        // "2.40", , true, true )
        Config.Document doc = Config.Document.builder()
            .version( "2.40" )
            .serverUrl( "https://play.dhis2.org/dev/api" )
            .licenseName( "BSD 3-Clause \"New\" or \"Revised\" License" )
            .licenseUrl( "https://raw.githubusercontent.com/dhis2/dhis2-core/master/LICENSE" )
            .syntheticSummary( true )
            .syntheticDescription( true )
            .build();
        return generate( api, new Config( Format.PRETTY_PRINT, doc ) );
    }

    public static String generate( Api api, Config config )
    {
        int endpoints = 0;
        for ( Api.Controller c : api.getControllers() )
            endpoints += c.getEndpoints().size();
        int capacity = endpoints * 256 + api.getSchemas().size() * 512;
        OpenApiGenerator gen = new OpenApiGenerator( api, config, new StringBuilder( capacity ) );
        gen.generateDocument();
        return gen.toString();
    }

    private final Api api;

    private final Config.Document document;

    private OpenApiGenerator( Api api, Config config, StringBuilder out )
    {
        super( out, config.format );
        this.api = api;
        this.document = config.document;
    }

    private final Map<String, List<Api.Endpoint>> endpointsByBaseOperationId = new HashMap<>();

    private final Map<String, Class<?>> typeByName = new TreeMap<>();

    private final Map<String, Api.Schema> refTypeByName = new TreeMap<>();

    private void generateDocument()
    {
        Descriptions tags = Descriptions.of( OpenApi.Tags.class );
        addRootObject( () -> {
            addStringMember( "openapi", "3.0.0" );
            addObjectMember( "info", () -> {
                addStringMember( "title", "DHIS2 API" );
                addStringMember( "version", document.version );
                addObjectMember( "license", () -> {
                    addStringMember( "name", document.licenseName );
                    addStringMember( "url", document.licenseUrl );
                } );
                addObjectMember( "contact", () -> {
                    addStringMember( "name", document.contactName );
                    addStringMember( "url", document.contactUrl );
                    addStringMember( "email", document.contactEmail );
                } );
            } );
            addArrayMember( "tags", api.getTags().stream(), tag -> addObjectMember( null, () -> {
                addStringMember( "name", tag );
                addStringMember( "description", tags.get( tag + ".description" ) );
                addObjectMember( "externalDocs", tags.exists( tag + ".externalDocs.url" ), () -> {
                    addStringMember( "url", tags.get( tag + ".externalDocs.url" ) );
                    addStringMember( "description", tags.get( tag + ".externalDocs.description" ) );
                } );
            } ) );
            addArrayMember( "servers",
                () -> addObjectMember( null, () -> addStringMember( "url", document.serverUrl ) ) );
            addObjectMember( "paths", this::generatePaths );
            addObjectMember( "components", () -> addObjectMember( "schemas", this::generateSchemas ) );
        } );
        log.info( format( "OpenAPI document generated for %d controllers with %d named schemas",
            api.getControllers().size(), api.getSchemas().size() ) );
    }

    private void generatePaths()
    {
        groupEndpointsByAbsolutePath().forEach( this::generatePath );
    }

    private void generatePath( String path, List<Api.Endpoint> endpoints )
    {
        EnumMap<RequestMethod, Api.Endpoint> endpointByMethod = new EnumMap<>( RequestMethod.class );
        endpoints.forEach( e -> e.getMethods().forEach(
            method -> endpointByMethod.compute( method, ( k, v ) -> ApiMerger.mergeEndpoints( v, e, method ) ) ) );
        addObjectMember( path, () -> endpointByMethod.forEach( this::generatePathMethod ) );
    }

    private void generatePathMethod( RequestMethod method, Api.Endpoint endpoint )
    {
        Set<String> tags = union( endpoint.getIn().getTags(), endpoint.getTags() );
        if ( endpoint.isSynthetic() )
            tags.add( "synthetic" );
        addObjectMember( method.name().toLowerCase(), () -> {
            addBooleanMember( "deprecated", endpoint.getDeprecated() );
            addStringMember( "description", endpoint.getDescription().orElse( "(missing)" ) );
            addStringMember( "operationId", getUniqueOperationId( endpoint ) );
            if ( !tags.isEmpty() )
            {
                addArrayMember( "tags", tags );
            }
            if ( !endpoint.getParameters().isEmpty() )
            {
                addArrayMember( "parameters",
                    () -> endpoint.getParameters().values().forEach( this::generateParameter ) );
            }
            if ( endpoint.getRequestBody().isPresent() )
            {
                addObjectMember( "requestBody",
                    () -> generateRequestBody( endpoint.getRequestBody().getValue() ) );
            }
            if ( !endpoint.getResponses().isEmpty() )
            {
                addObjectMember( "responses",
                    () -> endpoint.getResponses().values().forEach( this::generateResponse ) );
            }
        } );
    }

    private void generateParameter( Api.Parameter parameter )
    {
        addObjectMember( null, () -> {
            addStringMember( "name", parameter.getName() );
            addStringMember( "in", parameter.getIn().name().toLowerCase() );
            addStringMember( "description", parameter.getDescription() );
            addBooleanMember( "required", parameter.isRequired() );
            addObjectMember( "schema", () -> generateSchemaOrRef( parameter.getType() ) );
        } );
    }

    private void generateRequestBody( Api.RequestBody requestBody )
    {
        addStringMember( "description", requestBody.getDescription() );
        addBooleanMember( "required", requestBody.isRequired() );
        addObjectMember( "content",
            () -> requestBody.getConsumes().forEach( ( key, value ) -> addObjectMember( key.toString(),
                () -> addObjectMember( "schema", () -> generateSchemaOrRef( value ) ) ) ) );
    }

    private void generateResponse( Api.Response response )
    {
        addObjectMember( String.valueOf( response.getStatus().value() ), () -> {
            addStringMember( "description", response.getDescription().orElse( "(missing)" ) );
            addObjectMember( "headers",
                () -> response.getHeaders().values().forEach( header -> addObjectMember( header.getName(), () -> {
                    addStringMember( "description", header.getDescription() );
                    addObjectMember( "schema", () -> generateSchema( header.getType() ) );
                } ) ) );
            boolean hasContent = !response.getContent().isEmpty() && response.getStatus() != HttpStatus.NO_CONTENT;
            addObjectMember( "content", hasContent,
                () -> response.getContent().forEach( ( produces, body ) -> addObjectMember( produces.toString(),
                    () -> addObjectMember( "schema", () -> generateSchemaOrRef( body ) ) ) ) );
        } );
    }

    private void generateSchemas()
    {
        // make sure all types have a unique name
        api.getSchemas().entrySet().stream()
            .filter( e -> e.getValue().isNamed() )
            .forEach( e -> getUniqueName( e.getKey() ) );
        // write normal schemas
        typeByName
            .forEach( ( name, type ) -> addObjectMember( name, () -> generateSchema( api.getSchemas().get( type ) ) ) );
        // write ref schemas
        refTypeByName.forEach( ( name, schema ) -> addObjectMember( name, () -> generateSchema( schema ) ) );
    }

    private static boolean isReferencableSchema( Api.Schema schema )
    {
        return schema.isNamed()
            && !schema.getFields().isEmpty()
            && !SIMPLE_TYPES.containsKey( schema.getSource() );
    }

    private void generateSchemaOrRef( Api.Schema schema )
    {
        if ( schema == null )
            return;
        if ( schema.getSource() == Api.Ref.class )
        {
            Class<?> to = (Class<?>) schema.getHint();
            String name = "Ref:" + getUniqueName( to );
            addStringMember( "$ref", "#/components/schemas/" + name );
            refTypeByName.putIfAbsent( name, schema );
            return;
        }
        if ( !isReferencableSchema( schema ) )
        {
            generateSchema( schema );
        }
        else
        {
            addStringMember( "$ref", "#/components/schemas/" + getUniqueName( schema.getSource() ) );
        }
    }

    private String getUniqueName( Class<?> type )
    {
        String name = type.getSimpleName();
        Class<?> assigned = typeByName.get( name );
        if ( assigned == type )
        {
            return name;
        }
        if ( assigned == null )
        {
            typeByName.put( name, type );
            return name;
        }
        // clash => use full name
        name = type.getCanonicalName().replace( "org.hisp.dhis.", "" );
        typeByName.put( name, type );
        return name;
    }

    private void generateSchema( Api.Schema schema )
    {
        Class<?> type = schema.getSource();
        List<SimpleType> types = SIMPLE_TYPES.get( type );
        if ( types != null )
        {
            if ( types.size() == 1 )
            {
                generateSimpleTypeSchema( types.get( 0 ) );
            }
            else
            {
                addArrayMember( "oneOf",
                    () -> types.forEach( t -> addObjectMember( null, () -> generateSimpleTypeSchema( t ) ) ) );
            }
            return;
        }
        if ( type == Api.Ref.class )
        {
            generateRefType( schema );
            return;
        }
        if ( type == Api.Unknown.class )
        {
            addStringMember( "description",
                "The exact type is unknown, Java type was: " + schema.getHint().getTypeName() );
            return;
        }
        if ( type.isEnum() )
        {
            addStringMember( "type", "string" );
            addArrayMember( "enum", stream( type.getEnumConstants() )
                .map( e -> ((Enum<?>) e).name() ).collect( toList() ) );
            return;
        }
        if ( type.isArray() )
        {
            Api.Schema elements = schema.getSource() == Api.Ref[].class
                ? Api.ref( (Class<?>) schema.getHint() )
                : api.getSchemas().get( type.getComponentType() );
            addStringMember( "type", "array" );
            addObjectMember( "items", () -> generateSchemaOrRef( elements ) );
            return;
        }
        // best guess: it is an object type
        addStringMember( "type", "object" );
        if ( !schema.getFields().isEmpty() )
        {
            if ( type == Map.class )
            {
                addObjectMember( "additionalProperties",
                    () -> generateSchemaOrRef( schema.getFields().get( 1 ).getType() ) );
                if ( schema.getFields().get( 0 ).getType().getSource() != String.class )
                    addStringMember( "description", "keys are " + schema.getFields().get( 0 ).getType().getSource() );
                return;
            }
            addArrayMember( "required", schema.getRequiredFields() );
            addObjectMember( "properties", () -> schema.getFields()
                .forEach( field -> addObjectMember( field.getName(), () -> generateSchemaOrRef( field.getType() ) ) ) );
        }
        else
        {
            System.out.println( schema + " " + schema.getSource() + " " + schema.getHint() );
            log.warn( schema.toString() );
        }
    }

    private void generateSimpleTypeSchema( SimpleType simpleType )
    {
        addStringMember( "type", simpleType.getType() );
        addStringMember( "format", simpleType.getFormat() );
        addBooleanMember( "nullable", simpleType.getNullable() );
        addNumberMember( "minLength", simpleType.getMinLength() );
        addNumberMember( "maxLength", simpleType.getMaxLength() );
        if ( simpleType.getEnums() != null )
        {
            addArrayMember( "enum", List.of( simpleType.getEnums() ) );
        }
    }

    private void generateRefType( Api.Schema schema )
    {
        addStringMember( "type", "object" );
        addStringMember( "description", "A UID reference to a " + ((Class<?>) schema.getHint()).getSimpleName() );
        addArrayMember( "required", List.of( "id" ) );
        addObjectMember( "properties", () -> addObjectMember( "id", () -> {
            addStringMember( "type", "string" );
            addStringMember( "format", "uid" );
            addStringMember( "pattern", "^[0-9a-zA-Z]{11}$" );
            addBooleanMember( "readOnly", true );
            addNumberMember( "minLength", 11 );
            addNumberMember( "maxLength", 11 );
        } ) );
    }

    /*
     * Open API document generation helpers
     */

    private String getUniqueOperationId( Api.Endpoint endpoint )
    {
        String baseOperationId = endpoint.getIn().getName() + "." + endpoint.getName();
        List<Api.Endpoint> endpoints = endpointsByBaseOperationId.computeIfAbsent( baseOperationId,
            key -> new ArrayList<>() );
        endpoints.add( endpoint );
        return endpoints.size() == 1 ? baseOperationId : baseOperationId + endpoints.size();
    }

    private Map<String, List<Api.Endpoint>> groupEndpointsByAbsolutePath()
    {
        // OBS! We use a TreeMap to also get alphabetical order/grouping
        Map<String, List<Api.Endpoint>> endpointsByAbsolutePath = new TreeMap<>();
        for ( Api.Controller c : api.getControllers() )
        {
            if ( c.getPaths().isEmpty() )
                c.getPaths().add( "" );
            for ( String cPath : c.getPaths() )
            {
                for ( Api.Endpoint e : c.getEndpoints() )
                {
                    for ( String ePath : e.getPaths() )
                    {
                        String absolutePath = cPath + ePath;
                        endpointsByAbsolutePath.computeIfAbsent( absolutePath, key -> new ArrayList<>() ).add( e );
                    }
                }
            }
        }
        return endpointsByAbsolutePath;
    }

    private static <E> Set<E> union( Set<E> a, Set<E> b )
    {
        if ( a.isEmpty() && b.isEmpty() )
            return new HashSet<>();
        if ( a.isEmpty() )
            return b;
        if ( b.isEmpty() )
            return a;
        return Stream.concat( a.stream(), b.stream() ).collect( toSet() );
    }
}
