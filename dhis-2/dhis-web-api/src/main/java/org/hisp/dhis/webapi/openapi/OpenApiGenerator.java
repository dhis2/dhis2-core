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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.period.PeriodType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public class OpenApiGenerator
{
    @Value
    static class Config
    {
        Format format;

        Document document;

        @Value
        static class Format
        {
            boolean newLineAfterMember;

            boolean newLineAfterObjectStart;

            boolean newLineBeforeObjectEnd;

            boolean newLineAfterArrayStart;

            boolean newLineBeforeArrayEnd;

            String memberIndent;
        }

        @Value
        static class Document
        {
            String version;

            String serverUrl;

            boolean syntheticSummary;

            boolean syntheticDescription;
        }
    }

    @Value
    private static class BasicSchema
    {
        String type;

        String format;

        Boolean nullable;

        Integer minLength;

        Integer maxLength;
    }

    private static final Map<Class<?>, BasicSchema> BASIC_SCHEMAS = new IdentityHashMap<>();

    private static void addBasicSchema( Class<?> of, String type, String format, Boolean nullable )
    {
        addBasicSchema( of, type, format, nullable, null, null );
    }

    private static void addBasicSchema( Class<?> of, String type, String format, Boolean nullable, Integer minLength,
        Integer maxLength )
    {
        BASIC_SCHEMAS.put( of, new BasicSchema( type, format, nullable, minLength, maxLength ) );
    }

    static
    {
        addBasicSchema( int.class, "integer", "int32", false );
        addBasicSchema( long.class, "integer", "int64", false );
        addBasicSchema( float.class, "number", "float", false );
        addBasicSchema( double.class, "number", "double", false );
        addBasicSchema( boolean.class, "boolean", null, false );
        addBasicSchema( char.class, "string", null, false, 1, 1 );
        addBasicSchema( Integer.class, "integer", "int32", false );
        addBasicSchema( Long.class, "integer", "int64", true );
        addBasicSchema( Float.class, "number", "float", true );
        addBasicSchema( Double.class, "number", "double", true );
        addBasicSchema( Boolean.class, "boolean", null, true );
        addBasicSchema( Character.class, "string", null, true, 1, 1 );
        addBasicSchema( String.class, "string", null, true );
        addBasicSchema( Date.class, "string", "date-time", true );
        addBasicSchema( Locale.class, "string", null, true );
        addBasicSchema( JsonNode.class, "object", null, null );
        addBasicSchema( ObjectNode.class, "object", null, null );
        addBasicSchema( ArrayNode.class, "array", null, null );
        addBasicSchema( RootNode.class, "object", null, false );
        addBasicSchema( JsonPointer.class, "string", null, false );
        addBasicSchema( PeriodType.class, "string", null, false );
        addBasicSchema( Serializable.class, "string", null, null );
    }

    public static String generate( Api api )
    {
        return generate( api, new Config(
            new Config.Format( true, true, true, true, true, "  " ),
            new Config.Document( "2.40", "https://play.dhis2.org/dev", true, true ) ) );
    }

    public static String generate( Api api, Config config )
    {
        int endpoints = 0;
        for ( Api.Controller c : api.getControllers() )
            endpoints += c.getEndpoints().size();
        int capacity = endpoints * 256 + api.getSchemas().size() * 512;
        OpenApiGenerator gen = new OpenApiGenerator( api, new StringBuilder( capacity ), config.format, config.document,
            "" );
        gen.generateDocument();
        return gen.out.toString();
    }

    private final Api api;

    private final StringBuilder out;

    private final Config.Format format;

    private final Config.Document document;

    private String indent;

    private void generateDocument()
    {
        addRootObject( () -> {
            addStringMember( "openapi", "3.0.0" );
            addObjectMember( "info", () -> {
                addStringMember( "title", "DHIS2 API" );
                addStringMember( "version", document.version );
            } );
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
        // FIXME this discards endpoints where same method is defined multiple
        // times because of different media types
        // this needs to be merged but OpenAPI does not really support this
        // properly
        // as in theory these endpoints can be different not just by their
        // responses
        endpoints.forEach( e -> e.getMethods().forEach( m -> endpointByMethod.compute( m, ( k, v ) -> {
            // a hack to prefer the JSON endpoint if there is a clash
            if ( v == null )
                return e;
            if ( v.getConsumes().contains( MediaType.APPLICATION_JSON ) )
                return v;
            return e;
        } ) ) );
        addObjectMember( path, () -> endpointByMethod.forEach( this::generatePathMethod ) );
    }

    private void generatePathMethod( RequestMethod method, Api.Endpoint endpoint )
    {
        addObjectMember( method.name().toLowerCase(), () -> {
            addStringMember( "summary", null ); // TODO
            addStringMember( "operationId", endpoint.getName() );
            addArrayMember( "parameters", endpoint.getParameters().stream()
                .filter( p -> p.getIn() != Api.Parameter.In.BODY ),
                this::generateParameter );
            endpoint.getParameters().stream()
                .filter( p -> p.getIn() == Api.Parameter.In.BODY )
                .findFirst()
                .ifPresent( requestBody -> addObjectMember( "requestBody",
                    () -> generateRequestBody( requestBody, endpoint.getConsumes() ) ) );
            addObjectMember( "responses", () -> endpoint.getResponses()
                .forEach( response -> generateResponse( response, endpoint.getProduces(), endpoint.getConsumes() ) ) );
        } );
    }

    private void generateParameter( Api.Parameter parameter )
    {
        addObjectMember( null, () -> {
            addStringMember( "name", parameter.getName() );
            addStringMember( "in", parameter.getIn().name().toLowerCase() );
            addStringMember( "description", null ); // TODO
            addBooleanMember( "required", parameter.isRequired() );
            addObjectMember( "schema", () -> generateSchemaOrRef( parameter.getType() ) );
        } );
    }

    private void generateRequestBody( Api.Parameter body, List<MediaType> consumes )
    {
        if ( consumes.isEmpty() )
            consumes.add( MediaType.APPLICATION_JSON );
        addStringMember( "description", null ); // TODO
        addBooleanMember( "required", body.isRequired() );
        addObjectMember( "content", () -> {
            consumes.forEach( type -> {
                addObjectMember( type.toString(),
                    () -> addObjectMember( "schema", () -> generateSchemaOrRef( body.getType() ) ) );
            } );
        } );
    }

    private void generateResponse( Api.Response response, List<MediaType> produces, List<MediaType> consumes )
    {
        addObjectMember( String.valueOf( response.getStatus().value() ), () -> {
            addStringMember( "description", "TODO description is required" ); // TODO
            // addObjectMember( "headers", null ); //TODO
            if ( response.getBody() != null
                && response.getBody().getSource() != void.class
                && response.getStatus() != HttpStatus.NO_CONTENT )
            {
                if ( produces.isEmpty() )
                {
                    // make API symmetric by sing the first consumes if no
                    // produces is set
                    produces.add( !consumes.isEmpty() ? consumes.get( 0 ) : MediaType.APPLICATION_JSON );
                }
                addObjectMember( "content", () -> produces.forEach( type -> addObjectMember( type.toString(),
                    () -> addObjectMember( "schema", () -> generateSchemaOrRef( response.getBody() ) ) ) ) );
            }
        } );
    }

    private void generateSchemas()
    {
        api.getSchemas().values().stream()
            .filter( OpenApiGenerator::isReferencableSchema )
            .forEach( s -> addObjectMember( s.getName(), () -> generateSchema( s ) ) );
    }

    private static boolean isReferencableSchema( Api.Schema schema )
    {
        return !schema.getName().isEmpty()
            && !schema.getFields().isEmpty()
            && !BASIC_SCHEMAS.containsKey( schema.getSource() );
    }

    private void generateSchema( Api.Schema schema )
    {
        Class<?> type = schema.getSource();
        BasicSchema bs = BASIC_SCHEMAS.get( type );
        if ( bs != null )
        {
            addStringMember( "type", bs.getType() );
            addStringMember( "format", bs.getFormat() );
            addBooleanMember( "nullable", bs.getNullable() );
            addNumberMember( "minLength", bs.getMinLength() );
            addNumberMember( "maxLength", bs.getMaxLength() );
            return;
        }
        if ( type.isEnum() )
        {
            addStringMember( "type", "string" );
            addArrayMember( "enum", stream( type.getEnumConstants() ),
                c -> addStringMember( null, ((Enum<?>) c).name() ) );
            return;
        }
        if ( type.isArray() )
        {
            addStringMember( "type", "array" );
            addObjectMember( "items",
                () -> generateSchemaOrRef( api.getSchemas().get( type.getComponentType() ) ) );
            return;
        }
        // best guess: it is an object type
        addStringMember( "type", "object" );
        if ( !schema.getFields().isEmpty() )
        {
            addArrayMember( "required", schema.getFields().stream()
                .filter( f -> Boolean.TRUE.equals( f.getRequired() ) ),
                f -> addStringMember( null, f.getName() ) );
            addObjectMember( "properties", () -> schema.getFields()
                .forEach( field -> addObjectMember( field.getName(), () -> generateSchemaOrRef( field.getType() ) ) ) );
        }
        else
        {
            System.out.println( schema + " " + schema.getSource() + " " + schema.getHint() );
            log.warn( schema.toString() );
        }
    }

    private void generateSchemaOrRef( Api.Schema schema )
    {
        if ( schema == null )
            return;
        if ( !isReferencableSchema( schema ) )
        {
            generateSchema( schema );
        }
        else
        {
            addStringMember( "$ref", "#/components/schemas/" + schema.getName() );
        }
    }

    /*
     * Open API document generation helpers
     */

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

    /*
     * Basic JSON writing helpers
     */

    private void addRootObject( Runnable addMembers )
    {
        addObjectMember( null, addMembers );
        discardLastMemberComma( 0 );
    }

    private void addObjectMember( String name, Runnable addMembers )
    {
        appendMemberName( name );
        out.append( "{" );
        if ( format.newLineAfterObjectStart )
            out.append( '\n' );
        int length = out.length();
        if ( format.newLineAfterMember )
            indent += format.memberIndent;
        appendItems( addMembers );
        if ( format.newLineAfterMember )
            indent = indent.substring( 0, indent.length() - format.memberIndent.length() );
        discardLastMemberComma( length );
        if ( format.newLineBeforeObjectEnd )
        {
            out.append( '\n' );
            appendMemberIndent();
        }
        out.append( "}" );
        appendMemberComma();
    }

    private <E> void addArrayMember( String name, Stream<E> items, Consumer<E> forEach )
    {
        List<E> l = items.collect( toList() );
        if ( !l.isEmpty() )
        {
            addArrayMember( name, () -> l.forEach( forEach ) );
        }
    }

    private void addArrayMember( String name, Runnable addElements )
    {
        appendMemberName( name );
        out.append( '[' );
        if ( format.newLineAfterArrayStart )
            out.append( '\n' );
        int length = out.length();
        appendItems( addElements );
        discardLastMemberComma( length );
        if ( format.newLineBeforeArrayEnd )
        {
            out.append( '\n' );
            appendMemberIndent();
        }
        out.append( ']' );
        appendMemberComma();
    }

    private void addStringMember( String name, String value )
    {
        if ( value != null )
        {
            appendMemberName( name );
            appendString( value );
            appendMemberComma();
        }
    }

    private void addBooleanMember( String name, Boolean value )
    {
        if ( value != null )
        {
            addBooleanMember( name, value.booleanValue() );
        }
    }

    private void addBooleanMember( String name, boolean value )
    {
        appendMemberName( name );
        out.append( value ? "true" : "false" );
        appendMemberComma();
    }

    private void addNumberMember( String name, Integer value )
    {
        if ( value != null )
        {
            addNumberMember( name, value.intValue() );
        }
    }

    private void addNumberMember( String name, int value )
    {
        appendMemberName( name );
        out.append( value );
        appendMemberComma();
    }

    private void appendItems( Runnable items )
    {
        if ( items != null )
        {
            items.run();
        }
    }

    private StringBuilder appendString( String str )
    {
        return str == null
            ? out.append( "null" )
            : out.append( '"' ).append( str ).append( '"' );
    }

    private void appendMemberName( String name )
    {
        appendMemberIndent();
        if ( name != null )
        {
            appendString( name ).append( ':' );
        }
    }

    private void appendMemberIndent()
    {
        if ( format.newLineAfterMember )
            out.append( indent );
    }

    private void appendMemberComma()
    {
        out.append( ',' );
        if ( format.newLineAfterMember )
            out.append( '\n' );
    }

    private void discardLastMemberComma( int length )
    {
        if ( out.length() > length )
        {
            int back = format.newLineAfterMember ? 2 : 1;
            out.setLength( out.length() - back ); // discard last ,
        }
    }

}
