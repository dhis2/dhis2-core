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
import static java.util.stream.Collectors.toCollection;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.node.config.InclusionStrategy;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.webapi.common.UID;
import org.hisp.dhis.webmessage.WebMessageResponse;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generates a <a href=
 * "https://github.com/OAI/OpenAPI-Specification/blob/main/versions/">OpenAPI
 * 3.x</a> version JSON document from an {@link Api} model.
 * <p>
 * The generation offers a dozen configuration options which concern both the
 * {@link org.hisp.dhis.webapi.openapi.JsonGenerator.Format} of the generated
 * JSON as well as the semantic {@link Info} content.
 * <p>
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
    @Builder( toBuilder = true )
    static class Info
    {
        public static final Info DEFAULT = Info.builder()
            .title( "DHIS2 API" )
            .version( "2.40" )
            .serverUrl( "https://play.dhis2.org/dev/api" )
            .licenseName( "BSD 3-Clause \"New\" or \"Revised\" License" )
            .licenseUrl( "https://raw.githubusercontent.com/dhis2/dhis2-core/master/LICENSE" )
            .build();

        String title;

        String version;

        String serverUrl;

        String licenseName;

        String licenseUrl;

        String contactName;

        String contactUrl;

        String contactEmail;
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

        String pattern;

        Boolean nullable;

        Integer minLength;

        Integer maxLength;

        String[] enums;
    }

    private static final Map<Class<?>, List<SimpleType>> SIMPLE_TYPES = new IdentityHashMap<>();

    public static boolean isSimpleType( Class<?> type )
    {
        return SIMPLE_TYPES.containsKey( type );
    }

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
        addSimpleType( byte.class, schema -> schema.type( "integer" ).format( "int8" ).nullable( false ) );
        addSimpleType( byte[].class, schema -> schema.type( "string" ).format( "binary" ).nullable( false ) );
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
        addSimpleType( Class.class, schema -> schema.type( "string" ).format( "class" ).nullable( false ) );
        addSimpleType( Date.class, schema -> schema.type( "string" ).format( "date-time" ).nullable( true ) );
        addSimpleType( URI.class, schema -> schema.type( "string" ).format( "uri" ).nullable( true ) );
        addSimpleType( URL.class, schema -> schema.type( "string" ).format( "url" ).nullable( true ) );
        addSimpleType( UUID.class, schema -> schema.type( "string" ).format( "uuid" ).nullable( true )
            .pattern( "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$" ) );
        addSimpleType( UID.class, schema -> schema.type( "string" ).format( "uid" ).nullable( true )
            .pattern( CodeGenerator.UID_REGEXP ) );
        addSimpleType( Locale.class, schema -> schema.type( "string" ).nullable( true ) );
        addSimpleType( Instant.class, schema -> schema.type( "string" ).format( "date-time" ) );
        addSimpleType( Instant.class, schema -> schema.type( "integer" ).format( "int64" ) );
        addSimpleType( Serializable.class, schema -> schema.type( "string" ) );
        addSimpleType( Serializable.class, schema -> schema.type( "number" ) );
        addSimpleType( Serializable.class, schema -> schema.type( "boolean" ) );

        addSimpleType( Period.class, schema -> schema.type( "string" ).format( "period" ) );
        addSimpleType( PeriodType.class, schema -> schema.type( "string" )
            .enums( stream( PeriodTypeEnum.values() ).map( PeriodTypeEnum::getName ).toArray( String[]::new ) ) );
        addSimpleType( InclusionStrategy.class, schema -> schema.type( "string" )
            .enums( stream( InclusionStrategy.Include.values() ).map( Enum::name ).toArray( String[]::new ) ) );
        addSimpleType( MultipartFile.class, schema -> schema.type( "string" ).format( "binary" ) );

        addSimpleType( JsonNode.class, schema -> schema.type( "object" ) );
        addSimpleType( ObjectNode.class, schema -> schema.type( "object" ) );
        addSimpleType( ArrayNode.class, schema -> schema.type( "array" ) );
        addSimpleType( RootNode.class, schema -> schema.type( "object" ) );
        addSimpleType( JsonPointer.class, schema -> schema.type( "string" ) );

        addSimpleType( Geometry.class, schema -> schema.type( "object" ) );
        addSimpleType( WebMessageResponse.class, schema -> schema.type( "object" ) );
        addSimpleType( JobParameters.class, schema -> schema.type( "object" ) );
    }

    public static String generateJson( Api api, String serverUrl )
    {
        return generateJson( api, Format.PRETTY_PRINT,
            Info.DEFAULT.toBuilder().serverUrl( serverUrl ).build() );
    }

    public static String generateJson( Api api, Format format, Info info )
    {
        return generate( api, format, Language.JSON, info );
    }

    public static String generateYaml( Api api, String serverUrl )
    {
        return generateYaml( api, Format.PRETTY_PRINT,
            Info.DEFAULT.toBuilder().serverUrl( serverUrl ).build() );
    }

    public static String generateYaml( Api api, Format format, Info info )
    {
        return generate( api, format, Language.YAML, info );
    }

    private static String generate( Api api, Format format, Language language, Info info )
    {
        int endpoints = 0;
        for ( Api.Controller c : api.getControllers() )
            endpoints += c.getEndpoints().size();
        int capacity = endpoints * 256 + api.getSchemas().size() * 512;
        OpenApiGenerator gen = new OpenApiGenerator( api, language.getAdjustFormat().apply( format ), language,
            info, new StringBuilder( capacity ) );
        gen.generateDocument();
        return gen.toString();
    }

    private static final String NO_DESCRIPTION = "[no description yet]";

    private final Api api;

    private final Info info;

    private final Map<String, List<Api.Endpoint>> endpointsByBaseOperationId = new HashMap<>();

    private OpenApiGenerator( Api api, Format format, Language language, Info info,
        StringBuilder out )
    {
        super( out, format, language );
        this.api = api;
        this.info = info;
    }

    private void generateDocument()
    {
        addRootObject( () -> {
            addStringMember( "openapi", "3.0.0" );
            addObjectMember( "info", () -> {
                addStringMember( "title", info.title );
                addStringMember( "version", info.version );
                addObjectMember( "license", () -> {
                    addStringMember( "name", info.licenseName );
                    addStringMember( "url", info.licenseUrl );
                } );
                addObjectMember( "contact", () -> {
                    addStringMember( "name", info.contactName );
                    addStringMember( "url", info.contactUrl );
                    addStringMember( "email", info.contactEmail );
                } );
            } );
            addArrayMember( "tags", api.getTags().values(), tag -> addObjectMember( null, () -> {
                addStringMember( "name", tag.getName() );
                addStringMultilineMember( "description",
                    tag.getDescription().orElse( NO_DESCRIPTION ) );
                addObjectMember( "externalDocs", tag.getExternalDocsUrl().isPresent(), () -> {
                    addStringMember( "url", tag.getExternalDocsUrl().getValue() );
                    addStringMultilineMember( "description", tag.getExternalDocsDescription().getValue() );
                } );
            } ) );
            addArrayMember( "servers",
                () -> addObjectMember( null, () -> addStringMember( "url", info.serverUrl ) ) );
            addArrayMember( "security",
                () -> addObjectMember( null, () -> addArrayMember( "basicAuth",
                    () -> addArrayMember( null, List.of() ) ) ) );
            addObjectMember( "paths", this::generatePaths );
            addObjectMember( "components", () -> {
                addObjectMember( "securitySchemes", this::generateSecuritySchemes );
                addObjectMember( "schemas", this::generateSharedSchemas );
                addObjectMember( "parameters", this::generateSharedParameters );
            } );
        } );
        log.info( format( "OpenAPI document generated for %d controllers with %d named schemas",
            api.getControllers().size(), api.getSchemas().size() ) );
    }

    private void generatePaths()
    {
        api.getEndpoints().forEach( this::generatePath );
    }

    private void generatePath( String path, Map<RequestMethod, Api.Endpoint> endpoints )
    {
        addObjectMember( path, () -> endpoints.forEach( this::generatePathMethod ) );
    }

    private void generatePathMethod( RequestMethod method, Api.Endpoint endpoint )
    {
        Set<String> tags = mergeTags( endpoint.getIn().getTags(), endpoint.getTags() );
        if ( endpoint.isSynthetic() )
            tags.add( "synthetic" );
        addObjectMember( method.name().toLowerCase(), () -> {
            addBooleanMember( "deprecated", endpoint.getDeprecated() );
            addStringMultilineMember( "description",
                endpoint.getDescription().orElse( NO_DESCRIPTION ) );
            addStringMember( "operationId", getUniqueOperationId( endpoint ) );
            addArrayMember( "tags", tags );
            addArrayMember( "parameters", endpoint.getParameters().values(), this::generateParameterOrRef );
            if ( endpoint.getRequestBody().isPresent() )
            {
                addObjectMember( "requestBody",
                    () -> generateRequestBody( endpoint.getRequestBody().getValue() ) );
            }
            addObjectMember( "responses", endpoint.getResponses().values(), this::generateResponse );
        } );
    }

    private void generateSharedParameters()
    {
        Map<String, Api.Parameter> paramBySharedName = new TreeMap<>();
        for ( List<Api.Parameter> params : api.getComponents().getParameters().values() )
            params.forEach( p -> paramBySharedName.put( p.getFullName(), p ) );
        // use map to sort parameter by name
        paramBySharedName.values().forEach( this::generateParameter );
    }

    private void generateParameterOrRef( Api.Parameter parameter )
    {
        if ( parameter.isShared() )
        {
            // shared parameter usage: => reference object
            addObjectMember( null,
                () -> addStringMember( "$ref", "#/components/parameters/" + parameter.getFullName() ) );
        }
        else
        {
            generateParameter( parameter );
        }
    }

    private void generateParameter( Api.Parameter parameter )
    {
        // parameter definition (both shared and non-shared):
        addObjectMember( parameter.getFullName(), () -> {
            addStringMember( "name", parameter.getName() );
            addStringMember( "in", parameter.getIn().name().toLowerCase() );
            addStringMultilineMember( "description",
                parameter.getDescription().orElse( NO_DESCRIPTION ) );
            addBooleanMember( "required", parameter.isRequired() );
            addObjectMember( "schema", () -> generateSchemaOrRef( parameter.getType() ) );
        } );
    }

    private void generateRequestBody( Api.RequestBody requestBody )
    {
        addStringMultilineMember( "description",
            requestBody.getDescription().orElse( NO_DESCRIPTION ) );
        addBooleanMember( "required", requestBody.isRequired() );
        addObjectMember( "content",
            () -> requestBody.getConsumes().forEach( ( key, value ) -> addObjectMember( key.toString(),
                () -> addObjectMember( "schema", () -> generateSchemaOrRef( value ) ) ) ) );
    }

    private void generateResponse( Api.Response response )
    {
        addObjectMember( String.valueOf( response.getStatus().value() ), () -> {
            addStringMultilineMember( "description",
                response.getDescription().orElse( NO_DESCRIPTION ) );
            addObjectMember( "headers", response.getHeaders().values(),
                header -> addObjectMember( header.getName(), () -> {
                    addStringMultilineMember( "description", header.getDescription() );
                    addObjectMember( "schema", () -> generateSchema( header.getType() ) );
                } ) );
            boolean hasContent = !response.getContent().isEmpty() && response.getStatus() != HttpStatus.NO_CONTENT;
            addObjectMember( "content", hasContent,
                () -> response.getContent().forEach( ( produces, body ) -> addObjectMember( produces.toString(),
                    () -> addObjectMember( "schema", () -> generateSchemaOrRef( body ) ) ) ) );
        } );
    }

    private void generateSecuritySchemes()
    {
        addObjectMember( "basicAuth", () -> {
            addStringMember( "type", "http" );
            addStringMember( "scheme", "basic" );
        } );
    }

    private void generateSharedSchemas()
    {
        api.getComponents().getSchemas().forEach(
            ( name, schema ) -> addObjectMember( name, () -> generateSchema( schema ) ) );

        // write ref/uid schemas:
        api.getComponents().getAdditionalSchemas().forEach(
            ( name, schema ) -> addObjectMember( name, () -> generateSchema( schema ) ) );
    }

    private void generateSchemaOrRef( Api.Schema schema )
    {
        if ( schema == null )
            return;
        if ( schema.getSharedName().isPresent() )
        {
            addStringMember( "$ref", "#/components/schemas/" + schema.getSharedName().getValue() );
        }
        else
        {
            generateSchema( schema );
        }
    }

    private void generateSchema( Api.Schema schema )
    {
        Class<?> type = schema.getRawType();
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
        Api.Schema.Type schemaType = schema.getType();
        if ( schemaType == Api.Schema.Type.REF )
        {
            generateRefTypeSchema( schema );
            return;
        }
        if ( schemaType == Api.Schema.Type.UID )
        {
            generateUidSchema( schema );
            return;
        }
        if ( schemaType == Api.Schema.Type.UNKNOWN )
        {
            addStringMultilineMember( "description",
                "The exact type is unknown.  \n(Java type was: `" + schema.getSource().getTypeName() + "`)" );
            return;
        }
        if ( schemaType == Api.Schema.Type.ONE_OF )
        {
            addArrayMember( "oneOf", schema.getProperties(),
                property -> addObjectMember( null, () -> generateSchemaOrRef( property.getType() ) ) );
            return;
        }
        if ( schemaType == Api.Schema.Type.ENUM )
        {
            addStringMember( "type", "string" );
            addArrayMember( "enum", schema.getValues() );
            return;
        }
        if ( type.isEnum() )
        {
            addStringMember( "type", "string" );
            addArrayMember( "enum", stream( type.getEnumConstants() )
                .map( e -> ((Enum<?>) e).name() ).toList() );
            return;
        }
        if ( type.isArray() || schemaType == Api.Schema.Type.ARRAY )
        {
            Api.Schema elements = schema.getProperties().isEmpty()
                ? api.getSchemas().get( type.getComponentType() )
                : schema.getProperties().get( 0 ).getType();
            addStringMember( "type", "array" );
            addObjectMember( "items", () -> generateSchemaOrRef( elements ) );
            return;
        }
        // best guess: it is an object type
        addStringMember( "type", "object" );
        if ( !schema.getProperties().isEmpty() )
        {
            if ( Map.class.isAssignableFrom( type ) )
            {
                addObjectMember( "additionalProperties",
                    () -> generateSchemaOrRef( schema.getProperties().get( 1 ).getType() ) );
                if ( schema.getProperties().get( 0 ).getType().getRawType() != String.class )
                    addStringMultilineMember( "description",
                        "keys are " + schema.getProperties().get( 0 ).getType().getRawType() );
                return;
            }
            addArrayMember( "required", schema.getRequiredProperties() );
            addObjectMember( "properties", schema.getProperties(),
                property -> addObjectMember( property.getName(), () -> generateSchemaOrRef( property.getType() ) ) );
        }
        else
        {
            addStringMultilineMember( "description", "The actual type is unknown.  \n(Java type was: `" + type + "`)" );
            if ( type != Object.class )
            {
                log.warn( schema + " " + schema.getSource() );
            }
        }
    }

    private void generateSimpleTypeSchema( SimpleType simpleType )
    {
        String type = simpleType.getType();
        addStringMember( "type", type );
        if ( "array".equals( type ) )
        {
            addObjectMember( "items", () -> {
            } );
        }
        addStringMember( "format", simpleType.getFormat() );
        addNumberMember( "minLength", simpleType.getMinLength() );
        addNumberMember( "maxLength", simpleType.getMaxLength() );
        addStringMember( "pattern", simpleType.getPattern() );
        if ( simpleType.getEnums() != null )
        {
            addArrayMember( "enum", List.of( simpleType.getEnums() ) );
        }
    }

    private void generateRefTypeSchema( Api.Schema schema )
    {
        addStringMember( "type", "object" );
        addArrayMember( "required", List.of( "id" ) );
        addObjectMember( "properties", () -> addObjectMember( "id", () -> generateUidSchema( schema ) ) );
        addTypeDescriptionMember( schema.getRawType(), "A UID reference to a %s  \n(Java name `%s`)" );
    }

    private void generateUidSchema( Api.Schema schema )
    {
        addStringMember( "type", "string" );
        addStringMember( "format", "uid" );
        addStringMember( "pattern", "^[0-9a-zA-Z]{11}$" );
        addNumberMember( "minLength", 11 );
        addNumberMember( "maxLength", 11 );
        addStringMultilineMember( "example", generateUid( schema.getRawType() ) );
        if ( schema.getType() == Api.Schema.Type.UID )
        {
            addTypeDescriptionMember( schema.getRawType(), "A UID for an %s object  \n(Java name `%s`)" );
        }
    }

    private void addTypeDescriptionMember( Class<?> type, String template )
    {
        String name = "any type of object";
        if ( type != BaseIdentifiableObject.class && type != IdentifiableObject.class )
        {
            Api.Schema schema = api.getSchemas().get( type );
            name = schema == null ? type.getSimpleName() : schema.getSharedName().getValue();
        }
        addStringMultilineMember( "description", String.format( template, name, type.getTypeName() ) );
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

    /**
     * The first controller level tag is used to split controllers into OpenAPI
     * documents so it is added last since tools might give priority to the
     * order of tags.
     */
    private static <E> Set<E> mergeTags( Set<E> controller, Set<E> endpoint )
    {
        if ( controller.isEmpty() && endpoint.isEmpty() )
            return new HashSet<>();
        if ( controller.isEmpty() )
            return endpoint;
        Set<E> merged = Stream.concat( controller.stream().skip( 1 ), endpoint.stream() )
            .collect( toCollection( LinkedHashSet::new ) );
        if ( !controller.isEmpty() )
        {
            merged.add( controller.iterator().next() );
        }
        return merged;
    }

    /**
     * Generates an 11 character UID based on the target type. This is so each
     * UID type gets its unique but stable example.
     */
    private static String generateUid( Class<?> fromType )
    {
        char[] chars = CodeGenerator.ALPHANUMERIC_CHARS.toCharArray();
        String key = fromType.getSimpleName();
        key = key.repeat( (11 / key.length()) + 1 );
        StringBuilder uid = new StringBuilder( 11 );
        int offset = fromType.getSimpleName().length();
        for ( int i = 0; i < 11; i++ )
        {
            int index = key.charAt( i ) + offset;
            uid.append( chars[index % chars.length] );
            // this is just to get more realistic character distribution
            // 13 because it is about half the alphabet
            offset += 13;
        }
        return uid.toString();
    }
}
